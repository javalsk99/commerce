package lsk.commerce.domain;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lsk.commerce.util.InitialExtractor;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.GenerationType.IDENTITY;
import static jakarta.persistence.InheritanceType.SINGLE_TABLE;
import static lombok.AccessLevel.PUBLIC;

@Entity
@Inheritance(strategy = SINGLE_TABLE)
@DiscriminatorColumn(name = "dtype", length = 1)
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "UniqueAlbum", columnNames = {"name", "artist", "studio"}),
        @UniqueConstraint(name = "UniqueBook", columnNames = {"name", "author", "isbn"}),
        @UniqueConstraint(name = "UniqueMovie", columnNames = {"name", "actor", "director"})
})
@Getter
@NoArgsConstructor(access = PUBLIC)
public abstract class Product {

    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name = "product_id")
    private Long id;

    //양방향 매핑으로 변경
    @OneToMany(mappedBy = "product", cascade = ALL)
    private List<CategoryProduct> categoryProducts = new ArrayList<>();

    @NotBlank @Size(max = 50)
    @Column(length = 50)
    private String name;

    @Column(nullable = false, length = 50)
    private String nameInitial;

    @NotNull @Min(100)
    private Integer price;

    @NotNull @Min(0)
    private Integer stockQuantity;

    protected Product(String name, Integer price, Integer stockQuantity) {
        this.name = name;
        this.price = price;
        this.stockQuantity = stockQuantity;
    }

    protected void addStock(Integer quantity) {
        if (quantity == null) {
            throw new IllegalArgumentException("재고가 추가될 수량이 없습니다.");
        }

        this.stockQuantity += quantity;
    }

    protected void removeStock(Integer quantity) {
        if (quantity == null) {
            throw new IllegalArgumentException("재고가 감소될 수량이 없습니다.");
        }

        int restStock = this.stockQuantity - quantity;
        if (restStock < 0) {
            throw new IllegalArgumentException("재고가 부족합니다.");
        }

        this.stockQuantity = restStock;
    }

    protected void updateStock(Integer quantity, Integer newQuantity) {
        if (quantity == null) {
            throw new IllegalArgumentException("재고가 추가될 수량이 없습니다.");
        } else if (newQuantity == null) {
            throw new IllegalArgumentException("재고가 감소될 수량이 없습니다.");
        }

        int restStock = this.stockQuantity + quantity - newQuantity;
        if (restStock < 0) {
            throw new IllegalArgumentException("재고가 부족합니다.");
        }

        this.stockQuantity = restStock;
    }

    //가격, 수량만 변경
    public void updateProduct(Integer newPrice, Integer newStockQuantity) {
        if (newPrice == null && newStockQuantity == null) {
            throw new IllegalArgumentException("수정할 가격 또는 수량이 있어야 합니다.");
        }

        if (newPrice != null) {
            this.price = newPrice;
        }
        if (newStockQuantity != null) {
            this.stockQuantity = newStockQuantity;
        }
    }

    public void connectCategory(Category category) {
        if (category.getId() == null) {
            throw new IllegalArgumentException("식별자가 없는 잘못된 카테고리입니다.");
        }

        CategoryProduct categoryProduct = new CategoryProduct();
        categoryProduct.setProduct(this);
        categoryProduct.setCategory(category);
        this.categoryProducts.add(categoryProduct);
        category.getCategoryProducts().add(categoryProduct);
    }

    public void connectCategories(List<Category> categories) {
        for (Category category : categories) {
            while (category != null) {
                Category finalCategory = category;
                if (categoryProducts.stream().anyMatch(categoryProduct -> categoryProduct.getCategory() == finalCategory)) {
                    break;
                }

                connectCategory(category);
                category = category.getParent();
            }
        }
    }

    //상품 제거할 때 카테고리에서 카테고리 상품들 제거
    public void removeCategoryProductsFormCategory() {
        for (CategoryProduct categoryProduct : this.categoryProducts) {
            Category category = categoryProduct.getCategory();

            if (category != null) {
                category.getCategoryProducts().remove(categoryProduct);
            }
        }
    }

    //같은 카테고리 상품인 상품과 카테고리를 카테고리 상품 제거
    public CategoryProduct removeCategoryProduct(Category category) {
        if (this.categoryProducts.isEmpty()) {
            throw new IllegalArgumentException("상품과 연결된 카테고리가 없습니다.");
        }

        for (CategoryProduct categoryProduct : this.categoryProducts) {
            if (categoryProduct.getCategory() == null) {
                throw new IllegalArgumentException("상품에 카테고리가 제대로 들어가지 않았습니다.");
            }

            if (category.getId().equals(categoryProduct.getCategory().getId())) {
                this.categoryProducts.remove(categoryProduct);
                category.getCategoryProducts().remove(categoryProduct);
                return categoryProduct;
            }
        }

        throw new IllegalArgumentException("상품이 해당 카테고리에 없습니다.");
    }

    @PrePersist @PreUpdate
    protected void preHandler() {
        this.nameInitial = InitialExtractor.extract(this.name);
    }
}
