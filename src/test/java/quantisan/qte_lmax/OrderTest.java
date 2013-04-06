package quantisan.qte_lmax;

import com.lmax.api.FixedPointNumber;
import com.lmax.api.TimeInForce;
import com.lmax.api.order.MarketOrderSpecification;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OrderTest {
    @Test
    public void testToMarketOrder() throws Exception {
        long instrument = 4001;
        String orderId = "my order 1";
        FixedPointNumber quantity = FixedPointNumber.valueOf(1);
        FixedPointNumber stopLossOffset = FixedPointNumber.valueOf(10);
        MarketOrderSpecification mos = new MarketOrderSpecification(instrument, orderId, quantity, TimeInForce.IMMEDIATE_OR_CANCEL,stopLossOffset, null);

        assertEquals(mos.toString(),
                Order.toMarketOrder("{:instrument \"EURUSD\", :order-id \"my order 1\", :quantity 1, :stop-loss-offset 10}").toString());
    }
}