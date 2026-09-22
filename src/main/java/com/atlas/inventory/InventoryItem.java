package com.atlas.inventory;

public record InventoryItem(
        long id,
        String partNumber,
        String name,
        String category,
        String storageLocation,
        int quantity,
        int reorderLevel,
        String description) {

    public InventoryItem withId(long newId) {
        return new InventoryItem(newId, partNumber, name, category, storageLocation,
                quantity, reorderLevel, description);
    }

    public int reorderShortage() {
        return Math.max(0, reorderLevel - quantity);
    }

    public enum StockStatus {
        OUT_OF_STOCK,
        LOW,
        OK
    }

    public StockStatus stockStatus() {
        if (quantity == 0) {
            return StockStatus.OUT_OF_STOCK;
        }
        if (quantity <= reorderLevel) {
            return StockStatus.LOW;
        }
        return StockStatus.OK;
    }

    public String toJson() {
        return "{" +
                "\"id\":" + id + "," +
                "\"partNumber\":" + quote(partNumber) + "," +
                "\"name\":" + quote(name) + "," +
                "\"category\":" + quote(category) + "," +
                "\"storageLocation\":" + quote(storageLocation) + "," +
                "\"quantity\":" + quantity + "," +
                "\"reorderLevel\":" + reorderLevel + "," +
                "\"description\":" + quote(description) +
                "}";
    }

    private static String quote(String value) {
        if (value == null) {
            return "null";
        }

        StringBuilder escaped = new StringBuilder(value.length() + 2).append('"');
        for (char character : value.toCharArray()) {
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.append('"').toString();
    }
}
