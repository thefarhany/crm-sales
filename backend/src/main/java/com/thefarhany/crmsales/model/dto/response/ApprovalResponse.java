package com.thefarhany.crmsales.model.dto.response;

import com.thefarhany.crmsales.model.enums.ApprovalStatus;
import com.thefarhany.crmsales.model.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalResponse {

    private Long id;
    private Integer approvalLevel;
    private Role approverRole;
    private ApprovalStatus status;
    private String rejectionReason;
    private String notes;
    private LocalDateTime requestedAt;
    private LocalDateTime respondedAt;
    private LocalDateTime createdAt;
    private boolean actionable;
    private ContractInfo contract;

    private UserInfo requestedBy;

    private UserInfo approver;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContractInfo {
        private Long id;
        private String contractNumber;
        private String contractTitle;
        private String companyName;
        private String clientName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String fullName;
        private String email;
        private Role role;
    }
}
