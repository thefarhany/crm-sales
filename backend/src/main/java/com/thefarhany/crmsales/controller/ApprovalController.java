package com.thefarhany.crmsales.controller;

import com.thefarhany.crmsales.model.dto.ApiResponse;
import com.thefarhany.crmsales.model.dto.PageResponse;
import com.thefarhany.crmsales.model.dto.request.ApprovalActionRequest;
import com.thefarhany.crmsales.model.dto.response.ApprovalResponse;
import com.thefarhany.crmsales.model.enums.ApprovalStatus;
import com.thefarhany.crmsales.model.enums.Role;
import com.thefarhany.crmsales.service.ApprovalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/approvals")
public class ApprovalController {

    @Autowired
    private ApprovalService approvalService;

    /**
     * Get all approvals
     * Access: All authenticated users
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<ApprovalResponse>>> getAllApprovals(
            @RequestParam(required = false) ApprovalStatus status,
            @RequestParam(required = false) Role approverRole,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<ApprovalResponse> approvals = approvalService.getAllApprovals(
                status, approverRole, keyword, page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Approvals retrieved successfully", approvals)
        );
    }

    /**
     * Get approval by ID
     * Access: All authenticated users
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ApprovalResponse>> getApprovalById(@PathVariable Long id) {
        ApprovalResponse approval = approvalService.getApprovalById(id);
        return ResponseEntity.ok(
                ApiResponse.success("Approval retrieved successfully", approval)
        );
    }

    /**
     * Approve contract
     * Access: MARKETING_MANAGER, BOD
     */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'BOD')")
    public ResponseEntity<ApiResponse<ApprovalResponse>> approveContract(
            @PathVariable Long id,
            @RequestBody(required = false) ApprovalActionRequest request) {

        ApprovalResponse approval = approvalService.approveContract(id, request);
        return ResponseEntity.ok(
                ApiResponse.success("Contract approved successfully", approval)
        );
    }

    /**
     * Reject contract
     * Access: MARKETING_MANAGER, BOD
     */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'BOD')")
    public ResponseEntity<ApiResponse<ApprovalResponse>> rejectContract(
            @PathVariable Long id,
            @RequestBody ApprovalActionRequest request) {

        ApprovalResponse approval = approvalService.rejectContract(id, request);
        return ResponseEntity.ok(
                ApiResponse.success("Contract rejected successfully", approval)
        );
    }

    /**
     * Get pending approvals
     * Access: All authenticated users
     */
    @GetMapping("/pending")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<ApprovalResponse>>> getPendingApprovals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<ApprovalResponse> approvals = approvalService.getPendingApprovals(page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Pending approvals retrieved successfully", approvals)
        );
    }

    /**
     * Get approval history
     * Access: All authenticated users
     */
    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<ApprovalResponse>>> getApprovalHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<ApprovalResponse> approvals = approvalService.getApprovalHistory(page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Approval history retrieved successfully", approvals)
        );
    }

    /**
     * Get my approvals
     * Access: All authenticated users
     */
    @GetMapping("/my-approvals")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<ApprovalResponse>>> getMyApprovals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<ApprovalResponse> approvals = approvalService.getMyApprovals(page, size);
        return ResponseEntity.ok(
                ApiResponse.success("My approvals retrieved successfully", approvals)
        );
    }
}
