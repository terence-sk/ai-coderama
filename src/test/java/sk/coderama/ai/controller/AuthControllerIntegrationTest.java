package sk.coderama.ai.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import sk.coderama.ai.BaseIntegrationTest;
import sk.coderama.ai.dto.request.LoginRequest;
import sk.coderama.ai.dto.request.RegisterRequest;
import sk.coderama.ai.dto.response.ErrorResponse;
import sk.coderama.ai.dto.response.JwtResponse;
import sk.coderama.ai.dto.response.UserResponse;

import static org.assertj.core.api.Assertions.assertThat;

class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    void shouldRegisterUserSuccessfully() {
        // Given
        RegisterRequest request = new RegisterRequest(
                "Test User",
                "test@example.com",
                "password123"
        );

        // When
        ResponseEntity<UserResponse> response = restTemplate.postForEntity(
                baseUrl + "/api/auth/register",
                request,
                UserResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getEmail()).isEqualTo("test@example.com");
        assertThat(response.getBody().getName()).isEqualTo("Test User");
        assertThat(response.getBody().getId()).isNotNull();
    }

    @Test
    void shouldLoginWithValidCredentialsAndReturnJWT() {
        // Given - First register a user
        RegisterRequest registerRequest = new RegisterRequest(
                "Login User",
                "login@example.com",
                "password123"
        );
        restTemplate.postForEntity(
                baseUrl + "/api/auth/register",
                registerRequest,
                UserResponse.class
        );

        LoginRequest loginRequest = new LoginRequest("login@example.com", "password123");

        // When
        ResponseEntity<JwtResponse> response = restTemplate.postForEntity(
                baseUrl + "/api/auth/login",
                loginRequest,
                JwtResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isNotNull();
        assertThat(response.getBody().getType()).isEqualTo("Bearer");
        assertThat(response.getBody().getEmail()).isEqualTo("login@example.com");
        assertThat(response.getBody().getUserId()).isNotNull();
    }

    @Test
    void shouldReturn401WhenLoginWithInvalidCredentials() {
        // Given
        LoginRequest loginRequest = new LoginRequest("invalid@example.com", "wrongpassword");

        // When
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                baseUrl + "/api/auth/login",
                loginRequest,
                ErrorResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("Invalid credentials");
    }

    @Test
    void shouldReturn401WhenAccessingProtectedEndpointWithoutToken() {
        // When
        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
                baseUrl + "/api/users",
                ErrorResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldReturn200WhenAccessingProtectedEndpointWithValidToken() {
        // Given - Register and login to get token
        RegisterRequest registerRequest = new RegisterRequest(
                "Protected User",
                "protected@example.com",
                "password123"
        );
        restTemplate.postForEntity(
                baseUrl + "/api/auth/register",
                registerRequest,
                UserResponse.class
        );

        LoginRequest loginRequest = new LoginRequest("protected@example.com", "password123");
        ResponseEntity<JwtResponse> loginResponse = restTemplate.postForEntity(
                baseUrl + "/api/auth/login",
                loginRequest,
                JwtResponse.class
        );

        String token = loginResponse.getBody().getToken();

        // When
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/users",
                HttpMethod.GET,
                entity,
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldReturn400WhenRegisteringWithDuplicateEmail() {
        // Given
        RegisterRequest request = new RegisterRequest(
                "Duplicate User",
                "duplicate@example.com",
                "password123"
        );

        // Register first time
        restTemplate.postForEntity(
                baseUrl + "/api/auth/register",
                request,
                UserResponse.class
        );

        // When - Register again with same email
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                baseUrl + "/api/auth/register",
                request,
                ErrorResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("already exists");
    }

    @Test
    void shouldReturn400WhenRegisteringWithInvalidEmail() {
        // Given
        RegisterRequest request = new RegisterRequest(
                "Invalid Email User",
                "not-an-email",
                "password123"
        );

        // When
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                baseUrl + "/api/auth/register",
                request,
                ErrorResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrors()).isNotEmpty();
    }
}
