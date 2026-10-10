-- ========================================================
-- IU CANTEEN INITIAL SEED DATA
-- ========================================================

-- MENU ITEMS
INSERT INTO menu_items (id, name, category, available) VALUES
(1, 'Cơm gà xối mỡ', 'Cơm', 1),
(2, 'Cơm sườn nướng', 'Cơm', 1),
(3, 'Phở bò', 'Món nước', 1),
(4, 'Bún bò Huế', 'Món nước', 1),
(5, 'Bánh mì trứng', 'Ăn nhẹ', 1),
(6, 'Trà đào cam sả', 'Đồ uống', 1),
(7, 'Cà phê sữa', 'Đồ uống', 1),
(8, 'Nước cam ép', 'Đồ uống', 0);

-- MENU ITEM SIZES (S, M, L)
INSERT INTO menu_item_sizes (menu_item_id, size_label, price, portion_multiplier, daily_limit) VALUES
-- Cơm gà xối mỡ
(1, 'S', 30000, 0.8, 0),
(1, 'M', 35000, 1.0, 0),
(1, 'L', 42000, 1.3, 0),
-- Cơm sườn nướng
(2, 'S', 33000, 0.8, 0),
(2, 'M', 38000, 1.0, 0),
(2, 'L', 45000, 1.3, 0),
-- Phở bò
(3, 'S', 38000, 0.85, 0),
(3, 'M', 45000, 1.0, 0),
(3, 'L', 52000, 1.3, 0),
-- Bún bò Huế
(4, 'S', 35000, 0.85, 0),
(4, 'M', 42000, 1.0, 0),
(4, 'L', 50000, 1.3, 0),
-- Bánh mì trứng
(5, 'S', 15000, 0.7, 0),
(5, 'M', 20000, 1.0, 0),
(5, 'L', 26000, 1.4, 0),
-- Trà đào cam sả
(6, 'M', 25000, 1.0, 0),
(6, 'L', 30000, 1.3, 0),
-- Cà phê sữa
(7, 'M', 22000, 1.0, 0),
(7, 'L', 27000, 1.3, 0),
-- Nước cam ép
(8, 'M', 28000, 1.0, 0),
(8, 'L', 35000, 1.3, 0);

-- INGREDIENTS
INSERT INTO ingredients (id, name, unit, shelf_life_days, min_stock, current_stock) VALUES
(1, 'Gạo', 'kg', 180, 10.0, 25.0),
(2, 'Thịt gà', 'kg', 3, 5.0, 6.0),
(3, 'Sườn heo', 'kg', 3, 4.0, 3.0),
(4, 'Thịt bò', 'kg', 3, 4.0, 9.0),
(5, 'Bún/Phở tươi', 'kg', 2, 8.0, 12.0),
(6, 'Trứng', 'quả', 14, 60.0, 150.0),
(7, 'Bánh mì', 'ổ', 1, 30.0, 40.0),
(8, 'Cà phê bột', 'kg', 90, 2.0, 4.0),
(9, 'Sữa đặc', 'kg', 120, 3.0, 5.0),
(10, 'Trà', 'kg', 180, 1.0, 2.0);

-- RECIPES (Định lượng chuẩn cho size M)
INSERT INTO recipes (menu_item_id, ingredient_id, quantity) VALUES
(1, 1, 0.15), -- Cơm gà: 0.15kg gạo
(1, 2, 0.20), -- Cơm gà: 0.20kg gà
(2, 1, 0.15), -- Cơm sườn: 0.15kg gạo
(2, 3, 0.18), -- Cơm sườn: 0.18kg sườn
(3, 5, 0.20), -- Phở bò: 0.20kg bánh phở
(3, 4, 0.10), -- Phở bò: 0.10kg bò
(4, 5, 0.20), -- Bún bò: 0.20kg bún
(4, 4, 0.08), -- Bún bò: 0.08kg bò
(5, 7, 1.0),  -- Bánh mì trứng: 1 ổ bánh mì
(5, 6, 2.0),  -- Bánh mì trứng: 2 quả trứng
(6, 10, 0.01),-- Trà đào: 0.01kg trà
(7, 8, 0.02), -- Cà phê sữa: 0.02kg cà phê
(7, 9, 0.03); -- Cà phê sữa: 0.03kg sữa

-- INVENTORY BATCHES (FIFO)
INSERT INTO inventory_batches (ingredient_id, quantity, remaining_quantity, cost_per_unit, received_date, expiry_date, batch_code) VALUES
(2, 10.0, 6.0, 85000.0, date('now', '-1 day'), date('now', '+2 day'), 'BATCH-CK-01'),
(3, 6.0, 3.0, 120000.0, date('now', '-2 day'), date('now', '+1 day'), 'BATCH-PK-01'),
(1, 50.0, 25.0, 18000.0, date('now', '-10 day'), date('now', '+170 day'), 'BATCH-RC-01');

-- WASTE LOGS
INSERT INTO waste_logs (ingredient_id, quantity, reason, date, recorded_by) VALUES
(7, 5.0, 'Hết hạn', date('now', '-1 day'), 'vendor@iu.edu.vn');

-- CALENDAR EVENTS
INSERT INTO calendar_events (date, event_type, description, demand_factor) VALUES
(date('now'), 'Bình thường', 'Ngày học bình thường', 1.0),
(date('now', '+7 day'), 'Thi cử', 'Tuần thi giữa kỳ - Lượng khách tăng nhẹ', 1.15),
(date('now', '+14 day'), 'Nghỉ lễ', 'Ngày nghỉ - Canteen vắng khách', 0.25);

-- WEATHER DATA
INSERT INTO weather_data (date, temperature, rain_probability, weather_condition) VALUES
(date('now', '-3 day'), 30.5, 0.15, 'Nắng nhẹ, có mây'),
(date('now', '-2 day'), 29.0, 0.60, 'Mưa rào nhẹ'),
(date('now', '-1 day'), 33.0, 0.10, 'Nắng gắt'),
(date('now'), 31.0, 0.20, 'Nắng nhẹ, có mây'),
(date('now', '+1 day'), 32.5, 0.10, 'Nắng nhẹ'),
(date('now', '+2 day'), 28.5, 0.55, 'Mưa rào'),
(date('now', '+3 day'), 31.0, 0.30, 'Có mây');

-- DAILY SALES (Lịch sử bán hàng 5 ngày qua)
INSERT INTO daily_sales (date, menu_item_id, size_label, quantity_sold, revenue) VALUES
-- Cơm gà
(date('now', '-5 day'), 1, 'M', 28, 980000),
(date('now', '-4 day'), 1, 'M', 32, 1120000),
(date('now', '-3 day'), 1, 'M', 30, 1050000),
(date('now', '-2 day'), 1, 'M', 25, 875000),
(date('now', '-1 day'), 1, 'M', 35, 1225000),
-- Cơm sườn
(date('now', '-5 day'), 2, 'M', 20, 760000),
(date('now', '-4 day'), 2, 'M', 24, 912000),
(date('now', '-3 day'), 2, 'M', 22, 836000),
(date('now', '-2 day'), 2, 'M', 18, 684000),
(date('now', '-1 day'), 2, 'M', 26, 988000),
-- Phở bò
(date('now', '-5 day'), 3, 'M', 15, 675000),
(date('now', '-4 day'), 3, 'M', 18, 810000),
(date('now', '-3 day'), 3, 'M', 16, 720000),
(date('now', '-2 day'), 3, 'M', 14, 630000),
(date('now', '-1 day'), 3, 'M', 20, 900000);

-- FORECASTS (Lịch sử dự báo đã đối chiếu thực tế)
INSERT INTO forecasts (date, ingredient_id, predicted_need, actual_used, strategy_used) VALUES
(date('now', '-3 day'), 1, 15.0, 14.2, 'SMART_AI_ENSEMBLE'),
(date('now', '-3 day'), 2, 8.0, 7.8, 'SMART_AI_ENSEMBLE'),
(date('now', '-2 day'), 1, 14.5, 14.0, 'SMART_AI_ENSEMBLE'),
(date('now', '-2 day'), 2, 7.5, 7.0, 'SMART_AI_ENSEMBLE'),
(date('now', '-1 day'), 1, 16.0, 15.5, 'SMART_AI_ENSEMBLE'),
(date('now', '-1 day'), 2, 8.5, 8.2, 'SMART_AI_ENSEMBLE');

