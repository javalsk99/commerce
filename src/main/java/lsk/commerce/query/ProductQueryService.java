package lsk.commerce.query;

import lombok.RequiredArgsConstructor;
import lsk.commerce.dto.response.ProductResponse;
import lsk.commerce.query.dto.ProductSearchCond;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductQueryService {

    private final ProductQueryRepository productQueryRepository;

    public List<ProductResponse> searchProducts(ProductSearchCond cond) {
        return productQueryRepository.search(cond);
    }

    public List<ProductResponse> searchProductsByCategoryName(String categoryName, ProductSearchCond cond) {
        return productQueryRepository.searchByCategoryName(categoryName, cond);
    }
}
