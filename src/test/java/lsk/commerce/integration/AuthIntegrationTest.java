package lsk.commerce.integration;

import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityManager;
import lsk.commerce.domain.Role;
import lsk.commerce.dto.request.MemberCreateRequest;
import lsk.commerce.service.AuthService;
import lsk.commerce.service.MemberService;
import lsk.commerce.util.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;

@Transactional
@SpringBootTest
public class AuthIntegrationTest {

    @Autowired
    EntityManager em;

    @Autowired
    JwtProvider jwtProvider;

    @Autowired
    AuthService authService;

    @Autowired
    MemberService memberService;

    @Nested
    class Login {

        @Nested
        class SuccessCase {

            @Test
            @DisplayName("로그인 성공 시, 회원의 로그인 아이디와 등급 정보가 포함된 토큰을 반환한다")
            void basic() {
                //given
                memberService.join(createRequest());

                em.flush();
                em.clear();

                System.out.println("================= WHEN START =================");

                //when
                String token = authService.login("id_A", "abAB12!@");

                em.flush();
                em.clear();

                System.out.println("================= WHEN END ===================");

                //then
                Claims claims = jwtProvider.extractClaims(token);
                String loginId = claims.getSubject();
                String role = claims.get("role", String.class);

                thenSoftly(softly -> {
                    softly.then(loginId).isEqualTo("id_A");
                    softly.then(role).isEqualTo(Role.USER.name());
                });
            }

            private static MemberCreateRequest createRequest() {
                return MemberCreateRequest.builder()
                        .name("UserA")
                        .loginId("id_A")
                        .password("abAB12!@")
                        .zipcode("01234")
                        .baseAddress("서울시 강남구")
                        .detailAddress("101동 101호")
                        .build();
            }
        }
    }
}
