package lsk.commerce.integration;

import jakarta.persistence.EntityManager;
import lsk.commerce.domain.Member;
import lsk.commerce.domain.Role;
import lsk.commerce.dto.request.MemberChangePasswordRequest;
import lsk.commerce.dto.request.MemberCreateRequest;
import lsk.commerce.exception.DuplicateResourceException;
import lsk.commerce.repository.MemberRepository;
import lsk.commerce.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;

@Transactional
@SpringBootTest
public class MemberIntegrationTest {

    @Autowired
    EntityManager em;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    MemberService memberService;

    @Nested
    class Join {

        @Nested
        class SuccessCase {

            @Test
            @DisplayName("회원 가입 시 기본 등급이 부여되며, 비밀번호는 암호화되어 저장된다")
            void basic() {
                //given
                MemberCreateRequest request = createRequest("UserA", "id_A", "abAB12!@");

                System.out.println("================= WHEN START =================");

                //when
                String loginId = memberService.join(request);

                em.flush();
                em.clear();

                System.out.println("================= WHEN END ===================");

                //then
                Member member = memberRepository.findByLoginId(loginId)
                        .orElseThrow(() -> new AssertionError("회원이 저장되지 않았습니다"));

                thenSoftly(softly -> {
                    softly.then(member.getPassword()).isNotEqualTo("abAB12!@");
                    softly.then(passwordEncoder.matches("abAB12!@", member.getPassword())).isTrue();
                    softly.then(member.getRole()).isEqualTo(Role.USER);
                });
            }
        }

        @Nested
        class FailureCase {

            @Test
            @DisplayName("한 아이디가 중복으로 가입될 수 없다")
            void duplicateJoin() {
                //given
                memberService.join(createRequest("UserA", "id_A", "abAB12!@"));

                em.flush();
                em.clear();

                MemberCreateRequest request = createRequest("UserB", "id_A", "11111111");

                System.out.println("================= WHEN START =================");

                //when & then
                thenThrownBy(() -> {
                    memberService.join(request);
                    em.flush();
                })
                        .isInstanceOf(DuplicateResourceException.class)
                        .hasMessage("이미 사용 중인 아이디입니다. loginId: " + "id_A");

                System.out.println("================= WHEN END ===================");
            }
        }
    }

    @Nested
    class Change {

        @Nested
        class SuccessCase {

            @Test
            @DisplayName("비밀번호 수정 시, 암호화되어 저장된다")
            void basic() {
                //given
                String loginId = memberService.join(createRequest("UserA", "id_A", "abAB12!@"));

                em.flush();
                em.clear();

                MemberChangePasswordRequest request = new MemberChangePasswordRequest("11111111");

                System.out.println("================= WHEN START =================");

                //when
                memberService.changePassword(loginId, request);

                em.flush();
                em.clear();

                System.out.println("================= WHEN END ===================");

                //then
                Member member = memberRepository.findByLoginId(loginId)
                        .orElseThrow(() -> new AssertionError("회원이 저장되지 않았습니다"));

                thenSoftly(softly -> {
                    softly.then(member.getPassword()).isNotEqualTo("11111111");
                    softly.then(passwordEncoder.matches("11111111", member.getPassword())).isTrue();
                });
            }
        }
    }

    private static MemberCreateRequest createRequest(String name, String loginId, String password) {
        return MemberCreateRequest.builder()
                .name(name)
                .loginId(loginId)
                .password(password)
                .zipcode("01234")
                .baseAddress("서울시 강남구")
                .detailAddress("101동 101호")
                .build();
    }
}
