package lsk.commerce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lsk.commerce.config.WebConfig;
import lsk.commerce.dto.request.MemberLoginRequest;
import lsk.commerce.service.AuthService;
import lsk.commerce.util.JwtProvider;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebConfig.class)
)
class AuthControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    AuthService authService;

    @Nested
    class Login {

        @Nested
        class SuccessCase {

            @Test
            void basic() throws Exception {
                //given
                MemberLoginRequest request = new MemberLoginRequest("id_A", "00000000");
                String json = objectMapper.writeValueAsString(request);

                given(authService.login(anyString(), anyString())).willReturn("token");

                //when & then
                mvc.perform(post("/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isOk())
                        .andExpect(content().string("login"))
                        .andExpect(cookie().value("jjwt", "token"))
                        .andExpect(cookie().maxAge("jjwt", 3600))
                        .andDo(print());

                //then
                BDDMockito.then(authService).should().login(anyString(), anyString());
            }

            @Test
            void web() throws Exception {
                //given
                MultiValueMap<String, String> request = new LinkedMultiValueMap<>();
                request.add("loginId", "id_A");
                request.add("password", "00000000");

                given(authService.login(anyString(), anyString())).willReturn("token");

                //when & then
                mvc.perform(get("/web/login")
                                .params(request))
                        .andExpect(status().isOk())
                        .andExpect(content().string("login"))
                        .andExpect(cookie().value("jjwt", "token"))
                        .andExpect(cookie().maxAge("jjwt", 3600))
                        .andDo(print());

                //then
                BDDMockito.then(authService).should().login(anyString(), anyString());
            }
        }

        @Nested
        class FailureCase {

            @ParameterizedTest
            @MethodSource("invalidMemberLoginRequestProvider")
            void invalidInput(MemberLoginRequest request) throws Exception {
                //given
                String json = objectMapper.writeValueAsString(request);

                //when & then
                mvc.perform(post("/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isBadRequest())
                        .andDo(print());

                //then
                BDDMockito.then(authService).should(never()).login(anyString(), anyString());
            }

            @Test
            void wrongLoginIdOrPassword() throws Exception {
                //given
                MemberLoginRequest request = new MemberLoginRequest("id_B", "11111111");
                String json = objectMapper.writeValueAsString(request);

                given(authService.login(anyString(), anyString())).willThrow(new IllegalArgumentException("아이디 또는 비밀번호가 틀렸습니다"));

                //when & then
                mvc.perform(post("/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value("BAD_ARGUMENT"))
                        .andExpect(jsonPath("$.message").value("아이디 또는 비밀번호가 틀렸습니다"))
                        .andDo(print());

                //then
                BDDMockito.then(authService).should().login(anyString(), anyString());
            }

            static Stream<Arguments> invalidMemberLoginRequestProvider() {
                return Stream.of(
                        argumentSet("loginId null", new MemberLoginRequest(null, "00000000")),
                        argumentSet("password null", new MemberLoginRequest("id_A", null)),
                        argumentSet("password 빈 문자열", new MemberLoginRequest("id_A", "")),
                        argumentSet("password 공백", new MemberLoginRequest("id_A", " ".repeat(8))),
                        argumentSet("password 8자 미만", new MemberLoginRequest("id_A", "a".repeat(7)))
                );
            }
        }
    }

    @Nested
    class Logout {

        @Nested
        class SuccessCase {

            @Test
            void basic() throws Exception {
                //when & then
                mvc.perform(post("/logout"))
                        .andExpect(status().isOk())
                        .andExpect(content().string("logout"))
                        .andExpect(cookie().exists("jjwt"))
                        .andExpect(cookie().maxAge("jjwt", 0))
                        .andDo(print());
            }
        }
    }
}