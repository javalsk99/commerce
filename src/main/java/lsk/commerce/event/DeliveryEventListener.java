package lsk.commerce.event;

import lombok.RequiredArgsConstructor;
import lsk.commerce.service.DeliveryService;
import lsk.commerce.service.OrderService;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class DeliveryEventListener {

    private final TaskScheduler scheduler;
    private final OrderService orderService;
    private final DeliveryService deliveryService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void startDelivery(PaymentCompletedEvent event) {
        scheduler.schedule(() -> deliveryService.startDelivery(event.orderId()), Instant.now().plusSeconds(60));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void completeDelivery(DeliveryStartedEvent event) {
        scheduler.schedule(() -> deliveryService.completeDelivery(event.orderId()), Instant.now().plusSeconds(60));
    }
}
