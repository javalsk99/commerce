package lsk.commerce.controller;

import io.portone.sdk.server.webhook.Webhook;
import io.portone.sdk.server.webhook.WebhookTransaction;
import io.portone.sdk.server.webhook.WebhookVerifier;
import kotlin.Unit;
import lsk.commerce.api.portone.CompletePaymentRequest;
import lsk.commerce.api.portone.PortoneSecretProperties;
import lsk.commerce.api.portone.SyncPaymentException;
import lsk.commerce.domain.Order;
import lsk.commerce.dto.request.OrderRequest;
import lsk.commerce.dto.request.PaymentRequest;
import lsk.commerce.service.OrderService;
import lsk.commerce.service.PaymentSyncService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class PaymentController {

    private final OrderService orderService;
    private final PortoneSecretProperties secret;
    private final WebhookVerifier portoneWebhook;
    private final PaymentSyncService paymentSyncService;

    public PaymentController(OrderService orderService, PaymentSyncService paymentSyncService, PortoneSecretProperties secret) {
        this.orderService = orderService;
        this.paymentSyncService = paymentSyncService;
        this.secret = secret;
        portoneWebhook = new WebhookVerifier(secret.webhook());
    }

    @GetMapping("/api/payments/{orderNumber}")
    public OrderRequest getOrder(@PathVariable("orderNumber") String orderNumber) {
        Order order = orderService.findOrderWithAll(orderNumber);
        return orderService.getOrderRequest(order);
    }

    //브라우저에서 결제 완료 후 서버에 결제 완료를 알리는 용도 (결제 정보를 완전히 실시간으로 얻기 위해서는 웹훅 사용 / 수정할 곳 없음)
    @PostMapping("/api/payment/complete")
    public Mono<PaymentRequest> completePayment(@RequestBody CompletePaymentRequest completeRequest) {
        return paymentSyncService.syncPayment(completeRequest.paymentId);
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
            throw new SyncPaymentException();
        }
        if (webhook instanceof WebhookTransaction transaction) {
            return paymentSyncService.syncPayment(transaction.getData().getPaymentId()).map(payment -> Unit.INSTANCE);
        }
        return Mono.empty();
    }
}
