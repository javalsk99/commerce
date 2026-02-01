package lsk.commerce.dto.response;

import lombok.Getter;
import lsk.commerce.domain.Grade;
import lsk.commerce.domain.Member;

@Getter
public class MemberResponse {

    private String loginId;

    private Grade grade;

    public MemberResponse(String loginId, Grade grade) {
        this.loginId = loginId;
        this.grade = grade;
    }

    public static MemberResponse memberChangeDto(Member member) {
        return new MemberResponse(member.getLoginId(), member.getGrade());
    }
}
