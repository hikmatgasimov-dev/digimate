package com.digimate.repository;

import com.digimate.entity.HelpRequest;
import com.digimate.entity.User;
import com.digimate.entity.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HelpRequestRepository extends JpaRepository<HelpRequest, Long> {
    List<HelpRequest> findByCustomerOrderByCreatedAtDesc(User customer);

    // open requests of other people (the user's own requests are left out)
    List<HelpRequest> findByStatusAndCustomerNotOrderByCreatedAtDesc(RequestStatus status, User customer);

    // requests this helper has accepted
    List<HelpRequest> findByHelperOrderByCreatedAtDesc(User helper);

}
