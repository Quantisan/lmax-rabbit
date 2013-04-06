package quantisan.qte_lmax;

import com.lmax.api.FailureResponse;
import com.lmax.api.FixedPointNumber;
import com.lmax.api.Session;
import com.lmax.api.TimeInForce;
import com.lmax.api.order.MarketOrderSpecification;
import com.lmax.api.order.OrderCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.bpsm.edn.parser.Parseable;
import us.bpsm.edn.parser.Parser;
import us.bpsm.edn.parser.Parsers;

import java.util.Map;

import static us.bpsm.edn.Keyword.newKeyword;
import static us.bpsm.edn.parser.Parsers.defaultConfiguration;

public class Order {
    final static Logger logger = LoggerFactory.getLogger(Order.class);

    private final Session session;
    private final MarketOrderSpecification marketOrderSpecification;

    public enum OrderState { NONE, FAIL, PENDING };
    private OrderState orderState = OrderState.NONE;

    /**
     * Parse a EDN message to a market order.
     *
     * @param ednMessage a EDN message
     * @return a market order
     */
    public static MarketOrderSpecification toMarketOrder(String ednMessage) {
        Parseable pbr = Parsers.newParseable(ednMessage);
        Parser p = Parsers.newParser(defaultConfiguration());
        Map<?, ?> m = (Map<?, ?>) p.nextValue(pbr);
        String orderId = m.get(newKeyword("order-id")).toString();
        Long instrument = Instrument.toId(m.get(newKeyword("instrument")).toString());
        FixedPointNumber quantity = FixedPointNumber.valueOf((Long)m.get(newKeyword("quantity")));

        FixedPointNumber stopLossOffset = FixedPointNumber.valueOf((Long)m.get(newKeyword("stop-loss-offset")));

        return new MarketOrderSpecification(instrument, orderId, quantity, TimeInForce.IMMEDIATE_OR_CANCEL,stopLossOffset, null);
    }

    public Order(Session session, String message) {
        this.session = session;
        this.marketOrderSpecification = toMarketOrder(message);
    }

    public void execute() {
        if (getOrderState() == OrderState.NONE) {
            session.placeMarketOrder(this.marketOrderSpecification, new OrderCallback()
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
        }
    }

    public MarketOrderSpecification getMarketOrderSpecification() {
        return marketOrderSpecification;
    }

    public OrderState getOrderState() {
        return orderState;
    }

    public void setOrderState(OrderState orderState) {
        this.orderState = orderState;
    }
}
