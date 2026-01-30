package lsk.commerce.service;

import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Category;
import lsk.commerce.domain.CategoryProduct;
import lsk.commerce.domain.Product;
import lsk.commerce.repository.CategoryProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class CategoryProductService {

    private final CategoryProductRepository categoryProductRepository;

    //상품 삭제할 때 사용
    public void disconnect(CategoryProduct categoryProduct) {
        categoryProductRepository.delete(categoryProduct);
    }

    //연결만 끊을 때 사용
    public void disconnect(Category category, Product product) {
        CategoryProduct categoryProduct = product.removeCategoryProduct(category);
        categoryProductRepository.delete(categoryProduct);
    }

    public void connect(Product product, Category category) {
        product.connectCategory(category);
    }
}
