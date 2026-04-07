package lsk.commerce.service;

import io.portone.sdk.server.payment.FailedPayment;
import io.portone.sdk.server.payment.PaidPayment;
import io.portone.sdk.server.payment.PaymentClient;
import lsk.commerce.api.portone.SyncPaymentException;
import lsk.commerce.domain.Delivery;
import lsk.commerce.domain.Member;
import lsk.commerce.domain.Order;
import lsk.commerce.domain.OrderProduct;
import lsk.commerce.domain.Payment;
import lsk.commerce.domain.PaymentStatus;
import lsk.commerce.domain.product.Album;
import lsk.commerce.domain.product.Book;
import lsk.commerce.domain.product.Movie;
import lsk.commerce.dto.request.PaymentCompleteResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

@ExtendWith(MockitoExtension.class)
class PaymentSyncServiceTest {

    @Mock
    PaymentClient paymentClient;

    @Mock
    PaidPayment paidPayment;

    @Mock
    PaymentService paymentService;

    @InjectMocks
    PaymentSyncService paymentSyncService;

    Member member;
    Delivery delivery;
    Album album;
    Book book;
    Movie movie;
    OrderProduct orderProduct1;
    OrderProduct orderProduct2;
    Order order;

    @BeforeEach
    void beforeEach() {
        member = Member.builder()
                .loginId("id_A")
                .zipcode("01234")
                .baseAddress("서울시 강남구")
                .detailAddress("101동 101호")
                .build();
        delivery = new Delivery(member);

        album = Album.builder()
                .name("BANG BANG")
                .price(15000)
                .stockQuantity(10)
                .build();
        book = Book.builder()
                .name("자바 ORM 표준 JPA 프로그래밍")
                .price(15000)
                .stockQuantity(7)
                .build();
        movie = Movie.builder()
                .name("범죄도시")
                .price(15000)
                .stockQuantity(5)
                .build();

        orderProduct1 = OrderProduct.createOrderProduct(album, 5);
        orderProduct2 = OrderProduct.createOrderProduct(book, 3);

        order = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));

        Payment.requestPayment(order);
    }

    @Nested
    class SyncPayment {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                given(paymentClient.getPayment(anyString())).willReturn(CompletableFuture.completedFuture(paidPayment));
                given(paymentService.verifyAndComplete(any(PaidPayment.class), anyString())).willAnswer(invocation -> {
                    Payment payment = order.getPayment();
                    payment.complete(LocalDateTime.now());
                    return payment;
                });
                given(paymentService.getPaymentCompleteResponse(any(Payment.class))).willAnswer(invocation -> new PaymentCompleteResponse(order.getPayment().getPaymentId(), order.getPayment().getPaymentStatus()));

                //when & then
                StepVerifier.create(paymentSyncService.syncPayment(order.getPayment().getPaymentId(), "id_A").log())
                        .assertNext(response ->
                                then(response)
                                        .extracting("paymentId", "paymentStatus")
                                        .containsExactly(order.getPayment().getPaymentId(), PaymentStatus.COMPLETED)
                        )
                        .verifyComplete();

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(paymentClient).should().getPayment(anyString()));
                    softly.check(() -> BDDMockito.then(paymentService).should().verifyAndComplete(any(PaidPayment.class), anyString()));
                    softly.check(() -> BDDMockito.then(paymentService).should().getPaymentCompleteResponse(any(Payment.class)));
                });
            }
        }

        @Nested
        class FailureCase {

            @Test
            void failedFind() {
                //given
                given(paymentClient.getPayment(anyString())).willReturn(CompletableFuture.failedFuture(new RuntimeException("Find Failed")));

                //when & then
                StepVerifier.create(paymentSyncService.syncPayment(order.getPayment().getPaymentId(), "id_A"))
                        .verifyErrorSatisfies(error -> then(error).isInstanceOf(SyncPaymentException.class));
            }

            @Test
            void failedPayment() {
                //given
                FailedPayment failedPayment = mock(FailedPayment.class);

                given(paymentClient.getPayment(anyString())).willReturn(CompletableFuture.completedFuture(failedPayment));
                given(paymentService.failedPayment(anyString())).willAnswer(invocation -> {
                    Payment payment = order.getPayment();
                    payment.failed();
                    return payment;
                });
                given(paymentService.getPaymentCompleteResponse(any(Payment.class))).willAnswer(invocation -> new PaymentCompleteResponse(order.getPayment().getPaymentId(), order.getPayment().getPaymentStatus()));

                //when & then
                StepVerifier.create(paymentSyncService.syncPayment(order.getPayment().getPaymentId(), "id_A").log())
                        .assertNext(response ->
                                then(response)
                                        .extracting("paymentId", "paymentStatus")
                                        .containsExactly(order.getPayment().getPaymentId(), PaymentStatus.FAILED)
                        )
                        .verifyComplete();

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(paymentClient).should().getPayment(anyString()));
                    softly.check(() -> BDDMockito.then(paymentService).should().failedPayment(anyString()));
                    softly.check(() -> BDDMockito.then(paymentService).should().getPaymentCompleteResponse(any(Payment.class)));
                });
            }
        }
    }
}