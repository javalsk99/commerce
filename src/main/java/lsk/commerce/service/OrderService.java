package lsk.commerce.service;

import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.*;
import lsk.commerce.domain.product.Product;
import lsk.commerce.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static lsk.commerce.domain.OrderStatus.*;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final MemberService memberService;
    private final ProductService productService;

    //기존의 파라미터는 각 상품별 수량을 조절할 수 없어서 Map으로 변경
    public Long order(Long memberId, Map<Long, Integer> productIdsCount) {
        //엔티티 조회
        Member member = memberService.findMember(memberId);

        //배송 정보 생성
        Delivery delivery = new Delivery(member);

        List<OrderProduct> orderProducts = new ArrayList<>();

        for (Map.Entry<Long, Integer> productIdCountEntry : productIdsCount.entrySet()) {
            Long productId = productIdCountEntry.getKey();
            int count = productIdCountEntry.getValue();

            //주문 상품 생성
            Product product = productService.findProduct(productId);
            orderProducts.add(OrderProduct.createOrderProduct(product, count));
        }

        //주문 생성
        Order order = Order.createOrder(member, delivery, orderProducts);

        //주문 저장
        orderRepository.save(order);

        return order.getId();
    }

    public void updateOrder(Order order, Map<Long, Integer> newProductIdsCount) {
        //결제가 됐는지 검증
        if (!order.getOrderStatus().equals(CREATED)) {
            throw new IllegalStateException("결제가 완료돼서 주문을 수정할 수 없습니다.");
        }

        //기존 주문 상품 삭제
        OrderProduct.deleteOrderProduct(order);

        List<OrderProduct> newOrderProducts = new ArrayList<>();

        for (Map.Entry<Long, Integer> newProductIdCountEntry : newProductIdsCount.entrySet()) {
            Long newProductId = newProductIdCountEntry.getKey();
            int newCount = newProductIdCountEntry.getValue();

            //새로운 주문 상품 생성
            Product newProduct = productService.findProduct(newProductId);
            newOrderProducts.add(OrderProduct.createOrderProduct(newProduct, newCount));
        }

        //새로운 주문 상품으로 변경 (나중에 주문의 주문 상품과 새로운 주문 상품을 비교해서, 같은 상품이면 수량만 변경, 새로운 상품이면 추가, 기존 상품이 새로운 주문 상품에 없으면 삭제로 변경)
        Order.updateOrder(order, newOrderProducts);
    }

    public Order findOrder(Long orderId) {
        return orderRepository.findOne(orderId);
    }

    public void DeleteOrder(Order order) {
        orderRepository.delete(order);
    }
}
