package quantisan.qte_lmax;

import com.lmax.api.*;
import com.lmax.api.order.AmendStopsRequest;
import com.lmax.api.order.MarketOrderSpecification;
import com.lmax.api.order.OrderCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.bpsm.edn.Keyword;
import us.bpsm.edn.parser.Parseable;
import us.bpsm.edn.parser.Parser;
import us.bpsm.edn.parser.Parsers;

import java.util.Map;

import static us.bpsm.edn.Keyword.newKeyword;
import static us.bpsm.edn.parser.Parsers.defaultConfiguration;

public class Order {
    final static Logger logger = LoggerFactory.getLogger(Order.class);

    private final Session session;
    private final String orderId;
    private final long instrument;
    private final FixedPointNumber quantity;
    private final FixedPointNumber stopLossOffset;

    private enum OrderState { NONE, FAIL, PENDING }
    private OrderState orderState = OrderState.NONE;
    protected enum OrderType { MARKET, AMEND_STOP, CANCEL, UNKNOWN }
    private final OrderType orderType;

    public Order(Session session, String message) {
        this.session = session;
        orderType = parseOrderType(message);

        Parseable pbr = Parsers.newParseable(message);
        Parser p = Parsers.newParser(defaultConfiguration());
        Map<?, ?> m = (Map<?, ?>) p.nextValue(pbr);
        orderId = m.get(newKeyword("order-id")).toString(); // TODO log error if order-id contains space
        instrument = Instrument.toId(m.get(newKeyword("instrument")).toString());
        quantity = FixedPointNumber.valueOf(m.get(newKeyword("quantity")).toString());
        stopLossOffset = FixedPointNumber.valueOf(m.get(newKeyword("stop-loss-offset")).toString());
    }

    protected static OrderType parseOrderType(String ednMessage) {
        Parseable pbr = Parsers.newParseable(ednMessage);
        Parser p = Parsers.newParser(defaultConfiguration());
        Map<?, ?> m = (Map<?, ?>) p.nextValue(pbr);
        Keyword keyword = (Keyword)m.get(newKeyword("order-type"));
        if (keyword == null)
            return OrderType.UNKNOWN;
        else if (keyword.equals(newKeyword("market")))
            return OrderType.MARKET;
        else if (keyword.equals(newKeyword("amend-stop")))
            return OrderType.AMEND_STOP;
        else if (keyword.equals(newKeyword("cancel")))
            return OrderType.CANCEL;
        else
            return OrderType.UNKNOWN;
    }

    public void execute() {
        if (getOrderState() == OrderState.NONE && getOrderType() == OrderType.MARKET) {
            session.placeMarketOrder(new MarketOrderSpecification(instrument, orderId, quantity, TimeInForce.IMMEDIATE_OR_CANCEL, stopLossOffset, null),
                    new OrderCallback()
            {
                public void onSuccess(String placeOrderInstructionId)
                {
                    // note - this will be the same instructionId from above,
                    // it confirms this success is related to that specific place order request

                    //move from "new" to "pending" to show the order was successfully placed
                    logger.info("Order sent and pending: {}", placeOrderInstructionId);
                    setOrderState(OrderState.PENDING);
                }

                public void onFailure(FailureResponse failureResponse)
                {
                    setOrderState(OrderState.FAIL);

                    if (!failureResponse.isSystemFailure())
                    {
                        logger.error("Order data error - Message: {}, Description: {}",
                                failureResponse.getMessage(),
                                failureResponse.getDescription());
                    }
                    else
                    {
                        Exception e = failureResponse.getException();
                        if (e != null)
                        {
                            logger.error("Order exception raised - ", e);
                        }
                        else
                        {
                            logger.error("Order system error - Message: {}, Description: {}",
                                    failureResponse.getMessage(),
                                    failureResponse.getDescription());
                        }
                    }
                }

            });
        } else if (getOrderType() == OrderType.AMEND_STOP) {
            session.amendStops(new AmendStopsRequest(instrument, orderId, orderId, stopLossOffset, null), new OrderCallback()
            {
                public void onSuccess(String amendRequestInstructionId)
                {
                    logger.info("Order amended stop: {}", amendRequestInstructionId);
                }

                public void onFailure(FailureResponse failureResponse)
                {
                    setOrderState(OrderState.FAIL);

                    if (!failureResponse.isSystemFailure())
                    {
                        logger.error("Order stop amend data error - Message: {}, Description: {}",
                                failureResponse.getMessage(),
                                failureResponse.getDescription());
                    }
                    else
                    {
                        Exception e = failureResponse.getException();
                        if (e != null)
                        {
                            logger.error("Order stop amend exception raised - ", e);
                        }
                        else
                        {
                            logger.error("Order stop amend system error - Message: {}, Description: {}",
                                    failureResponse.getMessage(),
                                    failureResponse.getDescription());
                        }
                    }
                }
            });
        }
    }

    public OrderState getOrderState() {
        return orderState;
    }

    public void setOrderState(OrderState orderState) {
        this.orderState = orderState;
    }

    public OrderType getOrderType() {
        return orderType;
    }
}
