package lsk.commerce.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.portone.sdk.server.payment.PaidPayment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lsk.commerce.api.portone.PaymentCustomData;
import lsk.commerce.api.portone.SyncPaymentException;
import lsk.commerce.domain.Order;
import lsk.commerce.domain.Payment;
import lsk.commerce.domain.Product;
import lsk.commerce.dto.OrderProductDto;
import lsk.commerce.dto.request.PaymentCompleteResponse;
import lsk.commerce.dto.response.OrderPaymentResponse;
import lsk.commerce.dto.response.PaymentResponse;
import lsk.commerce.event.PaymentCompletedEvent;
import lsk.commerce.repository.PaymentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PaymentService {

    private final ObjectMapper objectMapper;
    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final ProductService productService;
    private final ApplicationEventPublisher eventPublisher;

    public Payment request(String orderNumber, String loginId) {
        Order order = orderService.findOrderWithDeliveryPayment(orderNumber);
        order.isOwner(loginId);
        Payment.requestPayment(order);
        paymentRepository.save(order.getPayment());
        return order.getPayment();
    }

    @Transactional(readOnly = true)
    public Payment findPaymentByPaymentId(String paymentId) {
        return paymentRepository.findWithOrder(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 결제 번호입니다"));
    }

    @Transactional(readOnly = true)
    public Payment findPaymentWithOrderDelivery(String paymentId) {
        return paymentRepository.findWithOrderDelivery(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 결제 번호입니다"));
    }

    protected Payment verifyAndComplete(PaidPayment paidPayment, String loginId) {
        if (!this.verifyPayment(paidPayment, loginId)) {
            throw new SyncPaymentException("결제 정보 검증 중 오류 발생");
        }

        log.info("결제 성공 {}", paidPayment);

        return this.completePayment(paidPayment);
    }

    public Payment failedPayment(String paymentId) {
        Payment payment = findPaymentByPaymentId(paymentId);
        payment.failed();
        return payment;
    }

    @Transactional(readOnly = true)
    public PaymentCompleteResponse getPaymentCompleteResponse(Payment payment) {
        return PaymentCompleteResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentResponse(Payment payment) {
        return PaymentResponse.from(payment);
    }

    private boolean verifyPayment(PaidPayment paidPayment, String loginId) {
        PaymentCustomData customDataDecoded = getPaymentCustomData(paidPayment);
        if (customDataDecoded == null) return false;

        OrderPaymentResponse response = verifyOrderProducts(customDataDecoded.orderNumber(), loginId);

        return verifyPriceAndOrderName(paidPayment, response);
    }

    private PaymentCustomData getPaymentCustomData(PaidPayment paidPayment) {
        var customData = paidPayment.getCustomData();
        if (customData == null) return null;

        PaymentCustomData customDataDecoded;
        try {
            customDataDecoded = objectMapper.readValue(customData, PaymentCustomData.class);
        } catch (JsonProcessingException e) {
            return null;
        }
        return customDataDecoded;
    }

    private OrderPaymentResponse verifyOrderProducts(String orderNumber, String loginId) {
        Order order = orderService.findOrderWithAllExceptMember(orderNumber);
        order.isOwner(loginId);
        order.validateReadyToPay();
        OrderPaymentResponse orderPaymentResponse = orderService.getOrderPaymentResponse(order);
        List<Product> products = productService.findProducts();

        if (orderPaymentResponse.orderProductDtoList().isEmpty()) {
            throw new IllegalArgumentException("주문 상품이 비어 있습니다");
        }

        for (OrderProductDto orderProduct : orderPaymentResponse.orderProductDtoList()) {
            if (products.stream().noneMatch(p -> p.getName().equals(orderProduct.name()))) {
                throw new IllegalArgumentException("잘못된 상품이 있습니다");
            }
        }

        return orderPaymentResponse;
    }

    private boolean verifyPriceAndOrderName(PaidPayment paidPayment, OrderPaymentResponse orderPaymentResponse) {
        if (paidPayment.getAmount().getTotal() != orderPaymentResponse.totalAmount().longValue()) {
            return false;
        }

        if (orderPaymentResponse.orderProductDtoList().size() == 1) {
            return paidPayment.getOrderName().equals(orderPaymentResponse.orderProductDtoList().getFirst().name());
        } else {
            return paidPayment.getOrderName().equals(orderPaymentResponse.orderProductDtoList().getFirst().name() + " 외 " + (orderPaymentResponse.orderProductDtoList().size() - 1) + "건");
        }
    }

    private Payment completePayment(PaidPayment paidPayment) {
        Payment payment = findPaymentWithOrderDelivery(paidPayment.getId());
        LocalDateTime paymentDate = LocalDateTime.ofInstant(paidPayment.getPaidAt(), ZoneId.of("Asia/Seoul"));
        payment.complete(paymentDate);
        payment.getOrder().completePaid();

        String orderNumber = payment.getOrder().getOrderNumber();
        eventPublisher.publishEvent(new PaymentCompletedEvent(orderNumber));

        return payment;
    }
}
