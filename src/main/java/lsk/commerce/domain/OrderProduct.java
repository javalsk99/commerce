package lsk.commerce.domain;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lsk.commerce.domain.product.Product;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.Map;

import static jakarta.persistence.FetchType.*;
import static lombok.AccessLevel.*;

@Entity
@Getter
@EqualsAndHashCode
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

    private int count;
    private int orderPrice;

    public static OrderProduct createOrderProduct(Product product, int count) {
        OrderProduct orderProduct = new OrderProduct();
        orderProduct.product = product;
        orderProduct.count = count;
        orderProduct.orderPrice = product.getPrice() * count;

        product.removeStock(count);
        return orderProduct;
    }

    protected void setOrder(Order order) {
        this.order = order;
    }

    //수량만 변경
    public static void updateCountOrderProduct(OrderProduct orderProduct, Product product, int newCount) {
        if (orderProduct.product.equals(product)) {
            product.updateStock(orderProduct.count, newCount);
            orderProduct.count = newCount;
        }
    }

    public static void deleteOrderProduct(Order order) {
        for (OrderProduct orderProduct : order.getOrderProducts()) {
            orderProduct.product.addStock(orderProduct.count);
        }
        order.getOrderProducts().removeAll(order.getOrderProducts());
    }
}
