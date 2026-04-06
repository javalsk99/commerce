package lsk.commerce.query.dto;

import lombok.Builder;
import lsk.commerce.domain.Role;

import java.util.ArrayList;
import java.util.List;

@Builder(toBuilder = true)
public record MemberQueryDto(
        String loginId,
        Role role,
        List<OrderQueryDto> orderQueryDtoList
) {
    public MemberQueryDto(String loginId, Role role) {
        this(loginId, role, new ArrayList<>());
    }
}
