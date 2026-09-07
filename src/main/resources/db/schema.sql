CREATE TABLE IF NOT EXISTS inventory_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    part_number TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    category TEXT NOT NULL,
    storage_location TEXT NOT NULL,
    quantity INTEGER NOT NULL,
    reorder_level INTEGER NOT NULL,
    description TEXT NOT NULL DEFAULT ''
);
