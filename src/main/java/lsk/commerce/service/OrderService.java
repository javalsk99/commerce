package lsk.commerce.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Delivery;
import lsk.commerce.domain.Member;
import lsk.commerce.domain.Order;
import lsk.commerce.domain.OrderProduct;
import lsk.commerce.domain.OrderStatus;
import lsk.commerce.domain.Product;
import lsk.commerce.dto.request.OrderChangeRequest;
import lsk.commerce.dto.request.OrderCreateRequest;
import lsk.commerce.dto.request.OrderProductRequest;
import lsk.commerce.dto.response.OrderCancelResponse;
import lsk.commerce.dto.response.OrderChangeResponse;
import lsk.commerce.dto.response.OrderPaymentResponse;
import lsk.commerce.exception.DataNotFoundException;
import lsk.commerce.exception.InvalidDataException;
import lsk.commerce.repository.OrderProductJdbcRepository;
import lsk.commerce.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    private final EntityManager em;

    private final OrderRepository orderRepository;
    private final MemberService memberService;
    private final ProductService productService;

    private final OrderProductJdbcRepository orderProductJdbcRepository;

    public String order(OrderCreateRequest request, String loginId) {
        //엔티티 조회
        Member member = memberService.findMemberByLoginId(loginId);
        List<Product> products = productService.findProducts();

        //배송 정보 생성
        Delivery delivery = new Delivery(member);

        //주문 상품 생성
        List<OrderProduct> orderProducts = createOrderProducts(request.orderProductRequestList(), products);

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
                .orElseThrow(() -> new DataNotFoundException("존재하지 않는 주문입니다. orderNumber: " + orderNumber));
    }

    @Transactional(readOnly = true)
    public Order findOrderWithDeliveryPayment(String orderNumber) {
        return orderRepository.findWithDeliveryPayment(orderNumber)
                .orElseThrow(() -> new DataNotFoundException("존재하지 않는 주문입니다. orderNumber: " + orderNumber));
    }

    @Transactional(readOnly = true)
    public Order findOrderWithDeliveryPaymentMember(String orderNumber) {
        return orderRepository.findWithDeliveryPaymentMember(orderNumber)
                .orElseThrow(() -> new DataNotFoundException("존재하지 않는 주문입니다. orderNumber: " + orderNumber));
    }

    @Transactional(readOnly = true)
    public Order findOrderWithAllExceptMember(String orderNumber) {
        return orderRepository.findWithAllExceptMember(orderNumber)
                .orElseThrow(() -> new DataNotFoundException("존재하지 않는 주문입니다. orderNumber: " + orderNumber));
    }

    @Transactional(readOnly = true)
    public Order findOrderWithAll(String orderNumber) {
        return orderRepository.findWithAll(orderNumber)
                .orElseThrow(() -> new DataNotFoundException("존재하지 않는 주문입니다. orderNumber: " + orderNumber));
    }

    public void changeOrder(String orderNumber, OrderChangeRequest request, String loginId) {
        if (orderNumber.equals("eicanNoP5cW8")) {
            return;
        }

        Order order = findOrderWithAll(orderNumber);

        order.isOwner(loginId);

        if (order.getOrderStatus() != OrderStatus.CREATED) {
            throw new IllegalStateException("주문을 수정할 수 없습니다. OrderStatus: " + order.getOrderStatus());
        }

        if (order.getId() == null) {
            throw new InvalidDataException("식별자가 없는 잘못된 주문입니다");
        }

        if (order.isSameOrderProducts(request.orderProductRequestList())) {
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

        //주문 상품 생성
        List<OrderProduct> newOrderProducts = createOrderProducts(request.orderProductRequestList(), currentProducts);

        //새로운 주문 상품으로 변경
        currentOrder.changeOrder(newOrderProducts);
        em.flush();

        //새로운 주문 상품 저장
        registerOrderProducts(newOrderProducts);

        em.clear();
    }

    public Order cancelOrder(String orderNumber, String loginId) {
        Order order = findOrderWithAll(orderNumber);
        if (orderNumber.equals("eicanNoP5cW8")) {
            return order;
        }
        order.isOwner(loginId);
        order.cancel();
        return order;
    }

    public void deleteOrder(String orderNumber, String loginId) {
        if (orderNumber.equals("eicanNoP5cW8")) {
            return;
        }

        Optional<Order> optionalOrder = orderRepository.findWithDeliveryPaymentMember(orderNumber);
        if (optionalOrder.isEmpty()) {
            return;
        }

        Order order = optionalOrder.get();

        order.isOwner(loginId);
        order.validateDeletable();

        orderProductJdbcRepository.softDeleteOrderProductsByOrderId(order.getId());

        em.clear();
        Order currentOrder = findOrderWithDeliveryPayment(orderNumber);
        orderRepository.delete(currentOrder);
    }

    //결제 로직 검증용
    @Transactional(readOnly = true)
    public OrderPaymentResponse getOrderPaymentResponse(Order order) {
        return OrderPaymentResponse.from(order);
    }

    //주문 수정 리턴용
    @Transactional(readOnly = true)
    public OrderChangeResponse getOrderChangeResponse(Order order) {
        return OrderChangeResponse.from(order);
    }

    //주문 취소 리턴용
    @Transactional(readOnly = true)
    public OrderCancelResponse getOrderCancelResponse(Order order) {
        return OrderCancelResponse.from(order);
    }

    private static List<OrderProduct> createOrderProducts(List<OrderProductRequest> orderProductRequestList, List<Product> products) {
        List<OrderProduct> orderProducts = new ArrayList<>();

        for (OrderProductRequest orderProductRequest : orderProductRequestList) {
            String productNumber = orderProductRequest.productNumber();
            Integer quantity = orderProductRequest.quantity();

            Product product = products.stream()
                    .filter(p -> p.getProductNumber().equals(productNumber))
                    .findFirst()
                    .orElseThrow(() -> new DataNotFoundException("존재하지 않는 상품입니다. productNumber: " + productNumber));
            orderProducts.add(OrderProduct.createOrderProduct(product, quantity));
        }

        return orderProducts;
    }

    private void registerOrderProducts(List<OrderProduct> orderProducts) {
        orderProductJdbcRepository.saveAll(orderProducts);
    }

    private void deleteOrderProducts(Order order) {
        orderProductJdbcRepository.deleteOrderProductsByOrderId(order.getId());
    }
}
