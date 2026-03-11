package lsk.commerce.domain;

import lsk.commerce.domain.product.Album;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

class OrderProductTest {

    @Nested
    class Create {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                Album album = Album.builder()
                        .price(15000)
                        .stockQuantity(10)
                        .build();

                //when
                OrderProduct orderProduct = OrderProduct.createOrderProduct(album, 5);

                //then
                then(orderProduct)
                        .extracting("order", "product", "orderPrice", "count")
                        .containsExactly(null, album, 75000, 5);
            }
        }

        @Nested
        class FailureCase {

            @Test
            void countNull() {
                //given
                Album album = Album.builder()
                        .price(15000)
                        .stockQuantity(10)
                        .build();

                //when & then
                thenThrownBy(() -> OrderProduct.createOrderProduct(album, null))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("수량이 없습니다");
            }

            @Test
            void exceed() {
                //given
                Album album = Album.builder()
                        .price(15000)
                        .stockQuantity(10)
                        .build();

                //when & then
                thenThrownBy(() -> OrderProduct.createOrderProduct(album, 11))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("재고가 부족합니다");

                //then
                then(album.getStockQuantity()).isEqualTo(10);
            }
        }
    }

    @Nested
    class GetProductName {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                Album album = Album.builder()
                        .name("BANG BANG")
                        .price(15000)
                        .stockQuantity(10)
                        .build();
                OrderProduct orderProduct = OrderProduct.createOrderProduct(album, 5);

                //when
                String productName = orderProduct.getProductName();

                //then
                then(productName).isEqualTo("BANG BANG");
            }
        }

        @Nested
        class FailureCase {

            @Test
            void productIsNull() {
                OrderProduct orderProduct = new OrderProduct();

                //when & then
                thenThrownBy(() -> orderProduct.getProductName())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("상품이 없습니다");
            }

            @Test
            void productNameIsNull() {
                Album album = Album.builder()
                        .price(15000)
                        .stockQuantity(10)
                        .build();
                OrderProduct orderProduct = OrderProduct.createOrderProduct(album, 5);

                //when & then
                thenThrownBy(() -> orderProduct.getProductName())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("상품 이름이 없습니다");
            }
        }
    }
}