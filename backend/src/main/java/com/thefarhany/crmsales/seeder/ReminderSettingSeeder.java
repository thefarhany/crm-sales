package com.thefarhany.crmsales.seeder;

import com.thefarhany.crmsales.model.entity.ReminderSetting;
import com.thefarhany.crmsales.repository.ReminderSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(5)
public class ReminderSettingSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ReminderSettingSeeder.class);

    @Autowired
    private ReminderSettingRepository settingRepository;

    @Override
    public void run(String... args) {
        if (settingRepository.count() > 0) {
            log.info("Reminder settings already exist. Skipping seeder.");
            return;
        }

        log.info("Seeding default reminder settings...");

        settingRepository.save(ReminderSetting.builder()
                .settingKey("REMINDER_ENABLED")
                .settingValue("true")
                .description("Aktifkan/nonaktifkan auto reminder system")
                .build());

        settingRepository.save(ReminderSetting.builder()
                .settingKey("REMINDER_DAYS")
                .settingValue("60,30,7,1")
                .description("Hari sebelum expiry untuk kirim reminder (comma separated)")
                .build());

        settingRepository.save(ReminderSetting.builder()
                .settingKey("REMINDER_CRON")
                .settingValue("0 0 8 * * ?")
                .description("Jadwal cron expression - default setiap hari jam 8 pagi")
                .build());

        settingRepository.save(ReminderSetting.builder()
                .settingKey("REMINDER_EMAIL_SUBJECT")
                .settingValue("⚠️ Contract Expiry Reminder - {{contractNumber}}")
                .description("Template subject email reminder")
                .build());

        log.info("✅ Reminder settings seeded successfully (4 records)");
    }
}
