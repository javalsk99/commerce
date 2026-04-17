package lsk.commerce.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lsk.commerce.exception.DuplicateResourceException;
import lsk.commerce.util.NanoIdProvider;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "UniqueNameWithParent", columnNames = {"name", "parent_id"}))
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Category {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "category_id")
    private Long id;

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9]{12}$", message = "카테고리 번호는 영문, 숫자만 사용하여 12자로 입력해 주세요")
    @Column(unique = true, length = 12)
    private String categoryNumber;

    //양방향 매핑으로 변경
    @OneToMany(mappedBy = "category")
    private List<CategoryProduct> categoryProducts = new ArrayList<>();

    @NotBlank
    @Size(max = 20)
    @Column(length = 20)
    private String name;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent")
    private List<Category> children = new ArrayList<>();

    private void connectParent(Category parentCategory) {
        this.parent = parentCategory;
        parentCategory.children.add(this);
    }

    public void unConnectParent() {
        if (this.parent == null) {
            return;
        }

        this.parent.children.remove(this);
        this.parent = null;
    }

    public static Category createCategory(Category parentCategory, String name) {
        Category category = new Category();
        category.categoryNumber = NanoIdProvider.createNanoId();
        category.name = name;
        if (parentCategory != null) {
            category.connectParent(parentCategory);
        }

        return category;
    }

    public void changeParentCategory(Category newParentCategory) {
        if (this.getId().equals(newParentCategory.getId()) || (this.parent != null && this.parent.getId().equals(newParentCategory.getId()))) {
            return;
        }

        if (this.name.equals(newParentCategory.getName())) {
            throw new DuplicateResourceException("자신과 같은 이름의 카테고리를 부모로 설정할 수 없습니다. name: " + this.name);
        }

        if (newParentCategory.getChildren().stream()
                .anyMatch(c -> c.getName().equals(this.getName()))) {
            throw new DuplicateResourceException("선택한 부모 카테고리에 이미 같은 이름의 카테고리가 있습니다. name: " + this.name);
        }

        Category check = newParentCategory;
        while (check != null) {
            if (this.getId().equals(check.getId())) {
                throw new IllegalArgumentException("자식을 부모로 설정할 수 없습니다");
            }

            check = check.parent;
        }

        this.unConnectParent();
        this.connectParent(newParentCategory);
    }
}
