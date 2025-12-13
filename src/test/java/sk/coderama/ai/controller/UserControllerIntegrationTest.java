package sk.coderama.ai.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import sk.coderama.ai.BaseIntegrationTest;
import sk.coderama.ai.dto.request.CreateUserRequest;
import sk.coderama.ai.dto.request.LoginRequest;
import sk.coderama.ai.dto.request.RegisterRequest;
import sk.coderama.ai.dto.request.UpdateUserRequest;
import sk.coderama.ai.dto.response.ErrorResponse;
import sk.coderama.ai.dto.response.JwtResponse;
import sk.coderama.ai.dto.response.UserResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserControllerIntegrationTest extends BaseIntegrationTest {

    private String authToken;

    @BeforeEach
    void setUpAuth() {
        // Register and login to get auth token
        RegisterRequest registerRequest = new RegisterRequest(
                "Admin User",
                "admin@test.com",
                "password123"
        );
        restTemplate.postForEntity(
                baseUrl + "/api/auth/register",
                registerRequest,
                UserResponse.class
        );

        LoginRequest loginRequest = new LoginRequest("admin@test.com", "password123");
        ResponseEntity<JwtResponse> loginResponse = restTemplate.postForEntity(
                baseUrl + "/api/auth/login",
                loginRequest,
                JwtResponse.class
        );

        authToken = loginResponse.getBody().getToken();
    }

    @Test
    void shouldGetAllUsers() {
        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        // When
        ResponseEntity<List<UserResponse>> response = restTemplate.exchange(
                baseUrl + "/api/users",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<UserResponse>>() {}
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isNotEmpty();
    }

    @Test
    void shouldGetUserById() {
        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        // Get the first user ID
        ResponseEntity<List<UserResponse>> usersResponse = restTemplate.exchange(
                baseUrl + "/api/users",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<UserResponse>>() {}
        );
        Long userId = usersResponse.getBody().get(0).getId();

        // When
        ResponseEntity<UserResponse> response = restTemplate.exchange(
                baseUrl + "/api/users/" + userId,
                HttpMethod.GET,
                entity,
                UserResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(userId);
    }

    @Test
    void shouldCreateUser() {
        // Given
        CreateUserRequest request = new CreateUserRequest(
                "New User",
                "newuser@test.com",
                "password123"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authToken);
        HttpEntity<CreateUserRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<UserResponse> response = restTemplate.exchange(
                baseUrl + "/api/users",
                HttpMethod.POST,
                entity,
                UserResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getEmail()).isEqualTo("newuser@test.com");
        assertThat(response.getBody().getName()).isEqualTo("New User");
    }

    @Test
    void shouldUpdateUser() {
        // Given - Create a user first
        CreateUserRequest createRequest = new CreateUserRequest(
                "Update Test",
                "update@test.com",
                "password123"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authToken);
        HttpEntity<CreateUserRequest> createEntity = new HttpEntity<>(createRequest, headers);

        ResponseEntity<UserResponse> createResponse = restTemplate.exchange(
                baseUrl + "/api/users",
                HttpMethod.POST,
                createEntity,
                UserResponse.class
        );

        Long userId = createResponse.getBody().getId();

        // Update request
        UpdateUserRequest updateRequest = new UpdateUserRequest();
        updateRequest.setName("Updated Name");

        HttpEntity<UpdateUserRequest> updateEntity = new HttpEntity<>(updateRequest, headers);

        // When
        ResponseEntity<UserResponse> response = restTemplate.exchange(
                baseUrl + "/api/users/" + userId,
                HttpMethod.PUT,
                updateEntity,
                UserResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("Updated Name");
    }

    @Test
    void shouldDeleteUser() {
        // Given - Create a user first
        CreateUserRequest createRequest = new CreateUserRequest(
                "Delete Test",
                "delete@test.com",
                "password123"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authToken);
        HttpEntity<CreateUserRequest> createEntity = new HttpEntity<>(createRequest, headers);

        ResponseEntity<UserResponse> createResponse = restTemplate.exchange(
                baseUrl + "/api/users",
                HttpMethod.POST,
                createEntity,
                UserResponse.class
        );

        Long userId = createResponse.getBody().getId();

        // When
        HttpEntity<?> deleteEntity = new HttpEntity<>(headers);
        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl + "/api/users/" + userId,
                HttpMethod.DELETE,
                deleteEntity,
                Void.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void shouldReturn404WhenGettingNonExistentUser() {
        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        // When
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                baseUrl + "/api/users/99999",
                HttpMethod.GET,
                entity,
                ErrorResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldReturn400WhenCreatingUserWithInvalidData() {
        // Given
        CreateUserRequest request = new CreateUserRequest(
                "",
                "invalid-email",
                "short"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authToken);
        HttpEntity<CreateUserRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                baseUrl + "/api/users",
                HttpMethod.POST,
                entity,
                ErrorResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrors()).isNotEmpty();
    }
}
