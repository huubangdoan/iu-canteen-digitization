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

        public Dish(int id, String name, int price, String category, int remaining, int avgDaily) {
            this.id = id;
            this.name.set(name);
            this.price.set(price);
            this.category.set(category);
            this.remaining.set(remaining);
            this.avgDaily = avgDaily;
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

    /** Một dòng trong giỏ hàng */
    public static class CartLine {
        public final Dish dish;
        public final IntegerProperty qty = new SimpleIntegerProperty(1);
        public final StringProperty note = new SimpleStringProperty("");
        public CartLine(Dish dish) { this.dish = dish; }
        public int subtotal() { return dish.price.get() * qty.get(); }
    }

    /** orders + order_items */
    public static class Order {
        public record Line(String name, int qty, int unitPrice, String note) {}

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
            for (Line l : lines) j.add(l.name() + " x" + l.qty());
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
            c -> new Observable[]{c.qty, c.note});

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
        // ---- Menu ----
        menu.add(new Dish(nextDishId++, "Cơm gà xối mỡ", 35000, "Cơm", 40, 60));
        menu.add(new Dish(nextDishId++, "Cơm sườn nướng", 38000, "Cơm", 35, 50));
        menu.add(new Dish(nextDishId++, "Phở bò", 45000, "Món nước", 25, 40));
        menu.add(new Dish(nextDishId++, "Bún bò Huế", 42000, "Món nước", 20, 30));
        menu.add(new Dish(nextDishId++, "Bánh mì trứng", 20000, "Ăn nhẹ", 50, 45));
        menu.add(new Dish(nextDishId++, "Trà đào cam sả", 25000, "Đồ uống", 60, 70));
        menu.add(new Dish(nextDishId++, "Cà phê sữa", 22000, "Đồ uống", 80, 90));
        Dish out = new Dish(nextDishId++, "Nước cam ép", 28000, "Đồ uống", 0, 25);
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

        // ---- Công thức (định lượng/phần) ----
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
                new Order.Line("Cơm gà xối mỡ", 1, 35000, "Ít cơm"), new Order.Line("Trà đào cam sả", 1, 25000, ""));
        seedOrder("an.nguyen@iu.edu.vn", now.minusMinutes(12), "11:45", "", Status.PENDING,
                new Order.Line("Phở bò", 2, 45000, "Không hành"));
        seedOrder("binh.tran@iu.edu.vn", now.minusMinutes(9), "11:50", "Gọi giúp mình khi xong", Status.ACCEPTED,
                new Order.Line("Cơm sườn nướng", 1, 38000, ""), new Order.Line("Cà phê sữa", 1, 22000, "Ít đá"));
        seedOrder("student@iu.edu.vn", now.minusMinutes(6), "12:00", "", Status.PREPARING,
                new Order.Line("Bánh mì trứng", 2, 20000, "Thêm tương ớt"));
        seedOrder("chi.le@iu.edu.vn", now.minusMinutes(3), "12:10", "", Status.READY,
                new Order.Line("Bún bò Huế", 1, 42000, ""));
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

    // ===================== Actions (mock) =====================
    public static ObservableList<RecipeLine> recipeFor(Dish d) {
        return recipes.computeIfAbsent(d.id, k -> FXCollections.observableArrayList());
    }

    public static Dish addDish(String name, int price, String category, int remaining) {
        Dish d = new Dish(nextDishId++, name, price, category, remaining, 20);
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
            lines.add(new Order.Line(c.dish.name.get(), c.qty.get(), c.dish.price.get(), c.note.get()));
            total += c.subtotal();
            c.dish.remaining.set(c.dish.remaining.get() - c.qty.get());
        }
        Order o = new Order(nextOrderId++, currentUser, LocalDateTime.now(), pickup, note, total, lines, Status.PENDING);
        orders.add(0, o);
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
