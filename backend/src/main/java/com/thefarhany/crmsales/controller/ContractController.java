package com.thefarhany.crmsales.controller;

import com.thefarhany.crmsales.model.dto.ApiResponse;
import com.thefarhany.crmsales.model.dto.PageResponse;
import com.thefarhany.crmsales.model.dto.request.BulkContractRequest;
import com.thefarhany.crmsales.model.dto.request.ContractSearchRequest;
import com.thefarhany.crmsales.model.dto.request.CreateContractRequest;
import com.thefarhany.crmsales.model.dto.request.UpdateContractRequest;
import com.thefarhany.crmsales.model.dto.response.ApprovalFlowResponse;
import com.thefarhany.crmsales.model.dto.response.ContractCalendarResponse;
import com.thefarhany.crmsales.model.dto.response.ContractResponse;
import com.thefarhany.crmsales.model.dto.response.ContractStatsResponse;
import com.thefarhany.crmsales.model.enums.ContractStatus;
import com.thefarhany.crmsales.model.enums.ContractType;
import com.thefarhany.crmsales.service.ApprovalService;
import com.thefarhany.crmsales.service.ContractService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    @Autowired
    private ContractService contractService;

    @Autowired
    private ApprovalService approvalService;

    /**
     * Get all contracts dengan filter dan pagination
     * Access: All authenticated users
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<ContractResponse>>> getAllContracts(
            @RequestParam(required = false) ContractStatus status,
            @RequestParam(required = false) ContractType contractType,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<ContractResponse> contracts = contractService.getAllContracts(
                status, contractType, clientId, keyword, page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Contracts retrieved successfully", contracts)
        );
    }

    /**
     * Create contract
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_TEAM')")
    public ResponseEntity<ApiResponse<ContractResponse>> createContract(
            @Valid @RequestPart("data") CreateContractRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        ContractResponse contract = contractService.createContract(request, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Contract created successfully", contract)
        );
    }

    /**
     * Get contract by ID
     * Access: All authenticated users
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ContractResponse>> getContractById(@PathVariable Long id) {
        ContractResponse contract = contractService.getContractById(id);
        return ResponseEntity.ok(
                ApiResponse.success("Contract retrieved successfully", contract)
        );
    }

    /**
     * Update contract
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_TEAM')")
    public ResponseEntity<ApiResponse<ContractResponse>> updateContract(
            @PathVariable Long id,
            @Valid @RequestPart("data") UpdateContractRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        ContractResponse contract = contractService.updateContract(id, request, file);
        return ResponseEntity.ok(
                ApiResponse.success("Contract updated successfully", contract)
        );
    }

    /**
     * Delete contract
     * Access: ADMIN, MARKETING_MANAGER
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteContract(@PathVariable Long id) {
        contractService.deleteContract(id);
        return ResponseEntity.ok(
                ApiResponse.success("Contract deleted successfully", null)
        );
    }

    /**
     * Upload/replace contract file
     * Access: ADMIN, MARKETING_MANAGER, MARKETING_TEAM
     */
    @PostMapping("/{id}/upload-file")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_TEAM')")
    public ResponseEntity<ApiResponse<ContractResponse>> uploadContractFile(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {

        ContractResponse contract = contractService.uploadContractFile(id, file);
        return ResponseEntity.ok(
                ApiResponse.success("Contract file uploaded successfully", contract)
        );
    }

    /**
     * Renew contract
     * Access: ADMIN, MARKETING_MANAGER, MARKETING_TEAM
     */
    @PostMapping("/{id}/renew")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_TEAM')")
    public ResponseEntity<ApiResponse<ContractResponse>> renewContract(@PathVariable Long id) {
        ContractResponse contract = contractService.renewContract(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Contract renewed successfully", contract)
        );
    }

    /**
     * Get contracts
     * Access: All authenticated users
     */
    @GetMapping("/expiring")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<ContractResponse>>> getExpiringContracts(
            @RequestParam(defaultValue = "60") int days,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<ContractResponse> contracts = contractService.getExpiringContracts(days, page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Expiring contracts retrieved successfully", contracts)
        );
    }

    /**
     * Get contract calendar view
     * Access: All authenticated users
     */
    @GetMapping("/calendar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ContractCalendarResponse>>> getContractCalendar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        List<ContractCalendarResponse> calendar = contractService.getContractCalendar(from, to);
        return ResponseEntity.ok(
                ApiResponse.success("Contract calendar retrieved successfully", calendar)
        );
    }

    /**
     * Get contract history
     * Access: All authenticated users
     */
    @GetMapping("/{id}/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ContractResponse>> getContractHistory(@PathVariable Long id) {
        ContractResponse contract = contractService.getContractById(id);
        return ResponseEntity.ok(
                ApiResponse.success("Contract history retrieved successfully", contract)
        );
    }

    /**
     * Get contract statistics
     * Access: All authenticated users
     */
    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ContractStatsResponse>> getContractStats() {
        ContractStatsResponse stats = contractService.getContractStats();
        return ResponseEntity.ok(
                ApiResponse.success("Contract statistics retrieved successfully", stats)
        );
    }

    /**
     * Get approval flow/timeline untuk contract
     * Access: All authenticated users
     */
    @GetMapping("/{id}/approval-flow")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ApprovalFlowResponse>> getApprovalFlow(@PathVariable Long id) {
        ApprovalFlowResponse flow = approvalService.getApprovalFlow(id);
        return ResponseEntity.ok(
                ApiResponse.success("Approval flow retrieved successfully", flow)
        );
    }

    /**
     * Bulk update contract status
     * Access: ADMIN, MARKETING_MANAGER
     */
    @PutMapping("/bulk-update")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_TEAM')")
    public ResponseEntity<ApiResponse<List<ContractResponse>>> bulkUpdate(
            @Valid @RequestBody BulkContractRequest request) {

        List<ContractResponse> contracts = contractService.bulkUpdateStatus(request);
        return ResponseEntity.ok(
                ApiResponse.success("Contracts updated successfully", contracts)
        );
    }

    /**
     * Bulk delete contracts
     * Access: ADMIN, MARKETING_MANAGER
     */
    @DeleteMapping("/bulk-delete")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_TEAM')")
    public ResponseEntity<ApiResponse<Void>> bulkDelete(
            @Valid @RequestBody BulkContractRequest request) {

        contractService.bulkDelete(request);
        return ResponseEntity.ok(
                ApiResponse.success("Contracts deleted successfully", null)
        );
    }

    /**
     * Advanced search contracts
     * Access: All authenticated users
     */
    @PostMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<ContractResponse>>> advancedSearch(
            @RequestBody ContractSearchRequest request) {

        PageResponse<ContractResponse> contracts = contractService.advancedSearch(request);
        return ResponseEntity.ok(
                ApiResponse.success("Search results retrieved successfully", contracts)
        );
    }

    /**
     * Export contracts ke CSV
     * Access: ADMIN, MARKETING_MANAGER
     */
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'MARKETING_TEAM')")
    public ResponseEntity<byte[]> exportContracts(
            @RequestParam(required = false) ContractStatus status,
            @RequestParam(required = false) ContractType contractType,
            @RequestParam(required = false) String keyword) {

        byte[] csvData = contractService.exportContracts(status, contractType, keyword);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", "contracts_export.csv");
        return new ResponseEntity<>(csvData, headers, HttpStatus.OK);
    }
}
