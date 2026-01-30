package lsk.commerce.service;

import lombok.RequiredArgsConstructor;
import lsk.commerce.api.portone.CompletePaymentRequest;
import lsk.commerce.domain.Order;
import lsk.commerce.domain.Payment;
import lsk.commerce.event.PaymentCompletedEvent;
import lsk.commerce.repository.PaymentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;

    //이후 결제 진행은 다른 비즈니스 로직 다 생성 후 진행
    public Long request(Order order) {
        Payment.requestPayment(order);
        CompletePaymentRequest completePaymentRequest = new CompletePaymentRequest(order.getPayment().getPaymentId());
        paymentRepository.save(order.getPayment());
        return order.getPayment().getId();
    }

    @Transactional(readOnly = true)
    public Payment findPayment(Long paymentId) {
        return paymentRepository.findOne(paymentId);
    }

    @Transactional(readOnly = true)
    public Payment findPaymentByPaymentId(String paymentId) {
        return paymentRepository.findByPaymentId(paymentId);
    }

    public Payment failedPayment(String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId);
        payment.failed();
        return payment;
    }

    public Payment completePayment(String paymentId, LocalDateTime paymentDate) {
        Payment payment = paymentRepository.findByPaymentId(paymentId);
        payment.complete(paymentDate);
        payment.getOrder().completePaid();

        String orderNumber = payment.getOrder().getOrderNumber();
        eventPublisher.publishEvent(new PaymentCompletedEvent(orderNumber));

        return payment;
    }
}
