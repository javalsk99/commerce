package lsk.commerce.service;

import jakarta.persistence.EntityManager;
import lsk.commerce.domain.Delivery;
import lsk.commerce.domain.DeliveryStatus;
import lsk.commerce.domain.Member;
import lsk.commerce.domain.Order;
import lsk.commerce.domain.OrderProduct;
import lsk.commerce.domain.OrderStatus;
import lsk.commerce.domain.Payment;
import lsk.commerce.domain.PaymentStatus;
import lsk.commerce.domain.product.Album;
import lsk.commerce.domain.product.Book;
import lsk.commerce.domain.product.Movie;
import lsk.commerce.dto.request.OrderChangeRequest;
import lsk.commerce.dto.request.OrderCreateRequest;
import lsk.commerce.dto.request.OrderProductRequest;
import lsk.commerce.dto.response.OrderChangeResponse;
import lsk.commerce.exception.DataNotFoundException;
import lsk.commerce.exception.InvalidDataException;
import lsk.commerce.exception.NotResourceOwnerException;
import lsk.commerce.repository.OrderProductJdbcRepository;
import lsk.commerce.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenNoException;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.assertj.core.api.BDDAssertions.tuple;
import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyList;
import static org.mockito.BDDMockito.anyLong;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.inOrder;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    EntityManager em;

    @Mock
    OrderRepository orderRepository;

    @Mock
    OrderProductJdbcRepository orderProductJdbcRepository;

    @Mock
    MemberService memberService;

    @Mock
    ProductService productService;

    @InjectMocks
    OrderService orderService;

    @Captor
    ArgumentCaptor<Order> orderCaptor;

    @Captor
    ArgumentCaptor<List<OrderProduct>> orderProductCaptor;

    Member member;
    Delivery delivery;
    Album album;
    Book book;
    Movie movie;
    String productNumber1;
    String productNumber2;
    String productNumber3;
    String wrongOrderNumber = "ll1lI1IlOO00";

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
                .artist("IVE")
                .studio("STARSHIP")
                .build();
        book = Book.builder()
                .name("자바 ORM 표준 JPA 프로그래밍")
                .price(15000)
                .stockQuantity(7)
                .author("김영한")
                .isbn("9788960777330")
                .build();
        movie = Movie.builder()
                .name("범죄도시")
                .price(15000)
                .stockQuantity(5)
                .actor("마동석")
                .director("강윤성")
                .build();

        productNumber1 = album.getProductNumber();
        productNumber2 = book.getProductNumber();
        productNumber3 = movie.getProductNumber();
    }

    @Nested
    class Create {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                OrderProductRequest orderProductRequest1 = new OrderProductRequest(productNumber1, 3);
                OrderProductRequest orderProductRequest2 = new OrderProductRequest(productNumber2, 2);
                OrderProductRequest orderProductRequest3 = new OrderProductRequest(productNumber3, 4);
                OrderCreateRequest request = new OrderCreateRequest(List.of(orderProductRequest1, orderProductRequest2, orderProductRequest3));

                given(memberService.findMemberByLoginId(anyString())).willReturn(member);
                given(productService.findProducts()).willReturn(List.of(album, book, movie));

                InOrder inOrder = inOrder(em, orderRepository, orderProductJdbcRepository);

                //when
                orderService.order(request, "id_A");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(memberService).should().findMemberByLoginId(anyString()));
                    softly.check(() -> BDDMockito.then(productService).should().findProducts());
                });

                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderRepository).should(inOrder).save(orderCaptor.capture()));
                    softly.check(() -> BDDMockito.then(em).should(inOrder).flush());
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should(inOrder).saveAll(orderProductCaptor.capture()));
                    softly.check(() -> BDDMockito.then(em).should(inOrder).clear());
                });

                Order order = orderCaptor.getValue();
                List<OrderProduct> orderProducts = orderProductCaptor.getValue();

                thenSoftly(softly -> {
                    softly.then(order.getOrderProducts()).isEqualTo(orderProducts);
                    softly.then(order.getOrderProducts())
                            .extracting("product.name", "product.price", "quantity", "orderPrice")
                            .containsExactlyInAnyOrder(
                                    tuple("BANG BANG", 15000, 3, 45000),
                                    tuple("자바 ORM 표준 JPA 프로그래밍", 15000, 2, 30000),
                                    tuple("범죄도시", 15000, 4, 60000));
                    softly.then(order.getTotalAmount()).isEqualTo(135000);
                });
                thenSoftly(softly -> {
                    softly.then(order.getOrderStatus()).isEqualTo(OrderStatus.CREATED);
                    softly.then(order.getDelivery().getDeliveryStatus()).isEqualTo(DeliveryStatus.WAITING);
                    softly.then(order.getPayment()).isNull();
                });
                thenSoftly(softly -> {
                    softly.then(album.getStockQuantity()).isEqualTo(7);
                    softly.then(book.getStockQuantity()).isEqualTo(5);
                    softly.then(movie.getStockQuantity()).isEqualTo(1);
                });
            }
        }

        @Nested
        class FailureCase {

            @Test
            void memberNotFound() {
                //given
                OrderProductRequest orderProductRequest1 = new OrderProductRequest(productNumber1, 3);
                OrderProductRequest orderProductRequest2 = new OrderProductRequest(productNumber2, 2);
                OrderProductRequest orderProductRequest3 = new OrderProductRequest(productNumber3, 4);
                OrderCreateRequest request = new OrderCreateRequest(List.of(orderProductRequest1, orderProductRequest2, orderProductRequest3));

                given(memberService.findMemberByLoginId(anyString())).willThrow(new DataNotFoundException("존재하지 않는 아이디입니다"));

                //when & then
                thenThrownBy(() -> orderService.order(request, "id_D"))
                        .isInstanceOf(DataNotFoundException.class)
                        .hasMessage("존재하지 않는 아이디입니다");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(memberService).should().findMemberByLoginId(anyString()));
                    softly.check(() -> BDDMockito.then(productService).should(never()).findProducts());
                    softly.check(() -> BDDMockito.then(orderRepository).should(never()).save(any()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should(never()).saveAll(any()));
                });
            }

            @Test
            void productNumberIsNull() {
                //given
                OrderProductRequest nullProductNumberOrderProductRequest = new OrderProductRequest(null, 3);
                OrderCreateRequest request = new OrderCreateRequest(List.of(nullProductNumberOrderProductRequest));

                given(memberService.findMemberByLoginId(anyString())).willReturn(member);
                given(productService.findProducts()).willReturn(List.of(album, book, movie));

                //when & then
                thenThrownBy(() -> orderService.order(request, "id_A"))
                        .isInstanceOf(DataNotFoundException.class)
                        .hasMessage("존재하지 않는 상품입니다. productNumber: " + "null");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(memberService).should().findMemberByLoginId(anyString()));
                    softly.check(() -> BDDMockito.then(productService).should().findProducts());
                    softly.check(() -> BDDMockito.then(orderRepository).should(never()).save(any()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should(never()).saveAll(any()));
                });
            }

            @Test
            void exceed() {
                OrderProductRequest exceedOrderProductRequest = new OrderProductRequest(productNumber1, 11);
                OrderCreateRequest request = new OrderCreateRequest(List.of(exceedOrderProductRequest));

                given(memberService.findMemberByLoginId(anyString())).willReturn(member);
                given(productService.findProducts()).willReturn(List.of(album, book, movie));

                //when & then
                thenThrownBy(() -> orderService.order(request, "id_A"))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("재고가 부족합니다. productNumber: " + productNumber1);

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(memberService).should().findMemberByLoginId(anyString()));
                    softly.check(() -> BDDMockito.then(productService).should().findProducts());
                    softly.check(() -> BDDMockito.then(orderRepository).should(never()).save(any()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should(never()).saveAll(any()));
                });
            }

            @Test
            void quantityIsNull() {
                //given
                OrderProductRequest nullQuantityOrderProductRequest = new OrderProductRequest(productNumber1, null);

                OrderCreateRequest request = new OrderCreateRequest(List.of(nullQuantityOrderProductRequest));

                given(memberService.findMemberByLoginId(anyString())).willReturn(member);
                given(productService.findProducts()).willReturn(List.of(album, book, movie));

                //when & then
                thenThrownBy(() -> orderService.order(request, "id_A"))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("수량이 없습니다");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(memberService).should().findMemberByLoginId(anyString()));
                    softly.check(() -> BDDMockito.then(productService).should().findProducts());
                    softly.check(() -> BDDMockito.then(orderRepository).should(never()).save(any()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should(never()).saveAll(any()));
                });
            }

            @Test
            void productNotFound() {
                //given
                OrderProductRequest notExistsProductNumberOrderProductRequest = new OrderProductRequest("lllIIIll00OO", 3);
                OrderCreateRequest request = new OrderCreateRequest(List.of(notExistsProductNumberOrderProductRequest));

                given(memberService.findMemberByLoginId(anyString())).willReturn(member);
                given(productService.findProducts()).willReturn(List.of(album, book, movie));

                //when & then
                thenThrownBy(() -> orderService.order(request, "id_A"))
                        .isInstanceOf(DataNotFoundException.class)
                        .hasMessage("존재하지 않는 상품입니다. productNumber: " + "lllIIIll00OO");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(memberService).should().findMemberByLoginId(anyString()));
                    softly.check(() -> BDDMockito.then(productService).should().findProducts());
                    softly.check(() -> BDDMockito.then(orderRepository).should(never()).save(any()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should(never()).saveAll(any()));
                });
            }

            @Test
            void failedSaveAll() {
                //given
                OrderProductRequest orderProductRequest1 = new OrderProductRequest(productNumber1, 3);
                OrderProductRequest orderProductRequest2 = new OrderProductRequest(productNumber2, 2);
                OrderProductRequest orderProductRequest3 = new OrderProductRequest(productNumber3, 4);
                OrderCreateRequest request = new OrderCreateRequest(List.of(orderProductRequest1, orderProductRequest2, orderProductRequest3));

                given(memberService.findMemberByLoginId(anyString())).willReturn(member);
                given(productService.findProducts()).willReturn(List.of(album, book, movie));
                willThrow(new RuntimeException("JDBC Batch INSERT Failed")).given(orderProductJdbcRepository).saveAll(anyList());

                //when & then
                thenThrownBy(() -> orderService.order(request, "id_A"))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessage("JDBC Batch INSERT Failed");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(memberService).should().findMemberByLoginId(anyString()));
                    softly.check(() -> BDDMockito.then(productService).should().findProducts());
                    softly.check(() -> BDDMockito.then(orderRepository).should().save(any()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should().saveAll(anyList()));
                });
            }
        }
    }

    abstract class Setup {

        OrderProduct orderProduct1;
        OrderProduct orderProduct2;
        OrderProduct orderProduct3;
        Order order;

        @BeforeEach
        void beforeEach() {
            orderProduct1 = OrderProduct.createOrderProduct(album, 3);
            orderProduct2 = OrderProduct.createOrderProduct(book, 2);
            orderProduct3 = OrderProduct.createOrderProduct(movie, 4);

            order = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2, orderProduct3));

            ReflectionTestUtils.setField(order, "id", 1L);
        }
    }

    @Nested
    class Find extends Setup {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                given(orderRepository.findByOrderNumber(anyString())).willReturn(Optional.of(order));

                //when
                Order findOrder = orderService.findOrder(order.getOrderNumber());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderRepository).should().findByOrderNumber(anyString()));
                    softly.then(findOrder).isEqualTo(order);
                });
            }
        }

        @Nested
        class FailureCase {

            @Test
            void orderNotFound() {
                //given
                given(orderRepository.findByOrderNumber(anyString())).willReturn(Optional.empty());

                //when & then
                thenThrownBy(() -> orderService.findOrder(wrongOrderNumber))
                        .isInstanceOf(DataNotFoundException.class)
                        .hasMessage("존재하지 않는 주문입니다. orderNumber: " + wrongOrderNumber);

                //then
                BDDMockito.then(orderRepository).should().findByOrderNumber(anyString());
            }
        }
    }

    @Nested
    class Change extends Setup {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                OrderProductRequest orderProductRequest1 = new OrderProductRequest(productNumber1, 2);
                OrderProductRequest orderProductRequest2 = new OrderProductRequest(productNumber2, 5);
                OrderChangeRequest request = new OrderChangeRequest(List.of(orderProductRequest1, orderProductRequest2));

                given(orderRepository.findWithAll(anyString())).willReturn(Optional.of(order));
                given(orderRepository.findByOrderNumber(anyString())).willReturn(Optional.of(order));
                given(productService.findProducts()).willReturn(List.of(album, book, movie));

                InOrder inOrder = inOrder(em, orderRepository, orderProductJdbcRepository);

                //when
                orderService.changeOrder(order.getOrderNumber(), request, "id_A");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderRepository).should(inOrder).findWithAll(anyString()));
                    softly.check(() -> BDDMockito.then(em).should(inOrder).flush());
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should(inOrder).deleteOrderProductsByOrderId(order.getId()));
                    softly.check(() -> BDDMockito.then(em).should(inOrder).clear());
                    softly.check(() -> BDDMockito.then(orderRepository).should(inOrder).findByOrderNumber(anyString()));
                });

                BDDMockito.then(productService).should().findProducts();

                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(em).should(inOrder).flush());
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should(inOrder).saveAll(orderProductCaptor.capture()));
                    softly.check(() -> BDDMockito.then(em).should(inOrder).clear());
                });

                List<OrderProduct> orderProducts = orderProductCaptor.getValue();
                thenSoftly(softly -> {
                    softly.then(orderProducts)
                            .extracting("product.name", "quantity", "orderPrice")
                            .containsExactlyInAnyOrder(
                                    tuple("BANG BANG", 2, 30000),
                                    tuple("자바 ORM 표준 JPA 프로그래밍", 5, 75000));
                    softly.then(orderProducts.getFirst().getOrder().getTotalAmount()).isEqualTo(105000);
                });
                thenSoftly(softly -> {
                    softly.then(album.getStockQuantity()).isEqualTo(8);
                    softly.then(book.getStockQuantity()).isEqualTo(2);
                    softly.then(movie.getStockQuantity()).isEqualTo(5);
                });
            }

            @Test
            void idempotency() {
                //given
                OrderProductRequest orderProductRequest1 = new OrderProductRequest(productNumber1, 2);
                OrderProductRequest orderProductRequest2 = new OrderProductRequest(productNumber2, 5);
                OrderChangeRequest request = new OrderChangeRequest(List.of(orderProductRequest1, orderProductRequest2));

                given(orderRepository.findWithAll(anyString())).willReturn(Optional.of(order));
                given(orderRepository.findByOrderNumber(anyString())).willReturn(Optional.of(order));
                given(productService.findProducts()).willReturn(List.of(album, book, movie));

                //when 첫 번째 호출
                orderService.changeOrder(order.getOrderNumber(), request, "id_A");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderRepository).should().findWithAll(anyString()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should().deleteOrderProductsByOrderId(order.getId()));
                    softly.check(() -> BDDMockito.then(orderRepository).should().findByOrderNumber(anyString()));
                    softly.check(() -> BDDMockito.then(productService).should().findProducts());
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should().saveAll(orderProductCaptor.capture()));
                });

                List<OrderProduct> orderProducts = orderProductCaptor.getValue();
                thenSoftly(softly -> {
                    softly.then(orderProducts)
                            .extracting("product.name", "quantity", "orderPrice")
                            .containsExactlyInAnyOrder(
                                    tuple("BANG BANG", 2, 30000),
                                    tuple("자바 ORM 표준 JPA 프로그래밍", 5, 75000));
                    softly.then(orderProducts.getFirst().getOrder().getTotalAmount()).isEqualTo(105000);
                });
                thenSoftly(softly -> {
                    softly.then(album.getStockQuantity()).isEqualTo(8);
                    softly.then(book.getStockQuantity()).isEqualTo(2);
                    softly.then(movie.getStockQuantity()).isEqualTo(5);
                });

                //when & then 두 번째 호출
                thenNoException().isThrownBy(() -> orderService.changeOrder(order.getOrderNumber(), request, "id_A"));

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderRepository).should(times(2)).findWithAll(anyString()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should(times(1)).deleteOrderProductsByOrderId(any()));
                    softly.check(() -> BDDMockito.then(orderRepository).should(times(1)).findByOrderNumber(any()));
                    softly.check(() -> BDDMockito.then(productService).should(times(1)).findProducts());
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should(times(1)).saveAll(any()));
                });

                thenSoftly(softly -> {
                    softly.then(album.getStockQuantity()).isEqualTo(8);
                    softly.then(book.getStockQuantity()).isEqualTo(2);
                    softly.then(movie.getStockQuantity()).isEqualTo(5);
                });
            }
        }

        @Nested
        class FailureCase {

            @Test
            void orderNotFound() {
                //given
                OrderProductRequest orderProductRequest1 = new OrderProductRequest(productNumber1, 2);
                OrderProductRequest orderProductRequest2 = new OrderProductRequest(productNumber2, 5);
                OrderChangeRequest request = new OrderChangeRequest(List.of(orderProductRequest1, orderProductRequest2));

                given(orderRepository.findWithAll(anyString())).willReturn(Optional.empty());

                //when & then
                thenThrownBy(() -> orderService.changeOrder(wrongOrderNumber, request, "id_A"))
                        .isInstanceOf(DataNotFoundException.class)
                        .hasMessage("존재하지 않는 주문입니다. orderNumber: " + wrongOrderNumber);

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderRepository).should().findWithAll(anyString()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should(never()).deleteOrderProductsByOrderId(any()));
                    softly.check(() -> BDDMockito.then(orderRepository).should(never()).findByOrderNumber(any()));
                    softly.check(() -> BDDMockito.then(productService).should(never()).findProducts());
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should(never()).saveAll(any()));
                });
            }

            @Test
            void notOwner() {
                //given
                OrderProductRequest orderProductRequest1 = new OrderProductRequest(productNumber1, 2);
                OrderProductRequest orderProductRequest2 = new OrderProductRequest(productNumber2, 5);
                OrderChangeRequest request = new OrderChangeRequest(List.of(orderProductRequest1, orderProductRequest2));

                given(orderRepository.findWithAll(anyString())).willReturn(Optional.of(order));

                //when & then
                thenThrownBy(() -> orderService.changeOrder(order.getOrderNumber(), request, "id_D"))
                        .isInstanceOf(NotResourceOwnerException.class)
                        .hasMessage("주문의 주인이 아닙니다");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderRepository).should().findWithAll(anyString()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should(never()).deleteOrderProductsByOrderId(any()));
                    softly.check(() -> BDDMockito.then(orderRepository).should(never()).findByOrderNumber(any()));
                    softly.check(() -> BDDMockito.then(productService).should(never()).findProducts());
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should(never()).saveAll(any()));
                });
            }

            @ParameterizedTest
            @EnumSource(value = OrderStatus.class, names = {"CANCELED", "PAID", "DELIVERED"})
            void orderStatusIsNotCreated(OrderStatus orderStatus) {
                //given
                Order notCreatedOrder = createNotCreatedOrder();

                ReflectionTestUtils.setField(notCreatedOrder, "orderStatus", orderStatus);

                OrderProductRequest orderProductRequest = new OrderProductRequest(productNumber1, 2);
                OrderChangeRequest request = new OrderChangeRequest(List.of(orderProductRequest));

                given(orderRepository.findWithAll(anyString())).willReturn(Optional.of(notCreatedOrder));

                //when & then
                thenThrownBy(() -> orderService.changeOrder(notCreatedOrder.getOrderNumber(), request, "id_A"))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("주문을 수정할 수 없습니다. OrderStatus: " + orderStatus);

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderRepository).should().findWithAll(anyString()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should(never()).deleteOrderProductsByOrderId(any()));
                    softly.check(() -> BDDMockito.then(orderRepository).should(never()).findByOrderNumber(any()));
                    softly.check(() -> BDDMockito.then(productService).should(never()).findProducts());
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should(never()).saveAll(any()));
                });
            }

            @Test
            void orderIdIsNull() {
                //given
                Order order = createNotSavedOrder();
                OrderProductRequest orderProductRequest1 = new OrderProductRequest(productNumber1, 2);
                OrderProductRequest orderProductRequest2 = new OrderProductRequest(productNumber2, 5);
                OrderChangeRequest request = new OrderChangeRequest(List.of(orderProductRequest1, orderProductRequest2));

                given(orderRepository.findWithAll(anyString())).willReturn(Optional.of(order));

                //when & then
                thenThrownBy(() -> orderService.changeOrder(order.getOrderNumber(), request, "id_A"))
                        .isInstanceOf(InvalidDataException.class)
                        .hasMessage("식별자가 없는 잘못된 주문입니다");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderRepository).should().findWithAll(anyString()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should(never()).deleteOrderProductsByOrderId(any()));
                    softly.check(() -> BDDMockito.then(orderRepository).should(never()).findByOrderNumber(any()));
                    softly.check(() -> BDDMockito.then(productService).should(never()).findProducts());
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should(never()).saveAll(any()));
                });
            }

            @Test
            void failedDeleteOrderProducts() {
                //given
                OrderProductRequest orderProductRequest1 = new OrderProductRequest(productNumber1, 2);
                OrderProductRequest orderProductRequest2 = new OrderProductRequest(productNumber2, 5);
                OrderChangeRequest request = new OrderChangeRequest(List.of(orderProductRequest1, orderProductRequest2));

                given(orderRepository.findWithAll(anyString())).willReturn(Optional.of(order));
                willThrow(new RuntimeException("JDBC DELETE Failed")).given(orderProductJdbcRepository).deleteOrderProductsByOrderId(anyLong());

                //when & then
                thenThrownBy(() -> orderService.changeOrder(order.getOrderNumber(), request, "id_A"))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessage("JDBC DELETE Failed");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderRepository).should().findWithAll(anyString()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should().deleteOrderProductsByOrderId(anyLong()));
                    softly.check(() -> BDDMockito.then(orderRepository).should(never()).findByOrderNumber(any()));
                    softly.check(() -> BDDMockito.then(productService).should(never()).findProducts());
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should(never()).saveAll(any()));
                });
            }

            @Test
            void productNumberIsNull() {
                //given
                OrderProductRequest nullProductNumberOrderProductRequest = new OrderProductRequest(null, 3);
                OrderChangeRequest request = new OrderChangeRequest(List.of(nullProductNumberOrderProductRequest));

                given(orderRepository.findWithAll(anyString())).willReturn(Optional.of(order));
                given(orderRepository.findByOrderNumber(anyString())).willReturn(Optional.of(order));
                given(productService.findProducts()).willReturn(List.of(album, book, movie));

                //when & then
                thenThrownBy(() -> orderService.changeOrder(order.getOrderNumber(), request, "id_A"))
                        .isInstanceOf(DataNotFoundException.class)
                        .hasMessage("존재하지 않는 상품입니다. productNumber: " + "null");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderRepository).should().findWithAll(anyString()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should().deleteOrderProductsByOrderId(anyLong()));
                    softly.check(() -> BDDMockito.then(orderRepository).should().findByOrderNumber(anyString()));
                    softly.check(() -> BDDMockito.then(productService).should().findProducts());
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should(never()).saveAll(any()));
                });
            }

            @Test
            void quantityIsNull() {
                //given
                OrderProductRequest nullQuantityOrderProductRequest = new OrderProductRequest(productNumber1, null);
                OrderChangeRequest request = new OrderChangeRequest(List.of(nullQuantityOrderProductRequest));

                given(orderRepository.findWithAll(anyString())).willReturn(Optional.of(order));
                given(orderRepository.findByOrderNumber(anyString())).willReturn(Optional.of(order));
                given(productService.findProducts()).willReturn(List.of(album, book, movie));

                //when & then
                thenThrownBy(() -> orderService.changeOrder(order.getOrderNumber(), request, "id_A"))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("수량이 없습니다");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderRepository).should().findWithAll(anyString()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should().deleteOrderProductsByOrderId(anyLong()));
                    softly.check(() -> BDDMockito.then(orderRepository).should().findByOrderNumber(anyString()));
                    softly.check(() -> BDDMockito.then(productService).should().findProducts());
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should(never()).saveAll(any()));
                });
            }

            @Test
            void productNotFound() {
                //given
                OrderProductRequest notExistsProductNumberOrderProductRequest = new OrderProductRequest("lllIIIll00OO", 3);
                OrderChangeRequest request = new OrderChangeRequest(List.of(notExistsProductNumberOrderProductRequest));

                given(orderRepository.findWithAll(anyString())).willReturn(Optional.of(order));
                given(orderRepository.findByOrderNumber(anyString())).willReturn(Optional.of(order));
                given(productService.findProducts()).willReturn(List.of(album, book, movie));

                //when & then
                thenThrownBy(() -> orderService.changeOrder(order.getOrderNumber(), request, "id_A"))
                        .isInstanceOf(DataNotFoundException.class)
                        .hasMessage("존재하지 않는 상품입니다. productNumber: " + "lllIIIll00OO");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderRepository).should().findWithAll(anyString()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should().deleteOrderProductsByOrderId(anyLong()));
                    softly.check(() -> BDDMockito.then(orderRepository).should().findByOrderNumber(anyString()));
                    softly.check(() -> BDDMockito.then(productService).should().findProducts());
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should(never()).saveAll(any()));
                });
            }

            @Test
            void failedSaveAll() {
                //given
                OrderProductRequest orderProductRequest1 = new OrderProductRequest(productNumber1, 2);
                OrderProductRequest orderProductRequest2 = new OrderProductRequest(productNumber2, 5);
                OrderChangeRequest request = new OrderChangeRequest(List.of(orderProductRequest1, orderProductRequest2));

                given(orderRepository.findWithAll(anyString())).willReturn(Optional.of(order));
                given(orderRepository.findByOrderNumber(anyString())).willReturn(Optional.of(order));
                given(productService.findProducts()).willReturn(List.of(album, book, movie));
                willThrow(new RuntimeException("JDBC Batch INSERT Failed")).given(orderProductJdbcRepository).saveAll(anyList());

                //when & then
                thenThrownBy(() -> orderService.changeOrder(order.getOrderNumber(), request, "id_A"))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessage("JDBC Batch INSERT Failed");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderRepository).should().findWithAll(anyString()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should().deleteOrderProductsByOrderId(anyLong()));
                    softly.check(() -> BDDMockito.then(orderRepository).should().findByOrderNumber(anyString()));
                    softly.check(() -> BDDMockito.then(productService).should().findProducts());
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should().saveAll(anyList()));
                });
            }

            private Order createNotCreatedOrder() {
                Delivery delivery = new Delivery(member);

                OrderProduct orderProduct = OrderProduct.createOrderProduct(album, 1);

                Order canceledOrder = Order.createOrder(member, delivery, List.of(orderProduct));

                ReflectionTestUtils.setField(canceledOrder, "id", 2L);

                return canceledOrder;
            }

            private Order createNotSavedOrder() {
                Delivery delivery = new Delivery(member);

                OrderProduct orderProduct1 = OrderProduct.createOrderProduct(album, 3);
                OrderProduct orderProduct2 = OrderProduct.createOrderProduct(book, 2);
                OrderProduct orderProduct3 = OrderProduct.createOrderProduct(movie, 1);

                return Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2, orderProduct3));
            }
        }
    }

    @Nested
    class Cancel extends Setup {

        @Nested
        class SuccessCase {

            @Test
            void withoutPayment() {
                //given
                given(orderRepository.findWithAll(anyString())).willReturn(Optional.of(order));

                //when
                orderService.cancelOrder(order.getOrderNumber(), "id_A");

                //then
                BDDMockito.then(orderRepository).should().findWithAll(anyString());
                thenSoftly(softly -> {
                    softly.then(order.getOrderProducts())
                            .extracting("product.name", "quantity", "orderPrice")
                            .containsExactlyInAnyOrder(
                                    tuple("BANG BANG", 3, 45000),
                                    tuple("자바 ORM 표준 JPA 프로그래밍", 2, 30000),
                                    tuple("범죄도시", 4, 60000));
                    softly.then(order.getTotalAmount()).isEqualTo(135000);
                });
                thenSoftly(softly -> {
                    softly.then(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELED);
                    softly.then(order.getDelivery().getDeliveryStatus()).isEqualTo(DeliveryStatus.CANCELED);
                    softly.then(order.getPayment()).isNull();
                });
                thenSoftly(softly -> {
                    softly.then(album.getStockQuantity()).isEqualTo(10);
                    softly.then(book.getStockQuantity()).isEqualTo(7);
                    softly.then(movie.getStockQuantity()).isEqualTo(5);
                });
            }

            @Test
            void withPayment() {
                //given
                Payment.requestPayment(order);

                given(orderRepository.findWithAll(anyString())).willReturn(Optional.of(order));

                //when
                orderService.cancelOrder(order.getOrderNumber(), "id_A");

                //then
                BDDMockito.then(orderRepository).should().findWithAll(anyString());
                thenSoftly(softly -> {
                    softly.then(order.getOrderProducts())
                            .extracting("product.name", "quantity", "orderPrice")
                            .containsExactlyInAnyOrder(
                                    tuple("BANG BANG", 3, 45000),
                                    tuple("자바 ORM 표준 JPA 프로그래밍", 2, 30000),
                                    tuple("범죄도시", 4, 60000));
                    softly.then(order.getTotalAmount()).isEqualTo(135000);
                });
                thenSoftly(softly -> {
                    softly.then(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELED);
                    softly.then(order.getDelivery().getDeliveryStatus()).isEqualTo(DeliveryStatus.CANCELED);
                    softly.then(order.getPayment().getPaymentStatus()).isEqualTo(PaymentStatus.CANCELED);
                });
                thenSoftly(softly -> {
                    softly.then(album.getStockQuantity()).isEqualTo(10);
                    softly.then(book.getStockQuantity()).isEqualTo(7);
                    softly.then(movie.getStockQuantity()).isEqualTo(5);
                });
            }

            @Test
            void idempotency() {
                //given
                Payment.requestPayment(order);

                given(orderRepository.findWithAll(anyString())).willReturn(Optional.of(order));

                //when 첫 번째 호출
                orderService.cancelOrder(order.getOrderNumber(), "id_A");

                //then
                BDDMockito.then(orderRepository).should().findWithAll(anyString());
                thenSoftly(softly -> {
                    softly.then(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELED);
                    softly.then(order.getDelivery().getDeliveryStatus()).isEqualTo(DeliveryStatus.CANCELED);
                    softly.then(order.getPayment().getPaymentStatus()).isEqualTo(PaymentStatus.CANCELED);
                });
                thenSoftly(softly -> {
                    softly.then(album.getStockQuantity()).isEqualTo(10);
                    softly.then(book.getStockQuantity()).isEqualTo(7);
                    softly.then(movie.getStockQuantity()).isEqualTo(5);
                });

                //when & then 두 번째 호출
                thenNoException().isThrownBy(() -> orderService.cancelOrder(order.getOrderNumber(), "id_A"));

                //then
                BDDMockito.then(orderRepository).should(times(2)).findWithAll(anyString());
                thenSoftly(softly -> {
                    softly.then(order.getOrderProducts())
                            .extracting("product.name", "quantity", "orderPrice")
                            .containsExactlyInAnyOrder(
                                    tuple("BANG BANG", 3, 45000),
                                    tuple("자바 ORM 표준 JPA 프로그래밍", 2, 30000),
                                    tuple("범죄도시", 4, 60000));
                    softly.then(order.getTotalAmount()).isEqualTo(135000);
                });
                thenSoftly(softly -> {
                    softly.then(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELED);
                    softly.then(order.getDelivery().getDeliveryStatus()).isEqualTo(DeliveryStatus.CANCELED);
                    softly.then(order.getPayment().getPaymentStatus()).isEqualTo(PaymentStatus.CANCELED);
                });
                thenSoftly(softly -> {
                    softly.then(album.getStockQuantity()).isEqualTo(10);
                    softly.then(book.getStockQuantity()).isEqualTo(7);
                    softly.then(movie.getStockQuantity()).isEqualTo(5);
                });
            }
        }

        @Nested
        class FailureCase {

            @Test
            void orderNotFound() {
                //given
                given(orderRepository.findWithAll(anyString())).willReturn(Optional.empty());

                //when & then
                thenThrownBy(() -> orderService.cancelOrder(wrongOrderNumber, "id_A"))
                        .isInstanceOf(DataNotFoundException.class)
                        .hasMessage("존재하지 않는 주문입니다. orderNumber: " + wrongOrderNumber);

                //then
                BDDMockito.then(orderRepository).should().findWithAll(anyString());
            }

            @Test
            void notOwner() {
                //given
                given(orderRepository.findWithAll(anyString())).willReturn(Optional.of(order));

                //when & then
                thenThrownBy(() -> orderService.cancelOrder(wrongOrderNumber, "id_D"))
                        .isInstanceOf(NotResourceOwnerException.class)
                        .hasMessage("주문의 주인이 아닙니다");

                //then
                BDDMockito.then(orderRepository).should().findWithAll(anyString());
            }
        }
    }

    @Nested
    class Delete extends Setup {

        @Nested
        class SuccessCase {

            @Test
            void withoutPayment() {
                //given
                order.cancel();

                given(orderRepository.findWithDeliveryPaymentMember(anyString())).willReturn(Optional.of(order));
                given(orderRepository.findWithDeliveryPayment(anyString())).willReturn(Optional.of(order));

                //when
                orderService.deleteOrder(order.getOrderNumber(), "id_A");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderRepository).should().findWithDeliveryPaymentMember(anyString()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should().softDeleteOrderProductsByOrderId(anyLong()));
                    softly.check(() -> BDDMockito.then(orderRepository).should().findWithDeliveryPayment(anyString()));
                    softly.check(() -> BDDMockito.then(orderRepository).should().delete(order));
                });
            }

            @Test
            void withPayment() {
                //given
                Payment.requestPayment(order);
                order.cancel();

                given(orderRepository.findWithDeliveryPaymentMember(anyString())).willReturn(Optional.of(order));
                given(orderRepository.findWithDeliveryPayment(anyString())).willReturn(Optional.of(order));

                //when
                orderService.deleteOrder(order.getOrderNumber(), "id_A");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderRepository).should().findWithDeliveryPaymentMember(anyString()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should().softDeleteOrderProductsByOrderId(anyLong()));
                    softly.check(() -> BDDMockito.then(orderRepository).should().findWithDeliveryPayment(anyString()));
                    softly.check(() -> BDDMockito.then(orderRepository).should().delete(order));
                });
            }

            @Test
            void shouldIgnoreDelete_WhenOrderNotFound() {
                //given
                given(orderRepository.findWithDeliveryPaymentMember(anyString())).willReturn(Optional.empty());

                //when
                orderService.deleteOrder(wrongOrderNumber, "id_A");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderRepository).should().findWithDeliveryPaymentMember(anyString()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should(never()).softDeleteOrderProductsByOrderId(any()));
                    softly.check(() -> BDDMockito.then(orderRepository).should(never()).findWithDeliveryPayment(any()));
                    softly.check(() -> BDDMockito.then(orderRepository).should(never()).delete(any()));
                });
            }

            @Test
            void idempotency() {
                //given
                Payment.requestPayment(order);
                order.cancel();

                given(orderRepository.findWithDeliveryPaymentMember(anyString()))
                        .willReturn(Optional.of(order))
                        .willReturn(Optional.empty());
                given(orderRepository.findWithDeliveryPayment(anyString())).willReturn(Optional.of(order));

                //when 첫 번째 호출
                orderService.deleteOrder(order.getOrderNumber(), "id_A");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderRepository).should().findWithDeliveryPaymentMember(anyString()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should().softDeleteOrderProductsByOrderId(anyLong()));
                    softly.check(() -> BDDMockito.then(orderRepository).should().findWithDeliveryPayment(anyString()));
                    softly.check(() -> BDDMockito.then(orderRepository).should().delete(order));
                });

                //when & then 두 번째 호출
                orderService.deleteOrder(order.getOrderNumber(), "id_A");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderRepository).should(times(2)).findWithDeliveryPaymentMember(anyString()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should().softDeleteOrderProductsByOrderId(any()));
                    softly.check(() -> BDDMockito.then(orderRepository).should().findWithDeliveryPayment(anyString()));
                    softly.check(() -> BDDMockito.then(orderRepository).should().delete(any()));
                });
            }
        }

        @Nested
        class FailureCase {

            @Test
            void notOwner() {
                //given
                order.cancel();

                given(orderRepository.findWithDeliveryPaymentMember(anyString())).willReturn(Optional.of(order));

                //when & then
                thenThrownBy(() -> orderService.deleteOrder(order.getOrderNumber(), "id_D"))
                        .isInstanceOf(NotResourceOwnerException.class)
                        .hasMessage("주문의 주인이 아닙니다");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderRepository).should().findWithDeliveryPaymentMember(anyString()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should(never()).softDeleteOrderProductsByOrderId(any()));
                    softly.check(() -> BDDMockito.then(orderRepository).should(never()).findWithDeliveryPayment(any()));
                    softly.check(() -> BDDMockito.then(orderRepository).should(never()).delete(any()));
                });
            }

            @Test
            void failedSoftDeleteOrderProducts() {
                //given
                order.cancel();

                given(orderRepository.findWithDeliveryPaymentMember(anyString())).willReturn(Optional.of(order));
                willThrow(new RuntimeException("JDBC Soft DELETE Failed")).given(orderProductJdbcRepository).softDeleteOrderProductsByOrderId(anyLong());

                //when & then
                thenThrownBy(() -> orderService.deleteOrder(order.getOrderNumber(), "id_A"))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessage("JDBC Soft DELETE Failed");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderRepository).should().findWithDeliveryPaymentMember(anyString()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should().softDeleteOrderProductsByOrderId(anyLong()));
                    softly.check(() -> BDDMockito.then(orderRepository).should(never()).findWithDeliveryPayment(any()));
                    softly.check(() -> BDDMockito.then(orderRepository).should(never()).delete(any()));
                });
            }
        }
    }

    @Nested
    class ChangeDto extends Setup {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //when
                OrderChangeResponse orderChangeResponse = orderService.getOrderChangeResponse(order);

                //then
                then(orderChangeResponse.orderProductDtoList())
                        .extracting("name", "quantity")
                        .containsExactlyInAnyOrder(tuple("BANG BANG", 3), tuple("자바 ORM 표준 JPA 프로그래밍", 2), tuple("범죄도시", 4));
            }
        }
    }
}