package sk.coderama.ai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sk.coderama.ai.entity.Order;
import sk.coderama.ai.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserId(Long userId);

    List<Order> findByStatusInAndCreatedAtBefore(List<OrderStatus> statuses, LocalDateTime createdAtBefore);
}
