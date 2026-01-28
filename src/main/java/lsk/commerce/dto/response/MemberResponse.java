package lsk.commerce.dto.response;

import lombok.Getter;
import lsk.commerce.domain.Grade;
import lsk.commerce.domain.Member;
import lsk.commerce.domain.Order;

import java.util.ArrayList;
import java.util.List;

@Getter
public class MemberResponse {

    private String loginId;

    private Grade grade;

    private List<OrderResponse> orders = new ArrayList<>();

    public MemberResponse(String loginId, Grade grade, List<OrderResponse> orders) {
        this.loginId = loginId;
        this.grade = grade;
        this.orders = orders;
    }

    public static MemberResponse memberChangeDto(Member member) {
        List<OrderResponse> orderResponses = new ArrayList<>();

        for (Order order : member.getOrders()) {
            OrderResponse orderDto = OrderResponse.orderChangeResponse(order);
            orderResponses.add(orderDto);
        }

        return new MemberResponse(member.getLoginId(), member.getGrade(), orderResponses);
    }
}
