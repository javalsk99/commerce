package lsk.commerce.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Order;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}
