package lsk.commerce.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Member;
import lsk.commerce.dto.request.MemberChangeAddressRequest;
import lsk.commerce.dto.request.MemberChangePasswordRequest;
import lsk.commerce.dto.request.MemberRequest;
import lsk.commerce.dto.response.MemberResponse;
import lsk.commerce.query.MemberQueryService;
import lsk.commerce.query.dto.MemberQueryDto;
import lsk.commerce.query.dto.MemberSearchCond;
import lsk.commerce.service.MemberService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final MemberQueryService memberQueryService;

    @PostMapping("/members")
    public String create(@Valid MemberRequest request) {
        String loginId = memberService.join(request);
        return loginId + " created";
    }

    @GetMapping("/members")
    public List<MemberQueryDto> memberList(@ModelAttribute MemberSearchCond cond) {
        return memberQueryService.searchMembers(cond);
    }

    @GetMapping("/members/{memberLoginId}")
    public MemberQueryDto findMember(@PathVariable("memberLoginId") String memberLoginId) {
        return memberQueryService.findMember(memberLoginId);
    }

    @PostMapping("/members/{memberLoginId}/password")
    public MemberResponse changePassword(@PathVariable("memberLoginId") String memberLoginId, @Valid MemberChangePasswordRequest form) {
        Member member = memberService.changePassword(memberLoginId, form.getPassword());
        return memberService.getMemberDto(member);
    }

    @PostMapping("/members/{memberLoginId}/address")
    public MemberResponse changeAddress(@PathVariable("memberLoginId") String memberLoginId, @Valid MemberChangeAddressRequest form) {
        Member member = memberService.changeAddress(memberLoginId, form.getCity(), form.getStreet(), form.getZipcode());
        return memberService.getMemberDto(member);
    }

    @DeleteMapping("/members/{memberLoginId}")
    public String delete(@PathVariable("memberLoginId") String memberLoginId) {
        memberService.deleteMember(memberLoginId);
        return "delete";
    }
}
