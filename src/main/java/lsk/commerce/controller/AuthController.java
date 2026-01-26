package lsk.commerce.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lsk.commerce.controller.form.LoginRequest;
import lsk.commerce.controller.form.LoginResponse;
import lsk.commerce.domain.Member;
import lsk.commerce.provider.JwtProvider;
import lsk.commerce.service.AuthService;
import lsk.commerce.service.MemberService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;
    private final AuthService authService;
    private final JwtProvider jwtProvider;

    @PostMapping("/login")
    public LoginResponse login(@Valid LoginRequest loginRequest, HttpServletResponse response) {
        Member loginMember = authService.login(loginRequest.getLoginId(), loginRequest.getPassword());
        if (memberService.findMemberByLoginId(loginRequest.getLoginId()) == null) {
            throw new IllegalArgumentException("잘못된 아이디를 입력했습니다. id: " + loginRequest.getLoginId());
        } else if (loginMember == null) {
            throw new IllegalArgumentException("잘못된 비밀번호를 입력했습니다.");
        }

        String token = jwtProvider.createToken(loginMember);

        Cookie cookie = new Cookie("jjwt", token);
        cookie.setMaxAge(3600);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        return new LoginResponse(loginMember.getLoginId(), loginMember.getGrade());
    }

    @PostMapping("/logout")
    public String logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("jjwt", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        return "logout";
    }
}
