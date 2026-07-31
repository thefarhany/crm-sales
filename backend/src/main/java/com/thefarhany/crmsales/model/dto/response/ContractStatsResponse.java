package com.thefarhany.crmsales.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractStatsResponse {

    private Long totalContracts;
    private Long activeContracts;
    private Long draftContracts;
    private Long expiredContracts;
    private Long pendingApprovalContracts;
    private Long terminatedContracts;

    private Long expiringSoon60Days;
    private Long expiringSoon30Days;
    private Long expiringSoon7Days;

    private BigDecimal totalContractValue;
    private BigDecimal activeContractValue;
}
