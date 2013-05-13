package quantisan.qte_lmax;

import com.lmax.api.*;
import com.lmax.api.order.AmendStopsRequest;
import com.lmax.api.order.MarketOrderSpecification;
import com.lmax.api.order.OrderCallback;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.bpsm.edn.Keyword;
import us.bpsm.edn.parser.Parseable;
import us.bpsm.edn.parser.Parser;
import us.bpsm.edn.parser.Parsers;

import java.io.IOException;
import java.util.Map;

import static us.bpsm.edn.Keyword.newKeyword;
import static us.bpsm.edn.parser.Parsers.defaultConfiguration;

public class Order {
    final static Logger logger = LoggerFactory.getLogger(Order.class);

    private final Session session;
    private final String orderId;
    private final long instrumentId;
    private final FixedPointNumber quantity;
    private final FixedPointNumber stopLossOffset;
    private final FixedPointNumber takeProfitOffset;
    private final Channel channel;

    private enum OrderState { NONE, FAIL, PENDING }
    private OrderState orderState = OrderState.NONE;
    protected enum OrderType { MARKET, AMEND_STOP, CANCEL, UNKNOWN }
    private final OrderType orderType;

    public Order(Session session, Channel channelAccountProducer, String message) {
        this.session = session;
        this.channel = channelAccountProducer;
        this.orderType = parseOrderType(message);

        Parseable pbr = Parsers.newParseable(message);
        Parser p = Parsers.newParser(defaultConfiguration());
        Map<?, ?> m = (Map<?, ?>) p.nextValue(pbr);
        orderId = m.get(newKeyword("order-id")).toString(); // TODO log error if order-id contains space
        Keyword instrument = (Keyword)m.get(newKeyword("instrument"));
        instrumentId = Instrument.toId(instrument.getName());     // TODO handle possible null val
        stopLossOffset = FixedPointNumber.valueOf((Long)m.get(newKeyword("stop-loss-offset")));
        takeProfitOffset = FixedPointNumber.valueOf((Long)m.get(newKeyword("take-profit-offset")));

        Object buffer = m.get(newKeyword("quantity"));      // quantity is null for amend order
        if (buffer != null)
            quantity = FixedPointNumber.valueOf(buffer.toString());
        else
            quantity = FixedPointNumber.ZERO;
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
            logger.info("Placing market orderId: {}, instrumentId: {}, quantity: {}, stopLossOffset: {}, takeProfitOffset: {}.", orderId, instrumentId, quantity, stopLossOffset, takeProfitOffset);
            session.placeMarketOrder(new MarketOrderSpecification(instrumentId, orderId, quantity, TimeInForce.IMMEDIATE_OR_CANCEL, stopLossOffset, takeProfitOffset),
                    new OrderCallback() {       // TODO use same ordercallback impl class
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
                            String message = "{:message-type :order-failed"     // TODO wire into qte-transcript
                                    + ", :order-id \"" + getOrderId() + "\""
                                    + ", :instrument " + getInstrumentId()
                                    + ", :reason " + failureResponse.getMessage() + "}";
                            try {
                                channel.basicPublish("", ThinBot.ACCOUNTING_EXCHANGE_NAME, null, message.getBytes());
                            } catch (IOException e) {
                                logger.error("Account message publish error.", e);
                            }

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
            logger.info("Amending orderId: {}, instrumentId: {}, stopLossOffset: {}, takeProfitOffset.", orderId, instrumentId, stopLossOffset, takeProfitOffset);
            session.amendStops(new AmendStopsRequest(instrumentId, orderId, orderId, stopLossOffset, takeProfitOffset), new OrderCallback()
            {
                public void onSuccess(String amendRequestInstructionId)
                {
                    logger.info("Order amended stop: {}", amendRequestInstructionId);
                }

                public void onFailure(FailureResponse failureResponse)
                {
                    setOrderState(OrderState.FAIL);
                    String message = "{:message-type :order-failed"
                            + ", :order-id \"" + getOrderId() + "\""
                            + ", :instrument " + getInstrumentId()
                            + ", :reason " + failureResponse.getMessage() + "}";
                    try {
                        channel.basicPublish("", ThinBot.ACCOUNTING_EXCHANGE_NAME, null, message.getBytes());
                    } catch (IOException e) {
                        logger.error("Account message publish error.", e);
                    }

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

    public String getOrderId() {
        return orderId;
    }

    public long getInstrumentId() {
        return instrumentId;
    }

    public FixedPointNumber getQuantity() {
        return quantity;
    }

    public FixedPointNumber getStopLossOffset() {
        return stopLossOffset;
    }

    public FixedPointNumber getTakeProfitOffset() {
        return takeProfitOffset;
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
