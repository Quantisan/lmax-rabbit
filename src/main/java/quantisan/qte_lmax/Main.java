package quantisan.qte_lmax;

import com.lmax.api.LmaxApi;

public class Main {

    public static void main(String[] args) {
        String url = "https://testapi.lmaxtrader.com";
        LmaxApi lmaxApi = new LmaxApi(url);
    }
}