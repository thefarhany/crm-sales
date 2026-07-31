package com.thefarhany.crmsales.model.dto.response;

import com.thefarhany.crmsales.model.enums.ContractStatus;
import com.thefarhany.crmsales.model.enums.ContractType;
import com.thefarhany.crmsales.model.enums.RenewalPeriod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractResponse {

    private Long id;
    private String contractNumber;
    private String contractTitle;
    private ContractType contractType;
    private BigDecimal contractValue;
    private LocalDate startDate;
    private LocalDate endDate;
    private RenewalPeriod renewalPeriod;
    private Boolean autoRenewal;
    private ContractStatus status;
    private String contractFileUrl;
    private String description;

    private ClientInfo client;

    private CreatorInfo createdBy;

    private Long daysUntilExpiry;
    private Boolean isExpiringSoon;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientInfo {
        private Long id;
        private String clientName;
        private String companyName;
        private String email;
        private String picName;
        private String picEmail;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatorInfo {
        private Long id;
        private String fullName;
        private String email;
    }
}
