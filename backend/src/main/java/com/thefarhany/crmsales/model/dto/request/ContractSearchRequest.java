package com.thefarhany.crmsales.model.dto.request;

import com.thefarhany.crmsales.model.enums.ContractStatus;
import com.thefarhany.crmsales.model.enums.ContractType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ContractSearchRequest {

    private String keyword;

    private Long clientId;

    private ContractStatus status;

    private ContractType contractType;

    private LocalDate startDateFrom;
    private LocalDate startDateTo;
    private LocalDate endDateFrom;
    private LocalDate endDateTo;

    private BigDecimal minValue;
    private BigDecimal maxValue;

    private int page = 0;
    private int size = 10;
}
