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
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.Map;

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

    @Id @GeneratedValue(strategy = IDENTITY)
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

    @NotNull @Min(100)
    private Integer orderPrice;

    @NotNull @Min(1)
    private Integer count;

    @Column(nullable = false)
    private boolean deleted = false;

    public static OrderProduct createOrderProduct(Product product, Integer count) {
        if (count == null) {
            throw new IllegalArgumentException("수량이 없습니다.");
        }

        OrderProduct orderProduct = new OrderProduct();
        orderProduct.product = product;
        orderProduct.orderPrice = product.getPrice() * count;
        orderProduct.count = count;

        product.removeStock(count);
        return orderProduct;
    }

    public String getProductName() {
        if (this.product == null) {
            throw new IllegalStateException("상품이 없습니다.");
        } else if (this.product.getName() == null) {
            throw new IllegalStateException("상품 이름이 없습니다.");
        }

        return this.product.getName();
    }

    //Order에서 사용해서 protected
    protected void setOrder(Order order) {
        this.order = order;
    }
}
