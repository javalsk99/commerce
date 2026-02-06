package lsk.commerce.service;

import lsk.commerce.domain.Grade;
import lsk.commerce.domain.Member;
import lsk.commerce.dto.response.MemberResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberServiceTest {

    @Autowired
    MemberService memberService;

    @Test
    void join() {
        //given
        Member member = createMember1();

        //when
        Long memberId = memberService.join(member);

        //then
        Member findMember = memberService.findMember(memberId);
        assertThat(memberId).isEqualTo(member.getId());
        assertThat(findMember.getGrade()).isEqualTo(Grade.USER);
    }

    @Test
    void adminJoin() {
        //given
        Member member = createMember1();

        //when
        Long memberId = memberService.adminJoin(member);

        //then
        Member findMember = memberService.findMember(memberId);
        assertThat(memberId).isEqualTo(member.getId());
        assertThat(findMember.getGrade()).isEqualTo(Grade.ADMIN);
    }

    @Test
    void duplicate_join() {
        //given
        Member member1 = createMember1();
        Member member2 = createMember2();

        //when
        memberService.join(member1);

        //then
        assertThrows(IllegalArgumentException.class, () -> {
            memberService.join(member2);
        });
    }

    @Test
    void find() {
        //given
        Member member1 = createMember1();
        Member member2 = createMember3();
        Member member3 = createMember4();

        Long memberId1 = memberService.join(member1);
        memberService.join(member2);
        memberService.join(member3);

        //when
        Member findMember1 = memberService.findMember(memberId1);
        Member findMember2 = memberService.findMemberByLoginId(member2.getLoginId());
        Member findMember3 = memberService.findMemberForLogin(member3.getLoginId());
        List<Member> findMembers = memberService.findMembers();

        //then
        assertThat(findMember1.getLoginId()).isEqualTo("idA");
        assertThat(findMember2.getLoginId()).isEqualTo("idC");
        assertThat(findMember3.getLoginId()).isEqualTo("idD");
        assertThat(findMembers)
                .extracting("loginId")
                .containsExactlyInAnyOrder("idA", "idC", "idD", "testId");
    }

    @Test
    void delete() {
        //given
        Member member = createMember1();
        Long memberId = memberService.join(member);

        //when
        memberService.deleteMember(member);
        Member findMember = memberService.findMember(memberId);

        //then
        assertThat(findMember).isNull();
    }

    @Test
    void change() {
        //given
        Member member = createMember1();
        Long memberId = memberService.join(member);
        Member findMember = memberService.findMember(memberId);

        //when
        memberService.changePassword(findMember.getLoginId(), "1234");
        memberService.changeAddress(findMember.getLoginId(), "seoul", "Gangseo", "01237");

        //then
        assertThat(findMember.getPassword()).isEqualTo("1234");
        assertThat(findMember.getAddress().getStreet()).isEqualTo("Gangseo");
    }

    @Test
    void change_dto() {
        //given
        Member member = createMember1();
        Long memberId = memberService.join(member);
        Member findMember = memberService.findMember(memberId);

        //when
        MemberResponse memberDto = memberService.getMemberDto(findMember);

        //then
        assertThat(memberDto.getLoginId()).isEqualTo("idA");
        assertThat(memberDto.getGrade()).isEqualTo(Grade.USER);
    }

    private Member createMember1() {
        return new Member("userA", "idA", "0000", "Seoul", "Gangnam", "01234");
    }

    private Member createMember2() {
        return new Member("userB", "idA", "1111", "Seoul", "Gangbuk", "01235");
    }

    private Member createMember3() {
        return new Member("userC", "idC", "2222", "Seoul", "Gangdong", "01236");
    }

    private Member createMember4() {
        return new Member("userD", "idD", "3333", "Seoul", "Gangseo", "01237");
    }
}