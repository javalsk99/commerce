package lsk.commerce.repository;

import lsk.commerce.domain.Category;
import lsk.commerce.domain.Delivery;
import lsk.commerce.domain.Member;
import lsk.commerce.domain.Order;
import lsk.commerce.domain.OrderProduct;
import lsk.commerce.domain.OrderStatus;
import lsk.commerce.domain.Payment;
import lsk.commerce.domain.PaymentStatus;
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
@Import(PaymentRepository.class)
class PaymentRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    PaymentRepository paymentRepository;

    Member member;
    Delivery delivery;
    Category category1;
    Category category2;
    Category category3;
    OrderProduct orderProduct1;
    OrderProduct orderProduct2;
    List<OrderProduct> orderProducts;
    Long orderId;

    @BeforeEach
    void beforeEach() {
        member = Member.builder()
                .name("유저A")
                .loginId("id_A")
                .password("00000000")
                .city("Seoul")
                .street("Gangnam")
                .zipcode("01234")
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

        Order order = Order.createOrder(member, delivery, orderProducts);
        orderId = em.persistAndGetId(order, Long.class);
        em.persist(orderProduct1);
        em.persist(orderProduct2);

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
                Order findOrder = em.find(Order.class, orderId);

                Payment.requestPayment(findOrder);
                Payment payment = findOrder.getPayment();

                System.out.println("================= WHEN START =================");

                //when
                paymentRepository.save(payment);
                em.flush();
                Long paymentId = payment.getId();
                em.clear();

                System.out.println("================= WHEN END ===================");

                //then
                Payment findPayment = em.find(Payment.class, paymentId);
                then(findPayment)
                        .extracting("order.orderStatus", "paymentAmount", "paymentStatus")
                        .containsExactly(OrderStatus.CREATED, 135000, PaymentStatus.PENDING);
            }
        }
    }

    private abstract class Setup {

        String paymentId;

        @BeforeEach
        void beforeEach() {
            Order order = em.find(Order.class, orderId);
            Payment.requestPayment(order);

            Payment payment = order.getPayment();
            em.persist(payment);
            paymentId = payment.getPaymentId();

            em.flush();
            em.clear();
        }
    }

    @Nested
    class Find extends Setup {

        @Nested
        class SuccessCase {

            @Test
            void byPaymentId() {
                System.out.println("================= WHEN START =================");

                //when
                Optional<Payment> findPayment = paymentRepository.findWithOrder(paymentId);

                System.out.println("================= WHEN END ===================");

                //then
                thenSoftly(softly -> {
                    softly.then(findPayment).isPresent();
                    softly.then(Hibernate.isInitialized(findPayment.get().getOrder())).isTrue();
                });
            }

            @Test
            void withOrderDelivery() {
                System.out.println("================= WHEN START =================");

                //when
                Optional<Payment> findPayment = paymentRepository.findWithOrderDelivery(paymentId);

                System.out.println("================= WHEN END ===================");

                //then
                thenSoftly(softly -> {
                    softly.then(findPayment).isPresent();
                    softly.then(Hibernate.isInitialized(findPayment.get().getOrder())).isTrue();
                    softly.then(Hibernate.isInitialized(findPayment.get().getOrder().getDelivery())).isTrue();
                });
            }
        }
    }
}