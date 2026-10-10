# 📋 IU Canteen Digitization – Yêu cầu bổ sung

> Tài liệu phân tích toàn bộ những gì cần bổ sung để biến prototype JavaFX hiện tại thành ứng dụng hoàn chỉnh:
> **Dự báo nguyên liệu thông minh** + **Đặt món có chọn size** + **Giảm lãng phí thức ăn thừa**.

---

## 📌 Mục lục

1. [Tổng quan hiện trạng](#1-tổng-quan-hiện-trạng)
2. [Kiến trúc hệ thống cần xây dựng](#2-kiến-trúc-hệ-thống-cần-xây-dựng)
3. [Cơ sở dữ liệu (Database)](#3-cơ-sở-dữ-liệu-database)
4. [Module 1 – Xác thực & Phân quyền](#4-module-1--xác-thực--phân-quyền)
5. [Module 2 – Đặt món có chọn Size & Số lượng](#5-module-2--đặt-món-có-chọn-size--số-lượng)
6. [Module 3 – Quản lý đơn hàng (Order Lifecycle)](#6-module-3--quản-lý-đơn-hàng-order-lifecycle)
7. [Module 4 – Quản lý Kho & Nguyên liệu](#7-module-4--quản-lý-kho--nguyên-liệu)
8. [Module 5 – Dự báo nguyên liệu thông minh (AI Forecast)](#8-module-5--dự-báo-nguyên-liệu-thông-minh-ai-forecast)
9. [Module 6 – Giảm lãng phí thức ăn thừa](#9-module-6--giảm-lãng-phí-thức-ăn-thừa)
10. [Module 7 – Thống kê & Báo cáo](#10-module-7--thống-kê--báo-cáo)
11. [Nguồn dữ liệu ngoài (External Data)](#11-nguồn-dữ-liệu-ngoài-external-data)
12. [Giao diện (UI) cần bổ sung / sửa đổi](#12-giao-diện-ui-cần-bổ-sung--sửa-đổi)
13. [Cấu trúc package đề xuất](#13-cấu-trúc-package-đề-xuất)
14. [Dependencies cần thêm vào pom.xml](#14-dependencies-cần-thêm-vào-pomxml)
15. [Roadmap triển khai](#15-roadmap-triển-khai)

---

## 1. Tổng quan hiện trạng

### ✅ Đã có (Frontend JavaFX – mock data)

| Thành phần | File | Trạng thái |
|---|---|---|
| Điểm khởi chạy & điều hướng | `App.java` | ✅ Hoạt động |
| Màn hình Đăng nhập / Đăng ký | `LoginView.java` | ⚠ Mock – phân quyền bằng prefix email |
| Giao diện Sinh viên: Thực đơn, Giỏ hàng, Đơn của tôi | `StudentView.java`, `StudentMenuView.java` | ⚠ Mock – chưa lưu DB |
| Giao diện Quản lý: Bảng bếp, Menu, Công thức, Thống kê, Kho, Dự đoán | `VendorView.java`, `VendorInventoryView.java` | ⚠ Mock – TODO khắp nơi |
| Logic dự đoán cơ bản (multiplier strategy) | `ForecastService.java`, `ForecastMethod.java`, `ForecastStrategy.java` | ⚠ Dùng `avgDaily` cứng |
| Kho dữ liệu tạm (mock) | `Store.java` | ❌ Cần thay bằng DB + Service |
| Giao diện CSS | `style.css` | ✅ Đầy đủ |

### ❌ Chưa có (cần bổ sung)

- Cơ sở dữ liệu (DB)
- Tầng Data Access (DAO / Repository)
- Tầng Service (business logic thực tế)
- Tính năng chọn Size khi đặt món
- Dự báo nguyên liệu dựa trên nhiều yếu tố (thời tiết, ngày lễ, trend, …)
- Hệ thống quản lý thức ăn thừa & giảm lãng phí
- Kết nối API thời tiết & dữ liệu ngày lễ
- Unit Test & Integration Test

---

## 2. Kiến trúc hệ thống cần xây dựng

```
┌──────────────────────────────────────────────────────┐
│                    JavaFX UI Layer                    │
│  LoginView · StudentView · VendorView · ...          │
├──────────────────────────────────────────────────────┤
│                   Service Layer                       │
│  AuthService · OrderService · MenuService             │
│  InventoryService · ForecastService · WasteService    │
│  WeatherService · HolidayService                      │
├──────────────────────────────────────────────────────┤
│                     DAO Layer                         │
│  UserDao · OrderDao · MenuDao · IngredientDao         │
│  RecipeDao · BatchDao · WasteDao · ForecastDao        │
│  SalesHistoryDao · LeftoverDao                        │
├──────────────────────────────────────────────────────┤
│                    Database                            │
│              SQLite  (hoặc MySQL)                     │
├──────────────────────────────────────────────────────┤
│                  External APIs                        │
│     OpenWeatherMap · Lịch ngày lễ VN                  │
└──────────────────────────────────────────────────────┘
```

---

## 3. Cơ sở dữ liệu (Database)

### 3.1. Bảng hiện tại cần tạo (dựa trên mock model trong `Store.java`)

```sql
-- ========== USERS ==========
CREATE TABLE users (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    full_name   TEXT NOT NULL,
    email       TEXT NOT NULL UNIQUE,        -- phải kết thúc @iu.edu.vn
    student_id  TEXT,                        -- mã sinh viên / nhân viên
    password    TEXT NOT NULL,               -- hash bcrypt
    role        TEXT NOT NULL DEFAULT 'STUDENT',  -- STUDENT | VENDOR | ADMIN
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ========== MENU ==========
CREATE TABLE menu_items (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT NOT NULL,
    category    TEXT NOT NULL,               -- Cơm | Món nước | Ăn nhẹ | Đồ uống
    available   BOOLEAN DEFAULT 1,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ========== SIZE & GIÁ (MỚI) ==========
CREATE TABLE menu_item_sizes (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    menu_item_id  INTEGER NOT NULL REFERENCES menu_items(id),
    size_label    TEXT NOT NULL,             -- 'S' | 'M' | 'L'
    price         INTEGER NOT NULL,          -- giá theo size (VNĐ)
    portion_multiplier REAL DEFAULT 1.0,    -- hệ số nhân nguyên liệu so với size M
    daily_limit   INTEGER DEFAULT 0,        -- 0 = không giới hạn
    UNIQUE(menu_item_id, size_label)
);

-- ========== NGUYÊN LIỆU ==========
CREATE TABLE ingredients (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    name            TEXT NOT NULL,
    unit            TEXT NOT NULL,           -- kg | quả | ổ | lít
    shelf_life_days INTEGER DEFAULT 1,
    min_stock       REAL DEFAULT 0,
    current_stock   REAL DEFAULT 0
);

-- ========== CÔNG THỨC ==========
CREATE TABLE recipes (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    menu_item_id    INTEGER NOT NULL REFERENCES menu_items(id),
    ingredient_id   INTEGER NOT NULL REFERENCES ingredients(id),
    qty_per_portion REAL NOT NULL,           -- định lượng cho 1 phần size M
    UNIQUE(menu_item_id, ingredient_id)
);

-- ========== LÔ HÀNG ==========
CREATE TABLE inventory_batches (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    ingredient_id   INTEGER NOT NULL REFERENCES ingredients(id),
    quantity        REAL NOT NULL,
    remaining_qty   REAL NOT NULL,           -- còn lại (trừ dần khi dùng)
    cost            REAL DEFAULT 0,
    received_date   DATE NOT NULL,
    expiry_date     DATE NOT NULL,
    status          TEXT DEFAULT 'ACTIVE'    -- ACTIVE | USED_UP | EXPIRED | WASTED
);

-- ========== MUA HÀNG ==========
CREATE TABLE purchases (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    supplier        TEXT,
    total_cost      REAL NOT NULL,
    purchase_date   DATE NOT NULL,
    notes           TEXT
);

CREATE TABLE purchase_items (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    purchase_id     INTEGER NOT NULL REFERENCES purchases(id),
    batch_id        INTEGER NOT NULL REFERENCES inventory_batches(id)
);

-- ========== ĐƠN HÀNG ==========
CREATE TABLE orders (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_id     INTEGER NOT NULL REFERENCES users(id),
    status          TEXT NOT NULL DEFAULT 'PENDING',
    pickup_time     TEXT,
    note            TEXT,
    total           INTEGER NOT NULL,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_items (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id        INTEGER NOT NULL REFERENCES orders(id),
    menu_item_id    INTEGER NOT NULL REFERENCES menu_items(id),
    size_label      TEXT NOT NULL DEFAULT 'M',   -- S | M | L
    quantity        INTEGER NOT NULL DEFAULT 1,
    unit_price      INTEGER NOT NULL,
    note            TEXT
);

-- ========== HAO HỤT / LÃNG PHÍ ==========
CREATE TABLE waste_logs (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    ingredient_id   INTEGER REFERENCES ingredients(id),
    menu_item_id    INTEGER REFERENCES menu_items(id),   -- NULL nếu hao hụt nguyên liệu thô
    quantity        REAL NOT NULL,
    reason          TEXT NOT NULL,   -- Hết hạn | Hư hỏng | Chuẩn bị dư | Khách không lấy | Khác
    cost_estimate   REAL DEFAULT 0,
    logged_date     DATE NOT NULL,
    logged_by       INTEGER REFERENCES users(id)
);
```

### 3.2. Bảng MỚI cho dự báo & giảm lãng phí

```sql
-- ========== LỊCH SỬ BÁN HÀNG (aggregated daily) ==========
CREATE TABLE daily_sales (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    sale_date       DATE NOT NULL,
    menu_item_id    INTEGER NOT NULL REFERENCES menu_items(id),
    size_label      TEXT NOT NULL DEFAULT 'M',
    quantity_sold   INTEGER NOT NULL,
    revenue         INTEGER NOT NULL,
    UNIQUE(sale_date, menu_item_id, size_label)
);

-- ========== DỮ LIỆU THỜI TIẾT ==========
CREATE TABLE weather_data (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    date            DATE NOT NULL UNIQUE,
    temperature     REAL,                   -- °C
    humidity        REAL,                   -- %
    condition       TEXT,                   -- Sunny | Rainy | Cloudy | Storm
    rain_mm         REAL DEFAULT 0,
    feels_like      REAL,
    wind_speed      REAL,
    source          TEXT DEFAULT 'OpenWeatherMap'
);

-- ========== NGÀY LỄ / SỰ KIỆN ==========
CREATE TABLE calendar_events (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    event_date      DATE NOT NULL,
    event_type      TEXT NOT NULL,           -- HOLIDAY | EXAM | UNIVERSITY_EVENT | SEMESTER_BREAK
    event_name      TEXT NOT NULL,
    demand_factor   REAL DEFAULT 1.0,        -- <1 = ít khách hơn, >1 = đông hơn
    notes           TEXT
);

-- ========== KẾT QUẢ DỰ BÁO ==========
CREATE TABLE forecasts (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    forecast_date   DATE NOT NULL,           -- ngày được dự báo
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    method          TEXT NOT NULL,            -- MOVING_AVG | WEEKDAY_AVG | WEIGHTED | ML_MODEL
    ingredient_id   INTEGER NOT NULL REFERENCES ingredients(id),
    predicted_need  REAL NOT NULL,
    actual_used     REAL,                    -- điền sau khi ngày đó kết thúc
    accuracy_pct    REAL,                    -- = 1 - |predicted - actual| / actual
    UNIQUE(forecast_date, method, ingredient_id)
);

-- ========== THỨC ĂN THỪA (LEFTOVER) ==========
CREATE TABLE leftovers (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    menu_item_id    INTEGER NOT NULL REFERENCES menu_items(id),
    size_label      TEXT NOT NULL DEFAULT 'M',
    quantity        INTEGER NOT NULL,        -- số phần thừa
    original_price  INTEGER NOT NULL,        -- giá gốc
    discount_price  INTEGER NOT NULL,        -- giá giảm (flash sale)
    discount_pct    REAL NOT NULL,            -- % giảm
    available_from  DATETIME NOT NULL,        -- bắt đầu bán giảm
    available_until DATETIME NOT NULL,        -- hết hạn
    status          TEXT DEFAULT 'AVAILABLE', -- AVAILABLE | SOLD | EXPIRED | DONATED
    sold_quantity   INTEGER DEFAULT 0,
    created_by      INTEGER REFERENCES users(id),
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ========== LỊCH SỬ TIẾT KIỆM LÃNG PHÍ ==========
CREATE TABLE waste_savings (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    date            DATE NOT NULL,
    total_leftover_portions INTEGER,         -- tổng phần thừa trong ngày
    sold_via_discount       INTEGER,         -- bán giảm giá
    donated                 INTEGER,         -- quyên góp
    wasted                  INTEGER,         -- bỏ đi
    money_saved             REAL,            -- tiền thu lại từ bán giảm giá
    food_saved_kg           REAL,            -- ước tính kg thức ăn được cứu
    UNIQUE(date)
);
```

---

## 4. Module 1 – Xác thực & Phân quyền

### Cần bổ sung

| Hạng mục | Chi tiết | File liên quan |
|---|---|---|
| **AuthService** | `login(email, password)` → xác thực từ DB, trả `User` + `Role` | `LoginView.java:74` |
| **Mã hóa mật khẩu** | Dùng **BCrypt** (thư viện `jBCrypt`) để hash mật khẩu | `LoginView.java:123` |
| **AuthService.register()** | Validate → hash password → insert vào bảng `users` | `LoginView.java:123` |
| **Session Management** | Thay `Store.currentUser` (String) bằng đối tượng `User` có `id`, `role`, `email` | `Store.java:151-152` |
| **UserDao** | `findByEmail()`, `insert()`, `updatePassword()` | Mới |

---

## 5. Module 2 – Đặt món có chọn Size & Số lượng

### Hiện trạng
- `Dish` trong `Store.java` chỉ có **1 giá cố định**, không có khái niệm size.
- `CartLine` chỉ lưu `dish` + `qty` + `note`, không có size.
- `Order.Line` lưu `name`, `qty`, `unitPrice`, `note` – không có size.

### Cần bổ sung

#### 5.1. Model mới

```java
// Thêm vào Dish hoặc tạo class riêng
public class DishSize {
    int id;
    int menuItemId;
    String sizeLabel;      // "S", "M", "L"
    int price;             // giá riêng cho từng size
    double portionMultiplier; // S=0.7, M=1.0, L=1.3
    int dailyLimit;        // giới hạn phần/ngày, 0 = không giới hạn
}
```

#### 5.2. Cập nhật CartLine

```java
public static class CartLine {
    public final Dish dish;
    public final DishSize size;              // MỚI
    public final IntegerProperty qty = new SimpleIntegerProperty(1);
    public final StringProperty note = new SimpleStringProperty("");
    
    public CartLine(Dish dish, DishSize size) {
        this.dish = dish;
        this.size = size;
    }
    
    public int subtotal() {
        return size.price * qty.get();   // dùng giá theo size
    }
}
```

#### 5.3. Cập nhật Order.Line

```java
public record Line(String name, String sizeLabel, int qty, int unitPrice, String note) {}
```

#### 5.4. Cập nhật UI – `StudentMenuView.java`

- Thêm **ComboBox chọn Size** (S / M / L) vào mỗi `dishCard`.
- Hiển thị **giá thay đổi** khi chọn size khác nhau.
- Ví dụ: `Phở bò – Size S: 35.000đ | M: 45.000đ | L: 55.000đ`.

#### 5.5. Cập nhật UI – `StudentView.java` (Giỏ hàng)

- Cột bảng giỏ hàng thêm cột **"Size"**.
- Tổng tiền tính theo `size.price × qty`.

#### 5.6. Cập nhật công thức (Recipe)

- Khi tính nguyên liệu cần dùng: `qty_per_portion × size.portionMultiplier × số_phần`.
- Ví dụ: Phở bò size L cần `0.10 kg thịt bò × 1.3 = 0.13 kg`.

---

## 6. Module 3 – Quản lý đơn hàng (Order Lifecycle)

### Các TODO cần xử lý

| # | TODO hiện tại | Cần làm |
|---|---|---|
| 1 | `StudentView.java:219` – `OrderService.create(...)` | Lưu `orders` + `order_items` vào DB, trừ `remaining` của `menu_item_sizes`, trừ tồn kho nguyên liệu theo công thức × portion_multiplier |
| 2 | `StudentView.java:72` – `OrderService.refreshMyOrders()` | Query DB lấy đơn hàng của user hiện tại, cập nhật vào `ObservableList` |
| 3 | `StudentView.java:333` – `OrderService.cancel(o.id)` | Đổi status → CANCELLED, hoàn lại `remaining` và tồn kho |
| 4 | `VendorView.java:145` – `OrderService.updateStatus(o.id, next)` | Update DB, push thông báo realtime (hoặc polling) |

### Service mới: `OrderService`

```java
public class OrderService {
    Order create(int userId, String pickupTime, String note, List<CartItem> items);
    void updateStatus(int orderId, Status newStatus);
    void cancel(int orderId);
    List<Order> getByUser(int userId, String filter);
    List<Order> getByStatus(Status... statuses);  // cho bảng bếp
    List<Order> getByDateRange(LocalDate from, LocalDate to);
}
```

---

## 7. Module 4 – Quản lý Kho & Nguyên liệu

### Các TODO cần xử lý

| # | TODO hiện tại | Cần làm |
|---|---|---|
| 1 | `VendorInventoryView.java:144` – `InventoryDao.insertBatch(...)` | Lưu lô hàng mới, cộng `current_stock` |
| 2 | `VendorInventoryView.java:144` – `PurchaseDao.insert(...)` | Lưu phiếu mua hàng |
| 3 | `VendorInventoryView.java:205` – `WasteDao.insert(...)` | Lưu ghi nhận hao hụt, trừ `current_stock` |
| 4 | `VendorView.java:218` – `MenuDao.insert(...)` | Lưu món mới + các size vào DB |
| 5 | `VendorView.java:308` – `RecipeDao.upsert(...)` | Thêm/cập nhật công thức nguyên liệu |

### Service mới: `InventoryService`

```java
public class InventoryService {
    void receiveBatch(int ingredientId, double qty, double cost, LocalDate received, LocalDate expiry);
    void logWaste(int ingredientId, double qty, String reason, LocalDate date, int userId);
    void deductIngredients(List<OrderItem> items);  // trừ kho khi có đơn mới
    void restoreIngredients(List<OrderItem> items);  // hoàn kho khi hủy đơn
    List<Ingredient> getLowStockAlerts();             // tồn < min_stock
    List<Batch> getExpiringBatches(int withinDays);  // sắp hết hạn
    
    // Trừ kho theo FIFO (lô nhập trước dùng trước)
    void deductFIFO(int ingredientId, double amount);
}
```

---

## 8. Module 5 – Dự báo nguyên liệu thông minh (AI Forecast)

### Hiện trạng

- `ForecastService.java` chỉ dùng `dish.avgDaily` (số cứng) × `demandMultiplier` (cũng cứng: 1.0 hoặc 1.1).
- `ForecastStrategy` interface chỉ có 1 method: `demandMultiplier(int days)` – quá đơn giản.
- Không có dữ liệu bán hàng thực tế, không xét thời tiết, ngày lễ, hay xu hướng.

### Cần bổ sung

#### 8.1. Nguồn dữ liệu đầu vào cho dự báo

| Nguồn dữ liệu | Mô tả | Cách lấy |
|---|---|---|
| **Lịch sử bán hàng** | Số phần bán mỗi ngày, theo món, theo size | Aggregate từ `order_items` → `daily_sales` |
| **Thời tiết** | Nhiệt độ, độ ẩm, mưa/nắng, dự báo | OpenWeatherMap API (miễn phí) |
| **Ngày lễ / sự kiện** | Lễ, thi, nghỉ hè, sự kiện trường | Bảng `calendar_events` (nhập tay + API) |
| **Thứ trong tuần** | Thứ 2-6 đông hơn T7-CN (trường học) | Tính từ `daily_sales.sale_date` |
| **Mùa / tháng** | Mùa nóng → đồ uống ↑, mùa lạnh → phở ↑ | Tính từ tháng trong năm |
| **Xu hướng (trend)** | Món mới đang lên, món cũ giảm dần | Linear regression trên `daily_sales` |
| **Lãng phí quá khứ** | Nếu thường xuyên thừa thì giảm forecast | Từ bảng `waste_logs` + `leftovers` |

#### 8.2. Các phương pháp dự báo mới

```java
public enum ForecastMethod implements ForecastStrategy {
    
    // Có sẵn (cần cải tiến)
    MOVING_AVERAGE,       // Trung bình động N ngày gần nhất
    WEEKDAY_AVERAGE,      // Trung bình theo từng thứ trong tuần
    
    // CẦN THÊM MỚI
    WEIGHTED_MOVING_AVG,  // Trung bình động có trọng số (ngày gần → trọng số cao hơn)
    EXPONENTIAL_SMOOTHING,// Exponential Smoothing (Holt-Winters)
    REGRESSION_MULTI,     // Hồi quy đa biến (bán hàng ~ thời tiết + thứ + mùa + lễ)
    ENSEMBLE,             // Kết hợp nhiều phương pháp, lấy trung bình có trọng số
}
```

#### 8.3. Cải tiến `ForecastService`

```java
public class ForecastService {
    
    /**
     * Dự báo nhu cầu nguyên liệu cho N ngày tới.
     * 
     * @param days          số ngày cần dự báo
     * @param method        phương pháp dự báo
     * @param weatherData   dự báo thời tiết N ngày tới (từ API)
     * @param events        danh sách sự kiện/ngày lễ trong N ngày tới
     * @return              danh sách nguyên liệu cần mua
     */
    List<ForecastRow> forecast(
        int days,
        ForecastMethod method,
        List<WeatherData> weatherData,
        List<CalendarEvent> events
    );
    
    /**
     * Dự báo theo từng ngày (chi tiết hơn).
     * Ví dụ: 5 ngày tới → trả 5 bản ghi riêng cho mỗi nguyên liệu.
     */
    Map<LocalDate, List<ForecastRow>> forecastDaily(int days, ForecastMethod method);
    
    /**
     * So sánh dự báo vs thực tế để đánh giá độ chính xác.
     */
    List<ForecastAccuracy> evaluateAccuracy(LocalDate from, LocalDate to);
    
    /**
     * Tự động chọn phương pháp dự báo tốt nhất dựa trên backtesting.
     */
    ForecastMethod bestMethod(int lookbackDays);
}
```

#### 8.4. Công thức dự báo (ví dụ Weighted Moving Average)

```
Dự báo số phần bán ngày mai cho món X:

  predicted_qty_X = Σ(weight_i × sales_i) 
                    × weather_factor 
                    × event_factor
                    × (1 - waste_adjustment)

Trong đó:
  - weight_i    = trọng số ngày thứ i (ngày gần hơn → trọng số lớn hơn)
  - sales_i     = số phần bán thực tế ngày i
  - weather_factor:
      + Mưa to      → 0.7  (ít người đến canteen)
      + Nắng nóng   → 1.2  (đồ uống tăng)
      + Bình thường  → 1.0
  - event_factor:
      + Ngày lễ / nghỉ     → 0.3
      + Tuần thi            → 0.8
      + Sự kiện trường      → 1.3
      + Ngày thường         → 1.0
  - waste_adjustment = tỷ lệ thừa trung bình 7 ngày gần nhất (0~0.3)

Nhu cầu nguyên liệu cho ngày mai:

  Với mỗi nguyên liệu J:
    need_J = Σ (predicted_qty_X × recipe(X, J) × size_multiplier)
             cho tất cả món X dùng nguyên liệu J

  Gợi ý mua:
    buy_J = max(0, need_J + min_stock_J - current_stock_J)
```

#### 8.5. Backtesting & Tự cải thiện

- Cuối mỗi ngày, tự động so sánh `predicted_need` vs `actual_used` cho từng nguyên liệu.
- Tính `accuracy_pct` và lưu vào bảng `forecasts`.
- Nếu phương pháp nào có accuracy thấp liên tục → tự động chuyển sang phương pháp khác.
- Dashboard hiển thị biểu đồ "Độ chính xác dự báo theo thời gian".

---

## 9. Module 6 – Giảm lãng phí thức ăn thừa

### 9.1. Quy trình quản lý thức ăn thừa

```
  15:00  Vendor kiểm tra: còn bao nhiêu phần chưa bán?
    ↓
  15:30  Đăng "Flash Sale" – giảm giá 30~50% các món thừa
    ↓
  15:30~17:00  Sinh viên thấy tab "🔥 Flash Sale" → mua giá rẻ
    ↓
  17:00  Kết thúc ngày:
         - Phần đã bán giảm giá → ghi nhận doanh thu
         - Phần còn lại → ghi nhận "lãng phí" hoặc "quyên góp"
    ↓
  Hệ thống tự động cập nhật waste_savings → feed ngược cho forecast
```

### 9.2. Tính năng cần xây dựng

#### Phía Vendor (Quản lý quán)

| Tính năng | Mô tả |
|---|---|
| **Kiểm kê cuối ngày** | Form để vendor nhập số phần còn lại cho từng món + size |
| **Tạo Flash Sale** | Chọn món thừa → đặt % giảm giá → đặt thời gian kết thúc |
| **Đánh dấu quyên góp** | Các phần không bán được → đánh dấu "Quyên góp" thay vì "Bỏ đi" |
| **Báo cáo lãng phí** | Biểu đồ hàng ngày/tuần: bao nhiêu phần thừa, bán được bao nhiêu, bỏ bao nhiêu |
| **Cảnh báo tự động** | Nếu một món thường xuyên thừa > 20% → cảnh báo giảm số lượng chuẩn bị |

#### Phía Student (Sinh viên)

| Tính năng | Mô tả |
|---|---|
| **Tab "🔥 Flash Sale"** | Hiển thị các món đang giảm giá, có countdown timer |
| **Badge giá gốc / giá mới** | Gạch ngang giá cũ, hiện giá mới nổi bật |
| **Thông báo** | Push notification hoặc banner khi có flash sale mới |

### 9.3. Service mới: `LeftoverService`

```java
public class LeftoverService {
    // Vendor tạo flash sale
    Leftover createFlashSale(int menuItemId, String sizeLabel, int qty,
                             int discountPct, LocalDateTime until);
    
    // Sinh viên xem và mua
    List<Leftover> getAvailableLeftovers();
    void purchaseLeftover(int leftoverId, int qty, int userId);
    
    // Cuối ngày: xử lý phần chưa bán
    void endOfDayProcess(LocalDate date);  // auto: AVAILABLE → EXPIRED hoặc DONATED
    
    // Thống kê
    WasteSavingSummary getSavings(LocalDate from, LocalDate to);
    
    // Feed ngược cho forecast
    double getAverageLeftoverRate(int menuItemId, int days);
}
```

---

## 10. Module 7 – Thống kê & Báo cáo

### Hiện trạng
- `VendorView.java:337-411` – Tab "Thống kê" dùng hoàn toàn dữ liệu cứng (hardcoded arrays).
- `VendorView.java:364` – `// TODO: thay bằng truy vấn thật từ orders/order_items theo khoảng ngày`.

### Cần bổ sung

| Báo cáo | Nguồn dữ liệu | Biểu đồ |
|---|---|---|
| **Doanh thu theo ngày/tuần/tháng** | `orders` + `order_items` | LineChart |
| **Món bán chạy (theo size)** | `daily_sales` | BarChart |
| **Giờ cao điểm** | `orders.created_at` → group by hour | BarChart |
| **Tỷ lệ lãng phí** | `waste_savings` | PieChart: Bán / Quyên góp / Bỏ |
| **Độ chính xác dự báo** | `forecasts` (predicted vs actual) | LineChart theo tuần |
| **Chi phí nguyên liệu vs Doanh thu** | `purchases` vs `orders` | StackedBarChart |
| **Xu hướng bán hàng** | `daily_sales` 30/60/90 ngày | LineChart + Trendline |
| **Ảnh hưởng thời tiết** | `weather_data` × `daily_sales` | ScatterChart |

### Service mới: `ReportService`

```java
public class ReportService {
    SalesSummary getSalesSummary(LocalDate from, LocalDate to);
    List<TopSellingItem> getTopSelling(LocalDate from, LocalDate to, int limit);
    Map<Integer, Integer> getHourlyDistribution(LocalDate from, LocalDate to);
    WasteReport getWasteReport(LocalDate from, LocalDate to);
    List<ForecastAccuracy> getForecastAccuracyTrend(int lastNDays);
}
```

---

## 11. Nguồn dữ liệu ngoài (External Data)

### 11.1. API Thời tiết – OpenWeatherMap

| Hạng mục | Chi tiết |
|---|---|
| **API** | `https://api.openweathermap.org/data/2.5/forecast` |
| **Gói miễn phí** | 60 calls/phút, dự báo 5 ngày/3 giờ |
| **Dữ liệu cần lấy** | `temp`, `humidity`, `weather[0].main`, `rain.3h`, `wind.speed` |
| **Lịch chạy** | Cron job 2 lần/ngày: 6:00 sáng + 18:00 tối |
| **Vị trí** | Tọa độ IU: `lat=10.8779`, `lon=106.8018` |

### Service mới: `WeatherService`

```java
public class WeatherService {
    List<WeatherData> fetchForecast(int days);    // gọi API
    WeatherData getByDate(LocalDate date);         // đọc từ DB
    double getDemandFactor(WeatherData w);         // quy đổi thành hệ số nhu cầu
}
```

### 11.2. Ngày lễ Việt Nam & Lịch trường

| Loại | Ví dụ | Demand Factor |
|---|---|---|
| Nghỉ lễ | 2/9, 1/5, 30/4, Tết | **0.1 ~ 0.3** (rất ít người) |
| Tuần thi | Midterm, Final | **0.7 ~ 0.8** (ít ăn canteen) |
| Sự kiện trường | Orientation, Open Day | **1.2 ~ 1.5** (đông hơn) |
| Nghỉ hè | Tháng 7-8 | **0.2 ~ 0.4** |
| Ngày thường (T2-T6) | — | **1.0** |
| Cuối tuần (T7-CN) | — | **0.5 ~ 0.6** |

### Service mới: `HolidayService`

```java
public class HolidayService {
    List<CalendarEvent> getEvents(LocalDate from, LocalDate to);
    double getDemandFactor(LocalDate date);
    void importVNHolidays(int year);   // tự động nhập ngày lễ VN
}
```

---

## 12. Giao diện (UI) cần bổ sung / sửa đổi

### 12.1. Sửa đổi màn hình hiện có

| Màn hình | Sửa đổi |
|---|---|
| **StudentMenuView** | + ComboBox chọn Size (S/M/L) trong mỗi dish card, giá thay đổi theo size |
| **StudentView – Giỏ hàng** | + Cột "Size" trong bảng, tổng tiền tính theo size |
| **StudentView – Đơn của tôi** | + Hiển thị size trong order summary |
| **VendorView – Thống kê** | Thay dữ liệu mock bằng query DB thật |
| **VendorView – Dự đoán** | + Thêm input Weather, chọn ngày cụ thể, biểu đồ so sánh predicted vs actual |

### 12.2. Màn hình MỚI cần xây dựng

| Màn hình | Vai trò | Mô tả |
|---|---|---|
| **🔥 Flash Sale** (Student) | Student | Tab mới: hiển thị món giảm giá, countdown timer, giá gạch ngang |
| **📦 Kiểm kê cuối ngày** (Vendor) | Vendor | Form nhập số phần còn lại → tạo flash sale hoặc ghi nhận lãng phí |
| **♻️ Quản lý lãng phí** (Vendor) | Vendor | Dashboard: tỷ lệ thừa, tiền tiết kiệm, trend theo tuần, cảnh báo |
| **🌤 Cài đặt dữ liệu ngoài** (Admin) | Admin | Nhập API key thời tiết, quản lý ngày lễ, xem log sync |
| **📈 Độ chính xác dự báo** (Vendor) | Vendor | Biểu đồ predicted vs actual, so sánh các method, auto-recommend |
| **📊 Dashboard tổng quan** (Admin) | Admin | KPI: doanh thu, lãng phí, accuracy, cảnh báo kho |

---

## 13. Cấu trúc package đề xuất

```
src/main/java/iu/canteen/
├── App.java                          # Entry point (có sẵn)
│
├── model/                            # POJO / Entity classes
│   ├── User.java
│   ├── MenuItem.java
│   ├── MenuItemSize.java             # MỚI: S/M/L
│   ├── Ingredient.java
│   ├── Recipe.java
│   ├── InventoryBatch.java
│   ├── Order.java
│   ├── OrderItem.java                # có sizeLabel
│   ├── WasteLog.java
│   ├── WeatherData.java              # MỚI
│   ├── CalendarEvent.java            # MỚI
│   ├── Forecast.java                 # MỚI
│   ├── Leftover.java                 # MỚI
│   ├── DailySales.java               # MỚI
│   └── WasteSaving.java              # MỚI
│
├── dao/                              # Data Access Objects
│   ├── Database.java                 # Connection pool / SQLite setup
│   ├── UserDao.java
│   ├── MenuDao.java
│   ├── IngredientDao.java
│   ├── RecipeDao.java
│   ├── BatchDao.java
│   ├── OrderDao.java
│   ├── WasteDao.java
│   ├── WeatherDao.java              # MỚI
│   ├── CalendarEventDao.java        # MỚI
│   ├── ForecastDao.java             # MỚI
│   ├── LeftoverDao.java             # MỚI
│   ├── DailySalesDao.java           # MỚI
│   └── WasteSavingDao.java          # MỚI
│
├── service/                          # Business logic
│   ├── AuthService.java
│   ├── OrderService.java
│   ├── MenuService.java
│   ├── InventoryService.java
│   ├── ForecastService.java          # cải tiến từ logic/
│   ├── WeatherService.java           # MỚI: gọi OpenWeatherMap
│   ├── HolidayService.java           # MỚI
│   ├── LeftoverService.java          # MỚI: flash sale & waste
│   ├── ReportService.java            # MỚI
│   └── SchedulerService.java         # MỚI: cron jobs
│
├── logic/                            # Forecast strategies (có sẵn, cải tiến)
│   ├── ForecastStrategy.java
│   ├── ForecastMethod.java
│   ├── MovingAverageStrategy.java    # MỚI: tách ra class riêng
│   ├── WeekdayAverageStrategy.java   # MỚI
│   ├── WeightedMovingAvgStrategy.java# MỚI
│   ├── ExponentialSmoothingStrategy.java # MỚI
│   ├── RegressionStrategy.java       # MỚI
│   └── EnsembleStrategy.java         # MỚI
│
└── ui/                               # JavaFX views (có sẵn + mở rộng)
    ├── Screen.java
    ├── AbstractAuthenticatedView.java
    ├── Ui.java
    ├── LoginView.java
    ├── StudentView.java
    ├── StudentMenuView.java
    ├── StudentFlashSaleView.java     # MỚI
    ├── VendorView.java
    ├── VendorInventoryView.java
    ├── VendorEndOfDayView.java       # MỚI
    ├── VendorWasteDashboard.java     # MỚI
    ├── VendorForecastView.java       # MỚI: tách ra + nâng cấp
    ├── AdminDashboardView.java       # MỚI
    └── AdminSettingsView.java        # MỚI
```

---

## 14. Dependencies cần thêm vào `pom.xml`

```xml
<dependencies>
    <!-- Có sẵn -->
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-controls</artifactId>
        <version>${javafx.version}</version>
    </dependency>

    <!-- CẦN THÊM -->

    <!-- JavaFX Charts (đã dùng trong VendorView nhưng chưa khai báo rõ) -->
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-graphics</artifactId>
        <version>${javafx.version}</version>
    </dependency>

    <!-- SQLite JDBC -->
    <dependency>
        <groupId>org.xerial</groupId>
        <artifactId>sqlite-jdbc</artifactId>
        <version>3.45.3.0</version>
    </dependency>

    <!-- BCrypt cho hash mật khẩu -->
    <dependency>
        <groupId>org.mindrot</groupId>
        <artifactId>jbcrypt</artifactId>
        <version>0.4</version>
    </dependency>

    <!-- HTTP Client cho gọi API thời tiết (Java 11+ có sẵn java.net.http) -->
    <!-- Hoặc dùng OkHttp nếu muốn: -->
    <!--
    <dependency>
        <groupId>com.squareup.okhttp3</groupId>
        <artifactId>okhttp</artifactId>
        <version>4.12.0</version>
    </dependency>
    -->

    <!-- JSON parsing -->
    <dependency>
        <groupId>com.google.code.gson</groupId>
        <artifactId>gson</artifactId>
        <version>2.11.0</version>
    </dependency>

    <!-- Apache Commons Math (cho Exponential Smoothing, Regression) -->
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-math3</artifactId>
        <version>3.6.1</version>
    </dependency>

    <!-- Test -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.10.2</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## 15. Roadmap triển khai

### Phase 1 – Nền tảng (Tuần 1–2) ⚙️

- [x] Tạo schema DB (SQLite) + class `Database.java` (connection)
- [x] Viết tất cả DAO classes
- [x] Viết `AuthService` + sửa `LoginView` dùng DB thật
- [x] Migrate mock data từ `Store.java` sang DB (seed data)
- [x] Unit test cho DAO + AuthService

### Phase 2 – Đặt món + Size (Tuần 3) 🍽

- [x] Thêm bảng `menu_item_sizes` + model `MenuItemSize`
- [x] Sửa `StudentMenuView`: thêm ComboBox chọn size
- [x] Sửa `CartLine`, `Order.Line`: thêm `sizeLabel`
- [x] Sửa `StudentView` giỏ hàng: hiển thị cột size
- [x] Cập nhật `Recipe`: tính nguyên liệu theo `portionMultiplier`

### Phase 3 – Order + Inventory thật (Tuần 4) 📦

- [x] Viết `OrderService`: create, cancel, updateStatus
- [x] Viết `InventoryService`: receiveBatch, deductFIFO, logWaste
- [x] Sửa tất cả TODO trong `StudentView`, `VendorView`, `VendorInventoryView`
- [x] Polling / refresh đơn hàng từ DB

### Phase 4 – Dự báo thông minh (Tuần 5–6) 🔮

- [x] Viết `WeatherService` + tích hợp OpenWeatherMap API
- [x] Viết `HolidayService` + nhập ngày lễ VN 2024-2025
- [x] Cải tiến `ForecastService`: thêm các phương pháp mới
- [x] Viết aggregate job: `order_items` → `daily_sales`
- [x] Viết `ForecastDao`: lưu + so sánh predicted vs actual
- [x] UI: nâng cấp tab Dự đoán, thêm biểu đồ accuracy

### Phase 5 – Giảm lãng phí (Tuần 7) ♻️

- [x] Viết `LeftoverService`: tạo flash sale, xử lý cuối ngày
- [x] UI Student: tab "🔥 Flash Sale"
- [x] UI Vendor: "Kiểm kê cuối ngày" + "Dashboard lãng phí"
- [x] Feed waste data ngược vào forecast (waste_adjustment)

### Phase 6 – Thống kê & Polish (Tuần 8) 📊

- [x] Viết `ReportService` + sửa tab Thống kê dùng dữ liệu thật
- [x] Admin Dashboard
- [x] Scheduler: auto-aggregate daily_sales, auto-fetch weather
- [x] End-to-end testing
- [x] Tối ưu giao diện, thêm animation, UX polish

---

## 📎 Tóm tắt

| Hạng mục | Số lượng ước tính |
|---|---|
| Bảng DB mới | **15 bảng** |
| DAO classes mới | **12 class** |
| Service classes mới | **9 class** |
| Forecast strategies mới | **4 strategy** |
| Màn hình UI mới | **6 view** |
| Màn hình UI cần sửa | **5 view** |
| External API tích hợp | **1 (OpenWeatherMap)** |
| Dependencies mới | **5 thư viện** |
| Tổng thời gian ước tính | **~8 tuần** (1 dev full-time) |

> **Lưu ý:** Tài liệu này được xây dựng dựa trên phân tích toàn bộ source code hiện có.
> Mọi TODO comment trong code đều đã được bao phủ và mở rộng thêm các tính năng mới
> phục vụ mục tiêu **dự báo thông minh** và **giảm lãng phí thức ăn**.
