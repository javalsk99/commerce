package lsk.commerce.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.portone.sdk.server.payment.PaidPayment;
import lsk.commerce.api.portone.PaymentCustomData;
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
import lsk.commerce.dto.OrderProductDto;
import lsk.commerce.dto.request.OrderRequest;
import lsk.commerce.event.PaymentCompletedEvent;
import lsk.commerce.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceUnitTest {

    @Mock
    OrderRequest orderRequest;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    PaidPayment paidPayment;

    @Mock
    PaymentRepository paymentRepository;

    @Mock
    ProductService productService;

    @Mock
    OrderService orderService;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @Spy
    ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    PaymentService paymentService;

    Member member;
    Delivery delivery;
    Album album;
    Book book;
    Movie movie;
    OrderProduct orderProduct1;
    OrderProduct orderProduct2;
    OrderProduct orderProduct3;
    Order singleOrder;
    Order multipleOrder;
    String wrongOrderNumber = "lllIIllIO00O";
    String wrongPaymentId = "dfoijxjfd342987jdfk";

    @BeforeEach
    void beforeEach() {
        member = Member.builder().city("Seoul").street("Gangnam").zipcode("01234").build();

        delivery = new Delivery(member);

        album = Album.builder().name("BANG BANG").price(15000).stockQuantity(10).build();
        book = Book.builder().name("자바 ORM 표준 JPA 프로그래밍").price(15000).stockQuantity(7).build();
        movie = Movie.builder().name("범죄도시").price(15000).stockQuantity(5).build();

        orderProduct1 = OrderProduct.createOrderProduct(album, 5);
        orderProduct2 = OrderProduct.createOrderProduct(book, 3);
        orderProduct3 = OrderProduct.createOrderProduct(movie, 2);

        singleOrder = Order.createOrder(member, delivery, List.of(orderProduct3));
        multipleOrder = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));

        Payment.requestPayment(singleOrder);
        Payment.requestPayment(multipleOrder);
    }

    @Nested
    class SuccessCase {

        @Test
        void request() {
            //given
            Order newOrder = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));

            given(orderService.findOrderWithAllExceptMember(anyString())).willReturn(newOrder);

            //when
            paymentService.request(newOrder.getOrderNumber());

            //then
            assertAll(
                    () -> then(orderService).should().findOrderWithAllExceptMember(anyString()),
                    () -> then(paymentRepository).should().save(newOrder.getPayment())
            );
            assertThat(newOrder.getPayment())
                    .extracting("order", "paymentAmount", "paymentDate", "paymentStatus")
                    .containsExactly(newOrder, newOrder.getTotalAmount(), null, PaymentStatus.PENDING);
        }

        @Test
        void find() {
            //given
            given(paymentRepository.findByPaymentId(anyString())).willReturn(Optional.of(multipleOrder.getPayment()));

            //when
            Payment payment = paymentService.findPaymentByPaymentId(multipleOrder.getPayment().getPaymentId());

            //then
            assertAll(
                    () -> then(paymentRepository).should().findByPaymentId(anyString()),
                    () -> assertThat(payment).isEqualTo(multipleOrder.getPayment())
            );
        }

        @Test
        void verifyAndComplete_hasSingleOrderProduct() {
            //given
            OrderProductDto orderProductDto = new OrderProductDto("범죄도시", 15000, 2, 30000);

            givenCustomData(singleOrder.getOrderNumber());

            findOrderAndProducts(singleOrder, List.of(orderProductDto));

            String orderName = givenOrderNameAndAmount(singleOrder, orderRequest);
            paidPaymentToString(orderName);

            givenCompletePayment(singleOrder);

            //when
            paymentService.verifyAndComplete(paidPayment);

            //then
            assertAll(
                    () -> then(objectMapper).should().readValue(paidPayment.getCustomData(), PaymentCustomData.class),
                    () -> then(orderService).should().findOrderWithAllExceptMember(anyString()),
                    () -> then(orderService).should().getOrderRequest(singleOrder),
                    () -> then(productService).should().findProducts(),
                    () -> then(paymentRepository).should().findWithOrderDelivery(anyString()),
                    () -> then(eventPublisher).should().publishEvent(any(PaymentCompletedEvent.class))
            );
            assertThat(paidPayment)
                    .extracting("customData", "amount.total", "orderName", "id", "paidAt")
                    .containsExactly("{\"orderNumber\":\"" + singleOrder.getOrderNumber() + "\"}", 30000L,
                            orderRequest.getOrderProducts().getFirst().getName(), singleOrder.getPayment().getPaymentId(),
                            singleOrder.getPayment().getPaymentDate().atZone(ZoneId.of("Asia/Seoul")).toInstant());
        }

        @Test
        void verifyAndComplete_hasMultipleOrderProducts() {
            //given
            OrderProductDto orderProductDto1 = new OrderProductDto("BANG BANG", 15000, 5, 75000);
            OrderProductDto orderProductDto2 = new OrderProductDto("자바 ORM 표준 JPA 프로그래밍", 15000, 3, 45000);

            givenCustomData(multipleOrder.getOrderNumber());

            findOrderAndProducts(multipleOrder, List.of(orderProductDto1, orderProductDto2));

            String orderName = givenOrderNameAndAmount(multipleOrder, orderRequest);
            paidPaymentToString(orderName);

            givenCompletePayment(multipleOrder);

            //when
            paymentService.verifyAndComplete(paidPayment);

            //then
            assertAll(
                    () -> then(objectMapper).should().readValue(paidPayment.getCustomData(), PaymentCustomData.class),
                    () -> then(orderService).should().findOrderWithAllExceptMember(anyString()),
                    () -> then(orderService).should().getOrderRequest(multipleOrder),
                    () -> then(productService).should().findProducts(),
                    () -> then(paymentRepository).should().findWithOrderDelivery(anyString()),
                    () -> then(eventPublisher).should().publishEvent(any(PaymentCompletedEvent.class))
            );
            assertThat(paidPayment)
                    .extracting("customData", "amount.total", "orderName", "id", "paidAt")
                    .containsExactly("{\"orderNumber\":\"" + multipleOrder.getOrderNumber() + "\"}", 120000L,
                            orderRequest.getOrderProducts().getFirst().getName() + " 외 " + (orderRequest.getOrderProducts().size() - 1) + "건",
                            multipleOrder.getPayment().getPaymentId(), multipleOrder.getPayment().getPaymentDate().atZone(ZoneId.of("Asia/Seoul")).toInstant());
        }

        private void givenCustomData(String orderNumber) {
            String json = "{\"orderNumber\":\"" + orderNumber + "\"}";
            given(paidPayment.getCustomData()).willReturn(json);
        }

        private void findOrderAndProducts(Order order, List<OrderProductDto> orderProductDto) {
            given(orderService.findOrderWithAllExceptMember(anyString())).willReturn(order);
            given(orderRequest.getOrderProducts()).willReturn(orderProductDto);
            given(orderService.getOrderRequest(any())).willReturn(orderRequest);

            given(productService.findProducts()).willReturn(List.of(album, book, movie));
        }

        private String givenOrderNameAndAmount(Order order, OrderRequest orderRequest) {
            given(paidPayment.getAmount().getTotal()).willReturn(order.getTotalAmount().longValue());
            given(orderRequest.getTotalAmount()).willReturn(order.getTotalAmount());

            String orderName;
            if (orderRequest.getOrderProducts().size() == 1) {
                orderName = orderRequest.getOrderProducts().getFirst().getName();
            } else {
                orderName = orderRequest.getOrderProducts().getFirst().getName() + " 외 " + (orderRequest.getOrderProducts().size() - 1) + "건";
            }

            given(paidPayment.getOrderName()).willReturn(orderName);
            return orderName;
        }

        private void paidPaymentToString(String orderName) {
            long total = paidPayment.getAmount().getTotal();
            given(paidPayment.toString()).willReturn("orderName = " + orderName + ", total = " + total);
        }

        private void givenCompletePayment(Order order) {
            String paymentId = order.getPayment().getPaymentId();
            given(paidPayment.getId()).willReturn(paymentId);
            given(paymentRepository.findWithOrderDelivery(anyString())).willReturn(Optional.of(order.getPayment()));
            given(paidPayment.getPaidAt()).willReturn(LocalDateTime.now().atZone(ZoneId.of("Asia/Seoul")).toInstant());
        }
    }

    @Nested
    class FailureCase {

        @Test
        void request_orderNotFound() {
            //given
            given(orderService.findOrderWithAllExceptMember(anyString())).willThrow(new IllegalArgumentException("존재하지 않는 주문입니다."));

            //when
            assertThatThrownBy(() -> paymentService.request(wrongOrderNumber))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 주문입니다.");

            //then
            assertAll(
                    () -> then(orderService).should().findOrderWithAllExceptMember(anyString()),
                    () -> then(paymentRepository).should(never()).save(any())
            );
        }

        @Test
        void request_duplicateRequest() {
            //given
            Order newOrder = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));

            given(orderService.findOrderWithAllExceptMember(anyString())).willReturn(newOrder);

            //when 첫 번째 호출
            paymentService.request(newOrder.getOrderNumber());

            //then
            assertAll(
                    () -> then(orderService).should().findOrderWithAllExceptMember(anyString()),
                    () -> then(paymentRepository).should().save(newOrder.getPayment())
            );
            assertThat(newOrder.getPayment())
                    .extracting("order", "paymentAmount", "paymentDate", "paymentStatus")
                    .containsExactly(newOrder, newOrder.getTotalAmount(), null, PaymentStatus.PENDING);

            //when 두 번째 호출
            assertThatThrownBy(() -> paymentService.request(newOrder.getOrderNumber()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("이미 결제 정보가 있습니다.");

            //then
            assertAll(
                    () -> then(orderService).should(times(2)).findOrderWithAllExceptMember(anyString()),
                    () -> then(paymentRepository).should().save(any())
            );
        }

        @Test
        void find_paymentNotFound() {
            //given
            given(paymentRepository.findByPaymentId(anyString())).willReturn(Optional.empty());

            //when
            assertThatThrownBy(() -> paymentService.findPaymentByPaymentId(wrongPaymentId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 결제 번호입니다.");
        }

        @Test
        void verifyAndComplete_orderNotFound() {
            //given
            String json = "{\"orderNumber\":\"" + multipleOrder.getOrderNumber() + "\"}";
            given(paidPayment.getCustomData()).willReturn(json);

            given(orderService.findOrderWithAllExceptMember(anyString())).willThrow(new IllegalArgumentException("존재하지 않는 주문입니다."));

            //when
            assertThatThrownBy(() -> paymentService.verifyAndComplete(paidPayment))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 주문입니다.");

            //then
            assertAll(
                    () -> then(objectMapper).should().readValue(paidPayment.getCustomData(), PaymentCustomData.class),
                    () -> then(orderService).should().findOrderWithAllExceptMember(anyString()),
                    () -> then(orderService).should(never()).getOrderRequest(any()),
                    () -> then(productService).should(never()).findProducts(),
                    () -> then(paymentRepository).should(never()).findWithOrderDelivery(any()),
                    () -> then(eventPublisher).should(never()).publishEvent(any())
            );
        }

        @Test
        void verifyAndComplete_orderProductDtoIsEmpty() {
            //given
            String json = "{\"orderNumber\":\"" + multipleOrder.getOrderNumber() + "\"}";
            given(paidPayment.getCustomData()).willReturn(json);

            given(orderService.findOrderWithAllExceptMember(anyString())).willReturn(multipleOrder);
            given(orderRequest.getOrderProducts()).willReturn(Collections.emptyList());
            given(orderService.getOrderRequest(any())).willReturn(orderRequest);

            given(productService.findProducts()).willReturn(List.of(album, book, movie));

            //when
            assertThatThrownBy(() -> paymentService.verifyAndComplete(paidPayment))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("주문 상품이 비어 있습니다.");

            //then
            assertAll(
                    () -> then(objectMapper).should().readValue(paidPayment.getCustomData(), PaymentCustomData.class),
                    () -> then(orderService).should().findOrderWithAllExceptMember(anyString()),
                    () -> then(orderService).should().getOrderRequest(multipleOrder),
                    () -> then(productService).should().findProducts(),
                    () -> then(paymentRepository).should(never()).findWithOrderDelivery(any()),
                    () -> then(eventPublisher).should(never()).publishEvent(any())
            );
        }

        @Test
        void verifyAndComplete_wrongProducts() {
            //given
            OrderProductDto orderProductDto1 = new OrderProductDto("BANG BANG", 15000, 5, 75000);
            OrderProductDto orderProductDto2 = new OrderProductDto("자바 ORM 표준 JPA 프로그래밍", 15000, 3, 45000);

            String json = "{\"orderNumber\":\"" + multipleOrder.getOrderNumber() + "\"}";
            given(paidPayment.getCustomData()).willReturn(json);

            given(orderService.findOrderWithAllExceptMember(anyString())).willReturn(multipleOrder);
            given(orderRequest.getOrderProducts()).willReturn(List.of(orderProductDto1, orderProductDto2));
            given(orderService.getOrderRequest(any())).willReturn(orderRequest);

            given(productService.findProducts()).willReturn(List.of(movie));

            //when
            assertThatThrownBy(() -> paymentService.verifyAndComplete(paidPayment))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("잘못된 상품이 있습니다.");

            //then
            assertAll(
                    () -> then(objectMapper).should().readValue(paidPayment.getCustomData(), PaymentCustomData.class),
                    () -> then(orderService).should().findOrderWithAllExceptMember(anyString()),
                    () -> then(orderService).should().getOrderRequest(multipleOrder),
                    () -> then(productService).should().findProducts(),
                    () -> then(paymentRepository).should(never()).findWithOrderDelivery(any()),
                    () -> then(eventPublisher).should(never()).publishEvent(any())
            );
        }

        @Test
        void verifyAndComplete_amountMismatch() {
            //given
            OrderProductDto orderProductDto1 = new OrderProductDto("BANG BANG", 15000, 5, 75000);
            OrderProductDto orderProductDto2 = new OrderProductDto("자바 ORM 표준 JPA 프로그래밍", 15000, 3, 45000);

            String json = "{\"orderNumber\":\"" + multipleOrder.getOrderNumber() + "\"}";
            given(paidPayment.getCustomData()).willReturn(json);

            given(orderService.findOrderWithAllExceptMember(anyString())).willReturn(multipleOrder);
            given(orderRequest.getOrderProducts()).willReturn(List.of(orderProductDto1, orderProductDto2));
            given(orderService.getOrderRequest(any())).willReturn(orderRequest);

            given(productService.findProducts()).willReturn(List.of(album, book, movie));

            given(paidPayment.getAmount().getTotal()).willReturn(10000L);
            given(orderRequest.getTotalAmount()).willReturn(120000);

            //when
            assertThatThrownBy(() -> paymentService.verifyAndComplete(paidPayment))
                    .isInstanceOf(SyncPaymentException.class);

            //then
            assertAll(
                    () -> then(objectMapper).should().readValue(paidPayment.getCustomData(), PaymentCustomData.class),
                    () -> then(orderService).should().findOrderWithAllExceptMember(anyString()),
                    () -> then(orderService).should().getOrderRequest(multipleOrder),
                    () -> then(productService).should().findProducts(),
                    () -> then(paymentRepository).should(never()).findWithOrderDelivery(any()),
                    () -> then(eventPublisher).should(never()).publishEvent(any())
            );
        }

        @Test
        void verifyAndComplete_orderNameMismatch_IsNotFirstName() {
            //given
            OrderProductDto orderProductDto1 = new OrderProductDto("BANG BANG", 15000, 5, 75000);
            OrderProductDto orderProductDto2 = new OrderProductDto("자바 ORM 표준 JPA 프로그래밍", 15000, 3, 45000);

            String json = "{\"orderNumber\":\"" + multipleOrder.getOrderNumber() + "\"}";
            given(paidPayment.getCustomData()).willReturn(json);

            given(orderService.findOrderWithAllExceptMember(anyString())).willReturn(multipleOrder);
            given(orderRequest.getOrderProducts()).willReturn(List.of(orderProductDto1, orderProductDto2));
            given(orderService.getOrderRequest(any())).willReturn(orderRequest);

            given(productService.findProducts()).willReturn(List.of(album, book, movie));

            given(paidPayment.getAmount().getTotal()).willReturn(multipleOrder.getTotalAmount().longValue());
            given(orderRequest.getTotalAmount()).willReturn(multipleOrder.getTotalAmount());

            String orderName = orderRequest.getOrderProducts().getLast().getName() + " 외 " + (orderRequest.getOrderProducts().size() - 1) + "건";

            given(paidPayment.getOrderName()).willReturn(orderName);

            //when
            assertThatThrownBy(() -> paymentService.verifyAndComplete(paidPayment))
                    .isInstanceOf(SyncPaymentException.class);

            //then
            assertAll(
                    () -> then(objectMapper).should().readValue(paidPayment.getCustomData(), PaymentCustomData.class),
                    () -> then(orderService).should().findOrderWithAllExceptMember(anyString()),
                    () -> then(orderService).should().getOrderRequest(multipleOrder),
                    () -> then(productService).should().findProducts(),
                    () -> then(paymentRepository).should(never()).findWithOrderDelivery(any()),
                    () -> then(eventPublisher).should(never()).publishEvent(any())
            );
        }

        @Test
        void verifyAndComplete_orderNameMismatch_multipleOrderProducts_withSingleFormat() {
            //given
            OrderProductDto orderProductDto1 = new OrderProductDto("BANG BANG", 15000, 5, 75000);
            OrderProductDto orderProductDto2 = new OrderProductDto("자바 ORM 표준 JPA 프로그래밍", 15000, 3, 45000);

            String json = "{\"orderNumber\":\"" + multipleOrder.getOrderNumber() + "\"}";
            given(paidPayment.getCustomData()).willReturn(json);

            given(orderService.findOrderWithAllExceptMember(anyString())).willReturn(multipleOrder);
            given(orderRequest.getOrderProducts()).willReturn(List.of(orderProductDto1, orderProductDto2));
            given(orderService.getOrderRequest(any())).willReturn(orderRequest);

            given(productService.findProducts()).willReturn(List.of(album, book, movie));

            given(paidPayment.getAmount().getTotal()).willReturn(multipleOrder.getTotalAmount().longValue());
            given(orderRequest.getTotalAmount()).willReturn(multipleOrder.getTotalAmount());

            String orderName = orderRequest.getOrderProducts().getFirst().getName();

            given(paidPayment.getOrderName()).willReturn(orderName);

            //when
            assertThatThrownBy(() -> paymentService.verifyAndComplete(paidPayment))
                    .isInstanceOf(SyncPaymentException.class);

            //then
            assertAll(
                    () -> then(objectMapper).should().readValue(paidPayment.getCustomData(), PaymentCustomData.class),
                    () -> then(orderService).should().findOrderWithAllExceptMember(anyString()),
                    () -> then(orderService).should().getOrderRequest(multipleOrder),
                    () -> then(productService).should().findProducts(),
                    () -> then(paymentRepository).should(never()).findWithOrderDelivery(any()),
                    () -> then(eventPublisher).should(never()).publishEvent(any())
            );
        }

        @Test
        void verifyAndComplete_wrongPaymentId() {
            //given
            Order notRequestOrder = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));

            OrderProductDto orderProductDto1 = new OrderProductDto("BANG BANG", 15000, 5, 75000);
            OrderProductDto orderProductDto2 = new OrderProductDto("자바 ORM 표준 JPA 프로그래밍", 15000, 3, 45000);

            String json = "{\"orderNumber\":\"" + notRequestOrder.getOrderNumber() + "\"}";
            given(paidPayment.getCustomData()).willReturn(json);

            given(orderService.findOrderWithAllExceptMember(anyString())).willReturn(notRequestOrder);
            given(orderRequest.getOrderProducts()).willReturn(List.of(orderProductDto1, orderProductDto2));
            given(orderService.getOrderRequest(any())).willReturn(orderRequest);

            given(productService.findProducts()).willReturn(List.of(album, book, movie));

            given(paidPayment.getAmount().getTotal()).willReturn(notRequestOrder.getTotalAmount().longValue());
            given(orderRequest.getTotalAmount()).willReturn(notRequestOrder.getTotalAmount());

            String orderName = orderRequest.getOrderProducts().getFirst().getName() + " 외 " + (orderRequest.getOrderProducts().size() - 1) + "건";

            given(paidPayment.getOrderName()).willReturn(orderName);

            long total = paidPayment.getAmount().getTotal();
            given(paidPayment.toString()).willReturn("orderName = " + orderName + ", total = " + total);

            String paymentId = "jfdioj23489fkjn2";
            given(paidPayment.getId()).willReturn(paymentId);
            given(paymentRepository.findWithOrderDelivery(anyString())).willReturn(Optional.empty());

            //when
            assertThatThrownBy(() -> paymentService.verifyAndComplete(paidPayment))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 결제 번호입니다.");

            //then
            assertAll(
                    () -> then(objectMapper).should().readValue(paidPayment.getCustomData(), PaymentCustomData.class),
                    () -> then(orderService).should().findOrderWithAllExceptMember(anyString()),
                    () -> then(orderService).should().getOrderRequest(notRequestOrder),
                    () -> then(productService).should().findProducts(),
                    () -> then(paymentRepository).should().findWithOrderDelivery(anyString()),
                    () -> then(eventPublisher).should(never()).publishEvent(any())
            );
        }

        @Test
        void verifyAndComplete_failedEventPublisher() {
            //given
            OrderProductDto orderProductDto1 = new OrderProductDto("BANG BANG", 15000, 5, 75000);
            OrderProductDto orderProductDto2 = new OrderProductDto("자바 ORM 표준 JPA 프로그래밍", 15000, 3, 45000);

            String json = "{\"orderNumber\":\"" + multipleOrder.getOrderNumber() + "\"}";
            given(paidPayment.getCustomData()).willReturn(json);

            given(orderService.findOrderWithAllExceptMember(anyString())).willReturn(multipleOrder);
            given(orderRequest.getOrderProducts()).willReturn(List.of(orderProductDto1, orderProductDto2));
            given(orderService.getOrderRequest(any())).willReturn(orderRequest);

            given(productService.findProducts()).willReturn(List.of(album, book, movie));

            given(paidPayment.getAmount().getTotal()).willReturn(multipleOrder.getTotalAmount().longValue());
            given(orderRequest.getTotalAmount()).willReturn(multipleOrder.getTotalAmount());

            String orderName = orderRequest.getOrderProducts().getFirst().getName() + " 외 " + (orderRequest.getOrderProducts().size() - 1) + "건";

            given(paidPayment.getOrderName()).willReturn(orderName);

            long total = paidPayment.getAmount().getTotal();
            given(paidPayment.toString()).willReturn("orderName = " + orderName + ", total = " + total);

            String paymentId = multipleOrder.getPayment().getPaymentId();
            given(paidPayment.getId()).willReturn(paymentId);
            given(paymentRepository.findWithOrderDelivery(anyString())).willReturn(Optional.of(multipleOrder.getPayment()));
            given(paidPayment.getPaidAt()).willReturn(LocalDateTime.now().atZone(ZoneId.of("Asia/Seoul")).toInstant());

            willThrow(new RuntimeException("Event Publish Failed")).given(eventPublisher).publishEvent(any(PaymentCompletedEvent.class));

            //when
            assertThatThrownBy(() -> paymentService.verifyAndComplete(paidPayment))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Event Publish Failed");

            //then
            assertAll(
                    () -> then(objectMapper).should().readValue(paidPayment.getCustomData(), PaymentCustomData.class),
                    () -> then(orderService).should().findOrderWithAllExceptMember(anyString()),
                    () -> then(orderService).should().getOrderRequest(multipleOrder),
                    () -> then(productService).should().findProducts(),
                    () -> then(paymentRepository).should().findWithOrderDelivery(anyString()),
                    () -> then(eventPublisher).should().publishEvent(any(PaymentCompletedEvent.class))
            );
        }
    }

/*
    @Test
    void failedPayment() {
        //when
        Payment failedPayment = paymentService.failedPayment(paymentId);

        //then
        assertThat(failedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(failedPayment.getPaymentDate()).isNull();
    }

    @Test
    void failedPayment_reFailed() {
        //given
        paymentService.failedPayment(paymentId);

        //when
        Payment failedPayment = paymentService.failedPayment(paymentId);

        //then
        assertThat(failedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(failedPayment.getPaymentDate()).isNull();
    }

*/
/*


    @Test
    void delete_order() {
        //given
        orderService.cancelOrder(orderNumber1);

        //when
        orderService.deleteOrder(orderNumber1);

        //then
        assertThrows(IllegalArgumentException.class, () ->
                paymentService.findPaymentByPaymentId(paymentId));
    }
*/
}
