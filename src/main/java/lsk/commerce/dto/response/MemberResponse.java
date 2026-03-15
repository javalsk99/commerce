package lsk.commerce.dto.response;

import lsk.commerce.domain.Member;

public record MemberResponse(
        String loginId,
        String city,
        String street,
        String zipcode
) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getLoginId(),
                member.getAddress().getCity(),
                member.getAddress().getStreet(),
                member.getAddress().getZipcode());
    }
}
