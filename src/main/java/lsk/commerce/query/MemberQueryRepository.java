package lsk.commerce.query;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lsk.commerce.query.dto.MemberQueryDto;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.*;

@Repository
@RequiredArgsConstructor
public class MemberQueryRepository {

    private final EntityManager em;

    protected static List<String> toMemberLoginIds(List<MemberQueryDto> result) {
        return result.stream()
                .map(m -> m.getLoginId())
                .collect(toList());
    }

    protected List<MemberQueryDto> findMembers() {
        return em.createQuery(
                        "select new lsk.commerce.query.dto.MemberQueryDto(m.loginId, m.grade)" +
                                " from Member m", MemberQueryDto.class)
                .getResultList();
    }

    protected Optional<MemberQueryDto> findMember(String loginId) {
        return em.createQuery(
                        "select new lsk.commerce.query.dto.MemberQueryDto(m.loginId, m.grade)" +
                                " from Member m" +
                                " where m.loginId = :loginId", MemberQueryDto.class)
                .setParameter("loginId", loginId)
                .getResultStream()
                .findFirst();
    }
}
