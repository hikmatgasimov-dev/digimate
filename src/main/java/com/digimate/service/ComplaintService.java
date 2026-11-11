package com.digimate.service;

import com.digimate.dto.ComplaintDto;
import com.digimate.dto.ComplaintResponse;
import com.digimate.dto.DismissDto;
import com.digimate.entity.Complaint;
import com.digimate.entity.HelpRequest;
import com.digimate.entity.User;
import com.digimate.entity.enums.ComplaintStatus;
import com.digimate.entity.enums.RequestStatus;
import com.digimate.exception.DuplicateComplaintException;
import com.digimate.exception.InvalidStatusTransitionException;
import com.digimate.exception.RequestNotFoundException;
import com.digimate.repository.ComplaintRepository;
import com.digimate.repository.HelpRequestRepository;
import com.digimate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Rules for customer reports about helpers.
 * These are empty stubs, so other code can compile. The real work comes later.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComplaintService {


    private final ComplaintRepository complaintRepository;
    private final HelpRequestRepository helpRequestRepository;
    private final UserRepository userRepository;

    /**
     * A customer reports the helper of one of their requests.
     */
    @Transactional
    public ComplaintResponse create(long requestId, ComplaintDto dto, String username) {
        HelpRequest request = helpRequestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException(("Request not found")));

        User customer = findUser(username);
        if (request.getCustomer() == null || !request.getCustomer().getId().equals(customer.getId())) {

            throw new AccessDeniedException("Only the creator can report a problem on this request");
        }

        if (request.getStatus() != RequestStatus.ACCEPTED && request.getStatus() != RequestStatus.COMPLETED) {
            throw new InvalidStatusTransitionException("You can only report a problem after a helper accepted the request");
        }
        if (request.getHelper() == null) {
            throw new InvalidStatusTransitionException("This request has no helper to report");
        }
        // The unique column complaint.request_id is the safety net if two reports arrive at the same moment.
        if (complaintRepository.existsByRequestId(requestId)) {
            throw new DuplicateComplaintException("This request already has a report");
        }
        Complaint complaint = new Complaint();
        complaint.setRequest(request);
        complaint.setHelper(request.getHelper());
        complaint.setCustomer(customer);
        complaint.setReason(dto.reason().trim());

        Complaint saved=complaintRepository.save(complaint);

        log.info("Report saved: requestId={}, helperId={}", requestId, request.getHelper().getId());

        return toResponse(saved);
    }
    /** The admin sees the open reports, newest first. */

    @Transactional(readOnly = true)
    public List<ComplaintResponse> findOpen() {
        return complaintRepository.findByStatusOrderByCreatedAtDesc(ComplaintStatus.OPEN).stream()
                .map(this::toResponse)
                .toList();
    }
     /**
     * Admin close report without action
     **/

    @Transactional
    public ComplaintResponse dismiss(Long complaintId, DismissDto dto) {

         Complaint complaint = complaintRepository.findById(complaintId)
                 .orElseThrow(() -> new RequestNotFoundException("Report not found"));

         if(complaint.getStatus()!=ComplaintStatus.OPEN){
             throw new InvalidStatusTransitionException("This report is already closed.");
         }

        complaint.setStatus(ComplaintStatus.DISMISSED);
        log.info("Report dismissed: complaintId={}", complaintId);
        return toResponse(complaint);
    }

    /**
     * The report status for each request of a customer, by request id.
     */

    @Transactional(readOnly = true)
    public Map<Long, ComplaintStatus> statusByRequest(String username) {
        User customer = findUser(username);
        return complaintRepository.findByCustomer(customer).stream()
                .collect(Collectors.toMap(complaint -> complaint.getRequest().getId(),Complaint::getStatus));
    }

    // The admin sees the helper's name and email and the customer's first name only. Never a phone number.
    private ComplaintResponse toResponse(Complaint complaint) {
        return new ComplaintResponse(
                complaint.getId(),
                complaint.getRequest().getId(),
                complaint.getHelper().getId(),
                complaint.getHelper().getFullName(),
                complaint.getHelper().getEmail(),
                firstName(complaint.getCustomer().getFullName()),
                complaint.getReason(),
                complaint.getStatus(),
                complaint.getCreatedAt());
    }


    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RequestNotFoundException("User not found"));
    }

    private String firstName(String fullName) {
        return fullName.trim().split("\\s+")[0];
    }
}
