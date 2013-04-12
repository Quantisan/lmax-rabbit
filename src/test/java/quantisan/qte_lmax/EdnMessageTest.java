package quantisan.qte_lmax;

import com.lmax.api.FixedPointNumber;
import com.lmax.api.TimeInForce;
import com.lmax.api.order.*;
import com.lmax.api.order.Order;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class EdnMessageTest {
    @Test
    public void testExecutionEvent() throws Exception {
        Execution exec = new Execution() {
            @Override
            public long getExecutionId() {
                return 100;
            }

            @Override
            public FixedPointNumber getPrice() {
                return FixedPointNumber.valueOf("1.300");
            }

            @Override
            public FixedPointNumber getQuantity() {
                return FixedPointNumber.valueOf("1.0");
            }

            @Override
            public Order getOrder() {
                return new Order() {
                    @Override
                    public String getInstructionId() {
                        return "my_order_1";
                    }

                    @Override
                    public String getOriginalInstructionId() {
                        return "my_order_1_original";
                    }

                    @Override
                    public String getOrderId() {
                        return "ABC123";
                    }

                    @Override
                    public long getInstrumentId() {
                        return 4001;
                    }

                    @Override
                    public long getAccountId() {
                        return 12345;
                    }

                    @Override
                    public OrderType getOrderType() {
                        return OrderType.STOP_PROFIT_ORDER;
                    }

                    @Override
                    public TimeInForce getTimeInForce() {
                        return TimeInForce.IMMEDIATE_OR_CANCEL;
                    }

                    @Override
                    public FixedPointNumber getQuantity() {
                        return FixedPointNumber.valueOf("1.0");
                    }

                    @Override
                    public FixedPointNumber getFilledQuantity() {
                        return FixedPointNumber.valueOf("1.0");
                    }

                    @Override
                    public FixedPointNumber getLimitPrice() {
                        return null;
                    }

                    @Override
                    public FixedPointNumber getStopReferencePrice() {
                        return null;
                    }

                    @Override
                    public FixedPointNumber getStopLossOffset() {
                        return null;
                    }

                    @Override
                    public FixedPointNumber getStopProfitOffset() {
                        return null;
                    }

                    @Override
                    public FixedPointNumber getCancelledQuantity() {
                        return FixedPointNumber.ZERO;
                    }

                    @Override
                    public FixedPointNumber getCommission() {
                        return FixedPointNumber.valueOf("0.030");
                    }
                };
            }

            @Override
            public FixedPointNumber getCancelledQuantity() {
                return FixedPointNumber.ZERO;
            }
        };

        assertEquals("{:lmax-order-type \"STOP_PROFIT_ORDER\", :lmax-order-id \"ABC123\", :order-id \"my_order_1\"," +
                " :original-order-id \"my_order_1_original\", :fill-price \"1.3\", :quantity \"1\"," +
                " :filled-quantity \"1\", :cancelled-quantity \"0\", :instrument \"EURUSD\", :commission \"0.03\"," +
                " :completed? true}",
                EdnMessage.executionEvent(exec));
    }
}
