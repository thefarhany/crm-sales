package com.thefarhany.crmsales.model.dto.request;

import com.thefarhany.crmsales.model.enums.EmailTone;
import lombok.Data;

@Data
public class AiEmailRequest {
    private Long contractId;
    private Long templateId;
    private EmailTone tone;
    private String additionalNote;
}