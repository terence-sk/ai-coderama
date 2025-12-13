package sk.coderama.ai.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sk.coderama.ai.dto.request.CreateOrderRequest;
import sk.coderama.ai.dto.request.OrderItemRequest;
import sk.coderama.ai.dto.request.UpdateOrderRequest;
import sk.coderama.ai.dto.response.OrderItemResponse;
import sk.coderama.ai.dto.response.OrderResponse;
import sk.coderama.ai.entity.Order;
import sk.coderama.ai.entity.OrderItem;
import sk.coderama.ai.entity.Product;
import sk.coderama.ai.exception.ResourceNotFoundException;
import sk.coderama.ai.repository.OrderRepository;
import sk.coderama.ai.repository.ProductRepository;
import sk.coderama.ai.repository.UserRepository;
import sk.coderama.ai.service.OrderService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Override
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        return mapToResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        // Validate user exists
        if (!userRepository.existsById(request.getUserId())) {
            throw new ResourceNotFoundException("User", "id", request.getUserId());
        }

        // Validate all products exist
        for (OrderItemRequest itemRequest : request.getItems()) {
            if (!productRepository.existsById(itemRequest.getProductId())) {
                throw new ResourceNotFoundException("Product", "id", itemRequest.getProductId());
            }
        }

        // Create and persist the order first to obtain its ID
        Order order = Order.builder()
                .userId(request.getUserId())
                .total(request.getTotal())
                .status(request.getStatus())
                .items(new ArrayList<>())
                .build();

        order = orderRepository.save(order);

        // Create order items with the generated order ID
        for (OrderItemRequest itemRequest : request.getItems()) {
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .orderId(order.getId())
                    .productId(itemRequest.getProductId())
                    .quantity(itemRequest.getQuantity())
                    .price(itemRequest.getPrice())
                    .build();
            order.getItems().add(orderItem);
        }

        // Calculate total from items
        BigDecimal calculatedTotal = order.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotal(calculatedTotal);

        Order savedOrder = orderRepository.save(order);
        return mapToResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse updateOrder(Long id, UpdateOrderRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));

        if (request.getStatus() != null) {
            order.setStatus(request.getStatus());
        }

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            // Validate all products exist
            for (OrderItemRequest itemRequest : request.getItems()) {
                if (!productRepository.existsById(itemRequest.getProductId())) {
                    throw new ResourceNotFoundException("Product", "id", itemRequest.getProductId());
                }
            }

            // Clear existing items and add new ones
            order.getItems().clear();

            for (OrderItemRequest itemRequest : request.getItems()) {
                OrderItem orderItem = OrderItem.builder()
                        .order(order)
                        .orderId(order.getId())
                        .productId(itemRequest.getProductId())
                        .quantity(itemRequest.getQuantity())
                        .price(itemRequest.getPrice())
                        .build();
                order.getItems().add(orderItem);
            }

            // Recalculate total
            BigDecimal calculatedTotal = order.getItems().stream()
                    .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            order.setTotal(calculatedTotal);
        } else if (request.getTotal() != null) {
            order.setTotal(request.getTotal());
        }

        Order updatedOrder = orderRepository.save(order);
        return mapToResponse(updatedOrder);
    }

    @Override
    @Transactional
    public void deleteOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        orderRepository.delete(order);
    }

    @Override
    public List<OrderResponse> getOrdersByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }
        return orderRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(this::mapItemToResponse)
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .total(order.getTotal())
                .status(order.getStatus())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private OrderItemResponse mapItemToResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .orderId(item.getOrderId())
                .productId(item.getProductId())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .createdAt(item.getCreatedAt())
                .build();
    }
}
