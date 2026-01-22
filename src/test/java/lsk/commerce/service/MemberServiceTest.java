package lsk.commerce.service;

import lsk.commerce.domain.Member;
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
        assertThat(memberId).isEqualTo(member.getId());
        assertThat(member.getName()).isEqualTo("userA");
    }

    @Test
    void duplicate_join() {
        //given
        Member member1 = createMember1();
        Member member2 = createMember2();

        //when
        memberService.join(member1);

        //then
        assertThrows(IllegalStateException.class, () -> {
            memberService.join(member2);
        });
    }

    @Test
    void find() {
        //given
        Member member1 = createMember1();
        Member member2 = createMember3();

        Long memberId1 = memberService.join(member1);
        Long memberId2 = memberService.join(member2);

        //when
        Member findMember1 = memberService.findMember(memberId1);
        Member findMember2 = memberService.findMember(memberId2);
        List<Member> findMembers = memberService.findMembers();

        //then
        assertThat(findMember1.getLoginId()).isEqualTo(member1.getLoginId());
        assertThat(findMember2.getLoginId()).isEqualTo(member2.getLoginId());
        assertThat(findMembers.size()).isEqualTo(2);
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

    private Member createMember1() {
        return new Member("userA", "idA", "0000", "Seoul", "Gangnam", "01234");
    }

    private Member createMember2() {
        return new Member("userB", "idA", "1111", "Seoul", "Gangbuk", "01235");
    }

    private Member createMember3() {
        return new Member("userC", "idC", "2222", "Seoul", "Gangdong", "01236");
    }
}