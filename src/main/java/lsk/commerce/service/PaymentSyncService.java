package lsk.commerce.service;

import io.portone.sdk.server.payment.PaidPayment;
import io.portone.sdk.server.payment.PaymentClient;
import lsk.commerce.api.portone.PortoneSecretProperties;
import lsk.commerce.api.portone.SyncPaymentException;
import lsk.commerce.dto.request.PaymentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class PaymentSyncService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentSyncService.class);

    private final PaymentService paymentService;
    private final PortoneSecretProperties secret;
    private final PaymentClient portone;

    public PaymentSyncService(PaymentService paymentService, PortoneSecretProperties secret) {
        this.paymentService = paymentService;
        this.secret = secret;
        portone = new PaymentClient(secret.api(), "https://api.portone.io", "store-3218fbd8-7af7-4043-8a4e-ec6e84fd858c");
    }

    public Mono<PaymentRequest> syncPayment(String paymentId) {
        return Mono.fromFuture(portone.getPayment(paymentId))
                .onErrorMap(e -> {
                    logger.error("포트원 조회 중 에러 발생: {}", e.getMessage());
                    e.printStackTrace();
                    return new SyncPaymentException();
                })
                .flatMap(actualPayment -> {
                    if (actualPayment instanceof PaidPayment paidPayment) {
                        return Mono.fromCallable(() -> paymentService.verifyAndComplete(paidPayment))
                                .subscribeOn(Schedulers.boundedElastic());

                    } else {
                        return Mono.fromCallable(() -> paymentService.failedPayment(paymentId))
                                .subscribeOn(Schedulers.boundedElastic());
                    }
                })
                .map(updatePayment -> paymentService.getPaymentRequest(updatePayment));
    }
}
