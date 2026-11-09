package com.digimate.service;

import com.digimate.dto.HelpRequestResponse;
import com.digimate.entity.HelpRequest;
import com.digimate.entity.Review;
import com.digimate.entity.User;
import com.digimate.entity.enums.HelperStatus;
import com.digimate.entity.enums.RequestStatus;
import com.digimate.exception.HelperNotVerifiedException;
import com.digimate.exception.InvalidStatusTransitionException;
import com.digimate.exception.RequestAlreadyTakenException;
import com.digimate.exception.RequestNotFoundException;
import com.digimate.repository.HelpRequestRepository;
import com.digimate.repository.ReviewRepository;
import com.digimate.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.springframework.security.access.AccessDeniedException;
import java.util.List;

/**
 * Rules for verified helpers: find open requests, accept one, and complete it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HelperFlowService {

    private final UserRepository userRepository;
    private final HelpRequestRepository helpRequestRepository;
    private final ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public List<HelpRequestResponse> findOpen(String username) {
        User helper = findVerifiedHelper(username);
        return helpRequestRepository
                .findByStatusAndCustomerNotOrderByCreatedAtDesc(RequestStatus.OPEN, helper).stream()
                .map(request -> HelpRequestResponse.from(request, helper, null))   // open requests show no contact
                .toList();
    }
    @Transactional
    public HelpRequestResponse accept(Long id,String username) {
        User helper = findVerifiedHelper(username);
        HelpRequest request=findRequest(id);

        if (request.getCustomer().getId().equals(helper.getId())) {
            throw new AccessDeniedException("You cannot accept your own request");
        }
        if (request.getStatus() != RequestStatus.OPEN) {
            throw new RequestAlreadyTakenException("This request is no longer open");
        }

        request.setHelper(helper);
        request.setStatus(RequestStatus.ACCEPTED);
        // If two helpers accept at the same moment, the second save fails with an optimistic lock
        // error (@Version). The global handler turns it into 409.
        HelpRequest saved = helpRequestRepository.save(request);
        log.info("Request accepted: requestId={}, helperId={}", saved.getId(), helper.getId());
        return toResponse(saved, helper);
    }

    @Transactional(readOnly = true)
    public List<HelpRequestResponse> findHelping(String username) {
        User helper = findVerifiedHelper(username);
        return helpRequestRepository.findByHelperOrderByCreatedAtDesc(helper).stream()
                .map(request -> toResponse(request, helper))
                .toList();
    }
    @Transactional
    public HelpRequestResponse complete(Long id, String username) {
        User helper = findUser(username);
        HelpRequest request = findRequest(id);

        if (request.getHelper() != null && !request.getHelper().getId().equals(helper.getId())) {
            throw new AccessDeniedException("Only the assigned helper can complete this request");
        }
        if (request.getStatus() != RequestStatus.ACCEPTED) {
            throw new InvalidStatusTransitionException("Only an accepted request can be completed");
        }

        request.setStatus(RequestStatus.COMPLETED);
        HelpRequest saved = helpRequestRepository.save(request);
        log.info("Request completed: requestId={}, helperId={}", saved.getId(), helper.getId());
        return toResponse(saved, helper);
    }
    private User findVerifiedHelper(String email) {
        User user = findUser(email);
        if (user.getHelperStatus() != HelperStatus.VERIFIED) {
            throw new HelperNotVerifiedException(messageFor(user.getHelperStatus()));
        }
        return user;
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RequestNotFoundException("User not found"));
    }
    private HelpRequest findRequest(Long id) {
        return helpRequestRepository.findById(id)
                .orElseThrow(() -> new RequestNotFoundException("Request not found"));
    }

    private HelpRequestResponse toResponse(HelpRequest request,User viewer){
        Review review = reviewRepository.findByRequestId(request.getId()).orElse(null);
        return HelpRequestResponse.from(request, viewer, review);
    }

    // A different text for each reason, so the person knows what to do next.
    private String messageFor(HelperStatus status) {
        return switch (status) {
            case PENDING -> "Your helper account is waiting for approval";
            case REMOVED -> "An admin removed your helper account";
            default -> "You are not registered as a helper";
        };
    }

}
