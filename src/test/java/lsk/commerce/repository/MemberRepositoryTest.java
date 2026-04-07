package lsk.commerce.repository;

import jakarta.validation.ConstraintViolationException;
import lsk.commerce.domain.Role;
import lsk.commerce.domain.Member;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(MemberRepository.class)
class MemberRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    MemberRepository memberRepository;

    @Nested
    class Save {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                Member member = createMember1();

                System.out.println("================= WHEN START =================");

                //when
                memberRepository.save(member);
                em.flush();
                Long memberId = member.getId();
                em.clear();

                System.out.println("================= WHEN END ===================");

                //then
                Member findMember = em.find(Member.class, memberId);
                then(findMember)
                        .extracting("id", "name", "initial", "loginId", "role", "password", "address.zipcode", "address.baseAddress", "address.detailAddress")
                        .containsExactly(memberId, "유저A", "ㅇㅈA", "id_A", Role.USER, "abAB12!@", "01234", "서울시 강남구", "101동 101호");
            }
        }

        @Nested
        class FailureCase {

            @ParameterizedTest
            @MethodSource("nullFieldsMemberProvider")
            void nullFields(Member member) {
                //when & then
                thenThrownBy(() -> memberRepository.save(member))
                        .isInstanceOf(ConstraintViolationException.class)
                        .hasMessageContaining("공백일 수 없습니다");
            }

            @ParameterizedTest
            @MethodSource("wrongLoginIdMemberProvider")
            void wrongLoginId(Member member, String message) {
                //when & then
                thenThrownBy(() -> memberRepository.save(member))
                        .isInstanceOf(ConstraintViolationException.class)
                        .hasMessageContaining(message);
            }

            @Test
            void existsLoginId() {
                //given
                Member member1 = createMember1();
                em.persistAndFlush(member1);
                em.clear();

                Member member2 = createMember2();

                System.out.println("================= WHEN START =================");

                //when & then
                thenThrownBy(() -> memberRepository.save(member2))
                        .isInstanceOf(org.hibernate.exception.ConstraintViolationException.class)
                        .hasMessageContaining("Duplicate entry");

                System.out.println("================= WHEN END ===================");
            }

            static Stream<Arguments> nullFieldsMemberProvider() {
                return Stream.of(
                        argumentSet("이름 null", Member.builder().loginId("id_A").password("abAB12!@").zipcode("01234").baseAddress("서울시 강남구").detailAddress("101동 101호").build()),
                        argumentSet("아이디 null", Member.builder().name("유저A").password("abAB12!@").zipcode("01234").baseAddress("서울시 강남구").detailAddress("101동 101호").build()),
                        argumentSet("비밀번호 null", Member.builder().name("유저A").loginId("id_A").zipcode("01234").baseAddress("서울시 강남구").detailAddress("101동 101호").build()),
                        argumentSet("zipcode null", Member.builder().name("유저A").loginId("id_A").password("abAB12!@").baseAddress("서울시 강남구").detailAddress("101동 101호").build()),
                        argumentSet("baseAddress null", Member.builder().name("유저A").loginId("id_A").password("abAB12!@").zipcode("01234").detailAddress("101동 101호").build()),
                        argumentSet("detailAddress null", Member.builder().name("유저A").loginId("id_A").password("abAB12!@").zipcode("01234").baseAddress("서울시 강남구").build())
                );
            }

            static Stream<Arguments> wrongLoginIdMemberProvider() {
                return Stream.of(
                        argumentSet("아이디 빈 문자열", Member.builder().name("유저A").loginId("").password("abAB12!@").zipcode("01234").baseAddress("서울시 강남구").detailAddress("101동 101호").build(), "공백일 수 없습니다"),
                        argumentSet("아이디 공백", Member.builder().name("유저A").loginId("    ").password("abAB12!@").zipcode("01234").baseAddress("서울시 강남구").detailAddress("101동 101호").build(), "공백일 수 없습니다"),
                        argumentSet("아이디 4자 미만", Member.builder().name("유저A").loginId("idA").password("abAB12!@").zipcode("01234").baseAddress("서울시 강남구").detailAddress("101동 101호").build(), "크기가 4에서 20 사이여야 합니다"),
                        argumentSet("아이디 20자 초과", Member.builder().name("유저A").loginId("a".repeat(21)).password("abAB12!@").zipcode("01234").baseAddress("서울시 강남구").detailAddress("101동 101호").build(), "크기가 4에서 20 사이여야 합니다")
                );
            }
        }
    }

    private abstract class Setup {

        Long memberId;
        String loginId;

        @BeforeEach
        void beforeEach() {
            Member member = createMember1();
            em.persistAndFlush(member);
            memberId = member.getId();
            loginId = member.getLoginId();
            em.clear();
        }
    }

    @Nested
    class Find extends Setup {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                System.out.println("================= WHEN START =================");

                //when
                Optional<Member> findMember = memberRepository.findByLoginId(loginId);

                System.out.println("================= WHEN END ===================");

                //then
                thenSoftly(softly -> {
                    softly.then(findMember).isPresent();
                    softly.then(Hibernate.isInitialized(findMember.get().getOrders())).isFalse();
                    softly.then(findMember.get().getLoginId()).isEqualTo(loginId);
                });
            }

            @Test
            void ShouldReturnEmpty_WhenMemberNotFound() {
                System.out.println("================= WHEN START =================");

                //when
                Optional<Member> findMember = memberRepository.findByLoginId("id_B");

                System.out.println("================= WHEN END ===================");

                //then
                then(findMember).isEmpty();
            }
        }
    }

    @Nested
    class Delete extends Setup {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                Member findMember = em.find(Member.class, memberId);

                System.out.println("================= WHEN START =================");

                //when
                memberRepository.delete(findMember);
                em.flush();
                em.clear();

                System.out.println("================= WHEN END ===================");

                //then
                Member deletedMember = em.find(Member.class, memberId);
                then(deletedMember).isNull();
            }
        }
    }

    @Nested
    class ExistsByLoginId extends Setup {

        @Nested
        class SuccessCase {

            @Test
            void shouldReturnTrue_WhenLoginIdExists() {
                //given
                Member member = createMember2();

                System.out.println("================= WHEN START =================");

                //when
                boolean result = memberRepository.existsByLoginId(member.getLoginId());

                System.out.println("================= WHEN END ===================");

                //then
                then(result).isTrue();
            }

            @Test
            void shouldReturnFalse_WhenLoginIdDoesNotExists() {
                //given
                Member member = createMember3();

                System.out.println("================= WHEN START =================");

                //when
                boolean result = memberRepository.existsByLoginId(member.getLoginId());

                System.out.println("================= WHEN END ===================");

                //then
                then(result).isFalse();
            }
        }
    }

    private Member createMember1() {
        return Member.builder()
                .name("유저A")
                .loginId("id_A")
                .password("abAB12!@")
                .zipcode("01234")
                .baseAddress("서울시 강남구")
                .detailAddress("101동 101호")
                .build();
    }

    private Member createMember2() {
        return Member.builder()
                .name("유저B")
                .loginId("id_A")
                .password("bcBC23@#")
                .zipcode("01234")
                .baseAddress("서울시 강북구")
                .detailAddress("101동 102호")
                .build();
    }

    private Member createMember3() {
        return Member.builder()
                .name("유저C")
                .loginId("id_C")
                .password("cdCD34#$")
                .zipcode("01234")
                .baseAddress("서울시 강동구")
                .detailAddress("101동 103호")
                .build();
    }
}