package lsk.commerce.service;

import lsk.commerce.domain.Member;
import lsk.commerce.util.JwtProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@SpringBootTest
@Transactional
class AuthServiceTest {

    @Autowired
    JwtProvider jwtProvider;
    @Autowired
    MemberService memberService;
    @Autowired
    AuthService authService;

    @Test
    void login() {
        //given
        Member member = createMember();

        //when
        String token = authService.login(member.getLoginId(), member.getPassword());

        //then
        assertThat(jwtProvider.validateToken(token)).isTrue();
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("loginIdPasswordProvider")
    void failed_login_wrongLoginIdPassword(String loginId, String password, String reason) {
        //given
        createMember();

        //when
        assertThrows(IllegalArgumentException.class, () ->
                authService.login(loginId, password));
    }

    private Member createMember() {
        Member member = new Member("userA", "id_A", "00000000", "Seoul", "Gangnam", "01234");
        memberService.join(member);
        return member;
    }

    static Stream<Arguments> loginIdPasswordProvider() {
        return Stream.of(
                arguments(null, "00000000", "아이디 null"),
                arguments("", "00000000", "아이디 빈 문자열"),
                arguments(" ", "00000000", "아이디 공백"),
                arguments("id_B", "00000000", "존재하지 않는 아이디"),
                arguments("id_A", null, "비밀번호 null"),
                arguments("id_A", "", "비밀번호 빈 문자열"),
                arguments("id_A", " ", "비밀번호 공백"),
                arguments("id_A", "11111111", "존재하지 않는 비밀번호")
        );
    }
}