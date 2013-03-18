package quantisan.qte_lmax;

import com.lmax.api.*;
import com.lmax.api.account.LoginCallback;
import com.lmax.api.account.LoginRequest;
import com.lmax.api.heartbeat.HeartbeatCallback;
import com.lmax.api.heartbeat.HeartbeatEventListener;
import com.lmax.api.heartbeat.HeartbeatRequest;
import com.lmax.api.heartbeat.HeartbeatSubscriptionRequest;
import com.lmax.api.orderbook.OrderBookEvent;
import com.lmax.api.orderbook.OrderBookEventListener;
import com.lmax.api.orderbook.OrderBookSubscriptionRequest;

public class ThinBot implements LoginCallback, HeartbeatEventListener, OrderBookEventListener, StreamFailureListener, Runnable {
    private final static int HEARTBEAT_PERIOD = 2 * 60 * 1000;

    private Session session;

    public static void main(String[] args) {
        String demoUrl = "https://testapi.lmaxtrader.com";
        LmaxApi lmaxApi = new LmaxApi(demoUrl);

        ThinBot thinBot = new ThinBot();
        lmaxApi.login(new LoginRequest("quantisan2", "J63VFqmXBaQStdAxKnD7", LoginRequest.ProductType.CFD_DEMO), thinBot);
    }

    @Override
    public void onLoginSuccess(Session session) {
        System.out.printf("Logged in, account details: %s%n", session.getAccountDetails());

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
        {
            subscribeToInstrument(session, instrumentId);
        }

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
        System.err.printf("Stream failure: %s", e.getMessage());
        e.printStackTrace(System.err);
    }

    //******************************************************************************//
    //   Market data

    @Override
    public void notify(OrderBookEvent orderBookEvent) {
        Tick tick = new Tick(orderBookEvent);
        System.out.println(tick);

    }

    private void subscribeToInstrument(final Session session, final long instrumentId)
    {
        session.subscribe(new OrderBookSubscriptionRequest(instrumentId), new Callback()
        {
            public void onSuccess()
            {
                System.out.printf("Subscribed to instrument %d.%n", instrumentId);
            }

            public void onFailure(final FailureResponse failureResponse)
            {
                System.err.printf("Failed to subscribe to instrument %d: %s%n", instrumentId, failureResponse);
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
                throw new RuntimeException("Failed");
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
            e.printStackTrace();
        }
    }
    //******************************************************************************//
}