package lsk.commerce.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Order;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OrderRepository {

    private final EntityManager em;

    public void save(Order order) {
        em.persist(order);
    }

    public Order findOne(Long orderId) {
        return em.find(Order.class, orderId);
    }

    public Order findByOrderNumber(String orderNumber) {
        return em.createQuery("select o from Order o where o.orderNumber = :orderNumber", Order.class)
                .setParameter("orderNumber", orderNumber)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    public void delete(Order order) {
        em.remove(order);
    }
}
