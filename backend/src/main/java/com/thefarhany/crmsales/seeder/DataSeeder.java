package com.thefarhany.crmsales.seeder;

import com.thefarhany.crmsales.model.entity.User;
import com.thefarhany.crmsales.model.enums.Role;
import com.thefarhany.crmsales.model.enums.UserStatus;
import com.thefarhany.crmsales.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        seedAdminUser();
    }

    /**
     * Create default ADMIN user jika belum ada
     */
    private void seedAdminUser() {
        if (userRepository.existsByUsername("admin")) {
            logger.info("Admin user already exists. Skipping seeding.");
            return;
        }

        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@crmsales.com");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setFullName("Administrator");
        admin.setPhone("08123456789");
        admin.setRole(Role.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());

        userRepository.save(admin);

        logger.info("============================================");
        logger.info("✅ Default ADMIN user created successfully!");
        logger.info("============================================");
        logger.info("Username: admin");
        logger.info("Password: admin123");
        logger.info("Email: admin@crmsales.com");
        logger.info("Role: ADMIN");
        logger.info("============================================");
        logger.info("⚠️  PLEASE CHANGE THE DEFAULT PASSWORD!");
        logger.info("============================================");
    }
}
