package lsk.commerce.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lsk.commerce.dto.request.MemberRequest;
import lsk.commerce.query.MemberQueryService;
import lsk.commerce.query.dto.MemberQueryDto;
import lsk.commerce.query.dto.MemberSearchCond;
import lsk.commerce.service.MemberService;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MemberController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class MemberControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    MemberService memberService;

    @MockitoBean
    MemberQueryService memberQueryService;

    @MockitoBean
    JwtProvider jwtProvider;

    @Nested
    class Create {

        @Nested
        class SuccessCase {

            @Test
            void basic() throws Exception {
                //given
                MemberRequest request = MemberRequest.builder()
                        .name("User")
                        .loginId("id_A")
                        .password("00000000")
                        .city("Seoul")
                        .street("Gangnam")
                        .zipcode("01234")
                        .build();
                String json = objectMapper.writeValueAsString(request);

                given(memberService.join(any(MemberRequest.class))).willReturn(request.loginId());

                //when
                mvc.perform(post("/members")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isCreated())
                        .andExpect(content().string("id_A"))
                        .andDo(print());

                //then
                BDDMockito.then(memberService).should().join(request);
            }
        }

        @Nested
        class FailureCase {

            @ParameterizedTest
            @MethodSource("invalidMemberRequestProvider")
            void invalidInput(MemberRequest request) throws Exception {
                //given
                String json = objectMapper.writeValueAsString(request);

                //when
                mvc.perform(post("/members")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isBadRequest())
                        .andDo(print());

                //then
                BDDMockito.then(memberService).should(never()).join(any());
            }

            @Test
            void join_Failed_existsLoginId() throws Exception {
                //given
                MemberRequest request = MemberRequest.builder()
                        .name("User")
                        .loginId("id_A")
                        .password("00000000")
                        .city("Seoul")
                        .street("Gangnam")
                        .zipcode("01234")
                        .build();
                String json = objectMapper.writeValueAsString(request);

                given(memberService.join(any(MemberRequest.class))).willThrow(new IllegalArgumentException("이미 사용 중인 아이디입니다"));

                //when
                mvc.perform(post("/members")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value("BAD_ARGUMENT"))
                        .andExpect(jsonPath("$.message").value("이미 사용 중인 아이디입니다"))
                        .andDo(print());

                //then
                BDDMockito.then(memberService).should().join(request);
            }

            static Stream<Arguments> invalidMemberRequestProvider() {
                return Stream.of(
                        argumentSet("name null", MemberRequest.builder().loginId("id_A").password("00000000").city("Seoul").street("Gangnam").zipcode("01234").build()),
                        argumentSet("loginId null", MemberRequest.builder().name("User").password("00000000").city("Seoul").street("Gangnam").zipcode("01234").build()),
                        argumentSet("password null", MemberRequest.builder().name("User").loginId("id_A").city("Seoul").street("Gangnam").zipcode("01234").build()),
                        argumentSet("city null", MemberRequest.builder().name("User").loginId("id_A").password("00000000").street("Gangnam").zipcode("01234").build()),
                        argumentSet("street null", MemberRequest.builder().name("User").loginId("id_A").password("00000000").city("Seoul").zipcode("01234").build()),
                        argumentSet("zipcode null", MemberRequest.builder().name("User").loginId("id_A").password("00000000").city("Seoul").street("Gangnam").build()),
                        argumentSet("password 빈 문자열", MemberRequest.builder().name("User").loginId("id_A").password("").city("Seoul").street("Gangnam").zipcode("01234").build()),
                        argumentSet("password 공백", MemberRequest.builder().loginId("id_A").password(" ".repeat(8)).city("Seoul").street("Gangnam").zipcode("01234").build()),
                        argumentSet("password 8자 미만", MemberRequest.builder().loginId("id_A").password("0".repeat(7)).city("Seoul").street("Gangnam").zipcode("01234").build()),
                        argumentSet("password 20자 초과", MemberRequest.builder().loginId("id_A").password("0".repeat(21)).city("Seoul").street("Gangnam").zipcode("01234").build())
                );
            }
        }
    }

    @Nested
    class MemberList {

        @Nested
        class SuccessCase {

            @Test
            void basic() throws Exception {
                //given
                MultiValueMap<String, String> cond = new LinkedMultiValueMap<>();
                cond.add("name", "ㅇㅈ");
                cond.add("loginId", "a");
                List<MemberQueryDto> memberQueryDtoList = List.of(MemberQueryDto.builder().loginId("id_A").build());

                given(memberQueryService.searchMembers(any(MemberSearchCond.class))).willReturn(memberQueryDtoList);

                //when
                mvc.perform(get("/members")
                                .params(cond))
                        .andExpect(status().isOk())
                        .andDo(print());
            }
        }
    }
}