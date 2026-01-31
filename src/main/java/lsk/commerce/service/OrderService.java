package lsk.commerce.service;

import lombok.RequiredArgsConstructor;
import lsk.commerce.dto.request.OrderRequest;
import lsk.commerce.domain.*;
import lsk.commerce.domain.Product;
import lsk.commerce.dto.response.OrderResponse;
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
    public String order(Long memberId, Map<Long, Integer> productIdsCount) {
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

        return order.getOrderNumber();
    }

    public String order(String memberLoginId, Map<String, Integer> productNamesCount) {
        //엔티티 조회
        Member member = memberService.findMemberByLoginId(memberLoginId);

        //배송 정보 생성
        Delivery delivery = new Delivery(member);

        List<OrderProduct> orderProducts = new ArrayList<>();

        for (Map.Entry<String, Integer> productNameCountEntry : productNamesCount.entrySet()) {
            String productName = productNameCountEntry.getKey();
            int count = productNameCountEntry.getValue();

            //주문 상품 생성
            Product product = productService.findProductByName(productName);
            orderProducts.add(OrderProduct.createOrderProduct(product, count));
        }

        //주문 생성
        Order order = Order.createOrder(member, delivery, orderProducts);

        //주문 저장
        orderRepository.save(order);

        return order.getOrderNumber();
    }

    public void updateOrder(String orderNumber, Map<String, Integer> newProductNamesCount) {
        Order order = findOrderByOrderNumber(orderNumber);

        //결제가 됐는지 검증
        if (order.getOrderStatus() != CREATED) {
            throw new IllegalStateException("결제가 완료돼서 주문을 수정할 수 없습니다.");
        }

        //기존 주문 상품 삭제
        OrderProduct.deleteOrderProduct(order);

        List<OrderProduct> newOrderProducts = new ArrayList<>();

        for (Map.Entry<String, Integer> newProductNameCountEntry : newProductNamesCount.entrySet()) {
            String newProductName = newProductNameCountEntry.getKey();
            int newCount = newProductNameCountEntry.getValue();

            //새로운 주문 상품 생성
            Product newProduct = productService.findProductByName(newProductName);
            newOrderProducts.add(OrderProduct.createOrderProduct(newProduct, newCount));
        }

        //새로운 주문 상품으로 변경 (나중에 주문의 주문 상품과 새로운 주문 상품을 비교해서, 같은 상품이면 수량만 변경, 새로운 상품이면 추가, 기존 상품이 새로운 주문 상품에 없으면 삭제로 변경)
        Order.updateOrder(order, newOrderProducts);
    }

    @Transactional(readOnly = true)
    public Order findOrder(Long orderId) {
        return orderRepository.findOne(orderId);
    }

    @Transactional(readOnly = true)
    public Order findOrderByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }

    public void deleteOrder(Order order) {
        if (order.getOrderStatus() == CREATED) {
            throw new IllegalStateException("주문을 취소해야 삭제할 수 있습니다.");
        } else if (order.getOrderStatus() == PAID) {
            throw new IllegalStateException("배송이 완료돼야 삭제할 수 있습니다.");
        }

        orderRepository.delete(order);
    }

    //결제 로직 검증용
    @Transactional(readOnly = true)
    public OrderRequest getOrderRequest(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber);
        return OrderRequest.orderChangeRequest(order);
    }

    //주문 리턴용
    @Transactional(readOnly = true)
    public OrderResponse getOrderResponse(Order order) {
        return OrderResponse.orderChangeResponse(order);
    }

    public void cancelOrder(Order order) {
        order.cancel();
    }
}
