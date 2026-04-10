package lsk.commerce.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        Server prodServer = new Server();
        prodServer.setUrl("https://lsk-commerce.shop");
        prodServer.setDescription("Commerce API Production Server");

        Server localServer = new Server();
        localServer.setUrl("http://localhost:8080");
        localServer.setDescription("Commerce API Local Server");

        return new OpenAPI()
                .info(new Info().title("Commerce API").version("v1")
                        .description("**@RequestBody**: Swagger UI를 통해 즉시 정상/예외 응답 테스트가 가능합니다. \n\n" +
                                "**@PathVariable**, **@RequestParam**: 현재 Swagger UI 라이브러리 제약으로 인해, **리스트 요소 누락**이나 **패턴 불일치** 발생 시 브라우저 단에서 요청이 사전 차단될 수 있습니다. \n\n" +
                                "Postman에서 @PathVariable에 빈 문자열을 넣으면 인터셉터에서 요청을 거절합니다. \n\n" +
                                "예시 응답처럼 잘 나오는지 응답을 직접 확인하시려면 Postman 등의 도구를 사용해 요청해 주시기 바랍니다."
                        )
                )
                .servers(List.of(prodServer));
    }
}
