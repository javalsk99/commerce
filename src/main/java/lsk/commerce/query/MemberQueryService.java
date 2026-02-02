package lsk.commerce.query;

import lombok.RequiredArgsConstructor;
import lsk.commerce.query.dto.MemberQueryDto;
import lsk.commerce.query.dto.OrderProductQueryDto;
import lsk.commerce.query.dto.OrderQueryDto;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;
import static lsk.commerce.query.MemberQueryRepository.toMemberLoginIds;
import static lsk.commerce.query.MemberQueryRepository.toOrderNumbers;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberQueryService {

    private final MemberQueryRepository memberQueryRepository;

    public MemberQueryDto findMember(String loginId) {
        MemberQueryDto member = memberQueryRepository.findMember(loginId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이디입니다."));

        List<OrderQueryDto> orders = memberQueryRepository.findOrdersByLoginId(loginId);
        Map<String, List<OrderQueryDto>> orderMap = assembleOrders(orders);

        member.setOrders(orderMap.get(member.getLoginId()));

        return member;
    }

    public List<MemberQueryDto> findMembers() {
        List<MemberQueryDto> members = memberQueryRepository.findMembers();
        List<String> loginIds = toMemberLoginIds(members);

        List<OrderQueryDto> orders = memberQueryRepository.findOrdersByLoginIds(loginIds);
        Map<String, List<OrderQueryDto>> orderMap = assembleOrders(orders);

        members.forEach(m -> m.setOrders(orderMap.get(m.getLoginId())));

        return members;
    }

    @NotNull
    private Map<String, List<OrderQueryDto>> assembleOrders(List<OrderQueryDto> orders) {
        List<String> orderNumbers = toOrderNumbers(orders);

        Map<String, List<OrderProductQueryDto>> orderProductMap = memberQueryRepository.findOrderProductMap(orderNumbers);

        orders.forEach(orderQueryDto -> orderQueryDto.setOrderProducts(orderProductMap.get(orderQueryDto.getOrderNumber())));

        Map<String, List<OrderQueryDto>> orderMap = orders.stream()
                .collect(groupingBy(orderQueryDto -> orderQueryDto.getLoginId()));
        return orderMap;
    }
}
