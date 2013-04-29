package quantisan.qte_lmax;

import com.lmax.api.FixedPointNumber;
import com.lmax.api.TimeInForce;
import com.lmax.api.order.*;
import com.lmax.api.order.Order;
import com.lmax.api.position.PositionEvent;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

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
                return FixedPointNumber.valueOf("1.318190");
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
                        return OrderType.STOP_LOSS_MARKET_ORDER;
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
                        return FixedPointNumber.valueOf("1.318190");
                    }

                    @Override
                    public FixedPointNumber getStopLossOffset() {
                        return FixedPointNumber.valueOf("0.01");
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
                        return FixedPointNumber.valueOf("13.21265");
                    }
                };
            }

            @Override
            public FixedPointNumber getCancelledQuantity() {
                return FixedPointNumber.ZERO;
            }
        };

        assertEquals("{:message-type :execution-event," + " :user-id \"demo\"," +
                " :lmax-order-type \"STOP_LOSS_MARKET_ORDER\", :lmax-order-id \"ABC123\", :order-id \"my_order_1\"," +
                " :original-order-id \"my_order_1_original\", :instrument \"EURUSD\", :fill-price 1318190," +
                " :stop-reference-price 1318190, :stop-offset 10000, :quantity 1000000," +
                " :filled-quantity 1000000, :cancelled-quantity 0, :commission 13212650," +
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
        assertEquals("{:message-type :position-event," +
                " :user-id \"demo\", :instrument \"EURUSD\", :valuation -1339613500," +
                " :short-unfilled-cost 0, :long-unfilled-cost 0, :quantity -10100000," +
                " :cumulative-cost -132279560000, :open-cost -132329190000}",
                EdnMessage.positionEvent(pe));
    }

    @Test
    public void testSafeLongValue() throws Exception {
        assertNull(EdnMessage.safeLongValue(null));
        assertEquals(10000000L, (long)EdnMessage.safeLongValue(FixedPointNumber.TEN));
    }
}
