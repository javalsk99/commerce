package lsk.commerce.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Category;
import lsk.commerce.domain.Product;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CategoryRepository {

    private final EntityManager em;

    public void save(Category category) {
        em.persist(category);
    }

    public Category findOne(Long categoryId) {
        return em.find(Category.class, categoryId);
    }

    public List<Category> findAll() {
        return em.createQuery("select c from Category c", Category.class)
                .getResultList();
    }

    public List<Product> findProductsByCategoryId(Long categoryId) {
        return em.createQuery("select p from Product p " +
                        " join p.categoryProducts cp" +
                        " join cp.category c" +
                        " where c.id = :id", Product.class)
                .setParameter("id", categoryId)
                .getResultList();
    }

    public void delete(Category category) {
        em.remove(category);
    }
}
