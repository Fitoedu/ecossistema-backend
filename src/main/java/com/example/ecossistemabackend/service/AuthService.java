package com.example.ecossistemabackend.service;

import com.example.ecossistemabackend.dto.request.LoginRequest;
import com.example.ecossistemabackend.dto.request.RegisterRequest;
import com.example.ecossistemabackend.dto.response.AuthResponse;
import com.example.ecossistemabackend.dto.response.UserResponse;
import com.example.ecossistemabackend.entity.Role;
import com.example.ecossistemabackend.entity.User;
import com.example.ecossistemabackend.exception.EmailAlreadyExistsException;
import com.example.ecossistemabackend.exception.InvalidCredentialsException;
import com.example.ecossistemabackend.exception.ResourceNotFoundException;
import com.example.ecossistemabackend.repository.UserRepository;
import com.example.ecossistemabackend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Já existe um usuário cadastrado com o e-mail: " + request.email());
        }

        User user = User.builder()
                .name(request.name().trim())
                .email(request.email().trim().toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.ROLE_USER)
                .build();

        User savedUser = userRepository.save(user);
        String token = jwtService.generateToken(savedUser);

        return AuthResponse.of(
                token,
                jwtService.getExpirationTime(),
                UserResponse.fromEntity(savedUser)
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email().trim().toLowerCase(),
                            request.password()
                    )
            );
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException("Credenciais inválidas. Verifique seu e-mail e senha.");
        }

        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        String token = jwtService.generateToken(user);

        return AuthResponse.of(
                token,
                jwtService.getExpirationTime(),
                UserResponse.fromEntity(user)
        );
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com o e-mail: " + email));
        return UserResponse.fromEntity(user);
    }
}
