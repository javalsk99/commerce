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
        OrderQueryDto orderQueryDto = orderQueryRepository.findOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new DataNotFoundException("존재하지 않는 주문입니다"));

        List<OrderProductQueryDto> orderProductQueryDtoList = orderProductQueryRepository.findOrderProductListByOrderNumber(orderNumber);

        return orderQueryDto.toBuilder()
                .orderProductQueryDtoList(orderProductQueryDtoList)
                .build();
    }

    public List<OrderQueryDto> searchOrders(OrderSearchCond cond) {
        List<OrderQueryDto> orderQueryDtoList = orderQueryRepository.search(cond);
        if (orderQueryDtoList.isEmpty()) {
            return orderQueryDtoList;
        }

        List<String> orderNumbers = orderQueryRepository.extractOrderNumbers(orderQueryDtoList);

        Map<String, List<OrderProductQueryDto>> orderProductMap = orderProductQueryRepository.findOrderProductListByOrderNumbers(orderNumbers);

        return orderQueryDtoList.stream()
                .map(orderQueryDto -> orderQueryDto.toBuilder()
                        .orderProductQueryDtoList(orderProductMap.get(orderQueryDto.orderNumber()))
                        .build())
                .toList();
    }

    protected Map<String, List<OrderQueryDto>> findOrderMapByLoginId(String loginId) {
        List<OrderQueryDto> orderQueryDtoList = orderQueryRepository.findOrdersByLoginId(loginId);
        return assembleOrders(orderQueryDtoList);
    }

    protected Map<String, List<OrderQueryDto>> findOrderMapByLoginIds(List<String> loginIds) {
        List<OrderQueryDto> orderQueryDtoList = orderQueryRepository.findOrdersByLoginIds(loginIds);
        return assembleOrders(orderQueryDtoList);
    }

    @NotNull
    private Map<String, List<OrderQueryDto>> assembleOrders(List<OrderQueryDto> orderQueryDtoList) {
        List<String> orderNumbers = orderQueryRepository.extractOrderNumbers(orderQueryDtoList);

        Map<String, List<OrderProductQueryDto>> orderProductMap = orderProductQueryRepository.findOrderProductListByOrderNumbers(orderNumbers);

        return orderQueryDtoList.stream()
                .map(orderQueryDto -> orderQueryDto.toBuilder()
                        .orderProductQueryDtoList(orderProductMap.get(orderQueryDto.orderNumber()))
                        .build())
                .toList()
                .stream()
                .collect(groupingBy(OrderQueryDto::loginId));
    }
}
