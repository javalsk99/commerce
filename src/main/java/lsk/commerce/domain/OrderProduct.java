package lsk.commerce.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lsk.commerce.exception.DataNotFoundException;
import lsk.commerce.exception.InvalidDataException;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = PROTECTED)
@SQLRestriction("deleted = false")
@SQLDelete(sql = "UPDATE order_product SET deleted = true WHERE order_product_id = ?")
public class OrderProduct {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "order_product_id")
    private Long id;

    @NotNull
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @NotNull
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @NotNull
    @Min(100)
    private Integer orderPrice;

    @NotNull
    @Min(1)
    private Integer quantity;

    @Column(nullable = false)
    private boolean deleted = false;

    public static OrderProduct createOrderProduct(Product product, Integer quantity) {
        if (quantity == null) {
            throw new IllegalArgumentException("수량이 없습니다");
        }

        OrderProduct orderProduct = new OrderProduct();
        orderProduct.product = product;
        orderProduct.orderPrice = product.getPrice() * quantity;
        orderProduct.quantity = quantity;

        product.removeStock(quantity);
        return orderProduct;
    }

    public String getProductNumber() {
        if (this.product == null) {
            throw new DataNotFoundException("상품이 없습니다");
        } else if (this.product.getProductNumber() == null) {
            throw new InvalidDataException("상품 번호가 없습니다");
        }

        return this.product.getProductNumber();
    }

    //Order에서 사용해서 protected
    protected void setOrder(Order order) {
        this.order = order;
    }
}
