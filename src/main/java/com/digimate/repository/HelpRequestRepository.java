package com.digimate.repository;

import com.digimate.entity.HelpRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public class HelpRequestRepository extends JpaRepository<HelpRequest, Long> {
    List<HelpRequest> findByCustomerOrderByCreatedAtDesc(User customer);

}
