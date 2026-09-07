package com.atlas.inventory;

import com.atlas.inventory.InventoryService.InventoryException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class InventoryServer {
    private static final int MAX_REQUEST_BYTES = 16_384;

    private final HttpServer server;
    private final InventoryService service;
    private final ExecutorService executor;

    public InventoryServer(int port, InventoryService service) throws IOException {
        this.service = service;
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        server.createContext("/", this::handle);
        executor = Executors.newFixedThreadPool(8);
        server.setExecutor(executor);
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
        executor.shutdownNow();
    }

    public int port() {
        return server.getAddress().getPort();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            route(exchange);
        } catch (InventoryException exception) {
            int status = switch (exception.type()) {
                case INVALID_INPUT -> 400;
                case NOT_FOUND -> 404;
                case CONFLICT -> 409;
            };
            sendJson(exchange, status, errorJson(exception.getMessage()));
        } catch (SQLException exception) {
            exception.printStackTrace();
            sendJson(exchange, 500, errorJson("The inventory database is unavailable"));
        } catch (IllegalArgumentException exception) {
            sendJson(exchange, 400, errorJson(exception.getMessage()));
        } catch (Exception exception) {
            exception.printStackTrace();
            sendJson(exchange, 500, errorJson("An unexpected server error occurred"));
        } finally {
            exchange.close();
        }
    }

    private void route(HttpExchange exchange) throws Exception {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if (path.equals("/health")) {
            if (!requireMethod(exchange, "GET")) {
                return;
            }
            if (service.isHealthy()) {
                sendJson(exchange, 200, "{\"status\":\"ok\"}");
            } else {
                sendJson(exchange, 503, "{\"status\":\"unavailable\"}");
            }
            return;
        }

        if (path.equals("/api/items")) {
            if (method.equals("GET")) {
                sendJson(exchange, 200, itemsJson(service.findAll()));
            } else if (method.equals("POST")) {
                requireFormContentType(exchange);
                InventoryItem created = service.create(parseItem(readForm(exchange)));
                sendJson(exchange, 201, created.toJson());
            } else {
                methodNotAllowed(exchange, "GET, POST");
            }
            return;
        }

        if (path.startsWith("/api/items/")) {
            long id = parseId(path.substring("/api/items/".length()));
            switch (method) {
                case "GET" -> sendJson(exchange, 200, service.findById(id).toJson());
                case "PUT" -> {
                    requireFormContentType(exchange);
                    sendJson(exchange, 200, service.update(id, parseItem(readForm(exchange))).toJson());
                }
                case "DELETE" -> {
                    service.delete(id);
                    exchange.sendResponseHeaders(204, -1);
                }
                default -> methodNotAllowed(exchange, "GET, PUT, DELETE");
            }
            return;
        }

        serveStatic(exchange, path);
    }

    private static void serveStatic(HttpExchange exchange, String path) throws IOException {
        if (!requireMethod(exchange, "GET")) {
            return;
        }
        String resourcePath;
        String contentType;
        switch (path) {
            case "/", "/index.html" -> {
                resourcePath = "/static/index.html";
                contentType = "text/html; charset=utf-8";
            }
            case "/styles.css" -> {
                resourcePath = "/static/styles.css";
                contentType = "text/css; charset=utf-8";
            }
            case "/app.js" -> {
                resourcePath = "/static/app.js";
                contentType = "text/javascript; charset=utf-8";
            }
            default -> {
                sendJson(exchange, 404, errorJson("Resource not found"));
                return;
            }
        }

        try (InputStream stream = InventoryServer.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                sendJson(exchange, 404, errorJson("Resource not found"));
                return;
            }
            send(exchange, 200, contentType, stream.readAllBytes());
        }
    }

    private static Map<String, String> readForm(HttpExchange exchange) throws IOException {
        byte[] body = exchange.getRequestBody().readNBytes(MAX_REQUEST_BYTES + 1);
        if (body.length > MAX_REQUEST_BYTES) {
            throw new IllegalArgumentException("Request body is too large");
        }

        Map<String, String> values = new HashMap<>();
        String encoded = new String(body, StandardCharsets.UTF_8);
        if (encoded.isBlank()) {
            return values;
        }
        for (String pair : encoded.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = decode(parts[0]);
            String value = parts.length == 2 ? decode(parts[1]) : "";
            values.put(key, value);
        }
        return values;
    }

    private static InventoryItem parseItem(Map<String, String> form) {
        return new InventoryItem(
                0,
                form.get("partNumber"),
                form.get("name"),
                form.get("category"),
                form.get("storageLocation"),
                parseInteger(form, "quantity", "Quantity"),
                parseInteger(form, "reorderLevel", "Reorder level"),
                form.get("description"));
    }

    private static int parseInteger(Map<String, String> form, String key, String label) {
        String value = form.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(label + " must be a whole number");
        }
    }

    private static long parseId(String value) {
        if (value.isBlank() || value.contains("/")) {
            throw new IllegalArgumentException("Invalid inventory item identifier");
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid inventory item identifier");
        }
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String itemsJson(List<InventoryItem> items) {
        return items.stream()
                .map(InventoryItem::toJson)
                .reduce("[", (json, item) -> json.equals("[") ? json + item : json + "," + item)
                + "]";
    }

    private static String errorJson(String message) {
        return "{\"error\":" + jsonString(message) + "}";
    }

    private static String jsonString(String value) {
        StringBuilder escaped = new StringBuilder("\"");
        for (char character : value.toCharArray()) {
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> escaped.append(character);
            }
        }
        return escaped.append('"').toString();
    }

    private static void requireFormContentType(HttpExchange exchange) {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null
                || !contentType.toLowerCase().startsWith("application/x-www-form-urlencoded")) {
            throw new IllegalArgumentException(
                    "Content-Type must be application/x-www-form-urlencoded");
        }
    }

    private static boolean requireMethod(HttpExchange exchange, String allowed) throws IOException {
        if (!exchange.getRequestMethod().equals(allowed)) {
            methodNotAllowed(exchange, allowed);
            return false;
        }
        return true;
    }

    private static void methodNotAllowed(HttpExchange exchange, String allowed) throws IOException {
        exchange.getResponseHeaders().set("Allow", allowed);
        sendJson(exchange, 405, errorJson("Method not allowed"));
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        send(exchange, status, "application/json; charset=utf-8",
                json.getBytes(StandardCharsets.UTF_8));
    }

    private static void send(HttpExchange exchange, int status, String contentType, byte[] body)
            throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
    }
}
