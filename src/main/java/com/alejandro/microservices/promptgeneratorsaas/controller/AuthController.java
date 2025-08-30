package com.alejandro.microservices.promptgeneratorsaas.controller;

import com.alejandro.microservices.promptgeneratorsaas.dto.AuthRequest;
import com.alejandro.microservices.promptgeneratorsaas.dto.AuthResponse;
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
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request, HttpServletResponse response) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Credenciales inválidas"));
        }

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

        // Get user role and full name
        User user = userRepository.findByUsername(request.getUsername()).orElse(null);
        String role = user != null ? user.getRole().name() : "USER";
        String fullName = user != null ? user.getFullName() : "";

        return ResponseEntity.ok(Map.of(
                "accessToken", accessToken,
                "username", request.getUsername(),
                "fullName", fullName,
                "role", role
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        // Validate password confirmation
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Las contraseñas no coinciden"));
        }

        // Check if user already exists by email
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El correo electrónico ya está registrado"));
        }

        // Check if username already exists (using email as username)
        if (userRepository.findByUsername(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El correo electrónico ya está registrado"));
        }

        // Create new user
        User user = User.builder()
                .username(request.getEmail()) // Use email as username
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .fullName(request.getFullName())
                .role(Role.USER) // Default role
                .build();

        try {
            userRepository.save(user);

            // Generate token for the new user
            final UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
            final String accessToken = jwtService.generateAccessToken(userDetails);

            return ResponseEntity.ok(Map.of(
                    "accessToken", accessToken,
                    "username", request.getEmail(),
                    "fullName", request.getFullName(),
                    "role", user.getRole().name()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al crear la cuenta. Intenta nuevamente."));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = WebUtils.getCookie(request, "refreshToken");
        if (cookie == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String refreshToken = cookie.getValue();
        if (!jwtService.validateRefreshToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = jwtService.extractUsername(refreshToken);
        
        // Optional: validate against stored refresh tokens
        if (!refreshTokenService.isValid(username, refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
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
        String fullName = user != null ? user.getFullName() : "";
        
        return ResponseEntity.ok(Map.of(
                "accessToken", newAccessToken,
                "username", username,
                "fullName", fullName
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
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
        
        return ResponseEntity.ok(Map.of("message", "Sesión cerrada exitosamente"));
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader(value = "Authorization", required = false) String token) {
        try {
            if (token == null || token.isBlank()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Falta el header Authorization"));
            }

            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            if (!jwtService.validateAccessToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Token inválido"));
            }
            
            String username = jwtService.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
            if (jwtService.validateToken(token, userDetails)) {
                User user = userRepository.findByUsername(username).orElse(null);
                String role = user != null ? user.getRole().name() : "USER";
                String fullName = user != null ? user.getFullName() : "";
                return ResponseEntity.ok(Map.of(
                        "accessToken", token,
                        "username", username,
                        "fullName", fullName,
                        "role", role
                ));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Token inválido"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Token inválido"));
        }
    }

    @PostMapping("/test-login")
    public ResponseEntity<?> testLogin(@RequestBody AuthRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
            String accessToken = jwtService.generateAccessToken(userDetails);

            // Get user role and full name
            User user = userRepository.findByUsername(request.getUsername()).orElse(null);
            String role = user != null ? user.getRole().name() : "USER";
            String fullName = user != null ? user.getFullName() : "";

            return ResponseEntity.ok(Map.of(
                    "accessToken", accessToken,
                    "username", request.getUsername(),
                    "fullName", fullName,
                    "role", role,
                    "message", "Login exitoso"
            ));
        } catch (BadCredentialsException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Credenciales inválidas"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error interno: " + e.getMessage()));
        }
    }
}
