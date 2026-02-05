package lsk.commerce.repository;

import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.OrderProduct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderProductJdbcRepository {

    private final JdbcTemplate jdbcTemplate;
    private static final int BATCH_SIZE = 100;

    public void saveAll(List<OrderProduct> orderProducts) {
        if (orderProducts.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO order_product (order_id, product_id, count, order_price, deleted)" +
                " VALUES (?, ?, ?, ?, ?)";

        for (int i = 0; i < orderProducts.size(); i += BATCH_SIZE) {
            List<OrderProduct> batchList = orderProducts.subList(i, Math.min(i + BATCH_SIZE, orderProducts.size()));

            jdbcTemplate.batchUpdate(sql, batchList, batchList.size(), (ps, orderProduct) -> {
                ps.setLong(1, orderProduct.getOrder().getId());
                ps.setLong(2, orderProduct.getProduct().getId());
                ps.setInt(3, orderProduct.getCount());
                ps.setInt(4, orderProduct.getOrderPrice());
                ps.setBoolean(5, orderProduct.isDeleted());
            });
        }
    }

    public void deleteOrderProductsByOrderId(Long orderId) {
        String sql = "DELETE FROM order_product WHERE order_id = ?";

        jdbcTemplate.update(sql, orderId);
    }

    public void softDeleteOrderProductsByOrderId(Long orderId) {
        String sql = "UPDATE order_product SET deleted = true WHERE order_id = ?";

        jdbcTemplate.update(sql, orderId);
    }
}
