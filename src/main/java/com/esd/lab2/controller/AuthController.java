
package com.esd.lab2.controller;

import com.esd.lab2.dto.LoginRequest;
import com.esd.lab2.dto.LoginResponse;
import com.esd.lab2.dto.SignupRequest;
import com.esd.lab2.dto.UserResponse;
import com.esd.lab2.security.JwtService;
import com.esd.lab2.security.TokenDenylistService;
import com.esd.lab2.service.AuthService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final TokenDenylistService tokenDenylistService;

    public AuthController(
            AuthService authService,
            JwtService jwtService,
            TokenDenylistService tokenDenylistService) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.tokenDenylistService = tokenDenylistService;
    }

    /**
     * POST /api/auth/signup
     * Returns HTTP 201 Created.
     */
    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signup(
            @Valid @RequestBody SignupRequest request) {

        UserResponse response = authService.signup(request);
        return ResponseEntity.status(201).body(response);
    }

    /**
     * POST /api/auth/login
     * Returns HTTP 200 OK with the JWT response.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/logout
     * Denies the current JWT until it expires.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {

        String token = authorization.substring(7);
        Claims claims = jwtService.validateToken(token);

        tokenDenylistService.deny(
                claims.getId(),
                claims.getExpiration().toInstant()
        );

        return ResponseEntity.noContent().build();
    }
}
