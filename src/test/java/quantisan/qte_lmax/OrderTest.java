package quantisan.qte_lmax;

import com.lmax.api.FixedPointNumber;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class OrderTest {
    @Test
    public void testMarketOrderConstructor() throws Exception {
        String message = "{:instrument :eurusd, :order-id \"my_order_1\", :quantity 0.1, :stop-loss-offset 10, :take-profit-offset 20, :order-type :market}";
        Order order = new Order(null, null, message);
        assertEquals(4001, order.getInstrumentId());
        assertEquals("my_order_1", order.getOrderId());
        assertEquals("0.1", order.getQuantity().toString());
        assertEquals("0.00001", order.getStopLossOffset().toString());
        assertEquals("0.00002", order.getTakeProfitOffset().toString());
    }

    @Test
    public void testAmendOrderConstructor() throws Exception {
        String message = "{:instrument :eurusd, :order-id \"my_order_1\", :stop-loss-offset 20, :take-profit-offset 30, :order-type :amend-stop}";
        Order order = new Order(null, null, message);
        assertEquals(4001, order.getInstrumentId());
        assertEquals("my_order_1", order.getOrderId());
        assertEquals(FixedPointNumber.ZERO, order.getQuantity());
        assertEquals("0.00002", order.getStopLossOffset().toString());
        assertEquals("0.00003", order.getTakeProfitOffset().toString());
    }

    @Test
    public void testParseOrderType() throws Exception {
        assertEquals(Order.OrderType.MARKET, Order.parseOrderType("{:order-type :market}"));
        assertEquals(Order.OrderType.AMEND_STOP, Order.parseOrderType("{:order-type :amend-stop}"));
        assertEquals(Order.OrderType.UNKNOWN, Order.parseOrderType("{}"));
    }

    @Test @Ignore // not done yet
    public void testExecute() throws Exception {
        String message = "{:instrument :eurusd, :order-id 1892131401901743, :quantity 0.1, :stop-loss-offset 28, :order-type :market}";
        Order order = new Order(null, null, message);
        order.execute();
    }

}