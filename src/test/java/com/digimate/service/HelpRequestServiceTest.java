package com.digimate.service;

import com.digimate.dto.HelpRequestDto;
import com.digimate.dto.HelpRequestResponse;
import com.digimate.entity.Category;
import com.digimate.entity.HelpRequest;
import com.digimate.entity.User;
import com.digimate.entity.enums.RequestMode;
import com.digimate.entity.enums.RequestStatus;
import com.digimate.entity.enums.Role;
import com.digimate.repository.CategoryRepository;
import com.digimate.repository.HelpRequestRepository;
import com.digimate.repository.ReviewRepository;
import com.digimate.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HelpRequestServiceTest {

    private static final String EMAIL = "anna@example.com";

    @Mock
    private HelpRequestRepository helpRequestRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private HelpRequestService service;

    private User customer;
    private Category category;
    private HelpRequestDto requestDto;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setId(1L);
        customer.setEmail(EMAIL);
        customer.setFullName("Anna Customer");
        customer.setRole(Role.USER);

        category = new Category();
        category.setId(10L);
        category.setName("Smartphone");

        requestDto = new HelpRequestDto("Set up WhatsApp", "Please help me install WhatsApp.", 10L,
                LocalDateTime.now().plusDays(2), RequestMode.IN_PERSON, "City library");
    }

    @Test
    void create_savesOpenRequest() {
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(customer));
        when(helpRequestRepository.save(any(HelpRequest.class))).thenAnswer(invocation -> {
            HelpRequest request = invocation.getArgument(0);
            request.setId(100L);
            return request;
        });

        HelpRequestResponse response = service.create(requestDto, EMAIL);

        assertEquals(100L, response.id());
        assertEquals(RequestStatus.OPEN, response.status());
        assertEquals("Set up WhatsApp", response.title());
        assertEquals("Smartphone", response.categoryName());
        verify(helpRequestRepository).save(any(HelpRequest.class));
    }

    @Test
    void findMine_returnsCustomersRequests() {
        HelpRequest request = request();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(customer));
        when(helpRequestRepository.findByCustomerOrderByCreatedAtDesc(customer)).thenReturn(List.of(request));
        when(reviewRepository.findByRequestId(100L)).thenReturn(Optional.empty());

        List<HelpRequestResponse> responses = service.findMine(EMAIL);

        assertEquals(1, responses.size());
        assertEquals(100L, responses.getFirst().id());
    }

    @Test
    void findById_returnsRequestForCreator() {
        HelpRequest request = request();
        when(helpRequestRepository.findById(100L)).thenReturn(Optional.of(request));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(customer));
        when(reviewRepository.findByRequestId(100L)).thenReturn(Optional.empty());

        HelpRequestResponse response = service.findById(100L, EMAIL);

        assertEquals(100L, response.id());
        assertEquals("Set up WhatsApp", response.title());
    }

    @Test
    void update_changesOpenRequest() {
        HelpRequest request = request();
        HelpRequestDto changed = new HelpRequestDto("Set up Viber", "Please help me install Viber.", 10L,
                LocalDateTime.now().plusDays(3), RequestMode.ONLINE, null);
        when(helpRequestRepository.findById(100L)).thenReturn(Optional.of(request));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(customer));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(reviewRepository.findByRequestId(100L)).thenReturn(Optional.empty());

        HelpRequestResponse response = service.update(100L, changed, EMAIL);

        assertEquals("Set up Viber", response.title());
        assertEquals(RequestMode.ONLINE, response.mode());
        assertEquals(RequestStatus.OPEN, response.status());
    }

    @Test
    void cancel_setsStatusCancelled() {
        HelpRequest request = request();
        when(helpRequestRepository.findById(100L)).thenReturn(Optional.of(request));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(customer));
        when(reviewRepository.findByRequestId(100L)).thenReturn(Optional.empty());

        HelpRequestResponse response = service.cancel(100L, EMAIL);

        assertEquals(RequestStatus.CANCELLED, response.status());
        assertEquals(RequestStatus.CANCELLED, request.getStatus());
    }

    private HelpRequest request() {
        HelpRequest request = new HelpRequest();
        request.setId(100L);
        request.setTitle("Set up WhatsApp");
        request.setDescription("Please help me install WhatsApp.");
        request.setCategory(category);
        request.setPreferredTime(LocalDateTime.now().plusDays(2));
        request.setMode(RequestMode.IN_PERSON);
        request.setPlace("City library");
        request.setCustomer(customer);
        request.setStatus(RequestStatus.OPEN);
        return request;
    }
    @Test
    void create_refusesAnAdmin() {
        customer.setRole(Role.ADMIN);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(customer));

        assertThrows(AccessDeniedException.class, () -> service.create(requestDto, EMAIL));
        verify(helpRequestRepository, never()).save(any(HelpRequest.class));
    }
}
