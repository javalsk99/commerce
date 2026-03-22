package lsk.commerce.service;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import lsk.commerce.dto.response.OrderPaymentResponse;
import lsk.commerce.event.PaymentCompletedEvent;
import lsk.commerce.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.BDDMockito;
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

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    OrderPaymentResponse orderPaymentResponse;

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
    String wrongPaymentId = "lllIIIll00OOII1111llO0O0Il1Il100OOlI";

    @BeforeEach
    void beforeEach() {
        member = Member.builder()
                .city("Seoul")
                .street("Gangnam")
                .zipcode("01234")
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
        orderProduct3 = OrderProduct.createOrderProduct(movie, 2);

        singleOrder = Order.createOrder(member, delivery, List.of(orderProduct3));
        multipleOrder = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));
    }

    @Nested
    class Request {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                given(orderService.findOrderWithDeliveryPayment(anyString())).willReturn(multipleOrder);

                //when
                paymentService.request(multipleOrder.getOrderNumber());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderService).should().findOrderWithDeliveryPayment(anyString()));
                    softly.check(() -> BDDMockito.then(paymentRepository).should().save(multipleOrder.getPayment()));
                });
                then(multipleOrder.getPayment())
                        .extracting("order", "paymentAmount", "paymentDate", "paymentStatus")
                        .containsExactly(multipleOrder, multipleOrder.getTotalAmount(), null, PaymentStatus.PENDING);
            }
        }

        @Nested
        class FailureCase {

            @Test
            void orderNotFound() {
                //given
                given(orderService.findOrderWithDeliveryPayment(anyString())).willThrow(new IllegalArgumentException("존재하지 않는 주문입니다"));

                //when & then
                thenThrownBy(() -> paymentService.request(wrongOrderNumber))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("존재하지 않는 주문입니다");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderService).should().findOrderWithDeliveryPayment(anyString()));
                    softly.check(() -> BDDMockito.then(paymentRepository).should(never()).save(any()));
                });
            }

            @Test
            void alreadyRequest() {
                //given
                given(orderService.findOrderWithDeliveryPayment(anyString())).willReturn(multipleOrder);

                //when 첫 번째 호출
                paymentService.request(multipleOrder.getOrderNumber());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderService).should().findOrderWithDeliveryPayment(anyString()));
                    softly.check(() -> BDDMockito.then(paymentRepository).should().save(multipleOrder.getPayment()));
                });
                then(multipleOrder.getPayment())
                        .extracting("order", "paymentAmount", "paymentDate", "paymentStatus")
                        .containsExactly(multipleOrder, multipleOrder.getTotalAmount(), null, PaymentStatus.PENDING);

                //when & then 두 번째 호출
                thenThrownBy(() -> paymentService.request(multipleOrder.getOrderNumber()))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("이미 결제 정보가 있습니다");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderService).should(times(2)).findOrderWithDeliveryPayment(anyString()));
                    softly.check(() -> BDDMockito.then(paymentRepository).should().save(any()));
                });
            }
        }
    }

    abstract class Setup {

        @BeforeEach
        void beforeEach() {
            Payment.requestPayment(singleOrder);
            Payment.requestPayment(multipleOrder);
        }
    }

    @Nested
    class Find extends Setup {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                given(paymentRepository.findWithOrder(anyString())).willReturn(Optional.of(multipleOrder.getPayment()));

                //when
                Payment payment = paymentService.findPaymentByPaymentId(multipleOrder.getPayment().getPaymentId());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(paymentRepository).should().findWithOrder(anyString()));
                    softly.check(() -> then(payment).isEqualTo(multipleOrder.getPayment()));
                });
            }
        }

        @Nested
        class FailureCase {

            @Test
            void paymentNotFound() {
                //given
                given(paymentRepository.findWithOrder(anyString())).willReturn(Optional.empty());

                //when & then
                thenThrownBy(() -> paymentService.findPaymentByPaymentId(wrongPaymentId))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("존재하지 않는 결제 번호입니다");
            }
        }
    }

    @Nested
    class VerityAndComplete extends Setup {

        @Nested
        class SuccessCase {

            @Test
            void hasSingleOrderProduct() throws JsonProcessingException {
                //given
                OrderProductDto orderProductDto = new OrderProductDto("범죄도시", 15000, 2, 30000);

                givenCustomData(singleOrder.getOrderNumber());

                findOrderAndProducts(singleOrder, List.of(orderProductDto));

                String orderName = givenOrderNameAndAmount(singleOrder, orderPaymentResponse);
                paidPaymentToString(orderName);

                givenCompletePayment(singleOrder);

                //when
                paymentService.verifyAndComplete(paidPayment);

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(objectMapper).should().readValue(paidPayment.getCustomData(), PaymentCustomData.class));
                    softly.check(() -> BDDMockito.then(orderService).should().findOrderWithAllExceptMember(anyString()));
                    softly.check(() -> BDDMockito.then(orderService).should().getOrderPaymentResponse(singleOrder));
                    softly.check(() -> BDDMockito.then(productService).should().findProducts());
                    softly.check(() -> BDDMockito.then(paymentRepository).should().findWithOrderDelivery(anyString()));
                    softly.check(() -> BDDMockito.then(eventPublisher).should().publishEvent(any(PaymentCompletedEvent.class)));
                });
                then(paidPayment)
                        .extracting("amount.total", "orderName", "id", "paidAt")
                        .containsExactly(
                                30000L, orderPaymentResponse.orderProductDtoList().getFirst().name(), singleOrder.getPayment().getPaymentId(),
                                singleOrder.getPayment().getPaymentDate().atZone(ZoneId.of("Asia/Seoul")).toInstant()
                        );

                PaymentCustomData paymentCustomData = objectMapper.readValue(paidPayment.getCustomData(), PaymentCustomData.class);
                then(paymentCustomData.orderNumber()).isEqualTo(singleOrder.getOrderNumber());
            }

            @Test
            void hasMultipleOrderProducts() throws JsonProcessingException {
                //given
                OrderProductDto orderProductDto1 = new OrderProductDto("BANG BANG", 15000, 5, 75000);
                OrderProductDto orderProductDto2 = new OrderProductDto("자바 ORM 표준 JPA 프로그래밍", 15000, 3, 45000);

                givenCustomData(multipleOrder.getOrderNumber());

                findOrderAndProducts(multipleOrder, List.of(orderProductDto1, orderProductDto2));

                String orderName = givenOrderNameAndAmount(multipleOrder, orderPaymentResponse);
                paidPaymentToString(orderName);

                givenCompletePayment(multipleOrder);

                //when
                paymentService.verifyAndComplete(paidPayment);

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(objectMapper).should().readValue(paidPayment.getCustomData(), PaymentCustomData.class));
                    softly.check(() -> BDDMockito.then(orderService).should().findOrderWithAllExceptMember(anyString()));
                    softly.check(() -> BDDMockito.then(orderService).should().getOrderPaymentResponse(multipleOrder));
                    softly.check(() -> BDDMockito.then(productService).should().findProducts());
                    softly.check(() -> BDDMockito.then(paymentRepository).should().findWithOrderDelivery(anyString()));
                    softly.check(() -> BDDMockito.then(eventPublisher).should().publishEvent(any(PaymentCompletedEvent.class)));
                });
                then(paidPayment)
                        .extracting("amount.total", "orderName", "id", "paidAt")
                        .containsExactly(
                                120000L, orderPaymentResponse.orderProductDtoList().getFirst().name() + " 외 " + (orderPaymentResponse.orderProductDtoList().size() - 1) + "건",
                                multipleOrder.getPayment().getPaymentId(), multipleOrder.getPayment().getPaymentDate().atZone(ZoneId.of("Asia/Seoul")).toInstant()
                        );

                PaymentCustomData paymentCustomData = objectMapper.readValue(paidPayment.getCustomData(), PaymentCustomData.class);
                then(paymentCustomData.orderNumber()).isEqualTo(multipleOrder.getOrderNumber());
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
            void orderNotFound() {
                //given
                givenCustomData(multipleOrder.getOrderNumber());

                given(orderService.findOrderWithAllExceptMember(anyString())).willThrow(new IllegalArgumentException("존재하지 않는 주문입니다"));

                //when & then
                thenThrownBy(() -> paymentService.verifyAndComplete(paidPayment))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("존재하지 않는 주문입니다");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(objectMapper).should().readValue(paidPayment.getCustomData(), PaymentCustomData.class));
                    softly.check(() -> BDDMockito.then(orderService).should().findOrderWithAllExceptMember(anyString()));
                    softly.check(() -> BDDMockito.then(orderService).should(never()).getOrderPaymentResponse(any()));
                    softly.check(() -> BDDMockito.then(productService).should(never()).findProducts());
                    softly.check(() -> BDDMockito.then(paymentRepository).should(never()).findWithOrderDelivery(any()));
                    softly.check(() -> BDDMockito.then(eventPublisher).should(never()).publishEvent(any()));
                });
            }

            @Test
            void orderProductDtoIsEmpty() {
                //given
                givenCustomData(multipleOrder.getOrderNumber());

                given(orderService.findOrderWithAllExceptMember(anyString())).willReturn(multipleOrder);
                given(orderPaymentResponse.orderProductDtoList()).willReturn(Collections.emptyList());
                given(orderService.getOrderPaymentResponse(any())).willReturn(orderPaymentResponse);

                given(productService.findProducts()).willReturn(List.of(album, book, movie));

                //when & then
                thenThrownBy(() -> paymentService.verifyAndComplete(paidPayment))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("주문 상품이 비어 있습니다");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(objectMapper).should().readValue(paidPayment.getCustomData(), PaymentCustomData.class));
                    softly.check(() -> BDDMockito.then(orderService).should().findOrderWithAllExceptMember(anyString()));
                    softly.check(() -> BDDMockito.then(orderService).should().getOrderPaymentResponse(multipleOrder));
                    softly.check(() -> BDDMockito.then(productService).should().findProducts());
                    softly.check(() -> BDDMockito.then(paymentRepository).should(never()).findWithOrderDelivery(any()));
                    softly.check(() -> BDDMockito.then(eventPublisher).should(never()).publishEvent(any()));
                });
            }

            @Test
            void wrongProducts() {
                //given
                OrderProductDto orderProductDto1 = new OrderProductDto("BANG BANG", 15000, 5, 75000);
                OrderProductDto orderProductDto2 = new OrderProductDto("자바 ORM 표준 JPA 프로그래밍", 15000, 3, 45000);

                givenCustomData(multipleOrder.getOrderNumber());

                given(orderService.findOrderWithAllExceptMember(anyString())).willReturn(multipleOrder);
                given(orderPaymentResponse.orderProductDtoList()).willReturn(List.of(orderProductDto1, orderProductDto2));
                given(orderService.getOrderPaymentResponse(any())).willReturn(orderPaymentResponse);

                given(productService.findProducts()).willReturn(List.of(movie));

                //when & then
                thenThrownBy(() -> paymentService.verifyAndComplete(paidPayment))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("잘못된 상품이 있습니다");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(objectMapper).should().readValue(paidPayment.getCustomData(), PaymentCustomData.class));
                    softly.check(() -> BDDMockito.then(orderService).should().findOrderWithAllExceptMember(anyString()));
                    softly.check(() -> BDDMockito.then(orderService).should().getOrderPaymentResponse(multipleOrder));
                    softly.check(() -> BDDMockito.then(productService).should().findProducts());
                    softly.check(() -> BDDMockito.then(paymentRepository).should(never()).findWithOrderDelivery(any()));
                    softly.check(() -> BDDMockito.then(eventPublisher).should(never()).publishEvent(any()));
                });
            }

            @Test
            void amountMismatch() {
                //given
                OrderProductDto orderProductDto1 = new OrderProductDto("BANG BANG", 15000, 5, 75000);
                OrderProductDto orderProductDto2 = new OrderProductDto("자바 ORM 표준 JPA 프로그래밍", 15000, 3, 45000);

                givenCustomData(multipleOrder.getOrderNumber());

                findOrderAndProducts(multipleOrder, List.of(orderProductDto1, orderProductDto2));

                given(paidPayment.getAmount().getTotal()).willReturn(10000L);
                given(orderPaymentResponse.totalAmount()).willReturn(120000);

                //when & then
                thenThrownBy(() -> paymentService.verifyAndComplete(paidPayment))
                        .isInstanceOf(SyncPaymentException.class);

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(objectMapper).should().readValue(paidPayment.getCustomData(), PaymentCustomData.class));
                    softly.check(() -> BDDMockito.then(orderService).should().findOrderWithAllExceptMember(anyString()));
                    softly.check(() -> BDDMockito.then(orderService).should().getOrderPaymentResponse(multipleOrder));
                    softly.check(() -> BDDMockito.then(productService).should().findProducts());
                    softly.check(() -> BDDMockito.then(paymentRepository).should(never()).findWithOrderDelivery(any()));
                    softly.check(() -> BDDMockito.then(eventPublisher).should(never()).publishEvent(any()));
                });
            }

            @Test
            void orderNameMismatch_IsNotFirstName() {
                //given
                OrderProductDto orderProductDto1 = new OrderProductDto("BANG BANG", 15000, 5, 75000);
                OrderProductDto orderProductDto2 = new OrderProductDto("자바 ORM 표준 JPA 프로그래밍", 15000, 3, 45000);

                givenCustomData(multipleOrder.getOrderNumber());

                findOrderAndProducts(multipleOrder, List.of(orderProductDto1, orderProductDto2));

                given(paidPayment.getAmount().getTotal()).willReturn(multipleOrder.getTotalAmount().longValue());
                given(orderPaymentResponse.totalAmount()).willReturn(multipleOrder.getTotalAmount());

                String orderName = orderPaymentResponse.orderProductDtoList().getLast().name() + " 외 " + (orderPaymentResponse.orderProductDtoList().size() - 1) + "건";

                given(paidPayment.getOrderName()).willReturn(orderName);

                //when & then
                thenThrownBy(() -> paymentService.verifyAndComplete(paidPayment))
                        .isInstanceOf(SyncPaymentException.class);

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(objectMapper).should().readValue(paidPayment.getCustomData(), PaymentCustomData.class));
                    softly.check(() -> BDDMockito.then(orderService).should().findOrderWithAllExceptMember(anyString()));
                    softly.check(() -> BDDMockito.then(orderService).should().getOrderPaymentResponse(multipleOrder));
                    softly.check(() -> BDDMockito.then(productService).should().findProducts());
                    softly.check(() -> BDDMockito.then(paymentRepository).should(never()).findWithOrderDelivery(any()));
                    softly.check(() -> BDDMockito.then(eventPublisher).should(never()).publishEvent(any()));
                });
            }

            @Test
            void orderNameMismatch_MultipleOrderProducts_WithSingleFormat() {
                //given
                OrderProductDto orderProductDto1 = new OrderProductDto("BANG BANG", 15000, 5, 75000);
                OrderProductDto orderProductDto2 = new OrderProductDto("자바 ORM 표준 JPA 프로그래밍", 15000, 3, 45000);

                givenCustomData(multipleOrder.getOrderNumber());

                findOrderAndProducts(multipleOrder, List.of(orderProductDto1, orderProductDto2));

                given(paidPayment.getAmount().getTotal()).willReturn(multipleOrder.getTotalAmount().longValue());
                given(orderPaymentResponse.totalAmount()).willReturn(multipleOrder.getTotalAmount());

                String orderName = orderPaymentResponse.orderProductDtoList().getFirst().name();

                given(paidPayment.getOrderName()).willReturn(orderName);

                //when & then
                thenThrownBy(() -> paymentService.verifyAndComplete(paidPayment))
                        .isInstanceOf(SyncPaymentException.class);

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(objectMapper).should().readValue(paidPayment.getCustomData(), PaymentCustomData.class));
                    softly.check(() -> BDDMockito.then(orderService).should().findOrderWithAllExceptMember(anyString()));
                    softly.check(() -> BDDMockito.then(orderService).should().getOrderPaymentResponse(multipleOrder));
                    softly.check(() -> BDDMockito.then(productService).should().findProducts());
                    softly.check(() -> BDDMockito.then(paymentRepository).should(never()).findWithOrderDelivery(any()));
                    softly.check(() -> BDDMockito.then(eventPublisher).should(never()).publishEvent(any()));
                });
            }

            @Test
            void wrongPaymentId() {
                //given
                Order notRequestOrder = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));

                OrderProductDto orderProductDto1 = new OrderProductDto("BANG BANG", 15000, 5, 75000);
                OrderProductDto orderProductDto2 = new OrderProductDto("자바 ORM 표준 JPA 프로그래밍", 15000, 3, 45000);

                givenCustomData(notRequestOrder.getOrderNumber());

                findOrderAndProducts(notRequestOrder, List.of(orderProductDto1, orderProductDto2));

                String orderName = givenOrderNameAndAmount(multipleOrder, orderPaymentResponse);
                paidPaymentToString(orderName);

                given(paidPayment.getId()).willReturn(wrongPaymentId);
                given(paymentRepository.findWithOrderDelivery(anyString())).willReturn(Optional.empty());

                //when & then
                thenThrownBy(() -> paymentService.verifyAndComplete(paidPayment))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("존재하지 않는 결제 번호입니다");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(objectMapper).should().readValue(paidPayment.getCustomData(), PaymentCustomData.class));
                    softly.check(() -> BDDMockito.then(orderService).should().findOrderWithAllExceptMember(anyString()));
                    softly.check(() -> BDDMockito.then(orderService).should().getOrderPaymentResponse(notRequestOrder));
                    softly.check(() -> BDDMockito.then(productService).should().findProducts());
                    softly.check(() -> BDDMockito.then(paymentRepository).should().findWithOrderDelivery(anyString()));
                    softly.check(() -> BDDMockito.then(eventPublisher).should(never()).publishEvent(any()));
                });
            }

            @Test
            void failedEventPublisher() {
                //given
                OrderProductDto orderProductDto1 = new OrderProductDto("BANG BANG", 15000, 5, 75000);
                OrderProductDto orderProductDto2 = new OrderProductDto("자바 ORM 표준 JPA 프로그래밍", 15000, 3, 45000);

                givenCustomData(multipleOrder.getOrderNumber());

                findOrderAndProducts(multipleOrder, List.of(orderProductDto1, orderProductDto2));

                String orderName = givenOrderNameAndAmount(multipleOrder, orderPaymentResponse);
                paidPaymentToString(orderName);

                String paymentId = multipleOrder.getPayment().getPaymentId();
                given(paidPayment.getId()).willReturn(paymentId);
                given(paymentRepository.findWithOrderDelivery(anyString())).willReturn(Optional.of(multipleOrder.getPayment()));
                given(paidPayment.getPaidAt()).willReturn(LocalDateTime.now().atZone(ZoneId.of("Asia/Seoul")).toInstant());

                willThrow(new RuntimeException("Event Publish Failed")).given(eventPublisher).publishEvent(any(PaymentCompletedEvent.class));

                //when & then
                thenThrownBy(() -> paymentService.verifyAndComplete(paidPayment))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessage("Event Publish Failed");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(objectMapper).should().readValue(paidPayment.getCustomData(), PaymentCustomData.class));
                    softly.check(() -> BDDMockito.then(orderService).should().findOrderWithAllExceptMember(anyString()));
                    softly.check(() -> BDDMockito.then(orderService).should().getOrderPaymentResponse(multipleOrder));
                    softly.check(() -> BDDMockito.then(productService).should().findProducts());
                    softly.check(() -> BDDMockito.then(paymentRepository).should().findWithOrderDelivery(anyString()));
                    softly.check(() -> BDDMockito.then(eventPublisher).should().publishEvent(any(PaymentCompletedEvent.class)));
                });
            }
        }

        private void givenCustomData(String orderNumber) {
            String json = "{\"orderNumber\":\"" + orderNumber + "\"}";
            given(paidPayment.getCustomData()).willReturn(json);
        }

        private void findOrderAndProducts(Order order, List<OrderProductDto> orderProductDto) {
            given(orderService.findOrderWithAllExceptMember(anyString())).willReturn(order);
            given(orderPaymentResponse.orderProductDtoList()).willReturn(orderProductDto);
            given(orderService.getOrderPaymentResponse(any())).willReturn(orderPaymentResponse);

            given(productService.findProducts()).willReturn(List.of(album, book, movie));
        }

        private String givenOrderNameAndAmount(Order order, OrderPaymentResponse orderRequest) {
            given(paidPayment.getAmount().getTotal()).willReturn(order.getTotalAmount().longValue());
            given(orderRequest.totalAmount()).willReturn(order.getTotalAmount());

            String orderName;
            if (orderRequest.orderProductDtoList().size() == 1) {
                orderName = orderRequest.orderProductDtoList().getFirst().name();
            } else {
                orderName = orderRequest.orderProductDtoList().getFirst().name() + " 외 " + (orderRequest.orderProductDtoList().size() - 1) + "건";
            }

            given(paidPayment.getOrderName()).willReturn(orderName);
            return orderName;
        }

        private void paidPaymentToString(String orderName) {
            long total = paidPayment.getAmount().getTotal();
            given(paidPayment.toString()).willReturn("orderName = " + orderName + ", total = " + total);
        }
    }

    @Nested
    class FailedPayment extends Setup {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                given(paymentRepository.findWithOrder(anyString())).willReturn(Optional.of(multipleOrder.getPayment()));

                //when
                paymentService.failedPayment(multipleOrder.getPayment().getPaymentId());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(paymentRepository).should().findWithOrder(anyString()));
                    softly.check(() -> then(multipleOrder.getPayment().getPaymentStatus()).isEqualTo(PaymentStatus.FAILED));
                });
            }
        }

        @Nested
        class FailureCase {

            @Test
            void paymentNotFound() {
                //given
                given(paymentRepository.findWithOrder(anyString())).willReturn(Optional.empty());

                //when & then
                thenThrownBy(() -> paymentService.failedPayment(wrongPaymentId))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("존재하지 않는 결제 번호입니다");

                //then
                BDDMockito.then(paymentRepository).should().findWithOrder(anyString());
            }
        }
    }
}
