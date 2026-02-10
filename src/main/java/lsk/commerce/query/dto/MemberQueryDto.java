package lsk.commerce.query.dto;

import lombok.Getter;
import lombok.Setter;
import lsk.commerce.domain.Grade;
import lsk.commerce.domain.Member;
import lsk.commerce.domain.Order;

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

    public MemberQueryDto(String loginId, Grade grade, List<OrderQueryDto> orders) {
        this.loginId = loginId;
        this.grade = grade;
        this.orders = orders;
    }

    public static MemberQueryDto changeQueryDto(Member member) {
        List<OrderQueryDto> orderQueryDtoList = new ArrayList<>();
        for (Order order : member.getOrders()) {
            OrderQueryDto orderQueryDto = OrderQueryDto.changeQueryDto(order);
            orderQueryDtoList.add(orderQueryDto);
        }

        return new MemberQueryDto(member.getLoginId(), member.getGrade(), orderQueryDtoList);
    }
}
