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
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

class MemberTest {

    PasswordEncoder passwordEncoder;

    @Nested
    class SuccessCase {

        @Test
        void setUser() {
            //when
            Member member = Member.builder().build();

            //then
            assertThat(member.getGrade()).isEqualTo(Grade.USER);
        }

        @Test
        void setAdmin() {
            //given
            Member member = Member.builder().build();

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

        @ParameterizedTest
        @MethodSource("addressProvider")
        void changeAddress(String city, String street, String zipcode) {
            //given
            Member member = Member.builder()
                    .city("Seoul")
                    .street("Gangnam")
                    .zipcode("01234")
                    .build();

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
                    argumentSet("city 변경", "Gyeonggi-do", "Gangnam", "01234"),
                    argumentSet("street 변경", "Seoul", "Gangbuk", "01234"),
                    argumentSet("zipcode 변경", "Seoul", "Gangnam", "01235"),
                    argumentSet("모두 변경", "Gyeonggi-do", "Gangbuk", "01235")
            );
        }
    }

    @Nested
    class FailureCase {

        @ParameterizedTest
        @MethodSource("passwordProvider")
        void changePassword_passwordBlank(String password) {
            //given
            Member member = getMember();

            //when
            assertThatThrownBy(() -> member.changePassword(password, passwordEncoder))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("비밀번호가 비어있습니다.");
        }

        @Test
        void changePassword_samePassword() {
            //given
            Member member = getMember();

            //when
            assertThatThrownBy(() -> member.changePassword("00000000", passwordEncoder))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("비밀번호가 기존과 달라야 합니다.");
        }

        @Test
        void changeAddress_sameAddress() {
            //given
            Member member = Member.builder()
                    .city("Seoul")
                    .street("Gangnam")
                    .zipcode("01234")
                    .build();

            //when
            assertThatThrownBy(() -> member.changeAddress(member.getAddress().getCity(), member.getAddress().getStreet(), member.getAddress().getZipcode()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("주소가 기존과 달라야 합니다.");
        }

        static Stream<Arguments> passwordProvider() {
            return Stream.of(
                    argumentSet("비밀번호 null", (Object) null),
                    argumentSet("비밀번호 공백", ""),
                    argumentSet("비밀번호 빈 문자열", " ")
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