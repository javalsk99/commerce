package lsk.commerce.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lsk.commerce.dto.request.MemberLoginRequest;
import lsk.commerce.dto.response.Result;
import lsk.commerce.service.AuthService;
import lsk.commerce.util.JwtProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<Result<String>> login(@RequestBody @Valid MemberLoginRequest loginRequest, HttpServletResponse response) {
        String token = authService.login(loginRequest.loginId(), loginRequest.password());

        Cookie cookie = new Cookie("jjwt", token);
        cookie.setMaxAge(3600);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        return ResponseEntity.ok(new Result<>("login", 1));
    }

    @PostMapping("/logout")
    public ResponseEntity<Result<String>> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("jjwt", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        return ResponseEntity.ok(new Result<>("logout", 1));
    }

    //결제하기 위한 로그인 (인터셉터 통과)
    @GetMapping("/web/login")
    public ResponseEntity<Result<String>> webLogin(@ModelAttribute @Valid MemberLoginRequest loginRequest, HttpServletResponse response) {
        String token = authService.login(loginRequest.loginId(), loginRequest.password());

        Cookie cookie = new Cookie("jjwt", token);
        cookie.setMaxAge(3600);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        return ResponseEntity.ok(new Result<>("login", 1));
    }
}
