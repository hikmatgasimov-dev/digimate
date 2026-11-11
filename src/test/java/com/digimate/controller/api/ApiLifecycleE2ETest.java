package com.digimate.controller.api;

import com.digimate.entity.enums.HelperStatus;
import com.digimate.entity.enums.RequestStatus;
import com.digimate.support.HttpE2ETestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.*;

/** SCRUM-57: Customer and helper E2E journeys through real HTTP and PostgreSQL. */
class ApiLifecycleE2ETest extends HttpE2ETestSupport {

    @Test
    @DisplayName("E2E-01: registration, verification, acceptance and completion")
    void shouldCompleteRequestFromRegistration() throws Exception {
        Actor customer = register("Customer", false);
        Actor helper = register("Helper", true);

        item(get("/api/admin/helpers/pending", admin), helper.id());
        verify(helper);
        assertAbsent(get("/api/admin/helpers/pending", admin), helper.id());
        item(get("/api/admin/helpers/verified", admin), helper.id());

        long requestsBefore = helpRequestRepository.count();
        long id = createRequest(customer);
        assertHiddenContacts(item(get("/api/requests/my", customer), id));
        assertHiddenContacts(item(get("/api/requests/open", helper), id));
        assertAbsent(get("/api/requests/my", helper), id);
        assertAbsent(get("/api/requests/helping", helper), id);

        assertContacts(accept(id, helper), customer);
        assertRequestState(id, customer, helper, RequestStatus.ACCEPTED);
        assertAbsent(get("/api/requests/open", helper), id);
        assertContacts(item(get("/api/requests/helping", helper), id), customer);
        assertContacts(item(get("/api/requests/my", customer), id), helper);
        assertContacts(detail(id, customer), helper);
        assertContacts(detail(id, helper), customer);

        JsonNode completed = request("POST", "/api/requests/" + id + "/complete", helper, null, 200);
        assertEquals("COMPLETED", completed.path("status").asString());
        assertContacts(completed, customer);
        assertRequestState(id, customer, helper, RequestStatus.COMPLETED);
        for (Actor participant : new Actor[]{customer, helper}) {
            JsonNode response = detail(id, participant);
            assertEquals("COMPLETED", response.path("status").asString());
            assertContacts(response, participant == customer ? helper : customer);
        }
        assertEquals("COMPLETED", item(get("/api/requests/my", customer), id).path("status").asString());
        assertEquals("COMPLETED", item(get("/api/requests/helping", helper), id).path("status").asString());
        assertAbsent(get("/api/requests/open", helper), id);
        assertEquals(requestsBefore + 1, helpRequestRepository.count());
    }

    @Test
    @DisplayName("E2E-02: a pending helper can retry with the same JWT after verification")
    void shouldAllowAcceptanceAfterVerification() throws Exception {
        Actor customer = customer();
        Actor helper = helper(HelperStatus.PENDING);
        long id = createRequest(customer);

        assertRejected("POST", "/api/requests/" + id + "/accept", helper, null, 403);
        assertRejected("GET", "/api/requests/open", helper, null, 403);
        verify(helper);
        assertAbsent(get("/api/admin/helpers/pending", admin), helper.id());
        assertContacts(accept(id, helper), customer);
        assertRequestState(id, customer, helper, RequestStatus.ACCEPTED);
    }

    @Test
    @DisplayName("E2E-03: a later helper cannot replace or complete the first helper's work")
    void shouldKeepTheFirstAssignedHelper() throws Exception {
        Actor customer = customer();
        Actor first = helper(HelperStatus.VERIFIED);
        Actor second = helper(HelperStatus.VERIFIED);
        long id = createRequest(customer);
        accept(id, first);

        assertRejected("POST", "/api/requests/" + id + "/accept", second, null, 409);
        assertRejected("POST", "/api/requests/" + id + "/complete", second, null, 403);
        assertRequestState(id, customer, first, RequestStatus.ACCEPTED);
        item(get("/api/requests/helping", first), id);
        assertAbsent(get("/api/requests/helping", second), id);
    }
}
