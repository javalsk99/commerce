package lsk.commerce.argumentresolver;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lsk.commerce.exception.InvalidDataException;
import lsk.commerce.util.JwtProvider;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class LoginReactiveArgumentResolver implements HandlerMethodArgumentResolver {

    private final JwtProvider jwtProvider;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        boolean hasLoginAnnotation = parameter.hasParameterAnnotation(Login.class);
        boolean isString = String.class.isAssignableFrom(parameter.getParameterType());
        return hasLoginAnnotation & isString;
    }

    @Override
    public Mono<Object> resolveArgument(MethodParameter parameter, BindingContext bindingContext, ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        HttpCookie cookie = request.getCookies().getFirst("jjwt");

        if (cookie == null) {
            throw new InvalidDataException("");
        }

        String token = cookie.getValue();
        Claims claims = jwtProvider.extractClaims(token);
        String loginId = claims.get("loginId", String.class);
        return Mono.just(loginId);
    }
}
