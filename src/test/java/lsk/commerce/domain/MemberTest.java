package lsk.commerce.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.stream.Stream;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenNoException;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

class MemberTest {

    PasswordEncoder passwordEncoder;

    @Nested
    class SetGrade {

        @Nested
        class SuccessCase {

            @Test
            void user() {
                //when
                Member member = Member.builder().build();

                //then
                then(member.getGrade()).isEqualTo(Grade.USER);
            }

            @Test
            void admin() {
                //given
                Member member = new Member();

                //when
                member.setAdmin();

                //then
                then(member.getGrade()).isEqualTo(Grade.ADMIN);
            }
        }
    }

    @Nested
    class ChangePassword {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                Member member = getMember();

                //when
                member.changePassword("11111111", passwordEncoder);

                //then
                then(passwordEncoder.matches("11111111", member.getPassword())).isTrue();
            }
        }

        @Nested
        class FailureCase {

            @ParameterizedTest
            @MethodSource("passwordProvider")
            void passwordBlank(String password) {
                //given
                Member member = getMember();

                //when & then
                thenThrownBy(() -> member.changePassword(password, passwordEncoder))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("비밀번호가 비어있습니다");
            }

            @Test
            void passwordIsSame() {
                //given
                Member member = getMember();

                //when & then
                thenThrownBy(() -> member.changePassword("00000000", passwordEncoder))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("비밀번호가 기존과 달라야 합니다");
            }

            static Stream<Arguments> passwordProvider() {
                return Stream.of(
                        argumentSet("비밀번호 null", (Object) null),
                        argumentSet("비밀번호 빈 문자열", ""),
                        argumentSet("비밀번호 공백", " ")
                );
            }
        }

        private Member getMember() {
            passwordEncoder = new BCryptPasswordEncoder();
            return Member.builder()
                    .password(passwordEncoder.encode("00000000"))
                    .build();
        }
    }

    @Nested
    class ChangeAddress {

        @Nested
        class SuccessCase {

            @Test
            void shouldIgnoreUpdate_WhenAddressIsSame() {
                //given
                Member member = Member.builder()
                        .city("Seoul")
                        .street("Gangnam")
                        .zipcode("01234")
                        .build();

                //when
                member.changeAddress(member.getAddress().getCity(), member.getAddress().getStreet(), member.getAddress().getZipcode());
            }

            @ParameterizedTest
            @MethodSource("addressProvider")
            void shouldUpdateSuccess_WhenAddressFieldsAreDifferent(String city, String street, String zipcode) {
                //given
                Member member = Member.builder()
                        .city("Seoul")
                        .street("Gangnam")
                        .zipcode("01234")
                        .build();

                //when
                member.changeAddress(city, street, zipcode);

                //then
                thenSoftly(softly -> {
                    softly.then(member.getAddress().getCity()).isEqualTo(city);
                    softly.then(member.getAddress().getStreet()).isEqualTo(street);
                    softly.then(member.getAddress().getZipcode()).isEqualTo(zipcode);
                });
            }

            @Test
            void idempotency() {
                //given
                Member member = Member.builder()
                        .city("Seoul")
                        .street("Gangnam")
                        .zipcode("01234")
                        .build();

                //when 첫 번째 호출
                member.changeAddress("Seoul", "Gangbuk", "01234");

                //then
                thenSoftly(softly -> {
                    softly.then(member.getAddress().getCity()).isEqualTo("Seoul");
                    softly.then(member.getAddress().getStreet()).isEqualTo("Gangbuk");
                    softly.then(member.getAddress().getZipcode()).isEqualTo("01234");
                });

                //when 두 번째 호출
                thenNoException().isThrownBy(() -> member.changeAddress("Seoul", "Gangbuk", "01234"));

                //then
                thenSoftly(softly -> {
                    softly.then(member.getAddress().getCity()).isEqualTo("Seoul");
                    softly.then(member.getAddress().getStreet()).isEqualTo("Gangbuk");
                    softly.then(member.getAddress().getZipcode()).isEqualTo("01234");
                });
            }

            static Stream<Arguments> addressProvider() {
                return Stream.of(
                        argumentSet("city 변경", "Gyeonggi-do", "Gangnam", "01234"),
                        argumentSet("street 변경", "Seoul", "Gangbuk", "01234"),
                        argumentSet("zipcode 변경", "Seoul", "Gangnam", "01235"),
                        argumentSet("모두 변경", "Gyeonggi-do", "Gangbuk", "01235")
                );
            }
        }
    }
}