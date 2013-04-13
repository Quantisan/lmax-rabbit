package quantisan.qte_lmax;

import com.lmax.api.FixedPointNumber;
import com.lmax.api.TimeInForce;
import com.lmax.api.order.*;
import com.lmax.api.order.Order;
import com.lmax.api.position.PositionEvent;
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

    @Test
    public void testPositionEvent() throws Exception {
        PositionEvent pe = new PositionEvent() {
            @Override
            public long getAccountId() {
                return 1327636348;
            }

            @Override
            public long getInstrumentId() {
                return 4001;
            }

            @Override
            public FixedPointNumber getValuation() {
                return FixedPointNumber.valueOf("-1339.6135");
            }

            @Override
            public FixedPointNumber getShortUnfilledCost() {
                return FixedPointNumber.ZERO;
            }

            @Override
            public FixedPointNumber getLongUnfilledCost() {
                return FixedPointNumber.ZERO;
            }

            @Override
            public FixedPointNumber getOpenQuantity() {
                return FixedPointNumber.valueOf("-10.1");
            }

            @Override
            public FixedPointNumber getCumulativeCost() {
                return FixedPointNumber.valueOf("-132279.56");
            }

            @Override
            public FixedPointNumber getOpenCost() {
                return FixedPointNumber.valueOf("-132329.19");
            }
        };
        assertEquals("{:account-id 1327636348, :instrument 4001, :valuation \"-1339.6135\"," +
                " :short-unfilled-cost \"0\", :long-unfilled-cost \"0\", :quantity \"-10.1\", " +
                ":cumulative-cost \"-132279.56\", :open-cost \"-132329.19\"}",
                EdnMessage.positionEvent(pe));
    }
}
