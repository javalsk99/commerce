package lsk.commerce.integration;

import jakarta.persistence.EntityManager;
import lsk.commerce.dto.request.MemberCreateRequest;
import lsk.commerce.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.BDDAssertions.thenThrownBy;

@Transactional
@SpringBootTest
public class MemberIntegrationTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberService memberService;

    @Nested
    class Join {

        @Nested
        class FailureCase {

            @Test
            @DisplayName("한 아이디가 중복으로 가입될 수 없다")
            void duplicateJoin() {
                //given
                memberService.join(createRequest("UserA", "id_A"));

                em.flush();
                em.clear();

                MemberCreateRequest request = createRequest("UserB", "id_A");

                System.out.println("================= WHEN START =================");

                //when & then
                thenThrownBy(() -> {
                    memberService.join(request);
                    em.flush();
                })
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("이미 사용 중인 아이디입니다");

                System.out.println("================= WHEN END ===================");
            }

            private static MemberCreateRequest createRequest(String name, String loginId) {
                return MemberCreateRequest.builder()
                        .name(name)
                        .loginId(loginId)
                        .password("00000000")
                        .city("Seoul")
                        .street("Gangnam")
                        .zipcode("01234")
                        .build();
            }
        }
    }
}
