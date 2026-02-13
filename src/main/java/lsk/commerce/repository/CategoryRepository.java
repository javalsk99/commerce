package lsk.commerce.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Category;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class CategoryRepository {

    private final EntityManager em;

    public void save(Category category) {
        em.persist(category);
    }

    public List<Category> findAll() {
        return em.createQuery("select c from Category c", Category.class)
                .getResultList();
    }

    public Optional<Category> findWithChild(String categoryName) {
        return em.createQuery("select c from Category c" +
                        " left join fetch c.child" +
                        " where c.name = :name", Category.class)
                .setParameter("name", categoryName)
                .getResultStream()
                .findFirst();
    }

    public void delete(Category category) {
        em.remove(category);
    }

    public List<Category> existsByCategoryName(String categoryName, String parentCategoryName) {
        return em.createQuery(
                "select c from Category c" +
                        " where c.name = :name" +
                        " or (:parentName is not null and c.name = :parentName)", Category.class)
                .setParameter("name", categoryName)
                .setParameter("parentName", parentCategoryName)
                .getResultList();
    }

    public Long countCategories(Set<Long> categoryIds) {
        return em.createQuery(
                        "select count(c) from Category c" +
                                " where c.id in :categoryIds", Long.class)
                .setParameter("categoryIds", categoryIds)
                .getSingleResult();
    }
}
