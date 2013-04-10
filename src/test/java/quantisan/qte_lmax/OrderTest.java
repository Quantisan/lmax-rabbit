package quantisan.qte_lmax;

import com.lmax.api.FixedPointNumber;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OrderTest {
    @Test
    public void testMarketOrderConstructor() throws Exception {
        String message = "{:instrument \"EURUSD\", :order-id \"my_order_1\", :quantity \"0.1\", :stop-loss-offset \"10\", :order-type :market}";
        Order order = new Order(null, message);
        assertEquals(4001, order.getInstrumentId());
        assertEquals("my_order_1", order.getOrderId());
        assertEquals("0.1", order.getQuantity().toString());
        assertEquals("10", order.getStopLossOffset().toString());
    }

    @Test
    public void testAmendOrderConstructor() throws Exception {
        String message = "{:instrument \"EURUSD\", :order-id \"my_order_1\", :stop-loss-offset \"20\", :order-type :amend-stop}";
        Order order = new Order(null, message);
        assertEquals(4001, order.getInstrumentId());
        assertEquals("my_order_1", order.getOrderId());
        assertEquals(FixedPointNumber.ZERO, order.getQuantity());
        assertEquals("20", order.getStopLossOffset().toString());
    }

    @Test
    public void testParseOrderType() throws Exception {
        assertEquals(Order.OrderType.MARKET, Order.parseOrderType("{:order-type :market}"));
        assertEquals(Order.OrderType.AMEND_STOP, Order.parseOrderType("{:order-type :amend-stop}"));
        assertEquals(Order.OrderType.UNKNOWN, Order.parseOrderType("{}"));
    }
}