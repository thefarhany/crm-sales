package com.thefarhany.crmsales.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    // ======= COMMON STATS (semua role bisa pakai) =======
    private long totalContracts;
    private long activeContracts;
    private long draftContracts;
    private long expiredContracts;
    private long pendingApprovalContracts;
    private long terminatedContracts;

    private long totalClients;
    private long activeClients;

    private long totalUsers;
    private long activeUsers;

    private long pendingApprovals;

    private BigDecimal totalContractValue;
    private BigDecimal activeContractValue;

    private long expiringSoon7Days;
    private long expiringSoon30Days;
    private long expiringSoon60Days;

    private List<UserRoleDistribution> userRolesDistribution;

    private BigDecimal clientGrowthPercentage;
    private long newClientsThisMonth;

    private long teamMemberCount;

    private long myContracts;
    private long myDraftContracts;
    private BigDecimal myMonthSales;

    private List<MonthlyRevenue> monthlyRevenue;
    private List<ContractTypeDistribution> contractTypeDistribution;
    private List<ContractStatusDistribution> contractStatusDistribution;
    private List<TopClient> topClients;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyRevenue {
        private String month;
        private BigDecimal value;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContractTypeDistribution {
        private String type;
        private long count;
        private BigDecimal value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContractStatusDistribution {
        private String status;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopClient {
        private Long clientId;
        private String companyName;
        private long contractCount;
        private BigDecimal totalValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserRoleDistribution {
        private String role;
        private long count;
    }
}
