package sk.coderama.ai.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import sk.coderama.ai.BaseIntegrationTest;
import sk.coderama.ai.dto.request.LoginRequest;
import sk.coderama.ai.dto.request.RegisterRequest;
import sk.coderama.ai.dto.response.ErrorResponse;
import sk.coderama.ai.dto.response.JwtResponse;
import sk.coderama.ai.dto.response.UserResponse;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityIntegrationTest extends BaseIntegrationTest {

    private String validToken;

    @BeforeEach
    void setUpAuth() {
        RegisterRequest registerRequest = new RegisterRequest(
                "Security Test User",
                "security@test.com",
                "password123"
        );
        restTemplate.postForEntity(
                baseUrl + "/api/auth/register",
                registerRequest,
                UserResponse.class
        );

        LoginRequest loginRequest = new LoginRequest("security@test.com", "password123");
        ResponseEntity<JwtResponse> loginResponse = restTemplate.postForEntity(
                baseUrl + "/api/auth/login",
                loginRequest,
                JwtResponse.class
        );

        validToken = loginResponse.getBody().getToken();
    }

    @Test
    void shouldAllowAccessToPublicEndpoints() {
        // Register endpoint
        ResponseEntity<String> registerResponse = restTemplate.getForEntity(
                baseUrl + "/api/auth/register",
                String.class
        );
        // Should not be 401 (actual response may be 405 for GET on POST endpoint)
        assertThat(registerResponse.getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);

        // Login endpoint
        ResponseEntity<String> loginResponse = restTemplate.getForEntity(
                baseUrl + "/api/auth/login",
                String.class
        );
        assertThat(loginResponse.getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldDenyAccessToProtectedEndpointsWithoutToken() {
        // Users endpoint
        ResponseEntity<ErrorResponse> usersResponse = restTemplate.getForEntity(
                baseUrl + "/api/users",
                ErrorResponse.class
        );
        assertThat(usersResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // Products endpoint
        ResponseEntity<ErrorResponse> productsResponse = restTemplate.getForEntity(
                baseUrl + "/api/products",
                ErrorResponse.class
        );
        assertThat(productsResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // Orders endpoint
        ResponseEntity<ErrorResponse> ordersResponse = restTemplate.getForEntity(
                baseUrl + "/api/orders",
                ErrorResponse.class
        );
        assertThat(ordersResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldAllowAccessToProtectedEndpointsWithValidToken() {
        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + validToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        // Users endpoint
        ResponseEntity<String> usersResponse = restTemplate.exchange(
                baseUrl + "/api/users",
                HttpMethod.GET,
                entity,
                String.class
        );
        assertThat(usersResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Products endpoint
        ResponseEntity<String> productsResponse = restTemplate.exchange(
                baseUrl + "/api/products",
                HttpMethod.GET,
                entity,
                String.class
        );
        assertThat(productsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Orders endpoint
        ResponseEntity<String> ordersResponse = restTemplate.exchange(
                baseUrl + "/api/orders",
                HttpMethod.GET,
                entity,
                String.class
        );
        assertThat(ordersResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldDenyAccessWithInvalidToken() {
        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer invalid.token.here");
        HttpEntity<?> entity = new HttpEntity<>(headers);

        // When
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                baseUrl + "/api/users",
                HttpMethod.GET,
                entity,
                ErrorResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldDenyAccessWithMalformedAuthorizationHeader() {
        // Given - Missing "Bearer" prefix
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", validToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        // When
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                baseUrl + "/api/users",
                HttpMethod.GET,
                entity,
                ErrorResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldValidateTokenCorrectly() {
        // Test that JwtTokenProvider validates a valid token
        boolean isValid = jwtTokenProvider.validateToken(validToken);
        assertThat(isValid).isTrue();

        // Test that JwtTokenProvider rejects an invalid token
        boolean isInvalid = jwtTokenProvider.validateToken("invalid.token");
        assertThat(isInvalid).isFalse();
    }

    @Test
    void shouldExtractEmailFromToken() {
        // Given a valid token
        // When extracting email
        String email = jwtTokenProvider.getEmailFromToken(validToken);

        // Then
        assertThat(email).isEqualTo("security@test.com");
    }

    @Test
    void shouldAllowAccessToSwaggerEndpoints() {
        // Swagger UI should be accessible without authentication
        ResponseEntity<String> swaggerResponse = restTemplate.getForEntity(
                baseUrl + "/swagger-ui.html",
                String.class
        );

        // Should not be 401 (may redirect to swagger-ui/index.html)
        assertThat(swaggerResponse.getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
