package lsk.commerce.controller;

import lsk.commerce.service.AuthService;
import lsk.commerce.util.JwtProvider;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.*;
import static org.mockito.BDDMockito.*;
import static org.assertj.core.api.BDDAssertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
class AuthControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AuthService authService;

    @MockitoBean
    JwtProvider jwtProvider;

    @Nested
    class Login {

        @Nested
        class SuccessCase {

            @Test
            void basic() throws Exception {
                //given
                given(authService.login(anyString(), anyString())).willReturn("token");

                //when & then
                mvc.perform(post("/login")
                                .param("loginId", "id_A")
                                .param("password", "00000000"))
                        .andExpect(status().isOk())
                        .andExpect(content().string("login"))
                        .andExpect(cookie().value("jjwt", "token"));

                //then
                BDDMockito.then(authService).should().login(anyString(), anyString());
            }
        }

        @Nested
        class FailureCase {

            @Test
            void wrongLoginIdOrPassword() throws Exception {
                //given
                given(authService.login(anyString(), anyString())).willThrow(new IllegalArgumentException("아이디 또는 비밀번호가 틀렸습니다"));

                //when
                mvc.perform(post("/login")
                                .param("loginId", "id_B")
                                .param("password", "11111111"))
                        .andExpect(status().isInternalServerError())
                        .andExpect(result -> then(result.getResolvedException())
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("아이디 또는 비밀번호가 틀렸습니다.")
                        );
            }

            @ParameterizedTest
            @MethodSource("idPasswordProvider")
            void invalidInput(String loginId, String password) throws Exception {
                //when & then
                mvc.perform(post("/login")
                                .param("loginId", loginId)
                                .param("password", password))
                        .andExpect(status().isBadRequest());

                //then
                BDDMockito.then(authService).should(never()).login(anyString(), anyString());
            }

            static Stream<Arguments> idPasswordProvider() {
                return Stream.of(
                        argumentSet("loginId null", null, "00000000"),
                        argumentSet("password null", "id_A", null),
                        argumentSet("password 빈 문자열", "id_A", ""),
                        argumentSet("password 공백", "id_A", " ".repeat(8)),
                        argumentSet("password 8자 미만", "id_A", "a".repeat(7))
                );
            }
        }
    }
}