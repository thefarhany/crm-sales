package com.thefarhany.crmsales.repository;

import com.thefarhany.crmsales.model.entity.Client;
import com.thefarhany.crmsales.model.enums.ClientStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByEmail(String email);

    Boolean existsByEmail(String email);

    Boolean existsByCompanyName(String companyName);

    List<Client> findByClientNameContainingIgnoreCase(String name);

    Page<Client> findByStatus(ClientStatus status, Pageable pageable);

    List<Client> findByStatus(ClientStatus status);

    @Query("SELECT c FROM Client c WHERE " +
            "(:status IS NULL OR c.status = :status) AND " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(c.clientName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.companyName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.picName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.industry) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Client> findByFilters(
            @Param("status") ClientStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    Long countByStatus(ClientStatus status);

    @Query("SELECT c FROM Client c WHERE " +
            "LOWER(c.clientName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.companyName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Client> searchClients(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Client c WHERE c.createdAt >= :from AND c.createdAt < :to")
    Long countClientsCreatedBetween(@Param("from") java.time.LocalDateTime from,
                                    @Param("to") java.time.LocalDateTime to);

    @Query("SELECT c FROM Client c WHERE " +
            "LOWER(c.companyName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.clientName) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Client> findTop5ByQuery(@Param("query") String query);

}
