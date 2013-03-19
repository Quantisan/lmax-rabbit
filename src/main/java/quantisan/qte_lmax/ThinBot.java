package quantisan.qte_lmax;

import com.lmax.api.*;
import com.lmax.api.account.LoginCallback;
import com.lmax.api.account.LoginRequest;
import com.lmax.api.heartbeat.HeartbeatCallback;
import com.lmax.api.heartbeat.HeartbeatEventListener;
import com.lmax.api.heartbeat.HeartbeatRequest;
import com.lmax.api.heartbeat.HeartbeatSubscriptionRequest;
import com.lmax.api.orderbook.*;
import com.mongodb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;

public class ThinBot implements LoginCallback, HeartbeatEventListener, OrderBookEventListener, StreamFailureListener, Runnable {
    final static Logger logger = LoggerFactory.getLogger(ThinBot.class);
    private final static int HEARTBEAT_PERIOD = 2 * 60 * 1000;

    private Session session;
    private DB db;


    public static void main(String[] args) {
        String demoUrl = "https://testapi.lmaxtrader.com";
        LmaxApi lmaxApi = new LmaxApi(demoUrl);

        ThinBot thinBot = new ThinBot();
        lmaxApi.login(new LoginRequest("quantisan2", "J63VFqmXBaQStdAxKnD7", LoginRequest.ProductType.CFD_DEMO), thinBot);
    }

    @Override
    public void onLoginSuccess(Session session) {
        logger.info("Logged in, account details: {}.", session.getAccountDetails());

        this.session = session;
        session.registerHeartbeatListener(this);
        session.registerOrderBookEventListener(this);
        session.registerStreamFailureListener(this);

        session.subscribe(new HeartbeatSubscriptionRequest(), new Callback()
        {
            public void onSuccess() { };

            @Override
            public void onFailure(final FailureResponse failureResponse)
            {
                throw new RuntimeException("Heartbeat subscription failed");
            }
        });

        for (long instrumentId = 4001; instrumentId < 4018; instrumentId++)
            subscribeToInstrument(session, instrumentId);

        subscribeToInstrument(session, 100637);  // Gold
        subscribeToInstrument(session, 100639);  // Silver

        MongoClient mongoClient = null;
        try {
            mongoClient = new MongoClient( "localhost" , 27017 );
        } catch (UnknownHostException e) {
            session.stop();
            logger.error("Unable to connect to MongoDB: {}.", e);
            throw new RuntimeException("Unable to connect to MongoDB");
        }

        mongoClient.setWriteConcern(WriteConcern.UNACKNOWLEDGED);
        this.db = mongoClient.getDB("ticks_buffer");
        DBCollection coll = db.getCollection("lmax");
        coll.ensureIndex(BasicDBObjectBuilder.start().add("Date", 1).get(),
                BasicDBObjectBuilder.start().add("expireAfterSeconds", 3600).get());
        coll.ensureIndex(BasicDBObjectBuilder.start().add("instrument", 1).add("Date", 1).get());

        new Thread(this).start();  // heartbeat request
        session.start();
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
        logger.debug(tick.toString());
        if(tick.isValid()) {
            DBCollection coll = db.getCollection("lmax");
            BasicDBObject item = new BasicDBObject("Date", tick.getDate()).
                    append("instrument", tick.getInstrumentId()).
                    append("bidPrice", tick.getBidPrice()).
                    append("bidVolume", tick.getBidVolume()).
                    append("askPrice", tick.getAskPrice()).
                    append("askVolume", tick.getAskVolume());
            coll.insert(item);
        }
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
            public void onSuccess(String token) { };

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
}