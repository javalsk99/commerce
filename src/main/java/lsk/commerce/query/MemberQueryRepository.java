package lsk.commerce.query;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lsk.commerce.dto.response.MemberResponse;
import lsk.commerce.dto.response.QMemberResponse;
import lsk.commerce.query.dto.MemberQueryDto;
import lsk.commerce.query.dto.MemberSearchCond;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static lsk.commerce.domain.QMember.member;

@Repository
@RequiredArgsConstructor
public class MemberQueryRepository {

    private final EntityManager em;
    private final JPAQueryFactory query;

    protected List<String> extractLoginIds(List<MemberQueryDto> result) {
        return result.stream()
                .map(MemberQueryDto::loginId)
                .filter(Objects::nonNull)
                .collect(toList());
    }

    protected Optional<MemberQueryDto> findMember(String loginId) {
        return em.createQuery(
                        "select new lsk.commerce.query.dto.MemberQueryDto(m.loginId, m.role)" +
                                " from Member m" +
                                " where m.loginId = :loginId", MemberQueryDto.class)
                .setParameter("loginId", loginId)
                .getResultList()
                .stream()
                .findFirst();
    }

    protected List<MemberResponse> search(MemberSearchCond cond) {
        return query.select(new QMemberResponse(member.loginId, member.address.city, member.address.street, member.address.zipcode))
                .from(member)
                .where(
                        containsMemberName(cond.name()),
                        containsMemberLoginId(cond.loginId())
                )
                .fetch();
    }

    private BooleanExpression containsMemberName(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }

        if (name.matches(".*[ㄱ-ㅎ].*")) {
            return member.initial.contains(name);
        }

        return member.name.containsIgnoreCase(name);
    }

    private BooleanExpression containsMemberLoginId(String loginId) {
        if (!StringUtils.hasText(loginId)) {
            return null;
        }

        return member.loginId.containsIgnoreCase(loginId);
    }
}
