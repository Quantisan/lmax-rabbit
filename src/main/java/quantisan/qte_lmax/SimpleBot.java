package quantisan.qte_lmax;

import com.lmax.api.*;
import com.lmax.api.account.LoginCallback;
import com.lmax.api.account.LoginRequest;
import com.lmax.api.heartbeat.HeartbeatCallback;
import com.lmax.api.heartbeat.HeartbeatEventListener;
import com.lmax.api.heartbeat.HeartbeatRequest;
import com.lmax.api.heartbeat.HeartbeatSubscriptionRequest;
import com.lmax.api.order.*;
import com.lmax.api.orderbook.OrderBookEvent;
import com.lmax.api.orderbook.OrderBookEventListener;

public class SimpleBot implements LoginCallback, HeartbeatEventListener, OrderBookEventListener, Runnable {
    private final static int HEARTBEAT_PERIOD = 2 * 60 * 1000;

    private Session session;

    public static void main(String[] args) {
        String demoUrl = "https://testapi.lmaxtrader.com";
        LmaxApi lmaxApi = new LmaxApi(demoUrl);

        SimpleBot simpleBot = new SimpleBot();
        lmaxApi.login(new LoginRequest("quantisan2", "J63VFqmXBaQStdAxKnD7", LoginRequest.ProductType.CFD_DEMO), simpleBot);
    }

    @Override
    public void onLoginSuccess(Session session) {
        System.out.printf("Logged in, account details: %s%n", session.getAccountDetails());


        this.session = session;
        session.registerHeartbeatListener(this);

        session.subscribe(new HeartbeatSubscriptionRequest(), new Callback()
        {
            public void onSuccess() { };

            @Override
            public void onFailure(final FailureResponse failureResponse)
            {
                throw new RuntimeException("Heartbeat subscription failed");
            }
        });

        session.registerOrderBookEventListener(this);

        new Thread(this).start();  // heartbeat request
        session.start();
    }

    @Override
    public void onLoginFailure(FailureResponse failureResponse) {
        System.err.printf("Failed to login, reason: %s%n", failureResponse);
    }

    @Override
    public void notify(OrderBookEvent orderBookEvent) {
        System.out.printf("Market data: %s%n", orderBookEvent);
    }

    private boolean shouldTradeGivenCurrentMarketData(OrderBookEvent orderBookEvent) {
        return false;
    }


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