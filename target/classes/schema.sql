-- ========================================================
-- IU CANTEEN DIGITIZATION - SQLITE DATABASE SCHEMA
-- ========================================================

PRAGMA foreign_keys = ON;

-- 1. USERS
CREATE TABLE IF NOT EXISTS users (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    full_name   TEXT NOT NULL,
    email       TEXT NOT NULL UNIQUE,
    student_id  TEXT,
    password    TEXT NOT NULL,
    role        TEXT NOT NULL DEFAULT 'STUDENT',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 2. MENU ITEMS
CREATE TABLE IF NOT EXISTS menu_items (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT NOT NULL,
    category    TEXT NOT NULL,
    available   BOOLEAN DEFAULT 1,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 3. MENU ITEM SIZES
CREATE TABLE IF NOT EXISTS menu_item_sizes (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    menu_item_id       INTEGER NOT NULL REFERENCES menu_items(id) ON DELETE CASCADE,
    size_label         TEXT NOT NULL,
    price              INTEGER NOT NULL,
    portion_multiplier REAL DEFAULT 1.0,
    daily_limit        INTEGER DEFAULT 0,
    UNIQUE(menu_item_id, size_label)
);

-- 4. INGREDIENTS
CREATE TABLE IF NOT EXISTS ingredients (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    name            TEXT NOT NULL UNIQUE,
    unit            TEXT NOT NULL,
    shelf_life_days INTEGER NOT NULL,
    min_stock       REAL NOT NULL DEFAULT 0,
    current_stock   REAL NOT NULL DEFAULT 0,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 5. RECIPES (Định lượng cho size M chuẩn - nhân với portion_multiplier khi chọn size khác)
CREATE TABLE IF NOT EXISTS recipes (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    menu_item_id  INTEGER NOT NULL REFERENCES menu_items(id) ON DELETE CASCADE,
    ingredient_id INTEGER NOT NULL REFERENCES ingredients(id) ON DELETE RESTRICT,
    quantity      REAL NOT NULL,
    UNIQUE(menu_item_id, ingredient_id)
);

-- 6. INVENTORY BATCHES (Quản lý lô hạn sử dụng - FIFO)
CREATE TABLE IF NOT EXISTS inventory_batches (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    ingredient_id      INTEGER NOT NULL REFERENCES ingredients(id) ON DELETE RESTRICT,
    quantity           REAL NOT NULL,
    remaining_quantity REAL NOT NULL,
    cost_per_unit      REAL NOT NULL,
    received_date      DATE NOT NULL,
    expiry_date        DATE NOT NULL,
    batch_code         TEXT
);

-- 7. ORDERS
CREATE TABLE IF NOT EXISTS orders (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id        INTEGER REFERENCES users(id) ON DELETE SET NULL,
    customer_email TEXT NOT NULL,
    pickup_time    TEXT NOT NULL,
    note           TEXT,
    total_amount   INTEGER NOT NULL,
    status         TEXT NOT NULL DEFAULT 'PENDING',
    created_at     DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 8. ORDER ITEMS (Bổ sung size_label & portion_multiplier)
CREATE TABLE IF NOT EXISTS order_items (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id           INTEGER NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    menu_item_id       INTEGER NOT NULL REFERENCES menu_items(id),
    size_label         TEXT NOT NULL DEFAULT 'M',
    quantity           INTEGER NOT NULL DEFAULT 1,
    unit_price         INTEGER NOT NULL,
    portion_multiplier REAL DEFAULT 1.0,
    note               TEXT
);

-- 9. WASTE LOGS (Ghi nhận thất thoát / hỏng nguyên liệu)
CREATE TABLE IF NOT EXISTS waste_logs (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    ingredient_id INTEGER NOT NULL REFERENCES ingredients(id),
    quantity      REAL NOT NULL,
    reason        TEXT NOT NULL,
    date          DATE NOT NULL,
    recorded_by   TEXT
);

-- 10. DAILY SALES (Lịch sử bán hàng tổng hợp theo ngày và size)
CREATE TABLE IF NOT EXISTS daily_sales (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    date          DATE NOT NULL,
    menu_item_id  INTEGER NOT NULL REFERENCES menu_items(id),
    size_label    TEXT NOT NULL,
    quantity_sold INTEGER NOT NULL DEFAULT 0,
    revenue       INTEGER NOT NULL DEFAULT 0,
    UNIQUE(date, menu_item_id, size_label)
);

-- 11. WEATHER DATA
CREATE TABLE IF NOT EXISTS weather_data (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    date             DATE NOT NULL UNIQUE,
    temperature      REAL,
    rain_probability REAL,
    weather_condition TEXT
);

-- 12. CALENDAR EVENTS (Ngày lễ, sự kiện trường, thi cử)
CREATE TABLE IF NOT EXISTS calendar_events (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    date          DATE NOT NULL UNIQUE,
    event_type    TEXT NOT NULL,
    description   TEXT,
    demand_factor REAL DEFAULT 1.0
);

-- 13. FORECASTS (Kết quả dự báo nguyên liệu)
CREATE TABLE IF NOT EXISTS forecasts (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    date           DATE NOT NULL,
    ingredient_id  INTEGER NOT NULL REFERENCES ingredients(id),
    predicted_need REAL NOT NULL,
    actual_used    REAL,
    strategy_used  TEXT NOT NULL,
    created_at     DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 14. LEFTOVERS (Giảm lãng phí: Flash Sale cuối ngày / quyên góp)
CREATE TABLE IF NOT EXISTS leftovers (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    menu_item_id       INTEGER NOT NULL REFERENCES menu_items(id),
    size_label         TEXT NOT NULL DEFAULT 'M',
    initial_quantity   INTEGER NOT NULL,
    remaining_quantity INTEGER NOT NULL,
    discount_percent   INTEGER NOT NULL DEFAULT 30,
    status             TEXT NOT NULL DEFAULT 'ACTIVE',
    date               DATE NOT NULL
);

-- 15. WASTE SAVINGS (Báo cáo tiết kiệm và giảm phát thải)
CREATE TABLE IF NOT EXISTS waste_savings (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    date                  DATE NOT NULL UNIQUE,
    total_leftovers_sold  INTEGER DEFAULT 0,
    food_waste_reduced_kg REAL DEFAULT 0,
    cost_saved_vnd        INTEGER DEFAULT 0
);

-- INDEXES FOR PERFORMANCE
CREATE INDEX IF NOT EXISTS idx_orders_customer ON orders(customer_email);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_date ON orders(created_at);
CREATE INDEX IF NOT EXISTS idx_order_items_order ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_daily_sales_date ON daily_sales(date);
CREATE INDEX IF NOT EXISTS idx_inventory_expiry ON inventory_batches(expiry_date);
CREATE INDEX IF NOT EXISTS idx_leftovers_status ON leftovers(status, date);
