package lsk.commerce.domain;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import static jakarta.persistence.FetchType.*;
import static jakarta.persistence.GenerationType.*;
import static lombok.AccessLevel.*;

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

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    private int count;
    private int orderPrice;

    private boolean deleted = false;

    public static OrderProduct createOrderProduct(Product product, int count) {
        OrderProduct orderProduct = new OrderProduct();
        orderProduct.product = product;
        orderProduct.count = count;
        orderProduct.orderPrice = product.getPrice() * count;

        product.removeStock(count);
        return orderProduct;
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

        order.getOrderProducts().clear();
    }

    //Order에서 사용해서 protected
    protected void setOrder(Order order) {
        this.order = order;
    }
}
