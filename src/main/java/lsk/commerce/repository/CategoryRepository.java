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

    public Category findByName(String name) {
        return em.createQuery("select c from Category c where c.name = :name", Category.class)
                .setParameter("name", name)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    public List<Category> findAll() {
        return em.createQuery("select c from Category c", Category.class)
                .getResultList();
    }

    public List<Product> findProductsByCategoryId(String categoryName) {
        return em.createQuery("select p from Product p " +
                        " join p.categoryProducts cp" +
                        " join cp.category c" +
                        " where c.name = :name", Product.class)
                .setParameter("name", categoryName)
                .getResultList();
    }

    public void delete(Category category) {
        em.remove(category);
    }
}
