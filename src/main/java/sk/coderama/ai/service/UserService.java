package sk.coderama.ai.service;

import sk.coderama.ai.dto.request.CreateUserRequest;
import sk.coderama.ai.dto.request.UpdateUserRequest;
import sk.coderama.ai.dto.response.UserResponse;

import java.util.List;

public interface UserService {

    List<UserResponse> getAllUsers();

    UserResponse getUserById(Long id);

    UserResponse createUser(CreateUserRequest request);

    UserResponse updateUser(Long id, UpdateUserRequest request);

    void deleteUser(Long id);
}
