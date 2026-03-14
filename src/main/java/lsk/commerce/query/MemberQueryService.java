package lsk.commerce.query;

import lombok.RequiredArgsConstructor;
import lsk.commerce.query.dto.MemberQueryDto;
import lsk.commerce.query.dto.MemberSearchCond;
import lsk.commerce.query.dto.OrderQueryDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberQueryService {

    private final MemberQueryRepository memberQueryRepository;
    private final OrderQueryService orderQueryService;

    public MemberQueryDto findMember(String loginId) {
        MemberQueryDto member = memberQueryRepository.findMember(loginId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이디입니다"));

        Map<String, List<OrderQueryDto>> orderMap = orderQueryService.findOrderMapByLoginId(loginId);

        return member.toBuilder()
                .orderQueryDtoList(orderMap.get(member.loginId()))
                .build();
    }

    public List<MemberQueryDto> searchMembers(MemberSearchCond cond) {
        List<MemberQueryDto> members = memberQueryRepository.search(cond);
        List<String> loginIds = memberQueryRepository.extractLoginIds(members);

        Map<String, List<OrderQueryDto>> orderMap = orderQueryService.findOrderMapByLoginIds(loginIds);

        return members.stream()
                .map(m -> m.toBuilder()
                        .orderQueryDtoList(orderMap.get(m.loginId()))
                        .build())
                .collect(toList());
    }
}
