package lsk.commerce.domain;

import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import lombok.Getter;

import static jakarta.persistence.FetchType.*;
import static jakarta.persistence.GenerationType.*;
import static lombok.AccessLevel.*;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
public class CategoryProduct {

    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name = "category_product_id")
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    protected void setProduct(Product product) {
        this.product = product;
    }

    protected void setCategory(Category category) {
        this.category = category;
    }
}
