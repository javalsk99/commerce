package lsk.commerce.query;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lsk.commerce.query.dto.OrderProductQueryDto;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;

@Repository
@RequiredArgsConstructor
public class OrderProductQueryRepository {

    private final EntityManager em;

    protected Map<String, List<OrderProductQueryDto>> findOrderProductList(List<String> orderNumbers) {
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

    protected List<OrderProductQueryDto> findOrderProductList(String orderNumber) {
        return em.createQuery(
                        "select new lsk.commerce.query.dto.OrderProductQueryDto(op.order.orderNumber, prod.name, prod.price, op.count, op.orderPrice)" +
                                " from OrderProduct op" +
                                " join op.product prod" +
                                " where op.order.orderNumber = :orderNumber", OrderProductQueryDto.class)
                .setParameter("orderNumber", orderNumber)
                .getResultList();
    }
}
