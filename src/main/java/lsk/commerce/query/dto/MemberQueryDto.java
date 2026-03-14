package lsk.commerce.query.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lsk.commerce.domain.Grade;
import lsk.commerce.domain.Member;

import java.util.ArrayList;
import java.util.List;

@Builder(toBuilder = true)
public record MemberQueryDto(
        String loginId,
        Grade grade,
        List<OrderQueryDto> orderQueryDtoList
) {

    public MemberQueryDto(String loginId, Grade grade) {
        this(loginId, grade, new ArrayList<>());
    }
}
