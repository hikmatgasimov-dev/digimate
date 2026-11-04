package com.digimate.service;

import com.digimate.dto.HelpRequestDto;
import com.digimate.dto.HelpRequestResponse;
import com.digimate.entity.Category;
import com.digimate.entity.HelpRequest;
import com.digimate.entity.Review;
import com.digimate.entity.User;
import com.digimate.entity.enums.RequestMode;
import com.digimate.entity.enums.RequestStatus;
import com.digimate.entity.enums.Role;
import com.digimate.exception.InvalidStatusTransitionException;
import com.digimate.exception.RequestNotFoundException;
import com.digimate.repository.CategoryRepository;
import com.digimate.repository.HelpRequestRepository;
import com.digimate.repository.ReviewRepository;
import com.digimate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class HelpRequestService {

    private final HelpRequestRepository helpRequestRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    @Transactional
    public HelpRequestResponse create(HelpRequestDto dto, String username) {
        Category category = findCategory(dto.categoryId());
        User customer = findUser(username);
        requirePlaceForInPerson(dto);

        HelpRequest request = new HelpRequest();
        copyFields(dto, request, category);
        request.setCustomer(customer);
        request.setStatus(RequestStatus.OPEN);

        HelpRequest saved = helpRequestRepository.save(request);
        log.info("Request created: requestId={}", saved.getId());
        return HelpRequestResponse.from(saved, customer, null);
    }

    @Transactional(readOnly = true)
    public List<HelpRequestResponse> findMine(String username) {
        User customer = findUser(username);
        return helpRequestRepository.findByCustomerOrderByCreatedAtDesc(customer).stream()
                .map(request -> toResponse(request, customer))
                .toList();
    }

    public HelpRequestResponse findById(Long id, String username) {
        HelpRequest request = findRequest(id);
        User viewer = findUser(username);

        boolean allowed = isCreator(request, viewer) || isAssignedHelper(request, viewer) || viewer.getRole() == Role.ADMIN;
        if (!allowed) {
            throw new AccessDeniedException("Not allowed to view this request");
        }
        return toResponse(request, viewer);
    }

    @Transactional
    public HelpRequestResponse update(Long id, HelpRequestDto dto, String username) {
        HelpRequest request = findRequest(id);
        User customer = findUser(username);
        requireCreator(request, customer);
        requireOpen(request);

        Category category = findCategory(dto.categoryId());
        requirePlaceForInPerson(dto);
        copyFields(dto, request, category);

        return toResponse(request, customer);
    }

    public HelpRequestResponse cancel(Long id, String username) {
        HelpRequest request = findRequest(id);
        User customer = findUser(username);
        requireCreator(request, customer);
        requireOpen(request);

        request.setStatus(RequestStatus.CANCELLED);
        return toResponse(request, customer);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RequestNotFoundException("User not found"));
    }

    private HelpRequest findRequest(Long id) {
        return helpRequestRepository.findById(id)
                .orElseThrow(() -> new RequestNotFoundException("Request not found"));
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RequestNotFoundException("Category not found"));
    }


    private void requirePlaceForInPerson(HelpRequestDto dto) {
        if (dto.mode() == RequestMode.IN_PERSON && (dto.place() == null || dto.place().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Place is required for in-person help");
        }
    }

    private void requireCreator(HelpRequest request, User user) {
        if (!isCreator(request, user)) {
            throw new AccessDeniedException("Only the creator can change this request");
        }
    }

    private void requireOpen(HelpRequest request) {
        if (request.getStatus() != RequestStatus.OPEN) {
            throw new InvalidStatusTransitionException("Only open requests can be changed");
        }
    }

    private boolean isCreator(HelpRequest request, User user) {
        return request.getCustomer() != null && request.getCustomer().getId().equals(user.getId());
    }

    private boolean isAssignedHelper(HelpRequest request, User user) {
        return request.getHelper() != null && request.getHelper().getId().equals(user.getId());
    }


    //Data mapping
    private void copyFields(HelpRequestDto dto, HelpRequest request, Category category) {
        request.setTitle(dto.title());
        request.setDescription(dto.description());
        request.setCategory(category);
        request.setPreferredTime(dto.preferredTime());
        request.setMode(dto.mode());
        request.setPlace(dto.place());
    }

    private HelpRequestResponse toResponse(HelpRequest request, User viewer) {
        Review review = reviewRepository.findByRequestId(request.getId()).orElse(null);
        return HelpRequestResponse.from(request, viewer, review);
    }
}
