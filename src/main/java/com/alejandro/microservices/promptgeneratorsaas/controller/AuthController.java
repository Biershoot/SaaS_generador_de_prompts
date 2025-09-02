package com.alejandro.microservices.promptgeneratorsaas.controller;

import com.alejandro.microservices.promptgeneratorsaas.dto.AuthRequest;
import com.alejandro.microservices.promptgeneratorsaas.dto.AuthResponse;
import com.alejandro.microservices.promptgeneratorsaas.dto.ApiResponse;
import com.alejandro.microservices.promptgeneratorsaas.dto.RegisterRequest;
import com.alejandro.microservices.promptgeneratorsaas.entity.Role;
import com.alejandro.microservices.promptgeneratorsaas.entity.User;
import com.alejandro.microservices.promptgeneratorsaas.repository.UserRepository;
import com.alejandro.microservices.promptgeneratorsaas.security.JwtService;
import com.alejandro.microservices.promptgeneratorsaas.security.RefreshTokenService;
import com.alejandro.microservices.promptgeneratorsaas.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.WebUtils;
import jakarta.validation.Valid;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request, HttpServletResponse response) {
        try {
            logger.info("Attempting login for user: {}", request.getUsername());

            // Check if user exists in database
            User user = userRepository.findByUsername(request.getUsername()).orElse(null);
            if (user == null) {
                logger.warn("User not found with username: {}", request.getUsername());
                // Try finding by email in case they're using email as username
                user = userRepository.findByEmail(request.getUsername()).orElse(null);
                if (user == null) {
                    logger.warn("User not found with email: {}", request.getUsername());
                    return ResponseEntity.badRequest().body(AuthResponse.error("Credenciales inválidas"));
                } else {
                    logger.info("User found by email: {}", user.getUsername());
                }
            }

            // Log user role and basic info (without sensitive data)
            logger.info("User found - Username: {}, Role: {}", user.getUsername(), user.getRole());

            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            logger.info("Authentication successful for user: {}", request.getUsername());

            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
            String accessToken = jwtService.generateAccessToken(userDetails);
            String refreshToken = jwtService.generateRefreshToken(userDetails);

            // Save refresh token (optional: for revocation)
            refreshTokenService.save(userDetails.getUsername(), refreshToken);

            // Set refresh token as HttpOnly cookie
            ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                    .httpOnly(true)
                    .secure(false) // Set to true in production with HTTPS
                    .path("/auth")
                    .maxAge(jwtService.getRefreshExpirySeconds())
                    .sameSite("Lax")
                    .build();
            response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            // Create user DTO with the format expected by frontend
            AuthResponse.UserDto userDto = AuthResponse.UserDto.builder()
                    .id(user.getId().toString())
                    .email(user.getEmail())
                    .name(user.getFullName())
                    .role(user.getRole().name())
                    .build();

            logger.info("Login successful for user: {} with role: {}", request.getUsername(), user.getRole());

            return ResponseEntity.ok(AuthResponse.success(accessToken, userDto, "Login exitoso"));

        } catch (BadCredentialsException e) {
            logger.error("Bad credentials for user: {}", request.getUsername());
            return ResponseEntity.badRequest().body(AuthResponse.error("Credenciales inválidas"));
        } catch (Exception e) {
            logger.error("Login error for user: {} - Error: {}", request.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponse.error("Error interno del servidor"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            // Validate password confirmation
            if (!request.getPassword().equals(request.getConfirmPassword())) {
                return ResponseEntity.badRequest().body(AuthResponse.error("Las contraseñas no coinciden"));
            }

            // Check if user already exists by email
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                return ResponseEntity.badRequest().body(AuthResponse.error("El correo electrónico ya está registrado"));
            }

            // Check if username already exists (using email as username)
            if (userRepository.findByUsername(request.getEmail()).isPresent()) {
                return ResponseEntity.badRequest().body(AuthResponse.error("El correo electrónico ya está registrado"));
            }

            // Create new user
            User user = User.builder()
                    .username(request.getEmail()) // Use email as username
                    .password(passwordEncoder.encode(request.getPassword()))
                    .email(request.getEmail())
                    .fullName(request.getFullName())
                    .role(Role.USER) // Default role
                    .build();

            userRepository.save(user);

            // Create user DTO
            AuthResponse.UserDto userDto = AuthResponse.UserDto.builder()
                    .id(user.getId().toString())
                    .email(user.getEmail())
                    .name(user.getFullName())
                    .role(user.getRole().name())
                    .build();

            return ResponseEntity.ok(AuthResponse.success(null, userDto, "Usuario registrado exitosamente"));

        } catch (Exception e) {
            logger.error("Registration error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponse.error("Error al crear la cuenta. Intenta nuevamente."));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            Cookie cookie = WebUtils.getCookie(request, "refreshToken");
            if (cookie != null) {
                refreshTokenService.revoke(cookie.getValue());
            }

            // Delete refresh token cookie
            ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                    .httpOnly(true)
                    .secure(false) // Set to true in production with HTTPS
                    .path("/auth")
                    .maxAge(0)
                    .build();
            response.setHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());

            return ResponseEntity.ok(ApiResponse.success("Logout exitoso"));
        } catch (Exception e) {
            logger.error("Logout error: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success("Logout exitoso")); // Even if error, logout succeeds
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        try {
            Cookie cookie = WebUtils.getCookie(request, "refreshToken");
            if (cookie == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AuthResponse.error("Token de refresh no encontrado"));
            }

            String refreshToken = cookie.getValue();
            if (!jwtService.validateRefreshToken(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AuthResponse.error("Token de refresh inválido"));
            }

            String username = jwtService.extractUsername(refreshToken);

            // Optional: validate against stored refresh tokens
            if (!refreshTokenService.isValid(username, refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AuthResponse.error("Token de refresh inválido"));
            }

            String newAccessToken = jwtService.generateAccessToken(username);

            // Optional: rotate refresh token for security
            String newRefreshToken = jwtService.generateRefreshToken(username);
            refreshTokenService.save(username, newRefreshToken);

            // Set new refresh token cookie
            ResponseCookie newCookie = ResponseCookie.from("refreshToken", newRefreshToken)
                    .httpOnly(true)
                    .secure(false) // Set to true in production with HTTPS
                    .path("/auth")
                    .maxAge(jwtService.getRefreshExpirySeconds())
                    .sameSite("Lax")
                    .build();
            response.setHeader(HttpHeaders.SET_COOKIE, newCookie.toString());

            // Get user info for response
            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AuthResponse.error("Usuario no encontrado"));
            }

            AuthResponse.UserDto userDto = AuthResponse.UserDto.builder()
                    .id(user.getId().toString())
                    .email(user.getEmail())
                    .name(user.getFullName())
                    .role(user.getRole().name())
                    .build();

            return ResponseEntity.ok(AuthResponse.success(newAccessToken, userDto, "Token renovado exitosamente"));

        } catch (Exception e) {
            logger.error("Refresh token error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.error("Error al renovar token"));
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<AuthResponse> validateToken(@RequestHeader(value = "Authorization", required = false) String token) {
        try {
            if (token == null || token.isBlank()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AuthResponse.error("Token de autorización requerido"));
            }

            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            if (!jwtService.validateAccessToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AuthResponse.error("Token inválido"));
            }

            String username = jwtService.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (!jwtService.validateToken(token, userDetails)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AuthResponse.error("Token inválido"));
            }

            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AuthResponse.error("Usuario no encontrado"));
            }

            AuthResponse.UserDto userDto = AuthResponse.UserDto.builder()
                    .id(user.getId().toString())
                    .email(user.getEmail())
                    .name(user.getFullName())
                    .role(user.getRole().name())
                    .build();

            return ResponseEntity.ok(AuthResponse.success(token, userDto, "Token válido"));

        } catch (Exception e) {
            logger.error("Token validation error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.error("Token inválido"));
        }
    }

    // Test endpoint to check user data - mantener para debugging
    @PostMapping("/test-login")
    public ResponseEntity<?> testLogin(@RequestBody AuthRequest request) {
        try {
            logger.info("Testing login for user: {}", request.getUsername());

            // Check by username
            User userByUsername = userRepository.findByUsername(request.getUsername()).orElse(null);

            // Check by email
            User userByEmail = userRepository.findByEmail(request.getUsername()).orElse(null);

            // Check password encoding
            String encodedPassword = passwordEncoder.encode(request.getPassword());

            return ResponseEntity.ok(Map.of(
                    "userFoundByUsername", userByUsername != null,
                    "userFoundByEmail", userByEmail != null,
                    "usernameFromDB", userByUsername != null ? userByUsername.getUsername() : "N/A",
                    "emailFromDB", userByEmail != null ? userByEmail.getEmail() : "N/A",
                    "inputUsername", request.getUsername(),
                    "inputPassword", request.getPassword(),
                    "encodedPassword", encodedPassword,
                    "passwordMatch", userByUsername != null ? passwordEncoder.matches(request.getPassword(), userByUsername.getPassword()) : false
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
