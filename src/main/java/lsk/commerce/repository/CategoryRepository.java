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

    public Optional<Category> findWithChild(String categoryNumber) {
        return em.createQuery(
                        "select c from Category c" +
                                " left join fetch c.children" +
                                " where c.categoryNumber = :categoryNumber", Category.class)
                .setParameter("categoryNumber", categoryNumber)
                .getResultList()
                .stream()
                .findFirst();
    }

    public List<Category> findWithParent(String categoryName, String parentNumber) {
        return em.createQuery(
                        "select c from Category c" +
                                " left join fetch c.parent" +
                                " where c.name = :name" +
                                " or c.categoryNumber = :parentNumber", Category.class)
                .setParameter("name", categoryName)
                .setParameter("parentNumber", parentNumber)
                .getResultList();
    }

    public List<Category> findAll() {
        return em.createQuery("select c from Category c", Category.class)
                .getResultList();
    }

    public void delete(Category category) {
        em.remove(category);
    }

    public List<Category> findByNumberSet(Set<String> categoryNumberSet) {
        return em.createQuery(
                        "select c from Category c" +
                                " where c.categoryNumber in :categoryNumberSet", Category.class)
                .setParameter("categoryNumberSet", categoryNumberSet)
                .getResultList();
    }
}
