package quantisan.qte_lmax;

import com.lmax.api.*;
import com.lmax.api.account.LoginCallback;
import com.lmax.api.account.LoginRequest;
import com.lmax.api.heartbeat.HeartbeatCallback;
import com.lmax.api.heartbeat.HeartbeatEventListener;
import com.lmax.api.heartbeat.HeartbeatRequest;
import com.lmax.api.heartbeat.HeartbeatSubscriptionRequest;
import com.lmax.api.orderbook.*;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ThinBot implements LoginCallback, HeartbeatEventListener, OrderBookEventListener, StreamFailureListener, SessionDisconnectedListener, Runnable {
    final static Logger logger = LoggerFactory.getLogger(ThinBot.class);
    private final static String EXCHANGE_NAME = "ticks";
    private final static int HEARTBEAT_PERIOD = 4 * 60 * 1000;
    private final static int reconnectTries = 5;
    private final static String brokerUrl = "https://testapi.lmaxtrader.com";

    private Session session;
    private int reconnectCount;
    private ConnectionFactory factory;
    private Connection connection;
    private Channel channel;

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

        // subscribe to heatbeat //
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
        for (long instrumentId = 4001; instrumentId < 4018; instrumentId++)
            subscribeToInstrument(session, instrumentId);

        subscribeToInstrument(session, 100637);  // Gold
        subscribeToInstrument(session, 100639);  // Silver

        // RabbitMQ
        factory = new ConnectionFactory();
        factory.setHost("localhost");
        try {
            connection = factory.newConnection();
            channel = connection.createChannel();
            channel.exchangeDeclare(EXCHANGE_NAME, "topic");
        } catch (IOException e) {
            logger.error("Can't open a rabbitmq connection.", e);
            System.exit(1);
        }

        new Thread(this).start();  // heartbeat request
        session.start();

        try {
            channel.close();
            connection.close();
        } catch (IOException e) {
            logger.error("Can't close rabbitmq connection.", e);
            System.exit(1);
        }

    }

    @Override
    public void onLoginFailure(FailureResponse failureResponse) {
        throw new RuntimeException("Unable to login: " + failureResponse.getDescription(), failureResponse.getException());
    }

    @Override
    public void notifyStreamFailure(Exception e)
    {
        logger.error("Stream failure. {}.", e);
    }

    //******************************************************************************//
    //   Market data

    @Override
    public void notify(OrderBookEvent orderBookEvent) {
        Tick tick = new Tick(orderBookEvent);

        if(tick.isValid()) {
            try {
                String routingKey = getRouting(tick.getInstrumentName());
                String message = tick.toString();
                channel.basicPublish(EXCHANGE_NAME, routingKey, null, message.getBytes());
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
    //******************************************************************************//

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
}