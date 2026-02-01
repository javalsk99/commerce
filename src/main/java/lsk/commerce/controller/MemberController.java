package lsk.commerce.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lsk.commerce.dto.query.MemberQueryDto;
import lsk.commerce.dto.request.MemberChangeAddressRequest;
import lsk.commerce.dto.request.MemberChangePasswordRequest;
import lsk.commerce.dto.request.MemberRequest;
import lsk.commerce.domain.Member;
import lsk.commerce.dto.response.MemberResponse;
import lsk.commerce.service.MemberService;
import lsk.commerce.service.query.MemberQueryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final MemberQueryService memberQueryService;

    @PostMapping("/members")
    public String create(@Valid MemberRequest request) {
        Member member = new Member(request.getName(), request.getLoginId(), request.getPassword(), request.getCity(), request.getStreet(), request.getZipcode());
        memberService.join(member);
        return member.getLoginId() + " created";
    }

    @GetMapping("/members")
    public List<MemberQueryDto> memberOrderList() {
        return memberQueryService.findMembersWithOrder();
    }

    @GetMapping("/members/{memberLoginId}")
    public MemberResponse findMember(@PathVariable("memberLoginId") String memberLoginId) {
        Member member = memberService.findMemberByLoginId(memberLoginId);
        return memberService.getMemberDto(member);
    }

    @PostMapping("/members/{memberLoginId}/password")
    public MemberResponse changePassword(@PathVariable("memberLoginId") String memberLoginId, @Valid MemberChangePasswordRequest form) {
        Member member = memberService.findMemberByLoginId(memberLoginId);
        memberService.changePassword(member.getLoginId(), form.getPassword());
        return memberService.getMemberDto(member);
    }

    @PostMapping("/members/{memberLoginId}/address")
    public MemberResponse changeAddress(@PathVariable("memberLoginId") String memberLoginId, @Valid MemberChangeAddressRequest form) {
        Member member = memberService.findMemberByLoginId(memberLoginId);
        memberService.changeAddress(member.getLoginId(), form.getCity(), form.getStreet(), form.getZipcode());
        return memberService.getMemberDto(member);
    }

    @DeleteMapping("/members/{memberLoginId}")
    public String delete(@PathVariable("memberLoginId") String memberLoginId) {
        memberService.deleteMember(memberService.findMemberByLoginId(memberLoginId));
        return "delete";
    }
}
