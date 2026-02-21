package lsk.commerce.domain;

import lsk.commerce.domain.product.Album;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class OrderProductTest {

    @Nested
    class SuccessCase {

        @Test
        void create() {
            //given
            Album album = Album.builder().price(15000).stockQuantity(10).build();

            //when
            OrderProduct orderProduct = OrderProduct.createOrderProduct(album, 5);

            //then
            assertThat(orderProduct)
                    .extracting("order", "product", "orderPrice", "count")
                    .containsExactly(null, album, 75000, 5);
        }
    }

    @Nested
    class FailureCase {

        @Test
        void failed_create_nullParameter() {
            //given
            Album album = Album.builder().price(15000).stockQuantity(10).build();

            //when
            assertAll(
                    () -> assertThatThrownBy(() -> OrderProduct.createOrderProduct(null, 5))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("주문하는 상품 또는 수량이 없습니다."),
                    () -> assertThatThrownBy(() -> OrderProduct.createOrderProduct(album, null))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("주문하는 상품 또는 수량이 없습니다.")
            );
        }

        @Test
        void failed_create_exceed() {
            //given
            Album album = Album.builder().price(15000).stockQuantity(10).build();

            //when
            assertThatThrownBy(() -> OrderProduct.createOrderProduct(album, 11))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("재고가 부족합니다.");

            //then
            assertThat(album.getStockQuantity()).isEqualTo(10);
        }
    }
}