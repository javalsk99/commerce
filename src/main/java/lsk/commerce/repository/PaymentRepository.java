package lsk.commerce.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Payment;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PaymentRepository {

    private final EntityManager em;

    public void save(Payment payment) {
        em.persist(payment);
    }

    public Payment findOne(Long paymentId) {
        return em.find(Payment.class, paymentId);
    }
}
