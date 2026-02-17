package lsk.commerce.service;

import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Member;
import lsk.commerce.util.JwtProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {

    private final MemberService memberService;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    public String login(String loginId, String password) {
        Member loginMember = memberService.findMemberForLogin(loginId);
        if (!passwordEncoder.matches(passwordEncoder.encode(password), loginMember.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 틀렸습니다.");
        }

        return jwtProvider.createToken(loginMember);
    }
}
