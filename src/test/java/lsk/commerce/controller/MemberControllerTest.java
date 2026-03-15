package lsk.commerce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lsk.commerce.config.WebConfig;
import lsk.commerce.domain.Grade;
import lsk.commerce.domain.Member;
import lsk.commerce.dto.request.MemberChangeAddressRequest;
import lsk.commerce.dto.request.MemberChangePasswordRequest;
import lsk.commerce.dto.request.MemberRequest;
import lsk.commerce.dto.response.MemberResponse;
import lsk.commerce.exception.DataNotFoundException;
import lsk.commerce.query.MemberQueryService;
import lsk.commerce.query.dto.MemberQueryDto;
import lsk.commerce.query.dto.MemberSearchCond;
import lsk.commerce.query.dto.OrderQueryDto;
import lsk.commerce.service.MemberService;
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

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = MemberController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebConfig.class)
)
class MemberControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    MemberService memberService;

    @MockitoBean
    MemberQueryService memberQueryService;

    @Nested
    class Create {

        @Nested
        class SuccessCase {

            @Test
            void basic() throws Exception {
                //given
                MemberRequest request = createMemberRequest();
                String json = objectMapper.writeValueAsString(request);

                given(memberService.join(any(MemberRequest.class))).willReturn(request.loginId());

                //when & then
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

                //when & then
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
                MemberRequest request = createMemberRequest();
                String json = objectMapper.writeValueAsString(request);

                given(memberService.join(any(MemberRequest.class))).willThrow(new IllegalArgumentException("이미 사용 중인 아이디입니다"));

                //when & then
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

        private static MemberRequest createMemberRequest() {
            return MemberRequest.builder()
                    .name("User")
                    .loginId("id_A")
                    .password("00000000")
                    .city("Seoul")
                    .street("Gangnam")
                    .zipcode("01234")
                    .build();
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
                cond.add("loginId", "id");
                List<OrderQueryDto> orderQueryDtoList1 = List.of(OrderQueryDto.builder().loginId("id_A").build());
                List<OrderQueryDto> orderQueryDtoList2 = List.of(OrderQueryDto.builder().loginId("id_B").build());
                MemberQueryDto memberQueryDto1 = MemberQueryDto.builder().loginId("id_A").grade(Grade.USER).orderQueryDtoList(orderQueryDtoList1).build();
                MemberQueryDto memberQueryDto2 = MemberQueryDto.builder().loginId("id_B").grade(Grade.USER).orderQueryDtoList(orderQueryDtoList2).build();
                List<MemberQueryDto> memberQueryDtoList = List.of(memberQueryDto1, memberQueryDto2);

                given(memberQueryService.searchMembers(any(MemberSearchCond.class))).willReturn(memberQueryDtoList);

                //when & then
                mvc.perform(get("/members")
                                .params(cond))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data[0].loginId").value("id_A"))
                        .andExpect(jsonPath("$.data[0].grade").value("USER"))
                        .andExpect(jsonPath("$.data[0].orderQueryDtoList").exists())
                        .andExpect(jsonPath("$.data[1].loginId").value("id_B"))
                        .andExpect(jsonPath("$.data[1].grade").value("USER"))
                        .andExpect(jsonPath("$.data[1].orderQueryDtoList").exists())
                        .andExpect(jsonPath("$.count").value(2))
                        .andDo(print());

                //then
                BDDMockito.then(memberQueryService).should().searchMembers(any(MemberSearchCond.class));
            }

            @Test
            void notFound() throws Exception {
                //given
                MultiValueMap<String, String> cond = new LinkedMultiValueMap<>();
                cond.add("name", "a");
                cond.add("loginId", "b");

                given(memberQueryService.searchMembers(any(MemberSearchCond.class))).willReturn(Collections.emptyList());

                //when & then
                mvc.perform(get("/members")
                                .params(cond))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data").isEmpty())
                        .andExpect(jsonPath("count").value(0))
                        .andDo(print());

                //then
                BDDMockito.then(memberQueryService).should().searchMembers(any(MemberSearchCond.class));
            }
        }
    }

    @Nested
    class FindMember {

        @Nested
        class SuccessCase {

            @Test
            void basic() throws Exception {
                //given
                List<OrderQueryDto> orderQueryDtoList = List.of(OrderQueryDto.builder().loginId("id_A").build());
                MemberQueryDto memberQueryDto = MemberQueryDto.builder().loginId("id_A").grade(Grade.USER).orderQueryDtoList(orderQueryDtoList).build();

                given(memberQueryService.findMember(anyString())).willReturn(memberQueryDto);

                //when & then
                mvc.perform(get("/members/{memberLoginId}", "id_A"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.loginId").value("id_A"))
                        .andExpect(jsonPath("$.data.grade").value("USER"))
                        .andExpect(jsonPath("$.data.orderQueryDtoList").exists())
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                BDDMockito.then(memberQueryService).should().findMember("id_A");
            }
        }

        @Nested
        class FailureCase {

            @Test
            void findMember_Failed_MemberNotFound() throws Exception {
                //given
                given(memberQueryService.findMember(anyString())).willThrow(new DataNotFoundException("존재하지 않는 아이디입니다"));

                //when & then
                mvc.perform(get("/members/{memberLoginId}", "id_C"))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                        .andExpect(jsonPath("$.message").value("존재하지 않는 아이디입니다"))
                        .andDo(print());

                //then
                BDDMockito.then(memberQueryService).should().findMember(anyString());
            }
        }
    }

    @Nested
    class ChangePassword {

        @Nested
        class SuccessCase {

            @Test
            void basic() throws Exception {
                //given
                MemberChangePasswordRequest request = new MemberChangePasswordRequest("11111111");
                String json = objectMapper.writeValueAsString(request);

                Member member = Member.builder()
                        .loginId("id_A")
                        .password("00000000")
                        .build();

                given(memberService.changePassword(anyString(), any(MemberChangePasswordRequest.class))).willReturn(member);

                //when & then
                mvc.perform(post("/members/{memberLoginId}/password", "id_A")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isOk())
                        .andExpect(content().string("비밀번호가 변경되었습니다."))
                        .andDo(print());

                //then
                BDDMockito.then(memberService).should().changePassword("id_A", request);
            }
        }

        @Nested
        class FailureCase {

            @ParameterizedTest
            @MethodSource("invalidPasswordProvider")
            void invalidInput(String password) throws Exception {
                //given
                MemberChangePasswordRequest request = new MemberChangePasswordRequest(password);
                String json = objectMapper.writeValueAsString(request);

                //when & then
                mvc.perform(post("/members/{memberLoginId}/password", "id_A")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isBadRequest())
                        .andDo(print());

                //then
                BDDMockito.then(memberService).should(never()).changePassword(any(), any());
            }

            @Test
            void changePassword_Failed_PasswordIsSame() throws Exception {
                //given
                MemberChangePasswordRequest request = new MemberChangePasswordRequest("00000000");
                String json = objectMapper.writeValueAsString(request);

                given(memberService.changePassword(anyString(), any(MemberChangePasswordRequest.class))).willThrow(new IllegalArgumentException("비밀번호가 기존과 달라야 합니다"));

                //when & then
                mvc.perform(post("/members/{memberLoginId}/password", "id_A")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value("BAD_ARGUMENT"))
                        .andExpect(jsonPath("$.message").value("비밀번호가 기존과 달라야 합니다"))
                        .andDo(print());

                //then
                BDDMockito.then(memberService).should().changePassword("id_A", request);
            }

            static Stream<Arguments> invalidPasswordProvider() {
                return Stream.of(
                        argumentSet("비밀번호 null", (String) null),
                        argumentSet("비밀번호 빈 문자열", ""),
                        argumentSet("비밀번호 공백", " ".repeat(8)),
                        argumentSet("비밀번호 7자 미만", "1".repeat(7)),
                        argumentSet("비밀번호 21자 초과", "1".repeat(21))
                );
            }
        }
    }

    @Nested
    class ChangeAddress {

        @Nested
        class SuccessCase {

            @ParameterizedTest
            @MethodSource("addressProvider")
            void basic(String city, String street, String zipcode) throws Exception {
                //given
                MemberChangeAddressRequest request = new MemberChangeAddressRequest(city, street, zipcode);
                String json = objectMapper.writeValueAsString(request);

                Member member = Member.builder()
                        .loginId("id_A")
                        .city("Seoul")
                        .street("Gangnam")
                        .zipcode("01234")
                        .build();
                MemberResponse memberResponse = new MemberResponse("id_A", city, street, zipcode);

                given(memberService.changeAddress(anyString(), any(MemberChangeAddressRequest.class))).willReturn(member);
                given(memberService.getMemberDto(any(Member.class))).willReturn(memberResponse);

                //when & then
                mvc.perform(post("/members/{memberLoginId}/address", "id_A")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.city").value(city))
                        .andExpect(jsonPath("$.data.street").value(street))
                        .andExpect(jsonPath("$.data.zipcode").value(zipcode))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(memberService).should().changeAddress("id_A", request));
                    softly.check(() -> BDDMockito.then(memberService).should().getMemberDto(member));
                });
            }

            static Stream<Arguments> addressProvider() {
                return Stream.of(
                        argumentSet("city만 변경", "Gyeonggi-do", "Gangnam", "01234"),
                        argumentSet("street만 변경", "Seoul", "Gangbuk", "01234"),
                        argumentSet("zipcode만 변경", "Seoul", "Gangnam", "01235"),
                        argumentSet("전부 변경", "Gyeonggi-do", "Gangbuk", "01235")
                );
            }
        }

        @Nested
        class FailureCase {

            @ParameterizedTest
            @MethodSource("invalidAddressProvider")
            void invalidInput(String city, String street, String zipcode) throws Exception {
                //given
                MemberChangeAddressRequest request = new MemberChangeAddressRequest(city, street, zipcode);
                String json = objectMapper.writeValueAsString(request);

                //when & then
                mvc.perform(post("/members/{memberLoginId}/address", "id_A")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isBadRequest())
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(memberService).should(never()).changeAddress(any(), any(MemberChangeAddressRequest.class)));
                    softly.check(() -> BDDMockito.then(memberService).should(never()).getMemberDto(any()));
                });
            }

            @Test
            void changeAddress_Failed_AddressIsSame() throws Exception {
                //given
                MemberChangeAddressRequest request = new MemberChangeAddressRequest("Seoul", "Gangnam", "01234");
                String json = objectMapper.writeValueAsString(request);

                given(memberService.changeAddress(anyString(), any(MemberChangeAddressRequest.class))).willThrow(new IllegalArgumentException("주소가 기존과 달라야 합니다"));

                //when & then
                mvc.perform(post("/members/{memberLoginId}/address", "id_A")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value("BAD_ARGUMENT"))
                        .andExpect(jsonPath("$.message").value("주소가 기존과 달라야 합니다"))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(memberService).should().changeAddress("id_A", request));
                    softly.check(() -> BDDMockito.then(memberService).should(never()).getMemberDto(any()));
                });
            }

            static Stream<Arguments> invalidAddressProvider() {
                return Stream.of(
                        argumentSet("city null", null, "Gangnam", "01234"),
                        argumentSet("street null", "Seoul", null, "01234"),
                        argumentSet("zipcode null", "Seoul", "Gangnam", null),
                        argumentSet("city 빈 문자열", "", "Gangnam", "01234"),
                        argumentSet("city 공백", " ", "Gangnam", "01234"),
                        argumentSet("city 50자 초과", "a".repeat(51), "Gangnam", "01234")
                );
            }
        }
    }

    @Nested
    class Delete {

        @Nested
        class SuccessCase {

            @Test
            void basic() throws Exception {
                //when & then
                mvc.perform(delete("/members/{memberLoginId}", "id_A"))
                        .andExpect(status().isOk())
                        .andExpect(content().string("delete"))
                        .andDo(print());

                //then
                BDDMockito.then(memberService).should().deleteMember("id_A");
            }
        }

        @Nested
        class FailureCase {

            @Test
            void deleteMember_Failed_MemberNotFound() throws Exception {
                //given
                willThrow(new IllegalArgumentException("존재하지 않는 아이디입니다")).given(memberService).deleteMember(anyString());

                //when & then
                mvc.perform(delete("/members/{memberLoginId}", "id_C"))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value("BAD_ARGUMENT"))
                        .andExpect(jsonPath("$.message").value("존재하지 않는 아이디입니다"))
                        .andDo(print());

                //then
                BDDMockito.then(memberService).should().deleteMember("id_C");
            }
        }
    }
}