package lsk.commerce.repository;

import lsk.commerce.domain.Category;
import lsk.commerce.domain.Delivery;
import lsk.commerce.domain.Member;
import lsk.commerce.domain.Order;
import lsk.commerce.domain.OrderProduct;
import lsk.commerce.domain.product.Album;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.tuple;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(OrderProductJdbcRepository.class)
class OrderProductJdbcRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    OrderProductJdbcRepository orderProductJdbcRepository;

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

        em.flush();
    }

    @Nested
    class Save {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                System.out.println("================= WHEN START =================");

                //when
                orderProductJdbcRepository.saveAll(orderProducts);
                em.clear();

                System.out.println("================= WHEN END ===================");

                //then
                Order findOrder = em.find(Order.class, orderId);
                then(findOrder.getOrderProducts())
                        .extracting("product.name", "orderPrice", "count")
                        .containsExactlyInAnyOrder(tuple("BANG BANG", 75000, 5), tuple("타임 캡슐", 60000, 4));
            }
        }
    }

    @Nested
    class Delete {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                System.out.println("================= WHEN START =================");

                //when
                orderProductJdbcRepository.deleteOrderProductsByOrderId(orderId);
                em.clear();

                System.out.println("================= WHEN END ===================");

                //then
                Order findOrder = em.find(Order.class, orderId);
                then(findOrder.getOrderProducts()).isEmpty();
            }

            @Test
            void softDelete() {
                System.out.println("================= WHEN START =================");

                //when
                orderProductJdbcRepository.softDeleteOrderProductsByOrderId(orderId);
                em.clear();

                System.out.println("================= WHEN END ===================");

                //then
                Order findOrder = em.find(Order.class, orderId);
                then(findOrder.getOrderProducts()).isEmpty();
            }
        }
    }
}