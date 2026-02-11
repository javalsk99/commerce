package lsk.commerce.query.dto;

import lombok.Getter;
import lombok.Setter;
import lsk.commerce.domain.Grade;
import lsk.commerce.domain.Member;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class MemberQueryDto {

    private String loginId;
    private Grade grade;
    private List<OrderQueryDto> orders = new ArrayList<>();

    public MemberQueryDto(String loginId, Grade grade) {
        this.loginId = loginId;
        this.grade = grade;
    }

    public static MemberQueryDto changeQueryDto(Member member) {
        return new MemberQueryDto(member.getLoginId(), member.getGrade());
    }
}
