package lsk.commerce.query;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lsk.commerce.query.dto.MemberQueryDto;
import lsk.commerce.query.dto.OrderProductQueryDto;
import lsk.commerce.query.dto.OrderQueryDto;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.*;

@Repository
@RequiredArgsConstructor
public class MemberQueryRepository {

    private final EntityManager em;

    public List<MemberQueryDto> findAllByDto() {
        List<MemberQueryDto> result = findMembers();
        List<String> loginIds = toMemberLoginIds(result);

        List<OrderQueryDto> orders = findOrdersByLoginIds(loginIds);
        List<String> orderNumbers = toOrderNumbers(orders);

        Map<String, List<OrderProductQueryDto>> orderProductMap = findOrderProductMap(orderNumbers);

        orders.forEach(orderQueryDto -> orderQueryDto.setOrderProducts(orderProductMap.get(orderQueryDto.getOrderNumber())));

        Map<String, List<OrderQueryDto>> orderMap = orders.stream()
                .collect(groupingBy(orderQueryDto -> orderQueryDto.getLoginId()));

        result.forEach(m -> m.setOrders(orderMap.get(m.getLoginId())));

        return result;
    }

    protected Map<String, List<OrderProductQueryDto>> findOrderProductMap(List<String> orderNumbers) {
        List<OrderProductQueryDto> orderProducts = em.createQuery(
                        "select new lsk.commerce.query.dto.OrderProductQueryDto(op.order.orderNumber, prod.name, prod.price, op.count, op.orderPrice)" +
                                " from OrderProduct op" +
                                " join op.product prod" +
                                " where op.order.orderNumber in :orderNumbers", OrderProductQueryDto.class)
                .setParameter("orderNumbers", orderNumbers)
                .getResultList();

        return orderProducts.stream()
                .collect(groupingBy(orderProductQueryDto -> orderProductQueryDto.getOrderNumber()));
    }

    protected static List<String> toOrderNumbers(List<OrderQueryDto> result) {
        return result.stream()
                .map(o -> o.getOrderNumber())
                .collect(toList());
    }

    protected List<OrderQueryDto> findOrdersByLoginIds(List<String> loginIds) {
        return em.createQuery(
                        "select new lsk.commerce.query.dto.OrderQueryDto(o.member.loginId, o.orderNumber, o.totalAmount, o.orderStatus, o.orderDate, pay.paymentStatus, pay.paymentDate, d.deliveryStatus, d.shippedDate, d.deliveredDate)" +
                                " from Order o" +
                                " join o.payment pay" +
                                " join o.delivery d" +
                                " where o.member.loginId in :loginIds", OrderQueryDto.class)
                .setParameter("loginIds", loginIds)
                .getResultList();
    }

    protected List<OrderQueryDto> findOrdersByLoginId(String loginId) {
        return em.createQuery(
                        "select new lsk.commerce.query.dto.OrderQueryDto(o.member.loginId, o.orderNumber, o.totalAmount, o.orderStatus, o.orderDate, pay.paymentStatus, pay.paymentDate, d.deliveryStatus, d.shippedDate, d.deliveredDate)" +
                                " from Order o" +
                                " join o.payment pay" +
                                " join o.delivery d" +
                                " where o.member.loginId = :loginId", OrderQueryDto.class)
                .setParameter("loginId", loginId)
                .getResultList();
    }

    protected static List<String> toMemberLoginIds(List<MemberQueryDto> result) {
        return result.stream()
                .map(m -> m.getLoginId())
                .collect(toList());
    }

    protected List<MemberQueryDto> findMembers() {
        return em.createQuery(
                        "select new lsk.commerce.query.dto.MemberQueryDto(m.loginId, m.grade)" +
                                " from Member m", MemberQueryDto.class)
                .getResultList();
    }

    protected Optional<MemberQueryDto> findMember(String loginId) {
        return em.createQuery(
                        "select new lsk.commerce.query.dto.MemberQueryDto(m.loginId, m.grade)" +
                                " from Member m" +
                                " where m.loginId = :loginId", MemberQueryDto.class)
                .setParameter("loginId", loginId)
                .getResultStream()
                .findFirst();
    }
}
