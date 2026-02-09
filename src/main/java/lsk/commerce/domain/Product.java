package lsk.commerce.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @NotNull @Min(100)
    private Integer price;

    @NotNull @Min(0)
    private Integer stockQuantity;

    public Product(String name, Integer price, Integer stockQuantity) {
        this.name = name;
        this.price = price;
        this.stockQuantity = stockQuantity;
    }

    protected void addStock(Integer quantity) {
        this.stockQuantity += quantity;
    }

    protected void removeStock(Integer quantity) {
        int restStock = this.stockQuantity - quantity;
        if (restStock < 0) {
            throw new IllegalArgumentException("재고가 부족합니다.");
        }

        this.stockQuantity = restStock;
    }

    protected void updateStock(Integer quantity, Integer newQuantity) {
        this.stockQuantity += quantity;

        int restStock = this.stockQuantity - newQuantity;
        if (restStock < 0) {
            throw new IllegalArgumentException("재고가 부족합니다.");
        }

        this.stockQuantity = restStock;
    }

    //가격, 수량만 변경
    public void updateProduct(Integer newPrice, Integer newStockQuantity) {
        this.price = newPrice;
        this.stockQuantity = newStockQuantity;
    }

    public void connectCategory(Category category) {
        CategoryProduct categoryProduct = new CategoryProduct();
        categoryProduct.setProduct(this);
        categoryProduct.setCategory(category);
        this.categoryProducts.add(categoryProduct);
        category.getCategoryProducts().add(categoryProduct);
    }

    public void addCategoryProduct(List<Category> categories) {
        for (Category category : categories) {
            if (category.getId() == null) {
                throw new IllegalArgumentException("존재하지 않는 카테고리입니다.");
            }

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
    public void removeCategoryProducts() {
        for (CategoryProduct categoryProduct : this.categoryProducts) {
            Category category = categoryProduct.getCategory();

            if (category != null) {
                category.getCategoryProducts().remove(categoryProduct);
            }
        }
    }

    //같은 카테고리 상품인 상품과 카테고리를 카테고리 상품 제거
    public CategoryProduct removeCategoryProduct(Category category) {
        if (categoryProducts.isEmpty()) {
            throw new IllegalArgumentException("상품과 연결된 카테고리가 없습니다.");
        }

        for (CategoryProduct categoryProduct : categoryProducts) {
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
}
