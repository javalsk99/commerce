package lsk.commerce.dto.response;

import lombok.Getter;
import lsk.commerce.domain.Grade;

@Getter
public class MemberLoginResponse {

    private String loginId;

    private Grade grade;

    public MemberLoginResponse(String loginId, Grade grade) {
        this.loginId = loginId;
        this.grade = grade;
    }
}
