package com.thefarhany.crmsales.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ReportSummaryResponse {

    private long reportsGeneratedThisMonth;
    private long totalActiveContracts;
    private BigDecimal totalContractValue;
}
