package lsk.commerce.config;

import lsk.commerce.interceptor.LoginCheckInterceptor;
import lsk.commerce.provider.JwtProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    JwtProvider jwtProvider;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginCheckInterceptor(jwtProvider))
                .order(1)
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "logout", "/css/**", "/*.ico", "/*.png", "/error");
    }
}
