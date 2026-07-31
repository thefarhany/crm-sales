package com.thefarhany.crmsales.model.dto.response;

import com.thefarhany.crmsales.model.enums.ReportFormat;
import com.thefarhany.crmsales.model.enums.ReportType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ReportLogResponse {

    private Long id;
    private String reportName;
    private ReportType reportType;
    private ReportFormat format;
    private LocalDate dateRangeStart;
    private LocalDate dateRangeEnd;
    private Long fileSize;
    private String generatedByName;
    private String generatedByRole;
    private LocalDateTime generatedAt;
}
