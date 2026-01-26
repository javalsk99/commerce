package lsk.commerce.interceptor;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lsk.commerce.provider.JwtProvider;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@RequiredArgsConstructor
public class LoginCheckInterceptor implements HandlerInterceptor {

    private final JwtProvider jwtProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        log.info("인증 체크 인터셉터 실행 [{}]{}", method, requestURI);

        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        if ("/members".equals(requestURI) && "POST".equalsIgnoreCase(method)) {
            return true;
        }

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = getToken(request, authorization);

        Claims claims;

        try {
            claims = jwtProvider.extractClaims(token);
        } catch (Exception e) {
            throw new JwtException("유효하지 않은 토큰입니다.");
        }

        String loginId = claims.getSubject();
        String grade = claims.get("grade", String.class);

        if (isAdminPath(requestURI, method)) {
            if (!"ADMIN".equals(grade)) {
                throw new RuntimeException("관리자만 접근할 수 있습니다.");
            }
        }

        request.setAttribute("loginId", loginId);
        return true;
    }

    @NotNull
    private static String getToken(HttpServletRequest request, String authorization) {
        String token = null;

        if (authorization != null && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7);
        }

        if (token == null && request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jjwt".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        if (token == null) {
            throw new RuntimeException("로그인을 해야 접근할 수 있습니다.");
        }
        return token;
    }

    private static boolean isAdminPath(String requestURI, String method) {
        if ("/members".equals(requestURI)) {
            return true;
        }

        if ((requestURI.startsWith("/products") || requestURI.startsWith("/categories"))
                && !"GET".equalsIgnoreCase(method)) {
            return true;
        }

        return false;
    }
}
