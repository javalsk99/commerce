package lsk.commerce.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Spy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class MemberTest {

    @Spy
    private PasswordEncoder passwordEncoder;

    @Test
    void setUser() {
        //given
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
        member.changePassword(passwordEncoder.encode("11111111"));

        //then
        assertThat(passwordEncoder.matches("11111111", member.getPassword())).isTrue();
    }

    @Test
    void failed_changePassword_notEncoded() {
        //given
        Member member = getMember();

        //when
        assertThatThrownBy(() -> member.changePassword("11111111"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("암호화되지 않은 비밀번호입니다.");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("addressProvider")
    void changeAddress(String city, String street, String zipcode, String reason) {
        //given
        Member member = getMember();

        //when
        member.changeAddress(city, street, zipcode);

        //then
        assertThat(member.getAddress().getCity()).isEqualTo(city);
        assertThat(member.getAddress().getStreet()).isEqualTo(street);
        assertThat(member.getAddress().getZipcode()).isEqualTo(zipcode);
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

    static Stream<Arguments> addressProvider() {
        return Stream.of(
                arguments("Gyeonggi-do", "Gangnam", "01234", "city 변경"),
                arguments("Seoul", "Gangbuk", "01234", "street 변경"),
                arguments("Seoul", "Gangnam", "01235", "zipcode 변경"),
                arguments("Gyeonggi-do", "Gangbuk", "01235", "모두 변경")
        );
    }
}