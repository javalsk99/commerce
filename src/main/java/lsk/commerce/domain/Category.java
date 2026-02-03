package lsk.commerce.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.*;
import static jakarta.persistence.GenerationType.*;
import static lombok.AccessLevel.*;

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

    private String name;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent")
    private List<Category> child = new ArrayList<>();

    private void connectParent(Category parentCategory) {
        this.parent = parentCategory;
        parentCategory.child.add(this);
    }

    //CategoryService에서 사용해서 public
    public void unConnectParent() {
        this.parent.child.remove(this);
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
        if (!this.getChild().isEmpty()) {
            throw new IllegalStateException("자식 카테고리가 있어서 부모 카테고리를 변경할 수 없습니다.");
        }

        this.unConnectParent();
        this.connectParent(newParentCategory);
        return this;
    }
}
