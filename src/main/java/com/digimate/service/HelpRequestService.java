package com.digimate.service;

import com.digimate.dto.HelpRequestDto;
import com.digimate.dto.HelpRequestResponse;
import com.digimate.repository.CategoryRepository;
import com.digimate.repository.HelpRequestRepository;
import com.digimate.repository.ReviewRepository;
import com.digimate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HelpRequestService {

    private final HelpRequestRepository helpRequestRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    //TODO Implement Service method bodies
    public HelpRequestResponse create(HelpRequestDto dto, String username) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public List<HelpRequestResponse> findMine(String username) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public HelpRequestResponse findById(Long id, String username) {
        throw new UnsupportedOperationException("Not implemented yet");
    }


    public HelpRequestResponse update(Long id, HelpRequestDto dto, String username) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public HelpRequestResponse cancel(Long id, String username) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
