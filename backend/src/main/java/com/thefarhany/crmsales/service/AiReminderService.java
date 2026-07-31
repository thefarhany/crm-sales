package com.thefarhany.crmsales.service;

import com.thefarhany.crmsales.exception.BadRequestException;
import com.thefarhany.crmsales.exception.ResourceNotFoundException;
import com.thefarhany.crmsales.model.dto.PageResponse;
import com.thefarhany.crmsales.model.dto.request.ManualReminderRequest;
import com.thefarhany.crmsales.model.dto.request.UpdateReminderSettingRequest;
import com.thefarhany.crmsales.model.dto.response.AiReminderResponse;
import com.thefarhany.crmsales.model.dto.response.ReminderSettingResponse;
import com.thefarhany.crmsales.model.entity.AiReminder;
import com.thefarhany.crmsales.model.entity.Contract;
import com.thefarhany.crmsales.model.entity.ReminderSetting;
import com.thefarhany.crmsales.model.entity.User;
import com.thefarhany.crmsales.model.enums.ContractStatus;
import com.thefarhany.crmsales.model.enums.ReminderStatus;
import com.thefarhany.crmsales.model.enums.ReminderType;
import com.thefarhany.crmsales.repository.AiReminderRepository;
import com.thefarhany.crmsales.repository.ContractRepository;
import com.thefarhany.crmsales.repository.ReminderSettingRepository;
import com.thefarhany.crmsales.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AiReminderService {

    private static final Logger log = LoggerFactory.getLogger(AiReminderService.class);

    private final RestTemplate restClient = new RestTemplate();

    @Autowired
    private AiReminderRepository reminderRepository;

    @Autowired
    private ReminderSettingRepository settingRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private NotificationService notificationService;

    // ====================================================================
    // QUARTZ JOB — dipanggil oleh ContractReminderJob setiap hari
    // ====================================================================

    @Transactional
    public void processAutoReminders() {
        log.info("=== Starting auto reminder process ===");

        String enabled = getSettingValue("REMINDER_ENABLED");
        if (!"true".equalsIgnoreCase(enabled)) {
            log.info("Reminder system is disabled. Skipping.");
            return;
        }

        String daysStr = getSettingValue("REMINDER_DAYS");
        if (daysStr == null || daysStr.isBlank()) {
            log.warn("REMINDER_DAYS setting is empty. Skipping.");
            return;
        }

        List<Integer> reminderDays = Arrays.stream(daysStr.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toList());

        LocalDate today = LocalDate.now();
        int totalSent = 0;
        int totalFailed = 0;

        for (Integer daysBefore : reminderDays) {
            LocalDate targetExpiryDate = today.plusDays(daysBefore);
            ReminderType type = ReminderType.fromDays(daysBefore);

            List<Contract> expiringContracts = contractRepository.findByEndDate(targetExpiryDate);

            List<Contract> activeExpiring = expiringContracts.stream()
                    .filter(c -> c.getStatus() == ContractStatus.ACTIVE)
                    .collect(Collectors.toList());

            if (activeExpiring.isEmpty()) continue;

            log.info("Day-{}: Found {} active contracts expiring on {}",
                    daysBefore, activeExpiring.size(), targetExpiryDate);

            for (Contract contract : activeExpiring) {
                boolean alreadySent = reminderRepository
                        .existsByContractIdAndReminderTypeAndStatus(
                                contract.getId(), type, ReminderStatus.SENT);

                if (alreadySent) {
                    log.debug("Reminder already sent for contract {} type {}", contract.getContractNumber(), type);
                    continue;
                }

                String clientEmail = resolveClientEmail(contract);
                String ccEmail = contract.getCreatedBy() != null ? contract.getCreatedBy().getEmail() : null;

                boolean success = sendReminderForContract(
                        contract, type, daysBefore,
                        clientEmail, ccEmail, contract.getCreatedBy()
                );

                if (success) totalSent++;
                else totalFailed++;

                addIntervalDelay(60000);
            }
        }

        log.info("=== Auto reminder completed: {} sent, {} failed ===", totalSent, totalFailed);
    }

    public AiReminderResponse generateDraftReminder(ManualReminderRequest request) {
        Contract contract = contractRepository.findById(request.getContractId())
                .orElseThrow(() -> new ResourceNotFoundException("Contract", "id", request.getContractId()));

        long daysBefore = ChronoUnit.DAYS.between(LocalDate.now(), contract.getEndDate());
        String selectedTone = request.getTone() != null ? request.getTone().name() : "PROFESSIONAL";

        String aiOpeningText = fetchAiOpeningParagraph(contract, (int) Math.max(daysBefore, 0), selectedTone);

        return AiReminderResponse.builder()
                .contractId(contract.getId())
                .message(aiOpeningText)
                .build();
    }

    private void addIntervalDelay(long milliseconds) {
        try {
            log.info("Interval delay... sleeping for {} seconds to prevent server overload.", milliseconds / 1000);
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            log.error("Jeda interval terganggu: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    // ====================================================================
    // CORE: Send Reminder For Contract (KHUSUS OTOMATIS/QUARTZ - NON AI)
    // ====================================================================

    private boolean sendReminderForContract(
            Contract contract, ReminderType type, int daysBefore,
            String toEmail, String ccEmail, User internalUser
    ) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("No client email found for contract {}", contract.getContractNumber());
            return false;
        }

        String subject = buildSubject(contract, daysBefore);
        String message = buildHtmlMessage(contract, daysBefore, ccEmail, null);

        AiReminder reminder = AiReminder.builder()
                .contract(contract)
                .reminderType(type)
                .recipientEmail(toEmail)
                .recipientUser(internalUser)
                .subject(subject)
                .message(message)
                .status(ReminderStatus.PENDING)
                .build();

        try {
            sendEmail(toEmail, ccEmail, subject, message);
            reminder.setStatus(ReminderStatus.SENT);
            reminder.setSentAt(LocalDateTime.now());
            reminderRepository.save(reminder);

            // In-app notification for managers
            notificationService.createNotificationForAllManagers(
                    com.thefarhany.crmsales.model.enums.NotificationType.CONTRACT_EXPIRING,
                    "Contract Expiring: " + contract.getContractNumber(),
                    String.format("Contract %s for %s will expire in %d days.", 
                            contract.getContractNumber(), contract.getClient().getCompanyName(), daysBefore),
                    "/contracts/" + contract.getId()
            );

            log.info("✅ Reminder SENT: contract={}, type={}, to={}, cc={}",
                    contract.getContractNumber(), type, toEmail, ccEmail);
            return true;

        } catch (Exception e) {
            reminder.setStatus(ReminderStatus.FAILED);
            reminder.setErrorMessage(e.getMessage());
            reminderRepository.save(reminder);

            log.error("❌ Reminder FAILED: contract={}, error={}",
                    contract.getContractNumber(), e.getMessage());
            return false;
        }
    }

    // ====================================================================
    // REST API — Send Manual Reminder (FULL AI DENGAN TONE)
    // ====================================================================

    @Transactional
    public AiReminderResponse sendManualReminder(ManualReminderRequest request) {
        Contract contract = contractRepository.findById(request.getContractId())
                .orElseThrow(() -> new ResourceNotFoundException("Contract", "id", request.getContractId()));

        User currentUser = getCurrentUser();

        String clientEmail = (request.getRecipientEmail() != null && !request.getRecipientEmail().isBlank())
                ? request.getRecipientEmail()
                : resolveClientEmail(contract);

        if (clientEmail == null || clientEmail.isBlank()) {
            throw new BadRequestException("No client email found for this contract. Please update client PIC email first.");
        }

        String ccEmail = currentUser.getEmail();
        long daysBefore = ChronoUnit.DAYS.between(LocalDate.now(), contract.getEndDate());

        String subject = request.getCustomSubject() != null && !request.getCustomSubject().isBlank()
                ? request.getCustomSubject()
                : buildSubject(contract, (int) Math.max(daysBefore, 0));

        String aiOpeningText;
        if (request.getCustomMessage() != null && !request.getCustomMessage().isBlank()) {
            aiOpeningText = request.getCustomMessage();
        } else {
            String selectedTone = request.getTone() != null ? request.getTone().name() : "PROFESSIONAL";
            aiOpeningText = fetchAiOpeningParagraph(contract, (int) Math.max(daysBefore, 0), selectedTone);
        }

        String message = buildHtmlMessage(contract, (int) Math.max(daysBefore, 0), ccEmail, aiOpeningText);

        AiReminder reminder = AiReminder.builder()
                .contract(contract)
                .reminderType(mapDaysToClosestType(daysBefore))
                .recipientEmail(clientEmail)
                .recipientUser(currentUser)
                .subject(subject)
                .message(message)
                .status(ReminderStatus.PENDING)
                .build();

        try {
            sendEmail(clientEmail, ccEmail, subject, message);
            reminder.setStatus(ReminderStatus.SENT);
            reminder.setSentAt(LocalDateTime.now());
        } catch (Exception e) {
            reminder.setStatus(ReminderStatus.FAILED);
            reminder.setErrorMessage(e.getMessage());
        }

        return mapToResponse(reminderRepository.save(reminder));
    }

    // ====================================================================
    // AI INTEGRATION — API REQUEST KE FASTAPI
    // ====================================================================

    private String fetchAiOpeningParagraph(Contract contract, int daysBefore, String tone) {
        try {
            String note = String.format(
                    "Tuliskan HANYA 1-2 paragraf pembuka email untuk mengingatkan klien bahwa kontrak mereka akan berakhir dalam %d hari. " +
                            "ATURAN SANGAT KETAT: " +
                            "1. JANGAN tulis subject, 'Dear', atau salam penutup. " +
                            "2. JANGAN gunakan tag HTML, SVG, Markdown, atau membuat logo/gambar apapun. " +
                            "3. JANGAN gunakan simbol aneh. " +
                            "HANYA berikan teks paragraf murni.",
                    daysBefore
            );

            Map<String, Object> pythonPayload = Map.of(
                    "client_name", nullSafe(contract.getClient().getPicName()),
                    "company_name", contract.getClient().getCompanyName(),
                    "end_date", contract.getEndDate().toString(),
                    "tone", tone != null ? tone : "PROFESSIONAL",
                    "additional_note", note
            );

            var response = restClient.postForObject(
                    "http://localhost:8000/api/ai/generate",
                    pythonPayload,
                    Map.class
            );

            if (response != null && response.get("content") != null) {
                String rawAiText = (String) response.get("content");
                return sanitizeAiResponse(rawAiText);
            }
        } catch (Exception e) {
            log.error("AI Server gagal merespon untuk kontrak {}: {}", contract.getContractNumber(), e.getMessage());
        }

        return null;
    }

    // ====================================================================
    // EMAIL BUILDER
    // ====================================================================

    private String buildHtmlMessage(Contract contract, int daysBefore, String ccEmail, String aiOpeningText) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");

        String picName = nullSafe(contract.getClient().getPicName());
        String companyName = contract.getClient().getCompanyName();

        String greeting = !picName.equals("-")
                ? String.format("Dear %s (%s),", picName, companyName)
                : String.format("Dear %s,", companyName);

        String ccInfo = (ccEmail != null && !ccEmail.isBlank())
                ? String.format("<p style=\"font-size: 12px; color: #888;\">CC: %s</p>", ccEmail)
                : "";

        String colorHex = daysBefore <= 7 ? "#dc3545" : daysBefore <= 30 ? "#fd7e14" : "#0d6efd";

        String openingParagraph;
        if (aiOpeningText != null && !aiOpeningText.isBlank()) {
            openingParagraph = String.format("<p>%s</p><div style='margin-bottom: 20px; color: #444;'>%s</div>", greeting, aiOpeningText);
        } else {
            openingParagraph = String.format("<p>%s</p><p>This is a reminder that the following contract will expire in <strong style=\"color: %s;\">%d day(s)</strong>:</p>", greeting, colorHex, daysBefore);
        }

        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="background-color: %s; color: white; padding: 15px; border-radius: 8px 8px 0 0;">
                        <h2 style="margin: 0;">⚠️ Contract Expiry Reminder</h2>
                    </div>
                    <div style="border: 1px solid #ddd; border-top: none; padding: 20px; border-radius: 0 0 8px 8px;">
                        
                        %s <table style="width: 100%%; border-collapse: collapse; margin: 15px 0;">
                            <tr style="background-color: #f8f9fa;">
                                <td style="padding: 10px; border: 1px solid #ddd; font-weight: bold; width: 40%%;">Contract Number</td>
                                <td style="padding: 10px; border: 1px solid #ddd;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding: 10px; border: 1px solid #ddd; font-weight: bold;">Contract Title</td>
                                <td style="padding: 10px; border: 1px solid #ddd;">%s</td>
                            </tr>
                            <tr style="background-color: #f8f9fa;">
                                <td style="padding: 10px; border: 1px solid #ddd; font-weight: bold;">Client</td>
                                <td style="padding: 10px; border: 1px solid #ddd;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding: 10px; border: 1px solid #ddd; font-weight: bold;">PIC Name</td>
                                <td style="padding: 10px; border: 1px solid #ddd;">%s</td>
                            </tr>
                            <tr style="background-color: #f8f9fa;">
                                <td style="padding: 10px; border: 1px solid #ddd; font-weight: bold;">Contract Value</td>
                                <td style="padding: 10px; border: 1px solid #ddd;">Rp %,.0f</td>
                            </tr>
                            <tr>
                                <td style="padding: 10px; border: 1px solid #ddd; font-weight: bold;">Start Date</td>
                                <td style="padding: 10px; border: 1px solid #ddd;">%s</td>
                            </tr>
                            <tr style="background-color: #fff3cd;">
                                <td style="padding: 10px; border: 1px solid #ddd; font-weight: bold;">End Date</td>
                                <td style="padding: 10px; border: 1px solid #ddd; font-weight: bold; color: #dc3545;">%s</td>
                            </tr>
                        </table>

                        <p>Please contact us to discuss the renewal of this contract at your earliest convenience.</p>

                        <div style="background-color: #e8f4fd; padding: 12px; border-radius: 6px; margin-top: 15px;">
                            <strong>💡 Need Assistance?</strong> Feel free to reach out to your account manager for renewal details.
                        </div>

                        <hr style="margin-top: 25px; border: none; border-top: 1px solid #eee;">
                        %s
                        <p style="font-size: 12px; color: #999;">
                            This is an automated message from Digi CRM System.<br>
                            Do not reply to this email.
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """,
                colorHex,
                openingParagraph,
                contract.getContractNumber(),
                nullSafe(contract.getContractTitle()),
                companyName,
                picName,
                contract.getContractValue() != null ? contract.getContractValue().doubleValue() : 0,
                contract.getStartDate().format(formatter),
                contract.getEndDate().format(formatter),
                ccInfo
        );
    }

    private void sendEmail(String to, String cc, String subject, String htmlBody) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        if (cc != null && !cc.isBlank()) {
            helper.setCc(cc);
        }
        helper.setSubject(subject);
        helper.setText(htmlBody, true);

        mailSender.send(message);
        log.info("📧 Email sent → To: {}, CC: {}", to, cc != null ? cc : "none");
    }

    private String resolveClientEmail(Contract contract) {
        if (contract.getClient() == null) return null;

        String picEmail = contract.getClient().getPicEmail();
        if (picEmail != null && !picEmail.isBlank()) return picEmail;

        String clientEmail = contract.getClient().getEmail();
        if (clientEmail != null && !clientEmail.isBlank()) return clientEmail;

        return null;
    }

    private String buildSubject(Contract contract, int daysBefore) {
        String template = getSettingValue("REMINDER_EMAIL_SUBJECT");
        if (template == null || template.isBlank()) {
            template = "⚠️ Contract Expiry Reminder - {{contractNumber}}";
        }
        return template
                .replace("{{contractNumber}}", contract.getContractNumber())
                .replace("{{daysBefore}}", String.valueOf(daysBefore))
                .replace("{{clientName}}", contract.getClient().getCompanyName());
    }

    // ====================================================================
    // REST API — GETTERS & SETTERS
    // ====================================================================

    public PageResponse<AiReminderResponse> getAllReminders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AiReminder> reminderPage = reminderRepository.findAllByOrderByCreatedAtDesc(pageable);
        List<AiReminderResponse> responses = reminderPage.getContent().stream().map(this::mapToResponse).collect(Collectors.toList());
        return PageResponse.<AiReminderResponse>builder().content(responses).pageNumber(reminderPage.getNumber()).pageSize(reminderPage.getSize()).totalElements(reminderPage.getTotalElements()).totalPages(reminderPage.getTotalPages()).last(reminderPage.isLast()).first(reminderPage.isFirst()).build();
    }

    public AiReminderResponse getReminderById(Long id) {
        AiReminder reminder = reminderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Reminder", "id", id));
        return mapToResponse(reminder);
    }

    public List<AiReminderResponse> getPendingReminders() {
        return reminderRepository.findPendingReminders().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public PageResponse<AiReminderResponse> getReminderHistory(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AiReminder> reminderPage = reminderRepository.findSentReminders(pageable);
        List<AiReminderResponse> responses = reminderPage.getContent().stream().map(this::mapToResponse).collect(Collectors.toList());
        return PageResponse.<AiReminderResponse>builder().content(responses).pageNumber(reminderPage.getNumber()).pageSize(reminderPage.getSize()).totalElements(reminderPage.getTotalElements()).totalPages(reminderPage.getTotalPages()).last(reminderPage.isLast()).first(reminderPage.isFirst()).build();
    }

    public List<ReminderSettingResponse> getAllSettings() {
        return settingRepository.findAll().stream().map(this::mapToSettingResponse).collect(Collectors.toList());
    }

    @Transactional
    public List<ReminderSettingResponse> updateSettings(List<UpdateReminderSettingRequest> requests) {
        User currentUser = getCurrentUser();
        for (UpdateReminderSettingRequest req : requests) {
            ReminderSetting setting = settingRepository.findBySettingKey(req.getSettingKey()).orElseThrow(() -> new ResourceNotFoundException("ReminderSetting", "settingKey", req.getSettingKey()));
            validateSettingValue(req.getSettingKey(), req.getSettingValue());
            setting.setSettingValue(req.getSettingValue());
            setting.setUpdatedBy(currentUser);
            settingRepository.save(setting);
        }
        return getAllSettings();
    }

    // ====================================================================
    // PRIVATE HELPERS & MAPPERS
    // ====================================================================

    private void validateSettingValue(String key, String value) {
        switch (key) {
            case "REMINDER_ENABLED":
                if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) throw new BadRequestException("REMINDER_ENABLED must be 'true' or 'false'");
                break;
            case "REMINDER_DAYS":
                try {
                    Arrays.stream(value.split(",")).map(String::trim).forEach(d -> {
                        int day = Integer.parseInt(d);
                        if (day <= 0 || day > 365) throw new NumberFormatException();
                    });
                } catch (NumberFormatException e) {
                    throw new BadRequestException("REMINDER_DAYS must be comma-separated positive integers (1-365), e.g. '60,30,7,1'");
                }
                break;
            case "REMINDER_CRON":
                if (value == null || value.isBlank()) throw new BadRequestException("REMINDER_CRON cannot be empty");
                break;
        }
    }

    private String getSettingValue(String key) {
        return settingRepository.findBySettingKey(key).map(ReminderSetting::getSettingValue).orElse(null);
    }

    private ReminderType mapDaysToClosestType(long daysBefore) {
        if (daysBefore >= 60) return ReminderType.DAY_60;
        if (daysBefore >= 30) return ReminderType.DAY_30;
        if (daysBefore >= 7) return ReminderType.DAY_7;
        return ReminderType.DAY_1;
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    private String nullSafe(String value) {
        return value != null ? value : "-";
    }

    private AiReminderResponse mapToResponse(AiReminder r) {
        long daysBeforeExpiry = ChronoUnit.DAYS.between(LocalDate.now(), r.getContract().getEndDate());
        return AiReminderResponse.builder()
                .id(r.getId())
                .contractId(r.getContract().getId())
                .contractNumber(r.getContract().getContractNumber())
                .clientName(r.getContract().getClient().getCompanyName())
                .contractEndDate(r.getContract().getEndDate().toString())
                .reminderType(r.getReminderType())
                .reminderTypeDescription(r.getReminderType().getDescription())
                .daysBeforeExpiry((int) daysBeforeExpiry)
                .recipientEmail(r.getRecipientEmail())
                .recipientUserId(r.getRecipientUser() != null ? r.getRecipientUser().getId() : null)
                .recipientUserName(r.getRecipientUser() != null ? r.getRecipientUser().getFullName() : null)
                .subject(r.getSubject())
                .message(r.getMessage())
                .status(r.getStatus())
                .statusDescription(r.getStatus().getDescription())
                .sentAt(r.getSentAt())
                .errorMessage(r.getErrorMessage())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private ReminderSettingResponse mapToSettingResponse(ReminderSetting s) {
        return ReminderSettingResponse.builder()
                .id(s.getId())
                .settingKey(s.getSettingKey())
                .settingValue(s.getSettingValue())
                .description(s.getDescription())
                .updatedByName(s.getUpdatedBy() != null ? s.getUpdatedBy().getFullName() : null)
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    // ====================================================================
    // AI TEXT SANITIZER - Pembasmi Halusinasi Gambar/Logo AI (VERSI NUKLIR)
    // ====================================================================
    private String sanitizeAiResponse(String rawText) {
        if (rawText == null) return "";

        // 1. BANTAI Markdown Code Blocks (Ini biang keroknya! AI suka ngebungkus SVG pake ```xml ... ```)
        String cleaned = rawText.replaceAll("(?s)```.*?```", "");

        // 2. BANTAI tag SVG murni beserta seluruh isinya
        cleaned = cleaned.replaceAll("(?i)(?s)<svg.*?>.*?</svg>", "");

        // 3. BANTAI sisa-sisa pecahan tag gambar/vektor kalau masih ada
        cleaned = cleaned.replaceAll("(?i)<(img|path|rect|polygon|circle|ellipse|line).*?>", "");

        // 4. BANTAI Markdown format gambar biasa ![alt](url)
        cleaned = cleaned.replaceAll("!\\[.*?\\]\\(.*?\\)", "");

        // 5. Bersihin spasi kosong/enter berlebih di awal dan akhir teks
        return cleaned.trim();
    }
}