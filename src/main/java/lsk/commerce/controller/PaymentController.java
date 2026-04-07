package lsk.commerce.controller;

import io.portone.sdk.server.webhook.Webhook;
import io.portone.sdk.server.webhook.WebhookTransaction;
import io.portone.sdk.server.webhook.WebhookVerifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kotlin.Unit;
import lombok.RequiredArgsConstructor;
import lsk.commerce.api.portone.CompletePaymentRequest;
import lsk.commerce.api.portone.SyncPaymentException;
import lsk.commerce.argumentresolver.Login;
import lsk.commerce.domain.Order;
import lsk.commerce.domain.Payment;
import lsk.commerce.dto.request.PaymentCompleteResponse;
import lsk.commerce.dto.response.OrderPaymentResponse;
import lsk.commerce.dto.response.PaymentResponse;
import lsk.commerce.dto.response.Result;
import lsk.commerce.exception.ErrorResult;
import lsk.commerce.service.OrderService;
import lsk.commerce.service.PaymentService;
import lsk.commerce.service.PaymentSyncService;
import lsk.commerce.swagger.ApiMemberOwnerForbiddenResponse;
import lsk.commerce.swagger.ApiOrderOwnerForbiddenResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Tag(
        name = "06. 결제",
        description = "요청이 완료된 주문은 https://lsk-commerce.shop/payments/{orderNumber}에서 결제를 진행해 주세요. \n\n" +
                "테스트 결제라서 실제 돈이 결제되지 않습니다. \n\n" +
                "**주의 사항** 실제 결제되진 않지만 카카오페이는 페이머니가 있어야 결제 테스트가 진행됩니다. 토스페이를 통해 진행해 주세요."
)
@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final WebhookVerifier portoneWebhook;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final PaymentSyncService paymentSyncService;

    @Operation(
            summary = "결제 요청",
            description = "**주문의 주인**만 결제 요청할 수 있습니다. \n\n" +
                    "주문의 주인이 아니면 관리자도 요청할 수 없습니다. \n\n" +
                    "예시 주문은 주인이 아니어도 요청되지 않고 성공합니다. \n\n" +
                    "**취소된 주문**은 요청할 수 없습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 주문", content = @Content(schema = @Schema(implementation = ErrorResult.class)))
    })
    @ApiOrderOwnerForbiddenResponse
    @PostMapping("/payments/orders/{orderNumber}")
    public ResponseEntity<Result<PaymentResponse>> requestPayment(
            @Parameter(description = "**12**자리의 주문 번호를 입력해 주세요.", example = "eicanNoP5cW8")
            @PathVariable("orderNumber") String orderNumber,
            @Parameter(hidden = true)
            @Login String loginId
    ) {
        Payment payment = paymentService.request(orderNumber, loginId);
        PaymentResponse paymentResponse = paymentService.getPaymentResponse(payment);
        return ResponseEntity.ok(new Result<>(paymentResponse, 1));
    }

    @Operation(hidden = true)
    @GetMapping("/api/payments/{orderNumber}")
    public ResponseEntity<Result<OrderPaymentResponse>> getOrder(
            @PathVariable("orderNumber") String orderNumber,
            @Login String loginId
    ) {
        Order order = orderService.findOrderWithAll(orderNumber);
        order.isOwner(loginId);
        OrderPaymentResponse orderPaymentResponse = orderService.getOrderPaymentResponse(order);
        return ResponseEntity.ok(new Result<>(orderPaymentResponse, 1));
    }

    @Operation(hidden = true)
    @PostMapping("/api/payments/complete")
    public Mono<ResponseEntity<Result<PaymentCompleteResponse>>> completePayment(
            @RequestBody CompletePaymentRequest completeRequest,
            @Login String loginId
    ) {
        Mono<PaymentCompleteResponse> paymentCompleteResponseMono = paymentSyncService.syncPayment(completeRequest.paymentId(), loginId);
        return paymentCompleteResponseMono
                .map(response -> ResponseEntity.ok(new Result<>(response, 1)));
    }

    @Operation(hidden = true)
    //결제 정보를 실시간으로 전달받기 위한 웹훅
    @PostMapping("/api/payment/webhook")
    public Mono<Unit> handleWebhook(
            @RequestBody String body,
            @RequestHeader("webhook-id") String webhookId,
            @RequestHeader("webhook-timestamp") String webhookTimestamp,
            @RequestHeader("webhook-signature") String webhookSignature,
            @Login String loginId
    ) throws SyncPaymentException {
        Webhook webhook;
        try {
            webhook = portoneWebhook.verify(body, webhookId, webhookSignature, webhookTimestamp);
        } catch (Exception e) {
            throw new SyncPaymentException("포트원 웹훅 처리 중 오류 발생");
        }
        if (webhook instanceof WebhookTransaction transaction) {
            return paymentSyncService.syncPayment(transaction.getData().getPaymentId(), loginId).map(payment -> Unit.INSTANCE);
        }
        return Mono.empty();
    }
}
