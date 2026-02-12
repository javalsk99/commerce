package lsk.commerce.service;

import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Order;
import lsk.commerce.event.DeliveryStartedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@Service
@Transactional(propagation = REQUIRES_NEW)
@RequiredArgsConstructor
public class DeliveryService {

    private final ApplicationEventPublisher eventPublisher;
    private final OrderService orderService;

    public void startDelivery(String orderNumber) {
        Order order = orderService.findOrderWithDelivery(orderNumber);
        order.getDelivery().startDelivery();

        eventPublisher.publishEvent(new DeliveryStartedEvent(orderNumber));
    }

    public void completeDelivery(String orderNumber) {
        Order order = orderService.findOrderWithDelivery(orderNumber);
        order.getDelivery().completeDelivery();
    }
}
