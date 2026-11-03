package com.digimate.support;

import com.digimate.entity.Category;
import com.digimate.entity.HelpRequest;
import com.digimate.entity.User;
import com.digimate.entity.enums.HelperStatus;
import com.digimate.entity.enums.RequestMode;
import com.digimate.entity.enums.RequestStatus;
import com.digimate.entity.enums.Role;

import java.time.LocalDateTime;

/**
 * Fresh, unsaved test entities. Override individual fields with setters and
 * persist dependencies explicitly in the test that needs them.
 */
public final class TestData {

    private TestData() {
    }

    public static User customer() {
        return user("Test Customer", "customer@example.test", HelperStatus.NONE);
    }

    public static User verifiedHelper() {
        return user("Verified Helper", "verified-helper@example.test", HelperStatus.VERIFIED);
    }

    public static User pendingHelper() {
        return user("Pending Helper", "pending-helper@example.test", HelperStatus.PENDING);
    }

    public static Category category() {
        Category category = new Category();
        category.setName("Digital basics");
        return category;
    }

    public static HelpRequest openRequest(User customer, Category category) {
        HelpRequest request = new HelpRequest();
        request.setTitle("Set up a video call");
        request.setDescription("Help me set up a video call on my laptop.");
        request.setCategory(category);
        request.setPreferredTime(LocalDateTime.now().plusDays(1).withNano(0));
        request.setMode(RequestMode.ONLINE);
        request.setStatus(RequestStatus.OPEN);
        request.setCustomer(customer);
        return request;
    }

    private static User user(String fullName, String email, HelperStatus helperStatus) {
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        // A synthetic BCrypt hash for fixtures, never a real account credential.
        user.setPasswordHash("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");
        user.setRole(Role.USER);
        user.setHelperStatus(helperStatus);
        return user;
    }
}
