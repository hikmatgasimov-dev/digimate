package com.digimate.support;

import com.digimate.entity.Category;
import com.digimate.entity.HelpRequest;
import com.digimate.entity.User;
import com.digimate.entity.enums.HelperStatus;
import com.digimate.entity.enums.RequestStatus;
import com.digimate.entity.enums.Role;
import com.digimate.repository.CategoryRepository;
import com.digimate.repository.HelpRequestRepository;
import com.digimate.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real HTTP and committed PostgreSQL reads. No test transaction surrounds HTTP writes.
 * Each scenario owns a unique category/users; closing the class drops its test schema.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
public abstract class HttpE2ETestSupport {

    protected static final String PASSWORD = "e2e-only-password";

    @LocalServerPort
    private int port;

    @Autowired protected UserRepository userRepository;
    @Autowired protected CategoryRepository categoryRepository;
    @Autowired protected HelpRequestRepository helpRequestRepository;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    protected Category category;
    protected Actor admin;
    private HttpClient httpClient;
    private int actorNumber;

    @BeforeEach
    void prepareHttpAndFixtures() throws Exception {
        assertEquals("digimate_test", jdbc.queryForObject("select current_database()", String.class));
        httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        category = TestData.category();
        category.setName("E2E " + UUID.randomUUID());
        category = categoryRepository.saveAndFlush(category);
        admin = fixtureActor("Admin", Role.ADMIN, HelperStatus.NONE);
    }

    @AfterEach
    void closeHttpClient() {
        if (httpClient != null) {
            // Bounded cleanup even when an HTTP assertion or the acceptance race fails.
            httpClient.shutdownNow();
            try {
                assertTrue(httpClient.awaitTermination(Duration.ofSeconds(5)), "HTTP client did not terminate");
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                fail("Interrupted while closing the HTTP client", ex);
            }
        }
    }

    protected Actor customer() throws Exception {
        return fixtureActor("Customer", Role.USER, HelperStatus.NONE);
    }

    protected Actor helper(HelperStatus status) throws Exception {
        return fixtureActor("Helper", Role.USER, status);
    }

    protected Actor fixtureActor(String label, Role role, HelperStatus status) throws Exception {
        User user = TestData.customer();
        user.setFullName("E2E " + label + " " + ++actorNumber);
        user.setEmail(UUID.randomUUID() + "@example.test");
        user.setPhone("+3712000" + String.format("%04d", actorNumber));
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setRole(role);
        user.setHelperStatus(status);
        user = userRepository.saveAndFlush(user);
        return new Actor(user.getId(), user.getFullName(), user.getEmail(), user.getPhone(), login(user.getEmail()));
    }

    protected Actor register(String label, boolean wantsToHelp) throws Exception {
        String name = "E2E " + label + " " + ++actorNumber;
        String email = UUID.randomUUID() + "@example.test";
        String phone = "+3712000" + String.format("%04d", actorNumber);
        long usersBefore = userRepository.count();
        JsonNode registered = request("POST", "/api/auth/register", null, Map.of(
                "fullName", name, "email", email, "phone", phone,
                "password", PASSWORD, "wantsToHelp", wantsToHelp), 201);
        long id = registered.path("id").asLong();
        assertAll(
                () -> assertTrue(id > 0),
                () -> assertEquals(name, registered.path("fullName").asString()),
                () -> assertEquals(email, registered.path("email").asString()),
                () -> assertEquals("USER", registered.path("role").asString()),
                () -> assertEquals(wantsToHelp ? "PENDING" : "NONE", registered.path("helperStatus").asString()),
                () -> assertFalse(registered.has("password")),
                () -> assertFalse(registered.has("passwordHash")),
                () -> assertEquals(usersBefore + 1, userRepository.count()));
        User saved = userRepository.findById(id).orElseThrow();
        assertAll(
                () -> assertEquals(Role.USER, saved.getRole()),
                () -> assertEquals(wantsToHelp ? HelperStatus.PENDING : HelperStatus.NONE, saved.getHelperStatus()),
                () -> assertEquals(name, saved.getFullName()),
                () -> assertEquals(email, saved.getEmail()),
                () -> assertEquals(phone, saved.getPhone()),
                () -> assertTrue(passwordEncoder.matches(PASSWORD, saved.getPasswordHash())));
        return new Actor(id, name, email, phone, login(email));
    }

    private String login(String email) throws Exception {
        JsonNode response = request("POST", "/api/auth/login", null,
                Map.of("email", email, "password", PASSWORD), 200);
        String token = response.path("token").asString();
        assertFalse(token.isBlank(), "API login must issue a JWT");
        assertEquals("Bearer", response.path("type").asString());
        return token;
    }

    protected JsonNode request(String method, String path, Actor actor, Object body, int expected) throws Exception {
        Reply reply = send(method, path, actor == null ? null : actor.token(), body);
        assertEquals(expected, reply.status(), method + " " + path);
        return reply.body();
    }

    protected Reply send(String method, String path, String token, Object body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(15)).header("Accept", "application/json");
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        HttpRequest.BodyPublisher publisher = HttpRequest.BodyPublishers.noBody();
        if (body != null) {
            builder.header("Content-Type", "application/json");
            publisher = HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body));
        }
        HttpResponse<String> response = httpClient.send(builder.method(method, publisher).build(),
                HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(), response.body().isBlank() ? null : json.readTree(response.body()));
    }

    protected JsonNode get(String path, Actor actor) throws Exception {
        return request("GET", path, actor, null, 200);
    }

    protected Map<String, Object> requestBody(String title) {
        return Map.of("title", title, "description", "Help with a video call on a test laptop.",
                "categoryId", category.getId(), "preferredTime", LocalDateTime.now().plusDays(3).withNano(0),
                "mode", "ONLINE", "place", "Video call");
    }

    protected long createRequest(Actor owner) throws Exception {
        long before = helpRequestRepository.count();
        Map<String, Object> input = requestBody("Set up a video call");
        JsonNode created = request("POST", "/api/requests", owner, input, 201);
        long id = created.path("id").asLong();
        Map<String, Object> stored = requestRow(id);
        assertTrue(id > 0);
        assertEquals(before + 1, helpRequestRepository.count());
        assertEquals("OPEN", created.path("status").asString());
        assertEquals(input.get("title"), created.path("title").asString());
        assertEquals(input.get("description"), created.path("description").asString());
        assertEquals(category.getName(), created.path("categoryName").asString());
        assertEquals(input.get("preferredTime"), LocalDateTime.parse(created.path("preferredTime").asString()));
        assertEquals("ONLINE", created.path("mode").asString());
        assertEquals("Video call", created.path("place").asString());
        assertFalse(created.path("createdAt").isNull());
        assertEquals(input.get("title"), stored.get("title"));
        assertEquals(input.get("description"), stored.get("description"));
        assertEquals(category.getId(), stored.get("category_id"));
        assertEquals(input.get("preferredTime"), ((java.sql.Timestamp) stored.get("preferred_time")).toLocalDateTime());
        assertEquals("ONLINE", stored.get("mode"));
        assertEquals("Video call", stored.get("place"));
        assertNotNull(stored.get("created_at"));
        assertNotNull(stored.get("version"));
        assertHiddenContacts(created);
        assertRequestState(id, owner, null, RequestStatus.OPEN);
        return id;
    }

    protected long fixtureRequest(Actor owner, Actor helper, RequestStatus status) {
        HelpRequest request = TestData.openRequest(userRepository.findById(owner.id()).orElseThrow(), category);
        request.setStatus(status);
        request.setHelper(helper == null ? null : userRepository.findById(helper.id()).orElseThrow());
        return helpRequestRepository.saveAndFlush(request).getId();
    }

    protected void verify(Actor helper) throws Exception {
        JsonNode verified = request("PUT", "/api/admin/helpers/" + helper.id() + "/verify", admin, null, 200);
        assertEquals(helper.id(), verified.path("id").asLong());
        assertEquals("VERIFIED", verified.path("helperStatus").asString());
        assertEquals(HelperStatus.VERIFIED, userRepository.findById(helper.id()).orElseThrow().getHelperStatus());
    }

    protected JsonNode accept(long id, Actor helper) throws Exception {
        JsonNode accepted = request("POST", "/api/requests/" + id + "/accept", helper, null, 200);
        assertEquals("ACCEPTED", accepted.path("status").asString());
        return accepted;
    }

    protected JsonNode detail(long id, Actor viewer) throws Exception {
        return get("/api/requests/" + id, viewer);
    }

    protected void assertRequestState(long id, Actor owner, Actor helper, RequestStatus status) {
        Map<String, Object> row = requestRow(id);
        assertAll(
                () -> assertEquals(owner.id(), row.get("customer_id")),
                () -> assertEquals(helper == null ? null : helper.id(), row.get("helper_id")),
                () -> assertEquals(status.name(), row.get("status")));
    }

    protected Map<String, Object> requestRow(long id) {
        return jdbc.queryForMap("select * from help_request where id = ?", id);
    }

    protected void assertContacts(JsonNode response, Actor counterpart) {
        assertAll(
                () -> assertEquals(counterpart.name(), response.path("counterpartName").asString()),
                () -> assertEquals(counterpart.phone(), response.path("counterpartPhone").asString()),
                () -> assertEquals(counterpart.email(), response.path("counterpartEmail").asString()));
    }

    protected void assertHiddenContacts(JsonNode response) {
        for (String field : List.of("counterpartName", "counterpartPhone", "counterpartEmail")) {
            assertTrue(response.has(field) && response.get(field).isNull(), field + " must be null");
        }
    }

    protected JsonNode item(JsonNode list, long id) {
        assertTrue(list.isArray(), "Expected an API list");
        for (JsonNode item : list) {
            if (item.path("id").asLong() == id) {
                return item;
            }
        }
        return fail("ID " + id + " absent from API list");
    }

    protected void assertAbsent(JsonNode list, long id) {
        assertTrue(list.isArray(), "Expected an API list");
        for (JsonNode item : list) {
            assertNotEquals(id, item.path("id").asLong(), "ID must be absent from API list");
        }
    }

    protected void assertRejected(String method, String path, Actor actor, Object body, int status) throws Exception {
        DatabaseState before = databaseState();
        Reply reply = send(method, path, actor == null ? null : actor.token(), body);
        assertAll(method + " " + path,
                () -> assertEquals(status, reply.status()),
                () -> assertEquals(before, databaseState(), "A rejected action must leave stored data unchanged"));
    }

    /** JDBC reads bypass Hibernate's persistence context and include row counts and versions. */
    protected DatabaseState databaseState() {
        return new DatabaseState(
                jdbc.queryForList("select * from help_request order by id"),
                jdbc.queryForList("select * from review order by id"),
                jdbc.queryForList("select * from complaint order by id"),
                jdbc.queryForList("select id, role, helper_status from users order by id"),
                jdbc.queryForList("select * from category order by id"));
    }

    protected record Actor(long id, String name, String email, String phone, String token) {
        @Override
        public String toString() {
            return "Actor[id=" + id + "]";
        }
    }

    protected record Reply(int status, JsonNode body) {
    }

    protected record DatabaseState(List<Map<String, Object>> requests, List<Map<String, Object>> reviews,
                                   List<Map<String, Object>> complaints, List<Map<String, Object>> users,
                                   List<Map<String, Object>> categories) {
    }
}
