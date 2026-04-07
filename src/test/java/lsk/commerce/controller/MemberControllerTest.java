package lsk.commerce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lsk.commerce.config.WebConfig;
import lsk.commerce.domain.Role;
import lsk.commerce.domain.Member;
import lsk.commerce.dto.request.MemberChangeAddressRequest;
import lsk.commerce.dto.request.MemberChangePasswordRequest;
import lsk.commerce.dto.request.MemberCreateRequest;
import lsk.commerce.dto.response.MemberResponse;
import lsk.commerce.exception.DataNotFoundException;
import lsk.commerce.exception.DuplicateResourceException;
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
import static org.mockito.BDDMockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
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
                MemberCreateRequest request = createMemberCreateRequest();
                String json = objectMapper.writeValueAsString(request);

                given(memberService.join(any(MemberCreateRequest.class))).willReturn(request.loginId());

                //when & then
                mvc.perform(post("/members")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data").value("id_A"))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                BDDMockito.then(memberService).should().join(request);
            }
        }

        @Nested
        class FailureCase {

            @ParameterizedTest
            @MethodSource("invalidCreateRequestProvider")
            void invalidInput(MemberCreateRequest request) throws Exception {
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
                MemberCreateRequest request = createMemberCreateRequest();
                String json = objectMapper.writeValueAsString(request);

                given(memberService.join(any(MemberCreateRequest.class))).willThrow(new DuplicateResourceException("이미 사용 중인 아이디입니다. loginId: " + "id_A"));

                //when & then
                mvc.perform(post("/members")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isConflict())
                        .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"))
                        .andExpect(jsonPath("$.message").value("이미 사용 중인 아이디입니다. loginId: " + "id_A"))
                        .andDo(print());

                //then
                BDDMockito.then(memberService).should().join(request);
            }

            static Stream<Arguments> invalidCreateRequestProvider() {
                return Stream.of(
                        argumentSet("name null", MemberCreateRequest.builder().loginId("id_A").password("abAB12!@").zipcode("01234").baseAddress("서울시 강남구").detailAddress("101동 101호").build()),
                        argumentSet("loginId null", MemberCreateRequest.builder().name("User").password("abAB12!@").zipcode("01234").baseAddress("서울시 강남구").detailAddress("101동 101호").build()),
                        argumentSet("password null", MemberCreateRequest.builder().name("User").loginId("id_A").zipcode("01234").baseAddress("서울시 강남구").detailAddress("101동 101호").build()),
                        argumentSet("zipcode null", MemberCreateRequest.builder().name("User").loginId("id_A").password("abAB12!@").baseAddress("서울시 강남구").detailAddress("101동 101호").build()),
                        argumentSet("baseAddress null", MemberCreateRequest.builder().name("User").loginId("id_A").password("abAB12!@").zipcode("01234").detailAddress("101동 101호").build()),
                        argumentSet("detailAddress null", MemberCreateRequest.builder().name("User").loginId("id_A").password("abAB12!@").zipcode("01234").baseAddress("서울시 강남구").build()),
                        argumentSet("password 빈 문자열", MemberCreateRequest.builder().name("User").loginId("id_A").password("").zipcode("01234").baseAddress("서울시 강남구").detailAddress("101동 101호").build()),
                        argumentSet("password 공백", MemberCreateRequest.builder().name("User").loginId("id_A").password(" ".repeat(8)).zipcode("01234").baseAddress("서울시 강남구").detailAddress("101동 101호").build()),
                        argumentSet("password 8자 미만", MemberCreateRequest.builder().name("User").loginId("id_A").password("0".repeat(7)).zipcode("01234").baseAddress("서울시 강남구").detailAddress("101동 101호").build()),
                        argumentSet("비밀번호 패턴 불일치", MemberCreateRequest.builder().name("User").loginId("id_A").password("00000000").zipcode("01234").baseAddress("서울시 강남구").detailAddress("101동 101호").build()),
                        argumentSet("password 20자 초과", MemberCreateRequest.builder().name("User").loginId("id_A").password("0".repeat(21)).zipcode("01234").baseAddress("서울시 강남구").detailAddress("101동 101호").build())
                );
            }
        }

        private static MemberCreateRequest createMemberCreateRequest() {
            return MemberCreateRequest.builder()
                    .name("User")
                    .loginId("id_A")
                    .password("abAB12!@")
                    .zipcode("01234")
                    .baseAddress("서울시 강남구")
                    .detailAddress("101동 101호")
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
                MemberResponse memberResponse1 = new MemberResponse("id_A", "01234", "서울시 강남구", "101동 101호");
                MemberResponse memberResponse2 = new MemberResponse("id_B", "01235", "서울시 강북구", "101동 102호");
                List<MemberResponse> memberResponseList = List.of(memberResponse1, memberResponse2);

                given(memberQueryService.searchMembers(any(MemberSearchCond.class))).willReturn(memberResponseList);

                //when & then
                mvc.perform(get("/members")
                                .params(cond))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data[0].loginId").value("id_A"))
                        .andExpect(jsonPath("$.data[0].zipcode").value("01234"))
                        .andExpect(jsonPath("$.data[0].baseAddress").value("서울시 강남구"))
                        .andExpect(jsonPath("$.data[0].detailAddress").value("101동 101호"))
                        .andExpect(jsonPath("$.data[1].loginId").value("id_B"))
                        .andExpect(jsonPath("$.data[1].zipcode").value("01235"))
                        .andExpect(jsonPath("$.data[1].baseAddress").value("서울시 강북구"))
                        .andExpect(jsonPath("$.data[1].detailAddress").value("101동 102호"))
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
                MemberQueryDto memberQueryDto = MemberQueryDto.builder().loginId("id_A").role(Role.USER).orderQueryDtoList(orderQueryDtoList).build();

                given(memberQueryService.findMember(anyString())).willReturn(memberQueryDto);

                //when & then
                mvc.perform(get("/members/{memberLoginId}", "id_A"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.loginId").value("id_A"))
                        .andExpect(jsonPath("$.data.role").value("USER"))
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
                mvc.perform(get("/members/{memberLoginId}", "id_D"))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                        .andExpect(jsonPath("$.message").value("존재하지 않는 아이디입니다"))
                        .andDo(print());

                //then
                BDDMockito.then(memberQueryService).should().findMember("id_D");
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
                MemberChangePasswordRequest request = new MemberChangePasswordRequest("cdCD34#$");
                String json = objectMapper.writeValueAsString(request);

                Member member = Member.builder()
                        .loginId("id_A")
                        .password("cdCD34#$")
                        .build();

                given(memberService.changePassword(anyString(), any(MemberChangePasswordRequest.class))).willReturn(member);

                //when & then
                mvc.perform(post("/members/{memberLoginId}/password", "id_A")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data").value("비밀번호가 변경되었습니다"))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                BDDMockito.then(memberService).should().changePassword("id_A", request);
            }
        }

        @Nested
        class FailureCase {

            @ParameterizedTest
            @MethodSource("invalidPasswordRequestProvider")
            void invalidInput(MemberChangePasswordRequest request) throws Exception {
                //given
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
                MemberChangePasswordRequest request = new MemberChangePasswordRequest("abAB12!@");
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

            static Stream<Arguments> invalidPasswordRequestProvider() {
                return Stream.of(
                        argumentSet("비밀번호 null", new MemberChangePasswordRequest(null)),
                        argumentSet("비밀번호 빈 문자열", new MemberChangePasswordRequest("")),
                        argumentSet("비밀번호 공백", new MemberChangePasswordRequest(" ".repeat(8))),
                        argumentSet("비밀번호 7자 미만", new MemberChangePasswordRequest("1".repeat(7))),
                        argumentSet("비밀번호 패턴 불일치", new MemberChangePasswordRequest("00000000")),
                        argumentSet("비밀번호 21자 초과", new MemberChangePasswordRequest("1".repeat(21)))
                );
            }
        }
    }

    @Nested
    class ChangeAddress {

        @Nested
        class SuccessCase {

            @Test
            void shouldReturnOk_WhenAddressIsSame() throws Exception {
                //given
                MemberChangeAddressRequest request = new MemberChangeAddressRequest("01234", "서울시 강남구", "101동 101호");
                String json = objectMapper.writeValueAsString(request);

                Member member = createMember();
                MemberResponse memberResponse = new MemberResponse("id_A", "01234", "서울시 강남구", "101동 101호");

                given(memberService.changeAddress(anyString(), any(MemberChangeAddressRequest.class))).willReturn(member);
                given(memberService.getMemberResponse(any(Member.class))).willReturn(memberResponse);

                //when & then
                mvc.perform(patch("/members/{memberLoginId}/address", "id_A")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.zipcode").value("01234"))
                        .andExpect(jsonPath("$.data.baseAddress").value("서울시 강남구"))
                        .andExpect(jsonPath("$.data.detailAddress").value("101동 101호"))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(memberService).should().changeAddress("id_A", request));
                    softly.check(() -> BDDMockito.then(memberService).should().getMemberResponse(member));
                });
            }

            @ParameterizedTest
            @MethodSource("addressRequestProvider")
            void shouldReturnOk_WhenAddressFieldsAreDifferent(MemberChangeAddressRequest request) throws Exception {
                //given
                String json = objectMapper.writeValueAsString(request);

                Member member = createMember();
                MemberResponse memberResponse = new MemberResponse("id_A", request.zipcode(), request.baseAddress(), request.detailAddress());

                given(memberService.changeAddress(anyString(), any(MemberChangeAddressRequest.class))).willReturn(member);
                given(memberService.getMemberResponse(any(Member.class))).willReturn(memberResponse);

                //when & then
                mvc.perform(patch("/members/{memberLoginId}/address", "id_A")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.zipcode").value(request.zipcode()))
                        .andExpect(jsonPath("$.data.baseAddress").value(request.baseAddress()))
                        .andExpect(jsonPath("$.data.detailAddress").value(request.detailAddress()))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(memberService).should().changeAddress("id_A", request));
                    softly.check(() -> BDDMockito.then(memberService).should().getMemberResponse(member));
                });
            }

            @Test
            void idempotency() throws Exception {
                //given
                MemberChangeAddressRequest request = new MemberChangeAddressRequest("01234", "서울시 강북구", "101동 101호");
                String json = objectMapper.writeValueAsString(request);

                Member member = createMember();
                MemberResponse memberResponse = new MemberResponse("id_A", "01234", "서울시 강북구", "101동 101호");

                given(memberService.changeAddress(anyString(), any(MemberChangeAddressRequest.class))).willReturn(member);
                given(memberService.getMemberResponse(any(Member.class))).willReturn(memberResponse);

                //when & then 첫 번째 요청
                mvc.perform(patch("/members/{memberLoginId}/address", "id_A")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.zipcode").value("01234"))
                        .andExpect(jsonPath("$.data.baseAddress").value("서울시 강북구"))
                        .andExpect(jsonPath("$.data.detailAddress").value("101동 101호"))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(memberService).should().changeAddress("id_A", request));
                    softly.check(() -> BDDMockito.then(memberService).should().getMemberResponse(member));
                });

                //when & then 두 번째 요청
                mvc.perform(patch("/members/{memberLoginId}/address", "id_A")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.zipcode").value("01234"))
                        .andExpect(jsonPath("$.data.baseAddress").value("서울시 강북구"))
                        .andExpect(jsonPath("$.data.detailAddress").value("101동 101호"))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(memberService).should(times(2)).changeAddress("id_A", request));
                    softly.check(() -> BDDMockito.then(memberService).should(times(2)).getMemberResponse(member));
                });
            }

            static Stream<Arguments> addressRequestProvider() {
                return Stream.of(
                        argumentSet("zipcode만 변경", new MemberChangeAddressRequest("01235", "서울시 강남구", "101동 101호")),
                        argumentSet("baseAddress만 변경", new MemberChangeAddressRequest("01234", "서울시 강북구", "101동 101호")),
                        argumentSet("detailAddress만 변경", new MemberChangeAddressRequest("01234", "서울시 강남구", "101동 102호")),
                        argumentSet("전부 변경", new MemberChangeAddressRequest("01235", "서울시 강북구", "101동 102호"))
                );
            }
        }

        @Nested
        class FailureCase {

            @ParameterizedTest
            @MethodSource("invalidAddressRequestProvider")
            void invalidInput(MemberChangeAddressRequest request) throws Exception {
                //given
                String json = objectMapper.writeValueAsString(request);

                //when & then
                mvc.perform(patch("/members/{memberLoginId}/address", "id_A")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isBadRequest())
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(memberService).should(never()).changeAddress(any(), any(MemberChangeAddressRequest.class)));
                    softly.check(() -> BDDMockito.then(memberService).should(never()).getMemberResponse(any()));
                });
            }

            static Stream<Arguments> invalidAddressRequestProvider() {
                return Stream.of(
                        argumentSet("zipcode null", new MemberChangeAddressRequest(null, "서울시 강남구", "101동 101호")),
                        argumentSet("baseAddress null", new MemberChangeAddressRequest("01234", null, "101동 101호")),
                        argumentSet("detailAddress null", new MemberChangeAddressRequest("01234", "서울시 강남구", null)),
                        argumentSet("baseAddress 빈 문자열", new MemberChangeAddressRequest("01234", "", "101동 101호")),
                        argumentSet("baseAddress 공백", new MemberChangeAddressRequest("01234", " ", "101동 101호")),
                        argumentSet("baseAddress 패턴 불일치", new MemberChangeAddressRequest("01234", "ㄱ", "101동 101호")),
                        argumentSet("baseAddress 50자 초과", new MemberChangeAddressRequest("01234", "a".repeat(51), "101동 101호"))
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
                //given
                createMember();

                //when & then
                mvc.perform(delete("/members/{memberLoginId}", "id_A"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data").value("delete"))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                BDDMockito.then(memberService).should().deleteMember("id_A");
            }

            @Test
            void idempotency() throws Exception {
                //given
                createMember();

                //when & then 첫 번째 요청
                mvc.perform(delete("/members/{memberLoginId}", "id_A"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data").value("delete"))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                BDDMockito.then(memberService).should().deleteMember("id_A");

                //when & then 두 번째 요청
                mvc.perform(delete("/members/{memberLoginId}", "id_A"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data").value("delete"))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                BDDMockito.then(memberService).should(times(2)).deleteMember("id_A");
            }
        }
    }

    private static Member createMember() {
        return Member.builder()
                .loginId("id_A")
                .zipcode("01234")
                .baseAddress("서울시 강남구")
                .detailAddress("101동 101호")
                .build();
    }
}