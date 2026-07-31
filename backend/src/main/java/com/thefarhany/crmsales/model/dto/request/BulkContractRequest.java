package com.thefarhany.crmsales.model.dto.request;

import com.thefarhany.crmsales.model.enums.ContractStatus;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BulkContractRequest {

    @NotEmpty(message = "Contract IDs are required")
    private List<Long> contractIds;

    private ContractStatus status;
}
