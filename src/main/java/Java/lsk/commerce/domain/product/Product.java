package Java.lsk.commerce.domain.product;

import jakarta.persistence.*;
import lombok.Getter;

import static jakarta.persistence.InheritanceType.*;

@Entity
@Inheritance(strategy = SINGLE_TABLE)
@DiscriminatorColumn(name = "dtype")
@Getter
public abstract class Product {

    @Id @GeneratedValue
    @Column(name = "product_id")
    private Long id;

    private String name;
    private int price;
    private int stockQuantity;
}
