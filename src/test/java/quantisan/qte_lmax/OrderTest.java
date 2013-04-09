package quantisan.qte_lmax;

import com.lmax.api.FixedPointNumber;
import com.lmax.api.TimeInForce;
import com.lmax.api.order.AmendStopsRequest;
import com.lmax.api.order.MarketOrderSpecification;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OrderTest {
    @Ignore @Test
    public void testToMarketOrder() throws Exception {
        long instrument = 4001;
        String orderId = "my order 1";
        FixedPointNumber quantity = FixedPointNumber.valueOf("1.0");
        FixedPointNumber stopLossOffset = FixedPointNumber.valueOf("10.0");
        MarketOrderSpecification mos = new MarketOrderSpecification(instrument, orderId, quantity, TimeInForce.IMMEDIATE_OR_CANCEL,stopLossOffset, null);
        // {:instrument "EURUSD", :order-id "my_order_1", :quantity "0.1", :stop-loss-offset "10", :order-type :market}
//        assertEquals(mos.toString(),
//                Order.toMarketOrder("{:instrument \"EURUSD\", :order-id \"my_order_1\", :quantity 1, :stop-loss-offset 10}").toString());
    }

    @Ignore @Test
    public void testToAmendStopOrder() throws Exception {
        long instrument = 4001;
        String orderId = "my order 1";
        FixedPointNumber stopLossOffset = FixedPointNumber.valueOf(10);
        AmendStopsRequest request = new AmendStopsRequest(instrument, orderId, orderId, stopLossOffset, null);
//        assertEquals(request,  // BUG equals useless and no toString
//                Order.toAmendStopOrder("{:order-type :amend-stop, :instrument \"EURUSD\", :order-id \"my order 1\", :stop-loss-offset 10}"));
    }

    @Ignore @Test
    public void testParseOrderType() throws Exception {
        assertEquals(Order.OrderType.MARKET, Order.parseOrderType("{:order-type :market}"));
        assertEquals(Order.OrderType.AMEND_STOP, Order.parseOrderType("{:order-type :amend-stop}"));
        assertEquals(Order.OrderType.UNKNOWN, Order.parseOrderType("{}"));
    }
}