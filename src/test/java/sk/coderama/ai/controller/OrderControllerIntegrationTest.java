package sk.coderama.ai.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import sk.coderama.ai.BaseIntegrationTest;
import sk.coderama.ai.dto.request.*;
import sk.coderama.ai.dto.response.*;
import sk.coderama.ai.entity.OrderStatus;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderControllerIntegrationTest extends BaseIntegrationTest {

    private String authToken;
    private Long testUserId;
    private Long testProductId;

    @BeforeEach
    void setUpAuth() {
        // Register and login
        RegisterRequest registerRequest = new RegisterRequest(
                "Order Admin",
                "orderadmin@test.com",
                "password123"
        );
        ResponseEntity<UserResponse> registerResponse = restTemplate.postForEntity(
                baseUrl + "/api/auth/register",
                registerRequest,
                UserResponse.class
        );
        testUserId = registerResponse.getBody().getId();

        LoginRequest loginRequest = new LoginRequest("orderadmin@test.com", "password123");
        ResponseEntity<JwtResponse> loginResponse = restTemplate.postForEntity(
                baseUrl + "/api/auth/login",
                loginRequest,
                JwtResponse.class
        );
        authToken = loginResponse.getBody().getToken();

        // Create a test product
        CreateProductRequest productRequest = new CreateProductRequest(
                "Order Test Product",
                "For testing orders",
                BigDecimal.valueOf(25.00),
                100
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authToken);
        HttpEntity<CreateProductRequest> productEntity = new HttpEntity<>(productRequest, headers);

        ResponseEntity<ProductResponse> productResponse = restTemplate.exchange(
                baseUrl + "/api/products",
                HttpMethod.POST,
                productEntity,
                ProductResponse.class
        );
        testProductId = productResponse.getBody().getId();
    }

    @Test
    void shouldCreateOrderWithItems() {
        // Given
        OrderItemRequest itemRequest = new OrderItemRequest(
                testProductId,
                2,
                BigDecimal.valueOf(25.00)
        );

        CreateOrderRequest request = new CreateOrderRequest(
                testUserId,
                BigDecimal.valueOf(50.00),
                OrderStatus.PENDING,
                Collections.singletonList(itemRequest)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authToken);
        HttpEntity<CreateOrderRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<OrderResponse> response = restTemplate.exchange(
                baseUrl + "/api/orders",
                HttpMethod.POST,
                entity,
                OrderResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUserId()).isEqualTo(testUserId);
        assertThat(response.getBody().getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getBody().getItems()).hasSize(1);
        assertThat(response.getBody().getTotal()).isEqualByComparingTo(BigDecimal.valueOf(50.00));
    }

    @Test
    void shouldGetOrderWithItems() {
        // Given - Create an order first
        OrderItemRequest itemRequest = new OrderItemRequest(
                testProductId,
                1,
                BigDecimal.valueOf(25.00)
        );

        CreateOrderRequest createRequest = new CreateOrderRequest(
                testUserId,
                BigDecimal.valueOf(25.00),
                OrderStatus.PENDING,
                Collections.singletonList(itemRequest)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authToken);
        HttpEntity<CreateOrderRequest> createEntity = new HttpEntity<>(createRequest, headers);

        ResponseEntity<OrderResponse> createResponse = restTemplate.exchange(
                baseUrl + "/api/orders",
                HttpMethod.POST,
                createEntity,
                OrderResponse.class
        );

        Long orderId = createResponse.getBody().getId();

        // When
        HttpEntity<?> getEntity = new HttpEntity<>(headers);
        ResponseEntity<OrderResponse> response = restTemplate.exchange(
                baseUrl + "/api/orders/" + orderId,
                HttpMethod.GET,
                getEntity,
                OrderResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(orderId);
        assertThat(response.getBody().getItems()).isNotEmpty();
    }

    @Test
    void shouldGetOrdersByUserId() {
        // Given - Create an order
        OrderItemRequest itemRequest = new OrderItemRequest(
                testProductId,
                1,
                BigDecimal.valueOf(25.00)
        );

        CreateOrderRequest createRequest = new CreateOrderRequest(
                testUserId,
                BigDecimal.valueOf(25.00),
                OrderStatus.PENDING,
                Collections.singletonList(itemRequest)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authToken);
        HttpEntity<CreateOrderRequest> createEntity = new HttpEntity<>(createRequest, headers);

        restTemplate.exchange(
                baseUrl + "/api/orders",
                HttpMethod.POST,
                createEntity,
                OrderResponse.class
        );

        // When
        HttpEntity<?> getEntity = new HttpEntity<>(headers);
        ResponseEntity<List<OrderResponse>> response = restTemplate.exchange(
                baseUrl + "/api/orders/user/" + testUserId,
                HttpMethod.GET,
                getEntity,
                new ParameterizedTypeReference<List<OrderResponse>>() {}
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isNotEmpty();
    }

    @Test
    void shouldReturn400WhenCreatingOrderWithEmptyItems() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest(
                testUserId,
                BigDecimal.valueOf(0.00),
                OrderStatus.PENDING,
                Collections.emptyList()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authToken);
        HttpEntity<CreateOrderRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                baseUrl + "/api/orders",
                HttpMethod.POST,
                entity,
                ErrorResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrors()).isNotEmpty();
    }

    @Test
    void shouldReturn400WhenCreatingOrderWithInvalidQuantity() {
        // Given
        OrderItemRequest itemRequest = new OrderItemRequest(
                testProductId,
                0,  // Invalid quantity
                BigDecimal.valueOf(25.00)
        );

        CreateOrderRequest request = new CreateOrderRequest(
                testUserId,
                BigDecimal.valueOf(0.00),
                OrderStatus.PENDING,
                Collections.singletonList(itemRequest)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authToken);
        HttpEntity<CreateOrderRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                baseUrl + "/api/orders",
                HttpMethod.POST,
                entity,
                ErrorResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrors()).isNotEmpty();
    }

    @Test
    void shouldReturn404WhenCreatingOrderWithNonExistentProduct() {
        // Given
        OrderItemRequest itemRequest = new OrderItemRequest(
                99999L,  // Non-existent product
                1,
                BigDecimal.valueOf(25.00)
        );

        CreateOrderRequest request = new CreateOrderRequest(
                testUserId,
                BigDecimal.valueOf(25.00),
                OrderStatus.PENDING,
                Collections.singletonList(itemRequest)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authToken);
        HttpEntity<CreateOrderRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                baseUrl + "/api/orders",
                HttpMethod.POST,
                entity,
                ErrorResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldUpdateOrderStatus() {
        // Given - Create an order first
        OrderItemRequest itemRequest = new OrderItemRequest(
                testProductId,
                1,
                BigDecimal.valueOf(25.00)
        );

        CreateOrderRequest createRequest = new CreateOrderRequest(
                testUserId,
                BigDecimal.valueOf(25.00),
                OrderStatus.PENDING,
                Collections.singletonList(itemRequest)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authToken);
        HttpEntity<CreateOrderRequest> createEntity = new HttpEntity<>(createRequest, headers);

        ResponseEntity<OrderResponse> createResponse = restTemplate.exchange(
                baseUrl + "/api/orders",
                HttpMethod.POST,
                createEntity,
                OrderResponse.class
        );

        Long orderId = createResponse.getBody().getId();

        // Update request
        UpdateOrderRequest updateRequest = new UpdateOrderRequest();
        updateRequest.setStatus(OrderStatus.COMPLETED);

        HttpEntity<UpdateOrderRequest> updateEntity = new HttpEntity<>(updateRequest, headers);

        // When
        ResponseEntity<OrderResponse> response = restTemplate.exchange(
                baseUrl + "/api/orders/" + orderId,
                HttpMethod.PUT,
                updateEntity,
                OrderResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }
}
