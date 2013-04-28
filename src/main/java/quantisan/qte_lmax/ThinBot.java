package quantisan.qte_lmax;

import com.lmax.api.*;
import com.lmax.api.account.*;
import com.lmax.api.heartbeat.HeartbeatCallback;
import com.lmax.api.heartbeat.HeartbeatEventListener;
import com.lmax.api.heartbeat.HeartbeatRequest;
import com.lmax.api.heartbeat.HeartbeatSubscriptionRequest;
import com.lmax.api.order.*;
import com.lmax.api.order.Order;
import com.lmax.api.orderbook.*;
import com.lmax.api.position.PositionEvent;
import com.lmax.api.position.PositionEventListener;
import com.lmax.api.position.PositionSubscriptionRequest;
import com.lmax.api.reject.InstructionRejectedEvent;
import com.lmax.api.reject.InstructionRejectedEventListener;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class ThinBot implements LoginCallback,
        HeartbeatEventListener, OrderBookEventListener, StreamFailureListener, SessionDisconnectedListener,
        InstructionRejectedEventListener, ExecutionEventListener, OrderEventListener, AccountStateEventListener,
        PositionEventListener
{
    final static Logger logger = LoggerFactory.getLogger(ThinBot.class);
    private final static String RABBITMQ_SERVER = "localhost";
    private final static String TICKS_EXCHANGE_NAME = "ticks";
    public final static String ACCOUNTING_EXCHANGE_NAME = "accounting.lmax";
    private final static String ORDER_QUEUE_NAME = "engine.command.lmax";  // TODO take username param and use individual order channel
    private final static int HEARTBEAT_PERIOD = 4 * 60 * 1000;
    private final static int reconnectTries = 5;
    private final static String brokerUrl = "https://testapi.lmaxtrader.com";   // TODO use properties file for account config
    public final static String USER_NAME = "paul";

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
        session.registerHeartbeatListener(this);             // TODO refactor subscribe stuff into func
        session.registerOrderBookEventListener(this);
        session.registerStreamFailureListener(this);
        session.registerSessionDisconnectedListener(this);
        session.registerOrderEventListener(this);
        session.registerExecutionEventListener(this);
        session.registerAccountStateEventListener(this);
        session.registerPositionEventListener(this);

        // subscribe to heartbeat //
        session.subscribe(new HeartbeatSubscriptionRequest(), new Callback()
        {
            public void onSuccess() {
                logger.debug("Subscribed to heartbeat event.");
            }

            @Override
            public void onFailure(final FailureResponse failureResponse)
            {
                throw new RuntimeException("Heartbeat subscription failed");
            }
        });

        String[] insts = new String[] {"EURUSD", "GBPUSD", "USDJPY", "XAUUSD", "XAGUSD"};
        List<String> instList = Arrays.asList(insts);

        // subscribe to instrument data //
        for (String instrumentName : instList) {
            subscribeToInstrument(session, Instrument.toId(instrumentName));
        }

        // RabbitMQ
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RABBITMQ_SERVER);
        Connection connection = null;

        try {
            connection = factory.newConnection();
            channelTickProducer = connection.createChannel();
            logger.debug("Declaring tick exchange.");
            channelTickProducer.exchangeDeclare(TICKS_EXCHANGE_NAME, "topic", true);  // durable

            channelAccountProducer = connection.createChannel();
            logger.debug("Declaring accounting exchange.");
            channelAccountProducer.exchangeDeclare(ACCOUNTING_EXCHANGE_NAME, "fanout", true);

            channelOrderReceiver = connection.createChannel();
            logger.debug("Declaring order queue.");
            channelOrderReceiver.queueDeclare(ORDER_QUEUE_NAME, true, false, false, null);  // durable
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
        final QueueingConsumer orderConsumer = new QueueingConsumer(channelOrderReceiver);
        try {
            channelOrderReceiver.basicConsume(ORDER_QUEUE_NAME, orderConsumer);
        } catch (IOException e) {
            logger.error("Can't start consumer.", e);
        }

        logger.debug("Listening queueing consumer.");
        new Thread(new OrderObserver(session, channelOrderReceiver, channelAccountProducer, orderConsumer)).start();
        session.subscribe(new ExecutionSubscriptionRequest(), new Callback() {
            @Override
            public void onSuccess() {
                logger.debug("Subscribed to execution event.");
            }

            @Override
            public void onFailure(FailureResponse failureResponse) {
                logger.error("Failed to subscribe to execution event. {}, ", failureResponse.getMessage(), failureResponse.getDescription());
                throw new RuntimeException("Execution event subscription failed");
            }
        });

        session.subscribe(new AccountSubscriptionRequest(), new Callback()
        {
            @Override
            public void onSuccess()
            {
                logger.debug("Successful account event subscription.");
            }

            @Override
            public void onFailure(FailureResponse failureResponse)
            {
                logger.error("Failed to subscribe to account event: {}", failureResponse);
                throw new RuntimeException("Account event subscription failed");
            }
        });

        session.subscribe(new PositionSubscriptionRequest(), new Callback() {
            @Override
            public void onSuccess() {
                logger.debug("Successful position event subscription.");
            }

            @Override
            public void onFailure(FailureResponse failureResponse) {
                logger.error("Failed to subscribe to position event: {}", failureResponse);
                throw new RuntimeException("Position event subscription failed");
            }
        });

        logger.debug("Session starting");
        session.start();

        try {
            logger.debug("Closing rabbitmq channels.");
            channelTickProducer.close();
            channelAccountProducer.close();
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
                channelTickProducer.basicPublish(TICKS_EXCHANGE_NAME, routingKey, null, tick.toEdn().getBytes());
//                logger.debug("Sent {}", tick.toString());
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
    // Account and order states event

    @Override
    public void notify(AccountStateEvent accountStateEvent) {
//        logger.info(accountStateEvent.toString());
    }

    @Override
    public void notify(Order order) {
//        logger.info(order.toString());
    }

    @Override
    public void notify(Execution execution) {
        String message = EdnMessage.executionEvent(execution);
        logger.info("Order executed: {}.", message);
        try {
            channelAccountProducer.basicPublish(ACCOUNTING_EXCHANGE_NAME, "", null, message.getBytes());
        } catch (IOException e) {
            logger.error("Cannot publish execution event: ", e);
        }
    }

    @Override
    public void notify(PositionEvent positionEvent) {
        String message = EdnMessage.positionEvent(positionEvent);
        logger.info("Position message: {}.", message);
        try {
            channelAccountProducer.basicPublish(ACCOUNTING_EXCHANGE_NAME, "", null, message.getBytes());
        } catch (IOException e) {
            logger.error("Cannot publish position event: ", e);
        }
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
        if (isWeekendNow() && e instanceof ConnectException)   // TODO reconnect on Sunday
        {
            session.stop();
            session.logout(new Callback() {
                @Override
                public void onSuccess() {
                    logger.info("Logged out for weekend LMAX server maintenance.");
                }

                @Override
                public void onFailure(FailureResponse failureResponse) {
                }
            });
        }

        logger.error("Stream failure.", e);
    }

    protected static boolean isWeekendNow() { // TODO move to helper class
        Calendar c = Calendar.getInstance();
        int day = c.get(Calendar.DAY_OF_WEEK);
        int hour = c.get(Calendar.HOUR_OF_DAY);
        return (day == 1 || day ==7);
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
    public void notify(InstructionRejectedEvent instructionRejected) {      // TODO not seem to work
        logger.warn("Rejection received for {}, reason: {}.", instructionRejected.getInstructionId(), instructionRejected.getReason());
        String message = "{:type :instruction-rejected"     // TODO move into EdnMessage
                + ", :order-id \"" + instructionRejected.getInstructionId() + "\""
                + ", :user-id " + ThinBot.USER_NAME
                + ", :instrument " + instructionRejected.getInstrumentId()
                + ", :reason " + instructionRejected.getReason() + "}";

        try {
            channelAccountProducer.basicPublish("", ACCOUNTING_EXCHANGE_NAME, null, message.getBytes());
        } catch (IOException e) {
            logger.error("Account message publish error.", e);
        }

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

}