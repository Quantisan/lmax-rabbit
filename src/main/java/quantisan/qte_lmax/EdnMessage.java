package quantisan.qte_lmax;

import com.lmax.api.order.Execution;

public final class EdnMessage {
    private EdnMessage() {};

    private static boolean isOrderComplete(com.lmax.api.order.Order order)
    {
        long completedQuantity = order.getFilledQuantity().longValue() + order.getCancelledQuantity().longValue();
        return order.getQuantity().longValue() == completedQuantity;
    }

    public static String executionEvent(Execution exe) {
        com.lmax.api.order.Order order = exe.getOrder();
        String fillPrice = exe.getPrice().toString();
        String quantity = order.getQuantity().toString();
        String filledQuantity = order.getFilledQuantity().toString();
        String cancelledQuantity = order.getCancelledQuantity().toString();
        String lmaxOrderId = order.getOrderId();
        String orderId = order.getInstructionId();
        String originalOrderId = order.getOriginalInstructionId();
        long instrumentId = order.getInstrumentId();
        boolean complete = isOrderComplete(order);

        String message = "";
        return message;
    }
}
