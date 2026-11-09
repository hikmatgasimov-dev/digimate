package com.digimate.service;

import com.digimate.dto.ComplaintDto;
import com.digimate.dto.ComplaintResponse;
import com.digimate.dto.DismissDto;
import com.digimate.entity.enums.ComplaintStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Rules for customer reports about helpers.
 * These are empty stubs, so other code can compile. The real work comes later.
 */
@Slf4j
@Service
public class ComplaintService {

    public ComplaintService create(long requestId, ComplaintDto dto ,String username){
        throw new UnsupportedOperationException("not implemented yet");
    }

    /** Admin close report without action **/

    public ComplaintResponse dismiss(Long complaintId, DismissDto dto){
        throw new UnsupportedOperationException("not implemented yet");
    }
    /** The report status for each request of a customer, by request id. */
    public Map<Long, ComplaintStatus> statusByRequest(String username) {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
