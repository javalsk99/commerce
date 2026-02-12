package lsk.commerce.query;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lsk.commerce.domain.DeliveryStatus;
import lsk.commerce.domain.OrderStatus;
import lsk.commerce.domain.PaymentStatus;
import lsk.commerce.domain.QOrderProduct;
import lsk.commerce.query.dto.OrderProductQueryDto;
import lsk.commerce.query.dto.OrderQueryDto;
import lsk.commerce.query.dto.OrderSearchCond;
import lsk.commerce.query.dto.QOrderQueryDto;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static lsk.commerce.domain.QDelivery.delivery;
import static lsk.commerce.domain.QMember.member;
import static lsk.commerce.domain.QOrder.order;
import static lsk.commerce.domain.QOrderProduct.orderProduct;
import static lsk.commerce.domain.QPayment.payment;
import static lsk.commerce.domain.QProduct.product;

@Repository
public class OrderQueryRepository {

    private final EntityManager em;
    private final JPAQueryFactory query;

    public OrderQueryRepository(EntityManager em) {
        this.em = em;
        this.query = new JPAQueryFactory(em);
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

    protected List<OrderQueryDto> search(OrderSearchCond cond) {
        return query.select(new QOrderQueryDto(order.member.loginId, order.orderNumber, order.totalAmount, order.orderStatus, order.orderDate, order.payment.paymentStatus,
                        order.payment.paymentDate, order.delivery.deliveryStatus, order.delivery.shippedDate, order.delivery.deliveredDate))
                .from(order)
                .join(order.member, member)
                .leftJoin(order.payment, payment)
                .join(order.delivery, delivery)
                .where(
                        eqMemberLoginId(cond.getMemberLoginId()),
                        containsProductName(cond.getProductName()),
                        eqOrderStatus(cond.getOrderStatus()),
                        startDate(cond.getStartDate()),
                        endDate(cond.getEndDate()),
                        eqPaymentStatus(cond.getPaymentStatus()),
                        eqDeliveryStatus(cond.getDeliveryStatus())
                )
                .fetch();
    }

    private BooleanExpression eqMemberLoginId(String memberLoginId) {
        if (!StringUtils.hasText(memberLoginId)) {
            return null;
        }

        return member.loginId.eq(memberLoginId);
    }

    private BooleanExpression containsProductName(String productName) {
        if (!StringUtils.hasText(productName)) {
            return null;
        }

        QOrderProduct subOrderProduct = orderProduct;
        return JPAExpressions.select(Projections.constructor(OrderProductQueryDto.class,
                        order.orderNumber,
                        product.name,
                        product.price,
                        orderProduct.count,
                        orderProduct.orderPrice))
                .from(subOrderProduct)
                .where(
                        subOrderProduct.order.eq(order),
                        containsProductNameAndInitial(productName, subOrderProduct)
                )
                .exists();
    }

    private static BooleanExpression containsProductNameAndInitial(String productName, QOrderProduct subOrderProduct) {
        if (productName.matches("^[ㄱ-ㅎ]+$")) {
            return subOrderProduct.product.nameInitial.contains(productName);
        }

        return subOrderProduct.product.name.containsIgnoreCase(productName);
    }

    private BooleanExpression eqOrderStatus(OrderStatus orderStatus) {
        if (orderStatus == null) {
            return null;
        }

        return order.orderStatus.eq(orderStatus);
    }

    private BooleanExpression startDate(LocalDate startDate) {
        if (startDate == null) {
            return null;
        }

        return order.orderDate.goe(startDate.atStartOfDay());
    }

    private BooleanExpression endDate(LocalDate endDate) {
        if (endDate == null) {
            return null;
        }

        return order.orderDate.lt(endDate.plusDays(1).atStartOfDay());
    }

    private BooleanExpression eqPaymentStatus(PaymentStatus paymentStatus) {
        if (paymentStatus == null) {
            return null;
        }

        return payment.paymentStatus.eq(paymentStatus);
    }

    private BooleanExpression eqDeliveryStatus(DeliveryStatus deliveryStatus) {
        if (deliveryStatus == null) {
            return null;
        }

        return delivery.deliveryStatus.eq(deliveryStatus);
    }
}
