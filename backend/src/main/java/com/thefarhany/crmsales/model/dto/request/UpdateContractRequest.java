package com.thefarhany.crmsales.model.dto.request;

import com.thefarhany.crmsales.model.enums.ContractStatus;
import com.thefarhany.crmsales.model.enums.ContractType;
import com.thefarhany.crmsales.model.enums.RenewalPeriod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateContractRequest {

    private Long clientId;

    @Size(max = 200, message = "Contract title must not exceed 200 characters")
    private String contractTitle;

    private ContractType contractType;

    @DecimalMin(value = "0.00", inclusive = false, message = "Contract value must be greater than 0")
    @Digits(integer = 13, fraction = 2, message = "Contract value must have at most 13 integer digits and 2 decimal places")
    private BigDecimal contractValue;

    private LocalDate startDate;

    private LocalDate endDate;

    private RenewalPeriod renewalPeriod;

    private Boolean autoRenewal;

    private ContractStatus status;

    private String description;
}
