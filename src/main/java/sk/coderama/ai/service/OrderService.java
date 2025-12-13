package sk.coderama.ai.service;

import sk.coderama.ai.dto.request.CreateOrderRequest;
import sk.coderama.ai.dto.request.UpdateOrderRequest;
import sk.coderama.ai.dto.response.OrderResponse;

import java.util.List;

public interface OrderService {

    List<OrderResponse> getAllOrders();

    OrderResponse getOrderById(Long id);

    OrderResponse createOrder(CreateOrderRequest request);

    OrderResponse updateOrder(Long id, UpdateOrderRequest request);

    void deleteOrder(Long id);

    List<OrderResponse> getOrdersByUserId(Long userId);
}
