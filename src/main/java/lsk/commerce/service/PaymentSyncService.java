package lsk.commerce.service;

import io.portone.sdk.server.payment.PaidPayment;
import io.portone.sdk.server.payment.PaymentClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lsk.commerce.api.portone.SyncPaymentException;
import lsk.commerce.dto.request.PaymentRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentSyncService {

    private final PaymentService paymentService;
    private final PaymentClient portone;

    public Mono<PaymentRequest> syncPayment(String paymentId) {
        return Mono.fromFuture(portone.getPayment(paymentId))
                .onErrorMap(e -> {
                    log.error("포트원 조회 중 에러 발생: {}", e.getMessage());
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
