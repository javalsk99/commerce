package lsk.commerce.service;

import lsk.commerce.domain.Member;
import lsk.commerce.dto.response.MemberResponse;
import lsk.commerce.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    @Transactional
    public Long join(Member member) {
        validateMember(member);
        memberRepository.save(member);
        return member.getId();
    }

    @Transactional
    public Long adminJoin(Member member) {
        validateMember(member);
        member.setAdmin();
        memberRepository.save(member);
        return member.getId();
    }

    public Member findMember(Long memberId) {
        return memberRepository.findOne(memberId);
    }

    public Member findMemberByLoginId(String loginId) {
        return memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이디입니다."));
    }

    public Member findMemberForLogin(String loginId) {
        return memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 틀렸습니다."));
    }

    public List<Member> findMembers() {
        return memberRepository.findAll();
    }

    @Transactional
    public void deleteMember(Member member) {
        memberRepository.delete(member);
    }

    @Transactional
    public Member changePassword(String memberLoginId, String newPassword) {
        Member member = findMemberByLoginId(memberLoginId);
        member.changePassword(newPassword);
        return member;
    }

    @Transactional
    public Member changeAddress(String memberLoginId, String newCity, String newStreet, String newZipcode) {
        Member member = findMemberByLoginId(memberLoginId);
        member.changeAddress(newCity, newStreet, newZipcode);
        return member;
    }

    public MemberResponse getMemberDto(Member member) {
        return MemberResponse.memberChangeDto(member);
    }

    private void validateMember(Member member) {
        if (memberRepository.existsByLoginId(member.getLoginId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
    }
}
