package lsk.commerce.service;

import lsk.commerce.domain.Member;
import lsk.commerce.dto.response.MemberResponse;
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

    public Long adminJoin(Member member) {
        validateMember(member);
        member.setAdmin();
        memberRepository.save(member);
        return member.getId();
    }

    @Transactional(readOnly = true)
    public Member findMember(Long memberId) {
        return memberRepository.findOne(memberId);
    }

    @Transactional(readOnly = true)
    public Member findMemberByLoginId(String loginId) {
        return memberRepository.findByLoginId(loginId);
    }

    @Transactional(readOnly = true)
    public List<Member> findMembers() {
        return memberRepository.findAll();
    }

    public void deleteMember(Member member) {
        memberRepository.delete(member);
    }

    public void changePassword(String memberLoginId, String newPassword) {
        Member member = memberRepository.findByLoginId(memberLoginId);
        member.changePassword(newPassword);
    }

    public void changeAddress(String memberLoginId, String newCity, String newStreet, String newZipcode) {
        Member member = memberRepository.findByLoginId(memberLoginId);
        member.changeAddress(newCity, newStreet, newZipcode);
    }

    @Transactional(readOnly = true)
    public MemberResponse getMemberDto(Member member) {
        return MemberResponse.memberChangeDto(member);
    }

    private void validateMember(Member member) {
        Member findMembers = memberRepository.findByLoginId(member.getLoginId());
        if (findMembers != null) {
            throw new IllegalStateException("이미 사용 중인 아이디입니다.");
        }
    }
}
