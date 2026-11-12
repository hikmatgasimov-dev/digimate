package com.digimate.controller.api;

import com.digimate.entity.Category;
import com.digimate.entity.enums.HelperStatus;
import com.digimate.entity.enums.RequestStatus;
import com.digimate.repository.ComplaintRepository;
import com.digimate.support.HttpE2ETestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** SCRUM-58: independent request, review and moderation journeys through real HTTP. */
class RequestActionsE2ETest extends HttpE2ETestSupport {

    @Autowired
    private ComplaintRepository complaintRepository;

    @Test
    @DisplayName("E2E-04: edit and cancel an owned OPEN request without deleting it")
    void shouldEditAndCancelOwnOpenRequest() throws Exception {
        Actor customer = customer();
        Actor helper = helper(HelperStatus.VERIFIED);
        long id = createRequest(customer);
        long countBefore = helpRequestRepository.count();
        Map<String, Object> original = requestRow(id);
        Category newCategory = new Category();
        newCategory.setName("Edited category " + UUID.randomUUID());
        newCategory = categoryRepository.saveAndFlush(newCategory);
        LocalDateTime time = LocalDateTime.now().plusDays(5).withNano(0);
        Map<String, Object> edit = Map.of("title", "Configure a test tablet",
                "description", "Set up accessibility on a tablet.", "categoryId", newCategory.getId(),
                "preferredTime", time, "mode", "IN_PERSON", "place", "Test library");

        JsonNode edited = request("PUT", "/api/requests/" + id, customer, edit, 200);
        Map<String, Object> stored = requestRow(id);
        assertAll(
                () -> assertEquals("Configure a test tablet", edited.path("title").asString()),
                () -> assertEquals("Set up accessibility on a tablet.", edited.path("description").asString()),
                () -> assertEquals("IN_PERSON", edited.path("mode").asString()),
                () -> assertEquals("Test library", edited.path("place").asString()),
                () -> assertEquals(time, LocalDateTime.parse(edited.path("preferredTime").asString())),
                () -> assertEquals(edit.get("title"), stored.get("title")),
                () -> assertEquals(edit.get("description"), stored.get("description")),
                () -> assertEquals(edit.get("categoryId"), stored.get("category_id")),
                () -> assertEquals(time, ((Timestamp) stored.get("preferred_time")).toLocalDateTime()),
                () -> assertEquals("IN_PERSON", stored.get("mode")),
                () -> assertEquals("Test library", stored.get("place")),
                () -> assertEquals(original.get("created_at"), stored.get("created_at")));
        assertEquals(newCategory.getName(), edited.path("categoryName").asString());
        assertRequestState(id, customer, null, RequestStatus.OPEN);
        assertHiddenContacts(edited);
        assertEquals("Configure a test tablet", item(get("/api/requests/open", helper), id).path("title").asString());

        request("DELETE", "/api/requests/" + id, customer, null, 204);
        assertRequestState(id, customer, null, RequestStatus.CANCELLED);
        Map<String, Object> expected = new HashMap<>(stored);
        expected.put("status", "CANCELLED");
        expected.put("version", ((Number) stored.get("version")).longValue() + 1);
        assertEquals(expected, requestRow(id));
        assertEquals(countBefore, helpRequestRepository.count());
        assertEquals("CANCELLED", item(get("/api/requests/my", customer), id).path("status").asString());
        assertHiddenContacts(detail(id, customer));
        assertAbsent(get("/api/requests/open", helper), id);
    }

    @Test
    @DisplayName("E2E-05: owner reviews completed work once; comment is normalized")
    void shouldReviewCompletedRequestOnlyOnce() throws Exception {
        String comment = "  Patient and helpful.  ";
        String normalized = "Patient and helpful.";
        Actor owner = customer();
        Actor helper = helper(HelperStatus.VERIFIED);
        long id = fixtureRequest(owner, helper, RequestStatus.COMPLETED);
        Map<String, Object> requestBefore = requestRow(id);
        long countBefore = jdbc.queryForObject("select count(*) from review", Long.class);
        Map<String, Object> review = new HashMap<>();
        review.put("rating", 5);
        review.put("comment", comment);

        JsonNode response = request("POST", "/api/requests/" + id + "/review", owner, review, 201);
        assertReview(response, 5, normalized);
        assertReview(detail(id, owner), 5, normalized);
        assertReview(detail(id, helper), 5, normalized);
        assertReview(item(get("/api/requests/my", owner), id), 5, normalized);
        assertReview(item(get("/api/requests/helping", helper), id), 5, normalized);
        Map<String, Object> stored = jdbc.queryForMap("select * from review where request_id = ?", id);
        assertEquals(5, stored.get("rating"));
        assertEquals(normalized, stored.get("comment"));
        assertEquals(requestBefore, requestRow(id));
        assertEquals(countBefore + 1, jdbc.queryForObject("select count(*) from review", Long.class));

        assertRejected("POST", "/api/requests/" + id + "/review", owner,
                Map.of("rating", 1, "comment", "Attempted replacement"), 409);
        assertEquals(1L, jdbc.queryForObject("select count(*) from review where request_id = ?", Long.class, id));
        assertEquals(stored, jdbc.queryForMap("select * from review where request_id = ?", id));
    }

    @Test
    @DisplayName("E2E-06: report, reject duplicates and dismiss a complaint")
    void shouldReportAndDismissAComplaint() throws Exception {
        Actor owner = customer();
        Actor helper = helper(HelperStatus.VERIFIED);
        long id = fixtureRequest(owner, helper, RequestStatus.ACCEPTED);
        Map<String, Object> requestBefore = requestRow(id);
        long complaintsBefore = complaintRepository.count();
        JsonNode created = request("POST", "/api/requests/" + id + "/complaint", owner,
                Map.of("reason", "  Helper did not attend the agreed call.  "), 201);
        long complaintId = created.path("id").asLong();
        assertEquals(id, created.path("requestId").asLong());
        assertEquals(helper.id(), created.path("helperId").asLong());
        assertEquals("OPEN", created.path("status").asString());
        assertEquals("Helper did not attend the agreed call.", created.path("text").asString());
        JsonNode listed = item(get("/api/admin/complaints", admin), complaintId);
        assertEquals(helper.name(), listed.path("helperName").asString());
        assertEquals(helper.email(), listed.path("helperEmail").asString());
        assertEquals("E2E", listed.path("customerFirstName").asString());
        assertFalse(listed.has("customerEmail"));
        assertFalse(listed.has("customerPhone"));
        assertFalse(listed.has("helperPhone"));
        Map<String, Object> stored = complaintRow(complaintId);
        assertEquals(owner.id(), stored.get("customer_id"));
        assertEquals(helper.id(), stored.get("helper_id"));
        assertEquals(id, stored.get("request_id"));
        assertEquals("OPEN", stored.get("status"));
        assertEquals("Helper did not attend the agreed call.", stored.get("reason"));
        assertEquals(complaintsBefore + 1, complaintRepository.count());
        assertRejected("POST", "/api/requests/" + id + "/complaint", owner, Map.of("reason", "Another report"), 409);

        JsonNode dismissed = request("PUT", "/api/admin/complaints/" + complaintId + "/dismiss", admin,
                Map.of("note", "Reviewed in the test."), 200);
        assertEquals("DISMISSED", dismissed.path("status").asString());
        Map<String, Object> expected = new HashMap<>(stored);
        expected.put("status", "DISMISSED");
        assertEquals(expected, complaintRow(complaintId));
        assertAbsent(get("/api/admin/complaints", admin), complaintId);
        assertEquals(requestBefore, requestRow(id));
        assertEquals(HelperStatus.VERIFIED, userRepository.findById(helper.id()).orElseThrow().getHelperStatus());
        assertRejected("POST", "/api/requests/" + id + "/complaint", owner, Map.of("reason", "Report after dismissal"), 409);
        assertRejected("PUT", "/api/admin/complaints/" + complaintId + "/dismiss", admin, null, 409);
        assertEquals(1L, jdbc.queryForObject("select count(*) from complaint where request_id = ?", Long.class, id));
    }

    @Test
    @DisplayName("E2E-07: removal releases accepted work, keeps completed work and isolates unrelated data")
    void shouldRemoveAHelperAndReleaseAcceptedWork() throws Exception {
        Actor owner = customer();
        Actor otherOwner = customer();
        Actor target = helper(HelperStatus.VERIFIED);
        Actor other = helper(HelperStatus.VERIFIED);
        long accepted = fixtureRequest(owner, target, RequestStatus.ACCEPTED);
        long secondAccepted = fixtureRequest(otherOwner, target, RequestStatus.ACCEPTED);
        long completed = fixtureRequest(owner, target, RequestStatus.COMPLETED);
        long unrelated = fixtureRequest(otherOwner, other, RequestStatus.ACCEPTED);
        long untouchedOpen = fixtureRequest(otherOwner, null, RequestStatus.OPEN);
        long untouchedCancelled = fixtureRequest(owner, null, RequestStatus.CANCELLED);
        long openComplaint = report(accepted, owner);
        long completedComplaint = report(completed, owner);
        long closedComplaint = report(secondAccepted, otherOwner);
        request("PUT", "/api/admin/complaints/" + closedComplaint + "/dismiss", admin, null, 200);
        long unrelatedComplaint = report(unrelated, otherOwner);
        Map<String, Object> acceptedBefore = requestRow(accepted);
        Map<String, Object> secondBefore = requestRow(secondAccepted);
        Map<String, Object> completedBefore = requestRow(completed);
        Map<String, Object> unrelatedBefore = requestRow(unrelated);
        Map<String, Object> openBefore = requestRow(untouchedOpen);
        Map<String, Object> cancelledBefore = requestRow(untouchedCancelled);
        Map<String, Object> openComplaintBefore = complaintRow(openComplaint);
        Map<String, Object> completedComplaintBefore = complaintRow(completedComplaint);
        Map<String, Object> closedBefore = complaintRow(closedComplaint);
        Map<String, Object> unrelatedReportBefore = complaintRow(unrelatedComplaint);
        Map<String, Object> targetBefore = userState(target.id());
        Map<String, Object> otherBefore = userState(other.id());
        DatabaseState beforeRemoval = databaseState();

        JsonNode removed = request("PUT", "/api/admin/helpers/" + target.id() + "/remove", admin,
                Map.of("reason", "Repeated missed appointments in this test."), 200);
        assertHelperRemoved(removed, target, targetBefore);
        assertReopened(accepted, owner, acceptedBefore);
        assertReopened(secondAccepted, otherOwner, secondBefore);
        assertCompletedRequestPreserved(completed, owner, target, completedBefore);
        assertComplaintRemoved(openComplaint, openComplaintBefore);
        assertComplaintRemoved(completedComplaint, completedComplaintBefore);
        assertEquals(closedBefore, complaintRow(closedComplaint));
        assertEquals(unrelatedReportBefore, complaintRow(unrelatedComplaint));
        assertEquals(unrelatedBefore, requestRow(unrelated));
        assertEquals(openBefore, requestRow(untouchedOpen));
        assertEquals(cancelledBefore, requestRow(untouchedCancelled));
        assertEquals(otherBefore, userState(other.id()));
        assertDatabaseInventoryPreserved(beforeRemoval);
        assertOpenComplaintsAfterRemoval(openComplaint, completedComplaint, unrelatedComplaint);
        assertAvailableWorkAfterRemoval(owner, target, other, accepted, secondAccepted);

        assertRemovedHelperCannotAccessRequests(target, accepted, untouchedOpen);
        accept(accepted, other);
        assertRequestState(accepted, owner, other, RequestStatus.ACCEPTED);
        assertRequestState(secondAccepted, otherOwner, null, RequestStatus.OPEN);
        assertRepeatedModerationRejected(target, openComplaint);
    }

    private void assertHelperRemoved(JsonNode response, Actor helper, Map<String, Object> before) {
        assertEquals(helper.id(), response.path("id").asLong());
        assertEquals("REMOVED", response.path("helperStatus").asString());
        Map<String, Object> expected = new HashMap<>(before);
        expected.put("helper_status", "REMOVED");
        assertEquals(expected, userState(helper.id()));
    }

    private void assertCompletedRequestPreserved(long id, Actor owner, Actor helper,
                                               Map<String, Object> before) throws Exception {
        assertRequestState(id, owner, helper, RequestStatus.COMPLETED);
        assertEquals(before, requestRow(id));
        assertContacts(detail(id, owner), helper);
        assertContacts(detail(id, helper), owner);
    }

    private void assertDatabaseInventoryPreserved(DatabaseState beforeRemoval) {
        DatabaseState afterRemoval = databaseState();
        assertEquals(beforeRemoval.requests().size(), afterRemoval.requests().size());
        assertEquals(beforeRemoval.complaints().size(), afterRemoval.complaints().size());
        assertEquals(beforeRemoval.reviews(), afterRemoval.reviews());
        assertEquals(beforeRemoval.categories(), afterRemoval.categories());
        assertEquals(beforeRemoval.users().size(), afterRemoval.users().size());
    }

    private void assertOpenComplaintsAfterRemoval(long openComplaint, long completedComplaint,
                                                long unrelatedComplaint) throws Exception {
        JsonNode openReports = get("/api/admin/complaints", admin);
        assertAbsent(openReports, openComplaint);
        assertAbsent(openReports, completedComplaint);
        item(openReports, unrelatedComplaint);
    }

    private void assertAvailableWorkAfterRemoval(Actor owner, Actor removedHelper, Actor otherHelper,
                                               long accepted, long secondAccepted) throws Exception {
        assertAbsent(get("/api/admin/helpers/verified", admin), removedHelper.id());
        item(get("/api/admin/helpers/verified", admin), otherHelper.id());
        assertHiddenContacts(detail(accepted, owner));
        assertHiddenContacts(item(get("/api/requests/open", otherHelper), accepted));
        item(get("/api/requests/open", otherHelper), secondAccepted);
    }

    private void assertRemovedHelperCannotAccessRequests(Actor helper, long releasedRequest,
                                                       long untouchedOpenRequest) throws Exception {
        assertRejected("GET", "/api/requests/" + releasedRequest, helper, null, 403);
        assertRejected("POST", "/api/requests/" + releasedRequest + "/accept", helper, null, 403);
        assertRejected("POST", "/api/requests/" + untouchedOpenRequest + "/accept", helper, null, 403);
    }

    private void assertRepeatedModerationRejected(Actor helper, long removedComplaint) throws Exception {
        assertRejected("PUT", "/api/admin/helpers/" + helper.id() + "/remove", admin,
                Map.of("reason", "Repeated removal"), 409);
        assertRejected("PUT", "/api/admin/helpers/" + helper.id() + "/verify", admin, null, 409);
        assertRejected("PUT", "/api/admin/complaints/" + removedComplaint + "/dismiss", admin, null, 409);
    }

    private long report(long id, Actor owner) throws Exception {
        return request("POST", "/api/requests/" + id + "/complaint", owner,
                Map.of("reason", "Helper missed an appointment in this test."), 201).path("id").asLong();
    }

    private Map<String, Object> complaintRow(long id) {
        return jdbc.queryForMap("select * from complaint where id = ?", id);
    }

    private Map<String, Object> userState(long id) {
        return jdbc.queryForMap("select id, role, helper_status from users where id = ?", id);
    }

    private void assertComplaintRemoved(long id, Map<String, Object> before) {
        Map<String, Object> expected = new HashMap<>(before);
        expected.put("status", "REMOVED");
        assertEquals(expected, complaintRow(id));
    }

    private void assertReopened(long id, Actor owner, Map<String, Object> before) {
        assertRequestState(id, owner, null, RequestStatus.OPEN);
        Map<String, Object> expected = new HashMap<>(before);
        expected.put("status", "OPEN");
        expected.put("helper_id", null);
        expected.put("version", ((Number) before.get("version")).longValue() + 1);
        assertEquals(expected, requestRow(id));
    }

    private void assertReview(JsonNode response, int rating, String comment) {
        assertEquals(rating, response.path("reviewRating").asInt());
        if (comment == null) {
            assertTrue(response.has("reviewComment") && response.get("reviewComment").isNull());
        } else {
            assertEquals(comment, response.path("reviewComment").asString());
        }
    }
}
