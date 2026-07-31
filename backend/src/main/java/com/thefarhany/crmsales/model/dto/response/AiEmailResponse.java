package com.thefarhany.crmsales.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiEmailResponse {
    private String subject;
    private String content;
    
    @JsonProperty("model_used")
    private String modelUsed;
}