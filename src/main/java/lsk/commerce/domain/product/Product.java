package lsk.commerce.domain.product;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lsk.commerce.domain.OrderProduct;
import static jakarta.persistence.InheritanceType.*;
import static lombok.AccessLevel.*;

@Entity
@Inheritance(strategy = SINGLE_TABLE)
@DiscriminatorColumn(name = "dtype")
@Getter
@NoArgsConstructor(access = PROTECTED)
public abstract class Product {

    @Id @GeneratedValue
    @Column(name = "product_id")
    private Long id;

    private String name;
    private int price;
    private int stockQuantity;

    public Product(String name, int price, int stockQuantity) {
        this.name = name;
        this.price = price;
        this.stockQuantity = stockQuantity;
    }

    public void addStock(int quantity) {
        this.stockQuantity += quantity;
    }

    public void removeStock(int quantity) {
        int restStock = this.stockQuantity - quantity;
        if (restStock < 0) {
            throw new IllegalArgumentException("재고가 부족합니다.");
        }

        this.stockQuantity = restStock;
    }

    public void updateProduct(String newName, int newPrice, int newStockQuantity) {
        this.name = newName;
        this.price = newPrice;
        this.stockQuantity = newStockQuantity;
    }
}
