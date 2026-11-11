package com.digimate.dto;

import com.digimate.entity.Category;
import com.digimate.entity.HelpRequest;
import com.digimate.entity.Review;
import com.digimate.entity.User;
import com.digimate.entity.enums.HelperStatus;
import com.digimate.entity.enums.RequestMode;
import com.digimate.entity.enums.RequestStatus;
import com.digimate.entity.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/** Unit tests for response mapping and participant contact visibility. */
class HelpRequestResponseTest {

    private User customer;
    private User helper;
    private HelpRequest request;

    @BeforeEach
    void setUp() {
        customer = user(1L, "Anna Novak", "anna@example.test", "20000001", Role.USER);
        helper = user(2L, "Hans Berg", "hans@example.test", "20000002", Role.USER);
        Category category = new Category();
        category.setId(3L);
        category.setName("Phone");
        request = new HelpRequest();
        request.setId(10L);
        request.setTitle("Install a messaging app");
        request.setDescription("Please help me install WhatsApp.");
        request.setCategory(category);
        request.setPreferredTime(LocalDateTime.of(2030, 11, 7, 12, 0));
        request.setMode(RequestMode.IN_PERSON);
        request.setPlace("City library");
        request.setStatus(RequestStatus.OPEN);
        request.setCreatedAt(Instant.parse("2030-11-05T12:00:00Z"));
        request.setCustomer(customer);
        request.setHelper(helper);
    }

    @Test
    void shouldMapRequestFields() {
        HelpRequestResponse response = HelpRequestResponse.from(request, customer, null);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.title()).isEqualTo("Install a messaging app");
        assertThat(response.description()).isEqualTo("Please help me install WhatsApp.");
        assertThat(response.categoryName()).isEqualTo("Phone");
        assertThat(response.preferredTime()).isEqualTo(request.getPreferredTime());
        assertThat(response.mode()).isEqualTo(RequestMode.IN_PERSON);
        assertThat(response.place()).isEqualTo("City library");
        assertThat(response.status()).isEqualTo(RequestStatus.OPEN);
        assertThat(response.createdAt()).isEqualTo(request.getCreatedAt());
    }

    @ParameterizedTest
    @EnumSource(value = RequestStatus.class, names = {"ACCEPTED", "COMPLETED"})
    void shouldShowHelperContactsToCustomer(RequestStatus status) {
        request.setStatus(status);

        HelpRequestResponse response = HelpRequestResponse.from(request, customer, null);

        assertThat(response.counterpartName()).isEqualTo(helper.getFullName());
        assertThat(response.counterpartPhone()).isEqualTo(helper.getPhone());
        assertThat(response.counterpartEmail()).isEqualTo(helper.getEmail());
    }

    @ParameterizedTest
    @EnumSource(value = RequestStatus.class, names = {"ACCEPTED", "COMPLETED"})
    void shouldShowCustomerContactsToAssignedHelper(RequestStatus status) {
        request.setStatus(status);

        HelpRequestResponse response = HelpRequestResponse.from(request, helper, null);

        assertThat(response.counterpartName()).isEqualTo(customer.getFullName());
        assertThat(response.counterpartPhone()).isEqualTo(customer.getPhone());
        assertThat(response.counterpartEmail()).isEqualTo(customer.getEmail());
    }

    @ParameterizedTest
    @MethodSource("hiddenContactViews")
    void shouldHideContactsWhenViewerIsNotAnEligibleParticipant(RequestStatus status, Long viewerId, Role role) {
        request.setStatus(status);
        User viewer = viewerId == null ? null : user(viewerId, "Other Person", "other@example.test", "20000003", role);

        HelpRequestResponse response = HelpRequestResponse.from(request, viewer, null);

        assertNoContacts(response);
    }

    @Test
    void shouldIdentifyCustomerByIdAcrossDifferentEntityInstances() {
        request.setStatus(RequestStatus.ACCEPTED);
        User detachedCustomer = user(customer.getId(), "Different Name", "different@example.test", "20000009", Role.USER);

        HelpRequestResponse response = HelpRequestResponse.from(request, detachedCustomer, null);

        assertThat(response.counterpartEmail()).isEqualTo(helper.getEmail());
    }

    @Test
    void shouldHideContactsWhenViewerHasNoId() {
        request.setStatus(RequestStatus.ACCEPTED);
        User unsavedViewer = user(null, customer.getFullName(), customer.getEmail(), customer.getPhone(), Role.USER);

        HelpRequestResponse response = HelpRequestResponse.from(request, unsavedViewer, null);

        assertNoContacts(response);
    }

    @Test
    void shouldReturnNoCounterpartWhenNoHelperIsAssigned() {
        request.setStatus(RequestStatus.ACCEPTED);
        request.setHelper(null);

        HelpRequestResponse response = HelpRequestResponse.from(request, customer, null);

        assertNoContacts(response);
    }

    @Test
    void shouldMapReviewFields() {
        Review review = new Review();
        review.setRating(5);
        review.setComment("Clear and patient help.");

        HelpRequestResponse response = HelpRequestResponse.from(request, customer, review);

        assertThat(response.reviewRating()).isEqualTo(5);
        assertThat(response.reviewComment()).isEqualTo("Clear and patient help.");
    }

    @Test
    void shouldReturnNullReviewFieldsWhenReviewIsAbsent() {
        HelpRequestResponse response = HelpRequestResponse.from(request, customer, null);

        assertThat(response.reviewRating()).isNull();
        assertThat(response.reviewComment()).isNull();
    }

    private static Stream<Arguments> hiddenContactViews() {
        Stream<Arguments> inactiveRequests = Stream.of(RequestStatus.OPEN, RequestStatus.CANCELLED)
                .flatMap(status -> Stream.of(
                        arguments(status, 1L, Role.USER), arguments(status, 2L, Role.USER),
                        arguments(status, 3L, Role.ADMIN), arguments(status, 4L, Role.USER),
                        arguments(status, null, Role.USER)));
        Stream<Arguments> nonParticipants = Stream.of(RequestStatus.ACCEPTED, RequestStatus.COMPLETED)
                .flatMap(status -> Stream.of(arguments(status, 3L, Role.ADMIN),
                        arguments(status, 4L, Role.USER), arguments(status, null, Role.USER)));
        return Stream.concat(inactiveRequests, nonParticipants);
    }

    private static User user(Long id, String name, String email, String phone, Role role) {
        User user = new User();
        user.setId(id);
        user.setFullName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setRole(role);
        user.setHelperStatus(HelperStatus.VERIFIED);
        return user;
    }

    private static void assertNoContacts(HelpRequestResponse response) {
        assertThat(response.counterpartName()).isNull();
        assertThat(response.counterpartPhone()).isNull();
        assertThat(response.counterpartEmail()).isNull();
    }
}
