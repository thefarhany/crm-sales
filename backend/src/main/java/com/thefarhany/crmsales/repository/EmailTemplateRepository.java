package com.thefarhany.crmsales.repository;

import com.thefarhany.crmsales.model.entity.EmailTemplate;
import com.thefarhany.crmsales.model.enums.EmailTemplateType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {
    Optional<EmailTemplate> findByTemplateType(EmailTemplateType templateType);
}
