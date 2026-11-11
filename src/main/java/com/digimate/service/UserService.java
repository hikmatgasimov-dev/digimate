package com.digimate.service;


import com.digimate.dto.RegisterRequest;
import com.digimate.dto.UserResponse;
import com.digimate.entity.Complaint;
import com.digimate.entity.HelpRequest;
import com.digimate.entity.User;
import com.digimate.entity.enums.ComplaintStatus;
import com.digimate.entity.enums.HelperStatus;
import com.digimate.entity.enums.RequestStatus;
import com.digimate.entity.enums.Role;
import com.digimate.exception.EmailAlreadyUsedException;
import com.digimate.exception.InvalidStatusTransitionException;
import com.digimate.exception.RequestNotFoundException;
import com.digimate.repository.ComplaintRepository;
import com.digimate.repository.HelpRequestRepository;
import com.digimate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.digimate.dto.RemoveHelperDto;

import java.util.List;
import java.util.Locale;
/**
 * Business rules for users: registration, and the admin verifying helpers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ComplaintRepository complaintRepository;
    private final HelpRequestRepository helpRequestRepository;

    @Transactional
    public UserResponse register(RegisterRequest request){

        //emails are stored in lower case

        String email=request.email().trim().toLowerCase(Locale.ROOT);

        if(userRepository.findByEmail(email).isPresent()){
            throw new EmailAlreadyUsedException("This email is already registered.");

        }

        User user =new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPhone(request.phone().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        // The role is never read from the request, so nobody can register as ADMIN.
        user.setRole(Role.USER); user.setHelperStatus(Boolean.TRUE.equals(request.wantsToHelp()) ? HelperStatus.PENDING : HelperStatus.NONE);

        User saved=userRepository.save(user);
        log.info("User registered :userId={}",saved.getId());

        return UserResponse.from(saved);

    }
    @Transactional(readOnly = true)
    public List<UserResponse>findPendingHelpers(){
        return userRepository.findByHelperStatus(HelperStatus.PENDING).stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public UserResponse verifyHelper(Long id){
        User user= userRepository.findById(id)
                .orElseThrow(()->new RequestNotFoundException("User not found"));

        if (user.getHelperStatus() != HelperStatus.PENDING) {
            throw new InvalidStatusTransitionException("Only a pending helper can be verified.");
        }

        user.setHelperStatus(HelperStatus.VERIFIED);

        log.info("Helper verified: userId={}", id);
        return UserResponse.from(user);

    }

    /** The admin sees every user, by name. A user has no phone number and no password in the answer. */
    @Transactional(readOnly = true)
    public List<UserResponse>findAll() {
        return userRepository.findAll(Sort.by("fullName")).stream()
                .map(UserResponse::from)
                .toList();
    }

    /** The admin sees the verified helpers. */
    @Transactional(readOnly = true)
    public List<UserResponse>findVerifiedHelpers(){
        return userRepository.findByHelperStatus(HelperStatus.VERIFIED).stream()
                .map(UserResponse::from)
                .toList();
    }
      /**
     * The admin removes a helper. Removal is permanent. Everything happens in one transaction:
     * the helper is REMOVED, their accepted requests are open again, and their open reports are closed.
     * Completed requests keep their helper. The reason is checked by the DTO and is not stored.
     */
    @Transactional
    public UserResponse removeHelper(Long id, RemoveHelperDto dto) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RequestNotFoundException("User not found"));

        if (user.getHelperStatus() != HelperStatus.VERIFIED) {
            throw new InvalidStatusTransitionException("Only a verified helper can be removed.");
        }

        user.setHelperStatus(HelperStatus.REMOVED);

        List<HelpRequest> accepted = helpRequestRepository.findByHelperAndStatus(user, RequestStatus.ACCEPTED);
        for (HelpRequest request : accepted) {
            request.setStatus(RequestStatus.OPEN);
            request.setHelper(null);
        }

        List<Complaint> openReports = complaintRepository.findByHelperAndStatus(user, ComplaintStatus.OPEN);
        for (Complaint report : openReports) {
            report.setStatus(ComplaintStatus.REMOVED);
        }

        log.info("Helper removed: userId={}, requestsReopened={}, reportsClosed={}",
                id, accepted.size(), openReports.size());
        return UserResponse.from(user);
    }



}
