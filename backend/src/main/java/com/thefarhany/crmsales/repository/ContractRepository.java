package com.thefarhany.crmsales.repository;

import com.thefarhany.crmsales.model.entity.Contract;
import com.thefarhany.crmsales.model.enums.ContractStatus;
import com.thefarhany.crmsales.model.enums.ContractType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {

    Optional<Contract> findByContractNumber(String contractNumber);

    List<Contract> findByEndDate(LocalDate endDate);

    List<Contract> findByEndDateBefore(LocalDate date);

    List<Contract> findByEndDateBetweenAndStatusNot(
            LocalDate startDate,
            LocalDate endDate,
            String status
    );

    Boolean existsByContractNumber(String contractNumber);

    @Query("SELECT c FROM Contract c WHERE " +
            "(:status IS NULL OR c.status = :status) AND " +
            "(:contractType IS NULL OR c.contractType = :contractType) AND " +
            "(:clientId IS NULL OR c.client.id = :clientId) AND " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(c.contractNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.contractTitle) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.client.companyName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.client.clientName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Contract> findByFilters(
            @Param("status") ContractStatus status,
            @Param("contractType") ContractType contractType,
            @Param("clientId") Long clientId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("SELECT c FROM Contract c WHERE " +
            "(:status IS NULL OR c.status = :status) AND " +
            "(:contractType IS NULL OR c.contractType = :contractType) AND " +
            "(:clientId IS NULL OR c.client.id = :clientId) AND " +
            "(:startDateFrom IS NULL OR c.startDate >= :startDateFrom) AND " +
            "(:startDateTo IS NULL OR c.startDate <= :startDateTo) AND " +
            "(:endDateFrom IS NULL OR c.endDate >= :endDateFrom) AND " +
            "(:endDateTo IS NULL OR c.endDate <= :endDateTo) AND " +
            "(:minValue IS NULL OR c.contractValue >= :minValue) AND " +
            "(:maxValue IS NULL OR c.contractValue <= :maxValue) AND " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(c.contractNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.contractTitle) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.client.companyName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Contract> advancedSearch(
            @Param("status") ContractStatus status,
            @Param("contractType") ContractType contractType,
            @Param("clientId") Long clientId,
            @Param("startDateFrom") LocalDate startDateFrom,
            @Param("startDateTo") LocalDate startDateTo,
            @Param("endDateFrom") LocalDate endDateFrom,
            @Param("endDateTo") LocalDate endDateTo,
            @Param("minValue") BigDecimal minValue,
            @Param("maxValue") BigDecimal maxValue,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    Page<Contract> findByClientId(Long clientId, Pageable pageable);

    List<Contract> findByClientId(Long clientId);

    Long countByClientId(Long clientId);

    Long countByClientIdAndStatus(Long clientId, ContractStatus status);

    @Query("SELECT c FROM Contract c WHERE " +
            "c.status = 'ACTIVE' AND " +
            "c.endDate BETWEEN :today AND :targetDate " +
            "ORDER BY c.endDate ASC")
    List<Contract> findExpiringContracts(
            @Param("today") LocalDate today,
            @Param("targetDate") LocalDate targetDate
    );

    @Query("SELECT c FROM Contract c WHERE " +
            "c.status = 'ACTIVE' AND " +
            "c.endDate BETWEEN :today AND :targetDate " +
            "ORDER BY c.endDate ASC")
    Page<Contract> findExpiringContractsPaged(
            @Param("today") LocalDate today,
            @Param("targetDate") LocalDate targetDate,
            Pageable pageable
    );

    @Query("SELECT c FROM Contract c WHERE " +
            "(c.startDate BETWEEN :from AND :to) OR " +
            "(c.endDate BETWEEN :from AND :to) OR " +
            "(c.startDate <= :from AND c.endDate >= :to)")
    List<Contract> findContractsForCalendar(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    Long countByStatus(ContractStatus status);

    @Query("SELECT COALESCE(SUM(c.contractValue), 0) FROM Contract c")
    BigDecimal sumTotalContractValue();

    @Query("SELECT COALESCE(SUM(c.contractValue), 0) FROM Contract c WHERE c.status = :status")
    BigDecimal sumContractValueByStatus(@Param("status") ContractStatus status);

    @Query("SELECT COUNT(c) FROM Contract c WHERE " +
            "c.status = 'ACTIVE' AND c.endDate BETWEEN :today AND :targetDate")
    Long countExpiringSoon(
            @Param("today") LocalDate today,
            @Param("targetDate") LocalDate targetDate
    );

    List<Contract> findByStatus(ContractStatus status);

    Page<Contract> findByStatus(ContractStatus status, Pageable pageable);

    List<Contract> findByIdIn(List<Long> ids);

    @Query("SELECT FUNCTION('DATE_FORMAT', c.startDate, '%Y-%m') AS month, " +
            "COALESCE(SUM(c.contractValue), 0) AS value, COUNT(c) AS cnt " +
            "FROM Contract c WHERE c.startDate >= :fromDate AND c.status <> com.thefarhany.crmsales.model.enums.ContractStatus.TERMINATED " +
            "GROUP BY FUNCTION('DATE_FORMAT', c.startDate, '%Y-%m') " +
            "ORDER BY month ASC")
    List<Object[]> findMonthlyRevenue(@Param("fromDate") LocalDate fromDate);

    @Query("SELECT c.contractType AS type, COUNT(c) AS cnt, " +
            "COALESCE(SUM(c.contractValue), 0) AS value " +
            "FROM Contract c WHERE c.status <> com.thefarhany.crmsales.model.enums.ContractStatus.TERMINATED " +
            "GROUP BY c.contractType")
    List<Object[]> findContractTypeDistribution();

    @Query("SELECT c.status AS status, COUNT(c) AS cnt FROM Contract c GROUP BY c.status")
    List<Object[]> findContractStatusDistribution();

    @Query("SELECT c.client.id, c.client.companyName, COUNT(c), " +
            "COALESCE(SUM(c.contractValue), 0) " +
            "FROM Contract c WHERE c.status <> com.thefarhany.crmsales.model.enums.ContractStatus.TERMINATED " +
            "GROUP BY c.client.id, c.client.companyName " +
            "ORDER BY SUM(c.contractValue) DESC")
    List<Object[]> findTopClients(Pageable pageable);

    @Query("SELECT c FROM Contract c WHERE " +
            "LOWER(c.contractNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.contractTitle) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Contract> findTop5ByQuery(@Param("query") String query);

    Long countByCreatedById(Long userId);

    Long countByCreatedByIdAndStatus(Long userId, ContractStatus status);

    @Query("SELECT COALESCE(SUM(c.contractValue), 0) FROM Contract c " +
            "WHERE c.createdBy.id = :userId " +
            "AND c.status <> com.thefarhany.crmsales.model.enums.ContractStatus.TERMINATED " +
            "AND c.startDate >= :fromDate AND c.startDate <= :toDate")
    BigDecimal sumContractValueByUser(@Param("userId") Long userId,
                                      @Param("fromDate") LocalDate fromDate,
                                      @Param("toDate") LocalDate toDate);

    @Query("SELECT FUNCTION('DATE_FORMAT', c.startDate, '%Y-%m') AS month, " +
            "COALESCE(SUM(c.contractValue), 0) AS value, COUNT(c) AS cnt " +
            "FROM Contract c WHERE c.createdBy.id = :userId " +
            "AND c.startDate >= :fromDate " +
            "AND c.status <> com.thefarhany.crmsales.model.enums.ContractStatus.TERMINATED " +
            "GROUP BY FUNCTION('DATE_FORMAT', c.startDate, '%Y-%m') " +
            "ORDER BY month ASC")
    List<Object[]> findMonthlyRevenueByUser(@Param("userId") Long userId,
                                            @Param("fromDate") LocalDate fromDate);

}
