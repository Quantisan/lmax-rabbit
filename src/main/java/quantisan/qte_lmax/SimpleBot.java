package quantisan.qte_lmax;

import com.lmax.api.FailureResponse;
import com.lmax.api.LmaxApi;
import com.lmax.api.Session;
import com.lmax.api.account.LoginCallback;
import com.lmax.api.account.LoginRequest;

public class SimpleBot implements LoginCallback {

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

        this.session = session; // Capture the session for use later.
    }

    @Override
    public void onLoginFailure(FailureResponse failureResponse) {
        System.err.printf("Failed to login, reason: %s%n", failureResponse);
    }
}