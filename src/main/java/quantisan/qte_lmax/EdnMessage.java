package quantisan.qte_lmax;

import com.lmax.api.order.Execution;
import com.lmax.api.position.PositionEvent;

public final class EdnMessage {
    private EdnMessage() {};

    private static boolean isOrderComplete(com.lmax.api.order.Order order)
    {
        long completedQuantity = order.getFilledQuantity().longValue() + order.getCancelledQuantity().longValue();
        return order.getQuantity().longValue() == completedQuantity;
    }

    public static String executionEvent(Execution exe) {
        com.lmax.api.order.Order order = exe.getOrder();
        String lmaxOrderId = order.getOrderId();  // TODO refactor into return
        String lmaxOrderType = order.getOrderType().toString();
        String orderId = order.getInstructionId();
        String originalOrderId = order.getOriginalInstructionId();
        String fillPrice = exe.getPrice().toString();
        String quantity = order.getQuantity().toString();
        String filledQuantity = order.getFilledQuantity().toString();
        String cancelledQuantity = order.getCancelledQuantity().toString();
        String instrument = Instrument.toName(order.getInstrumentId());
        String commission = order.getCommission().toString();
        boolean complete = isOrderComplete(order);

        return "{:message-type :execution-event" +
                ", :user-id \"" + ThinBot.USER_NAME + "\""
                + ", :lmax-order-type \"" + lmaxOrderType + "\""
                + ", :lmax-order-id \"" + lmaxOrderId + "\""
                + ", :order-id \"" + orderId + "\""
                + ", :original-order-id \"" + originalOrderId + "\""
                + ", :fill-price \"" + fillPrice + "\""
                + ", :quantity \"" + quantity + "\""
                + ", :filled-quantity \"" + filledQuantity + "\""
                + ", :cancelled-quantity \"" + cancelledQuantity + "\""
                + ", :instrument \"" + instrument + "\""
                + ", :commission \"" + commission + "\""
                + ", :completed? " + complete + "}";
    }

    public static String positionEvent(PositionEvent pe) {
        return "{:message-type :position-event" +
                ", :user-id \"" + ThinBot.USER_NAME + "\"" +
                ", :instrument \"" + Instrument.toName(pe.getInstrumentId()) + "\"" +
                ", :valuation \"" + pe.getValuation() + "\"" +
                ", :short-unfilled-cost \"" + pe.getShortUnfilledCost() + "\"" +
                ", :long-unfilled-cost \"" + pe.getLongUnfilledCost() + "\"" +
                ", :quantity \"" + pe.getOpenQuantity() + "\"" +
                ", :cumulative-cost \"" + pe.getCumulativeCost() + "\"" +
                ", :open-cost \"" + pe.getOpenCost() + "\"}";
    }
}
