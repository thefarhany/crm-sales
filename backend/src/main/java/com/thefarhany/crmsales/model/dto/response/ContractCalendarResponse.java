package com.thefarhany.crmsales.model.dto.response;

import com.thefarhany.crmsales.model.enums.ContractStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractCalendarResponse {

    private Long id;
    private String contractNumber;
    private String contractTitle;
    private String clientName;
    private String companyName;
    private LocalDate startDate;
    private LocalDate endDate;
    private ContractStatus status;
    private Long daysUntilExpiry;
}
