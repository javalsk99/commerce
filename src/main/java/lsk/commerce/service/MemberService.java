package lsk.commerce.service;

import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Member;
import lsk.commerce.dto.request.MemberRequest;
import lsk.commerce.dto.response.MemberResponse;
import lsk.commerce.repository.MemberRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public String join(MemberRequest request) {
        Member member = getMember(request);
        validateMember(member);
        memberRepository.save(member);
        return member.getLoginId();
    }

    @Transactional
    public String adminJoin(MemberRequest request) {
        Member member = getMember(request);
        validateMember(member);
        member.setAdmin();
        memberRepository.save(member);
        return member.getLoginId();
    }

    private Member getMember(MemberRequest request) {
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("비밀번호가 비어있습니다.");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        return Member.builder()
                .name(request.getName())
                .loginId(request.getLoginId())
                .password(encodedPassword)
                .city(request.getCity())
                .street(request.getStreet())
                .zipcode(request.getZipcode())
                .build();
    }

    public Member findMemberByLoginId(String loginId) {
        return memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이디입니다."));
    }

    public Member findMemberForLogin(String loginId) {
        return memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 틀렸습니다."));
    }

    @Transactional
    public void deleteMember(String memberLoginId) {
        Member member = findMemberByLoginId(memberLoginId);

        memberRepository.delete(member);
    }

    @Transactional
    public Member changePassword(String memberLoginId, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("비밀번호가 비어있습니다.");
        }

        Member member = findMemberByLoginId(memberLoginId);

        String newEncodedPassword = passwordEncoder.encode(newPassword);
        if (passwordEncoder.matches(newEncodedPassword, member.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 기존과 달라야 합니다.");
        }

        member.changePassword(newEncodedPassword);
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
