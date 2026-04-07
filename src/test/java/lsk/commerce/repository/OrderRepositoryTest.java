package lsk.commerce.repository;

import lsk.commerce.domain.Category;
import lsk.commerce.domain.Delivery;
import lsk.commerce.domain.DeliveryStatus;
import lsk.commerce.domain.Member;
import lsk.commerce.domain.Order;
import lsk.commerce.domain.OrderProduct;
import lsk.commerce.domain.OrderStatus;
import lsk.commerce.domain.Payment;
import lsk.commerce.domain.product.Album;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(OrderRepository.class)
class OrderRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    OrderRepository orderRepository;

    Member member;
    Delivery delivery;
    Category category1;
    Category category2;
    Category category3;
    OrderProduct orderProduct1;
    OrderProduct orderProduct2;
    List<OrderProduct> orderProducts;

    @BeforeEach
    void beforeEach() {
        member = Member.builder()
                .name("유저A")
                .loginId("id_A")
                .password("abAB12!@")
                .zipcode("01234")
                .baseAddress("서울시 강남구")
                .detailAddress("101동 101호")
                .build();
        delivery = new Delivery(member);
        em.persist(member);

        category1 = Category.createCategory(null, "가요");
        category2 = Category.createCategory(category1, "댄스");
        category3 = Category.createCategory(category1, "발라드");
        em.persist(category1);
        em.persist(category2);
        em.persist(category3);

        Album album1 = Album.builder().name("BANG BANG").price(15000).stockQuantity(10).artist("IVE").studio("STARSHIP").build();
        Album album2 = Album.builder().name("타임 캡슐").price(15000).stockQuantity(10).artist("다비치").studio("씨에이엠위더스").build();
        em.persist(album1);
        em.persist(album2);
        album1.connectCategory(category2);
        album2.connectCategory(category3);

        orderProduct1 = OrderProduct.createOrderProduct(album1, 5);
        orderProduct2 = OrderProduct.createOrderProduct(album2, 4);
        orderProducts = List.of(orderProduct1, orderProduct2);

        em.flush();
        em.clear();
    }

    @Nested
    class Save {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                Order order = Order.createOrder(member, delivery, orderProducts);

                System.out.println("================= WHEN START =================");

                //when
                orderRepository.save(order);
                em.flush();
                Long orderId = order.getId();

                System.out.println("================= WHEN END ===================");

                em.persist(orderProduct1);
                em.persist(orderProduct2);
                em.flush();
                em.clear();

                //then
                Order findOrder = em.find(Order.class, orderId);
                thenSoftly(softly -> {
                    softly.then(findOrder)
                            .extracting("member.id", "delivery.deliveryStatus", "payment", "totalAmount", "orderStatus")
                            .containsExactly(member.getId(), DeliveryStatus.WAITING, null, 135000, OrderStatus.CREATED);
                    softly.then(findOrder.getOrderProducts())
                            .extracting("product.name")
                            .containsExactlyInAnyOrder("BANG BANG", "타임 캡슐");
                });
            }
        }
    }

    private abstract class Setup {

        Long orderId;
        String orderNumber;

        @BeforeEach
        void beforeEach() {
            Order order = Order.createOrder(member, delivery, orderProducts);
            em.persist(order);
            em.persist(orderProduct1);
            em.persist(orderProduct2);
            Payment.requestPayment(order);

            orderId = order.getId();
            orderNumber = order.getOrderNumber();

            em.flush();
            em.clear();
        }
    }

    @Nested
    class Find extends Setup {

        @Nested
        class SuccessCase {

            @Test
            void byOrderNumber() {
                System.out.println("================= WHEN START =================");

                //when
                Optional<Order> findOrder = orderRepository.findByOrderNumber(orderNumber);

                System.out.println("================= WHEN END ===================");

                //then
                thenSoftly(softly -> {
                    softly.then(findOrder).isPresent();
                    softly.then(Hibernate.isInitialized(findOrder.get().getMember())).isFalse();
                    softly.then(Hibernate.isInitialized(findOrder.get().getDelivery())).isFalse();
                    softly.then(Hibernate.isInitialized(findOrder.get().getPayment())).isFalse();
                    softly.then(Hibernate.isInitialized(findOrder.get().getOrderProducts())).isFalse();
                });
            }

            @Test
            void withDelivery() {
                System.out.println("================= WHEN START =================");

                //when
                Optional<Order> findOrder = orderRepository.findWithDelivery(orderNumber);

                System.out.println("================= WHEN END ===================");

                //then
                thenSoftly(softly -> {
                    softly.then(findOrder).isPresent();
                    softly.then(Hibernate.isInitialized(findOrder.get().getMember())).isFalse();
                    softly.then(Hibernate.isInitialized(findOrder.get().getDelivery())).isTrue();
                    softly.then(Hibernate.isInitialized(findOrder.get().getPayment())).isFalse();
                    softly.then(Hibernate.isInitialized(findOrder.get().getOrderProducts())).isFalse();
                });
            }

            @Test
            void withDeliveryPayment() {
                System.out.println("================= WHEN START =================");

                //when
                Optional<Order> findOrder = orderRepository.findWithDeliveryPayment(orderNumber);

                System.out.println("================= WHEN END ===================");

                //then
                thenSoftly(softly -> {
                    softly.then(findOrder).isPresent();
                    softly.then(Hibernate.isInitialized(findOrder.get().getMember())).isFalse();
                    softly.then(Hibernate.isInitialized(findOrder.get().getDelivery())).isTrue();
                    softly.then(Hibernate.isInitialized(findOrder.get().getPayment())).isTrue();
                    softly.then(Hibernate.isInitialized(findOrder.get().getOrderProducts())).isFalse();
                });
            }

            @Test
            void withAllExceptMember() {
                System.out.println("================= WHEN START =================");

                //when
                Optional<Order> findOrder = orderRepository.findWithAllExceptMember(orderNumber);

                System.out.println("================= WHEN END ===================");

                //then
                thenSoftly(softly -> {
                    softly.then(findOrder).isPresent();
                    softly.then(Hibernate.isInitialized(findOrder.get().getMember())).isFalse();
                    softly.then(Hibernate.isInitialized(findOrder.get().getDelivery())).isTrue();
                    softly.then(Hibernate.isInitialized(findOrder.get().getPayment())).isTrue();
                    softly.then(Hibernate.isInitialized(findOrder.get().getOrderProducts())).isTrue();
                });
            }

            @Test
            void withAll() {
                System.out.println("================= WHEN START =================");

                //when
                Optional<Order> findOrder = orderRepository.findWithAll(orderNumber);

                System.out.println("================= WHEN END ===================");

                //then
                thenSoftly(softly -> {
                    softly.then(findOrder).isPresent();
                    softly.then(Hibernate.isInitialized(findOrder.get().getMember())).isTrue();
                    softly.then(Hibernate.isInitialized(findOrder.get().getDelivery())).isTrue();
                    softly.then(Hibernate.isInitialized(findOrder.get().getPayment())).isTrue();
                    softly.then(Hibernate.isInitialized(findOrder.get().getOrderProducts())).isTrue();
                });
            }
        }
    }

    @Nested
    class Delete extends Setup {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                Order findOrder = em.find(Order.class, orderId);

                System.out.println("================= WHEN START =================");

                //when
                orderRepository.delete(findOrder);
                em.flush();
                em.clear();

                System.out.println("================= WHEN END ===================");

                //then
                Order deletedOrder = em.find(Order.class, orderId);
                then(deletedOrder).isNull();
            }
        }
    }
}