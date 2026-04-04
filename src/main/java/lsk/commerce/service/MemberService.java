package lsk.commerce.service;

import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Grade;
import lsk.commerce.domain.Member;
import lsk.commerce.dto.request.MemberChangeAddressRequest;
import lsk.commerce.dto.request.MemberChangePasswordRequest;
import lsk.commerce.dto.request.MemberCreateRequest;
import lsk.commerce.dto.response.MemberResponse;
import lsk.commerce.repository.MemberRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public String join(MemberCreateRequest request) {
        Member member = getMember(request);
        validateMember(member);
        memberRepository.save(member);
        return member.getLoginId();
    }

    public Member findMemberByLoginId(String loginId) {
        return memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이디입니다"));
    }

    public Member findMemberForLogin(String loginId) {
        return memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 틀렸습니다"));
    }

    @Transactional
    public Member changePassword(String memberLoginId, MemberChangePasswordRequest request) {
        Member member = findMemberByLoginId(memberLoginId);
        if (memberLoginId.equals("testId")) {
            return member;
        }
        member.changePassword(request.password(), passwordEncoder);
        return member;
    }

    @Transactional
    public Member changeAddress(String memberLoginId, MemberChangeAddressRequest request) {
        Member member = findMemberByLoginId(memberLoginId);
        member.changeAddress(request.city(), request.street(), request.zipcode());
        return member;
    }

    @Transactional
    public void deleteMember(String memberLoginId) {
        if (memberLoginId.equals("testId")) {
            return;
        }

        Optional<Member> optionalMember = memberRepository.findByLoginId(memberLoginId);
        if (optionalMember.isEmpty()) {
            return;
        }

        Member member = optionalMember.get();

        memberRepository.delete(member);
    }

    public MemberResponse getMemberDto(Member member) {
        return MemberResponse.from(member);
    }

    private Member getMember(MemberCreateRequest request) {
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("비밀번호가 비어있습니다");
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        if (!encodedPassword.startsWith("$2a$")) {
            throw new IllegalArgumentException("암호화되지 않은 비밀번호입니다");
        }

        return Member.builder()
                .name(request.name())
                .loginId(request.loginId())
                .password(encodedPassword)
                .city(request.city())
                .street(request.street())
                .zipcode(request.zipcode())
                .build();
    }

    private void validateMember(Member member) {
        if (memberRepository.existsByLoginId(member.getLoginId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다");
        }
    }
}
