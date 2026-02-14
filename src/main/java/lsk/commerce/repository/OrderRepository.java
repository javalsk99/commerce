package lsk.commerce.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Order;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrderRepository {

    private final EntityManager em;

    public void save(Order order) {
        em.persist(order);
    }

    public Optional<Order> findWithDelivery(String orderNumber) {
        return em.createQuery(
                        "select o from Order o" +
                                " join fetch o.delivery" +
                                " where o.orderNumber = :orderNumber", Order.class)
                .setParameter("orderNumber", orderNumber)
                .getResultStream()
                .findFirst();
    }

    public Optional<Order> findWithDeliveryPayment(String orderNumber) {
        return em.createQuery(
                        "select o from Order o" +
                                " join fetch o.delivery" +
                                " left join fetch o.payment" +
                                " where o.orderNumber = :orderNumber", Order.class)
                .setParameter("orderNumber", orderNumber)
                .getResultStream()
                .findFirst();
    }

    public Optional<Order> findWithAllExceptMember(String orderNumber) {
        return em.createQuery(
                        "select o from Order o" +
                                " join fetch o.delivery" +
                                " left join fetch o.payment" +
                                " left join fetch o.orderProducts op" +
                                " join fetch op.product" +
                                " where o.orderNumber = :orderNumber", Order.class)
                .setParameter("orderNumber", orderNumber)
                .getResultStream()
                .findFirst();
    }

    public Optional<Order> findWithAll(String orderNumber) {
        return em.createQuery(
                        "select o from Order o" +
                                " join fetch o.delivery" +
                                " left join fetch o.payment" +
                                " left join fetch o.orderProducts op" +
                                " join fetch op.product" +
                                " join fetch o.member" +
                                " where o.orderNumber = :orderNumber", Order.class)
                .setParameter("orderNumber", orderNumber)
                .getResultStream()
                .findFirst();
    }

    public void delete(Order order) {
        em.remove(order);
    }
}
