package lsk.commerce.repository;

import jakarta.validation.ConstraintViolationException;
import lsk.commerce.domain.Grade;
import lsk.commerce.domain.Member;
import org.junit.jupiter.api.Assertions;
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

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.*;

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

                //when
                memberRepository.save(member);
                em.persist(member);
                Long memberId = member.getId();
                em.clear();

                //then
                Member findMember = em.find(Member.class, memberId);
                assertThat(findMember)
                        .extracting("id", "name", "initial", "loginId", "grade", "password", "address.city", "address.street", "address.zipcode")
                        .containsExactly(memberId, "유저A", "ㅇㅈA", "id_A", Grade.USER, "00000000", "Seoul", "Gangnam", "01234");
            }
        }

        @Nested
        class FailureCase {

            @ParameterizedTest
            @MethodSource("nullFieldsMemberProvider")
            void nullFields(Member member) {
                //when
                assertThatThrownBy(() -> memberRepository.save(member))
                        .isInstanceOf(ConstraintViolationException.class)
                        .hasMessageContaining("공백일 수 없습니다");
            }

            @ParameterizedTest
            @MethodSource("wrongLoginIdMemberProvider")
            void wrongLoginId(Member member, String message) {
                //when
                assertThatThrownBy(() -> memberRepository.save(member))
                        .isInstanceOf(ConstraintViolationException.class)
                        .hasMessageContaining(message);
            }

            @Test
            void existsLoginId() {
                //given
                Member member1 = createMember1();
                em.persistAndFlush(member1);
                em.clear();

                //when
                Member member2 = createMember2();
                assertThatThrownBy(() -> memberRepository.save(member2))
                        .isInstanceOf(org.hibernate.exception.ConstraintViolationException.class)
                        .hasMessageContaining("Duplicate entry");
            }

            static Stream<Arguments> nullFieldsMemberProvider() {
                return Stream.of(
                        argumentSet("이름 null", Member.builder().loginId("id_A").password("00000000").city("Seoul").street("Gangnam").zipcode("01234").build()),
                        argumentSet("아이디 null", Member.builder().name("유저A").password("00000000").city("Seoul").street("Gangnam").zipcode("01234").build()),
                        argumentSet("비밀번호 null", Member.builder().name("유저A").loginId("id_A").city("Seoul").street("Gangnam").zipcode("01234").build()),
                        argumentSet("city null", Member.builder().name("유저A").loginId("id_A").password("00000000").street("Gangnam").zipcode("01234").build()),
                        argumentSet("street null", Member.builder().name("유저A").loginId("id_A").password("00000000").city("Seoul").zipcode("01234").build()),
                        argumentSet("zipcode null", Member.builder().name("유저A").loginId("id_A").password("00000000").city("Seoul").street("Gangnam").build())
                );
            }

            static Stream<Arguments> wrongLoginIdMemberProvider() {
                return Stream.of(
                        argumentSet("아이디 빈 문자열", Member.builder().name("유저A").loginId("").password("00000000").city("Seoul").street("Gangnam").zipcode("01234").build(), "공백일 수 없습니다"),
                        argumentSet("아이디 공백", Member.builder().name("유저A").loginId("    ").password("00000000").city("Seoul").street("Gangnam").zipcode("01234").build(), "공백일 수 없습니다"),
                        argumentSet("아이디 4자 미만", Member.builder().name("유저A").loginId("idA").password("00000000").city("Seoul").street("Gangnam").zipcode("01234").build(), "크기가 4에서 20 사이여야 합니다"),
                        argumentSet("아이디 20자 초과", Member.builder().name("유저A").loginId("a".repeat(21)).password("00000000").city("Seoul").street("Gangnam").zipcode("01234").build(), "크기가 4에서 20 사이여야 합니다")
                );
            }
        }
    }

    @Nested
    class Find {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                Member member = createMember1();
                em.persistAndFlush(member);
                String loginId = member.getLoginId();
                em.clear();

                //when
                Optional<Member> findMember = memberRepository.findByLoginId(loginId);

                //then
                assertAll(
                        () -> assertThat(findMember).isPresent(),
                        () -> assertThat(findMember.get().getLoginId()).isEqualTo(loginId)
                );
            }
        }

        @Nested
        class FailureCase {

            @Test
            void memberNotFound() {
                //when
                Optional<Member> findMember = memberRepository.findByLoginId("id_B");

                //then
                assertThat(findMember).isEmpty();
            }
        }
    }

    private Member createMember1() {
        return Member.builder()
                .name("유저A")
                .loginId("id_A")
                .password("00000000")
                .city("Seoul")
                .street("Gangnam")
                .zipcode("01234")
                .build();
    }

    private Member createMember2() {
        return Member.builder()
                .name("유저B")
                .loginId("id_A")
                .password("11111111")
                .city("Gyeonggi-do")
                .street("Gangbuk")
                .zipcode("01235")
                .build();
    }
}