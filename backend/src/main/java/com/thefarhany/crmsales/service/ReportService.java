package com.thefarhany.crmsales.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.thefarhany.crmsales.exception.BadRequestException;
import com.thefarhany.crmsales.exception.ResourceNotFoundException;
import com.thefarhany.crmsales.model.dto.request.DownloadReportRequest;
import com.thefarhany.crmsales.model.dto.response.ReportLogResponse;
import com.thefarhany.crmsales.model.dto.response.ReportSummaryResponse;
import com.thefarhany.crmsales.model.entity.*;
import com.thefarhany.crmsales.model.enums.*;
import com.thefarhany.crmsales.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired
    private ReportLogRepository reportLogRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ContractApprovalRepository approvalRepository;

    @Autowired
    private UserRepository userRepository;

    // ==================== SUMMARY ====================

    public ReportSummaryResponse getSummary() {
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        return ReportSummaryResponse.builder()
                .reportsGeneratedThisMonth(reportLogRepository.countByGeneratedAtBetween(startOfMonth, now))
                .totalActiveContracts(contractRepository.countByStatus(ContractStatus.ACTIVE))
                .totalContractValue(contractRepository.sumContractValueByStatus(ContractStatus.ACTIVE))
                .build();
    }

    // ==================== RECENT LOGS ====================

    public Page<ReportLogResponse> getRecentReports(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return reportLogRepository.findAllByOrderByGeneratedAtDesc(pageable)
                .map(this::mapToResponse);
    }

    // ==================== GENERATE & DOWNLOAD ====================

    @Transactional
    public byte[] generateAndDownload(DownloadReportRequest request) {
        byte[] fileBytes;

        switch (request.getReportType()) {
            case CONTRACT -> fileBytes = generateContractReport(request);
            case CLIENT -> fileBytes = generateClientReport(request);
            case APPROVAL -> fileBytes = generateApprovalReport(request);
            case REVENUE -> fileBytes = generateRevenueReport(request);
            default -> throw new BadRequestException("Invalid report type");
        }

        // Log the download
        saveReportLog(request, fileBytes.length);

        return fileBytes;
    }

    public String getFileName(DownloadReportRequest request) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String extension = request.getFormat() == ReportFormat.PDF ? ".pdf" : ".xlsx";
        return request.getReportType().name().toLowerCase() + "_report_" + timestamp + extension;
    }

    public String getContentType(ReportFormat format) {
        return format == ReportFormat.PDF
                ? "application/pdf"
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }

    // ==================== CONTRACT REPORT ====================

    private byte[] generateContractReport(DownloadReportRequest request) {
        List<Contract> contracts = getContractsByDateRange(request.getDateRangeStart(), request.getDateRangeEnd());

        if (request.getFormat() == ReportFormat.EXCEL) {
            return generateContractExcel(contracts);
        } else {
            return generateContractPdf(contracts);
        }
    }

    private List<Contract> getContractsByDateRange(LocalDate start, LocalDate end) {
        if (start != null && end != null) {
            return contractRepository.findAll().stream()
                    .filter(c -> !c.getStartDate().isBefore(start) && !c.getStartDate().isAfter(end))
                    .collect(Collectors.toList());
        }
        return contractRepository.findAll();
    }

    private byte[] generateContractExcel(List<Contract> contracts) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Contract Report");
            String[] headers = {"No", "Contract Number", "Title", "Client", "Type", "Value",
                    "Start Date", "End Date", "Status"};

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            for (int i = 0; i < contracts.size(); i++) {
                Contract c = contracts.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(i + 1);
                row.createCell(1).setCellValue(c.getContractNumber());
                row.createCell(2).setCellValue(c.getContractTitle());
                row.createCell(3).setCellValue(c.getClient().getCompanyName());
                row.createCell(4).setCellValue(c.getContractType().name());
                row.createCell(5).setCellValue(c.getContractValue().doubleValue());
                row.createCell(6).setCellValue(c.getStartDate().format(df));
                row.createCell(7).setCellValue(c.getEndDate().format(df));
                row.createCell(8).setCellValue(c.getStatus().name());
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel report", e);
        }
    }

    private byte[] generateContractPdf(List<Contract> contracts) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            // Title
            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD, new Color(30, 64, 175));
            Paragraph title = new Paragraph("Contract Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(12);
            document.add(title);

            // Subtitle
            Font subtitleFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.GRAY);
            Paragraph subtitle = new Paragraph("Generated on " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")), subtitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(20);
            document.add(subtitle);

            // Table
            PdfPTable table = new PdfPTable(new float[]{1, 3, 4, 4, 2, 3, 2.5f, 2.5f, 2});
            table.setWidthPercentage(100);

            String[] headers = {"No", "Contract No", "Title", "Client", "Type", "Value",
                    "Start", "End", "Status"};
            Font headerFont = new Font(Font.HELVETICA, 8, Font.BOLD, Color.WHITE);

            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(new Color(30, 64, 175));
                cell.setPadding(6);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            Font dataFont = new Font(Font.HELVETICA, 7, Font.NORMAL);
            DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            for (int i = 0; i < contracts.size(); i++) {
                Contract c = contracts.get(i);
                Color bgColor = (i % 2 == 0) ? Color.WHITE : new Color(245, 247, 250);

                addPdfCell(table, String.valueOf(i + 1), dataFont, bgColor);
                addPdfCell(table, c.getContractNumber(), dataFont, bgColor);
                addPdfCell(table, c.getContractTitle(), dataFont, bgColor);
                addPdfCell(table, c.getClient().getCompanyName(), dataFont, bgColor);
                addPdfCell(table, c.getContractType().name(), dataFont, bgColor);
                addPdfCell(table, formatCurrency(c.getContractValue()), dataFont, bgColor);
                addPdfCell(table, c.getStartDate().format(df), dataFont, bgColor);
                addPdfCell(table, c.getEndDate().format(df), dataFont, bgColor);
                addPdfCell(table, c.getStatus().name(), dataFont, bgColor);
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    // ==================== CLIENT REPORT ====================

    private byte[] generateClientReport(DownloadReportRequest request) {
        List<Client> clients = clientRepository.findAll();

        if (request.getFormat() == ReportFormat.EXCEL) {
            return generateClientExcel(clients);
        } else {
            return generateClientPdf(clients);
        }
    }

    private byte[] generateClientExcel(List<Client> clients) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Client Report");
            String[] headers = {"No", "Client Name", "Company", "Email", "Phone",
                    "Industry", "City", "PIC Name", "Status"};

            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (int i = 0; i < clients.size(); i++) {
                Client c = clients.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(i + 1);
                row.createCell(1).setCellValue(c.getClientName());
                row.createCell(2).setCellValue(c.getCompanyName());
                row.createCell(3).setCellValue(c.getEmail());
                row.createCell(4).setCellValue(c.getPhone() != null ? c.getPhone() : "");
                row.createCell(5).setCellValue(c.getIndustry() != null ? c.getIndustry() : "");
                row.createCell(6).setCellValue(c.getCity() != null ? c.getCity() : "");
                row.createCell(7).setCellValue(c.getPicName());
                row.createCell(8).setCellValue(c.getStatus().name());
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel report", e);
        }
    }

    private byte[] generateClientPdf(List<Client> clients) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD, new Color(6, 95, 70));
            Paragraph title = new Paragraph("Client Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(12);
            document.add(title);

            Font subtitleFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.GRAY);
            Paragraph subtitle = new Paragraph("Generated on " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")), subtitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(20);
            document.add(subtitle);

            PdfPTable table = new PdfPTable(new float[]{1, 3, 4, 4, 2.5f, 2.5f, 2.5f, 3, 2});
            table.setWidthPercentage(100);

            String[] headers = {"No", "Client Name", "Company", "Email", "Phone",
                    "Industry", "City", "PIC Name", "Status"};
            Font headerFont = new Font(Font.HELVETICA, 8, Font.BOLD, Color.WHITE);

            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(new Color(6, 95, 70));
                cell.setPadding(6);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            Font dataFont = new Font(Font.HELVETICA, 7, Font.NORMAL);
            for (int i = 0; i < clients.size(); i++) {
                Client c = clients.get(i);
                Color bgColor = (i % 2 == 0) ? Color.WHITE : new Color(245, 247, 250);

                addPdfCell(table, String.valueOf(i + 1), dataFont, bgColor);
                addPdfCell(table, c.getClientName(), dataFont, bgColor);
                addPdfCell(table, c.getCompanyName(), dataFont, bgColor);
                addPdfCell(table, c.getEmail(), dataFont, bgColor);
                addPdfCell(table, c.getPhone() != null ? c.getPhone() : "-", dataFont, bgColor);
                addPdfCell(table, c.getIndustry() != null ? c.getIndustry() : "-", dataFont, bgColor);
                addPdfCell(table, c.getCity() != null ? c.getCity() : "-", dataFont, bgColor);
                addPdfCell(table, c.getPicName(), dataFont, bgColor);
                addPdfCell(table, c.getStatus().name(), dataFont, bgColor);
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    // ==================== APPROVAL REPORT ====================

    private byte[] generateApprovalReport(DownloadReportRequest request) {
        List<ContractApproval> approvals = approvalRepository.findAll();

        if (request.getFormat() == ReportFormat.EXCEL) {
            return generateApprovalExcel(approvals);
        } else {
            return generateApprovalPdf(approvals);
        }
    }

    private byte[] generateApprovalExcel(List<ContractApproval> approvals) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Approval Report");
            String[] headers = {"No", "Contract Number", "Contract Title", "Approval Level",
                    "Approver Role", "Approver", "Status", "Requested By", "Requested At", "Responded At"};

            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            for (int i = 0; i < approvals.size(); i++) {
                ContractApproval a = approvals.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(i + 1);
                row.createCell(1).setCellValue(a.getContract().getContractNumber());
                row.createCell(2).setCellValue(a.getContract().getContractTitle());
                row.createCell(3).setCellValue(a.getApprovalLevel());
                row.createCell(4).setCellValue(a.getApproverRole().name());
                row.createCell(5).setCellValue(a.getApprover() != null ? a.getApprover().getFullName() : "-");
                row.createCell(6).setCellValue(a.getStatus().name());
                row.createCell(7).setCellValue(a.getRequestedBy().getFullName());
                row.createCell(8).setCellValue(a.getRequestedAt() != null ? a.getRequestedAt().format(dtf) : "-");
                row.createCell(9).setCellValue(a.getRespondedAt() != null ? a.getRespondedAt().format(dtf) : "-");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel report", e);
        }
    }

    private byte[] generateApprovalPdf(List<ContractApproval> approvals) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD, new Color(146, 64, 14));
            Paragraph title = new Paragraph("Approval Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(12);
            document.add(title);

            Font subtitleFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.GRAY);
            Paragraph subtitle = new Paragraph("Generated on " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")), subtitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(20);
            document.add(subtitle);

            PdfPTable table = new PdfPTable(new float[]{1, 2.5f, 3, 1.5f, 2, 2.5f, 1.5f, 2.5f, 2.5f, 2.5f});
            table.setWidthPercentage(100);

            String[] headers = {"No", "Contract No", "Title", "Level", "Role",
                    "Approver", "Status", "Requested By", "Requested At", "Responded At"};
            Font headerFont = new Font(Font.HELVETICA, 7, Font.BOLD, Color.WHITE);

            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(new Color(146, 64, 14));
                cell.setPadding(5);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            Font dataFont = new Font(Font.HELVETICA, 6.5f, Font.NORMAL);
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");

            for (int i = 0; i < approvals.size(); i++) {
                ContractApproval a = approvals.get(i);
                Color bgColor = (i % 2 == 0) ? Color.WHITE : new Color(245, 247, 250);

                addPdfCell(table, String.valueOf(i + 1), dataFont, bgColor);
                addPdfCell(table, a.getContract().getContractNumber(), dataFont, bgColor);
                addPdfCell(table, a.getContract().getContractTitle(), dataFont, bgColor);
                addPdfCell(table, String.valueOf(a.getApprovalLevel()), dataFont, bgColor);
                addPdfCell(table, a.getApproverRole().name(), dataFont, bgColor);
                addPdfCell(table, a.getApprover() != null ? a.getApprover().getFullName() : "-", dataFont, bgColor);
                addPdfCell(table, a.getStatus().name(), dataFont, bgColor);
                addPdfCell(table, a.getRequestedBy().getFullName(), dataFont, bgColor);
                addPdfCell(table, a.getRequestedAt() != null ? a.getRequestedAt().format(dtf) : "-", dataFont, bgColor);
                addPdfCell(table, a.getRespondedAt() != null ? a.getRespondedAt().format(dtf) : "-", dataFont, bgColor);
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    // ==================== REVENUE REPORT ====================

    private byte[] generateRevenueReport(DownloadReportRequest request) {
        LocalDate fromDate = request.getDateRangeStart() != null
                ? request.getDateRangeStart()
                : LocalDate.now().minusMonths(12);

        List<Object[]> monthlyRevenue = contractRepository.findMonthlyRevenue(fromDate);

        if (request.getFormat() == ReportFormat.EXCEL) {
            return generateRevenueExcel(monthlyRevenue);
        } else {
            return generateRevenuePdf(monthlyRevenue);
        }
    }

    private byte[] generateRevenueExcel(List<Object[]> monthlyRevenue) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Revenue Report");
            String[] headers = {"No", "Month", "Total Contracts", "Total Value"};

            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (int i = 0; i < monthlyRevenue.size(); i++) {
                Object[] data = monthlyRevenue.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(i + 1);
                row.createCell(1).setCellValue(data[0].toString());
                row.createCell(2).setCellValue(((Number) data[2]).longValue());
                row.createCell(3).setCellValue(((BigDecimal) data[1]).doubleValue());
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel report", e);
        }
    }

    private byte[] generateRevenuePdf(List<Object[]> monthlyRevenue) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD, new Color(91, 33, 182));
            Paragraph title = new Paragraph("Revenue Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(12);
            document.add(title);

            Font subtitleFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.GRAY);
            Paragraph subtitle = new Paragraph("Generated on " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")), subtitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(20);
            document.add(subtitle);

            PdfPTable table = new PdfPTable(new float[]{1, 3, 2, 4});
            table.setWidthPercentage(100);

            String[] headers = {"No", "Month", "Contracts", "Total Value"};
            Font headerFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);

            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(new Color(91, 33, 182));
                cell.setPadding(6);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            Font dataFont = new Font(Font.HELVETICA, 8, Font.NORMAL);
            for (int i = 0; i < monthlyRevenue.size(); i++) {
                Object[] data = monthlyRevenue.get(i);
                Color bgColor = (i % 2 == 0) ? Color.WHITE : new Color(245, 247, 250);

                addPdfCell(table, String.valueOf(i + 1), dataFont, bgColor);
                addPdfCell(table, data[0].toString(), dataFont, bgColor);
                addPdfCell(table, String.valueOf(((Number) data[2]).longValue()), dataFont, bgColor);
                addPdfCell(table, formatCurrency((BigDecimal) data[1]), dataFont, bgColor);
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    // ==================== HELPERS ====================

    private void addPdfCell(PdfPTable table, String text, Font font, Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setPadding(5);
        table.addCell(cell);
    }

    private String formatCurrency(BigDecimal value) {
        return String.format("Rp %,.0f", value);
    }

    private void saveReportLog(DownloadReportRequest request, long fileSize) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        String reportName = switch (request.getReportType()) {
            case CONTRACT -> "Contract Summary Report";
            case CLIENT -> "Client Report";
            case APPROVAL -> "Approval Report";
            case REVENUE -> "Revenue Report";
        };

        ReportLog log = ReportLog.builder()
                .reportName(reportName)
                .reportType(request.getReportType())
                .format(request.getFormat())
                .dateRangeStart(request.getDateRangeStart())
                .dateRangeEnd(request.getDateRangeEnd())
                .fileSize(fileSize)
                .generatedBy(user)
                .generatedAt(LocalDateTime.now())
                .build();

        reportLogRepository.save(log);
    }

    private ReportLogResponse mapToResponse(ReportLog log) {
        return ReportLogResponse.builder()
                .id(log.getId())
                .reportName(log.getReportName())
                .reportType(log.getReportType())
                .format(log.getFormat())
                .dateRangeStart(log.getDateRangeStart())
                .dateRangeEnd(log.getDateRangeEnd())
                .fileSize(log.getFileSize())
                .generatedByName(log.getGeneratedBy().getFullName())
                .generatedByRole(log.getGeneratedBy().getRole().name())
                .generatedAt(log.getGeneratedAt())
                .build();
    }
}
