package sk.coderama.ai.service;

import sk.coderama.ai.dto.request.LoginRequest;
import sk.coderama.ai.dto.request.RegisterRequest;
import sk.coderama.ai.dto.response.JwtResponse;
import sk.coderama.ai.dto.response.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    JwtResponse login(LoginRequest request);
}
