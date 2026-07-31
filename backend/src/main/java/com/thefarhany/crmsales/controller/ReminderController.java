package com.thefarhany.crmsales.controller;

import com.thefarhany.crmsales.model.dto.ApiResponse;
import com.thefarhany.crmsales.model.dto.PageResponse;
import com.thefarhany.crmsales.model.dto.request.ManualReminderRequest;
import com.thefarhany.crmsales.model.dto.request.UpdateReminderSettingRequest;
import com.thefarhany.crmsales.model.dto.response.AiReminderResponse;
import com.thefarhany.crmsales.model.dto.response.ReminderSettingResponse;
import com.thefarhany.crmsales.service.AiReminderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reminders")
public class ReminderController {

    @Autowired
    private AiReminderService reminderService;

    /**
     * Get all reminders dengan pagination
     * Access: ADMIN, MARKETING_MANAGER
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'MARKETING_TEAM', 'BOD')")
    public ResponseEntity<ApiResponse<PageResponse<AiReminderResponse>>> getAllReminders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<AiReminderResponse> reminders = reminderService.getAllReminders(page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Reminders retrieved successfully", reminders)
        );
    }

    /**
     * Get reminder by ID
     * Access: ADMIN, MARKETING_MANAGER
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'MARKETING_TEAM', 'BOD')")
    public ResponseEntity<ApiResponse<AiReminderResponse>> getReminderById(@PathVariable Long id) {

        AiReminderResponse reminder = reminderService.getReminderById(id);
        return ResponseEntity.ok(
                ApiResponse.success("Reminder retrieved successfully", reminder)
        );
    }

    /**
     * Get pending reminders
     * Access: ADMIN, MARKETING_MANAGER
     */
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'MARKETING_TEAM', 'BOD')")
    public ResponseEntity<ApiResponse<List<AiReminderResponse>>> getPendingReminders() {

        List<AiReminderResponse> reminders = reminderService.getPendingReminders();
        return ResponseEntity.ok(
                ApiResponse.success("Pending reminders retrieved successfully", reminders)
        );
    }

    /**
     * Send manual reminder
     * Access: MARKETING_TEAM
     */
    @PostMapping("/send-manual")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_TEAM')")
    public ResponseEntity<ApiResponse<AiReminderResponse>> sendManualReminder(
            @Valid @RequestBody ManualReminderRequest request) {

        AiReminderResponse reminder = reminderService.sendManualReminder(request);
        return ResponseEntity.ok(
                ApiResponse.success("Manual reminder sent successfully", reminder)
        );
    }

    /**
     * Get reminder settings
     * Access: ADMIN, MARKETING_MANAGER, MARKETING_TEAM
     */
    @GetMapping("/settings")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'MARKETING_TEAM', 'BOD')")
    public ResponseEntity<ApiResponse<List<ReminderSettingResponse>>> getSettings() {

        List<ReminderSettingResponse> settings = reminderService.getAllSettings();
        return ResponseEntity.ok(
                ApiResponse.success("Reminder settings retrieved successfully", settings)
        );
    }

    /**
     * Update reminder settings
     * Access: ADMIN, MARKETING_MANAGER
     */
    @PutMapping("/settings")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER')")
    public ResponseEntity<ApiResponse<List<ReminderSettingResponse>>> updateSettings(
            @Valid @RequestBody List<UpdateReminderSettingRequest> requests) {

        List<ReminderSettingResponse> settings = reminderService.updateSettings(requests);
        return ResponseEntity.ok(
                ApiResponse.success("Reminder settings updated successfully", settings)
        );
    }

    /**
     * Get reminder history (SENT only)
     * Access: ADMIN, MARKETING_MANAGER, MARKETING_TEAM
     */
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'MARKETING_TEAM', 'BOD')")
    public ResponseEntity<ApiResponse<PageResponse<AiReminderResponse>>> getReminderHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<AiReminderResponse> history = reminderService.getReminderHistory(page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Reminder history retrieved successfully", history)
        );
    }

    @PostMapping("/run-job")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> runReminderJobOnce() {
        reminderService.processAutoReminders();
        return ResponseEntity.ok(
                ApiResponse.success("Reminder job executed successfully.", null)
        );
    }

    /**
     * Generate AI Draft without sending email
     * Access: MARKETING_TEAM, ADMIN
     */
    @PostMapping("/generate-draft")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_TEAM')")
    public ResponseEntity<ApiResponse<AiReminderResponse>> generateDraftReminder(
            @Valid @RequestBody ManualReminderRequest request) {

        AiReminderResponse response = reminderService.generateDraftReminder(request);
        return ResponseEntity.ok(
                ApiResponse.success("AI Draft generated successfully", response)
        );
    }
}
