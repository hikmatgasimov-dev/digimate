package com.digimate.config;

import com.digimate.entity.Category;
import com.digimate.entity.User;
import com.digimate.entity.enums.HelperStatus;
import com.digimate.entity.enums.Role;
import com.digimate.repository.CategoryRepository;
import com.digimate.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    static final List<String> CATEGORIES = List.of(
            "Health",
            "Government services",
            "Banking",
            "Phone",
            "Computer",
            "Online safety"
    );

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_BYTES = 72;

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final String demoPassword;

    public DataInitializer(UserRepository userRepository,
                           CategoryRepository categoryRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${DEMO_PASSWORD:}") String demoPassword) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.demoPassword = demoPassword;
    }

    @Override
    @Transactional
    public void run(String @NonNull ... args) {
        int categoriesCreated = 0;
        for (String name : CATEGORIES) {
            if (addCategory(name)) {
                categoriesCreated++;
            }
        }

        if (!isValidDemoPassword()) {
            log.warn("DEMO_PASSWORD not set (min. 8/ max. 72 characters): demo accounts skipped, {} categories created",
                    categoriesCreated);
            return;
        }

        int usersCreated = 0;
        if (addUser("Demo Admin", "admin@digimate.test", "+37060000000", Role.ADMIN, HelperStatus.NONE)) {
            usersCreated++;
        }
        if (addUser("Verified Helper", "helper@digimate.test", "+37060000001", Role.USER, HelperStatus.VERIFIED)) {
            usersCreated++;
        }
        if (addUser("Pending Helper", "pending@digimate.test", "+37060000002", Role.USER, HelperStatus.PENDING)) {
            usersCreated++;
        }
        if (addUser("Customer One", "customer1@digimate.test", "+37060000003", Role.USER, HelperStatus.NONE)) {
            usersCreated++;
        }
        if (addUser("Customer Two", "customer2@digimate.test", "+37060000004", Role.USER, HelperStatus.NONE)) {
            usersCreated++;
        }
        if (addUser("Verified Helper Two", "helper2@digimate.test", "+37060000005", Role.USER, HelperStatus.VERIFIED)) {
            usersCreated++;
        }
        if (addUser("Customer Three", "customer3@digimate.test", "+37060000006", Role.USER, HelperStatus.NONE)) {
            usersCreated++;
        }

        log.info("Demo data ready: {} users and {} categories created", usersCreated, categoriesCreated);
    }

    private boolean isValidDemoPassword() {
        return demoPassword.length() >= MIN_PASSWORD_LENGTH
                && demoPassword.getBytes(StandardCharsets.UTF_8).length <= MAX_PASSWORD_BYTES;
    }

    private boolean addCategory(String name) {
        if (categoryRepository.existsByName(name)) {
            return false;
        }
        Category category = new Category();
        category.setName(name);
        categoryRepository.save(category);
        return true;
    }

    private boolean addUser(String fullName, String email, String phone, Role role, HelperStatus helperStatus) {
        if (userRepository.existsByEmail(email)) {
            return false;
        }
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(demoPassword));
        user.setRole(role);
        user.setHelperStatus(helperStatus);
        userRepository.save(user);
        return true;
    }
}