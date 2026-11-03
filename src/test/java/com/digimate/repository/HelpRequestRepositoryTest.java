package com.digimate.repository;

import com.digimate.entity.Category;
import com.digimate.entity.HelpRequest;
import com.digimate.entity.User;
import com.digimate.entity.enums.RequestMode;
import com.digimate.entity.enums.RequestStatus;
import com.digimate.support.TestData;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = NONE)
class HelpRequestRepositoryTest {

    @Autowired
    private HelpRequestRepository helpRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistAndLoadOpenRequest() {
        User customer = userRepository.save(TestData.customer());
        Category category = categoryRepository.save(TestData.category());
        HelpRequest request = TestData.openRequest(customer, category);
        LocalDateTime preferredTime = LocalDateTime.now().plusDays(2).withNano(0);
        request.setPreferredTime(preferredTime);

        Long requestId = helpRequestRepository.saveAndFlush(request).getId();
        entityManager.clear();

        HelpRequest loaded = helpRequestRepository.findById(requestId).orElseThrow();
        assertEquals("Set up a video call", loaded.getTitle());
        assertEquals("Help me set up a video call on my laptop.", loaded.getDescription());
        assertEquals(RequestStatus.OPEN, loaded.getStatus());
        assertEquals(RequestMode.ONLINE, loaded.getMode());
        assertEquals(preferredTime, loaded.getPreferredTime());
        assertEquals(customer.getId(), loaded.getCustomer().getId());
        assertEquals(category.getId(), loaded.getCategory().getId());
        assertEquals("Digital basics", loaded.getCategory().getName());
        assertNull(loaded.getHelper());
        assertNotNull(loaded.getCreatedAt());
        assertNotNull(loaded.getVersion());
    }
}
