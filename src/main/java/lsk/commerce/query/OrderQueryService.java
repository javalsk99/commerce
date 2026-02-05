package lsk.commerce.query;

import lombok.RequiredArgsConstructor;
import lsk.commerce.query.dto.OrderProductQueryDto;
import lsk.commerce.query.dto.OrderQueryDto;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;
import static lsk.commerce.query.OrderQueryRepository.toOrderNumbers;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrderQueryRepository orderQueryRepository;
    private final OrderProductQueryRepository orderProductQueryRepository;

    public OrderQueryDto findOrder(String orderNumber) {
        OrderQueryDto order = orderQueryRepository.findOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        List<OrderProductQueryDto> orderProductList = orderProductQueryRepository.findOrderProductList(orderNumber);

        order.setOrderProducts(orderProductList);

        return order;
    }

    protected Map<String, List<OrderQueryDto>> findOrderMap(String loginId) {
        List<OrderQueryDto> orders = orderQueryRepository.findOrdersByLoginId(loginId);
        return assembleOrders(orders);
    }

    protected Map<String, List<OrderQueryDto>> findOrderMap(List<String> loginIds) {
        List<OrderQueryDto> orders = orderQueryRepository.findOrdersByLoginIds(loginIds);
        return assembleOrders(orders);
    }

    @NotNull
    private Map<String, List<OrderQueryDto>> assembleOrders(List<OrderQueryDto> orders) {
        List<String> orderNumbers = toOrderNumbers(orders);

        Map<String, List<OrderProductQueryDto>> orderProductMap = orderProductQueryRepository.findOrderProductList(orderNumbers);

        orders.forEach(orderQueryDto -> orderQueryDto.setOrderProducts(orderProductMap.get(orderQueryDto.getOrderNumber())));

        Map<String, List<OrderQueryDto>> orderMap = orders.stream()
                .collect(groupingBy(orderQueryDto -> orderQueryDto.getLoginId()));
        return orderMap;
    }
}
