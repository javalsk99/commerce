package lsk.commerce.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Payment;
import org.springframework.stereotype.Repository;

import java.util.Optional;

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

    public Optional<Payment> findByPaymentId(String paymentId) {
        return em.createQuery("select p from Payment p where p.paymentId = :paymentId", Payment.class)
                .setParameter("paymentId", paymentId)
                .getResultStream()
                .findFirst();
    }

    public Optional<Payment> findWithOrderDelivery(String paymentId) {
        return em.createQuery(
                        "select p from Payment p" +
                                " join fetch p.order o" +
                                " join fetch o.delivery" +
                                " where p.paymentId = :paymentId", Payment.class)
                .setParameter("paymentId", paymentId)
                .getResultStream()
                .findFirst();
    }
}
