package lsk.commerce.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lsk.commerce.controller.form.ChangeAddressMemberForm;
import lsk.commerce.controller.form.ChangePasswordMemberForm;
import lsk.commerce.controller.form.MemberForm;
import lsk.commerce.domain.Member;
import lsk.commerce.service.MemberService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/members")
    public String create(@Valid MemberForm form) {
        Member member = new Member(form.getName(), form.getLoginId(), form.getPassword(), form.getCity(), form.getStreet(), form.getZipcode());
        memberService.join(member);
        return member.getLoginId() + " created";
    }

    @GetMapping("/members")
    public List<MemberForm> memberList() {
        List<Member> members = memberService.findMembers();
        List<MemberForm> memberForms = new ArrayList<>();

        for (Member member : members) {
            MemberForm memberForm = MemberForm.memberChangeForm(member);
            memberForms.add(memberForm);
        }

        return memberForms;
    }

    @GetMapping("/members/{memberLoginId}")
    public MemberForm findMember(@PathVariable("memberLoginId") String memberLoginId) {
        if (memberService.findMemberByLoginId(memberLoginId) == null) {
            throw new IllegalArgumentException("잘못된 아이디를 입력했습니다. id: " + memberLoginId);
        }

        Member member = memberService.findMemberByLoginId(memberLoginId);
        return MemberForm.memberChangeForm(member);
    }

    @PostMapping("/members/{memberLoginId}/password")
    public MemberForm changePassword(@PathVariable("memberLoginId") String memberLoginId, @Valid ChangePasswordMemberForm form) {
        if (memberService.findMemberByLoginId(memberLoginId) == null) {
            throw new IllegalArgumentException("잘못된 아이디를 입력했습니다. id: " + memberLoginId);
        }

        Member member = memberService.findMemberByLoginId(memberLoginId);
        memberService.changePassword(member.getLoginId(), form.getPassword());
        return MemberForm.memberChangeForm(member);
    }

    @PostMapping("/members/{memberLoginId}/address")
    public MemberForm changeAddress(@PathVariable("memberLoginId") String memberLoginId, @Valid ChangeAddressMemberForm form) {
        if (memberService.findMemberByLoginId(memberLoginId) == null) {
            throw new IllegalArgumentException("잘못된 아이디를 입력했습니다. id: " + memberLoginId);
        }

        Member member = memberService.findMemberByLoginId(memberLoginId);
        memberService.changeAddress(member.getLoginId(), form.getCity(), form.getStreet(), form.getZipcode());
        return MemberForm.memberChangeForm(member);
    }

    @DeleteMapping("/members/{memberLoginId}")
    public String delete(@PathVariable("memberLoginId") String memberLoginId) {
        if (memberService.findMemberByLoginId(memberLoginId) == null) {
            throw new IllegalArgumentException("잘못된 아이디를 입력했습니다. id: " + memberLoginId);
        }

        memberService.deleteMember(memberService.findMemberByLoginId(memberLoginId));
        return "delete";
    }
}
