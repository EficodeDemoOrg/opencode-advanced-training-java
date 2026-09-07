package com.atlas.inventory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryServerTest {
    @TempDir
    Path temporaryDirectory;

    private InventoryRepository repository;
    private InventoryServer server;
    private HttpClient client;
    private String baseUrl;

    @BeforeEach
    void setUp() throws Exception {
        repository = new InventoryRepository(temporaryDirectory.resolve("inventory.db"));
        repository.initialize();
        server = new InventoryServer(0, new InventoryService(repository));
        server.start();
        client = HttpClient.newHttpClient();
        baseUrl = "http://localhost:" + server.port();
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void servesTheUserInterfaceAndHealthCheck() throws Exception {
        HttpResponse<String> page = send("GET", "/", null, null);
        HttpResponse<String> health = send("GET", "/health", null, null);

        assertEquals(200, page.statusCode());
        assertTrue(page.headers().firstValue("Content-Type").orElse("").startsWith("text/html"));
        assertTrue(page.body().contains("Atlas Inventory"));
        assertEquals(200, health.statusCode());
        assertEquals("{\"status\":\"ok\"}", health.body());
    }

    @Test
    void listsAndRetrievesItems() throws Exception {
        HttpResponse<String> list = send("GET", "/api/items", null, null);
        long firstId = repository.findAll().get(0).id();
        HttpResponse<String> item = send("GET", "/api/items/" + firstId, null, null);

        assertEquals(200, list.statusCode());
        assertTrue(list.body().startsWith("["));
        assertTrue(list.body().contains("\"partNumber\":\"PCB-CTRL-100\""));
        assertEquals(200, item.statusCode());
        assertTrue(item.body().contains("\"id\":" + firstId));
    }

    @Test
    void createsUpdatesAndDeletesAnItem() throws Exception {
        HttpResponse<String> created = sendForm("POST", "/api/items",
                itemForm("IC-555", "555 Timer", "Integrated Circuit", "C-09-01", 24, 6));

        assertEquals(201, created.statusCode());
        InventoryItem item = repository.findAll().stream()
                .filter(candidate -> candidate.partNumber().equals("IC-555"))
                .findFirst()
                .orElseThrow();

        HttpResponse<String> updated = sendForm("PUT", "/api/items/" + item.id(),
                itemForm("IC-555", "555 Timer IC", "Integrated Circuit", "C-09-02", 30, 8));
        assertEquals(200, updated.statusCode());
        assertTrue(updated.body().contains("\"name\":\"555 Timer IC\""));
        assertEquals(30, repository.findById(item.id()).orElseThrow().quantity());

        HttpResponse<String> deleted = send("DELETE", "/api/items/" + item.id(), null, null);
        assertEquals(204, deleted.statusCode());
        assertFalse(repository.findById(item.id()).isPresent());
    }

    @Test
    void rejectsInvalidCreateRequests() throws Exception {
        HttpResponse<String> negativeQuantity = sendForm("POST", "/api/items",
                itemForm("BAD-STOCK", "Invalid Stock", "Component", "A-01-01", -1, 2));
        HttpResponse<String> missingName = sendForm("POST", "/api/items",
                itemForm("BAD-NAME", "", "Component", "A-01-01", 1, 2));

        assertEquals(400, negativeQuantity.statusCode());
        assertTrue(negativeQuantity.body().contains("Quantity cannot be negative"));
        assertEquals(400, missingName.statusCode());
        assertTrue(missingName.body().contains("Name is required"));
    }

    @Test
    void reportsDuplicatePartNumbersAsConflict() throws Exception {
        Map<String, String> form = itemForm(
                "PCB-CTRL-100", "Another Board", "Circuit Board", "A-09-09", 2, 1);

        HttpResponse<String> response = sendForm("POST", "/api/items", form);

        assertEquals(409, response.statusCode());
        assertTrue(response.body().contains("part number already exists"));
    }

    @Test
    void reportsMissingItemsAndInvalidPaths() throws Exception {
        HttpResponse<String> missing = send("GET", "/api/items/999999", null, null);
        HttpResponse<String> invalid = send("GET", "/api/items/not-a-number", null, null);

        assertEquals(404, missing.statusCode());
        assertEquals(400, invalid.statusCode());
    }

    @Test
    void rejectsUnsupportedMethodsAndContentTypes() throws Exception {
        HttpResponse<String> unsupported = send("PATCH", "/api/items", null, null);
        HttpResponse<String> wrongContentType = send(
                "POST", "/api/items", "application/json", "{}");

        assertEquals(405, unsupported.statusCode());
        assertEquals("GET, POST", unsupported.headers().firstValue("Allow").orElseThrow());
        assertEquals(400, wrongContentType.statusCode());
    }

    private HttpResponse<String> sendForm(String method, String path, Map<String, String> values)
            throws Exception {
        String body = values.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
        return send(method, path, "application/x-www-form-urlencoded;charset=UTF-8", body);
    }

    private HttpResponse<String> send(String method, String path, String contentType, String body)
            throws Exception {
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body);
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .method(method, publisher);
        if (contentType != null) {
            request.header("Content-Type", contentType);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static Map<String, String> itemForm(String partNumber, String name, String category,
                                                 String location, int quantity, int reorderLevel) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("partNumber", partNumber);
        form.put("name", name);
        form.put("category", category);
        form.put("storageLocation", location);
        form.put("quantity", Integer.toString(quantity));
        form.put("reorderLevel", Integer.toString(reorderLevel));
        form.put("description", "Created during an integration test");
        return form;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
