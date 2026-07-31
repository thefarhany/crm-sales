package com.thefarhany.crmsales.service;

import com.thefarhany.crmsales.exception.ResourceNotFoundException;
import com.thefarhany.crmsales.model.dto.response.DashboardResponse;
import com.thefarhany.crmsales.model.entity.User;
import com.thefarhany.crmsales.model.enums.*;
import com.thefarhany.crmsales.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContractApprovalRepository approvalRepository;

    public DashboardResponse getDashboardData() {
        User currentUser = getCurrentUser();
        LocalDate today = LocalDate.now();

        // ======= COMMON STATS =======
        long totalContracts = contractRepository.count();
        long activeContracts = safeCount(contractRepository.countByStatus(ContractStatus.ACTIVE));
        long draftContracts = safeCount(contractRepository.countByStatus(ContractStatus.DRAFT));
        long expiredContracts = safeCount(contractRepository.countByStatus(ContractStatus.EXPIRED));
        long pendingApprovalContracts = safeCount(contractRepository.countByStatus(ContractStatus.PENDING_APPROVAL));
        long terminatedContracts = safeCount(contractRepository.countByStatus(ContractStatus.TERMINATED));

        long totalClients = clientRepository.count();
        long activeClients = safeCount(clientRepository.countByStatus(ClientStatus.ACTIVE));

        long totalUsers = userRepository.count();
        long activeUsers = safeCount(userRepository.countByStatus(UserStatus.ACTIVE));

        long pendingApprovals = safeCount(approvalRepository.countByStatus(ApprovalStatus.PENDING));

        BigDecimal totalContractValue = safeBigDecimal(contractRepository.sumTotalContractValue());
        BigDecimal activeContractValue = safeBigDecimal(contractRepository.sumContractValueByStatus(ContractStatus.ACTIVE));

        long expiringSoon7 = safeCount(contractRepository.countExpiringSoon(today, today.plusDays(7)));
        long expiringSoon30 = safeCount(contractRepository.countExpiringSoon(today, today.plusDays(30)));
        long expiringSoon60 = safeCount(contractRepository.countExpiringSoon(today, today.plusDays(60)));

        // ======= CHART DATA =======
        LocalDate twelveMonthsAgo = today.minusMonths(12);

        List<DashboardResponse.MonthlyRevenue> monthlyRevenue;
        if (currentUser.getRole() == Role.MARKETING_TEAM) {
            monthlyRevenue = contractRepository.findMonthlyRevenueByUser(
                    currentUser.getId(), twelveMonthsAgo
            ).stream().map(this::mapMonthlyRevenue).collect(Collectors.toList());
        } else {
            monthlyRevenue = contractRepository.findMonthlyRevenue(twelveMonthsAgo)
                    .stream().map(this::mapMonthlyRevenue).collect(Collectors.toList());
        }

        List<DashboardResponse.ContractTypeDistribution> typeDistribution =
                contractRepository.findContractTypeDistribution().stream()
                        .map(row -> DashboardResponse.ContractTypeDistribution.builder()
                                .type(row[0].toString())
                                .count(((Number) row[1]).longValue())
                                .value(toBigDecimal(row[2]))
                                .build())
                        .collect(Collectors.toList());

        List<DashboardResponse.ContractStatusDistribution> statusDistribution =
                contractRepository.findContractStatusDistribution().stream()
                        .map(row -> DashboardResponse.ContractStatusDistribution.builder()
                                .status(row[0].toString())
                                .count(((Number) row[1]).longValue())
                                .build())
                        .collect(Collectors.toList());

        List<DashboardResponse.TopClient> topClients =
                contractRepository.findTopClients(PageRequest.of(0, 5)).stream()
                        .map(row -> DashboardResponse.TopClient.builder()
                                .clientId(((Number) row[0]).longValue())
                                .companyName((String) row[1])
                                .contractCount(((Number) row[2]).longValue())
                                .totalValue(toBigDecimal(row[3]))
                                .build())
                        .collect(Collectors.toList());

        List<DashboardResponse.UserRoleDistribution> userRolesDistribution =
                userRepository.findUserRoleDistribution().stream()
                        .map(row -> DashboardResponse.UserRoleDistribution.builder()
                                .role(row[0].toString())
                                .count(((Number) row[1]).longValue())
                                .build())
                        .collect(Collectors.toList());

        LocalDate firstDayThisMonth = today.withDayOfMonth(1);
        LocalDate firstDayLastMonth = firstDayThisMonth.minusMonths(1);

        long clientsThisMonth = safeCount(clientRepository.countClientsCreatedBetween(
                firstDayThisMonth.atStartOfDay(), LocalDateTime.now()));
        long clientsLastMonth = safeCount(clientRepository.countClientsCreatedBetween(
                firstDayLastMonth.atStartOfDay(), firstDayThisMonth.atStartOfDay()));

        BigDecimal clientGrowthPercentage = BigDecimal.ZERO;
        if (clientsLastMonth > 0) {
            clientGrowthPercentage = BigDecimal.valueOf(clientsThisMonth - clientsLastMonth)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(clientsLastMonth), 1, RoundingMode.HALF_UP);
        }

        long teamMemberCount = safeCount(userRepository.countByStatus(UserStatus.ACTIVE));

        long myContracts = safeCount(contractRepository.countByCreatedById(currentUser.getId()));
        long myDraftContracts = safeCount(contractRepository.countByCreatedByIdAndStatus(
                currentUser.getId(), ContractStatus.DRAFT));

        LocalDate firstOfMonth = today.withDayOfMonth(1);
        LocalDate lastOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        BigDecimal myMonthSales = safeBigDecimal(contractRepository.sumContractValueByUser(
                currentUser.getId(), firstOfMonth, lastOfMonth));

        return DashboardResponse.builder()
                .totalContracts(totalContracts)
                .activeContracts(activeContracts)
                .draftContracts(draftContracts)
                .expiredContracts(expiredContracts)
                .pendingApprovalContracts(pendingApprovalContracts)
                .terminatedContracts(terminatedContracts)
                .totalClients(totalClients)
                .activeClients(activeClients)
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .pendingApprovals(pendingApprovals)
                .totalContractValue(totalContractValue)
                .activeContractValue(activeContractValue)
                .expiringSoon7Days(expiringSoon7)
                .expiringSoon30Days(expiringSoon30)
                .expiringSoon60Days(expiringSoon60)
                .userRolesDistribution(userRolesDistribution)
                .clientGrowthPercentage(clientGrowthPercentage)
                .newClientsThisMonth(clientsThisMonth)
                .teamMemberCount(teamMemberCount)
                .myContracts(myContracts)
                .myDraftContracts(myDraftContracts)
                .myMonthSales(myMonthSales)
                .monthlyRevenue(monthlyRevenue)
                .contractTypeDistribution(typeDistribution)
                .contractStatusDistribution(statusDistribution)
                .topClients(topClients)
                .build();
    }

    // ====== Helpers ======

    private DashboardResponse.MonthlyRevenue mapMonthlyRevenue(Object[] row) {
        return DashboardResponse.MonthlyRevenue.builder()
                .month((String) row[0])
                .value(toBigDecimal(row[1]))
                .count(row[2] != null ? ((Number) row[2]).longValue() : 0)
                .build();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        return new BigDecimal(value.toString());
    }

    private BigDecimal safeBigDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private long safeCount(Long value) {
        return value != null ? value : 0L;
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }
}
