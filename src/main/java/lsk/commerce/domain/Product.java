package lsk.commerce.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.*;
import static jakarta.persistence.GenerationType.*;
import static jakarta.persistence.InheritanceType.*;
import static lombok.AccessLevel.*;

@Entity
@Inheritance(strategy = SINGLE_TABLE)
@DiscriminatorColumn(name = "dtype")
@Getter
@NoArgsConstructor(access = PUBLIC)
public abstract class Product {

    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name = "product_id")
    private Long id;

    //양방향 매핑으로 변경
    @OneToMany(mappedBy = "product", cascade = ALL)
    private List<CategoryProduct> categoryProducts = new ArrayList<>();

    private String name;
    private int price;
    private int stockQuantity;

    public Product(String name, int price, int stockQuantity) {
        this.name = name;
        this.price = price;
        this.stockQuantity = stockQuantity;
    }

    protected void addStock(int quantity) {
        this.stockQuantity += quantity;
    }

    protected void removeStock(int quantity) {
        int restStock = this.stockQuantity - quantity;
        if (restStock < 0) {
            throw new IllegalArgumentException("재고가 부족합니다.");
        }

        this.stockQuantity = restStock;
    }

    protected void updateStock(int quantity, int newQuantity) {
        this.stockQuantity += quantity;

        int restStock = this.stockQuantity - newQuantity;
        if (restStock < 0) {
            throw new IllegalArgumentException("재고가 부족합니다.");
        }

        this.stockQuantity = restStock;
    }

    //가격, 수량만 변경
    public void updateProduct(int newPrice, int newStockQuantity) {
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

    //CategoryProduct에서 protected 생성자, 메서드를 사용하기 위해서 패키지 이동
    public void addCategoryProduct(Category... categories) {
        for (Category category : categories) {
            while (category != null) {
                connectCategory(category);
                category = category.getParent();
            }
        }
    }

    public void addCategoryProduct(List<Category> categories) {
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
