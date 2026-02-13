package lsk.commerce.repository;

import lsk.commerce.domain.Member;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MemberRepository {

    private final EntityManager em;

    public void save(Member member) {
        em.persist(member);
    }

    public Optional<Member> findByLoginId(String loginId) {
        return em.createQuery("select m from Member m where m.loginId = :loginId", Member.class)
                .setParameter("loginId", loginId)
                .getResultStream()
                .findFirst();
    }

    public void delete(Member member) {
        em.remove(member);
    }

    public boolean existsByLoginId(String loginId) {
        return em.createQuery("select count(m) > 0 from Member m where m.loginId = :loginId", Boolean.class)
                .setParameter("loginId", loginId)
                .getSingleResult();
    }
}
