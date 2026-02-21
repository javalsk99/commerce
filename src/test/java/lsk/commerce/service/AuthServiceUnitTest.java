package lsk.commerce.service;

import lsk.commerce.domain.Member;
import lsk.commerce.util.JwtProvider;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AuthServiceUnitTest {

    @Mock
    MemberService memberService;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtProvider jwtProvider;

    @InjectMocks
    AuthService authService;

    String loginId = "id_A";
    String rawPassword = "12345678";
    String encodedPassword = "$2a$fdio8cv7xh2h98";
    String token = "eyJhfdlsji";

    @Nested
    class SuccessCase {

        @Test
        void login() {
            //given
            Member member = Member.builder().loginId(loginId).password(encodedPassword).build();

            given(memberService.findMemberForLogin(any())).willReturn(member);
            given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);
            given(jwtProvider.createToken(any())).willReturn(token);

            //when
            String jjwt = authService.login(loginId, rawPassword);

            //then
            then(memberService).should().findMemberForLogin(any());
            then(passwordEncoder).should().matches(anyString(), anyString());
            then(jwtProvider).should().createToken(eq(member));
            assertThat(jjwt).isEqualTo(token);
        }
    }

    @Nested
    class FailureCase {

        @Test
        void failed_login_wrongLoginId() {
            //given
            Member.builder().loginId("id_B").password(encodedPassword).build();

            given(memberService.findMemberForLogin(any())).willThrow(new IllegalArgumentException("아이디 또는 비밀번호가 틀렸습니다."));

            //when
            assertThatThrownBy(() -> authService.login(loginId, rawPassword))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("아이디 또는 비밀번호가 틀렸습니다.");

            //then
            then(memberService).should().findMemberForLogin(any());
            then(passwordEncoder).should(never()).matches(anyString(), anyString());
            then(jwtProvider).should(never()).createToken(any());
        }

        @Test
        void failed_login_wrongPassword() {
            //given
            Member member = Member.builder().loginId(loginId).password("11111111").build();

            given(memberService.findMemberForLogin(any())).willReturn(member);
            given(passwordEncoder.matches(anyString(), anyString())).willThrow(new IllegalArgumentException("아이디 또는 비밀번호가 틀렸습니다."));

            //when
            assertThatThrownBy(() -> authService.login(loginId, rawPassword))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("아이디 또는 비밀번호가 틀렸습니다.");

            //then
            then(memberService).should().findMemberForLogin(any());
            then(passwordEncoder).should().matches(anyString(), anyString());
            then(jwtProvider).should(never()).createToken(any());
        }
    }
}