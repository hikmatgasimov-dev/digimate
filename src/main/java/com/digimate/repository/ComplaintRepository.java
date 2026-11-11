package com.digimate.repository;

import com.digimate.entity.Complaint;
import com.digimate.entity.User;
import com.digimate.entity.enums.ComplaintStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {


    boolean existsByRequestId(Long requestId);

    // the reports the admin still has to look at, newest first
    List<Complaint> findByStatusOrderByCreatedAtDesc(ComplaintStatus status);

    // the reports a customer has sent (one per request)
    List<Complaint> findByCustomer(User customer);

    // the open reports about one helper (used when the helper is removed)
    List<Complaint> findByHelperAndStatus(User helper, ComplaintStatus status);
}
