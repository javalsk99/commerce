package lsk.commerce.interceptor;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lsk.commerce.exception.NotAdminException;
import lsk.commerce.exception.NotResourceOwnerException;
import lsk.commerce.util.JwtProvider;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

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

        if (("/members".equals(requestURI) && "POST".equalsIgnoreCase(method)) || "OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        String token = jwtProvider.getToken(request);

        if (!jwtProvider.validateToken(token)) {
            throw new JwtException("유효하지 않은 토큰입니다");
        }

        Claims claims = jwtProvider.extractClaims(token);
        String loginId = claims.getSubject();
        String grade = claims.get("grade", String.class);
        if (loginId == null || grade == null) {
            throw new JwtException("잘못된 토큰입니다");
        }

        isMemberPath(request, requestURI, loginId);

        if (isAdminPath(requestURI, method)) {
            if (!"ADMIN".equals(grade)) {
                throw new NotAdminException("관리자만 접근할 수 있습니다");
            }
        }

        return true;
    }

    private static void isMemberPath(HttpServletRequest request, String requestURI, String loginId) {
        if (requestURI.startsWith("/members/")) {
            Map<String, String> pathVariables = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            if (pathVariables != null && pathVariables.containsKey("memberLoginId")) {
                String memberLoginId = pathVariables.get("memberLoginId");

                if (memberLoginId == null || !memberLoginId.equals(loginId)) {
                    throw new NotResourceOwnerException("아이디의 주인이 아닙니다");
                }
            }
        }
    }

    private static boolean isAdminPath(String requestURI, String method) {
        if ("/members".equals(requestURI) && "GET".equalsIgnoreCase(method)) {
            return true;
        }

        if ((requestURI.startsWith("/products") || requestURI.startsWith("/categories")) && !"GET".equalsIgnoreCase(method)) {
            return true;
        }

        return requestURI.startsWith("/orders") && "GET".equalsIgnoreCase(method);
    }
}
