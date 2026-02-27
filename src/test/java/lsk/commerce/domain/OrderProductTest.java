package lsk.commerce.domain;

import lsk.commerce.domain.product.Album;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

        @Test
        void getProductName() {
            Album album = Album.builder().name("BANG BANG").price(15000).stockQuantity(10).build();
            OrderProduct orderProduct = OrderProduct.createOrderProduct(album, 5);

            //when
            String productName = orderProduct.getProductName();

            //then
            assertThat(productName).isEqualTo("BANG BANG");
        }
    }

    @Nested
    class FailureCase {

        @Test
        void failed_create_countNull() {
            //given
            Album album = Album.builder().price(15000).stockQuantity(10).build();

            //when
            assertThatThrownBy(() -> OrderProduct.createOrderProduct(album, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("수량이 없습니다.");
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

        @Test
        void getProductName_productIsNull() {
            OrderProduct orderProduct = new OrderProduct();

            //when
            assertThatThrownBy(() -> orderProduct.getProductName())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("상품이 없습니다.");
        }

        @Test
        void getProductName_productNameIsNull() {
            Album album = Album.builder().price(15000).stockQuantity(10).build();
            OrderProduct orderProduct = OrderProduct.createOrderProduct(album, 5);

            //when
            assertThatThrownBy(() -> orderProduct.getProductName())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("상품 이름이 없습니다.");
        }
    }
}