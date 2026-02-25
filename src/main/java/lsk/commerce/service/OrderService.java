package lsk.commerce.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Delivery;
import lsk.commerce.domain.DeliveryStatus;
import lsk.commerce.domain.Member;
import lsk.commerce.domain.Order;
import lsk.commerce.domain.OrderProduct;
import lsk.commerce.domain.OrderStatus;
import lsk.commerce.domain.PaymentStatus;
import lsk.commerce.domain.Product;
import lsk.commerce.dto.request.OrderRequest;
import lsk.commerce.dto.response.OrderResponse;
import lsk.commerce.repository.OrderProductJdbcRepository;
import lsk.commerce.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


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
        if (productNamesCount == null || productNamesCount.isEmpty()) {
            throw new IllegalArgumentException("주문할 상품이 없습니다.");
        }

        //엔티티 조회
        Member member = memberService.findMemberByLoginId(memberLoginId);
        List<Product> products = productService.findProducts();

        //배송 정보 생성
        Delivery delivery = new Delivery(member);

        List<OrderProduct> orderProducts = new ArrayList<>();

        for (Map.Entry<String, Integer> productNameCountEntry : productNamesCount.entrySet()) {
            String productName = productNameCountEntry.getKey();
            Integer count = productNameCountEntry.getValue();

            //주문 상품 생성
            Product product = products.stream()
                    .filter(p -> p.getName().equals(productName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다. name: " + productName));
            orderProducts.add(OrderProduct.createOrderProduct(product, count));
        }

        //주문 생성
        Order order = Order.createOrder(member, delivery, orderProducts);

        //주문 저장
        orderRepository.save(order);
        em.flush();

        //주문 상품 저장
        registerOrderProducts(orderProducts);

        String orderNumber = order.getOrderNumber();
        em.clear();

        return orderNumber;
    }

    @Transactional(readOnly = true)
    public Order findOrder(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));
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

    public void updateOrder(String orderNumber, Map<String, Integer> newProductNamesCount) {
        if (newProductNamesCount == null || newProductNamesCount.isEmpty()) {
            throw new IllegalArgumentException("주문을 수정할 상품이 없습니다.");
        }

        Order order = findOrderWithAllExceptMember(orderNumber);

        if (order.getId() == null) {
            throw new IllegalArgumentException("식별자가 없는 잘못된 주문입니다.");
        }

        Map<String, Integer> currentProductNamesCount = order.getOrderProductsAsMap();
        if (currentProductNamesCount.equals(newProductNamesCount)) {
            return;
        }

        //기존 주문의 주문 상품 리스트 비우기
        order.clearOrderProduct();
        em.flush();

        //기존 주문 상품 삭제
        deleteOrderProducts(order);

        //영속성 컨텍스트 정리
        em.clear();

        Order currentOrder = findOrder(orderNumber);
        List<Product> currentProducts = productService.findProducts();

        List<OrderProduct> newOrderProducts = new ArrayList<>();

        for (Map.Entry<String, Integer> newProductNameCountEntry : newProductNamesCount.entrySet()) {
            String newProductName = newProductNameCountEntry.getKey();
            Integer newCount = newProductNameCountEntry.getValue();

            Product newProduct = currentProducts.stream()
                    .filter(p -> p.getName().equals(newProductName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다. name: " + newProductName));
            newOrderProducts.add(OrderProduct.createOrderProduct(newProduct, newCount));
        }

        //새로운 주문 상품으로 변경
        currentOrder.updateOrder(newOrderProducts);
        em.flush();

        //새로운 주문 상품 저장
        registerOrderProducts(newOrderProducts);

        em.clear();
    }

    public Order cancelOrder(String orderNumber) {
        Order order = findOrderWithAllExceptMember(orderNumber);
        order.cancel();
        return order;
    }

    public void deleteOrder(String orderNumber) {
        Order order = findOrderWithDeliveryPayment(orderNumber);
        order.validateDeletable();

        orderProductJdbcRepository.softDeleteOrderProductsByOrderId(order.getId());

        em.clear();
        Order currentOrder = findOrderWithDelivery(orderNumber);
        orderRepository.delete(currentOrder);
    }

    //결제 로직 검증용
    @Transactional(readOnly = true)
    public OrderRequest getOrderRequest(Order order) {
        return OrderRequest.orderChangeRequest(order);
    }

    //주문 리턴용
    @Transactional(readOnly = true)
    public OrderResponse getOrderResponse(Order order) {
        return OrderResponse.orderChangeResponse(order);
    }

    private void registerOrderProducts(List<OrderProduct> orderProducts) {
        orderProductJdbcRepository.saveAll(orderProducts);
    }

    private void deleteOrderProducts(Order order) {
        orderProductJdbcRepository.deleteOrderProductsByOrderId(order.getId());
    }
}
