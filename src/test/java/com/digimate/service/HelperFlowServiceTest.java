

package com.digimate.service;

import com.digimate.dto.HelpRequestResponse;
import com.digimate.entity.Category;
import com.digimate.entity.HelpRequest;
import com.digimate.entity.User;
import com.digimate.entity.enums.HelperStatus;
import com.digimate.entity.enums.RequestMode;
import com.digimate.entity.enums.RequestStatus;
import com.digimate.entity.enums.Role;
import com.digimate.exception.HelperNotVerifiedException;
import com.digimate.repository.HelpRequestRepository;
import com.digimate.repository.ReviewRepository;
import com.digimate.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
@ExtendWith(MockitoExtension.class)
class HelperFlowServiceTest {

    @Mock UserRepository userRepository;
    @Mock HelpRequestRepository helpRequestRepository;
    @Mock ReviewRepository reviewRepository;
    @InjectMocks HelperFlowService service;

    private User customer;
    private User helper;
    private HelpRequest request;

    @BeforeEach
    void setUp() {
        customer = user(1L, "anna@example.test", "Anna Customer", HelperStatus.NONE);
        helper = user(2L, "hans@example.test", "Hans Helper", HelperStatus.VERIFIED);

        Category category = new Category();
        category.setName("Phone");

        request = new HelpRequest();
        request.setTitle("Set up WhatsApp");
        request.setDescription("Help me install WhatsApp.");
        request.setCategory(category);
        request.setPreferredTime(LocalDateTime.of(2030, 11, 10, 15, 0));
        request.setMode(RequestMode.ONLINE);
        request.setStatus(RequestStatus.OPEN);
        request.setCustomer(customer);
    }

    private User user(Long id, String email, String name, HelperStatus status) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setFullName(name);
        user.setPhone("29123456");
        user.setRole(Role.USER);
        user.setHelperStatus(status);
        return user;
    }

    @Test
    void shouldListOpenRequestsOfOthersWithoutContactDetails() {
        when(userRepository.findByEmail("hans@example.test")).thenReturn(Optional.of(helper));
        when(helpRequestRepository.findByStatusAndCustomerNotOrderByCreatedAtDesc(RequestStatus.OPEN, helper))
                .thenReturn(List.of(request));

        List<HelpRequestResponse> result = service.findOpen("hans@example.test");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(RequestStatus.OPEN);
        assertThat(result.get(0).counterpartName()).isNull();
        assertThat(result.get(0).counterpartPhone()).isNull();
        assertThat(result.get(0).counterpartEmail()).isNull();
    }

    @Test
    void shouldAcceptAnOpenRequestAndShowTheCustomerContactToTheHelper() {
        request.setId(10L);
        when(userRepository.findByEmail("hans@example.test")).thenReturn(Optional.of(helper));
        when(helpRequestRepository.findById(10L)).thenReturn(Optional.of(request));
        when(helpRequestRepository.save(any(HelpRequest.class))).thenAnswer(call -> call.getArgument(0));
        when(reviewRepository.findByRequestId(10L)).thenReturn(Optional.empty());

        HelpRequestResponse result = service.accept(10L, "hans@example.test");

        assertThat(request.getStatus()).isEqualTo(RequestStatus.ACCEPTED);
        assertThat(request.getHelper()).isSameAs(helper);
        assertThat(result.status()).isEqualTo(RequestStatus.ACCEPTED);
        assertThat(result.counterpartName()).isEqualTo("Anna Customer");
        assertThat(result.counterpartEmail()).isEqualTo("anna@example.test");
    }

    @Test
    void shouldListTheRequestsIHaveAccepted() {
        request.setId(10L);
        request.setStatus(RequestStatus.ACCEPTED);
        request.setHelper(helper);
        when(userRepository.findByEmail("hans@example.test")).thenReturn(Optional.of(helper));
        when(helpRequestRepository.findByHelperOrderByCreatedAtDesc(helper)).thenReturn(List.of(request));
        when(reviewRepository.findByRequestId(10L)).thenReturn(Optional.empty());

        List<HelpRequestResponse> result = service.findHelping("hans@example.test");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).counterpartName()).isEqualTo("Anna Customer");
    }

    @Test
    void shouldCompleteAnAcceptedRequestAsTheAssignedHelper() {
        request.setId(10L);
        request.setStatus(RequestStatus.ACCEPTED);
        request.setHelper(helper);
        when(userRepository.findByEmail("hans@example.test")).thenReturn(Optional.of(helper));
        when(helpRequestRepository.findById(10L)).thenReturn(Optional.of(request));
        when(helpRequestRepository.save(any(HelpRequest.class))).thenAnswer(call -> call.getArgument(0));
        when(reviewRepository.findByRequestId(10L)).thenReturn(Optional.empty());

        HelpRequestResponse result = service.complete(10L, "hans@example.test");

        assertThat(request.getStatus()).isEqualTo(RequestStatus.COMPLETED);
        assertThat(result.status()).isEqualTo(RequestStatus.COMPLETED);
    }

    @ParameterizedTest
    @CsvSource({
            "PENDING, Your helper account is waiting for approval",
            "REMOVED, An admin removed your helper account",
            "NONE,    You are not registered as a helper"
    })
    void shouldGiveAClearMessageToSomeoneWhoCannotHelp(HelperStatus status, String message) {
        User notHelper = user(3L, "other@example.test", "Other Person", status);
        when(userRepository.findByEmail("other@example.test")).thenReturn(Optional.of(notHelper));

        assertThatThrownBy(() -> service.findOpen("other@example.test"))
                .isInstanceOf(HelperNotVerifiedException.class)
                .hasMessage(message);
    }

    @Test
    void shouldRefuseTheAcceptedListForSomeoneWhoIsNotAVerifiedHelper() {
        User pending = user(3L, "pending@example.test", "Pending Helper", HelperStatus.PENDING);
        when(userRepository.findByEmail("pending@example.test")).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.findHelping("pending@example.test"))
                .isInstanceOf(HelperNotVerifiedException.class);
    }


}