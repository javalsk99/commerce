package lsk.commerce.service;

import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import lsk.commerce.domain.Member;
import lsk.commerce.dto.request.MemberRequest;
import lsk.commerce.dto.response.MemberResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Stream;

import static lsk.commerce.domain.Grade.ADMIN;
import static lsk.commerce.domain.Grade.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@Transactional
@SpringBootTest
class MemberServiceTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberService memberService;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void join() {
        //given
        MemberRequest request1 = createMember1();
        MemberRequest request2 = createMember3();

        //when
        String loginId1 = memberService.join(request1);
        String loginId2 = memberService.adminJoin(request2);

        //then
        Member findMember1 = memberService.findMemberByLoginId(loginId1);
        Member findMember2 = memberService.findMemberByLoginId(loginId2);
        assertThat(findMember1.getGrade()).isEqualTo(USER);
        assertThat(findMember2.getGrade()).isEqualTo(ADMIN);
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("memberRequestProvider")
    void failed_join(MemberRequest request, String reason) {
        //when
        assertThatThrownBy(() -> memberService.join(request))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("passwordProvider")
    void failed_join_wrongPassword(String password, String reason) {
        //given
        MemberRequest request = createMember4(password);

        //when
        assertThatThrownBy(() -> memberService.join(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호가 비어있습니다.");
    }

    @Test
    void duplicate_join() {
        //given
        MemberRequest request1 = createMember1();
        MemberRequest request2 = createMember2();

        //when
        memberService.join(request1);

        //then
        assertThatThrownBy(() -> memberService.join(request2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 사용 중인 아이디입니다.");
    }

    @Test
    void find() {
        //given
        MemberRequest request1 = createMember1();
        MemberRequest request2 = createMember3();

        String loginId1 = memberService.join(request1);
        memberService.join(request2);

        //when
        Member findMember1 = memberService.findMemberByLoginId(loginId1);
        Member findMember2 = memberService.findMemberForLogin(request2.getLoginId());

        //then
        assertThat(findMember1.getLoginId()).isEqualTo("id_A");
        assertThat(findMember2.getLoginId()).isEqualTo("id_C");
    }

    @Test
    void failed_find() {
        //given
        MemberRequest request = createMember1();
        memberService.join(request);

        //when
        assertThatThrownBy(() -> memberService.findMemberByLoginId("id_B"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 아이디입니다.");
    }

    @Test
    void delete() {
        //given
        MemberRequest request = createMember1();
        String loginId = memberService.join(request);

        //when
        memberService.deleteMember(loginId);

        //then
        assertThatThrownBy(() -> memberService.findMemberByLoginId(loginId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 아이디입니다.");
    }

    @Test
    void failed_delete_alreadyDeleted() {
        //given
        MemberRequest request = createMember1();
        String loginId = memberService.join(request);
        memberService.deleteMember(loginId);

        //when
        assertThatThrownBy(() -> memberService.deleteMember(loginId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 아이디입니다.");
    }

    @Test
    void changePassword() {
        //given
        MemberRequest request = createMember1();
        String loginId = memberService.join(request);

        //when
        memberService.changePassword(loginId, "12345678");
        em.flush();

        //then
        Member findMember = memberService.findMemberByLoginId(loginId);
        assertThat(passwordEncoder.matches("12345678", findMember.getPassword())).isTrue();
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("passwordProvider")
    void failed_changePassword(String newPassword, String reason) {
        //given
        MemberRequest request = createMember1();
        String loginId = memberService.join(request);

        //when
        assertThatThrownBy(() -> memberService.changePassword(loginId, newPassword))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호가 비어있습니다.");
    }

    @Test
    void changeAddress() {
        //given
        MemberRequest request = createMember1();
        String loginId = memberService.join(request);

        //when
        memberService.changeAddress(loginId, "seoul", "Gangseo", "01237");
        em.flush();

        //then
        Member findMember = memberService.findMemberByLoginId(loginId);
        assertThat(findMember.getAddress().getStreet()).isEqualTo("Gangseo");
    }

    @Test
    void failed_changeAddress_sameAddress() {
        //given
        MemberRequest request = createMember1();
        String loginId = memberService.join(request);
        Member member = memberService.findMemberByLoginId(loginId);

        //when
        assertThatThrownBy(() -> memberService.changeAddress(loginId, member.getAddress().getCity(), member.getAddress().getStreet(), member.getAddress().getZipcode()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("주소가 기존과 달라야 합니다.");
    }

    @Test
    void changeDto() {
        //given
        MemberRequest request = createMember1();
        String loginId = memberService.join(request);
        Member findMember = memberService.findMemberByLoginId(loginId);

        //when
        MemberResponse memberDto = memberService.getMemberDto(findMember);

        //then
        assertThat(memberDto.getLoginId()).isEqualTo("id_A");
        assertThat(memberDto.getGrade()).isEqualTo(USER);
    }

    private MemberRequest createMember1() {
        return new MemberRequest("userA", "id_A", "00000000", "Seoul", "Gangnam", "01234");
    }

    private MemberRequest createMember2() {
        return new MemberRequest("userB", "id_A", "11111111", "Seoul", "Gangbuk", "01235");
    }

    private MemberRequest createMember3() {
        return new MemberRequest("userC", "id_C", "22222222", "Seoul", "Gangdong", "01236");
    }

    private MemberRequest createMember4(String password) {
        return MemberRequest.builder()
                .name("userD")
                .loginId("id_D")
                .password(password)
                .city("Seoul")
                .street("Gangseo")
                .zipcode("01237")
                .build();
    }

    static Stream<Arguments> memberRequestProvider() {
        return Stream.of(
                arguments(new MemberRequest(null, "loginId", "password", "city", "street", "zipcode"), "이름 null"),
                arguments(new MemberRequest("name", null, "password", "city", "street", "zipcode"), "아이디 null"),
                arguments(new MemberRequest("name", "", "password", "city", "street", "zipcode"), "아이디 빈 문자열"),
                arguments(new MemberRequest("name", "    ", "password", "city", "street", "zipcode"), "아이디 공백 4칸"),
                arguments(new MemberRequest("name", "abc", "password", "city", "street", "zipcode"), "아이디 4자 미만"),
                arguments(new MemberRequest("name", "abcdefghijklmnopqrstuvwxyz", "password", "city", "street", "zipcode"), "아이디 20자 초과"),
                arguments(new MemberRequest("name", "loginId", "password", null, "street", "zipcode"), "도시 null"),
                arguments(new MemberRequest("name", "loginId", "password", "city", null, "zipcode"), "거리명 null"),
                arguments(new MemberRequest("name", "loginId", "password", "city", "street", null), "우편번호 null")
        );
    }

    static Stream<Arguments> passwordProvider() {
        return Stream.of(
                arguments(null, "비밀번호 null"),
                arguments("", "비밀번호 빈 문자열"),
                arguments(" ", "비밀번호 공백")
        );
    }
}