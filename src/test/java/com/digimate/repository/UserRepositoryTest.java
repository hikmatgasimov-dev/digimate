package com.digimate.repository;

import com.digimate.entity.User;
import com.digimate.entity.enums.HelperStatus;
import com.digimate.support.TestData;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

/** Integration tests for account queries and the database email constraint. */
@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldFindUserByStoredEmail() {
        User customer = userRepository.save(TestData.customer());
        userRepository.saveAndFlush(TestData.verifiedHelper());
        entityManager.clear();

        User found = userRepository.findByEmail(customer.getEmail()).orElseThrow();

        assertEquals(customer.getId(), found.getId());
        assertEquals(customer.getEmail(), found.getEmail());
        assertEquals(customer.getFullName(), found.getFullName());
    }

    @Test
    void shouldReturnEmptyWhenEmailDoesNotExist() {
        userRepository.saveAndFlush(TestData.customer());
        entityManager.clear();

        Optional<User> found = userRepository.findByEmail("missing@example.test");

        assertTrue(found.isEmpty());
    }

    @Test
    void shouldReportExistingEmail() {
        User customer = userRepository.saveAndFlush(TestData.customer());
        entityManager.clear();

        boolean exists = userRepository.existsByEmail(customer.getEmail());

        assertTrue(exists);
    }

    @Test
    void shouldReportMissingEmail() {
        userRepository.saveAndFlush(TestData.customer());
        entityManager.clear();

        boolean exists = userRepository.existsByEmail("missing@example.test");

        assertFalse(exists);
    }

    @Test
    void shouldFindOnlyPendingHelpers() {
        userRepository.save(TestData.customer());
        userRepository.save(TestData.verifiedHelper());
        User firstPending = userRepository.save(TestData.pendingHelper());
        User secondPending = TestData.pendingHelper();
        secondPending.setEmail("another.pending@example.test");
        userRepository.saveAndFlush(secondPending);
        entityManager.clear();

        List<User> pending = userRepository.findByHelperStatus(HelperStatus.PENDING);

        assertEquals(2, pending.size());
        assertEquals(java.util.Set.of(firstPending.getId(), secondPending.getId()),
                pending.stream().map(User::getId).collect(java.util.stream.Collectors.toSet()));
        assertTrue(pending.stream().allMatch(user -> user.getHelperStatus() == HelperStatus.PENDING));
    }

    @Test
    void shouldReturnEmptyWhenNoHelpersArePending() {
        userRepository.save(TestData.customer());
        userRepository.saveAndFlush(TestData.verifiedHelper());
        entityManager.clear();

        List<User> pending = userRepository.findByHelperStatus(HelperStatus.PENDING);

        assertTrue(pending.isEmpty());
    }

    @Test
    void shouldRejectDuplicateStoredEmail() {
        userRepository.saveAndFlush(TestData.customer());
        User duplicate = TestData.customer();

        assertThrows(DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(duplicate));
    }
}
