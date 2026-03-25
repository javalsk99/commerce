package lsk.commerce.argumentresolver;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lsk.commerce.exception.InvalidDataException;
import lsk.commerce.util.JwtProvider;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.util.WebUtils;

@RequiredArgsConstructor
public class LoginMvcArgumentResolver implements HandlerMethodArgumentResolver {

    private final JwtProvider jwtProvider;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        boolean hasLoginAnnotation = parameter.hasParameterAnnotation(Login.class);
        boolean isString = String.class.isAssignableFrom(parameter.getParameterType());
        return hasLoginAnnotation & isString;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
        Cookie cookie = WebUtils.getCookie(request, "jjwt");

        if (cookie == null) {
            throw new InvalidDataException("");
        }

        String token = cookie.getAttribute("jjwt");
        Claims claims = jwtProvider.extractClaims(token);
        return claims.get("loginId", String.class);
    }
}
