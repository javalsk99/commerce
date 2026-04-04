package lsk.commerce.query;

import lombok.RequiredArgsConstructor;
import lsk.commerce.dto.response.MemberResponse;
import lsk.commerce.exception.DataNotFoundException;
import lsk.commerce.query.dto.MemberQueryDto;
import lsk.commerce.query.dto.MemberSearchCond;
import lsk.commerce.query.dto.OrderQueryDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberQueryService {

    private final MemberQueryRepository memberQueryRepository;
    private final OrderQueryService orderQueryService;

    public MemberQueryDto findMember(String loginId) {
        MemberQueryDto memberQueryDto = memberQueryRepository.findMember(loginId)
                .orElseThrow(() -> new DataNotFoundException("존재하지 않는 아이디입니다"));

        Map<String, List<OrderQueryDto>> orderMap = orderQueryService.findOrderMapByLoginId(loginId);

        return memberQueryDto.toBuilder()
                .orderQueryDtoList(orderMap.get(memberQueryDto.loginId()))
                .build();
    }

    public List<MemberResponse> searchMembers(MemberSearchCond cond) {
        return memberQueryRepository.search(cond);
    }
}
