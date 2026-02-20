package lsk.commerce.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Category {

    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name = "category_id")
    private Long id;

    //양방향 매핑으로 변경
    @OneToMany(mappedBy = "category")
    private List<CategoryProduct> categoryProducts = new ArrayList<>();

    @NotBlank @Size(max = 20)
    @Column(unique = true, length = 20)
    private String name;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent")
    private List<Category> child = new ArrayList<>();

    private void connectParent(Category parentCategory) {
        if (parentCategory == null) {
            throw new IllegalArgumentException("부모 카테고리로 선택한 카테고리가 존재하지 않습니다.");
        }

        this.parent = parentCategory;
        parentCategory.child.add(this);
    }

    public void unConnectParent() {
        if (this.parent == null) {
            return;
        }

        this.parent.child.remove(this);
        this.parent = null;
    }

    public static Category createCategory(Category parentCategory, String name) {
        Category category = new Category();
        category.name = name;
        if (parentCategory != null) {
            category.connectParent(parentCategory);
        }

        return category;
    }

    public Category changeParentCategory(Category newParentCategory) {
        Category check = newParentCategory;
        while (check != null) {
            if (this.getId().equals(check.getId())) {
                throw new IllegalArgumentException("자신 또는 자식을 부모로 설정할 수 없습니다.");
            }

            check = check.parent;
        }

        this.unConnectParent();
        this.connectParent(newParentCategory);
        return this;
    }
}
