package com.thefarhany.crmsales.service;

import com.thefarhany.crmsales.exception.BadRequestException;
import com.thefarhany.crmsales.exception.ResourceNotFoundException;
import com.thefarhany.crmsales.model.dto.PageResponse;
import com.thefarhany.crmsales.model.dto.request.*;
import com.thefarhany.crmsales.model.dto.response.ContractCalendarResponse;
import com.thefarhany.crmsales.model.dto.response.ContractResponse;
import com.thefarhany.crmsales.model.dto.response.ContractStatsResponse;
import com.thefarhany.crmsales.model.entity.Client;
import com.thefarhany.crmsales.model.entity.Contract;
import com.thefarhany.crmsales.model.entity.User;
import com.thefarhany.crmsales.model.enums.ContractStatus;
import com.thefarhany.crmsales.model.enums.ContractType;
import com.thefarhany.crmsales.model.enums.RenewalPeriod;
import com.thefarhany.crmsales.model.enums.Role;
import com.thefarhany.crmsales.repository.ClientRepository;
import com.thefarhany.crmsales.repository.ContractRepository;
import com.thefarhany.crmsales.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ContractService {

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApprovalService approvalService;

    @Value("${file.upload-dir:uploads/contracts}")
    private String uploadDir;

    @Transactional
    public ContractResponse createContract(CreateContractRequest request, MultipartFile file) {
        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", request.getClientId()));

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date must be after start date");
        }

        User currentUser = getCurrentUser();
        String contractNumber = generateContractNumber();

        Contract contract = new Contract();
        contract.setClient(client);
        contract.setContractNumber(contractNumber);
        contract.setContractTitle(request.getContractTitle());
        contract.setContractType(request.getContractType());
        contract.setContractValue(request.getContractValue());
        contract.setStartDate(request.getStartDate());
        contract.setEndDate(request.getEndDate());
        contract.setRenewalPeriod(request.getRenewalPeriod() != null ? request.getRenewalPeriod() : RenewalPeriod.ANNUALLY);
        contract.setAutoRenewal(request.getAutoRenewal() != null ? request.getAutoRenewal() : false);
        contract.setStatus(ContractStatus.DRAFT);
        contract.setDescription(request.getDescription());
        contract.setCreatedBy(currentUser);
        contract.setCreatedAt(LocalDateTime.now());
        contract.setUpdatedAt(LocalDateTime.now());

        if (file != null && !file.isEmpty()) {
            String fileUrl = saveContractFile(contractNumber, file);
            contract.setContractFileUrl(fileUrl);
        }

        Contract savedContract = contractRepository.save(contract);

        if (isContractComplete(savedContract)) {
            approvalService.createApprovalRecords(savedContract, currentUser);
            savedContract = contractRepository.findById(savedContract.getId()).orElse(savedContract);
        }

        return mapToContractResponse(savedContract);
    }

    public PageResponse<ContractResponse> getAllContracts(ContractStatus status, ContractType contractType,
                                                          Long clientId, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Contract> contractPage = contractRepository.findByFilters(status, contractType, clientId, keyword, pageable);

        List<ContractResponse> responses = contractPage.getContent().stream()
                .map(this::mapToContractResponse)
                .collect(Collectors.toList());

        return PageResponse.<ContractResponse>builder()
                .content(responses)
                .pageNumber(contractPage.getNumber())
                .pageSize(contractPage.getSize())
                .totalElements(contractPage.getTotalElements())
                .totalPages(contractPage.getTotalPages())
                .last(contractPage.isLast())
                .first(contractPage.isFirst())
                .build();
    }

    public ContractResponse getContractById(Long id) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contract", "id", id));
        return mapToContractResponse(contract);
    }

    @Transactional
    public ContractResponse updateContract(Long id, UpdateContractRequest request, MultipartFile file) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contract", "id", id));

        if (contract.getStatus() == ContractStatus.TERMINATED) {
            throw new BadRequestException("Cannot update terminated contract");
        }

        User currentUser = getCurrentUser();
        if (contract.getStatus() != ContractStatus.DRAFT && currentUser.getRole() != Role.ADMIN) {
            throw new BadRequestException("Only DRAFT contracts can be edited");
        }

        if (request.getClientId() != null) {
            Client client = clientRepository.findById(request.getClientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Client", "id", request.getClientId()));
            contract.setClient(client);
        }
        if (request.getContractTitle() != null) {
            contract.setContractTitle(request.getContractTitle());
        }
        if (request.getContractType() != null) {
            contract.setContractType(request.getContractType());
        }
        if (request.getContractValue() != null) {
            contract.setContractValue(request.getContractValue());
        }
        if (request.getStartDate() != null) {
            contract.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            contract.setEndDate(request.getEndDate());
        }
        if (request.getRenewalPeriod() != null) {
            contract.setRenewalPeriod(request.getRenewalPeriod());
        }
        if (request.getAutoRenewal() != null) {
            contract.setAutoRenewal(request.getAutoRenewal());
        }
        if (request.getStatus() != null && currentUser.getRole() == Role.ADMIN) {
            contract.setStatus(request.getStatus());
        }
        if (request.getDescription() != null) {
            contract.setDescription(request.getDescription());
        }

        if (contract.getEndDate().isBefore(contract.getStartDate())) {
            throw new BadRequestException("End date must be after start date");
        }

        if (file != null && !file.isEmpty()) {
            String fileUrl = saveContractFile(contract.getContractNumber(), file);
            contract.setContractFileUrl(fileUrl);
        }

        contract.setUpdatedAt(LocalDateTime.now());
        Contract updatedContract = contractRepository.save(contract);

        if (updatedContract.getStatus() == ContractStatus.DRAFT && isContractComplete(updatedContract)) {
            approvalService.createApprovalRecords(updatedContract, currentUser);
            updatedContract = contractRepository.findById(updatedContract.getId()).orElse(updatedContract);
        }

        return mapToContractResponse(updatedContract);
    }

    @Transactional
    public void deleteContract(Long id) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contract", "id", id));
        contract.setStatus(ContractStatus.TERMINATED);
        contract.setUpdatedAt(LocalDateTime.now());
        contractRepository.save(contract);
    }

    @Transactional
    public ContractResponse uploadContractFile(Long id, MultipartFile file) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contract", "id", id));

        String fileUrl = saveContractFile(contract.getContractNumber(), file);
        contract.setContractFileUrl(fileUrl);
        contract.setUpdatedAt(LocalDateTime.now());

        Contract updatedContract = contractRepository.save(contract);
        return mapToContractResponse(updatedContract);
    }

    @Transactional
    public ContractResponse renewContract(Long id) {
        Contract oldContract = contractRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contract", "id", id));

        if (oldContract.getStatus() != ContractStatus.ACTIVE && oldContract.getStatus() != ContractStatus.EXPIRED) {
            throw new BadRequestException("Only ACTIVE or EXPIRED contracts can be renewed");
        }

        LocalDate newStartDate = oldContract.getEndDate().plusDays(1);
        LocalDate newEndDate;

        switch (oldContract.getRenewalPeriod()) {
            case MONTHLY:
                newEndDate = newStartDate.plusMonths(1).minusDays(1);
                break;
            case QUARTERLY:
                newEndDate = newStartDate.plusMonths(3).minusDays(1);
                break;
            case ANNUALLY:
            default:
                newEndDate = newStartDate.plusYears(1).minusDays(1);
                break;
        }

        User currentUser = getCurrentUser();

        Contract newContract = new Contract();
        newContract.setClient(oldContract.getClient());
        newContract.setContractNumber(generateContractNumber());
        newContract.setContractTitle(oldContract.getContractTitle() + " (Renewal)");
        newContract.setContractType(oldContract.getContractType());
        newContract.setContractValue(oldContract.getContractValue());
        newContract.setStartDate(newStartDate);
        newContract.setEndDate(newEndDate);
        newContract.setRenewalPeriod(oldContract.getRenewalPeriod());
        newContract.setAutoRenewal(oldContract.getAutoRenewal());
        newContract.setStatus(ContractStatus.DRAFT);
        newContract.setDescription("Renewed from contract " + oldContract.getContractNumber());
        newContract.setCreatedBy(currentUser);
        newContract.setCreatedAt(LocalDateTime.now());
        newContract.setUpdatedAt(LocalDateTime.now());

        Contract savedContract = contractRepository.save(newContract);

        if (oldContract.getStatus() == ContractStatus.ACTIVE) {
            oldContract.setStatus(ContractStatus.EXPIRED);
            oldContract.setUpdatedAt(LocalDateTime.now());
            contractRepository.save(oldContract);
        }

        return mapToContractResponse(savedContract);
    }

    public PageResponse<ContractResponse> getExpiringContracts(int days, int page, int size) {
        LocalDate today = LocalDate.now();
        LocalDate targetDate = today.plusDays(days > 0 ? days : 60);
        Pageable pageable = PageRequest.of(page, size, Sort.by("endDate").ascending());
        Page<Contract> contractPage = contractRepository.findExpiringContractsPaged(today, targetDate, pageable);

        List<ContractResponse> responses = contractPage.getContent().stream()
                .map(this::mapToContractResponse)
                .collect(Collectors.toList());

        return PageResponse.<ContractResponse>builder()
                .content(responses)
                .pageNumber(contractPage.getNumber())
                .pageSize(contractPage.getSize())
                .totalElements(contractPage.getTotalElements())
                .totalPages(contractPage.getTotalPages())
                .last(contractPage.isLast())
                .first(contractPage.isFirst())
                .build();
    }

    public List<ContractCalendarResponse> getContractCalendar(LocalDate from, LocalDate to) {
        if (from == null) from = LocalDate.now().withDayOfMonth(1);
        if (to == null) to = from.plusMonths(1).minusDays(1);

        List<Contract> contracts = contractRepository.findContractsForCalendar(from, to);
        return contracts.stream()
                .map(this::mapToCalendarResponse)
                .collect(Collectors.toList());
    }

    public ContractStatsResponse getContractStats() {
        LocalDate today = LocalDate.now();
        return ContractStatsResponse.builder()
                .totalContracts(contractRepository.count())
                .activeContracts(contractRepository.countByStatus(ContractStatus.ACTIVE))
                .draftContracts(contractRepository.countByStatus(ContractStatus.DRAFT))
                .expiredContracts(contractRepository.countByStatus(ContractStatus.EXPIRED))
                .pendingApprovalContracts(contractRepository.countByStatus(ContractStatus.PENDING_APPROVAL))
                .terminatedContracts(contractRepository.countByStatus(ContractStatus.TERMINATED))
                .expiringSoon60Days(contractRepository.countExpiringSoon(today, today.plusDays(60)))
                .expiringSoon30Days(contractRepository.countExpiringSoon(today, today.plusDays(30)))
                .expiringSoon7Days(contractRepository.countExpiringSoon(today, today.plusDays(7)))
                .totalContractValue(contractRepository.sumTotalContractValue())
                .activeContractValue(contractRepository.sumContractValueByStatus(ContractStatus.ACTIVE))
                .build();
    }

    @Transactional
    public List<ContractResponse> bulkUpdateStatus(BulkContractRequest request) {
        if (request.getStatus() == null) {
            throw new BadRequestException("Status is required for bulk update");
        }

        List<Contract> contracts = contractRepository.findByIdIn(request.getContractIds());
        if (contracts.isEmpty()) {
            throw new BadRequestException("No contracts found for the given IDs");
        }

        contracts.forEach(contract -> {
            contract.setStatus(request.getStatus());
            contract.setUpdatedAt(LocalDateTime.now());
        });

        List<Contract> updatedContracts = contractRepository.saveAll(contracts);
        return updatedContracts.stream()
                .map(this::mapToContractResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void bulkDelete(BulkContractRequest request) {
        List<Contract> contracts = contractRepository.findByIdIn(request.getContractIds());
        if (contracts.isEmpty()) {
            throw new BadRequestException("No contracts found for the given IDs");
        }

        contracts.forEach(contract -> {
            contract.setStatus(ContractStatus.TERMINATED);
            contract.setUpdatedAt(LocalDateTime.now());
        });
        contractRepository.saveAll(contracts);
    }

    public PageResponse<ContractResponse> advancedSearch(ContractSearchRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by("createdAt").descending());
        Page<Contract> contractPage = contractRepository.advancedSearch(
                request.getStatus(),
                request.getContractType(),
                request.getClientId(),
                request.getStartDateFrom(),
                request.getStartDateTo(),
                request.getEndDateFrom(),
                request.getEndDateTo(),
                request.getMinValue(),
                request.getMaxValue(),
                request.getKeyword(),
                pageable
        );

        List<ContractResponse> responses = contractPage.getContent().stream()
                .map(this::mapToContractResponse)
                .collect(Collectors.toList());

        return PageResponse.<ContractResponse>builder()
                .content(responses)
                .pageNumber(contractPage.getNumber())
                .pageSize(contractPage.getSize())
                .totalElements(contractPage.getTotalElements())
                .totalPages(contractPage.getTotalPages())
                .last(contractPage.isLast())
                .first(contractPage.isFirst())
                .build();
    }

    public byte[] exportContracts(ContractStatus status, ContractType contractType, String keyword) {
        List<Contract> contracts;
        if (status != null || contractType != null || (keyword != null && !keyword.isEmpty())) {
            Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by("id").ascending());
            Page<Contract> page = contractRepository.findByFilters(status, contractType, null, keyword, pageable);
            contracts = page.getContent();
        } else {
            contracts = contractRepository.findAll(Sort.by("id").ascending());
        }

        StringBuilder csv = new StringBuilder();
        csv.append("ID,Contract Number,Contract Title,Client Name,Company Name,Contract Type,Contract Value," +
                "Start Date,End Date,Renewal Period,Auto Renewal,Status,Created By,Created At\n");

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (Contract c : contracts) {
            csv.append(String.format("%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%.2f,\"%s\",\"%s\",\"%s\",%s,\"%s\",\"%s\",\"%s\"\n",
                    c.getId(),
                    nullSafe(c.getContractNumber()),
                    nullSafe(c.getContractTitle()),
                    nullSafe(c.getClient().getClientName()),
                    nullSafe(c.getClient().getCompanyName()),
                    c.getContractType(),
                    c.getContractValue(),
                    c.getStartDate().format(dateFormatter),
                    c.getEndDate().format(dateFormatter),
                    c.getRenewalPeriod(),
                    c.getAutoRenewal(),
                    c.getStatus(),
                    nullSafe(c.getCreatedBy().getFullName()),
                    c.getCreatedAt()
            ));
        }

        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] csvBytes = csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] result = new byte[bom.length + csvBytes.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(csvBytes, 0, result, bom.length, csvBytes.length);
        return result;
    }

    public PageResponse<ContractResponse> getContractsByClient(Long clientId, int page, int size) {
        clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Contract> contractPage = contractRepository.findByClientId(clientId, pageable);

        List<ContractResponse> responses = contractPage.getContent().stream()
                .map(this::mapToContractResponse)
                .collect(Collectors.toList());

        return PageResponse.<ContractResponse>builder()
                .content(responses)
                .pageNumber(contractPage.getNumber())
                .pageSize(contractPage.getSize())
                .totalElements(contractPage.getTotalElements())
                .totalPages(contractPage.getTotalPages())
                .last(contractPage.isLast())
                .first(contractPage.isFirst())
                .build();
    }

    private String saveContractFile(String contractNumber, MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new BadRequestException("Only PDF files are allowed");
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BadRequestException("File size must not exceed 10MB");
        }

        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileName = contractNumber + "_" + UUID.randomUUID().toString().substring(0, 8) + ".pdf";
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/contracts/" + fileName;
        } catch (IOException e) {
            throw new BadRequestException("Failed to upload file: " + e.getMessage());
        }
    }

    private String generateContractNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        String contractNumber = "CTR-" + dateStr + "-" + randomStr;

        while (contractRepository.existsByContractNumber(contractNumber)) {
            randomStr = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            contractNumber = "CTR-" + dateStr + "-" + randomStr;
        }
        return contractNumber;
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    private ContractResponse mapToContractResponse(Contract contract) {
        long daysUntilExpiry = ChronoUnit.DAYS.between(LocalDate.now(), contract.getEndDate());

        return ContractResponse.builder()
                .id(contract.getId())
                .contractNumber(contract.getContractNumber())
                .contractTitle(contract.getContractTitle())
                .contractType(contract.getContractType())
                .contractValue(contract.getContractValue())
                .startDate(contract.getStartDate())
                .endDate(contract.getEndDate())
                .renewalPeriod(contract.getRenewalPeriod())
                .autoRenewal(contract.getAutoRenewal())
                .status(contract.getStatus())
                .contractFileUrl(contract.getContractFileUrl())
                .description(contract.getDescription())
                .client(ContractResponse.ClientInfo.builder()
                        .id(contract.getClient().getId())
                        .clientName(contract.getClient().getClientName())
                        .companyName(contract.getClient().getCompanyName())
                        .email(contract.getClient().getEmail())
                        .picName(contract.getClient().getPicName())
                        .picEmail(contract.getClient().getPicEmail())
                        .build())
                .createdBy(ContractResponse.CreatorInfo.builder()
                        .id(contract.getCreatedBy().getId())
                        .fullName(contract.getCreatedBy().getFullName())
                        .email(contract.getCreatedBy().getEmail())
                        .build())
                .daysUntilExpiry(daysUntilExpiry)
                .isExpiringSoon(daysUntilExpiry <= 60 && daysUntilExpiry >= 0)
                .createdAt(contract.getCreatedAt())
                .updatedAt(contract.getUpdatedAt())
                .build();
    }

    private ContractCalendarResponse mapToCalendarResponse(Contract contract) {
        long daysUntilExpiry = ChronoUnit.DAYS.between(LocalDate.now(), contract.getEndDate());

        return ContractCalendarResponse.builder()
                .id(contract.getId())
                .contractNumber(contract.getContractNumber())
                .contractTitle(contract.getContractTitle())
                .clientName(contract.getClient().getClientName())
                .companyName(contract.getClient().getCompanyName())
                .startDate(contract.getStartDate())
                .endDate(contract.getEndDate())
                .status(contract.getStatus())
                .daysUntilExpiry(daysUntilExpiry)
                .build();
    }

    private boolean isContractComplete(Contract contract) {
        return contract.getContractTitle() != null && !contract.getContractTitle().isBlank()
                && contract.getClient() != null
                && contract.getContractType() != null
                && contract.getContractValue() != null
                && contract.getStartDate() != null
                && contract.getEndDate() != null;
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

}
