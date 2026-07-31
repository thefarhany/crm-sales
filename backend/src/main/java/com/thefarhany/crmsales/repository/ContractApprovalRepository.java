package com.thefarhany.crmsales.repository;

import com.thefarhany.crmsales.model.entity.ContractApproval;
import com.thefarhany.crmsales.model.enums.ApprovalStatus;
import com.thefarhany.crmsales.model.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContractApprovalRepository extends JpaRepository<ContractApproval, Long> {

    Long countByStatus(ApprovalStatus status);

    List<ContractApproval> findByContractIdOrderByApprovalLevelAsc(Long contractId);

    Optional<ContractApproval> findByContractIdAndApprovalLevel(Long contractId, Integer approvalLevel);

    Page<ContractApproval> findByApproverRoleAndStatus(
            Role approverRole,
            ApprovalStatus status,
            Pageable pageable
    );

    @Query("SELECT a FROM ContractApproval a WHERE " +
            "(:status IS NULL OR a.status = :status) AND " +
            "(:approverRole IS NULL OR a.approverRole = :approverRole) AND " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(a.contract.contractNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(a.contract.contractTitle) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(a.contract.client.companyName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<ContractApproval> findByFilters(
            @Param("status") ApprovalStatus status,
            @Param("approverRole") Role approverRole,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("SELECT a FROM ContractApproval a WHERE " +
            "a.approverRole = :role AND " +
            "a.status = 'PENDING' AND " +
            "a.approvalLevel = (" +
            "  SELECT MIN(a2.approvalLevel) FROM ContractApproval a2 " +
            "  WHERE a2.contract = a.contract AND a2.status = 'PENDING'" +
            ")")
    Page<ContractApproval> findActionablePendingByRole(
            @Param("role") Role role,
            Pageable pageable
    );

    Page<ContractApproval> findByApproverId(Long approverId, Pageable pageable);

    Page<ContractApproval> findByRequestedById(Long requestedById, Pageable pageable);

    boolean existsByContractIdAndStatus(Long contractId, ApprovalStatus status);

    Long countByApproverRoleAndStatus(Role approverRole, ApprovalStatus status);

    void deleteByContractId(Long contractId);
}
