package lsk.commerce.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lsk.commerce.dto.request.MemberLoginRequest;
import lsk.commerce.dto.response.MemberLoginResponse;
import lsk.commerce.domain.Member;
import lsk.commerce.provider.JwtProvider;
import lsk.commerce.service.AuthService;
import lsk.commerce.service.MemberService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;
    private final AuthService authService;
    private final JwtProvider jwtProvider;

    @PostMapping("/login")
    public MemberLoginResponse login(@Valid MemberLoginRequest loginRequest, HttpServletResponse response) {
        Member loginMember = authService.login(loginRequest.getLoginId(), loginRequest.getPassword());

        String token = jwtProvider.createToken(loginMember);

        Cookie cookie = new Cookie("jjwt", token);
        cookie.setMaxAge(3600);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        return new MemberLoginResponse(loginMember.getLoginId(), loginMember.getGrade());
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

    //결제하기 위한 로그인 (인터셉터 통과)
    @GetMapping("/web/login")
    public String webLogin(@Valid MemberLoginRequest loginRequest, HttpServletResponse response) {
        Member loginMember = authService.login(loginRequest.getLoginId(), loginRequest.getPassword());
        if (memberService.findMemberByLoginId(loginRequest.getLoginId()) == null || loginMember == null) {
            throw new IllegalArgumentException("로그인에 실패했습니다.");
        }

        String token = jwtProvider.createToken(loginMember);

        Cookie cookie = new Cookie("jjwt", token);
        cookie.setMaxAge(3600);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        return "login";
    }
}
