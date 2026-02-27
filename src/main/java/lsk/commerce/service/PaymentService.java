package lsk.commerce.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.portone.sdk.server.payment.PaidPayment;
import lombok.RequiredArgsConstructor;
import lsk.commerce.api.portone.CompletePaymentRequest;
import lsk.commerce.api.portone.PaymentCustomData;
import lsk.commerce.api.portone.SyncPaymentException;
import lsk.commerce.domain.Order;
import lsk.commerce.domain.Payment;
import lsk.commerce.domain.Product;
import lsk.commerce.dto.OrderProductDto;
import lsk.commerce.dto.request.OrderRequest;
import lsk.commerce.dto.request.PaymentRequest;
import lsk.commerce.event.PaymentCompletedEvent;
import lsk.commerce.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    private final ObjectMapper objectMapper;
    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final ProductService productService;
    private final ApplicationEventPublisher eventPublisher;

    //이후 결제 진행은 다른 비즈니스 로직 다 생성 후 진행
    public Order request(String orderNumber) {
        Order order = orderService.findOrderWithAllExceptMember(orderNumber);
        Payment.requestPayment(order);
        new CompletePaymentRequest(order.getPayment().getPaymentId());
        paymentRepository.save(order.getPayment());
        return order;
    }

    @Transactional(readOnly = true)
    public Payment findPaymentByPaymentId(String paymentId) {
        return paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 결제 번호입니다."));
    }

    @Transactional(readOnly = true)
    public Payment findPaymentWithOrderDelivery(String paymentId) {
        return paymentRepository.findWithOrderDelivery(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 결제 번호입니다."));
    }

    protected Payment verifyAndComplete(PaidPayment paidPayment) {
        if (!this.verifyPayment(paidPayment)) {
            throw new SyncPaymentException();
        }

        logger.info("결제 성공 {}", paidPayment);

        return this.completePayment(paidPayment);
    }

    public Payment failedPayment(String paymentId) {
        Payment payment = findPaymentByPaymentId(paymentId);
        payment.failed();
        return payment;
    }

    @Transactional(readOnly = true)
    public PaymentRequest getPaymentRequest(Payment payment) {
        return PaymentRequest.paymentChangeDto(payment);
    }

    private boolean verifyPayment(PaidPayment paidPayment) {
        PaymentCustomData customDataDecoded = getPaymentCustomData(paidPayment);
        if (customDataDecoded == null) return false;

        OrderRequest orderRequest = verifyOrderProducts(customDataDecoded.orderNumber());

        return verifyPriceAndOrderName(paidPayment, orderRequest);
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

    private OrderRequest verifyOrderProducts(String orderNumber) {
        Order order = orderService.findOrderWithAllExceptMember(orderNumber);
        OrderRequest orderRequest = orderService.getOrderRequest(order);
        List<Product> products = productService.findProducts();

        if (orderRequest.getOrderProducts().isEmpty()) {
            throw new IllegalArgumentException("주문 상품이 비어 있습니다.");
        }

        for (OrderProductDto orderProduct : orderRequest.getOrderProducts()) {
            if (products.stream().noneMatch(p -> p.getName().equals(orderProduct.getName()))) {
                throw new IllegalArgumentException("잘못된 상품이 있습니다.");
            }
        }

        return orderRequest;
    }

    private boolean verifyPriceAndOrderName(PaidPayment paidPayment, OrderRequest orderRequest) {
        if (paidPayment.getAmount().getTotal() != orderRequest.getTotalAmount().longValue()) {
            return false;
        }

        if (orderRequest.getOrderProducts().size() == 1) {
            return paidPayment.getOrderName().equals(orderRequest.getOrderProducts().getFirst().getName());
        } else {
            return paidPayment.getOrderName().equals(orderRequest.getOrderProducts().getFirst().getName() + " 외 " + (orderRequest.getOrderProducts().size() - 1) + "건");
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
