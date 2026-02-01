package lsk.commerce.dto.query;

import lombok.Data;
import lsk.commerce.domain.Grade;

import java.util.ArrayList;
import java.util.List;

@Data
public class MemberQueryDto {

    private String loginId;
    private Grade grade;
    private List<OrderQueryDto> orders = new ArrayList<>();

    public MemberQueryDto(String loginId, Grade grade) {
        this.loginId = loginId;
        this.grade = grade;
    }

    public MemberQueryDto(String loginId, Grade grade, List<OrderQueryDto> orders) {
        this.loginId = loginId;
        this.grade = grade;
        this.orders = orders;
    }
}
