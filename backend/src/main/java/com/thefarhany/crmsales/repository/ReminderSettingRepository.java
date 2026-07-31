package com.thefarhany.crmsales.repository;

import com.thefarhany.crmsales.model.entity.ReminderSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReminderSettingRepository extends JpaRepository<ReminderSetting, Long> {

    Optional<ReminderSetting> findBySettingKey(String settingKey);

    boolean existsBySettingKey(String settingKey);
}
