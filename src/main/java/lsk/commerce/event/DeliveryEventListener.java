package lsk.commerce.event;

import lombok.RequiredArgsConstructor;
import lsk.commerce.service.DeliveryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class DeliveryEventListener {

    private final TaskScheduler scheduler;
    private final DeliveryService deliveryService;

    @Value("${delay}")
    private long delaySeconds;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void startDelivery(PaymentCompletedEvent event) {
        scheduler.schedule(() -> deliveryService.startDelivery(event.orderNumber()), Instant.now().plusSeconds(delaySeconds));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void completeDelivery(DeliveryStartedEvent event) {
        scheduler.schedule(() -> deliveryService.completeDelivery(event.orderNumber()), Instant.now().plusSeconds(delaySeconds));
    }
}
