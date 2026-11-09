package com.digimate.service;


import com.digimate.dto.RegisterRequest;
import com.digimate.dto.UserResponse;
import com.digimate.entity.User;
import com.digimate.entity.enums.HelperStatus;
import com.digimate.entity.enums.Role;
import com.digimate.exception.EmailAlreadyUsedException;
import com.digimate.exception.InvalidStatusTransitionException;
import com.digimate.exception.RequestNotFoundException;
import com.digimate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    /** The admin sees every user. Not implemented yet. */
    @Transactional(readOnly = true)
    public List<UserResponse>findAll(){
        throw new UnsupportedOperationException("not implemented yet");
    }

    /** The admin sees the verified helpers. Not implemented yet. */
    @Transactional(readOnly = true)
    public List<UserResponse>findVerifiedHelpers(){
        throw new UnsupportedOperationException("not implemented yet");
    }
    /** The admin removes a helper. Not implemented yet. */
    @Transactional
    public UserResponse removeHelper(Long id, RemoveHelperDto dto) {
        throw new UnsupportedOperationException("not implemented yet");
    }


}
