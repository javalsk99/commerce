package lsk.commerce.domain;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lsk.commerce.domain.product.Product;
import jakarta.persistence.*;
import lombok.Getter;

import static jakarta.persistence.FetchType.*;
import static lombok.AccessLevel.*;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
public class OrderProduct {

    @Id @GeneratedValue
    @Column(name = "order_product_id")
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    private int orderPrice;
    private int count;

    public static OrderProduct createOrderProduct(int count, Product product) {
        OrderProduct orderProduct = new OrderProduct();
        orderProduct.product = product;
        orderProduct.orderPrice = product.getPrice() * count;
        orderProduct.count = count;

        product.removeStock(count);
        return orderProduct;
    }

    protected void setOrder(Order order) {
        this.order = order;
    }
}
