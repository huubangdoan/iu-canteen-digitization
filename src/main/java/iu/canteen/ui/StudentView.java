package iu.canteen.ui;

import iu.canteen.App;
import iu.canteen.logic.Store;
import iu.canteen.logic.Store.CartLine;
import iu.canteen.logic.Store.Dish;
import iu.canteen.logic.Store.Order;
import iu.canteen.logic.Store.Status;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ListChangeListener;
import javafx.collections.WeakListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Giao diện Sinh viên / Nhân viên: Menu → Giỏ hàng → Theo dõi đơn. */
public class StudentView extends AbstractAuthenticatedView {
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter HMS = DateTimeFormatter.ofPattern("HH:mm:ss");

    private TabPane tabs;
    private Tab cartTab;
    private Tab ordersTab;
    private Timeline poll;

    private final StudentMenuView menuView = new StudentMenuView();
    private final StudentFlashSaleView flashSaleView = new StudentFlashSaleView();
    private ListChangeListener<CartLine> cartListener;

    // Orders tab
    private TableView<Order> ordersTable;
    private HBox stepper;
    private Label lastUpdated;

    @Override
    protected String roleLabel() {
        return "Sinh viên / Nhân viên";
    }

    @Override
    protected void onLogout() {
        if (poll != null) poll.stop();
        flashSaleView.stopTimer();
        Store.cart.clear();
    }

    @Override
    protected Parent buildContent() {
        Tab menuTab = new Tab("🍽  Thực đơn", menuView.build(this::addToCart));
        Tab flashSaleTab = new Tab("🔥  Flash Sale", flashSaleView.build(() -> {
            try {
                new iu.canteen.service.OrderService().refreshMyOrders(Store.currentUser);
            } catch (Exception ignored) {}
            if (ordersTable != null) ordersTable.refresh();
        }));
        cartTab = new Tab("🛒  Giỏ hàng", buildCart());
        ordersTab = new Tab("📋  Đơn của tôi", buildOrders());
        tabs = new TabPane(menuTab, flashSaleTab, cartTab, ordersTab);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        cartListener = c -> cartTab.setText("🛒  Giỏ hàng (" + Store.cartCount() + ")");
        Store.cart.addListener(new WeakListChangeListener<>(cartListener));

        // Polling "thời gian thực": làm mới trạng thái đơn mỗi 3 giây từ Database
        poll = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            try {
                new iu.canteen.service.OrderService().refreshMyOrders(Store.currentUser);
            } catch (Exception ignored) {}
            ordersTable.refresh();
            updateStepper(ordersTable.getSelectionModel().getSelectedItem());
            lastUpdated.setText("Tự động cập nhật mỗi 3 giây • lần cuối " + LocalTime.now().format(HMS));
        }));
        poll.setCycleCount(Timeline.INDEFINITE);
        poll.play();
        return tabs;
    }

    private void addToCart(Dish d, iu.canteen.model.MenuItemSize size, int qty) {
        String sizeLabel = size != null ? size.getSizeLabel() : "M";
        for (CartLine l : Store.cart) {
            if (l.dish == d && l.size.get().equalsIgnoreCase(sizeLabel)) {
                if (l.qty.get() + qty <= d.remaining.get()) {
                    l.qty.set(l.qty.get() + qty);
                } else {
                    Ui.warn("Không đủ số lượng", "Món \"" + d.name.get() + "\" chỉ còn " + d.remaining.get() + " phần.");
                }
                return;
            }
        }
        if (qty <= d.remaining.get()) {
            Store.cart.add(new CartLine(d, size, qty));
        } else {
            Ui.warn("Không đủ số lượng", "Món \"" + d.name.get() + "\" chỉ còn " + d.remaining.get() + " phần.");
        }
    }

    // =========================================================
    // TAB 2: GIỎ HÀNG
    // =========================================================
    private Node buildCart() {
        TableView<CartLine> table = new TableView<>(Store.cart);
        table.setEditable(true);
        table.setPlaceholder(new Label("Giỏ hàng trống – hãy chọn món ở tab Thực đơn 🍽"));

        TableColumn<CartLine, String> cName = new TableColumn<>("Món");
        cName.setCellValueFactory(c -> c.getValue().dish.name);
        cName.setPrefWidth(180);

        TableColumn<CartLine, CartLine> cSize = new TableColumn<>("Size");
        cSize.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        cSize.setPrefWidth(85);
        cSize.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(CartLine l, boolean empty) {
                super.updateItem(l, empty);
                if (empty || l == null) {
                    setGraphic(null);
                } else {
                    String s = l.size.get();
                    Label badge = Ui.label("Size " + s, "size-badge");
                    switch (s.toUpperCase()) {
                        case "S" -> badge.getStyleClass().add("size-s");
                        case "L" -> badge.getStyleClass().add("size-l");
                        default -> badge.getStyleClass().add("size-m");
                    }
                    setGraphic(badge);
                }
            }
        });

        TableColumn<CartLine, String> cPrice = new TableColumn<>("Đơn giá");
        cPrice.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(Ui.money(c.getValue().unitPrice.get())));
        cPrice.setPrefWidth(90);

        TableColumn<CartLine, CartLine> cQty = new TableColumn<>("Số lượng");
        cQty.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        cQty.setPrefWidth(130);
        cQty.setSortable(false);
        cQty.setCellFactory(col -> new TableCell<>() {
            private final Button minus = Ui.button("−", "btn-small");
            private final Button plus = Ui.button("+", "btn-small");
            private final Label q = new Label();
            private final HBox box = new HBox(8, minus, q, plus);
            {
                box.setAlignment(Pos.CENTER);
                minus.setOnAction(e -> {
                    CartLine l = getItem();
                    if (l != null && l.qty.get() > 1) l.qty.set(l.qty.get() - 1);
                });
                plus.setOnAction(e -> {
                    CartLine l = getItem();
                    if (l == null) return;
                    if (l.qty.get() < l.dish.remaining.get()) {
                        l.qty.set(l.qty.get() + 1);
                    } else {
                        Ui.warn("Không đủ số lượng", "Chỉ còn " + l.dish.remaining.get() + " phần.");
                    }
                });
            }
            @Override protected void updateItem(CartLine l, boolean empty) {
                super.updateItem(l, empty);
                if (empty || l == null) {
                    setGraphic(null);
                } else {
                    q.setText(String.valueOf(l.qty.get()));
                    setGraphic(box);
                }
            }
        });

        TableColumn<CartLine, String> cNote = new TableColumn<>("Ghi chú cho món (nhấp đúp để sửa)");
        cNote.setCellValueFactory(c -> c.getValue().note);
        cNote.setCellFactory(TextFieldTableCell.forTableColumn());
        cNote.setOnEditCommit(e -> e.getRowValue().note.set(e.getNewValue()));
        cNote.setPrefWidth(240);

        TableColumn<CartLine, String> cSub = new TableColumn<>("Thành tiền");
        cSub.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(Ui.money(c.getValue().subtotal())));
        cSub.setPrefWidth(100);

        TableColumn<CartLine, CartLine> cDel = new TableColumn<>("");
        cDel.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        cDel.setPrefWidth(60);
        cDel.setSortable(false);
        cDel.setCellFactory(col -> new TableCell<>() {
            private final Button b = Ui.button("✕", "btn-danger");
            { b.setOnAction(e -> { if (getItem() != null) Store.cart.remove(getItem()); }); }
            @Override protected void updateItem(CartLine l, boolean empty) {
                super.updateItem(l, empty);
                setGraphic(empty || l == null ? null : b);
            }
        });

        table.getColumns().addAll(List.of(cName, cSize, cPrice, cQty, cNote, cSub, cDel));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // ---- Panel thanh toán / xác nhận ----
        ComboBox<String> pickup = new ComboBox<>();
        pickup.getItems().addAll(pickupSlots());
        pickup.getSelectionModel().selectFirst();
        pickup.setMaxWidth(Double.MAX_VALUE);

        TextArea note = new TextArea();
        note.setPromptText("Ghi chú chung cho đơn (không bắt buộc)");
        note.setPrefRowCount(3);
        note.setWrapText(true);

        Label itemCount = Ui.label("0 món", "muted");
        Label total = Ui.label("0đ", "price");
        total.setStyle("-fx-font-size: 22px;");

        Runnable refreshTotals = () -> {
            itemCount.setText(Store.cartCount() + " phần");
            total.setText(Ui.money(Store.cartTotal()));
            table.refresh();
        };
        cartListenerHook(refreshTotals);

        Button confirm = Ui.button("Kiểm tra & Xác nhận đặt món", "btn-accent");
        confirm.setMaxWidth(Double.MAX_VALUE);
        confirm.setOnAction(e -> {
            if (Store.cart.isEmpty()) {
                Ui.warn("Giỏ hàng trống", "Hãy chọn ít nhất một món.");
                return;
            }
            // Kiểm tra tổng số lượng còn lại theo từng món
            java.util.Map<Dish, Integer> totalByDish = new java.util.HashMap<>();
            for (CartLine l : Store.cart) {
                totalByDish.merge(l.dish, l.qty.get(), Integer::sum);
            }

            List<String> problems = new ArrayList<>();
            for (java.util.Map.Entry<Dish, Integer> entry : totalByDish.entrySet()) {
                Dish d = entry.getKey();
                int requested = entry.getValue();
                if (!d.available.get() || d.remaining.get() <= 0) {
                    problems.add("• " + d.name.get() + ": đã hết món");
                } else if (requested > d.remaining.get()) {
                    problems.add("• " + d.name.get() + ": chỉ còn " + d.remaining.get() + " phần (bạn chọn tổng " + requested + ")");
                }
            }
            if (!problems.isEmpty()) {
                Ui.warn("Một số món không đủ số lượng", String.join("\n", problems) + "\n\nVui lòng điều chỉnh giỏ hàng.");
                return;
            }
            String slot = pickup.getValue();
            if (Ui.confirm("Xác nhận đặt món?", Store.cartCount() + " phần • " + Ui.money(Store.cartTotal())
                    + "\nGiờ lấy: " + slot)) {
                try {
                    iu.canteen.service.OrderService orderService = new iu.canteen.service.OrderService();
                    Order o = orderService.createOrder(Store.currentUser, slot, note.getText(), new ArrayList<>(Store.cart));
                    note.clear();
                    Ui.info("Đặt món thành công 🎉", "Mã đơn #" + o.id + ". Bạn có thể theo dõi trạng thái ở tab \"Đơn của tôi\".");
                    tabs.getSelectionModel().select(ordersTab);
                    ordersTable.getSelectionModel().select(o);
                } catch (Exception ex) {
                    Ui.warn("Không thể đặt món", ex.getMessage());
                }
            }
        });

        VBox side = Ui.card(
                Ui.label("Thông tin nhận món", "h2"),
                Ui.label("Chọn giờ lấy", "field-label"), pickup,
                Ui.label("Ghi chú", "field-label"), note,
                new Separator(),
                new HBox(8, Ui.label("Tổng cộng", "field-label"), Ui.spacer(), itemCount),
                total, confirm);
        side.setPrefWidth(300);
        side.setMinWidth(300);

        HBox box = new HBox(16, table, side);
        HBox.setHgrow(table, Priority.ALWAYS);
        box.setPadding(new Insets(16));
        return box;
    }

    /** Gắn callback cập nhật tổng tiền khi giỏ hàng thay đổi. */
    private void cartListenerHook(Runnable r) {
        ListChangeListener<CartLine> l = c -> r.run();
        cartTotalsListener = l;
        Store.cart.addListener(new WeakListChangeListener<>(l));
    }
    private ListChangeListener<CartLine> cartTotalsListener;

    private List<String> pickupSlots() {
        List<String> slots = new ArrayList<>();
        LocalTime open = LocalTime.of(7, 0);
        LocalTime close = LocalTime.of(18, 0);
        LocalTime t = LocalTime.now().plusMinutes(15);
        int m = (t.getMinute() + 14) / 15 * 15;          // làm tròn lên mốc 15 phút
        t = t.withMinute(0).withSecond(0).withNano(0).plusMinutes(m);
        if (t.isBefore(open) || t.isBefore(LocalTime.now())) t = open;   // sau nửa đêm hoặc trước giờ mở cửa
        for (; !t.isAfter(close) && !t.isBefore(open); t = t.plusMinutes(15)) {
            slots.add(t.format(HM));
            if (t.equals(LocalTime.of(23, 45))) break;
        }
        if (slots.isEmpty()) slots.add("07:00 (ngày mai)");
        return slots;
    }

    // =========================================================
    // TAB 3: ĐƠN CỦA TÔI
    // =========================================================
    private Node buildOrders() {
        ChoiceBox<String> filter = new ChoiceBox<>();
        filter.getItems().addAll("Tất cả", "Đang xử lý", "Lịch sử");
        filter.getSelectionModel().selectFirst();

        FilteredList<Order> mine = new FilteredList<>(Store.orders);
        Runnable applyFilter = () -> mine.setPredicate(o -> {
            if (!o.customer.equals(Store.currentUser)) return false;
            Status s = o.status.get();
            boolean done = s == Status.COMPLETED || s == Status.CANCELLED;
            switch (filter.getValue()) {
                case "Đang xử lý": return !done;
                case "Lịch sử":    return done;
                default:           return true;
            }
        });
        filter.setOnAction(e -> applyFilter.run());
        applyFilter.run();

        ordersTable = new TableView<>(mine);
        ordersTable.setPlaceholder(new Label("Bạn chưa có đơn nào."));

        TableColumn<Order, String> cId = new TableColumn<>("Mã đơn");
        cId.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>("#" + c.getValue().id));
        cId.setPrefWidth(70);

        TableColumn<Order, String> cAt = new TableColumn<>("Đặt lúc");
        cAt.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().createdAt.format(HM)));
        cAt.setPrefWidth(70);

        TableColumn<Order, String> cPick = new TableColumn<>("Giờ lấy");
        cPick.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().pickup));
        cPick.setPrefWidth(70);

        TableColumn<Order, String> cItems = new TableColumn<>("Món");
        cItems.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().summary()));
        cItems.setPrefWidth(320);

        TableColumn<Order, String> cTotal = new TableColumn<>("Tổng tiền");
        cTotal.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(Ui.money(c.getValue().total)));
        cTotal.setPrefWidth(100);

        TableColumn<Order, Status> cStatus = new TableColumn<>("Trạng thái");
        cStatus.setCellValueFactory(c -> c.getValue().status);
        cStatus.setPrefWidth(130);
        cStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Status s, boolean empty) {
                super.updateItem(s, empty);
                setGraphic(empty || s == null ? null : Ui.badge(s));
            }
        });

        TableColumn<Order, Order> cAct = new TableColumn<>("");
        cAct.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        cAct.setPrefWidth(90);
        cAct.setSortable(false);
        cAct.setCellFactory(col -> new TableCell<>() {
            private final Button cancel = Ui.button("Hủy đơn", "btn-danger");
            {
                cancel.setOnAction(e -> {
                    Order o = getItem();
                    if (o != null && Ui.confirm("Hủy đơn #" + o.id + "?", "Chỉ hủy được khi quán chưa nhận đơn.")) {
                        try {
                            new iu.canteen.service.OrderService().cancelOrder(o.id);
                            ordersTable.refresh();
                            updateStepper(o);
                            Ui.info("Đã hủy đơn", "Đơn hàng #" + o.id + " đã được hủy thành công.");
                        } catch (Exception ex) {
                            Ui.warn("Không thể hủy đơn", ex.getMessage());
                        }
                    }
                });
            }
            @Override protected void updateItem(Order o, boolean empty) {
                super.updateItem(o, empty);
                // Chỉ hiện nút Hủy khi đơn còn PENDING
                setGraphic(empty || o == null || o.status.get() != Status.PENDING ? null : cancel);
            }
        });

        ordersTable.getColumns().addAll(List.of(cId, cAt, cPick, cItems, cTotal, cStatus, cAct));
        ordersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        stepper = new HBox(8);
        stepper.setAlignment(Pos.CENTER_LEFT);
        updateStepper(null);
        ordersTable.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> updateStepper(b));

        lastUpdated = Ui.label("Tự động cập nhật mỗi 3 giây", "muted");

        HBox head = new HBox(10, Ui.label("Đơn hàng của tôi", "h1"), Ui.spacer(), lastUpdated, filter);
        head.setAlignment(Pos.CENTER_LEFT);

        VBox progress = Ui.card(Ui.label("Tiến trình đơn đã chọn", "h2"), stepper);

        VBox box = new VBox(14, head, ordersTable, progress);
        VBox.setVgrow(ordersTable, Priority.ALWAYS);
        box.setPadding(new Insets(16));
        return box;
    }

    private void updateStepper(Order o) {
        stepper.getChildren().clear();
        if (o == null) {
            stepper.getChildren().add(Ui.label("Chọn một đơn trong bảng để xem tiến trình.", "muted"));
            return;
        }
        if (o.status.get() == Status.CANCELLED) {
            stepper.getChildren().add(Ui.label("Đơn #" + o.id + " đã bị hủy", "badge", "badge-cancelled"));
            return;
        }
        Status[] flow = {Status.PENDING, Status.ACCEPTED, Status.PREPARING, Status.READY, Status.COMPLETED};
        int cur = o.status.get().ordinal();   // PENDING..COMPLETED = 0..4
        for (int i = 0; i < flow.length; i++) {
            String mark = i < cur ? "✔ " : (i == cur ? "● " : "○ ");
            Label step = new Label(mark + flow[i].label);
            step.getStyleClass().add("step");
            if (i < cur) step.getStyleClass().add("step-done");
            else if (i == cur) step.getStyleClass().add("step-current");
            stepper.getChildren().add(step);
            if (i < flow.length - 1) {
                Region line = new Region();
                line.setPrefSize(46, 2);
                line.setMinSize(46, 2);
                line.getStyleClass().add(i < cur ? "line-done" : "line");
                stepper.getChildren().add(line);
            }
        }
    }
}
