package lsk.commerce.service;

import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Member;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {

    private final MemberService memberService;

    public Member login(String loginId, String password) {
        Member member = memberService.findMemberByLoginId(loginId);
        if (!member.getPassword().equals(password)) {
            return null;
        }

        return member;
    }
}
