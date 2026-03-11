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
import lsk.commerce.dto.request.OrderRequest;
import lsk.commerce.dto.response.OrderResponse;
import lsk.commerce.repository.OrderProductJdbcRepository;
import lsk.commerce.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.BDDAssertions.thenNoException;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.assertj.core.api.BDDAssertions.tuple;
import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
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
    String wrongOrderNumber = "lllIIllIO00O";

    @BeforeEach
    void beforeEach() {
        member = Member.builder()
                .loginId("id_A")
                .city("Seoul")
                .street("Gangnam")
                .zipcode("01234")
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

    }

    @Nested
    class create {

        Map<String, Integer> productMap = Map.of("BANG BANG", 3, "자바 ORM 표준 JPA 프로그래밍", 2, "범죄도시", 4);

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                given(memberService.findMemberByLoginId(anyString())).willReturn(member);
                given(productService.findProducts()).willReturn(List.of(album, book, movie));

                InOrder inOrder = inOrder(em, orderRepository, orderProductJdbcRepository);

                //when
                orderService.order("id_A", productMap);

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
                            .extracting("product.name", "product.price", "count", "orderPrice")
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
            void productMapIsEmpty() {
                //given
                Map<String, Integer> emptyProductMap = new HashMap<>();

                //when & then
                thenThrownBy(() -> orderService.order("id_A", emptyProductMap))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("주문할 상품이 없습니다");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(memberService).should(never()).findMemberByLoginId(any()));
                    softly.check(() -> BDDMockito.then(productService).should(never()).findProducts());
                    softly.check(() -> BDDMockito.then(orderRepository).should(never()).save(any()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should(never()).saveAll(any()));
                });
            }

            @Test
            void memberNotFound() {
                //given
                given(memberService.findMemberByLoginId(anyString())).willThrow(new IllegalArgumentException("존재하지 않는 아이디입니다"));

                //when & then
                thenThrownBy(() -> orderService.order("id_B", productMap))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("존재하지 않는 아이디입니다");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(memberService).should().findMemberByLoginId(anyString()));
                    softly.check(() -> BDDMockito.then(productService).should(never()).findProducts());
                    softly.check(() -> BDDMockito.then(orderRepository).should(never()).save(any()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should(never()).saveAll(any()));
                });
            }

            @ParameterizedTest
            @MethodSource("lsk.commerce.service.OrderServiceTest#keyValueProvider")
            void productMapContainsNullEntry(String key, Integer value, String message) {
                //given
                given(memberService.findMemberByLoginId(anyString())).willReturn(member);
                given(productService.findProducts()).willReturn(List.of(album, book, movie));

                Map<String, Integer> hasNullProductMap = new HashMap<>();
                hasNullProductMap.put(key, value);

                //when & then
                thenThrownBy(() -> orderService.order("id_A", hasNullProductMap))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage(message);

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
                given(memberService.findMemberByLoginId(anyString())).willReturn(member);
                given(productService.findProducts()).willReturn(List.of(album, book, movie));

                Map<String, Integer> notExistsNameProductMap = Map.of("타임 캡슐", 3);

                //when & then
                thenThrownBy(() -> orderService.order("id_A", notExistsNameProductMap))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("존재하지 않는 상품입니다. name: " + "타임 캡슐");

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
                given(memberService.findMemberByLoginId(anyString())).willReturn(member);
                given(productService.findProducts()).willReturn(List.of(album, book, movie));
                willThrow(new RuntimeException("JDBC Batch INSERT Failed")).given(orderProductJdbcRepository).saveAll(anyList());

                //when & then
                thenThrownBy(() -> orderService.order("id_A", productMap))
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
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("존재하지 않는 주문입니다");

                //then
                BDDMockito.then(orderRepository).should().findByOrderNumber(anyString());
            }
        }
    }

    @Nested
    class Update extends Setup {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                given(orderRepository.findWithAllExceptMember(anyString())).willReturn(Optional.of(order));
                given(orderRepository.findByOrderNumber(anyString())).willReturn(Optional.of(order));
                given(productService.findProducts()).willReturn(List.of(album, book, movie));

                InOrder inOrder = inOrder(em, orderRepository, orderProductJdbcRepository);

                //when
                orderService.updateOrder(order.getOrderNumber(), Map.of("BANG BANG", 2, "자바 ORM 표준 JPA 프로그래밍", 5));

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderRepository).should(inOrder).findWithAllExceptMember(anyString()));
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
                            .extracting("product.name", "count", "orderPrice")
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
                given(orderRepository.findWithAllExceptMember(anyString())).willReturn(Optional.of(order));
                given(orderRepository.findByOrderNumber(anyString())).willReturn(Optional.of(order));
                given(productService.findProducts()).willReturn(List.of(album, book, movie));

                //when 첫 번째 호출
                orderService.updateOrder(order.getOrderNumber(), Map.of("BANG BANG", 2, "자바 ORM 표준 JPA 프로그래밍", 5));

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderRepository).should().findWithAllExceptMember(anyString()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should().deleteOrderProductsByOrderId(order.getId()));
                    softly.check(() -> BDDMockito.then(orderRepository).should().findByOrderNumber(anyString()));
                    softly.check(() -> BDDMockito.then(productService).should().findProducts());
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should().saveAll(orderProductCaptor.capture()));
                });

                List<OrderProduct> orderProducts = orderProductCaptor.getValue();
                thenSoftly(softly -> {
                    softly.then(orderProducts)
                            .extracting("product.name", "count", "orderPrice")
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
                thenNoException().isThrownBy(() -> orderService.updateOrder(order.getOrderNumber(), Map.of("BANG BANG", 2, "자바 ORM 표준 JPA 프로그래밍", 5)));

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderRepository).should(times(2)).findWithAllExceptMember(anyString()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should().deleteOrderProductsByOrderId(any()));
                    softly.check(() -> BDDMockito.then(orderRepository).should().findByOrderNumber(any()));
                    softly.check(() -> BDDMockito.then(productService).should().findProducts());
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should().saveAll(any()));
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
            void productMapIsEmpty() {
                //given
                Map<String, Integer> emptyProductMap = new HashMap<>();

                //when & then
                thenThrownBy(() -> orderService.updateOrder(order.getOrderNumber(), emptyProductMap))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("주문을 수정할 상품이 없습니다");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderRepository).should(never()).findWithAllExceptMember(any()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should(never()).deleteOrderProductsByOrderId(any()));
                    softly.check(() -> BDDMockito.then(orderRepository).should(never()).findByOrderNumber(any()));
                    softly.check(() -> BDDMockito.then(productService).should(never()).findProducts());
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should(never()).saveAll(any()));
                });
            }

            @Test
            void orderNotFound() {
                //given
                Map<String, Integer> newProductMap = Map.of("BANG BANG", 2, "자바 ORM 표준 JPA 프로그래밍", 5);

                given(orderRepository.findWithAllExceptMember(anyString())).willReturn(Optional.empty());

                //when & then
                thenThrownBy(() -> orderService.updateOrder(wrongOrderNumber, newProductMap))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("존재하지 않는 주문입니다");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderRepository).should().findWithAllExceptMember(anyString()));
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

                Map<String, Integer> newProductMap = Map.of("BANG BANG", 2, "자바 ORM 표준 JPA 프로그래밍", 5);

                given(orderRepository.findWithAllExceptMember(anyString())).willReturn(Optional.of(order));

                //when & then
                thenThrownBy(() -> orderService.updateOrder(order.getOrderNumber(), newProductMap))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("식별자가 없는 잘못된 주문입니다");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderRepository).should().findWithAllExceptMember(anyString()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should(never()).deleteOrderProductsByOrderId(any()));
                    softly.check(() -> BDDMockito.then(orderRepository).should(never()).findByOrderNumber(any()));
                    softly.check(() -> BDDMockito.then(productService).should(never()).findProducts());
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should(never()).saveAll(any()));
                });
            }

            @Test
            void failedDeleteOrderProducts() {
                //given
                Map<String, Integer> newProductMap = Map.of("BANG BANG", 2, "자바 ORM 표준 JPA 프로그래밍", 5);

                given(orderRepository.findWithAllExceptMember(anyString())).willReturn(Optional.of(order));
                willThrow(new RuntimeException("JDBC DELETE Failed")).given(orderProductJdbcRepository).deleteOrderProductsByOrderId(anyLong());

                //when & then
                thenThrownBy(() -> orderService.updateOrder(order.getOrderNumber(), newProductMap))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessage("JDBC DELETE Failed");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderRepository).should().findWithAllExceptMember(anyString()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should().deleteOrderProductsByOrderId(anyLong()));
                    softly.check(() -> BDDMockito.then(orderRepository).should(never()).findByOrderNumber(any()));
                    softly.check(() -> BDDMockito.then(productService).should(never()).findProducts());
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should(never()).saveAll(any()));
                });
            }

            @ParameterizedTest
            @MethodSource("lsk.commerce.service.OrderServiceTest#keyValueProvider")
            void productMapContainsNullEntry(String key, Integer value, String message) {
                //given
                Map<String, Integer> newProductMap = new HashMap<>();
                newProductMap.put(key, value);

                given(orderRepository.findWithAllExceptMember(anyString())).willReturn(Optional.of(order));
                given(orderRepository.findByOrderNumber(anyString())).willReturn(Optional.of(order));
                given(productService.findProducts()).willReturn(List.of(album, book, movie));

                //when & then
                thenThrownBy(() -> orderService.updateOrder(order.getOrderNumber(), newProductMap))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage(message);

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderRepository).should().findWithAllExceptMember(anyString()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should().deleteOrderProductsByOrderId(anyLong()));
                    softly.check(() -> BDDMockito.then(orderRepository).should().findByOrderNumber(anyString()));
                    softly.check(() -> BDDMockito.then(productService).should().findProducts());
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should(never()).saveAll(any()));
                });
            }

            @Test
            void productNotFound() {
                //given
                Map<String, Integer> newProductMap = Map.of("타임 캡슐", 3);

                given(orderRepository.findWithAllExceptMember(anyString())).willReturn(Optional.of(order));
                given(orderRepository.findByOrderNumber(anyString())).willReturn(Optional.of(order));
                given(productService.findProducts()).willReturn(List.of(album, book, movie));

                //when & then
                thenThrownBy(() -> orderService.updateOrder(order.getOrderNumber(), newProductMap))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("존재하지 않는 상품입니다. name: " + "타임 캡슐");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderRepository).should().findWithAllExceptMember(anyString()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should().deleteOrderProductsByOrderId(anyLong()));
                    softly.check(() -> BDDMockito.then(orderRepository).should().findByOrderNumber(anyString()));
                    softly.check(() -> BDDMockito.then(productService).should().findProducts());
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should(never()).saveAll(any()));
                });
            }

            @Test
            void failedSaveAll() {
                //given
                Map<String, Integer> newProductMap = Map.of("BANG BANG", 2, "자바 ORM 표준 JPA 프로그래밍", 5);

                given(orderRepository.findWithAllExceptMember(anyString())).willReturn(Optional.of(order));
                given(orderRepository.findByOrderNumber(anyString())).willReturn(Optional.of(order));
                given(productService.findProducts()).willReturn(List.of(album, book, movie));
                willThrow(new RuntimeException("JDBC Batch INSERT Failed")).given(orderProductJdbcRepository).saveAll(anyList());

                //when & then
                thenThrownBy(() -> orderService.updateOrder(order.getOrderNumber(), newProductMap))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessage("JDBC Batch INSERT Failed");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderRepository).should().findWithAllExceptMember(anyString()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should().deleteOrderProductsByOrderId(anyLong()));
                    softly.check(() -> BDDMockito.then(orderRepository).should().findByOrderNumber(anyString()));
                    softly.check(() -> BDDMockito.then(productService).should().findProducts());
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should().saveAll(anyList()));
                });
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
                given(orderRepository.findWithAllExceptMember(anyString())).willReturn(Optional.of(order));

                //when
                orderService.cancelOrder(order.getOrderNumber());

                //then
                BDDMockito.then(orderRepository).should().findWithAllExceptMember(anyString());
                thenSoftly(softly -> {
                    softly.then(order.getOrderProducts())
                            .extracting("product.name", "count", "orderPrice")
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

                given(orderRepository.findWithAllExceptMember(anyString())).willReturn(Optional.of(order));

                //when
                orderService.cancelOrder(order.getOrderNumber());

                //then
                BDDMockito.then(orderRepository).should().findWithAllExceptMember(anyString());
                thenSoftly(softly -> {
                    softly.then(order.getOrderProducts())
                            .extracting("product.name", "count", "orderPrice")
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

                given(orderRepository.findWithAllExceptMember(anyString())).willReturn(Optional.of(order));

                //when 첫 번째 호출
                orderService.cancelOrder(order.getOrderNumber());

                //then
                BDDMockito.then(orderRepository).should().findWithAllExceptMember(anyString());
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
                thenNoException().isThrownBy(() -> orderService.cancelOrder(order.getOrderNumber()));

                //then
                BDDMockito.then(orderRepository).should(times(2)).findWithAllExceptMember(anyString());
                thenSoftly(softly -> {
                    softly.then(order.getOrderProducts())
                            .extracting("product.name", "count", "orderPrice")
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
                given(orderRepository.findWithAllExceptMember(anyString())).willReturn(Optional.empty());

                //when & then
                thenThrownBy(() -> orderService.cancelOrder(wrongOrderNumber))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("존재하지 않는 주문입니다");

                //then
                BDDMockito.then(orderRepository).should().findWithAllExceptMember(anyString());
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

                given(orderRepository.findWithDeliveryPayment(anyString())).willReturn(Optional.of(order));
                given(orderRepository.findWithDelivery(anyString())).willReturn(Optional.of(order));

                //when
                orderService.deleteOrder(order.getOrderNumber());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderRepository).should().findWithDeliveryPayment(anyString()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should().softDeleteOrderProductsByOrderId(anyLong()));
                    softly.check(() -> BDDMockito.then(orderRepository).should().findWithDelivery(anyString()));
                    softly.check(() -> BDDMockito.then(orderRepository).should().delete(order));
                });
            }

            @Test
            void withPayment() {
                //given
                Payment.requestPayment(order);
                order.cancel();

                given(orderRepository.findWithDeliveryPayment(anyString())).willReturn(Optional.of(order));
                given(orderRepository.findWithDelivery(anyString())).willReturn(Optional.of(order));

                //when
                orderService.deleteOrder(order.getOrderNumber());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderRepository).should().findWithDeliveryPayment(anyString()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should().softDeleteOrderProductsByOrderId(anyLong()));
                    softly.check(() -> BDDMockito.then(orderRepository).should().findWithDelivery(anyString()));
                    softly.check(() -> BDDMockito.then(orderRepository).should().delete(order));
                });
            }
        }

        @Nested
        class FailureCase {

            @Test
            void orderNotFound() {
                //given
                given(orderRepository.findWithDeliveryPayment(anyString())).willReturn(Optional.empty());

                //when & then
                thenThrownBy(() -> orderService.deleteOrder(wrongOrderNumber))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("존재하지 않는 주문입니다");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderRepository).should().findWithDeliveryPayment(anyString()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should(never()).softDeleteOrderProductsByOrderId(any()));
                    softly.check(() -> BDDMockito.then(orderRepository).should(never()).findWithDelivery(any()));
                    softly.check(() -> BDDMockito.then(orderRepository).should(never()).delete(any()));
                });
            }

            @Test
            void failedSoftDeleteOrderProducts() {
                //given
                order.cancel();

                given(orderRepository.findWithDeliveryPayment(anyString())).willReturn(Optional.of(order));
                willThrow(new RuntimeException("JDBC Soft DELETE Failed")).given(orderProductJdbcRepository).softDeleteOrderProductsByOrderId(anyLong());

                //when & then
                thenThrownBy(() -> orderService.deleteOrder(order.getOrderNumber()))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessage("JDBC Soft DELETE Failed");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderRepository).should().findWithDeliveryPayment(anyString()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should().softDeleteOrderProductsByOrderId(anyLong()));
                    softly.check(() -> BDDMockito.then(orderRepository).should(never()).findWithDelivery(any()));
                    softly.check(() -> BDDMockito.then(orderRepository).should(never()).delete(any()));
                });
            }

            @Test
            void failedDeleteOrder() {
                //given
                order.cancel();

                given(orderRepository.findWithDeliveryPayment(anyString())).willReturn(Optional.of(order));
                given(orderRepository.findWithDelivery(anyString())).willReturn(Optional.of(order));
                willThrow(new RuntimeException("DELETE Failed")).given(orderRepository).delete(any());

                //when & then
                thenThrownBy(() -> orderService.deleteOrder(order.getOrderNumber()))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessage("DELETE Failed");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderRepository).should().findWithDeliveryPayment(anyString()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should().softDeleteOrderProductsByOrderId(anyLong()));
                    softly.check(() -> BDDMockito.then(orderRepository).should().findWithDelivery(any()));
                    softly.check(() -> BDDMockito.then(orderRepository).should().delete(any()));
                });
            }

            @Test
            void alreadyDeleted() {
                //given
                Payment.requestPayment(order);
                order.cancel();

                given(orderRepository.findWithDeliveryPayment(anyString()))
                        .willReturn(Optional.of(order))
                        .willReturn(Optional.empty());
                given(orderRepository.findWithDelivery(anyString())).willReturn(Optional.of(order));

                //when 첫 번째 호출
                orderService.deleteOrder(order.getOrderNumber());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderRepository).should().findWithDeliveryPayment(anyString()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should().softDeleteOrderProductsByOrderId(anyLong()));
                    softly.check(() -> BDDMockito.then(orderRepository).should().findWithDelivery(anyString()));
                    softly.check(() -> BDDMockito.then(orderRepository).should().delete(order));
                });

                //when & then 두 번째 호출
                thenThrownBy(() -> orderService.deleteOrder(order.getOrderNumber()))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("존재하지 않는 주문입니다");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderRepository).should(times(2)).findWithDeliveryPayment(anyString()));
                    softly.check(() -> BDDMockito.then(orderProductJdbcRepository).should().softDeleteOrderProductsByOrderId(any()));
                    softly.check(() -> BDDMockito.then(orderRepository).should().findWithDelivery(any()));
                    softly.check(() -> BDDMockito.then(orderRepository).should().delete(any()));
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
                Payment.requestPayment(order);

                //when
                OrderRequest orderRequest = orderService.getOrderRequest(order);
                OrderResponse orderResponse = orderService.getOrderResponse(order);

                //then
                thenSoftly(softly -> {
                    softly.then(orderRequest)
                            .extracting("orderNumber", "memberLoginId", "paymentId")
                            .containsExactlyInAnyOrder(order.getOrderNumber(), "id_A", order.getPayment().getPaymentId());
                    softly.then(orderResponse)
                            .extracting("paymentDate", "shippedDate", "deliveredDate")
                            .containsOnlyNulls();
                    softly.then(orderRequest.getOrderProducts())
                            .usingRecursiveComparison()
                            .isEqualTo(orderResponse.getOrderProducts());
                });
            }
        }
    }

    static Stream<Arguments> keyValueProvider() {
        return Stream.of(
                argumentSet("키 null", null, 3, "존재하지 않는 상품입니다. name: " + "null"),
                argumentSet("값 null", "BANG BANG", null, "수량이 없습니다"),
                argumentSet("키, 값 모두 null", null, null, "존재하지 않는 상품입니다. name: " + "null")
        );
    }
}