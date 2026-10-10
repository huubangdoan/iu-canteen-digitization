package iu.canteen.ui;

import iu.canteen.logic.Store;
import iu.canteen.logic.Store.Batch;
import iu.canteen.logic.Store.Ingredient;
import iu.canteen.logic.Store.WasteLog;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Builds the inventory, receiving, and waste tabs for the vendor dashboard. */
final class VendorInventoryView {
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    Node build() {
        TabPane tabs = new TabPane(
                new Tab("Tồn kho & lô hàng", stockPane()),
                new Tab("Nhập kho", receivePane()),
                new Tab("Ghi hao hụt", wastePane()));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        return tabs;
    }

    private Node stockPane() {
        TableView<Ingredient> stock = new TableView<>(Store.ingredients);
        stock.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(Ingredient ingredient, boolean empty) {
                super.updateItem(ingredient, empty);
                getStyleClass().remove("row-warn");
                if (!empty && ingredient != null && ingredient.stock.get() < ingredient.minStock) {
                    getStyleClass().add("row-warn");
                }
            }
        });
        TableColumn<Ingredient, String> name = new TableColumn<>("Nguyên liệu");
        name.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().name));
        TableColumn<Ingredient, String> unit = new TableColumn<>("Đơn vị");
        unit.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().unit));
        TableColumn<Ingredient, String> quantity = new TableColumn<>("Tồn hiện tại");
        quantity.setCellValueFactory(c -> c.getValue().stock.asString("%.1f"));
        TableColumn<Ingredient, String> minimum = new TableColumn<>("Tồn tối thiểu");
        minimum.setCellValueFactory(c -> new ReadOnlyStringWrapper(Ui.num(c.getValue().minStock)));
        TableColumn<Ingredient, String> shelfLife = new TableColumn<>("HSD (ngày)");
        shelfLife.setCellValueFactory(c -> new ReadOnlyStringWrapper(String.valueOf(c.getValue().shelfLifeDays)));
        TableColumn<Ingredient, String> status = new TableColumn<>("Tình trạng");
        status.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().stock.get() < c.getValue().minStock ? "⚠ Dưới mức tối thiểu" : "Đủ"));
        stock.getColumns().addAll(List.of(name, unit, quantity, minimum, shelfLife, status));
        stock.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableView<Batch> batches = batchTable();
        VBox box = new VBox(10, Ui.label("Tồn kho nguyên liệu", "h2"), stock,
                Ui.label("Lô hàng (theo hạn dùng)", "h2"), batches);
        VBox.setVgrow(stock, Priority.ALWAYS);
        VBox.setVgrow(batches, Priority.ALWAYS);
        box.setPadding(new Insets(16));
        return box;
    }

    private TableView<Batch> batchTable() {
        TableView<Batch> table = new TableView<>(Store.batches);
        table.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(Batch batch, boolean empty) {
                super.updateItem(batch, empty);
                getStyleClass().removeAll("row-warn", "row-soon");
                if (!empty && batch != null) {
                    long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), batch.expiry);
                    if (daysLeft < 0) getStyleClass().add("row-warn");
                    else if (daysLeft <= 2) getStyleClass().add("row-soon");
                }
            }
        });
        TableColumn<Batch, String> name = new TableColumn<>("Nguyên liệu");
        name.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().ingredient.name));
        TableColumn<Batch, String> quantity = new TableColumn<>("Số lượng");
        quantity.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                Ui.num(c.getValue().qty) + " " + c.getValue().ingredient.unit));
        TableColumn<Batch, String> received = new TableColumn<>("Ngày nhập");
        received.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().received.format(DMY)));
        TableColumn<Batch, String> expiry = new TableColumn<>("Hạn dùng");
        expiry.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().expiry.format(DMY)));
        TableColumn<Batch, String> remaining = new TableColumn<>("Còn lại");
        remaining.setCellValueFactory(c -> {
            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), c.getValue().expiry);
            return new ReadOnlyStringWrapper(daysLeft < 0 ? "Đã hết hạn" : daysLeft + " ngày");
        });
        TableColumn<Batch, String> cost = new TableColumn<>("Chi phí");
        cost.setCellValueFactory(c -> new ReadOnlyStringWrapper(Ui.money(c.getValue().cost)));
        table.getColumns().addAll(List.of(name, quantity, received, expiry, remaining, cost));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        return table;
    }

    private Node receivePane() {
        ComboBox<Ingredient> ingredient = new ComboBox<>(Store.ingredients);
        ingredient.setPromptText("Chọn nguyên liệu");
        ingredient.setMaxWidth(Double.MAX_VALUE);
        TextField quantity = new TextField();
        quantity.setPromptText("Số lượng");
        TextField cost = new TextField();
        cost.setPromptText("Tổng chi phí (đ)");
        DatePicker received = new DatePicker(LocalDate.now());
        DatePicker expiry = new DatePicker();
        received.setMaxWidth(Double.MAX_VALUE);
        expiry.setMaxWidth(Double.MAX_VALUE);

        Runnable autoExpiry = () -> {
            if (ingredient.getValue() != null && received.getValue() != null) {
                expiry.setValue(received.getValue().plusDays(ingredient.getValue().shelfLifeDays));
            }
        };
        ingredient.setOnAction(event -> autoExpiry.run());
        received.setOnAction(event -> autoExpiry.run());

        Button save = Ui.button("Nhập kho", "btn-primary");
        save.setMaxWidth(Double.MAX_VALUE);
        save.setOnAction(event -> {
            Double amount = Ui.parse(quantity.getText());
            Double totalCost = Ui.parse(cost.getText());
            if (ingredient.getValue() == null || amount == null || amount <= 0
                    || totalCost == null || totalCost < 0
                    || received.getValue() == null || expiry.getValue() == null) {
                Ui.warn("Thiếu thông tin", "Chọn nguyên liệu, nhập số lượng, chi phí và ngày hợp lệ.");
                return;
            }
            Ingredient selected = ingredient.getValue();
            double costPerUnit = totalCost / amount;
            try {
                new iu.canteen.service.InventoryService().receiveBatch(
                        selected.id, amount, costPerUnit, received.getValue(), expiry.getValue(), null);
                quantity.clear();
                cost.clear();
                Ui.info("Đã nhập kho thành công 🎉", "+" + Ui.num(amount) + " " + selected.unit + " " + selected.name);
            } catch (Exception ex) {
                Ui.warn("Lỗi nhập kho", ex.getMessage());
            }
        });

        VBox form = Ui.card(Ui.label("Phiếu nhập kho", "h2"),
                Ui.label("Nguyên liệu", "field-label"), ingredient,
                Ui.label("Số lượng", "field-label"), quantity,
                Ui.label("Chi phí", "field-label"), cost,
                Ui.label("Ngày nhập", "field-label"), received,
                Ui.label("Hạn dùng (tự tính theo HSD, có thể sửa)", "field-label"), expiry,
                save);
        form.setPrefWidth(300);
        form.setMinWidth(300);

        VBox right = new VBox(10, Ui.label("Lịch sử nhập kho", "h2"), batchTable());
        VBox.setVgrow(right.getChildren().get(1), Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        HBox box = new HBox(16, form, right);
        box.setPadding(new Insets(16));
        return box;
    }

    private Node wastePane() {
        ComboBox<Ingredient> ingredient = new ComboBox<>(Store.ingredients);
        ingredient.setPromptText("Chọn nguyên liệu");
        ingredient.setMaxWidth(Double.MAX_VALUE);
        TextField quantity = new TextField();
        quantity.setPromptText("Số lượng hao hụt");
        ComboBox<String> reason = new ComboBox<>();
        reason.getItems().addAll("Hết hạn", "Hư hỏng", "Đổ vỡ", "Chuẩn bị dư", "Khác");
        reason.getSelectionModel().selectFirst();
        reason.setMaxWidth(Double.MAX_VALUE);
        DatePicker date = new DatePicker(LocalDate.now());
        date.setMaxWidth(Double.MAX_VALUE);

        TableView<WasteLog> table = new TableView<>(Store.wastes);
        TableColumn<WasteLog, String> loggedDate = new TableColumn<>("Ngày");
        loggedDate.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().date.format(DMY)));
        TableColumn<WasteLog, String> name = new TableColumn<>("Nguyên liệu");
        name.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().ingredient.name));
        TableColumn<WasteLog, String> amount = new TableColumn<>("Số lượng");
        amount.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                Ui.num(c.getValue().qty) + " " + c.getValue().ingredient.unit));
        TableColumn<WasteLog, String> wasteReason = new TableColumn<>("Lý do");
        wasteReason.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().reason));
        table.getColumns().addAll(List.of(loggedDate, name, amount, wasteReason));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        Button save = Ui.button("Ghi hao hụt", "btn-accent");
        save.setMaxWidth(Double.MAX_VALUE);
        save.setOnAction(event -> {
            Double lost = Ui.parse(quantity.getText());
            if (ingredient.getValue() == null || lost == null || lost <= 0 || date.getValue() == null) {
                Ui.warn("Thiếu thông tin", "Chọn nguyên liệu, nhập số lượng và ngày hợp lệ.");
                return;
            }
            Ingredient selected = ingredient.getValue();
            try {
                new iu.canteen.service.InventoryService().logWaste(
                        selected.id, lost, reason.getValue(), date.getValue(), Store.currentUser);
                quantity.clear();
                Ui.info("Đã ghi nhận hao hụt", "-" + Ui.num(lost) + " " + selected.unit + " " + selected.name);
            } catch (Exception ex) {
                Ui.warn("Lỗi ghi hao hụt", ex.getMessage());
            }
        });

        VBox form = Ui.card(Ui.label("Ghi nhận hao hụt", "h2"),
                Ui.label("Nguyên liệu", "field-label"), ingredient,
                Ui.label("Số lượng", "field-label"), quantity,
                Ui.label("Lý do", "field-label"), reason,
                Ui.label("Ngày", "field-label"), date,
                save);
        form.setPrefWidth(300);
        form.setMinWidth(300);

        VBox right = new VBox(10, Ui.label("Nhật ký hao hụt", "h2"), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        HBox box = new HBox(16, form, right);
        box.setPadding(new Insets(16));
        return box;
    }
}
