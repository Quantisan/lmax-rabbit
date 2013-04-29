package quantisan.qte_lmax;

import com.lmax.api.FixedPointNumber;
import com.lmax.api.order.Execution;
import com.lmax.api.position.PositionEvent;
import us.bpsm.edn.printer.Printer;
import us.bpsm.edn.printer.Printers;

import java.io.StringWriter;

public final class EdnMessage {
    private EdnMessage() {}

    private static Boolean isOrderComplete(com.lmax.api.order.Order order)
    {
        Long completedQuantity;
        try {
            completedQuantity= order.getFilledQuantity().longValue() + order.getCancelledQuantity().longValue();
        } catch(NullPointerException e) { return null; }

        return order.getQuantity().longValue() == completedQuantity;
    }

    public static Long safeLongValue(FixedPointNumber x) {
        if (x == null)
            return null;
        else
            return x.longValue();
    }

    public static String ednPrinter(Object obj) {
        StringWriter sw = new StringWriter();
        Printer ew = Printers.newPrinter(Printers.defaultPrinterProtocol(), sw);
        ew.printValue(obj);
        ew.close();
        return sw.toString();
    }

    public static String executionEvent(Execution exe) {
        com.lmax.api.order.Order o = exe.getOrder();
        String lmaxOrderId = o.getOrderId();  // TODO refactor into return
        String lmaxOrderType = o.getOrderType().toString();
        String orderId = o.getInstructionId();
        String originalOrderId = o.getOriginalInstructionId();
        String fillPrice = ednPrinter(safeLongValue(exe.getPrice()));
        String quantity = ednPrinter(safeLongValue(o.getQuantity()));
        String filledQuantity = ednPrinter(safeLongValue(o.getFilledQuantity()));
        String cancelledQuantity = ednPrinter(safeLongValue(o.getCancelledQuantity()));
        String instrument = Instrument.toName(o.getInstrumentId());
        String commission = ednPrinter(safeLongValue(o.getCommission()));
        Boolean complete = isOrderComplete(o);

        return "{:message-type :execution-event" +          // TODO use edn-java printer
                ", :user-id \"" + ThinBot.USER_NAME + "\""
                + ", :lmax-order-type \"" + lmaxOrderType + "\""
                + ", :lmax-order-id \"" + lmaxOrderId + "\""
                + ", :order-id \"" + orderId + "\""
                + ", :original-order-id \"" + originalOrderId + "\""
                + ", :instrument \"" + instrument + "\""
                + ", :fill-price " + fillPrice
                + ", :stop-reference-price " + ednPrinter(safeLongValue(o.getStopReferencePrice()))
                + ", :stop-offset " + ednPrinter(safeLongValue(o.getStopLossOffset()))
                + ", :take-profit-offset " + ednPrinter(safeLongValue(o.getStopProfitOffset()))
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
                ", :valuation " + ednPrinter(safeLongValue(pe.getValuation())) +
                ", :short-unfilled-cost " + ednPrinter(safeLongValue(pe.getShortUnfilledCost())) +
                ", :long-unfilled-cost " + ednPrinter(safeLongValue(pe.getLongUnfilledCost())) +
                ", :quantity " + ednPrinter(safeLongValue(pe.getOpenQuantity())) +
                ", :cumulative-cost " + ednPrinter(safeLongValue(pe.getCumulativeCost())) +
                ", :open-cost " + ednPrinter(safeLongValue(pe.getOpenCost())) + "}";
    }
}
