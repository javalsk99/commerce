package lsk.commerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lsk.commerce.argumentresolver.Login;
import lsk.commerce.domain.Order;
import lsk.commerce.dto.request.OrderChangeRequest;
import lsk.commerce.dto.request.OrderCreateRequest;
import lsk.commerce.dto.response.OrderCancelResponse;
import lsk.commerce.dto.response.OrderChangeResponse;
import lsk.commerce.dto.response.OrderSearchResponse;
import lsk.commerce.dto.response.Result;
import lsk.commerce.query.OrderQueryService;
import lsk.commerce.query.dto.OrderQueryDto;
import lsk.commerce.query.dto.OrderSearchCond;
import lsk.commerce.service.OrderService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "05. 주문", description = "생성, 검색, 수정, 삭제, 취소")
@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderQueryService orderQueryService;

    @Operation(
            summary = "주문 생성",
            description = "**상품 번호**와 주문할 **수량**을 입력해 주세요. \n\n" +
                    "주문할 상품은 한 가지 이상 넣어주세요. \n\n" +
                    "결제 테스트 진행을 위해 주문을 생성한 후, 응답으로 받은 **주문 번호**를 복사하여 다음 단계에서 활용하세요."
    )
    @PostMapping("/orders")
    public ResponseEntity<Result<String>> create(
            @RequestBody @Valid OrderCreateRequest request,
            @Parameter(hidden = true)
            @Login String loginId
    ) {
        String orderNumber = orderService.order(request, loginId);
        return ResponseEntity.status(HttpStatus.CREATED).body(new Result<>(orderNumber, 1));
    }

    @Operation(
            summary = "주문 검색",
            description = "**관리자**만 검색할 수 있습니다. \n\n" +
                    "검색 조건에 맞춰 조회합니다. \n\n" +
                    "원하지 않는 검색 조건은 비워주세요."
    )
    @GetMapping("/orders")
    public ResponseEntity<Result<List<OrderSearchResponse>>> orderList(@ParameterObject @ModelAttribute OrderSearchCond cond) {
        List<OrderSearchResponse> orderSearchResponseList = orderQueryService.searchOrders(cond);
        return ResponseEntity.ok(new Result<>(orderSearchResponseList, orderSearchResponseList.size()));
    }

    @Operation(
            summary = "주문 상세 조회",
            description = "**관리자**만 조회할 수 있습니다. \n\n" +
                    "주문의 상세 정보를 조회합니다."
    )
    @GetMapping("/orders/{orderNumber}")
    public ResponseEntity<Result<OrderQueryDto>> findOrder(
            @Parameter(description = "**12**자리의 주문 번호를 입력해 주세요.")
            @PathVariable("orderNumber") String orderNumber
    ) {
        OrderQueryDto orderQueryDto = orderQueryService.findOrder(orderNumber);
        return ResponseEntity.ok(new Result<>(orderQueryDto, 1));
    }

    @Operation(
            summary = "주문 수정",
            description = "**주문의 주인**만 수정할 수 있습니다. \n\n" +
                    "주문의 주인이 아니면 관리자도 수정할 수 없습니다. \n\n" +
                    "예시 주문은 주인이 아니어도 수정되지 않고 성공합니다. \n\n" +
                    "**취소된 주문**은 수정할 수 없습니다." +
                    "**회원 상세 조회**를 통해 주문이 있는지 확인하고 주문 번호를 입력해 주세요."
    )
    @PatchMapping("/orders/{orderNumber}")
    public ResponseEntity<Result<OrderChangeResponse>> changeOrder(
            @Parameter(description = "**12**자리의 주문 번호를 입력해 주세요.")
            @PathVariable("orderNumber") String orderNumber,
            @RequestBody @Valid OrderChangeRequest request,
            @Parameter(hidden = true)
            @Login String loginId
    ) {
        orderService.changeOrder(orderNumber, request, loginId);
        Order order = orderService.findOrderWithAllExceptMember(orderNumber);
        OrderChangeResponse orderChangeResponse = orderService.getOrderChangeResponse(order);
        return ResponseEntity.ok(new Result<>(orderChangeResponse, 1));
    }

    @Operation(
            summary = "주문 삭제",
            description = "**주문의 주인**만 삭제할 수 있습니다. (**관리자 삭제 불가**) \n\n" +
                    "주문의 주인이 아니면 관리자도 삭제할 수 없습니다. \n\n" +
                    "예시 주문은 주인이 아니어도 삭제되지 않고 성공합니다. \n\n" +
                    "**회원 상세 조회**를 통해 주문이 있는지 확인하고 주문 번호를 입력해 주세요." +
                    "**결제가 완료되지 않은 주문**은 **취소**해야 삭제할 수 있습니다. \n\n" +
                    "**결제가 완료된 주문**은 **배송이 완료**돼야 삭제할 수 있습니다."
    )
    @DeleteMapping("/orders/{orderNumber}")
    public ResponseEntity<Result<String>> delete(
            @Parameter(description = "**12**자리의 주문 번호를 입력해 주세요.")
            @PathVariable("orderNumber") String orderNumber,
            @Parameter(hidden = true)
            @Login String loginId
    ) {
        orderService.deleteOrder(orderNumber, loginId);
        return ResponseEntity.ok(new Result<>("delete", 1));
    }

    @Operation(
            summary = "주문 취소",
            description = "**주문의 주인**만 취소할 수 있습니다. \n\n" +
                    "주문의 주인이 아니면 관리자도 취소할 수 없습니다. \n\n" +
                    "예시 주문은 주인이 아니어도 취소되지 않고 성공합니다. \n\n" +
                    "**회원 상세 조회**를 통해 주문이 있는지 확인하고 주문 번호를 입력해 주세요. \n\n" +
                    "**결제가 완료되지 않은 주문**만 취소할 수 있습니다."
    )
    @PatchMapping("/orders/{orderNumber}/cancel")
    public ResponseEntity<Result<OrderCancelResponse>> cancelOrder(
            @Parameter(description = "**12**자리의 주문 번호를 입력해 주세요.")
            @PathVariable("orderNumber") String orderNumber,
            @Parameter(hidden = true)
            @Login String loginId
    ) {
        Order canceledOrder = orderService.cancelOrder(orderNumber, loginId);
        OrderCancelResponse orderCancelResponse = orderService.getOrderCancelResponse(canceledOrder);
        return ResponseEntity.ok(new Result<>(orderCancelResponse, 1));
    }
}
