package lsk.commerce.service;

import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Order;
import lsk.commerce.domain.Payment;
import lsk.commerce.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    //이후 결제 진행은 다른 비즈니스 로직 다 생성 후 진행
    public Long request(Order order) {
        Payment.requestPayment(order);
        paymentRepository.save(order.getPayment());
        return order.getPayment().getId();
    }

    public Long request(Payment payment) {
        paymentRepository.save(payment);
        return payment.getId();
    }

    public Payment findPayment(Long paymentId) {
        return paymentRepository.findOne(paymentId);
    }

    public Optional<Payment> findPaymentByPaymentId(String paymentId) {
        return paymentRepository.findByPaymentId(paymentId);
    }
}
