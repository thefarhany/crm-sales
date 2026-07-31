package com.thefarhany.crmsales.service;

import com.thefarhany.crmsales.exception.BadRequestException;
import com.thefarhany.crmsales.exception.ResourceNotFoundException;
import com.thefarhany.crmsales.model.dto.PageResponse;
import com.thefarhany.crmsales.model.dto.request.ApprovalActionRequest;
import com.thefarhany.crmsales.model.dto.response.ApprovalFlowResponse;
import com.thefarhany.crmsales.model.dto.response.ApprovalResponse;
import com.thefarhany.crmsales.model.entity.Contract;
import com.thefarhany.crmsales.model.entity.ContractApproval;
import com.thefarhany.crmsales.model.entity.User;
import com.thefarhany.crmsales.model.enums.ApprovalStatus;
import com.thefarhany.crmsales.model.enums.ContractStatus;
import com.thefarhany.crmsales.model.enums.Role;
import com.thefarhany.crmsales.repository.ContractApprovalRepository;
import com.thefarhany.crmsales.repository.ContractRepository;
import com.thefarhany.crmsales.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ApprovalService {

    @Autowired
    private ContractApprovalRepository approvalRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Transactional
    public ApprovalFlowResponse requestApproval(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract", "id", contractId));

        if (contract.getStatus() != ContractStatus.DRAFT) {
            throw new BadRequestException("Only DRAFT contracts can be submitted for approval");
        }

        if (approvalRepository.existsByContractIdAndStatus(contractId, ApprovalStatus.PENDING)) {
            throw new BadRequestException("This contract already has a pending approval request");
        }

        validateContractFields(contract);

        User currentUser = getCurrentUser();

        createApprovalRecords(contract, currentUser);

        return getApprovalFlow(contractId);
    }

    @Transactional
    public void createApprovalRecords(Contract contract, User requestedBy) {
        approvalRepository.deleteByContractId(contract.getId());

        ContractApproval level1 = new ContractApproval();
        level1.setContract(contract);
        level1.setApprovalLevel(1);
        level1.setApproverRole(Role.MARKETING_MANAGER);
        level1.setStatus(ApprovalStatus.PENDING);
        level1.setRequestedBy(requestedBy);
        level1.setRequestedAt(LocalDateTime.now());
        approvalRepository.save(level1);

        ContractApproval level2 = new ContractApproval();
        level2.setContract(contract);
        level2.setApprovalLevel(2);
        level2.setApproverRole(Role.BOD);
        level2.setStatus(ApprovalStatus.PENDING);
        level2.setRequestedBy(requestedBy);
        level2.setRequestedAt(LocalDateTime.now());
        approvalRepository.save(level2);

        contract.setStatus(ContractStatus.PENDING_APPROVAL);
        contract.setUpdatedAt(LocalDateTime.now());
        contractRepository.save(contract);

        // Notify Marketing Manager (Level 1)
        notificationService.createNotificationForRole(
                Role.MARKETING_MANAGER,
                com.thefarhany.crmsales.model.enums.NotificationType.APPROVAL_NEEDED,
                "New Approval Request",
                "Contract " + contract.getContractNumber() + " requires your approval.",
                "/approvals"
        );
    }

    private void validateContractFields(Contract contract) {
        StringBuilder missing = new StringBuilder();

        if (contract.getContractTitle() == null || contract.getContractTitle().isBlank())
            missing.append("Contract Title, ");
        if (contract.getClient() == null)
            missing.append("Client, ");
        if (contract.getContractType() == null)
            missing.append("Contract Type, ");
        if (contract.getContractValue() == null)
            missing.append("Contract Value, ");
        if (contract.getStartDate() == null)
            missing.append("Start Date, ");
        if (contract.getEndDate() == null)
            missing.append("End Date, ");

        if (!missing.isEmpty()) {
            missing.setLength(missing.length() - 2);
            throw new BadRequestException("Please complete required fields before submitting: " + missing);
        }
    }

    @Transactional
    public ApprovalResponse approveContract(Long approvalId, ApprovalActionRequest request) {
        ContractApproval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new ResourceNotFoundException("Approval", "id", approvalId));

        if (approval.getStatus() != ApprovalStatus.PENDING) {
            throw new BadRequestException("This approval has already been processed");
        }

        User currentUser = getCurrentUser();

        if (currentUser.getRole() != approval.getApproverRole() && currentUser.getRole() != Role.ADMIN) {
            throw new BadRequestException("You don't have permission to approve at this level");
        }

        if (approval.getApprovalLevel() > 1) {
            ContractApproval previousLevel = approvalRepository
                    .findByContractIdAndApprovalLevel(approval.getContract().getId(), approval.getApprovalLevel() - 1)
                    .orElseThrow(() -> new BadRequestException("Previous approval level not found"));

            if (previousLevel.getStatus() != ApprovalStatus.APPROVED) {
                throw new BadRequestException("Previous approval level has not been approved yet");
            }
        }

        approval.setApprover(currentUser);
        approval.setStatus(ApprovalStatus.APPROVED);
        approval.setRespondedAt(LocalDateTime.now());
        approval.setNotes(request != null ? request.getNotes() : null);
        approvalRepository.save(approval);

        // Notify next level approver or notify owner that it's fully active
        List<ContractApproval> allApprovals = approvalRepository
                .findByContractIdOrderByApprovalLevelAsc(approval.getContract().getId());

        boolean allApproved = allApprovals.stream()
                .allMatch(a -> a.getStatus() == ApprovalStatus.APPROVED);

        if (allApproved) {
            Contract contract = approval.getContract();
            contract.setStatus(ContractStatus.ACTIVE);
            contract.setUpdatedAt(LocalDateTime.now());
            contractRepository.save(contract);

            // Notify Requester
            notificationService.createNotification(
                    approval.getRequestedBy(),
                    com.thefarhany.crmsales.model.enums.NotificationType.APPROVAL_APPROVED,
                    "Contract Approved",
                    "Your contract " + contract.getContractNumber() + " has been fully approved and is now ACTIVE.",
                    "/contracts/" + contract.getId()
            );
        } else {
            // Find next pending level
            ContractApproval nextLevel = allApprovals.stream()
                    .filter(a -> a.getStatus() == ApprovalStatus.PENDING)
                    .findFirst()
                    .orElse(null);

            if (nextLevel != null) {
                notificationService.createNotificationForRole(
                        nextLevel.getApproverRole(),
                        com.thefarhany.crmsales.model.enums.NotificationType.APPROVAL_NEEDED,
                        "Pending Approval",
                        "Contract " + approval.getContract().getContractNumber() + " is waiting for your level " + nextLevel.getApprovalLevel() + " approval.",
                        "/approvals"
                );
            }
        }

        return mapToApprovalResponse(approval);
    }

    @Transactional
    public ApprovalResponse rejectContract(Long approvalId, ApprovalActionRequest request) {
        ContractApproval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new ResourceNotFoundException("Approval", "id", approvalId));

        if (approval.getStatus() != ApprovalStatus.PENDING) {
            throw new BadRequestException("This approval has already been processed");
        }

        if (request == null || request.getRejectionReason() == null || request.getRejectionReason().isBlank()) {
            throw new BadRequestException("Rejection reason is required");
        }

        User currentUser = getCurrentUser();

        if (currentUser.getRole() != approval.getApproverRole() && currentUser.getRole() != Role.ADMIN) {
            throw new BadRequestException("You don't have permission to reject at this level");
        }

        if (approval.getApprovalLevel() > 1) {
            ContractApproval previousLevel = approvalRepository
                    .findByContractIdAndApprovalLevel(approval.getContract().getId(), approval.getApprovalLevel() - 1)
                    .orElse(null);

            if (previousLevel != null && previousLevel.getStatus() != ApprovalStatus.APPROVED) {
                throw new BadRequestException("Previous approval level has not been approved yet");
            }
        }

        approval.setApprover(currentUser);
        approval.setStatus(ApprovalStatus.REJECTED);
        approval.setRespondedAt(LocalDateTime.now());
        approval.setRejectionReason(request.getRejectionReason());
        approval.setNotes(request.getNotes());
        approvalRepository.save(approval);

        Contract contract = approval.getContract();
        contract.setStatus(ContractStatus.DRAFT);
        contract.setUpdatedAt(LocalDateTime.now());
        contractRepository.save(contract);

        // Notify Requester
        notificationService.createNotification(
                approval.getRequestedBy(),
                com.thefarhany.crmsales.model.enums.NotificationType.APPROVAL_REJECTED,
                "Contract Rejected",
                "Your contract " + contract.getContractNumber() + " has been REJECTED. Reason: " + request.getRejectionReason(),
                "/contracts/" + contract.getId() + "/edit"
        );

        return mapToApprovalResponse(approval);
    }

    public PageResponse<ApprovalResponse> getAllApprovals(
            ApprovalStatus status, Role approverRole, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ContractApproval> approvalPage = approvalRepository.findByFilters(status, approverRole, keyword, pageable);
        List<ApprovalResponse> responses = approvalPage.getContent().stream()
                .map(this::mapToApprovalResponse)
                .collect(Collectors.toList());
        return PageResponse.<ApprovalResponse>builder()
                .content(responses)
                .pageNumber(approvalPage.getNumber())
                .pageSize(approvalPage.getSize())
                .totalElements(approvalPage.getTotalElements())
                .totalPages(approvalPage.getTotalPages())
                .last(approvalPage.isLast())
                .first(approvalPage.isFirst())
                .build();
    }

    public ApprovalResponse getApprovalById(Long id) {
        ContractApproval approval = approvalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Approval", "id", id));
        return mapToApprovalResponse(approval);
    }

    public PageResponse<ApprovalResponse> getPendingApprovals(int page, int size) {
        User currentUser = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ContractApproval> approvalPage;
        if (currentUser.getRole() == Role.ADMIN) {
            approvalPage = approvalRepository.findByFilters(ApprovalStatus.PENDING, null, null, pageable);
        } else {
            approvalPage = approvalRepository.findActionablePendingByRole(currentUser.getRole(), pageable);
        }
        List<ApprovalResponse> responses = approvalPage.getContent().stream()
                .map(this::mapToApprovalResponse)
                .collect(Collectors.toList());
        return PageResponse.<ApprovalResponse>builder()
                .content(responses)
                .pageNumber(approvalPage.getNumber())
                .pageSize(approvalPage.getSize())
                .totalElements(approvalPage.getTotalElements())
                .totalPages(approvalPage.getTotalPages())
                .last(approvalPage.isLast())
                .first(approvalPage.isFirst())
                .build();
    }

    public PageResponse<ApprovalResponse> getMyApprovals(int page, int size) {
        User currentUser = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("respondedAt").descending());
        Page<ContractApproval> approvalPage = approvalRepository.findByApproverId(currentUser.getId(), pageable);
        List<ApprovalResponse> responses = approvalPage.getContent().stream()
                .map(this::mapToApprovalResponse)
                .collect(Collectors.toList());
        return PageResponse.<ApprovalResponse>builder()
                .content(responses)
                .pageNumber(approvalPage.getNumber())
                .pageSize(approvalPage.getSize())
                .totalElements(approvalPage.getTotalElements())
                .totalPages(approvalPage.getTotalPages())
                .last(approvalPage.isLast())
                .first(approvalPage.isFirst())
                .build();
    }

    public PageResponse<ApprovalResponse> getApprovalHistory(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("respondedAt").descending());
        Page<ContractApproval> approvalPage = approvalRepository.findAll(pageable);
        List<ApprovalResponse> responses = approvalPage.getContent().stream()
                .map(this::mapToApprovalResponse)
                .collect(Collectors.toList());
        return PageResponse.<ApprovalResponse>builder()
                .content(responses)
                .pageNumber(approvalPage.getNumber())
                .pageSize(approvalPage.getSize())
                .totalElements(approvalPage.getTotalElements())
                .totalPages(approvalPage.getTotalPages())
                .last(approvalPage.isLast())
                .first(approvalPage.isFirst())
                .build();
    }

    public ApprovalFlowResponse getApprovalFlow(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract", "id", contractId));
        List<ContractApproval> approvals = approvalRepository
                .findByContractIdOrderByApprovalLevelAsc(contractId);
        Integer currentLevel = approvals.stream()
                .filter(a -> a.getStatus() == ApprovalStatus.PENDING)
                .map(ContractApproval::getApprovalLevel)
                .min(Integer::compareTo)
                .orElse(null);
        List<ApprovalFlowResponse.ApprovalStep> steps = approvals.stream()
                .map(a -> ApprovalFlowResponse.ApprovalStep.builder()
                        .approvalId(a.getId())
                        .level(a.getApprovalLevel())
                        .approverRole(a.getApproverRole())
                        .approverName(a.getApprover() != null ? a.getApprover().getFullName() : null)
                        .status(a.getStatus())
                        .notes(a.getNotes())
                        .rejectionReason(a.getRejectionReason())
                        .requestedAt(a.getRequestedAt())
                        .respondedAt(a.getRespondedAt())
                        .isCurrentStep(a.getApprovalLevel().equals(currentLevel))
                        .build())
                .collect(Collectors.toList());
        return ApprovalFlowResponse.builder()
                .contractId(contract.getId())
                .contractNumber(contract.getContractNumber())
                .contractTitle(contract.getContractTitle())
                .currentStatus(contract.getStatus().name())
                .steps(steps)
                .build();
    }

    private User getCurrentUser() {
        String username = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    private boolean isActionable(ContractApproval approval) {
        if (approval.getStatus() != ApprovalStatus.PENDING) {
            return false;
        }

        if (approval.getApprovalLevel() == 1) {
            return true;
        }

        Optional<ContractApproval> previousLevel = approvalRepository
                .findByContractIdAndApprovalLevel(
                        approval.getContract().getId(),
                        approval.getApprovalLevel() - 1
                );

        return previousLevel.isPresent()
                && previousLevel.get().getStatus() == ApprovalStatus.APPROVED;
    }

    private ApprovalResponse mapToApprovalResponse(ContractApproval approval) {
        return ApprovalResponse.builder()
                .id(approval.getId())
                .approvalLevel(approval.getApprovalLevel())
                .approverRole(approval.getApproverRole())
                .status(approval.getStatus())
                .rejectionReason(approval.getRejectionReason())
                .notes(approval.getNotes())
                .requestedAt(approval.getRequestedAt())
                .respondedAt(approval.getRespondedAt())
                .createdAt(approval.getCreatedAt())
                .actionable(isActionable(approval))
                .contract(ApprovalResponse.ContractInfo.builder()
                        .id(approval.getContract().getId())
                        .contractNumber(approval.getContract().getContractNumber())
                        .contractTitle(approval.getContract().getContractTitle())
                        .companyName(approval.getContract().getClient().getCompanyName())
                        .clientName(approval.getContract().getClient().getClientName())
                        .build())
                .requestedBy(ApprovalResponse.UserInfo.builder()
                        .id(approval.getRequestedBy().getId())
                        .fullName(approval.getRequestedBy().getFullName())
                        .email(approval.getRequestedBy().getEmail())
                        .role(approval.getRequestedBy().getRole())
                        .build())
                .approver(approval.getApprover() != null
                        ? ApprovalResponse.UserInfo.builder()
                        .id(approval.getApprover().getId())
                        .fullName(approval.getApprover().getFullName())
                        .email(approval.getApprover().getEmail())
                        .role(approval.getApprover().getRole())
                        .build()
                        : null)
                .build();
    }
}
