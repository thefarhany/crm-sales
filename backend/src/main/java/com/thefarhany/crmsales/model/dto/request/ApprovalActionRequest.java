package com.thefarhany.crmsales.model.dto.request;

import lombok.Data;

@Data
public class ApprovalActionRequest {
    private String notes;
    private String rejectionReason;
}
