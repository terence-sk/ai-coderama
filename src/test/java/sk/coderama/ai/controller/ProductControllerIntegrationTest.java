package sk.coderama.ai.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import sk.coderama.ai.BaseIntegrationTest;
import sk.coderama.ai.dto.request.CreateProductRequest;
import sk.coderama.ai.dto.request.LoginRequest;
import sk.coderama.ai.dto.request.RegisterRequest;
import sk.coderama.ai.dto.request.UpdateProductRequest;
import sk.coderama.ai.dto.response.ErrorResponse;
import sk.coderama.ai.dto.response.JwtResponse;
import sk.coderama.ai.dto.response.ProductResponse;
import sk.coderama.ai.dto.response.UserResponse;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductControllerIntegrationTest extends BaseIntegrationTest {

    private String authToken;

    @BeforeEach
    void setUpAuth() {
        RegisterRequest registerRequest = new RegisterRequest(
                "Product Admin",
                "productadmin@test.com",
                "password123"
        );
        restTemplate.postForEntity(
                baseUrl + "/api/auth/register",
                registerRequest,
                UserResponse.class
        );

        LoginRequest loginRequest = new LoginRequest("productadmin@test.com", "password123");
        ResponseEntity<JwtResponse> loginResponse = restTemplate.postForEntity(
                baseUrl + "/api/auth/login",
                loginRequest,
                JwtResponse.class
        );

        authToken = loginResponse.getBody().getToken();
    }

    @Test
    void shouldGetAllProducts() {
        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        // When
        ResponseEntity<List<ProductResponse>> response = restTemplate.exchange(
                baseUrl + "/api/products",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<ProductResponse>>() {}
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void shouldCreateProduct() {
        // Given
        CreateProductRequest request = new CreateProductRequest(
                "Test Product",
                "Test Description",
                BigDecimal.valueOf(99.99),
                100
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authToken);
        HttpEntity<CreateProductRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<ProductResponse> response = restTemplate.exchange(
                baseUrl + "/api/products",
                HttpMethod.POST,
                entity,
                ProductResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("Test Product");
        assertThat(response.getBody().getPrice()).isEqualByComparingTo(BigDecimal.valueOf(99.99));
        assertThat(response.getBody().getStock()).isEqualTo(100);
    }

    @Test
    void shouldUpdateProduct() {
        // Given - Create a product first
        CreateProductRequest createRequest = new CreateProductRequest(
                "Original Product",
                "Original Description",
                BigDecimal.valueOf(50.00),
                50
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authToken);
        HttpEntity<CreateProductRequest> createEntity = new HttpEntity<>(createRequest, headers);

        ResponseEntity<ProductResponse> createResponse = restTemplate.exchange(
                baseUrl + "/api/products",
                HttpMethod.POST,
                createEntity,
                ProductResponse.class
        );

        Long productId = createResponse.getBody().getId();

        // Update request
        UpdateProductRequest updateRequest = new UpdateProductRequest();
        updateRequest.setName("Updated Product");
        updateRequest.setPrice(BigDecimal.valueOf(75.00));

        HttpEntity<UpdateProductRequest> updateEntity = new HttpEntity<>(updateRequest, headers);

        // When
        ResponseEntity<ProductResponse> response = restTemplate.exchange(
                baseUrl + "/api/products/" + productId,
                HttpMethod.PUT,
                updateEntity,
                ProductResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("Updated Product");
        assertThat(response.getBody().getPrice()).isEqualByComparingTo(BigDecimal.valueOf(75.00));
    }

    @Test
    void shouldDeleteProduct() {
        // Given - Create a product first
        CreateProductRequest createRequest = new CreateProductRequest(
                "Delete Product",
                "To be deleted",
                BigDecimal.valueOf(10.00),
                10
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authToken);
        HttpEntity<CreateProductRequest> createEntity = new HttpEntity<>(createRequest, headers);

        ResponseEntity<ProductResponse> createResponse = restTemplate.exchange(
                baseUrl + "/api/products",
                HttpMethod.POST,
                createEntity,
                ProductResponse.class
        );

        Long productId = createResponse.getBody().getId();

        // When
        HttpEntity<?> deleteEntity = new HttpEntity<>(headers);
        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl + "/api/products/" + productId,
                HttpMethod.DELETE,
                deleteEntity,
                Void.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void shouldReturn400WhenCreatingProductWithNegativePrice() {
        // Given
        CreateProductRequest request = new CreateProductRequest(
                "Invalid Product",
                "Has negative price",
                BigDecimal.valueOf(-10.00),
                100
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authToken);
        HttpEntity<CreateProductRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                baseUrl + "/api/products",
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
    void shouldReturn400WhenCreatingProductWithNegativeStock() {
        // Given
        CreateProductRequest request = new CreateProductRequest(
                "Invalid Stock Product",
                "Has negative stock",
                BigDecimal.valueOf(10.00),
                -5
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authToken);
        HttpEntity<CreateProductRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                baseUrl + "/api/products",
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
    void shouldReturn404WhenGettingNonExistentProduct() {
        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        // When
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                baseUrl + "/api/products/99999",
                HttpMethod.GET,
                entity,
                ErrorResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
