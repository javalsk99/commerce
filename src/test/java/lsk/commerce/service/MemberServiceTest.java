package lsk.commerce.service;

import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import lsk.commerce.domain.Member;
import lsk.commerce.dto.response.MemberResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Stream;

import static lsk.commerce.domain.Grade.ADMIN;
import static lsk.commerce.domain.Grade.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@Transactional
@SpringBootTest
class MemberServiceTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberService memberService;

    @Test
    void join() {
        //given
        Member member1 = createMember1();
        Member member2 = createMember3();

        //when
        String loginId1 = memberService.join(member1);
        String loginId2 = memberService.adminJoin(member2);

        //then
        Member findMember1 = memberService.findMemberByLoginId(loginId1);
        Member findMember2 = memberService.findMemberByLoginId(loginId2);
        assertThat(findMember1.getGrade()).isEqualTo(USER);
        assertThat(findMember2.getGrade()).isEqualTo(ADMIN);
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("memberProvider")
    void failed_join(Member member, String reason) {
        //when
        assertThrows(ConstraintViolationException.class, () ->
                memberService.join(member));
    }

    @Test
    void duplicate_join() {
        //given
        Member member1 = createMember1();
        Member member2 = createMember2();

        //when
        memberService.join(member1);

        //then
        assertThrows(IllegalArgumentException.class, () ->
                memberService.join(member2));
    }

    @Test
    void find() {
        //given
        Member member1 = createMember1();
        Member member2 = createMember3();

        String loginId1 = memberService.join(member1);
        memberService.join(member2);

        //when
        Member findMember1 = memberService.findMemberByLoginId(loginId1);
        Member findMember2 = memberService.findMemberForLogin(member2.getLoginId());

        //then
        assertThat(findMember1.getLoginId()).isEqualTo("id_A");
        assertThat(findMember2.getLoginId()).isEqualTo("id_C");
    }

    @Test
    void failed_find() {
        //given
        Member member = createMember1();
        memberService.join(member);

        //when
        assertThrows(IllegalArgumentException.class, () ->
                memberService.findMemberByLoginId("id_B"));
    }

    @Test
    void delete() {
        //given
        Member member = createMember1();
        String loginId = memberService.join(member);

        //when
        memberService.deleteMember(loginId);

        //then
        assertThrows(IllegalArgumentException.class, () ->
                memberService.findMemberByLoginId(loginId));
    }

    @Test
    void failed_delete_alreadyDeleted() {
        //given
        Member member = createMember1();
        String loginId = memberService.join(member);
        memberService.deleteMember(loginId);

        //when
        assertThrows(IllegalArgumentException.class, () ->
                memberService.deleteMember(loginId));
    }

    @Test
    void change_password() {
        //given
        Member member = createMember1();
        String loginId = memberService.join(member);
        Member findMember = memberService.findMemberByLoginId(loginId);

        //when
        memberService.changePassword(findMember.getLoginId(), "12345678");
        em.flush();

        //then
        assertThat(findMember.getPassword()).isEqualTo("12345678");
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("passwordProvider")
    void failed_change_password(String newPassword, String reason) {
        //given
        Member member = createMember1();
        String loginId = memberService.join(member);

        //when
        assertThrows(ConstraintViolationException.class, () -> {
            memberService.changePassword(loginId, newPassword);
            em.flush();
        });
    }

    @Test
    void change_address() {
        //given
        Member member = createMember1();
        String loginId = memberService.join(member);
        Member findMember = memberService.findMemberByLoginId(loginId);

        //when
        memberService.changeAddress(findMember.getLoginId(), "seoul", "Gangseo", "01237");
        em.flush();

        //then
        assertThat(findMember.getAddress().getStreet()).isEqualTo("Gangseo");
    }

    @Test
    void change_dto() {
        //given
        Member member = createMember1();
        String loginId = memberService.join(member);
        Member findMember = memberService.findMemberByLoginId(loginId);

        //when
        MemberResponse memberDto = memberService.getMemberDto(findMember);

        //then
        assertThat(memberDto.getLoginId()).isEqualTo("id_A");
        assertThat(memberDto.getGrade()).isEqualTo(USER);
    }

    private Member createMember1() {
        return new Member("userA", "id_A", "00000000", "Seoul", "Gangnam", "01234");
    }

    private Member createMember2() {
        return new Member("userB", "id_A", "11111111", "Seoul", "Gangbuk", "01235");
    }

    private Member createMember3() {
        return new Member("userC", "id_C", "22222222", "Seoul", "Gangdong", "01236");
    }

    private Member createMember4() {
        return new Member("userD", "id_D", "33333333", "Seoul", "Gangseo", "01237");
    }

    static Stream<Arguments> memberProvider() {
        return Stream.of(
                arguments(new Member(null, "loginId", "password", "city", "street", "zipcode"), "이름 null"),
                arguments(new Member("name", null, "password", "city", "street", "zipcode"), "아이디 null"),
                arguments(new Member("name", "", "password", "city", "street", "zipcode"), "아이디 빈 문자열"),
                arguments(new Member("name", "    ", "password", "city", "street", "zipcode"), "아이디 공백 4칸"),
                arguments(new Member("name", "abc", "password", "city", "street", "zipcode"), "아이디 4자 미만"),
                arguments(new Member("name", "abcdefghijklmnopqrstuvwxyz", "password", "city", "street", "zipcode"), "아이디 20자 초과"),
                arguments(new Member("name", "loginId", null, "city", "street", "zipcode"), "비밀번호 null"),
                arguments(new Member("name", "loginId", "password", null, "street", "zipcode"), "도시 null"),
                arguments(new Member("name", "loginId", "password", "city", null, "zipcode"), "거리명 null"),
                arguments(new Member("name", "loginId", "password", "city", "street", null), "우편번호 null")
        );
    }

    static Stream<Arguments> passwordProvider() {
        return Stream.of(
                arguments(null, "비밀번호 null"),
                arguments("", "비밀번호 빈 문자열"),
                arguments(" ", "비밀번호 공백"),
                arguments("abcdefg", "비밀번호 8자 미만")
        );
    }
}