package lsk.commerce.domain;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class MemberTest {

    PasswordEncoder passwordEncoder;

    @Nested
    class SuccessCase {

        @Test
        void setUser() {
            //when
            Member member = getMember();

            //then
            assertThat(member.getGrade()).isEqualTo(Grade.USER);
        }

        @Test
        void setAdmin() {
            //given
            Member member = getMember();

            //when
            member.setAdmin();

            //then
            assertThat(member.getGrade()).isEqualTo(Grade.ADMIN);
        }

        @Test
        void changePassword() {
            //given
            Member member = getMember();

            //when
            member.changePassword("11111111", passwordEncoder);

            //then
            assertThat(passwordEncoder.matches("11111111", member.getPassword())).isTrue();
        }

        @ParameterizedTest(name = "[{index}] {3}")
        @MethodSource("addressProvider")
        void changeAddress(String city, String street, String zipcode, String reason) {
            //given
            Member member = getMember();

            //when
            member.changeAddress(city, street, zipcode);

            //then
            Assertions.assertAll(
                    () -> assertThat(member.getAddress().getCity()).isEqualTo(city),
                    () -> assertThat(member.getAddress().getStreet()).isEqualTo(street),
                    () -> assertThat(member.getAddress().getZipcode()).isEqualTo(zipcode)
            );
        }

        static Stream<Arguments> addressProvider() {
            return Stream.of(
                    arguments("Gyeonggi-do", "Gangnam", "01234", "city 변경"),
                    arguments("Seoul", "Gangbuk", "01234", "street 변경"),
                    arguments("Seoul", "Gangnam", "01235", "zipcode 변경"),
                    arguments("Gyeonggi-do", "Gangbuk", "01235", "모두 변경")
            );
        }
    }

    @Nested
    class FailureCase {

        @ParameterizedTest(name = "[{index}] {1}")
        @MethodSource("passwordProvider")
        void failed_changePassword_BlankPassword(String password, String reason) {
            //given
            Member member = getMember();

            //when
            assertThatThrownBy(() -> member.changePassword(password, passwordEncoder))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("비밀번호가 비어있습니다.");
        }

        @Test
        void failed_changePassword_samePassword() {
            //given
            Member member = getMember();

            //when
            assertThatThrownBy(() -> member.changePassword("00000000", passwordEncoder))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("비밀번호가 기존과 달라야 합니다.");
        }

        @Test
        void failed_changeAddress_sameAddress() {
            //given
            Member member = getMember();

            //when
            assertThatThrownBy(() -> member.changeAddress(member.getAddress().getCity(), member.getAddress().getStreet(), member.getAddress().getZipcode()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("주소가 기존과 달라야 합니다.");
        }

        static Stream<Arguments> passwordProvider() {
            return Stream.of(
                    arguments(null, "비밀번호 null"),
                    arguments("", "비밀번호 공백"),
                    arguments(" ", "비밀번호 빈 문자열")
            );
        }
    }

    private Member getMember() {
        passwordEncoder = new BCryptPasswordEncoder();
        return Member.builder()
                .name("userA")
                .loginId("id_A")
                .password(passwordEncoder.encode("00000000"))
                .city("Seoul")
                .street("Gangnam")
                .zipcode("01234")
                .build();
    }
}