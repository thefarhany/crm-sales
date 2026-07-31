package com.thefarhany.crmsales.controller;

import com.thefarhany.crmsales.model.dto.ApiResponse;
import com.thefarhany.crmsales.model.dto.request.DownloadReportRequest;
import com.thefarhany.crmsales.model.dto.response.ReportLogResponse;
import com.thefarhany.crmsales.model.dto.response.ReportSummaryResponse;
import com.thefarhany.crmsales.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    // GET /api/reports/summary
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'MARKETING_TEAM', 'BOD')")
    public ResponseEntity<ApiResponse<ReportSummaryResponse>> getSummary() {
        ReportSummaryResponse summary = reportService.getSummary();
        return ResponseEntity.ok(ApiResponse.success("Report summary retrieved successfully", summary));
    }

    // GET /api/reports/recent
    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'MARKETING_TEAM', 'BOD')")
    public ResponseEntity<ApiResponse<Page<ReportLogResponse>>> getRecentReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ReportLogResponse> reports = reportService.getRecentReports(page, size);
        return ResponseEntity.ok(ApiResponse.success("Recent reports retrieved successfully", reports));
    }

    // POST /api/reports/download
    @PostMapping("/download")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'MARKETING_TEAM', 'BOD')")
    public ResponseEntity<byte[]> downloadReport(
            @Valid @RequestBody DownloadReportRequest request) {
        byte[] fileBytes = reportService.generateAndDownload(request);
        String fileName = reportService.getFileName(request);
        String contentType = reportService.getContentType(request.getFormat());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .body(fileBytes);
    }
}
