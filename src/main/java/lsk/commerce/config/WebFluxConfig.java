package lsk.commerce.config;

import lombok.RequiredArgsConstructor;
import lsk.commerce.argumentresolver.LoginReactiveArgumentResolver;
import lsk.commerce.util.JwtProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebFluxConfig implements WebFluxConfigurer {

    private final JwtProvider jwtProvider;

    @Override
    public void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
        configurer.addCustomResolver(new LoginReactiveArgumentResolver(jwtProvider));
    }
}
