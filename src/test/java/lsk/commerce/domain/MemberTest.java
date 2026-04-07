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
    class Create {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //when
                Member member = Member.builder().build();

                //then
                then(member.getRole()).isEqualTo(Role.USER);
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
                member.changePassword("cdCD34#$", passwordEncoder);

                //then
                then(passwordEncoder.matches("cdCD34#$", member.getPassword())).isTrue();
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
                thenThrownBy(() -> member.changePassword("abAB12!@", passwordEncoder))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("비밀번호가 기존과 달라야 합니다");
            }

            static Stream<Arguments> passwordProvider() {
                return Stream.of(
                        argumentSet("비밀번호 null", (String) null),
                        argumentSet("비밀번호 빈 문자열", ""),
                        argumentSet("비밀번호 공백", " ")
                );
            }
        }

        private Member getMember() {
            passwordEncoder = new BCryptPasswordEncoder();
            return Member.builder()
                    .password(passwordEncoder.encode("abAB12!@"))
                    .build();
        }
    }

    @Nested
    class ChangeAddress {

        @Nested
        class SuccessCase {

            @Test
            void shouldIgnoreChange_WhenAddressIsSame() {
                //given
                Member member = Member.builder()
                        .zipcode("01234")
                        .baseAddress("서울시 강남구")
                        .detailAddress("101동 101호")
                        .build();

                //when
                member.changeAddress(member.getAddress().getBaseAddress(), member.getAddress().getDetailAddress(), member.getAddress().getZipcode());
            }

            @ParameterizedTest
            @MethodSource("addressProvider")
            void shouldChangeSuccess_WhenAddressFieldsAreDifferent(String zipcode, String baseAddress, String detailAddress) {
                //given
                Member member = Member.builder()
                        .zipcode("01234")
                        .baseAddress("서울시 강남구")
                        .detailAddress("101동 101호")
                        .build();

                //when
                member.changeAddress(zipcode, baseAddress, detailAddress);

                //then
                thenSoftly(softly -> {
                    softly.then(member.getAddress().getZipcode()).isEqualTo(zipcode);
                    softly.then(member.getAddress().getBaseAddress()).isEqualTo(baseAddress);
                    softly.then(member.getAddress().getDetailAddress()).isEqualTo(detailAddress);
                });
            }

            @Test
            void idempotency() {
                //given
                Member member = Member.builder()
                        .zipcode("01234")
                        .baseAddress("서울시 강남구")
                        .detailAddress("101동 101호")
                        .build();

                //when 첫 번째 호출
                member.changeAddress("01234", "서울시 강북구", "101동 101호");

                //then
                thenSoftly(softly -> {
                    softly.then(member.getAddress().getZipcode()).isEqualTo("01234");
                    softly.then(member.getAddress().getBaseAddress()).isEqualTo("서울시 강북구");
                    softly.then(member.getAddress().getDetailAddress()).isEqualTo("101동 101호");
                });

                //when 두 번째 호출
                thenNoException().isThrownBy(() -> member.changeAddress("01234", "서울시 강북구", "101동 101호"));

                //then
                thenSoftly(softly -> {
                    softly.then(member.getAddress().getZipcode()).isEqualTo("01234");
                    softly.then(member.getAddress().getBaseAddress()).isEqualTo("서울시 강북구");
                    softly.then(member.getAddress().getDetailAddress()).isEqualTo("101동 101호");
                });
            }

            static Stream<Arguments> addressProvider() {
                return Stream.of(
                        argumentSet("zipcode 변경", "01235", "서울시 강남구", "101동 101호"),
                        argumentSet("baseAddress 변경", "01234", "서울시 강북구", "101동 101호"),
                        argumentSet("detailAddress 변경", "01234", "서울시 강남구", "101동 102호"),
                        argumentSet("모두 변경", "01235", "서울시 강북구", "101동 102호")
                );
            }
        }
    }
}