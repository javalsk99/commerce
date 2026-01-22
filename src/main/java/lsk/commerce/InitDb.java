package lsk.commerce;

import io.portone.sdk.server.common.Currency;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Category;
import lsk.commerce.domain.Member;
import lsk.commerce.domain.Product;
import lsk.commerce.domain.product.Album;
import lsk.commerce.service.CategoryService;
import lsk.commerce.service.MemberService;
import lsk.commerce.service.OrderService;
import lsk.commerce.service.ProductService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class InitDb {

    private final InitService initService;

    @PostConstruct
    public void init() {
        initService.dbInit();
    }

    @Component
    @Transactional
    @RequiredArgsConstructor
    static class InitService {

        private final MemberService memberService;
        private final CategoryService categoryService;
        private final ProductService productService;
        private final OrderService orderService;

        public void dbInit() {
            Long memberId = memberService.join(new Member("test", "testId", "testPassword", "seoul", "Gangbuk", "11111"));
            Category parentCategory = Category.createParentCategory("dance");
            categoryService.create(parentCategory);
            productService.register(new Album("하얀 그리움", 100, 10, "fromis_9", "ASND", Currency.Krw.INSTANCE.getValue()), parentCategory);
            Product product = productService.findProduct(1L);
            Long orderId = orderService.order(memberId, Map.of(product.getId(), 1));
        }
    }
}
