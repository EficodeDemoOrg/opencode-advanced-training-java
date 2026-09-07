package com.atlas.inventory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class InventoryRepository {
    private static final String COLUMNS =
            "id, part_number, name, category, storage_location, quantity, reorder_level, description";

    private final String databaseUrl;

    public InventoryRepository(Path databasePath) {
        this(databasePath.toString().equals(":memory:")
                ? "jdbc:sqlite::memory:"
                : "jdbc:sqlite:" + databasePath.toAbsolutePath());
    }

    public InventoryRepository(String databaseUrl) {
        this.databaseUrl = databaseUrl;
    }

    public void initialize() throws SQLException, IOException {
        createParentDirectory();
        try (Connection connection = openConnection()) {
            executeScript(connection, readResource("/db/schema.sql"));
            if (count(connection) == 0) {
                executeScript(connection, readResource("/db/seed.sql"));
            }
        }
    }

    public List<InventoryItem> findAll() throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM inventory_items ORDER BY name, part_number";
        List<InventoryItem> items = new ArrayList<>();
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                items.add(mapItem(results));
            }
        }
        return items;
    }

    public Optional<InventoryItem> findById(long id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM inventory_items WHERE id = ?";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? Optional.of(mapItem(results)) : Optional.empty();
            }
        }
    }

    public InventoryItem create(InventoryItem item) throws SQLException {
        String sql = """
                INSERT INTO inventory_items
                    (part_number, name, category, storage_location, quantity, reorder_level, description)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindItem(statement, item);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Creating an inventory item did not return an identifier");
                }
                return item.withId(keys.getLong(1));
            }
        }
    }

    public boolean update(long id, InventoryItem item) throws SQLException {
        String sql = """
                UPDATE inventory_items
                SET part_number = ?, name = ?, category = ?, storage_location = ?,
                    quantity = ?, reorder_level = ?, description = ?
                WHERE id = ?
                """;
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindItem(statement, item);
            statement.setLong(8, id);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean delete(long id) throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM inventory_items WHERE id = ?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean isHealthy() {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1");
             ResultSet results = statement.executeQuery()) {
            return results.next() && results.getInt(1) == 1;
        } catch (SQLException exception) {
            return false;
        }
    }

    private Connection openConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(databaseUrl);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
        }
        return connection;
    }

    private void createParentDirectory() throws IOException {
        if (!databaseUrl.startsWith("jdbc:sqlite:") || databaseUrl.equals("jdbc:sqlite::memory:")) {
            return;
        }
        String fileName = databaseUrl.substring("jdbc:sqlite:".length());
        Path parent = Path.of(fileName).toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private static int count(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM inventory_items");
             ResultSet results = statement.executeQuery()) {
            return results.next() ? results.getInt(1) : 0;
        }
    }

    private static void bindItem(PreparedStatement statement, InventoryItem item) throws SQLException {
        statement.setString(1, item.partNumber());
        statement.setString(2, item.name());
        statement.setString(3, item.category());
        statement.setString(4, item.storageLocation());
        statement.setInt(5, item.quantity());
        statement.setInt(6, item.reorderLevel());
        statement.setString(7, item.description());
    }

    private static InventoryItem mapItem(ResultSet results) throws SQLException {
        return new InventoryItem(
                results.getLong("id"),
                results.getString("part_number"),
                results.getString("name"),
                results.getString("category"),
                results.getString("storage_location"),
                results.getInt("quantity"),
                results.getInt("reorder_level"),
                results.getString("description"));
    }

    private static String readResource(String path) throws IOException {
        try (InputStream stream = InventoryRepository.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("Missing resource: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void executeScript(Connection connection, String script) throws SQLException {
        for (String statementSql : script.split(";")) {
            if (!statementSql.isBlank()) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(statementSql);
                }
            }
        }
    }
}
