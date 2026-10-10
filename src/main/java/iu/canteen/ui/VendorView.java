package iu.canteen.ui;

import iu.canteen.App;
import iu.canteen.logic.ForecastMethod;
import iu.canteen.logic.ForecastService;
import iu.canteen.logic.ForecastService.ForecastResult;
import iu.canteen.logic.Store;
import iu.canteen.logic.Store.*;
import iu.canteen.model.CalendarEvent;
import iu.canteen.model.ForecastEntry;
import iu.canteen.model.WeatherData;
import iu.canteen.service.HolidayService;
import iu.canteen.service.WeatherService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.ListChangeListener;
import javafx.collections.WeakListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Giao diện Quản lý quán (VENDOR / ADMIN). */
public class VendorView extends AbstractAuthenticatedView {
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");
    private static final Status[] BOARD = {Status.PENDING, Status.ACCEPTED, Status.PREPARING, Status.READY};

    private final Map<Status, VBox> bodies = new EnumMap<>(Status.class);
    private final Map<Status, Label> titles = new EnumMap<>(Status.class);
    private Label doneToday;

    private ListChangeListener<Order> orderListener;

    private javafx.animation.Timeline poll;

    private final VendorEndOfDayView endOfDayView = new VendorEndOfDayView();
    private final VendorWasteDashboard wasteDashboard = new VendorWasteDashboard();
    private final iu.canteen.service.ReportService reportService = new iu.canteen.service.ReportService();

    @Override
    protected String roleLabel() {
        return Store.currentRole == Role.ADMIN ? "Quản trị" : "Quản lý quán";
    }

    @Override
    protected void onLogout() {
        if (poll != null) poll.stop();
    }

    @Override
    protected Parent buildContent() {
        TabPane tabs = new TabPane(
                new Tab("🔥  Đơn đến", buildBoard()),
                new Tab("🍽  Menu", buildMenuManager()),
                new Tab("📖  Công thức", buildRecipes()),
                new Tab("📦  Kho", buildInventory()),
                new Tab("🔮  Dự đoán", buildForecast()),
                new Tab("♻️  Kiểm kê & Flash Sale", endOfDayView.build()),
                new Tab("📉  Giảm lãng phí", wasteDashboard.build()),
                new Tab("📊  Thống kê", buildStats()));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Tự động đồng bộ đơn từ DB mỗi 3 giây cho bảng bếp
        poll = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.seconds(3), e -> {
            try {
                new iu.canteen.service.OrderService().refreshAllOrders();
            } catch (Exception ignored) {}
        }));
        poll.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        poll.play();

        return tabs;
    }

    // =========================================================
    // TAB: ĐƠN ĐẾN (bảng bếp)
    // =========================================================
    private Node buildBoard() {
        HBox row = new HBox(14);
        row.setPadding(new Insets(4));
        for (Status s : BOARD) {
            VBox body = new VBox(10);
            body.setPadding(new Insets(4));
            ScrollPane sp = new ScrollPane(body);
            sp.setFitToWidth(true);
            sp.getStyleClass().add("transparent-scroll");
            Label title = Ui.label("", "col-title");
            VBox col = new VBox(8, title, sp);
            col.getStyleClass().add("board-col");
            VBox.setVgrow(sp, Priority.ALWAYS);
            HBox.setHgrow(col, Priority.ALWAYS);
            col.setPrefWidth(280);
            bodies.put(s, body);
            titles.put(s, title);
            row.getChildren().add(col);
        }
        doneToday = Ui.label("", "muted");
        HBox head = new HBox(10, Ui.label("Bảng bếp – đơn đến", "h1"), Ui.spacer(), doneToday);
        head.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(12, head, row);
        VBox.setVgrow(row, Priority.ALWAYS);
        box.setPadding(new Insets(16));

        orderListener = c -> renderBoard();
        Store.orders.addListener(new WeakListChangeListener<>(orderListener));
        renderBoard();
        return box;
    }

    private void renderBoard() {
        for (Status s : BOARD) {
            VBox body = bodies.get(s);
            body.getChildren().clear();
            int n = 0;
            List<Order> list = new ArrayList<>();
            for (Order o : Store.orders) if (o.status.get() == s) list.add(o);
            list.sort(Comparator.comparing(o -> o.pickup));
            for (Order o : list) {
                body.getChildren().add(orderCard(o));
                n++;
            }
            titles.get(s).setText(s.label + "  (" + n + ")");
        }
        long done = Store.orders.stream().filter(o -> o.status.get() == Status.COMPLETED).count();
        doneToday.setText("Đã hoàn thành hôm nay: " + done + " đơn");
    }

    private String actionText(Status s) {
        switch (s) {
            case PENDING:   return "✔ Nhận đơn";
            case ACCEPTED:  return "👨‍🍳 Bắt đầu chuẩn bị";
            case PREPARING: return "🔔 Sẵn sàng";
            case READY:     return "✅ Hoàn thành";
            default:        return "";
        }
    }

    private Node orderCard(Order o) {
        VBox box = new VBox(6);
        box.getStyleClass().add("order-card");
        HBox head = new HBox(8, Ui.label("#" + o.id, "order-id"), Ui.spacer(),
                Ui.label(o.createdAt.format(HM), "muted"));
        box.getChildren().addAll(head, Ui.label(o.customer, "muted"));
        for (Order.Line l : o.lines) {
            String sizeInfo = (l.size() != null && !l.size().isEmpty()) ? " [Size " + l.size() + "]" : "";
            box.getChildren().add(Ui.label(l.qty() + " × " + l.name() + sizeInfo, "line-item"));
            if (l.note() != null && !l.note().isBlank()) {
                box.getChildren().add(Ui.label("   ↳ " + l.note(), "note-text"));
            }
        }
        if (o.note != null && !o.note.isBlank()) {
            Label n = Ui.label("📝 " + o.note, "note-text");
            n.setWrapText(true);
            box.getChildren().add(n);
        }
        box.getChildren().add(Ui.label("⏰ Lấy lúc " + o.pickup, "pickup-chip"));
        box.getChildren().add(Ui.label(Ui.money(o.total), "price"));

        Status s = o.status.get();
        Status next = s.next();
        HBox actions = new HBox(8);
        if (next != null) {
            Button b = Ui.button(actionText(s), "btn-primary");
            b.setOnAction(e -> new iu.canteen.service.OrderService().updateStatus(o.id, next));
            actions.getChildren().add(b);
        }
        if (s == Status.PENDING) {
            Button rej = Ui.button("Từ chối", "btn-danger");
            rej.setOnAction(e -> {
                if (Ui.confirm("Từ chối đơn #" + o.id + "?", "Khách sẽ thấy đơn bị hủy.")) {
                    new iu.canteen.service.OrderService().updateStatus(o.id, Status.CANCELLED);
                }
            });
            actions.getChildren().add(rej);
        }
        box.getChildren().add(actions);
        return box;
    }

    // =========================================================
    // TAB: QUẢN LÝ MENU
    // =========================================================
    private Node buildMenuManager() {
        TableView<Dish> table = new TableView<>(Store.menu);
        table.setEditable(true);

        TableColumn<Dish, Integer> cId = new TableColumn<>("ID");
        cId.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().id));
        cId.setPrefWidth(50);

        TableColumn<Dish, String> cName = new TableColumn<>("Tên món");
        cName.setCellValueFactory(c -> c.getValue().name);
        cName.setCellFactory(TextFieldTableCell.forTableColumn());
        cName.setPrefWidth(220);

        TableColumn<Dish, String> cCat = new TableColumn<>("Danh mục");
        cCat.setCellValueFactory(c -> c.getValue().category);
        cCat.setCellFactory(ComboBoxTableCell.forTableColumn(Store.CATEGORIES.toArray(new String[0])));
        cCat.setPrefWidth(120);

        TableColumn<Dish, Integer> cPrice = new TableColumn<>("Giá (đ)");
        cPrice.setCellValueFactory(c -> c.getValue().price.asObject());
        cPrice.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        cPrice.setPrefWidth(100);

        TableColumn<Dish, Integer> cRem = new TableColumn<>("Còn lại (phần)");
        cRem.setCellValueFactory(c -> c.getValue().remaining.asObject());
        cRem.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        cRem.setPrefWidth(110);

        TableColumn<Dish, Boolean> cAvail = new TableColumn<>("Đang bán");
        cAvail.setCellValueFactory(c -> c.getValue().available);
        cAvail.setCellFactory(CheckBoxTableCell.forTableColumn(cAvail));
        cAvail.setPrefWidth(80);

        table.getColumns().addAll(List.of(cId, cName, cCat, cPrice, cRem, cAvail));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // Form thêm món
        TextField name = new TextField();
        name.setPromptText("Tên món");
        TextField price = new TextField();
        price.setPromptText("Giá (đ)");
        ComboBox<String> cat = new ComboBox<>();
        cat.getItems().addAll(Store.CATEGORIES);
        cat.getSelectionModel().selectFirst();
        cat.setMaxWidth(Double.MAX_VALUE);
        TextField qty = new TextField("30");

        Button add = Ui.button("＋ Thêm món", "btn-primary");
        add.setMaxWidth(Double.MAX_VALUE);
        add.setOnAction(e -> {
            Double p = Ui.parse(price.getText());
            Double q = Ui.parse(qty.getText());
            if (name.getText().isBlank() || p == null || q == null || p <= 0) {
                Ui.warn("Thiếu thông tin", "Nhập tên món, giá và số phần hợp lệ.");
                return;
            }
            try {
                iu.canteen.dao.MenuDao menuDao = new iu.canteen.dao.MenuDao();
                iu.canteen.model.MenuItem created = menuDao.createMenuItem(name.getText().trim(), cat.getValue(), true);
                if (created != null) {
                    menuDao.addMenuItemSize(created.getId(), "M", p.intValue(), 1.0, 0);
                }
            } catch (Exception ignored) {}
            Store.addDish(name.getText().trim(), p.intValue(), cat.getValue(), q.intValue());
            name.clear();
            price.clear();
        });

        Button del = Ui.button("Xóa món đã chọn", "btn-danger");
        del.setMaxWidth(Double.MAX_VALUE);
        del.setOnAction(e -> {
            Dish d = table.getSelectionModel().getSelectedItem();
            if (d != null && Ui.confirm("Xóa \"" + d.name.get() + "\"?", "Thao tác này không hoàn tác được.")) {
                Store.menu.remove(d);
            }
        });

        VBox side = Ui.card(Ui.label("Thêm món mới", "h2"),
                Ui.label("Tên món", "field-label"), name,
                Ui.label("Danh mục", "field-label"), cat,
                Ui.label("Giá", "field-label"), price,
                Ui.label("Số phần hôm nay", "field-label"), qty,
                add, new Separator(), del,
                Ui.label("Mẹo: nhấp đúp vào ô để sửa trực tiếp; bỏ tick “Đang bán” để ẩn món.", "muted"));
        ((Label) side.getChildren().get(side.getChildren().size() - 1)).setWrapText(true);
        side.setPrefWidth(280);
        side.setMinWidth(280);

        HBox box = new HBox(16, table, side);
        HBox.setHgrow(table, Priority.ALWAYS);
        box.setPadding(new Insets(16));
        return box;
    }

    // =========================================================
    // TAB: CÔNG THỨC
    // =========================================================
    private Node buildRecipes() {
        ListView<Dish> list = new ListView<>(Store.menu);
        list.setPrefWidth(240);

        Label title = Ui.label("Chọn một món ở bên trái", "h2");
        TableView<RecipeLine> table = new TableView<>();
        table.setEditable(true);
        table.setPlaceholder(new Label("Món này chưa có công thức."));

        TableColumn<RecipeLine, String> cIng = new TableColumn<>("Nguyên liệu");
        cIng.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().ingredient.name));
        cIng.setPrefWidth(200);

        TableColumn<RecipeLine, Double> cQty = new TableColumn<>("Định lượng / phần");
        cQty.setCellValueFactory(c -> c.getValue().qty.asObject());
        cQty.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        cQty.setPrefWidth(150);

        TableColumn<RecipeLine, String> cUnit = new TableColumn<>("Đơn vị");
        cUnit.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().ingredient.unit));
        cUnit.setPrefWidth(80);

        TableColumn<RecipeLine, RecipeLine> cDel = new TableColumn<>("");
        cDel.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        cDel.setPrefWidth(60);
        cDel.setSortable(false);
        cDel.setCellFactory(col -> new TableCell<>() {
            private final Button b = Ui.button("✕", "btn-danger");
            {
                b.setOnAction(e -> {
                    Dish d = list.getSelectionModel().getSelectedItem();
                    if (d != null && getItem() != null) Store.recipeFor(d).remove(getItem());
                });
            }
            @Override protected void updateItem(RecipeLine r, boolean empty) {
                super.updateItem(r, empty);
                setGraphic(empty || r == null ? null : b);
            }
        });
        table.getColumns().addAll(List.of(cIng, cQty, cUnit, cDel));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        ComboBox<Ingredient> ing = new ComboBox<>(Store.ingredients);
        ing.setPromptText("Nguyên liệu");
        TextField qty = new TextField();
        qty.setPromptText("Định lượng");
        qty.setPrefWidth(110);
        Button add = Ui.button("＋ Thêm", "btn-primary");
        add.setOnAction(e -> {
            Dish d = list.getSelectionModel().getSelectedItem();
            Double q = Ui.parse(qty.getText());
            if (d == null || ing.getValue() == null || q == null || q <= 0) {
                Ui.warn("Thiếu thông tin", "Chọn món, nguyên liệu và nhập định lượng hợp lệ.");
                return;
            }
            try {
                new iu.canteen.dao.RecipeDao().saveRecipe(d.id, ing.getValue().id, q);
            } catch (Exception ignored) {}
            Store.recipeFor(d).add(new RecipeLine(ing.getValue(), q));
            qty.clear();
        });

        list.getSelectionModel().selectedItemProperty().addListener((o, a, d) -> {
            if (d == null) {
                table.setItems(null);
                title.setText("Chọn một món ở bên trái");
            } else {
                table.setItems(Store.recipeFor(d));
                title.setText("Công thức: " + d.name.get());
            }
        });
        list.getSelectionModel().selectFirst();

        VBox right = Ui.card(title, table, new HBox(8, ing, qty, add),
                Ui.label("Công thức là đầu vào cho phần dự đoán nguyên liệu: nhập càng chính xác thì dự đoán càng sát.", "muted"));
        VBox.setVgrow(table, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);

        HBox box = new HBox(16, list, right);
        box.setPadding(new Insets(16));
        return box;
    }

    // =========================================================
    // TAB: THỐNG KÊ
    // =========================================================
    private Node buildStats() {
        ComboBox<String> range = new ComboBox<>();
        range.getItems().addAll("Hôm nay", "7 ngày qua", "30 ngày qua");
        range.getSelectionModel().selectFirst();

        Label kOrders = Ui.label("", "kpi-value");
        Label kRevenue = Ui.label("", "kpi-value");
        Label kBest = Ui.label("", "kpi-value");
        Label kPeak = Ui.label("", "kpi-value");

        CategoryAxis hx = new CategoryAxis();
        hx.setLabel("Giờ");
        NumberAxis hy = new NumberAxis();
        hy.setLabel("Số đơn");
        LineChart<String, Number> hourly = new LineChart<>(hx, hy);
        hourly.setTitle("Số đơn theo giờ");
        hourly.setLegendVisible(false);
        hourly.setCreateSymbols(true);

        CategoryAxis bx = new CategoryAxis();
        NumberAxis by = new NumberAxis();
        by.setLabel("Số phần bán");
        BarChart<String, Number> best = new BarChart<>(bx, by);
        best.setTitle("Món bán chạy");
        best.setLegendVisible(false);

        Runnable load = () -> {
            LocalDate today = LocalDate.now();
            LocalDate from = switch (range.getSelectionModel().getSelectedIndex()) {
                case 1 -> today.minusDays(6);
                case 2 -> today.minusDays(29);
                default -> today;
            };

            var summary = reportService.getSalesSummary(from, today);
            var hourlyDist = reportService.getHourlyDistribution(from, today);
            var topSelling = reportService.getTopSelling(from, today, 8);

            XYChart.Series<String, Number> s1 = new XYChart.Series<>();
            hourlyDist.forEach((hr, count) -> {
                s1.getData().add(new XYChart.Data<>(String.format("%02dh", hr), count));
            });
            hourly.getData().setAll(List.of(s1));

            XYChart.Series<String, Number> s2 = new XYChart.Series<>();
            if (topSelling.isEmpty()) {
                for (Dish d : Store.menu) {
                    s2.getData().add(new XYChart.Data<>(d.name.get(), d.avgDaily));
                }
            } else {
                for (var item : topSelling) {
                    s2.getData().add(new XYChart.Data<>(item.menuItemName() + " (" + item.sizeLabel() + ")", item.quantitySold()));
                }
            }
            best.getData().setAll(List.of(s2));

            kOrders.setText(String.valueOf(summary.totalOrders()));
            kRevenue.setText(Ui.money(summary.totalRevenue()));
            kBest.setText(summary.bestSellingDish());
            kPeak.setText(summary.totalOrders() > 0 ? String.format("%02d:00 – %02d:00", summary.peakHour(), summary.peakHour() + 1) : "-");
        };
        range.setOnAction(e -> load.run());
        load.run();

        HBox kpis = new HBox(14, kpi("Tổng số đơn", kOrders), kpi("Doanh thu", kRevenue),
                kpi("Bán chạy nhất", kBest), kpi("Giờ cao điểm", kPeak));
        for (Node n : kpis.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);

        HBox charts = new HBox(14, hourly, best);
        HBox.setHgrow(hourly, Priority.ALWAYS);
        HBox.setHgrow(best, Priority.ALWAYS);

        HBox head = new HBox(10, Ui.label("Thống kê bán hàng", "h1"), Ui.spacer(), range);
        head.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(14, head, kpis, charts);
        VBox.setVgrow(charts, Priority.ALWAYS);
        box.setPadding(new Insets(16));
        return box;
    }

    private Node kpi(String title, Label value) {
        VBox v = Ui.card(Ui.label(title, "kpi-title"), value);
        v.setMaxWidth(Double.MAX_VALUE);
        return v;
    }

    // =========================================================
    // TAB: KHO (tồn kho / nhập kho / hao hụt)
    // =========================================================
    private Node buildInventory() {
        return new VendorInventoryView().build();
    }

    // =========================================================
    // TAB: DỰ ĐOÁN NGUYÊN LIỆU
    // =========================================================
    private Node buildForecast() {
        WeatherService weatherService = new WeatherService();
        HolidayService holidayService = new HolidayService();

        ComboBox<Integer> days = new ComboBox<>();
        days.getItems().addAll(3, 4, 5, 6, 7);
        days.getSelectionModel().select(Integer.valueOf(5));
        days.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Integer i) { return i == null ? "" : i + " ngày tới"; }
            @Override public Integer fromString(String s) { return null; }
        });

        ComboBox<ForecastMethod> method = new ComboBox<>();
        method.getItems().addAll(ForecastMethod.values());
        method.getSelectionModel().select(ForecastMethod.SMART_AI_ENSEMBLE);

        TableView<ForecastRow> table = new TableView<>();
        table.setPlaceholder(new Label("Bấm “Chạy dự đoán” để xem gợi ý mua nguyên liệu thông minh."));

        TableColumn<ForecastRow, String> n = new TableColumn<>("Nguyên liệu");
        n.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().name()));
        TableColumn<ForecastRow, String> u = new TableColumn<>("Đơn vị");
        u.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().unit()));
        TableColumn<ForecastRow, String> s = new TableColumn<>("Tồn hiện tại");
        s.setCellValueFactory(c -> new ReadOnlyStringWrapper(Ui.num(c.getValue().stock())));
        TableColumn<ForecastRow, String> need = new TableColumn<>("Dự kiến cần dùng");
        need.setCellValueFactory(c -> new ReadOnlyStringWrapper(Ui.num(c.getValue().need())));
        TableColumn<ForecastRow, String> buy = new TableColumn<>("Gợi ý mua thêm");
        buy.setCellValueFactory(c -> new ReadOnlyStringWrapper(Ui.num(c.getValue().buy())));
        TableColumn<ForecastRow, String> lv = new TableColumn<>("Mức độ");
        lv.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().level()));
        lv.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) {
                    setGraphic(null);
                    return;
                }
                Label l = new Label(v);
                String cls = v.equals("Khẩn cấp") ? "badge-urgent" : v.equals("Nên mua") ? "badge-buy" : "badge-enough";
                l.getStyleClass().addAll("badge", cls);
                setGraphic(l);
            }
        });
        table.getColumns().addAll(List.of(n, u, s, need, buy, lv));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        CategoryAxis cx = new CategoryAxis();
        NumberAxis cy = new NumberAxis();
        BarChart<String, Number> chart = new BarChart<>(cx, cy);
        chart.setTitle("Tồn hiện tại vs. nhu cầu dự kiến");
        chart.setPrefHeight(260);

        Label suggestion = Ui.label("", "muted");
        suggestion.setWrapText(true);

        // Header thông tin bối cảnh thời tiết & sự kiện
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        WeatherData w = weatherService.getWeatherByDate(tomorrow);
        double wFactor = weatherService.getDemandFactor(w);
        double hFactor = holidayService.getDemandFactor(tomorrow);
        double histAccuracy = ForecastService.getAccuracy(LocalDate.now().minusDays(30), LocalDate.now());

        Label weatherInfo = Ui.label(
                String.format("🌤 Thời tiết: %.1f°C, %s (x%.2f)", w.getTemperature(), w.getWeatherCondition(), wFactor),
                "badge"
        );
        weatherInfo.getStyleClass().add("badge-buy");

        String eventDesc = holidayService.getEventByDate(tomorrow)
                .map(CalendarEvent::getDescription)
                .orElse("Ngày học bình thường");
        Label eventInfo = Ui.label(
                String.format("📅 Sự kiện: %s (x%.2f)", eventDesc, hFactor),
                "badge"
        );
        eventInfo.getStyleClass().add("badge-enough");

        Label accInfo = Ui.label(
                String.format("🎯 Độ chính xác mô hình: %.1f%%", histAccuracy),
                "badge"
        );
        accInfo.getStyleClass().add("badge-urgent");

        HBox contextBox = new HBox(12, weatherInfo, eventInfo, accInfo);
        contextBox.setAlignment(Pos.CENTER_LEFT);

        Button run = Ui.button("🔮 Chạy dự đoán", "btn-primary");
        Button save = Ui.button("💾 Lưu dự đoán", "btn-accent");
        save.setDisable(true);

        List<ForecastEntry> currentEntries = new ArrayList<>();

        run.setOnAction(e -> {
            ForecastResult result = ForecastService.forecastDetailed(days.getValue(), method.getValue(), tomorrow);
            table.getItems().setAll(result.rows());
            currentEntries.clear();
            currentEntries.addAll(result.dbEntries());

            XYChart.Series<String, Number> have = new XYChart.Series<>();
            have.setName("Tồn hiện tại");
            XYChart.Series<String, Number> want = new XYChart.Series<>();
            want.setName("Dự kiến cần");
            StringJoiner buyList = new StringJoiner(", ");
            for (ForecastRow r : result.rows()) {
                have.getData().add(new XYChart.Data<>(r.name(), r.stock()));
                want.getData().add(new XYChart.Data<>(r.name(), r.need()));
                if (r.buy() > 0) buyList.add(r.name() + " " + Ui.num(r.buy()) + " " + r.unit());
            }
            chart.getData().setAll(List.of(have, want));
            suggestion.setText(buyList.length() == 0
                    ? String.format("✅ Tồn kho đủ cho %d ngày tới (Hệ số tổng: %.2fx, Phương pháp: %s).",
                        days.getValue(), result.overallMultiplier(), method.getValue().getLabel())
                    : String.format("🛒 Gợi ý nhập kho (Hệ số tổng: %.2fx): %s", result.overallMultiplier(), buyList));
            save.setDisable(false);
        });

        save.setOnAction(e -> {
            if (!currentEntries.isEmpty()) {
                ForecastService.saveForecastToDatabase(currentEntries);
                Ui.info("Đã lưu dự đoán", "Đã lưu " + currentEntries.size() + " mục dự báo vào Database để theo dõi độ chính xác.");
                save.setDisable(true);
            }
        });

        HBox controls = new HBox(10, Ui.label("Dự đoán nguyên liệu", "h1"), Ui.spacer(),
                Ui.label("Khoảng thời gian:", "muted"), days,
                Ui.label("Phương pháp:", "muted"), method, run, save);
        controls.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(12, controls, contextBox, table, Ui.card(suggestion), chart);
        VBox.setVgrow(table, Priority.ALWAYS);
        box.setPadding(new Insets(16));
        return box;
    }
}
