package com.thefarhany.crmsales.controller;

import com.thefarhany.crmsales.model.dto.ApiResponse;
import com.thefarhany.crmsales.model.dto.request.ForgotPasswordRequest;
import com.thefarhany.crmsales.model.dto.request.LoginRequest;
import com.thefarhany.crmsales.model.dto.request.ResetPasswordRequest;
import com.thefarhany.crmsales.model.dto.response.LoginResponse;
import com.thefarhany.crmsales.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    /**
     * POST /api/auth/login
     * Login user dan set JWT token ke HttpOnly Cookie
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        LoginResponse loginResponse = authService.login(request);

        ResponseCookie cookie = ResponseCookie.from("accessToken", loginResponse.getAccessToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(jwtExpirationMs / 1000)
                .sameSite("Lax")
                .build();

        response.addHeader("Set-Cookie", cookie.toString());

        LoginResponse responseWithoutToken = LoginResponse.builder()
                .userId(loginResponse.getUserId())
                .username(loginResponse.getUsername())
                .fullName(loginResponse.getFullName())
                .email(loginResponse.getEmail())
                .role(loginResponse.getRole())
                .build();

        return ResponseEntity.ok(
                ApiResponse.success("Login successful", responseWithoutToken)
        );
    }

    /**
     * POST /api/auth/logout
     * Logout user dengan delete cookie
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {

        ResponseCookie cookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        response.addHeader("Set-Cookie", cookie.toString());

        return ResponseEntity.ok(
                ApiResponse.success("Logout successful", null)
        );
    }

    /**
     * POST /api/auth/refresh-token
     * Refresh JWT token
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<String>> refreshToken(
            @CookieValue(name = "accessToken", required = false) String oldToken,
            HttpServletResponse response) {

        if (oldToken == null || oldToken.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("No token found in cookie")
            );
        }

        String newToken = authService.refreshToken(oldToken);

        ResponseCookie cookie = ResponseCookie.from("accessToken", newToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(jwtExpirationMs / 1000)
                .sameSite("Lax")
                .build();

        response.addHeader("Set-Cookie", cookie.toString());

        return ResponseEntity.ok(
                ApiResponse.success("Token refreshed successfully", newToken)
        );
    }

    /**
     * POST /api/auth/forgot-password
     * Request reset password - send token via email
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        authService.forgotPassword(request);

        return ResponseEntity.ok(
                ApiResponse.success("Reset password link has been sent to your email", null)
        );
    }

    /**
     * POST /api/auth/reset-password
     * Reset password dengan token dari email
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        authService.resetPassword(request);

        return ResponseEntity.ok(
                ApiResponse.success("Password has been reset successfully", null)
        );
    }

    /**
     * GET /api/auth/validate-reset-token
     * Validate reset token (untuk check apakah token masih valid)
     */
    @GetMapping("/validate-reset-token")
    public ResponseEntity<ApiResponse<Boolean>> validateResetToken(
            @RequestParam String token) {

        boolean isValid = authService.validateResetToken(token);

        if (isValid) {
            return ResponseEntity.ok(
                    ApiResponse.success("Token is valid", true)
            );
        } else {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Token is invalid or expired")
            );
        }
    }
}
