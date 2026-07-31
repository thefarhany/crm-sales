package com.thefarhany.crmsales.repository;

import com.thefarhany.crmsales.model.entity.ReportLog;
import com.thefarhany.crmsales.model.enums.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ReportLogRepository extends JpaRepository<ReportLog, Long> {

    Page<ReportLog> findAllByOrderByGeneratedAtDesc(Pageable pageable);

    Page<ReportLog> findByGeneratedByIdOrderByGeneratedAtDesc(Long userId, Pageable pageable);

    Long countByGeneratedAtBetween(LocalDateTime from, LocalDateTime to);

    Long countByReportTypeAndGeneratedAtBetween(ReportType reportType,
                                                LocalDateTime from,
                                                LocalDateTime to);

    @Query("SELECT COUNT(r) FROM ReportLog r WHERE r.generatedBy.id = :userId " +
            "AND r.generatedAt BETWEEN :from AND :to")
    Long countByUserAndPeriod(@Param("userId") Long userId,
                              @Param("from") LocalDateTime from,
                              @Param("to") LocalDateTime to);
}
