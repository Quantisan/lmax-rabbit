package quantisan.qte_lmax;

import com.lmax.api.FixedPointNumber;
import com.lmax.api.order.Execution;
import com.lmax.api.position.PositionEvent;

public final class EdnMessage {
    private EdnMessage() {}

    private static boolean isOrderComplete(com.lmax.api.order.Order order)
    {
        long completedQuantity = order.getFilledQuantity().longValue() + order.getCancelledQuantity().longValue();
        return order.getQuantity().longValue() == completedQuantity;
    }

    public static Long safeLongValue(FixedPointNumber x) {
        if (x == null)
            return null;
        else
            return x.longValue();
    }

    public static String executionEvent(Execution exe) {
        com.lmax.api.order.Order o = exe.getOrder();
        String lmaxOrderId = o.getOrderId();  // TODO refactor into return
        String lmaxOrderType = o.getOrderType().toString();
        String orderId = o.getInstructionId();
        String originalOrderId = o.getOriginalInstructionId();
        Long fillPrice = exe.getPrice().longValue();
        Long quantity = o.getQuantity().longValue();
        Long filledQuantity = o.getFilledQuantity().longValue();
        Long cancelledQuantity = o.getCancelledQuantity().longValue();
        String instrument = Instrument.toName(o.getInstrumentId());
        Long commission = o.getCommission().longValue();
        boolean complete = isOrderComplete(o);

        return "{:message-type :execution-event" +
                ", :user-id \"" + ThinBot.USER_NAME + "\""
                + ", :lmax-order-type \"" + lmaxOrderType + "\""
                + ", :lmax-order-id \"" + lmaxOrderId + "\""
                + ", :order-id \"" + orderId + "\""
                + ", :original-order-id \"" + originalOrderId + "\""
                + ", :instrument \"" + instrument + "\""
                + ", :fill-price " + fillPrice
                + ", :stop-reference-price " + safeLongValue(o.getStopReferencePrice())
                + ", :stop-offset " + safeLongValue(o.getStopLossOffset())
                + ", :take-profit-offset" + safeLongValue(o.getStopProfitOffset())
                + ", :quantity " + quantity
                + ", :filled-quantity " + filledQuantity
                + ", :cancelled-quantity " + cancelledQuantity
                + ", :commission " + commission
                + ", :completed? " + complete + "}";
    }

    public static String positionEvent(PositionEvent pe) {
        return "{:message-type :position-event" +
                ", :user-id \"" + ThinBot.USER_NAME + "\"" +
                ", :instrument \"" + Instrument.toName(pe.getInstrumentId()) + "\"" +
                ", :valuation " + safeLongValue(pe.getValuation()) +
                ", :short-unfilled-cost " + safeLongValue(pe.getShortUnfilledCost()) +
                ", :long-unfilled-cost " + safeLongValue(pe.getLongUnfilledCost()) +
                ", :quantity " + safeLongValue(pe.getOpenQuantity()) +
                ", :cumulative-cost " + safeLongValue(pe.getCumulativeCost()) +
                ", :open-cost " + safeLongValue(pe.getOpenCost()) + "}";
    }
}
