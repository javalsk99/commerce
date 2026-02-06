package lsk.commerce.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lsk.commerce.dto.request.OrderRequest;
import lsk.commerce.domain.*;
import lsk.commerce.domain.Product;
import lsk.commerce.dto.response.OrderResponse;
import lsk.commerce.repository.OrderProductJdbcRepository;
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

    private final EntityManager em;

    private final OrderRepository orderRepository;
    private final MemberService memberService;
    private final ProductService productService;

    private final OrderProductJdbcRepository orderProductJdbcRepository;

    public String order(String memberLoginId, Map<String, Integer> productNamesCount) {
        //엔티티 조회
        Member member = memberService.findMemberByLoginId(memberLoginId);
        List<Product> products = productService.findProducts();

        //배송 정보 생성
        Delivery delivery = new Delivery(member);

        List<OrderProduct> orderProducts = new ArrayList<>();

        for (Map.Entry<String, Integer> productNameCountEntry : productNamesCount.entrySet()) {
            String productName = productNameCountEntry.getKey();
            int count = productNameCountEntry.getValue();

            //주문 상품 생성
            Product product = products.stream()
                    .filter(p -> productName.equals(p.getName()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다. name: " + productName));
            orderProducts.add(OrderProduct.createOrderProduct(product, count));
        }

        //주문 생성
        Order order = Order.createOrder(member, delivery, orderProducts);

        //주문 저장
        orderRepository.save(order);

        //주문 상품 저장
        registerOrderProducts(orderProducts);

        return order.getOrderNumber();
    }

    public Order updateOrder(String orderNumber, Map<String, Integer> newProductNamesCount) {
        Order order = findOrderWithAllExceptMember(orderNumber);

        //결제가 됐는지 검증
        if (order.getOrderStatus() != CREATED) {
            throw new IllegalStateException("결제가 완료돼서 주문을 수정할 수 없습니다.");
        }

        //기존 주문 상품 삭제
        OrderProduct.deleteOrderProduct(order);

        //영속성 컨텍스트 정리
        em.flush();
        em.clear();

        Order currentOrder = findOrderWithDeliveryPayment(orderNumber);
        List<Product> currentProducts = productService.findProducts();

        List<OrderProduct> newOrderProducts = new ArrayList<>();

        for (Map.Entry<String, Integer> newProductNameCountEntry : newProductNamesCount.entrySet()) {
            String newProductName = newProductNameCountEntry.getKey();
            int newCount = newProductNameCountEntry.getValue();

            Product newProduct = currentProducts.stream()
                    .filter(p -> newProductName.equals(p.getName()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다. name: " + newProductName));
            newOrderProducts.add(OrderProduct.createOrderProduct(newProduct, newCount));
        }

        //새로운 주문 상품으로 변경 (나중에 주문의 주문 상품과 새로운 주문 상품을 비교해서, 같은 상품이면 수량만 변경, 새로운 상품이면 추가, 기존 상품이 새로운 주문 상품에 없으면 삭제로 변경)
        Order.updateOrder(currentOrder, newOrderProducts);

        //기존의 주문 상품 삭제와 새로운 주문 상품 저장
        orderProductJdbcRepository.deleteOrderProductsByOrderId(currentOrder.getId());
        orderProductJdbcRepository.saveAll(newOrderProducts);

        return currentOrder;
    }

    @Transactional(readOnly = true)
    public Order findOrder(Long orderId) {
        return orderRepository.findOne(orderId);
    }

    @Transactional(readOnly = true)
    public Order findOrderByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }

    @Transactional(readOnly = true)
    public Order findOrderWithDelivery(String orderNumber) {
        return orderRepository.findWithDelivery(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));
    }

    @Transactional(readOnly = true)
    public Order findOrderWithDeliveryPayment(String orderNumber) {
        return orderRepository.findWithDeliveryPayment(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));
    }

    @Transactional(readOnly = true)
    public Order findOrderWithAllExceptMember(String orderNumber) {
        return orderRepository.findWithAllExceptMember(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));
    }

    @Transactional(readOnly = true)
    public Order findOrderWithAll(String orderNumber) {
        return orderRepository.findWithAll(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));
    }

    public void deleteOrder(String orderNumber) {
        Order order = findOrderWithDeliveryPayment(orderNumber);
        if (order.getOrderStatus() == CREATED) {
            throw new IllegalStateException("주문을 취소해야 삭제할 수 있습니다.");
        } else if (order.getOrderStatus() == PAID) {
            throw new IllegalStateException("배송이 완료돼야 삭제할 수 있습니다.");
        }

        orderProductJdbcRepository.softDeleteOrderProductsByOrderId(order.getId());
        orderRepository.delete(order);
    }

    //결제 로직 검증용
    @Transactional(readOnly = true)
    public OrderRequest getOrderRequest(String orderNumber) {
        Order order = findOrderWithAll(orderNumber);
        return OrderRequest.orderChangeRequest(order);
    }

    //주문 리턴용
    @Transactional(readOnly = true)
    public OrderResponse getOrderResponse(Order order) {
        return OrderResponse.orderChangeResponse(order);
    }

    public Order cancelOrder(String orderNumber) {
        Order order = findOrderWithAllExceptMember(orderNumber);
        order.cancel();
        return order;
    }

    private void registerOrderProducts(List<OrderProduct> orderProducts) {
        if (orderProducts.isEmpty()) {
            throw new IllegalArgumentException("주문 상품이 없습니다.");
        }

        orderProductJdbcRepository.saveAll(orderProducts);
    }
}
