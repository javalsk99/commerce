package lsk.commerce.domain;

import lsk.commerce.domain.product.Album;
import lsk.commerce.exception.DataNotFoundException;
import lsk.commerce.exception.InvalidDataException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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
                        .extracting("order", "product", "orderPrice", "quantity")
                        .containsExactly(null, album, 75000, 5);
            }
        }

        @Nested
        class FailureCase {

            @Test
            void quantityNull() {
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
                        .hasMessage("재고가 부족합니다. productNumber: " + album.getProductNumber());

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
                String albumNumber = album.getProductNumber();
                OrderProduct orderProduct = OrderProduct.createOrderProduct(album, 5);

                //when
                String productNumber = orderProduct.getProductNumber();

                //then
                then(productNumber).isEqualTo(albumNumber);
            }
        }

        @Nested
        class FailureCase {

            @Test
            void productIsNull() {
                OrderProduct orderProduct = new OrderProduct();

                //when & then
                thenThrownBy(orderProduct::getProductNumber)
                        .isInstanceOf(DataNotFoundException.class)
                        .hasMessage("상품이 없습니다");
            }

            @Test
            void productNameIsNull() {
                Album album = new Album();

                ReflectionTestUtils.setField(album, "price", 15000);
                ReflectionTestUtils.setField(album, "stockQuantity", 10);

                OrderProduct orderProduct = OrderProduct.createOrderProduct(album, 5);

                //when & then
                thenThrownBy(orderProduct::getProductNumber)
                        .isInstanceOf(InvalidDataException.class)
                        .hasMessage("상품 번호가 없습니다");
            }
        }
    }
}