package com.thefarhany.crmsales.model.dto.request;

import com.thefarhany.crmsales.model.enums.ContractType;
import com.thefarhany.crmsales.model.enums.RenewalPeriod;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateContractRequest {

    @NotNull(message = "Client ID is required")
    private Long clientId;

    @NotBlank(message = "Contract title is required")
    @Size(max = 200, message = "Contract title must not exceed 200 characters")
    private String contractTitle;

    @NotNull(message = "Contract type is required")
    private ContractType contractType;

    @NotNull(message = "Contract value is required")
    @DecimalMin(value = "0.00", inclusive = false, message = "Contract value must be greater than 0")
    @Digits(integer = 13, fraction = 2, message = "Contract value must have at most 13 integer digits and 2 decimal places")
    private BigDecimal contractValue;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private RenewalPeriod renewalPeriod = RenewalPeriod.ANNUALLY;

    private Boolean autoRenewal = false;

    private String description;
}
