package lsk.commerce.service;

import io.portone.sdk.server.payment.PaidPayment;
import io.portone.sdk.server.payment.PaymentClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lsk.commerce.api.portone.SyncPaymentException;
import lsk.commerce.dto.request.PaymentCompleteResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentSyncService {

    private final PaymentService paymentService;
    private final PaymentClient portone;

    public Mono<PaymentCompleteResponse> syncPayment(String paymentId, String loginId) {
        return Mono.fromFuture(portone.getPayment(paymentId))
                .onErrorMap(e -> new SyncPaymentException("결제 정보 조회 중 오류 발생"))
                .flatMap(actualPayment -> {
                    if (actualPayment instanceof PaidPayment paidPayment) {
                        return Mono.fromCallable(() -> paymentService.verifyAndComplete(paidPayment, loginId))
                                .subscribeOn(Schedulers.boundedElastic());

                    } else {
                        log.error("결제 실패 {}", actualPayment);
                        return Mono.fromCallable(() -> paymentService.failedPayment(paymentId))
                                .subscribeOn(Schedulers.boundedElastic());
                    }
                })
                .map(paymentService::getPaymentCompleteResponse);
    }
}
