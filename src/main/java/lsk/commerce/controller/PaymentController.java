package lsk.commerce.controller;

import io.portone.sdk.server.webhook.Webhook;
import io.portone.sdk.server.webhook.WebhookTransaction;
import io.portone.sdk.server.webhook.WebhookVerifier;
import kotlin.Unit;
import lombok.RequiredArgsConstructor;
import lsk.commerce.api.portone.CompletePaymentRequest;
import lsk.commerce.api.portone.SyncPaymentException;
import lsk.commerce.domain.Order;
import lsk.commerce.dto.response.OrderPaymentResponse;
import lsk.commerce.dto.request.PaymentCompleteResponse;
import lsk.commerce.dto.response.Result;
import lsk.commerce.service.OrderService;
import lsk.commerce.service.PaymentSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final OrderService orderService;
    private final WebhookVerifier portoneWebhook;
    private final PaymentSyncService paymentSyncService;

    @GetMapping("/api/payments/{orderNumber}")
    public ResponseEntity<Result<OrderPaymentResponse>> getOrder(@PathVariable("orderNumber") String orderNumber) {
        Order order = orderService.findOrderWithAll(orderNumber);
        OrderPaymentResponse orderPaymentResponse = orderService.getOrderPaymentResponse(order);
        return ResponseEntity.ok(new Result<>(orderPaymentResponse, 1));
    }

    //브라우저에서 결제 완료 후 서버에 결제 완료를 알리는 용도 (결제 정보를 완전히 실시간으로 얻기 위해서는 웹훅 사용 / 수정할 곳 없음)
    @PostMapping("/api/payments/complete")
    public Mono<ResponseEntity<Result<PaymentCompleteResponse>>> completePayment(@RequestBody CompletePaymentRequest completeRequest) {
        Mono<PaymentCompleteResponse> paymentCompleteResponseMono = paymentSyncService.syncPayment(completeRequest.paymentId());
        return paymentCompleteResponseMono
                .map(response -> ResponseEntity.ok(new Result<>(response, 1)));
    }

    //결제 정보를 실시간으로 전달받기 위한 웹훅
    @PostMapping("/api/payment/webhook")
    public Mono<Unit> handleWebhook(
            @RequestBody String body,
            @RequestHeader("webhook-id") String webhookId,
            @RequestHeader("webhook-timestamp") String webhookTimestamp,
            @RequestHeader("webhook-signature") String webhookSignature
    ) throws SyncPaymentException {
        Webhook webhook;
        try {
            webhook = portoneWebhook.verify(body, webhookId, webhookSignature, webhookTimestamp);
        } catch (Exception e) {
            throw new SyncPaymentException("포트원 웹훅 처리 중 오류 발생");
        }
        if (webhook instanceof WebhookTransaction transaction) {
            return paymentSyncService.syncPayment(transaction.getData().getPaymentId()).map(payment -> Unit.INSTANCE);
        }
        return Mono.empty();
    }
}
