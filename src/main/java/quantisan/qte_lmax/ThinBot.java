package quantisan.qte_lmax;

import com.lmax.api.*;
import com.lmax.api.account.LoginCallback;
import com.lmax.api.account.LoginRequest;
import com.lmax.api.heartbeat.HeartbeatCallback;
import com.lmax.api.heartbeat.HeartbeatEventListener;
import com.lmax.api.heartbeat.HeartbeatRequest;
import com.lmax.api.heartbeat.HeartbeatSubscriptionRequest;
import com.lmax.api.orderbook.*;
import com.lmax.api.reject.InstructionRejectedEvent;
import com.lmax.api.reject.InstructionRejectedEventListener;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ThinBot implements LoginCallback,
        HeartbeatEventListener, OrderBookEventListener, StreamFailureListener, SessionDisconnectedListener,
        InstructionRejectedEventListener
{
    final static Logger logger = LoggerFactory.getLogger(ThinBot.class);
    private final static String TICKS_EXCHANGE_NAME = "ticks";
    private final static String ACCOUNTING_QUEUE_NAME = "lmax.accounting";
    private final static String ORDER_QUEUE_NAME = "lmax.order";  // TODO take username param and use individual order channel
    private final static int HEARTBEAT_PERIOD = 4 * 60 * 1000;
    private final static int reconnectTries = 5;
    private final static String brokerUrl = "https://testapi.lmaxtrader.com";

    private Session session;
    private int reconnectCount;
    private Channel channelTickProducer;
    private Channel channelAccountProducer;
    private Channel channelOrderReceiver;

    public static void loginLmax(String url) {
        LmaxApi lmaxApi = new LmaxApi(url);
        ThinBot thinBot = new ThinBot();
        lmaxApi.login(new LoginRequest("quantisan2", "J63VFqmXBaQStdAxKnD7", LoginRequest.ProductType.CFD_DEMO), thinBot);
    }

    public static void main(String[] args) {
        loginLmax(brokerUrl);
    }

    @Override
    public void onLoginSuccess(Session session) {
        logger.info("Logged in, account details: {}.", session.getAccountDetails());

        this.session = session;
        this.reconnectCount = 0;
        session.registerHeartbeatListener(this);
        session.registerOrderBookEventListener(this);
        session.registerStreamFailureListener(this);
        session.registerSessionDisconnectedListener(this);

        // subscribe to heartbeat //
        session.subscribe(new HeartbeatSubscriptionRequest(), new Callback()
        {
            public void onSuccess() { }

            @Override
            public void onFailure(final FailureResponse failureResponse)
            {
                throw new RuntimeException("Heartbeat subscription failed");
            }
        });

        // subscribe to instrument data //
        for (long instrumentId = 4001; instrumentId < 4018; instrumentId++) {
            subscribeToInstrument(session, instrumentId);
        }
        subscribeToInstrument(session, 100637);  // Gold
        subscribeToInstrument(session, 100639);  // Silver

        // RabbitMQ
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = null;

        try {
            connection = factory.newConnection();
            channelTickProducer = connection.createChannel();
            logger.debug("Declaring tick exchange.");
            channelTickProducer.exchangeDeclare(TICKS_EXCHANGE_NAME, "topic", true);  // durable

            channelAccountProducer = connection.createChannel();
            logger.debug("Declaring accounting queue.");
            channelAccountProducer.queueDeclare(ACCOUNTING_QUEUE_NAME, false, true, false, null);

            channelOrderReceiver = connection.createChannel();
            logger.debug("Declaring order queue.");
            channelOrderReceiver.queueDeclare(ORDER_QUEUE_NAME, false, true, false, null);  // exclusive
        } catch (IOException e) {
            logger.error("Can't open a rabbitmq connection.", e);
            System.exit(1);
        }

        // heartbeat request
        logger.debug("Starting heartbeat.");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try
                {
                    while (!Thread.currentThread().isInterrupted())
                    {
                        Thread.sleep(HEARTBEAT_PERIOD);
                        requestHeartbeat();
                    }
                }
                catch (InterruptedException e)
                {
                    logger.warn("Fail to request heartbeat: {}.", e);
                }
            }
        }).start();

        // consumer for order queue
        final QueueingConsumer consumer = new QueueingConsumer(channelOrderReceiver);
        try {
            channelOrderReceiver.basicConsume(ORDER_QUEUE_NAME, consumer);
        } catch (IOException e) {
            logger.error("Can't start consumer.", e);
        }

        logger.debug("Listening queueing consumer.");
        new Thread(new OrderObserver(session, channelOrderReceiver, consumer)).start();

        logger.debug("Session starting");
        session.start();

        try {
            logger.debug("Closing rabbitmq channels.");
            channelTickProducer.close();
            channelOrderReceiver.close();
            logger.debug("Closing rabbitmq connection.");
            connection.close();
        } catch (IOException e) {
            logger.error("Can't close rabbitmq connection.", e);
            System.exit(1);
        }
    }

    //******************************************************************************//
    //   Market data

    @Override
    public void notify(OrderBookEvent orderBookEvent) {
        Tick tick = new Tick(orderBookEvent);

        if(tick.isValid()) {
            try {
                String routingKey = getRouting(tick.getInstrumentName());
                String message = tick.toEdn();
                channelTickProducer.basicPublish(TICKS_EXCHANGE_NAME, routingKey, null, message.getBytes());
                logger.debug("Sent {}", tick.toString());


            } catch (IOException e) {
                logger.error("Error publishing tick.", e);
            }
        }
    }

    private String getRouting(String inst) {
        return "lmax." + inst.toUpperCase();
    }

    private void subscribeToInstrument(final Session session, final long instrumentId)
    {
        session.subscribe(new OrderBookSubscriptionRequest(instrumentId), new Callback()
        {
            public void onSuccess()
            {
                logger.debug("Subscribed to instrument {}.", instrumentId);
            }

            public void onFailure(final FailureResponse failureResponse)
            {
                logger.warn("Failed to subscribe to instrument: {}.", instrumentId);
                logger.warn("{}. : {}.", failureResponse.getMessage(), failureResponse.getDescription());
            }
        });
    }
    //******************************************************************************//

    //******************************************************************************//
    //   Heart Beat

    @Override
    public void notify(long accountId, String token) {
    }

    private void requestHeartbeat()
    {
        this.session.requestHeartbeat(new HeartbeatRequest("token"), new HeartbeatCallback()
        {
            @Override
            public void onSuccess(String token) { }

            @Override
            public void onFailure(FailureResponse failureResponse)
            {
                logger.warn(failureResponse.getMessage(), failureResponse.getDescription());
                throw new RuntimeException("Heartbeat receive failed");
            }
        });
    }

    //******************************************************************************//

    //******************************************************************************//
    // Handling errors

    @Override
    public void onLoginFailure(FailureResponse failureResponse) {
        throw new RuntimeException("Unable to login: " + failureResponse.getDescription(), failureResponse.getException());
    }

    @Override
    public void notifyStreamFailure(Exception e)
    {
        logger.error("Stream failure. {}.", e);
    }

    @Override
    public void notifySessionDisconnected() {
        if (++reconnectCount <= reconnectTries)
        {
            logger.warn("Session disconnected - attempting to log in again (attempt {})", reconnectCount);
            loginLmax(brokerUrl);
        } else {
            logger.error("Session disconnected - aborting after too many reconnect attempts");
            System.exit(1);
        }
    }

    @Override
    public void notify(InstructionRejectedEvent instructionRejected) {
        logger.warn("Rejection received for {}, reason: {}.", instructionRejected.getInstructionId(), instructionRejected.getReason());
        String message = "{:type :instruction-reject"
                + ", :order-id \"" + instructionRejected.getInstructionId() + "\""
                + ", :account-id " + instructionRejected.getAccountId()
                + ", :instrument " + instructionRejected.getInstructionId()
                + ", :reason " + instructionRejected.getReason() + "}";

        try {
            channelAccountProducer.basicPublish("", ACCOUNTING_QUEUE_NAME, null, message.getBytes());
        } catch (IOException e) {
            logger.error("Account message publish error.", e);
        }

    }
    //******************************************************************************//
}