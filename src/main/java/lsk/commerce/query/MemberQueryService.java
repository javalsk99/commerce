package lsk.commerce.query;

import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Member;
import lsk.commerce.query.dto.MemberQueryDto;
import lsk.commerce.query.dto.MemberSearchCond;
import lsk.commerce.query.dto.OrderQueryDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static lsk.commerce.query.MemberQueryRepository.toMemberLoginIds;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberQueryService {

    private final MemberQueryRepository memberQueryRepository;
    private final OrderQueryService orderQueryService;

    public MemberQueryDto findMember(String loginId) {
        MemberQueryDto member = memberQueryRepository.findMember(loginId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이디입니다."));

        Map<String, List<OrderQueryDto>> orderMap = orderQueryService.findOrderMap(loginId);

        member.setOrders(orderMap.get(member.getLoginId()));

        return member;
    }

    public List<MemberQueryDto> findMembers() {
        List<MemberQueryDto> members = memberQueryRepository.findMembers();
        List<String> loginIds = toMemberLoginIds(members);

        Map<String, List<OrderQueryDto>> orderMap = orderQueryService.findOrderMap(loginIds);

        members.forEach(m -> m.setOrders(orderMap.get(m.getLoginId())));

        return members;
    }

    public List<MemberQueryDto> searchMembers(MemberSearchCond cond) {
        List<Member> members = memberQueryRepository.search(cond);
        List<MemberQueryDto> memberQueryDtoList = memberChangeQueryDtoList(members);
        List<String> loginIds = toMemberLoginIds(memberQueryDtoList);

        Map<String, List<OrderQueryDto>> orderMap = orderQueryService.findOrderMap(loginIds);

        memberQueryDtoList.forEach(m -> m.setOrders(orderMap.get(m.getLoginId())));

        return memberQueryDtoList;
    }

    private List<MemberQueryDto> memberChangeQueryDtoList(List<Member> members) {
        List<MemberQueryDto> memberQueryDtoList = new ArrayList<>();
        for (Member member : members) {
            MemberQueryDto memberQueryDto = MemberQueryDto.changeQueryDto(member);
            memberQueryDtoList.add(memberQueryDto);
        }

        return memberQueryDtoList;
    }
}
