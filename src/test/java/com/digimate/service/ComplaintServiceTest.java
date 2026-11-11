
package com.digimate.service;
import com.digimate.dto.ComplaintDto;
import com.digimate.dto.ComplaintResponse;
import com.digimate.dto.DismissDto;
import com.digimate.entity.Complaint;
import com.digimate.entity.HelpRequest;
import com.digimate.entity.User;
import com.digimate.entity.enums.ComplaintStatus;
import com.digimate.entity.enums.RequestStatus;
import com.digimate.repository.ComplaintRepository;
import com.digimate.repository.HelpRequestRepository;
import com.digimate.repository.UserRepository;
import com.digimate.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplaintServiceTest {

    private static final String EMAIL = "customer@example.test";

    @Mock ComplaintRepository complaintRepository;
    @Mock HelpRequestRepository helpRequestRepository;
    @Mock UserRepository userRepository;
    @InjectMocks ComplaintService service;

    private User customer;
    private User helper;
    private HelpRequest request;

    @BeforeEach
    void setUp() {
        customer = TestData.customer();
        customer.setId(1L);
        customer.setEmail(EMAIL);
        customer.setFullName("Anna Ozola");
        helper = TestData.verifiedHelper();
        helper.setId(2L);

        request = TestData.openRequest(customer, TestData.category());
        request.setId(100L);
        request.setHelper(helper);
        request.setStatus(RequestStatus.ACCEPTED);
    }

    @Test
    void shouldSaveAnOpenReportAboutTheHelperOfTheRequest() {
        when(helpRequestRepository.findById(100L)).thenReturn(Optional.of(request));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(customer));
        when(complaintRepository.existsByRequestId(100L)).thenReturn(false);
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ComplaintResponse response = service.create(100L, new ComplaintDto("  The helper did not show up.  "), EMAIL);

        ArgumentCaptor<Complaint> saved = ArgumentCaptor.forClass(Complaint.class);
        verify(complaintRepository).save(saved.capture());
        assertEquals(ComplaintStatus.OPEN, saved.getValue().getStatus());
        assertEquals(helper, saved.getValue().getHelper());
        assertEquals(customer, saved.getValue().getCustomer());
        assertEquals(request, saved.getValue().getRequest());
        assertEquals("The helper did not show up.", saved.getValue().getReason());
        assertEquals(100L, response.requestId());
        assertEquals(ComplaintStatus.OPEN, response.status());
    }

    @Test
    void shouldShowTheOpenReportsWithTheHelperAndOnlyTheCustomersFirstName() {
        Complaint complaint = complaint(ComplaintStatus.OPEN);
        when(complaintRepository.findByStatusOrderByCreatedAtDesc(ComplaintStatus.OPEN)).thenReturn(List.of(complaint));

        List<ComplaintResponse> reports = service.findOpen();

        assertEquals(1, reports.size());
        ComplaintResponse report = reports.get(0);
        assertEquals("Verified Helper", report.helperName());
        assertEquals("verified-helper@example.test", report.helperEmail());
        assertEquals("Anna", report.customerFirstName());
        assertEquals("The helper did not show up.", report.text());
    }

    @Test
    void shouldCloseAReportWithoutAction() {
        Complaint complaint = complaint(ComplaintStatus.OPEN);
        when(complaintRepository.findById(7L)).thenReturn(Optional.of(complaint));

        ComplaintResponse response = service.dismiss(7L, new DismissDto("Misunderstanding"));

        assertEquals(ComplaintStatus.DISMISSED, complaint.getStatus());
        assertEquals(ComplaintStatus.DISMISSED, response.status());
    }

    @Test
    void shouldGiveTheReportStatusForEachRequestOfACustomer() {
        Complaint complaint = complaint(ComplaintStatus.OPEN);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(customer));
        when(complaintRepository.findByCustomer(customer)).thenReturn(List.of(complaint));

        Map<Long, ComplaintStatus> statuses = service.statusByRequest(EMAIL);

        assertEquals(Map.of(100L, ComplaintStatus.OPEN), statuses);
    }

    private Complaint complaint(ComplaintStatus status) {
        Complaint complaint = new Complaint();
        complaint.setId(7L);
        complaint.setRequest(request);
        complaint.setHelper(helper);
        complaint.setCustomer(customer);
        complaint.setReason("The helper did not show up.");
        complaint.setStatus(status);
        return complaint;
    }
}