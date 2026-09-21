package com.atlas.inventory;

import java.sql.SQLException;
import java.util.List;

public final class InventoryService {
    private static final int PART_NUMBER_MAX_LENGTH = 50;
    private static final int NAME_MAX_LENGTH = 100;
    private static final int CATEGORY_MAX_LENGTH = 50;
    private static final int LOCATION_MAX_LENGTH = 50;
    private static final int DESCRIPTION_MAX_LENGTH = 500;

    private final InventoryRepository repository;

    public InventoryService(InventoryRepository repository) {
        this.repository = repository;
    }

    public List<InventoryItem> findAll() throws SQLException {
        return repository.findAll();
    }

    public InventoryItem findById(long id) throws SQLException {
        requirePositiveId(id);
        return repository.findById(id)
                .orElseThrow(() -> new InventoryException(ErrorType.NOT_FOUND,
                        "Inventory item " + id + " was not found"));
    }

    public InventoryItem create(InventoryItem item) throws SQLException {
        InventoryItem validatedItem = validateForCreate(item);
        try {
            return repository.create(validatedItem);
        } catch (SQLException exception) {
            throw translateConstraintViolation(exception);
        }
    }

    public InventoryItem update(long id, InventoryItem item) throws SQLException {
        requirePositiveId(id);
        InventoryItem validatedItem = validateForUpdate(item).withId(id);
        try {
            if (!repository.update(id, validatedItem)) {
                throw new InventoryException(ErrorType.NOT_FOUND,
                        "Inventory item " + id + " was not found");
            }
            return validatedItem;
        } catch (SQLException exception) {
            throw translateConstraintViolation(exception);
        }
    }

    public void delete(long id) throws SQLException {
        requirePositiveId(id);
        if (!repository.delete(id)) {
            throw new InventoryException(ErrorType.NOT_FOUND,
                    "Inventory item " + id + " was not found");
        }
    }

    public boolean isHealthy() {
        return repository.isHealthy();
    }

    public static int totalReorderShortage(List<InventoryItem> items) {
        int total = 0;
        for (InventoryItem item : items) {
            total += item.reorderShortage();
        }
        return total;
    }

    private static InventoryItem validateForCreate(InventoryItem item) {
        InventoryItem normalized = normalize(item);
        validateTextFields(normalized);
        if (normalized.quantity() < 0) {
            throw invalid("Quantity cannot be negative");
        }
        if (normalized.reorderLevel() < 0) {
            throw invalid("Reorder level cannot be negative");
        }
        return normalized.withId(0);
    }

    private static InventoryItem validateForUpdate(InventoryItem item) {
        InventoryItem normalized = normalize(item);
        validateTextFields(normalized);
        if (normalized.reorderLevel() < 0) {
            throw invalid("Reorder level cannot be negative");
        }
        return normalized;
    }

    private static InventoryItem normalize(InventoryItem item) {
        if (item == null) {
            throw invalid("An inventory item is required");
        }
        return new InventoryItem(
                item.id(),
                trim(item.partNumber()),
                trim(item.name()),
                trim(item.category()),
                trim(item.storageLocation()),
                item.quantity(),
                item.reorderLevel(),
                trim(item.description()));
    }

    private static void validateTextFields(InventoryItem item) {
        requireText("Part number", item.partNumber(), PART_NUMBER_MAX_LENGTH);
        requireText("Name", item.name(), NAME_MAX_LENGTH);
        requireText("Category", item.category(), CATEGORY_MAX_LENGTH);
        requireText("Storage location", item.storageLocation(), LOCATION_MAX_LENGTH);
        if (item.description().length() > DESCRIPTION_MAX_LENGTH) {
            throw invalid("Description must be " + DESCRIPTION_MAX_LENGTH + " characters or fewer");
        }
    }

    private static void requireText(String field, String value, int maximumLength) {
        if (value.isEmpty()) {
            throw invalid(field + " is required");
        }
        if (value.length() > maximumLength) {
            throw invalid(field + " must be " + maximumLength + " characters or fewer");
        }
    }

    private static void requirePositiveId(long id) {
        if (id <= 0) {
            throw invalid("Inventory item identifier must be positive");
        }
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static InventoryException translateConstraintViolation(SQLException exception)
            throws SQLException {
        if (exception.getErrorCode() == 19
                && exception.getMessage() != null
                && exception.getMessage().contains("inventory_items.part_number")) {
            return new InventoryException(ErrorType.CONFLICT,
                    "An inventory item with that part number already exists");
        }
        throw exception;
    }

    private static InventoryException invalid(String message) {
        return new InventoryException(ErrorType.INVALID_INPUT, message);
    }

    public enum ErrorType {
        INVALID_INPUT,
        NOT_FOUND,
        CONFLICT
    }

    public static final class InventoryException extends RuntimeException {
        private final ErrorType type;

        public InventoryException(ErrorType type, String message) {
            super(message);
            this.type = type;
        }

        public ErrorType type() {
            return type;
        }
    }
}
