package lsk.commerce.dto.response;

import com.querydsl.core.annotations.QueryProjection;
import lsk.commerce.domain.Member;

@QueryProjection
public record MemberResponse(
        String loginId,
        String zipcode,
        String baseAddress,
        String detailAddress
) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getLoginId(),
                member.getAddress().getZipcode(),
                member.getAddress().getBaseAddress(),
                member.getAddress().getDetailAddress()
        );
    }
}
