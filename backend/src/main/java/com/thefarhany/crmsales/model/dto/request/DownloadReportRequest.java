package com.thefarhany.crmsales.model.dto.request;

import com.thefarhany.crmsales.model.enums.ReportFormat;
import com.thefarhany.crmsales.model.enums.ReportType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DownloadReportRequest {

    @NotNull(message = "Report type is required")
    private ReportType reportType;

    @NotNull(message = "Format is required")
    private ReportFormat format;

    private LocalDate dateRangeStart;

    private LocalDate dateRangeEnd;
}
