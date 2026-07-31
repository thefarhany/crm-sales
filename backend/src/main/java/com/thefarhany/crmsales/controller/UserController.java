package com.thefarhany.crmsales.controller;

import com.thefarhany.crmsales.model.dto.ApiResponse;
import com.thefarhany.crmsales.model.dto.PageResponse;
import com.thefarhany.crmsales.model.dto.request.ChangePasswordRequest;
import com.thefarhany.crmsales.model.dto.request.CreateUserRequest;
import com.thefarhany.crmsales.model.dto.request.UpdateProfileRequest;
import com.thefarhany.crmsales.model.dto.request.AdminResetPasswordRequest;
import com.thefarhany.crmsales.model.dto.request.UpdateUserRequest;
import com.thefarhany.crmsales.model.dto.response.UserResponse;
import com.thefarhany.crmsales.model.enums.Role;
import com.thefarhany.crmsales.model.enums.UserStatus;
import com.thefarhany.crmsales.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * GET /api/users
     * Get all users dengan filter dan pagination
     * Access: ADMIN only
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getAllUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<UserResponse> users = userService.getAllUsers(role, status, keyword, page, size);

        return ResponseEntity.ok(
                ApiResponse.success("Users retrieved successfully", users)
        );
    }

    /**
     * POST /api/users
     * Create user baru (Admin create user - bukan self-register!)
     * Access: ADMIN only
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request) {

        UserResponse user = userService.createUser(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("User created successfully", user)
        );
    }

    /**
     * GET /api/users/{id}
     * Get user by ID
     * Access: All authenticated users
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {

        UserResponse user = userService.getUserById(id);

        return ResponseEntity.ok(
                ApiResponse.success("User retrieved successfully", user)
        );
    }

    /**
     * PUT /api/users/{id}
     * Update user (admin update user)
     * Access: ADMIN only
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {

        UserResponse user = userService.updateUser(id, request);

        return ResponseEntity.ok(
                ApiResponse.success("User updated successfully", user)
        );
    }

    /**
     * DELETE /api/users/{id}
     * Deactivate user (soft delete - set status INACTIVE)
     * Access: ADMIN only
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(@PathVariable Long id) {

        userService.deactivateUser(id);

        return ResponseEntity.ok(
                ApiResponse.success("User deactivated successfully", null)
        );
    }

    /**
     * PUT /api/users/{id}/change-password
     */
    @PutMapping("{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> adminResetPassword(
            @PathVariable Long id,
            @Valid @RequestBody AdminResetPasswordRequest request) {
        userService.adminResetPassword(id, request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully", null));
    }

    @PutMapping("me/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> changeMyPassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changeMyPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    /**
     * GET /api/users/me
     * Get current logged user
     * Access: All authenticated users
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {

        UserResponse user = userService.getCurrentUser();

        return ResponseEntity.ok(
                ApiResponse.success("Current user retrieved successfully", user)
        );
    }

    /**
     * PUT /api/users/me
     */
    @PutMapping("me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        UserResponse user = userService.updateProfile(request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", user));
    }

    /**
     * GET /api/users/by-role/{role}
     * Get users by role (untuk dropdown)
     * Access: ADMIN, MARKETING_MANAGER
     */
    @GetMapping("/by-role/{role}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsersByRole(@PathVariable Role role) {

        List<UserResponse> users = userService.getUsersByRole(role);

        return ResponseEntity.ok(
                ApiResponse.success("Users retrieved successfully", users)
        );
    }

    /**
     * PUT /api/users/{id}/assign-role
     * Assign role to user
     * Access: ADMIN only
     */
    @PutMapping("/{id}/assign-role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> assignRole(
            @PathVariable Long id,
            @RequestParam Role role) {

        UserResponse user = userService.assignRole(id, role);

        return ResponseEntity.ok(
                ApiResponse.success("Role assigned successfully", user)
        );
    }
}

