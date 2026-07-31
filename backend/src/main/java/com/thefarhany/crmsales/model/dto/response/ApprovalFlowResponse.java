package com.thefarhany.crmsales.model.dto.response;

import com.thefarhany.crmsales.model.enums.ApprovalStatus;
import com.thefarhany.crmsales.model.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalFlowResponse {
    private Long contractId;
    private String contractNumber;
    private String contractTitle;
    private String currentStatus;
    private List<ApprovalStep> steps;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApprovalStep {
        private Long approvalId;
        private Integer level;
        private Role approverRole;
        private String approverName;
        private ApprovalStatus status;
        private String notes;
        private String rejectionReason;
        private LocalDateTime requestedAt;
        private LocalDateTime respondedAt;
        private boolean isCurrentStep;
    }
}
