package lsk.commerce.service;

import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Delivery;
import lsk.commerce.domain.Member;
import lsk.commerce.domain.Order;
import lsk.commerce.domain.OrderProduct;
import lsk.commerce.domain.product.Product;
import lsk.commerce.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final MemberService memberService;
    private final ProductService productService;

    public Long order(Long memberId, int count, Long... productIds) {
        //엔티티 조회
        Member member = memberService.findMember(memberId);
        List<Product> products = new ArrayList<>();
        for (Long productId : productIds) {
            products.add(productService.findProduct(productId));
        }

        //배송 정보 생성
        Delivery delivery = new Delivery(member);

        //주문 상품 생성
        List<OrderProduct> orderProducts = new ArrayList<>();
        for (Product product : products) {
            orderProducts.add(OrderProduct.createOrderProduct(count, product));
        }

        //주문 생성
        Order order = Order.CreateOrder(member, delivery, orderProducts);

        //주문 저장
        orderRepository.save(order);

        return order.getId();
    }
}
