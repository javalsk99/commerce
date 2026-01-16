package lsk.commerce.service;

import jakarta.persistence.NoResultException;
import lsk.commerce.domain.Member;
import lsk.commerce.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    public Long join(Member member) {
        validateMember(member);
        memberRepository.save(member);
        return member.getId();
    }

    @Transactional(readOnly = true)
    public Member findMember(Long memberId) {
        return memberRepository.findOne(memberId);
    }

    @Transactional(readOnly = true)
    public List<Member> findMembers() {
        return memberRepository.findAll();
    }

    public void deleteMember(Long memberId) {
        memberRepository.delete(memberRepository.findOne(memberId));
    }

    public void changePassword(Long memberId, String newPassword) {
        Member member = memberRepository.findOne(memberId);
        member.changePassword(newPassword);
    }

    public void changeAddress(Long memberId, String newCity, String newStreet, String newZipcode) {
        Member member = memberRepository.findOne(memberId);
        member.changeAddress(newCity, newStreet, newZipcode);
    }

    private void validateMember(Member member) {
        List<Member> findMembers = memberRepository.findByLoginId(member.getLoginId());
        if (!findMembers.isEmpty()) {
            throw new IllegalStateException("이미 사용 중인 아이디입니다.");
        }
    }
}
