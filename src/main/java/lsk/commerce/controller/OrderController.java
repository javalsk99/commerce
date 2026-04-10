package lsk.commerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lsk.commerce.argumentresolver.Login;
import lsk.commerce.domain.Order;
import lsk.commerce.dto.request.OrderChangeRequest;
import lsk.commerce.dto.request.OrderCreateRequest;
import lsk.commerce.dto.response.OrderCancelResponse;
import lsk.commerce.dto.response.OrderChangeResponse;
import lsk.commerce.dto.response.OrderSearchResponse;
import lsk.commerce.dto.response.Result;
import lsk.commerce.exception.ErrorResult;
import lsk.commerce.query.OrderQueryService;
import lsk.commerce.query.dto.OrderQueryDto;
import lsk.commerce.query.dto.OrderSearchCond;
import lsk.commerce.service.OrderService;
import lsk.commerce.swagger.ApiAdminForbiddenResponse;
import lsk.commerce.swagger.ApiOrderOwnerForbiddenResponse;
import lsk.commerce.swagger.ApiUnauthorizedResponse;
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

@Tag(name = "05. 주문", description = "결제 테스트 진행을 위해 주문을 **생성**한 후, 응답으로 받은 **주문 번호**를 복사하여 다음 단계에서 활용하세요.")
@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderQueryService orderQueryService;

    @Operation(
            summary = "주문 생성",
            description = "**상품 번호**: (필수, 중복 불가) 영문, 숫자만 사용하여 12자로 입력해 주세요. \n\n" +
                    "**주문 수량**: (필수) 1개 이상, 100개 이하여야 합니다. \n\n" +
                    "주문할 상품을 한 종류 이상 입력해 주세요."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(
                    responseCode = "400",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResult.class),
                            examples = {
                                    @ExampleObject(name = "주문 상품 빈 리스트", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"BODY\", \"field\": \"orderProductRequestList\", \"message\": \"주문할 상품을 한 종류 이상 입력해 주세요\"}]}", description = "{} \n\n {\"orderProductRequestList\": []}"),
                                    @ExampleObject(name = "상품 번호, 주문 수량 미입력", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"BODY\", \"field\": \"orderProductRequestList[0].productNumber\", \"message\": \"상품 번호는 필수입니다\"}, {\"location\": \"BODY\", \"field\": \"orderProductRequestList[0].quantity\", \"message\": \"주문 수량은 필수입니다\"}]}", description = "{\"orderProductRequestList\": [{}]}"),
                                    @ExampleObject(name = "상품 번호 빈 문자열 (공백 포함)", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"BODY\", \"field\": \"orderProductRequestList[0].productNumber\", \"message\": \"상품 번호는 필수입니다\"}, {\"location\": \"BODY\", \"field\": \"orderProductRequestList[0].productNumber\", \"message\": \"상품 번호는 영문, 숫자만 사용하여 12자로 입력해 주세요\"}]}", description = "{\"orderProductRequestList\": [{\"productNumber\": \"\", \"quantity\": 1}]}"),
                                    @ExampleObject(name = "첫 번째 상품 주문 수량 1개 미만, 두 번째 상품 주문 수량 100개 초과", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"BODY\", \"field\": \"orderProductRequestList[0].quantity\", \"message\": \"주문 수량은 1개 이상이어야 합니다\"}, {\"location\": \"BODY\", \"field\": \"orderProductRequestList[1].quantity\", \"message\": \"주문 수량은 100개 이하여야 합니다\"}]}"),
                                    @ExampleObject(name = "주문 상품 중복", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"BODY\", \"field\": \"productsUnique\", \"message\": \"중복된 상품이 주문에 포함되어 있습니다\"}]}"),
                                    @ExampleObject(name = "재고 부족", value = "{\"code\": \"BAD_ARGUMENT\", \"message\": \"재고가 부족합니다. productNumber: WxgG3CzGZhAZ\", \"errors\": null}")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResult.class),
                            examples = @ExampleObject(name = "존재하지 않는 상품", value = "{\"code\": \"NOT_FOUND\", \"message\": \"존재하지 않는 상품입니다. productNumber: WxgG3CzGZhAZ\", \"errors\": null}")
                    )
            )
    })
    @ApiUnauthorizedResponse
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
                    "원하지 않는 검색 조건은 비워주세요. \n\n" +
                    "Postman에서 검색 시작일과 검색 종료일은 형식이 불일치되면 기본 예외 메시지가 출력됩니다. (공백만 입력되면 무시됩니다.) \n\n" +
                    "초성과 한글은 섞이면 검색 결과가 나오지 않습니다. \n\n" +
                    "ex) 프로그래ㅁ (X), jPa ㅍㄹㄱㄹㅁ (O)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(
                    responseCode = "400",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResult.class),
                            examples = {
                                    @ExampleObject(name = "검색 시작일 형식 불일치", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"FORM\", \"field\": \"startDate\", \"message\": \"Failed to convert value of type 'java.lang.String' to required type 'java.time.LocalDate'; Failed to convert from type [java.lang.String] to type [@io.swagger.v3.oas.annotations.media.Schema @org.springframework.format.annotation.DateTimeFormat java.time.LocalDate] for value [20260410]\"}]}"),
                                    @ExampleObject(name = "공백 섞인 검색 종료일", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"FORM\", \"field\": \"startDate\", \"message\": \"Failed to convert value of type 'java.lang.String' to required type 'java.time.LocalDate'; Failed to convert from type [java.lang.String] to type [@io.swagger.v3.oas.annotations.media.Schema @org.springframework.format.annotation.DateTimeFormat java.time.LocalDate] for value [20 26-04-10]\"}]}")
                            }
                    )
            )
    })
    @ApiAdminForbiddenResponse
    @GetMapping("/orders")
    public ResponseEntity<Result<List<OrderSearchResponse>>> orderList(@ParameterObject @ModelAttribute @Valid OrderSearchCond cond) {
        List<OrderSearchResponse> orderSearchResponseList = orderQueryService.searchOrders(cond);
        return ResponseEntity.ok(new Result<>(orderSearchResponseList, orderSearchResponseList.size()));
    }

    @Operation(
            summary = "주문 상세 조회",
            description = "**주문의 주인**만 조회할 수 있습니다. \n\n" +
                    "주문의 상세 정보를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(
                    responseCode = "400",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResult.class),
                            examples = @ExampleObject(name = "주문 번호 공백 (패턴 불일치 포함)", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"PATH\", \"field\": \"orderNumber\", \"message\": \"주문 번호는 영문, 숫자만 사용하여 12자로 입력해 주세요\"}]}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResult.class),
                            examples = @ExampleObject(name = "존재하지 않는 주문", value = "{\"code\": \"NOT_FOUND\", \"message\": \"존재하지 않는 주문입니다. orderNumber: eicanNoP5cW8\", \"errors\": null}")
                    )
            )
    })
    @ApiOrderOwnerForbiddenResponse
    @GetMapping("/orders/{orderNumber}")
    public ResponseEntity<Result<OrderQueryDto>> findOrder(
            @Parameter(description = "**12**자리의 주문 번호를 입력해 주세요.", example = "eicanNoP5cW8")
            @Pattern(regexp = "^[A-Za-z0-9]{12}$", message = "주문 번호는 영문, 숫자만 사용하여 12자로 입력해 주세요")
            @PathVariable("orderNumber") String orderNumber,
            @Parameter(hidden = true)
            @Login String loginId
    ) {
        OrderQueryDto orderQueryDto = orderQueryService.findOrder(orderNumber, loginId);
        return ResponseEntity.ok(new Result<>(orderQueryDto, 1));
    }

    @Operation(
            summary = "주문 수정",
            description = "**주문의 주인**만 수정할 수 있습니다. \n\n" +
                    "주문의 주인이 아니면 관리자도 수정할 수 없습니다. \n\n" +
                    "예시 주문은 주인이 아니어도 수정되지 않고 성공합니다. \n\n" +
                    "**취소**된 주문과 **결제 완료**된 주문은 수정할 수 없습니다. \n\n" +
                    "**회원 상세 조회**를 통해 주문이 있는지 확인하고 주문 번호를 입력해 주세요. \n\n" +
                    "**상품 번호**: 필수, 12자 \n\n" +
                    "**수량**: 필수, 0개 이상, 100개 이하"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(
                    responseCode = "400",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResult.class),
                            examples = {
                                    @ExampleObject(name = "주문 번호 공백 (패턴 불일치 포함)", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"PATH\", \"field\": \"orderNumber\", \"message\": \"주문 번호는 영문, 숫자만 사용하여 12자로 입력해 주세요\"}]}"),
                                    @ExampleObject(
                                            name = "첫 번째 상품 상품 번호 빈 문자열 (공백 포함), 두 번째 상품 상품 번호 패턴 불일치, 두 번째 상품 주문 수량 1개 미만",
                                            value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"BODY\", \"field\": \"orderProductRequestList[0].productNumber\", \"message\": \"상품 번호는 필수입니다\"}, {\"location\": \"BODY\", \"field\": \"orderProductRequestList[0].productNumber\", \"message\": \"상품 번호는 영문, 숫자만 사용하여 12자로 입력해 주세요\"}, {\"location\": \"BODY\", \"field\": \"orderProductRequestList[1].productNumber\", \"message\": \"상품 번호는 영문, 숫자만 사용하여 12자로 입력해 주세요\"}, {\"location\": \"BODY\", \"field\": \"orderProductRequestList[1].quantity\", \"message\": \"주문 수량은 1개 이상이어야 합니다\"}]}"
                                    ),
                                    @ExampleObject(name = "주문 상품 중복", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"BODY\", \"field\": \"productsUnique\", \"message\": \"중복된 상품이 주문에 포함되어 있습니다\"}]}"),
                                    @ExampleObject(name = "주문 취소 상태", value = "{\"code\": \"BAD_STATUS\", \"message\": \"주문을 수정할 수 없습니다. OrderStatus: CANCELED\", \"errors\": null}"),
                                    @ExampleObject(name = "주문 결제 완료 상태", value = "{\"code\": \"BAD_STATUS\", \"message\": \"주문을 수정할 수 없습니다. OrderStatus: PAID\", \"errors\": null}"),
                                    @ExampleObject(name = "주문 배송 완료 상태", value = "{\"code\": \"BAD_STATUS\", \"message\": \"주문을 수정할 수 없습니다. OrderStatus: DELIVERED\", \"errors\": null}"),
                                    @ExampleObject(name = "재고 부족", value = "{\"code\": \"BAD_ARGUMENT\", \"message\": \"재고가 부족합니다. productNumber: WxgG3CzGZhAZ\", \"errors\": null}")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResult.class),
                            examples = {
                                    @ExampleObject(name = "존재하지 않는 주문", value = "{\"code\": \"NOT_FOUND\", \"message\": \"존재하지 않는 주문입니다. orderNumber: eicanNoP5cW8\", \"errors\": null}"),
                                    @ExampleObject(name = "존재하지 않는 상품", value = "{\"code\": \"NOT_FOUND\", \"message\": \"존재하지 않는 상품입니다. productNumber: WxgG3CzGZhAZ\", \"errors\": null}")
                            }
                    )
            )
    })
    @ApiOrderOwnerForbiddenResponse
    @PatchMapping("/orders/{orderNumber}")
    public ResponseEntity<Result<OrderChangeResponse>> changeOrder(
            @Parameter(description = "**12**자리의 주문 번호를 입력해 주세요.", example = "eicanNoP5cW8")
            @Pattern(regexp = "^[A-Za-z0-9]{12}$", message = "주문 번호는 영문, 숫자만 사용하여 12자로 입력해 주세요")
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
                    "**결제가 완료된 주문**은 **배송이 완료**되어야 삭제할 수 있습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(
                    responseCode = "400",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResult.class),
                            examples = {
                                    @ExampleObject(name = "주문 번호 공백 (패턴 불일치 포함)", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"PATH\", \"field\": \"orderNumber\", \"message\": \"주문 번호는 영문, 숫자만 사용하여 12자로 입력해 주세요\"}]}"),
                                    @ExampleObject(name = "주문 생성 상태", value = "{\"code\": \"BAD_STATUS\", \"message\": \"주문을 취소해야 삭제할 수 있습니다\", \"errors\": null}"),
                                    @ExampleObject(name = "주문 결제 완료 상태", value = "{\"code\": \"BAD_STATUS\", \"message\": \"배송이 완료되어야 삭제할 수 있습니다\", \"errors\": null}")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResult.class),
                            examples = @ExampleObject(name = "존재하지 않는 주문", value = "{\"code\": \"NOT_FOUND\", \"message\": \"존재하지 않는 주문입니다. orderNumber: eicanNoP5cW8\", \"errors\": null}")
                    )
            )
    })
    @ApiOrderOwnerForbiddenResponse
    @DeleteMapping("/orders/{orderNumber}")
    public ResponseEntity<Result<String>> delete(
            @Parameter(description = "**12**자리의 주문 번호를 입력해 주세요.", example = "eicanNoP5cW8")
            @Pattern(regexp = "^[A-Za-z0-9]{12}$", message = "주문 번호는 영문, 숫자만 사용하여 12자로 입력해 주세요")
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
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(
                    responseCode = "400",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResult.class),
                            examples = {
                                    @ExampleObject(name = "주문 번호 공백 (패턴 불일치 포함)", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"PATH\", \"field\": \"orderNumber\", \"message\": \"주문 번호는 영문, 숫자만 사용하여 12자로 입력해 주세요\"}]}"),
                                    @ExampleObject(name = "주문 결제 완료 상태", value = "{\"code\": \"BAD_STATUS\", \"message\": \"결제 완료된 주문이어서 취소할 수 없습니다. OrderStatus: PAID\", \"errors\": null}"),
                                    @ExampleObject(name = "주문 배송 완료 상태", value = "{\"code\": \"BAD_STATUS\", \"message\": \"결제 완료된 주문이어서 취소할 수 없습니다. OrderStatus: DELIVERED\", \"errors\": null}")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResult.class),
                            examples = @ExampleObject(name = "존재하지 않는 주문", value = "{\"code\": \"NOT_FOUND\", \"message\": \"존재하지 않는 주문입니다. orderNumber: eicanNoP5cW8\", \"errors\": null}")
                    )
            )
    })
    @ApiOrderOwnerForbiddenResponse
    @PatchMapping("/orders/{orderNumber}/cancel")
    public ResponseEntity<Result<OrderCancelResponse>> cancelOrder(
            @Parameter(description = "**12**자리의 주문 번호를 입력해 주세요.", example = "eicanNoP5cW8")
            @Pattern(regexp = "^[A-Za-z0-9]{12}$", message = "주문 번호는 영문, 숫자만 사용하여 12자로 입력해 주세요")
            @PathVariable("orderNumber") String orderNumber,
            @Parameter(hidden = true)
            @Login String loginId
    ) {
        Order canceledOrder = orderService.cancelOrder(orderNumber, loginId);
        OrderCancelResponse orderCancelResponse = orderService.getOrderCancelResponse(canceledOrder);
        return ResponseEntity.ok(new Result<>(orderCancelResponse, 1));
    }
}
