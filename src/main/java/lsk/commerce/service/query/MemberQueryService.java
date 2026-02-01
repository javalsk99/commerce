package lsk.commerce.service.query;

import lombok.RequiredArgsConstructor;
import lsk.commerce.dto.query.MemberQueryDto;
import lsk.commerce.repository.query.MemberQueryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberQueryService {

    private final MemberQueryRepository memberQueryRepository;

    public List<MemberQueryDto> findMembersWithOrder() {
        return memberQueryRepository.findAllByDto();
    }
}
