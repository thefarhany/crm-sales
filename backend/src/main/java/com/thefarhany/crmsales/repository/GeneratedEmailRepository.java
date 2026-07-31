package com.thefarhany.crmsales.repository;

import com.thefarhany.crmsales.model.entity.GeneratedEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GeneratedEmailRepository extends JpaRepository<GeneratedEmail, Long> {
    List<GeneratedEmail> findByContractId(Long contractId);
}
