package lsk.commerce.query;

import lombok.RequiredArgsConstructor;
import lsk.commerce.exception.DataNotFoundException;
import lsk.commerce.query.dto.OrderProductQueryDto;
import lsk.commerce.query.dto.OrderQueryDto;
import lsk.commerce.query.dto.OrderSearchCond;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrderQueryRepository orderQueryRepository;
    private final OrderProductQueryRepository orderProductQueryRepository;

    public OrderQueryDto findOrder(String orderNumber) {
        OrderQueryDto order = orderQueryRepository.findOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new DataNotFoundException("존재하지 않는 주문입니다"));

        List<OrderProductQueryDto> orderProductList = orderProductQueryRepository.findOrderProductListByOrderNumber(orderNumber);

        return order.toBuilder()
                .orderProductQueryDtoList(orderProductList)
                .build();
    }

    public List<OrderQueryDto> searchOrders(OrderSearchCond cond) {
        List<OrderQueryDto> orders = orderQueryRepository.search(cond);
        if (orders.isEmpty()) {
            return orders;
        }

        List<String> orderNumbers = orderQueryRepository.extractOrderNumbers(orders);

        Map<String, List<OrderProductQueryDto>> orderProductMap = orderProductQueryRepository.findOrderProductListByOrderNumbers(orderNumbers);

        return orders.stream()
                .map(orderQueryDto -> orderQueryDto.toBuilder()
                        .orderProductQueryDtoList(orderProductMap.get(orderQueryDto.orderNumber()))
                        .build())
                .toList();
    }

    protected Map<String, List<OrderQueryDto>> findOrderMapByLoginId(String loginId) {
        List<OrderQueryDto> orders = orderQueryRepository.findOrdersByLoginId(loginId);
        return assembleOrders(orders);
    }

    protected Map<String, List<OrderQueryDto>> findOrderMapByLoginIds(List<String> loginIds) {
        List<OrderQueryDto> orders = orderQueryRepository.findOrdersByLoginIds(loginIds);
        return assembleOrders(orders);
    }

    @NotNull
    private Map<String, List<OrderQueryDto>> assembleOrders(List<OrderQueryDto> orders) {
        List<String> orderNumbers = orderQueryRepository.extractOrderNumbers(orders);

        Map<String, List<OrderProductQueryDto>> orderProductMap = orderProductQueryRepository.findOrderProductListByOrderNumbers(orderNumbers);

        List<OrderQueryDto> orderQueryDtoList = orders.stream()
                .map(orderQueryDto -> orderQueryDto.toBuilder()
                        .orderProductQueryDtoList(orderProductMap.get(orderQueryDto.orderNumber()))
                        .build())
                .toList();

        return orderQueryDtoList.stream()
                .collect(groupingBy(OrderQueryDto::loginId));
    }
}
