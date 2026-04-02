package lsk.commerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lsk.commerce.dto.request.MemberLoginRequest;
import lsk.commerce.dto.response.Result;
import lsk.commerce.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "01. 인증", description = "관리자 계정 testId, testPassword")
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "로그인", description = "아이디와 비밀번호를 검증하고 토큰을 담은 쿠키를 생성합니다.")
    @PostMapping("/login")
    public ResponseEntity<Result<String>> login(
            @RequestBody @Valid MemberLoginRequest loginRequest,
            HttpServletResponse response
    ) {
        String token = authService.login(loginRequest.loginId(), loginRequest.password());

        Cookie cookie = new Cookie("jjwt", token);
        cookie.setMaxAge(3600);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        return ResponseEntity.ok(new Result<>("login", 1));
    }

    @Operation(summary = "로그아웃", description = "쿠키를 제거합니다.")
    @PostMapping("/logout")
    public ResponseEntity<Result<String>> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("jjwt", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        return ResponseEntity.ok(new Result<>("logout", 1));
    }
}
