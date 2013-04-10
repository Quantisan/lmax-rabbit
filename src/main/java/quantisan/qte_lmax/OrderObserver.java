package quantisan.qte_lmax;

import com.lmax.api.Session;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class OrderObserver implements Runnable {
    final static Logger logger = LoggerFactory.getLogger(OrderObserver.class);
    private final Channel channelOrderReceiver;
    private final QueueingConsumer orderConsumer;
    private final Session session;
    private final Channel channelAccountProducer;

    public OrderObserver(Session session, Channel channelOrderReceiver, Channel channelAccountProducer, QueueingConsumer orderConsumer) {
        this.session = session;
        this.channelOrderReceiver = channelOrderReceiver;
        this.channelAccountProducer = channelAccountProducer;
        this.orderConsumer = orderConsumer;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            QueueingConsumer.Delivery delivery = null;
            try {
                delivery = orderConsumer.nextDelivery();
            } catch (InterruptedException e) {
                logger.error("Interrupted before order delivery.", e);
            }
            Order order = new Order(session, channelAccountProducer, new String(delivery.getBody()));
            order.execute();
            try {
                channelOrderReceiver.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (IOException e) {
                logger.error("Fail to acknowledge order message.", e);
            }
        }
    }
}
