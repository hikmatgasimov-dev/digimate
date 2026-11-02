package com.digimate.repository;

import com.digimate.entity.HelpRequest;
import com.digimate.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HelpRequestRepository extends JpaRepository<HelpRequest, Long> {
    List<HelpRequest> findByCustomerOrderByCreatedAtDesc(User customer);

}
