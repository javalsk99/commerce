package lsk.commerce.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Category;
import lsk.commerce.domain.CategoryProduct;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CategoryProductRepository {

    private final EntityManager em;

    public List<CategoryProduct> findAllWithProductByCategory(Category category) {
        return em.createQuery(
                        "select cp from CategoryProduct cp" +
                                " join fetch cp.product p" +
                                " where cp.category = :category", CategoryProduct.class)
                .setParameter("category", category)
                .getResultList();
    }

    public void delete(CategoryProduct categoryProduct) {
        em.remove(categoryProduct);
    }
}
