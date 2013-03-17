package quantisan.qte_lmax;

import com.lmax.api.Callback;
import com.lmax.api.FailureResponse;
import com.lmax.api.LmaxApi;
import com.lmax.api.Session;
import com.lmax.api.account.LoginCallback;
import com.lmax.api.account.LoginRequest;
import com.lmax.api.orderbook.OrderBookEvent;
import com.lmax.api.orderbook.OrderBookEventListener;
import com.lmax.api.orderbook.OrderBookSubscriptionRequest;

public class SimpleBot implements LoginCallback, OrderBookEventListener {

    private final static long GBP_USD_INSTRUMENT_ID = 4001;
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
        session.registerOrderBookEventListener(this);
        session.subscribe(new OrderBookSubscriptionRequest(GBP_USD_INSTRUMENT_ID), new Callback()
        {
            public void onSuccess()
            {
                System.out.println("Successful subscription");
            }

            public void onFailure(FailureResponse failureResponse)
            {
                System.err.printf("Failed to subscribe: %s%n", failureResponse);
            }
        });

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
}