package iu.canteen.logic;

import javafx.beans.Observable;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Kho dữ liệu tạm trong bộ nhớ (mock) để dựng giao diện.
 * Khi nối DB thật (SQLite/MySQL...), thay các danh sách + hàm ở đây bằng DAO/Service.
 */
public final class Store {
    private Store() {}

    // ===================== Enums =====================
    public enum Role { STUDENT, VENDOR, ADMIN }

    public enum Status {
        PENDING("Chờ nhận"), ACCEPTED("Đã nhận"), PREPARING("Đang chuẩn bị"),
        READY("Sẵn sàng"), COMPLETED("Hoàn thành"), CANCELLED("Đã hủy");

        public final String label;
        Status(String label) { this.label = label; }

        public Status next() {
            switch (this) {
                case PENDING:   return ACCEPTED;
                case ACCEPTED:  return PREPARING;
                case PREPARING: return READY;
                case READY:     return COMPLETED;
                default:        return null;
            }
        }
        @Override public String toString() { return label; }
    }

    // ===================== Models =====================
    /** menu_items */
    public static class Dish {
        public final int id;
        public final StringProperty name = new SimpleStringProperty();
        public final IntegerProperty price = new SimpleIntegerProperty();
        public final StringProperty category = new SimpleStringProperty();
        public final BooleanProperty available = new SimpleBooleanProperty(true);
        /** Số phần còn lại hôm nay (mock cho bước "kiểm tra số lượng còn lại") */
        public final IntegerProperty remaining = new SimpleIntegerProperty();
        /** Lượng bán trung bình/ngày (mock dữ liệu cho dự đoán) */
        public final int avgDaily;
        /** Các size khả dụng (S, M, L) */
        public final ObservableList<iu.canteen.model.MenuItemSize> sizes = FXCollections.observableArrayList();

        public Dish(int id, String name, int price, String category, int remaining, int avgDaily) {
            this.id = id;
            this.name.set(name);
            this.price.set(price);
            this.category.set(category);
            this.remaining.set(remaining);
            this.avgDaily = avgDaily;
        }

        public iu.canteen.model.MenuItemSize getSize(String label) {
            if (label != null) {
                for (iu.canteen.model.MenuItemSize s : sizes) {
                    if (s.getSizeLabel().equalsIgnoreCase(label)) return s;
                }
            }
            return sizes.isEmpty() ? null : sizes.get(0);
        }

        public int priceFor(String label) {
            iu.canteen.model.MenuItemSize s = getSize(label);
            return s != null ? s.getPrice() : price.get();
        }

        public double portionFor(String label) {
            iu.canteen.model.MenuItemSize s = getSize(label);
            return s != null ? s.getPortionMultiplier() : 1.0;
        }

        public boolean orderable() { return available.get() && remaining.get() > 0; }
        @Override public String toString() { return name.get(); }
    }

    /** ingredients */
    public static class Ingredient {
        public final int id;
        public final String name;
        public final String unit;
        public final int shelfLifeDays;
        public final double minStock;
        public final DoubleProperty stock = new SimpleDoubleProperty();

        public Ingredient(int id, String name, String unit, int shelfLifeDays, double minStock, double stock) {
            this.id = id; this.name = name; this.unit = unit;
            this.shelfLifeDays = shelfLifeDays; this.minStock = minStock;
            this.stock.set(stock);
        }
        @Override public String toString() { return name + " (" + unit + ")"; }
    }

    /** recipes */
    public static class RecipeLine {
        public final Ingredient ingredient;
        public final DoubleProperty qty = new SimpleDoubleProperty();
        public RecipeLine(Ingredient ingredient, double qty) { this.ingredient = ingredient; this.qty.set(qty); }
        public double adjustedQty(double portionMultiplier) { return qty.get() * portionMultiplier; }
    }

    /** inventory_batches */
    public static class Batch {
        public final Ingredient ingredient;
        public final double qty;
        public final double cost;
        public final LocalDate received;
        public final LocalDate expiry;
        public Batch(Ingredient i, double qty, double cost, LocalDate received, LocalDate expiry) {
            this.ingredient = i; this.qty = qty; this.cost = cost; this.received = received; this.expiry = expiry;
        }
    }

    /** waste_logs */
    public static class WasteLog {
        public final Ingredient ingredient;
        public final double qty;
        public final String reason;
        public final LocalDate date;
        public WasteLog(Ingredient i, double qty, String reason, LocalDate date) {
            this.ingredient = i; this.qty = qty; this.reason = reason; this.date = date;
        }
    }

    /** Một dòng trong giỏ hàng có chọn Size & Số lượng */
    public static class CartLine {
        public final Dish dish;
        public final StringProperty size = new SimpleStringProperty("M");
        public final IntegerProperty unitPrice = new SimpleIntegerProperty();
        public final DoubleProperty portionMultiplier = new SimpleDoubleProperty(1.0);
        public final IntegerProperty qty = new SimpleIntegerProperty(1);
        public final StringProperty note = new SimpleStringProperty("");

        public CartLine(Dish dish, iu.canteen.model.MenuItemSize sizeObj, int quantity) {
            this.dish = dish;
            this.size.set(sizeObj != null ? sizeObj.getSizeLabel() : "M");
            this.unitPrice.set(sizeObj != null ? sizeObj.getPrice() : dish.price.get());
            this.portionMultiplier.set(sizeObj != null ? sizeObj.getPortionMultiplier() : 1.0);
            this.qty.set(Math.max(1, quantity));
        }

        public CartLine(Dish dish) {
            this(dish, dish.getSize("M"), 1);
        }

        public int subtotal() { return unitPrice.get() * qty.get(); }
    }

    /** orders + order_items */
    public static class Order {
        public record Line(String name, String size, int qty, int unitPrice, double portionMultiplier, String note) {
            public Line(String name, int qty, int unitPrice, String note) {
                this(name, "M", qty, unitPrice, 1.0, note);
            }
        }

        public final int id;
        public final String customer;
        public final ObjectProperty<Status> status = new SimpleObjectProperty<>(Status.PENDING);
        public final LocalDateTime createdAt;
        public final String pickup;
        public final String note;
        public final int total;
        public final List<Line> lines;

        public Order(int id, String customer, LocalDateTime createdAt, String pickup, String note,
                     int total, List<Line> lines, Status status) {
            this.id = id; this.customer = customer; this.createdAt = createdAt;
            this.pickup = pickup; this.note = note; this.total = total; this.lines = lines;
            this.status.set(status);
        }
        public String summary() {
            StringJoiner j = new StringJoiner(", ");
            for (Line l : lines) {
                String sizeTag = (l.size() != null && !l.size().isEmpty()) ? " (" + l.size() + ")" : "";
                j.add(l.name() + sizeTag + " x" + l.qty());
            }
            return j.toString();
        }
    }

    public record ForecastRow(String name, String unit, double stock, double need, double buy, String level) {}

    // ===================== Session =====================
    public static String currentUser = "";
    public static Role currentRole = Role.STUDENT;

    // ===================== Data =====================
    public static final ObservableList<Dish> menu = FXCollections.observableArrayList(
            d -> new Observable[]{d.name, d.price, d.category, d.available, d.remaining});

    public static final ObservableList<CartLine> cart = FXCollections.observableArrayList(
            c -> new Observable[]{c.qty, c.note, c.size, c.unitPrice});

    public static final ObservableList<Order> orders = FXCollections.observableArrayList(
            o -> new Observable[]{o.status});

    public static final ObservableList<Ingredient> ingredients = FXCollections.observableArrayList(
            i -> new Observable[]{i.stock});

    public static final ObservableList<Batch> batches = FXCollections.observableArrayList();
    public static final ObservableList<WasteLog> wastes = FXCollections.observableArrayList();
    public static final Map<Integer, ObservableList<RecipeLine>> recipes = new HashMap<>();

    public static final List<String> CATEGORIES = List.of("Cơm", "Món nước", "Ăn nhẹ", "Đồ uống");
    private static int nextOrderId = 1;
    private static int nextDishId = 1;

    static {
        // ---- Menu & Sizes (S, M, L) ----
        Dish d1 = new Dish(nextDishId++, "Cơm gà xối mỡ", 35000, "Cơm", 40, 60);
        d1.sizes.addAll(
                new iu.canteen.model.MenuItemSize(1, 1, "S", 30000, 0.8, 0),
                new iu.canteen.model.MenuItemSize(2, 1, "M", 35000, 1.0, 0),
                new iu.canteen.model.MenuItemSize(3, 1, "L", 42000, 1.3, 0)
        );
        menu.add(d1);

        Dish d2 = new Dish(nextDishId++, "Cơm sườn nướng", 38000, "Cơm", 35, 50);
        d2.sizes.addAll(
                new iu.canteen.model.MenuItemSize(4, 2, "S", 33000, 0.8, 0),
                new iu.canteen.model.MenuItemSize(5, 2, "M", 38000, 1.0, 0),
                new iu.canteen.model.MenuItemSize(6, 2, "L", 45000, 1.3, 0)
        );
        menu.add(d2);

        Dish d3 = new Dish(nextDishId++, "Phở bò", 45000, "Món nước", 25, 40);
        d3.sizes.addAll(
                new iu.canteen.model.MenuItemSize(7, 3, "S", 38000, 0.85, 0),
                new iu.canteen.model.MenuItemSize(8, 3, "M", 45000, 1.0, 0),
                new iu.canteen.model.MenuItemSize(9, 3, "L", 52000, 1.3, 0)
        );
        menu.add(d3);

        Dish d4 = new Dish(nextDishId++, "Bún bò Huế", 42000, "Món nước", 20, 30);
        d4.sizes.addAll(
                new iu.canteen.model.MenuItemSize(10, 4, "S", 35000, 0.85, 0),
                new iu.canteen.model.MenuItemSize(11, 4, "M", 42000, 1.0, 0),
                new iu.canteen.model.MenuItemSize(12, 4, "L", 50000, 1.3, 0)
        );
        menu.add(d4);

        Dish d5 = new Dish(nextDishId++, "Bánh mì trứng", 20000, "Ăn nhẹ", 50, 45);
        d5.sizes.addAll(
                new iu.canteen.model.MenuItemSize(13, 5, "S", 15000, 0.7, 0),
                new iu.canteen.model.MenuItemSize(14, 5, "M", 20000, 1.0, 0),
                new iu.canteen.model.MenuItemSize(15, 5, "L", 26000, 1.4, 0)
        );
        menu.add(d5);

        Dish d6 = new Dish(nextDishId++, "Trà đào cam sả", 25000, "Đồ uống", 60, 70);
        d6.sizes.addAll(
                new iu.canteen.model.MenuItemSize(16, 6, "M", 25000, 1.0, 0),
                new iu.canteen.model.MenuItemSize(17, 6, "L", 30000, 1.3, 0)
        );
        menu.add(d6);

        Dish d7 = new Dish(nextDishId++, "Cà phê sữa", 22000, "Đồ uống", 80, 90);
        d7.sizes.addAll(
                new iu.canteen.model.MenuItemSize(18, 7, "M", 22000, 1.0, 0),
                new iu.canteen.model.MenuItemSize(19, 7, "L", 27000, 1.3, 0)
        );
        menu.add(d7);

        Dish out = new Dish(nextDishId++, "Nước cam ép", 28000, "Đồ uống", 0, 25);
        out.sizes.addAll(
                new iu.canteen.model.MenuItemSize(20, 8, "M", 28000, 1.0, 0),
                new iu.canteen.model.MenuItemSize(21, 8, "L", 35000, 1.3, 0)
        );
        menu.add(out);

        // ---- Nguyên liệu ----
        Ingredient rice  = new Ingredient(1, "Gạo", "kg", 180, 10, 25);
        Ingredient chick = new Ingredient(2, "Thịt gà", "kg", 3, 5, 6);
        Ingredient pork  = new Ingredient(3, "Sườn heo", "kg", 3, 4, 3);
        Ingredient beef  = new Ingredient(4, "Thịt bò", "kg", 3, 4, 9);
        Ingredient noodle= new Ingredient(5, "Bún/Phở tươi", "kg", 2, 8, 12);
        Ingredient egg   = new Ingredient(6, "Trứng", "quả", 14, 60, 150);
        Ingredient bread = new Ingredient(7, "Bánh mì", "ổ", 1, 30, 40);
        Ingredient coffee= new Ingredient(8, "Cà phê bột", "kg", 90, 2, 4);
        Ingredient milk  = new Ingredient(9, "Sữa đặc", "kg", 120, 3, 5);
        Ingredient tea   = new Ingredient(10, "Trà", "kg", 180, 1, 2);
        ingredients.addAll(rice, chick, pork, beef, noodle, egg, bread, coffee, milk, tea);

        // ---- Công thức (định lượng/phần chuẩn size M) ----
        setRecipe(1, new RecipeLine(rice, 0.15), new RecipeLine(chick, 0.20));
        setRecipe(2, new RecipeLine(rice, 0.15), new RecipeLine(pork, 0.18));
        setRecipe(3, new RecipeLine(noodle, 0.20), new RecipeLine(beef, 0.10));
        setRecipe(4, new RecipeLine(noodle, 0.20), new RecipeLine(beef, 0.08));
        setRecipe(5, new RecipeLine(bread, 1), new RecipeLine(egg, 2));
        setRecipe(6, new RecipeLine(tea, 0.01));
        setRecipe(7, new RecipeLine(coffee, 0.02), new RecipeLine(milk, 0.03));

        // ---- Lô hàng + hao hụt ----
        LocalDate today = LocalDate.now();
        batches.add(new Batch(chick, 10, 850000, today.minusDays(1), today.plusDays(2)));
        batches.add(new Batch(pork, 6, 720000, today.minusDays(2), today.plusDays(1)));
        batches.add(new Batch(rice, 50, 900000, today.minusDays(10), today.plusDays(170)));
        wastes.add(new WasteLog(bread, 5, "Hết hạn", today.minusDays(1)));

        // ---- Đơn mẫu ----
        LocalDateTime now = LocalDateTime.now();
        seedOrder("student@iu.edu.vn", now.minusMinutes(50), "11:30", "", Status.COMPLETED,
                new Order.Line("Cơm gà xối mỡ", "M", 1, 35000, 1.0, "Ít cơm"),
                new Order.Line("Trà đào cam sả", "M", 1, 25000, 1.0, ""));
        seedOrder("an.nguyen@iu.edu.vn", now.minusMinutes(12), "11:45", "", Status.PENDING,
                new Order.Line("Phở bò", "M", 2, 45000, 1.0, "Không hành"));
        seedOrder("binh.tran@iu.edu.vn", now.minusMinutes(9), "11:50", "Gọi giúp mình khi xong", Status.ACCEPTED,
                new Order.Line("Cơm sườn nướng", "M", 1, 38000, 1.0, ""),
                new Order.Line("Cà phê sữa", "M", 1, 22000, 1.0, "Ít đá"));
        seedOrder("student@iu.edu.vn", now.minusMinutes(6), "12:00", "", Status.PREPARING,
                new Order.Line("Bánh mì trứng", "M", 2, 20000, 1.0, "Thêm tương ớt"));
        seedOrder("chi.le@iu.edu.vn", now.minusMinutes(3), "12:10", "", Status.READY,
                new Order.Line("Bún bò Huế", "M", 1, 42000, 1.0, ""));
    }

    private static void setRecipe(int dishId, RecipeLine... lines) {
        recipes.put(dishId, FXCollections.observableArrayList(lines));
    }

    private static void seedOrder(String customer, LocalDateTime at, String pickup, String note,
                                  Status st, Order.Line... lines) {
        int total = 0;
        for (Order.Line l : lines) total += l.qty() * l.unitPrice();
        orders.add(0, new Order(nextOrderId++, customer, at, pickup, note, total, List.of(lines), st));
    }

    // ===================== Actions =====================
    public static ObservableList<RecipeLine> recipeFor(Dish d) {
        return recipes.computeIfAbsent(d.id, k -> FXCollections.observableArrayList());
    }

    public static Dish addDish(String name, int price, String category, int remaining) {
        Dish d = new Dish(nextDishId++, name, price, category, remaining, 20);
        d.sizes.add(new iu.canteen.model.MenuItemSize(0, d.id, "M", price, 1.0, 0));
        menu.add(d);
        return d;
    }

    public static int cartTotal() {
        int t = 0;
        for (CartLine c : cart) t += c.subtotal();
        return t;
    }

    public static int cartCount() {
        int t = 0;
        for (CartLine c : cart) t += c.qty.get();
        return t;
    }

    public static Order placeOrder(String pickup, String note) {
        List<Order.Line> lines = new ArrayList<>();
        int total = 0;
        for (CartLine c : cart) {
            lines.add(new Order.Line(c.dish.name.get(), c.size.get(), c.qty.get(), c.unitPrice.get(), c.portionMultiplier.get(), c.note.get()));
            total += c.subtotal();
            c.dish.remaining.set(c.dish.remaining.get() - c.qty.get());
        }
        Order o = new Order(nextOrderId++, currentUser, LocalDateTime.now(), pickup, note, total, lines, Status.PENDING);
        orders.add(0, o);

        // Lưu đơn hàng vào SQLite
        try {
            iu.canteen.dao.OrderDao orderDao = new iu.canteen.dao.OrderDao();
            iu.canteen.model.Order dbOrder = new iu.canteen.model.Order();
            dbOrder.setCustomerEmail(currentUser);
            dbOrder.setPickupTime(pickup);
            dbOrder.setNote(note);
            dbOrder.setTotalAmount(total);
            dbOrder.setStatus(iu.canteen.model.Order.Status.PENDING);
            dbOrder.setCreatedAt(LocalDateTime.now());

            List<iu.canteen.model.OrderItem> dbItems = new ArrayList<>();
            for (CartLine c : cart) {
                iu.canteen.model.OrderItem it = new iu.canteen.model.OrderItem();
                it.setMenuItemId(c.dish.id);
                it.setMenuItemName(c.dish.name.get());
                it.setSizeLabel(c.size.get());
                it.setQuantity(c.qty.get());
                it.setUnitPrice(c.unitPrice.get());
                it.setPortionMultiplier(c.portionMultiplier.get());
                it.setNote(c.note.get());
                dbItems.add(it);
            }
            dbOrder.setItems(dbItems);
            orderDao.createOrder(dbOrder);
        } catch (Exception ignored) {}

        cart.clear();
        return o;
    }

    /** Hủy đơn (chỉ khi quán chưa nhận) và trả lại số phần. */
    public static void cancelOrder(Order o) {
        if (o.status.get() != Status.PENDING) return;
        o.status.set(Status.CANCELLED);
        for (Order.Line l : o.lines) {
            for (Dish d : menu) {
                if (d.name.get().equals(l.name())) d.remaining.set(d.remaining.get() + l.qty());
            }
        }
    }

    /** Dự đoán nguyên liệu (mock): nhu cầu = Σ(lượng bán TB/ngày × định lượng × số ngày). */
    public static List<ForecastRow> forecast(int days, ForecastStrategy strategy) {
        return ForecastService.forecast(days, strategy);
    }
}
