package lsk.commerce.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Member;
import lsk.commerce.dto.request.MemberChangeAddressRequest;
import lsk.commerce.dto.request.MemberChangePasswordRequest;
import lsk.commerce.dto.request.MemberCreateRequest;
import lsk.commerce.dto.response.MemberResponse;
import lsk.commerce.dto.response.Result;
import lsk.commerce.query.MemberQueryService;
import lsk.commerce.query.dto.MemberQueryDto;
import lsk.commerce.query.dto.MemberSearchCond;
import lsk.commerce.service.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final MemberQueryService memberQueryService;

    @PostMapping("/members")
    public ResponseEntity<Result<String>> create(@RequestBody @Valid MemberCreateRequest request) {
        String loginId = memberService.join(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new Result<>(loginId, 1));
    }

    @GetMapping("/members")
    public ResponseEntity<Result<List<MemberQueryDto>>> memberList(@ModelAttribute MemberSearchCond cond) {
        List<MemberQueryDto> memberQueryDtoList = memberQueryService.searchMembers(cond);
        return ResponseEntity.ok(new Result<>(memberQueryDtoList, memberQueryDtoList.size()));
    }

    @GetMapping("/members/{memberLoginId}")
    public ResponseEntity<Result<MemberQueryDto>> findMember(@PathVariable("memberLoginId") String memberLoginId) {
        MemberQueryDto memberQueryDto = memberQueryService.findMember(memberLoginId);
        return ResponseEntity.ok(new Result<>(memberQueryDto, 1));
    }

    @PostMapping("/members/{memberLoginId}/password")
    public ResponseEntity<Result<String>> changePassword(
            @PathVariable("memberLoginId") String memberLoginId,
            @RequestBody @Valid MemberChangePasswordRequest request
    ) {
        memberService.changePassword(memberLoginId, request);
        return ResponseEntity.ok(new Result<>("비밀번호가 변경되었습니다", 1));
    }

    @PatchMapping("/members/{memberLoginId}/address")
    public ResponseEntity<Result<MemberResponse>> changeAddress(
            @PathVariable("memberLoginId") String memberLoginId,
            @RequestBody @Valid MemberChangeAddressRequest request
    ) {
        Member member = memberService.changeAddress(memberLoginId, request);
        MemberResponse memberResponse = memberService.getMemberDto(member);
        return ResponseEntity.ok(new Result<>(memberResponse, 1));
    }

    @DeleteMapping("/members/{memberLoginId}")
    public ResponseEntity<Result<String>> delete(@PathVariable("memberLoginId") String memberLoginId) {
        memberService.deleteMember(memberLoginId);
        return ResponseEntity.ok(new Result<>("delete", 1));
    }
}
