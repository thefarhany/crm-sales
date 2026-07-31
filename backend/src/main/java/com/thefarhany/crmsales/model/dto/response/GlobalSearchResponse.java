package com.thefarhany.crmsales.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalSearchResponse {
    private String id;
    private String title;
    private String subtitle;
    private String type; // CONTRACT, CLIENT
    private String link;
}
