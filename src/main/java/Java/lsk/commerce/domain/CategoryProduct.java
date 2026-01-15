package Java.lsk.commerce.domain;

import Java.lsk.commerce.domain.product.Product;
import jakarta.persistence.*;
import lombok.Getter;

import static jakarta.persistence.FetchType.*;

@Entity
@Getter
public class CategoryProduct {

    @Id @GeneratedValue
    @Column(name = "category_product_id")
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "product_id")
    private Product product;
}
