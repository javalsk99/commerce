package lsk.commerce.query;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lsk.commerce.query.dto.OrderQueryDto;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Repository
@RequiredArgsConstructor
public class OrderQueryRepository {

    private final EntityManager em;

    protected static List<String> toOrderNumbers(List<OrderQueryDto> result) {
        return result.stream()
                .map(o -> o.getOrderNumber())
                .collect(toList());
    }

    protected List<OrderQueryDto> findOrdersByLoginIds(List<String> loginIds) {
        return em.createQuery(
                        "select new lsk.commerce.query.dto.OrderQueryDto(o.member.loginId, o.orderNumber, o.totalAmount, o.orderStatus, o.orderDate, pay.paymentStatus, pay.paymentDate, d.deliveryStatus, d.shippedDate, d.deliveredDate)" +
                                " from Order o" +
                                " left join o.payment pay" +
                                " join o.delivery d" +
                                " where o.member.loginId in :loginIds", OrderQueryDto.class)
                .setParameter("loginIds", loginIds)
                .getResultList();
    }

    protected List<OrderQueryDto> findOrdersByLoginId(String loginId) {
        return em.createQuery(
                        "select new lsk.commerce.query.dto.OrderQueryDto(o.member.loginId, o.orderNumber, o.totalAmount, o.orderStatus, o.orderDate, pay.paymentStatus, pay.paymentDate, d.deliveryStatus, d.shippedDate, d.deliveredDate)" +
                                " from Order o" +
                                " left join o.payment pay" +
                                " join o.delivery d" +
                                " where o.member.loginId = :loginId", OrderQueryDto.class)
                .setParameter("loginId", loginId)
                .getResultList();
    }

    protected Optional<OrderQueryDto> findOrderByOrderNumber(String orderNumber) {
        return em.createQuery(
                        "select new lsk.commerce.query.dto.OrderQueryDto(o.orderNumber, o.totalAmount, o.orderStatus, o.orderDate, pay.paymentStatus, pay.paymentDate, d.deliveryStatus, d.shippedDate, d.deliveredDate)" +
                                " from Order o" +
                                " left join o.payment pay" +
                                " join o.delivery d" +
                                " where o.orderNumber = :orderNumber", OrderQueryDto.class)
                .setParameter("orderNumber", orderNumber)
                .getResultStream()
                .findFirst();
    }
}
