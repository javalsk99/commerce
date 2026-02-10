package lsk.commerce.query;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lsk.commerce.domain.Member;
import lsk.commerce.query.dto.MemberQueryDto;
import lsk.commerce.query.dto.MemberSearchCond;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static lsk.commerce.domain.QMember.member;

@Repository
public class MemberQueryRepository {

    private final EntityManager em;
    private final JPAQueryFactory query;

    public MemberQueryRepository(EntityManager em) {
        this.em = em;
        this.query = new JPAQueryFactory(em);
    }

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

    protected List<Member> search(MemberSearchCond cond) {
        return query.select(member)
                .from(member)
                .where(
                        likeMemberName(cond.getName()),
                        likeMemberLoginId(cond.getLoginId())
                )
                .fetch();
    }

    private BooleanExpression likeMemberName(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }

        if (name.matches("^[ㄱ-ㅎ]+$")) {
            return member.initial.contains(name);
        }

        return member.name.containsIgnoreCase(name);
    }

    private BooleanExpression likeMemberLoginId(String loginId) {
        if (!StringUtils.hasText(loginId)) {
            return null;
        }

        return member.loginId.containsIgnoreCase(loginId);
    }
}
