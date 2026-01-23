package lsk.commerce.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.portone.sdk.server.payment.PaidPayment;
import io.portone.sdk.server.payment.PaymentClient;
import io.portone.sdk.server.payment.VirtualAccountIssuedPayment;
import io.portone.sdk.server.webhook.Webhook;
import io.portone.sdk.server.webhook.WebhookTransaction;
import io.portone.sdk.server.webhook.WebhookVerifier;
import kotlin.Unit;
import lsk.commerce.controller.form.OrderForm;
import lsk.commerce.domain.Order;
import lsk.commerce.domain.Payment;
import lsk.commerce.domain.Product;
import lsk.commerce.api.portone.CompletePaymentRequest;
import lsk.commerce.api.portone.PaymentCustomData;
import lsk.commerce.api.portone.PortoneSecretProperties;
import lsk.commerce.api.portone.SyncPaymentException;
import lsk.commerce.service.OrderService;
import lsk.commerce.service.PaymentService;
import lsk.commerce.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import static lsk.commerce.domain.PaymentStatus.*;

@RestController
public class PaymentController {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final ProductService productService;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final PortoneSecretProperties secret;
    private final PaymentClient portone;
    private final WebhookVerifier portoneWebhook;

    public PaymentController(ProductService productService, OrderService orderService, PaymentService paymentService, PortoneSecretProperties secret) {
        this.productService = productService;
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.secret = secret;
        portone = new PaymentClient(secret.api(), "https://api.portone.io", "store-3218fbd8-7af7-4043-8a4e-ec6e84fd858c");
        portoneWebhook = new WebhookVerifier(secret.webhook());
    }

    @GetMapping("/api/orders/{orderId}")
    public OrderForm getOrder(@PathVariable("orderId") Long orderId) {
        Order order = orderService.findOrder(orderId);
        return OrderForm.orderChangeForm(order);
    }

    //브라우저에서 결제 완료 후 서버에 결제 완료를 알리는 용도 (결제 정보를 완전히 실시간으로 얻기 위해서는 웹훅 사용 / 수정할 곳 없음)
    @PostMapping("/api/payment/complete")
    public Mono<Payment> completePayment(@RequestBody CompletePaymentRequest completeRequest) {
        return syncPayment(completeRequest.paymentId);
    }

    //결제 정보를 실시간으로 전달받기 위한 웹훅 (수정할 곳 없음)
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
            return syncPayment(transaction.getData().getPaymentId()).map(payment -> Unit.INSTANCE);
        }
        return Mono.empty();
    }

    //서버의 결제 데이터베이스를 따라하는 샘플 / syncPayment 호출시에 포트원의 결제 건을 조회하여 상태를 동기화하고 결제 완료시에 완료 처리를 한다 (실제 데이터베이스 사용시에는 결제건 단위 락을 잡아 동시성 문제 피하기 / 수정해야 함)
    private Mono<Payment> syncPayment(String paymentId) {
        Payment payment = paymentService.findPaymentByPaymentId(paymentId)
                .orElseGet(() -> {
                    Payment newPayment = new Payment(paymentId, PENDING);
                    paymentService.request(newPayment);
                    return newPayment;
                });
        return Mono.fromFuture(portone.getPayment(paymentId))
                .onErrorMap(e -> {
                        logger.error("포트원 조회 중 에러 발생: {}", e.getMessage()); // 진짜 에러 이유 출력
                        e.printStackTrace(); // 에러 스택 추적
                        return new SyncPaymentException();})
                .flatMap(actualPayment -> {
                    switch (actualPayment) {
                        case PaidPayment paidPayment:
                            if (!verifPayment(paidPayment)) return Mono.error(new SyncPaymentException());
                            logger.info("결제 성공 {}", actualPayment);
                            if (payment.getPaymentStatus().equals(COMPLETED)) {
                                return Mono.just(payment);
                            } else {
                                payment.setPaymentStatus(COMPLETED);
                                return Mono.just(payment);
                            }
                        case VirtualAccountIssuedPayment ignored:
                            payment.setPaymentStatus(FAILED);
                            return Mono.just(payment);
                        default:
                            return Mono.just(payment);
                    }
                });
    }

    //결제는 브라우저에서 진행되기 때문에, 결제 승인 정보와 결제 항목이 일치하는지 확인해야 한다 / 포트원의 customData 파라미터에 결제 항목의 id인 product 필드를 지정하고, 서버의 결제 항목 정보와 일치하는지 확인 (수정해야 함)
    public boolean verifPayment(PaidPayment payment) {
        //실연동 시에 테스트 채널키로 변조되어 결제되지 않도록 검증
        //if (!payment.getChannel().getType().equals(SelectedChannelType.Live.INSTANCE)) return false;

        var customData = payment.getCustomData();
        if (customData == null) return false;

        PaymentCustomData customDataDecoded;
        try {
            customDataDecoded = objectMapper.readValue(customData, PaymentCustomData.class);
        } catch (JsonProcessingException e) {
            return false;
        }

        Product product = productService.findProduct(Long.valueOf(customDataDecoded.product()));
        if (product == null) return false;

        //결제로 주문한 상품의 이름과 상품의 이름이 같은지, 결제한 총 금액과 상품의 가격이 같은지 (주문 금액으로 변경), 결제한 화폐와 상품의 화폐가 같은지
        return payment.getOrderName().equals(product.getName()) &&
                payment.getAmount().getTotal() == product.getPrice() &&
                payment.getCurrency().getValue().equals(product.getCurrency());
    }
}
