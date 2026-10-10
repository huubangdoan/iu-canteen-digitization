package iu.canteen.ui;

import iu.canteen.logic.Store;
import iu.canteen.logic.Store.Dish;
import iu.canteen.model.Leftover;
import iu.canteen.model.MenuItemSize;
import iu.canteen.service.LeftoverService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Giao diện Kiểm kê cuối ngày & Quản lý Flash Sale dành cho Vendor (Phase 5).
 */
public class VendorEndOfDayView {
    private final LeftoverService leftoverService = new LeftoverService();
    private final ObservableList<Leftover> tableData = FXCollections.observableArrayList();
    private TableView<Leftover> table;

    public Node build() {
        // 1. Tiêu đề và hướng dẫn
        Label title = Ui.label("Kiểm kê cuối ngày & Đăng bán Flash Sale", "h1");
        Label subtitle = Ui.label("Quy trình 15:00–17:30: Kiểm tra suất ăn còn thừa → Đăng Flash Sale giảm 20-50% → Chốt sổ cuối ngày quyên góp hoặc tiêu hủy.", "muted");
        VBox header = new VBox(4, title, subtitle);

        // 2. Form tạo Flash Sale
        Node formNode = buildCreateForm();

        // 3. Bảng danh sách Flash Sale hôm nay
        Node tableNode = buildTable();

        // 4. Thanh tác vụ Chốt sổ cuối ngày
        Node actionFooter = buildFooterActions();

        VBox root = new VBox(16, header, formNode, tableNode, actionFooter);
        root.setPadding(new Insets(16));
        VBox.setVgrow(tableNode, Priority.ALWAYS);

        refresh();
        return root;
    }

    private Node buildCreateForm() {
        ComboBox<Dish> dishCombo = new ComboBox<>(Store.menu);
        dishCombo.setPromptText("Chọn món ăn...");
        dishCombo.setPrefWidth(220);

        ComboBox<String> sizeCombo = new ComboBox<>(FXCollections.observableArrayList("S", "M", "L"));
        sizeCombo.setValue("M");
        sizeCombo.setPrefWidth(80);

        Spinner<Integer> qtySpinner = new Spinner<>(1, 100, 5);
        qtySpinner.setEditable(true);
        qtySpinner.setPrefWidth(80);

        ComboBox<Integer> discountCombo = new ComboBox<>(FXCollections.observableArrayList(20, 30, 40, 50));
        discountCombo.setValue(30);
        discountCombo.setPrefWidth(90);

        Button submitBtn = Ui.button("🚀 Mở bán Flash Sale", "btn-primary");
        submitBtn.setOnAction(e -> {
            Dish dish = dishCombo.getValue();
            if (dish == null) {
                Ui.warn("Chưa chọn món", "Vui lòng chọn món ăn cần mở Flash Sale.");
                return;
            }
            int qty = qtySpinner.getValue();
            int discount = discountCombo.getValue();
            String size = sizeCombo.getValue();

            try {
                leftoverService.createFlashSale(dish.id, size, qty, discount, LocalDate.now());
                Ui.info("Đăng Flash Sale thành công",
                        String.format("Đã mở bán %d suất %s (Size %s) với mức giảm giá %d%%!",
                                qty, dish.name.get(), size, discount));
                refresh();
            } catch (Exception ex) {
                Ui.warn("Lỗi tạo Flash Sale", ex.getMessage());
            }
        });

        HBox form = new HBox(12,
                new VBox(4, Ui.label("Món ăn", "field-label"), dishCombo),
                new VBox(4, Ui.label("Size", "field-label"), sizeCombo),
                new VBox(4, Ui.label("Số phần thừa", "field-label"), qtySpinner),
                new VBox(4, Ui.label("Giảm giá (%)", "field-label"), discountCombo),
                new VBox(4, new Label(" "), submitBtn)
        );
        form.setAlignment(Pos.BOTTOM_LEFT);
        form.getStyleClass().add("card");
        return form;
    }

    private Node buildTable() {
        table = new TableView<>(tableData);
        table.setPlaceholder(new Label("Chưa có món Flash Sale nào được tạo trong ngày hôm nay."));

        TableColumn<Leftover, String> cName = new TableColumn<>("Món ăn");
        cName.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getMenuItemName()));
        cName.setPrefWidth(180);

        TableColumn<Leftover, String> cSize = new TableColumn<>("Size");
        cSize.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getSizeLabel()));
        cSize.setPrefWidth(70);

        TableColumn<Leftover, String> cOrigPrice = new TableColumn<>("Giá gốc");
        cOrigPrice.setCellValueFactory(c -> new ReadOnlyStringWrapper(Ui.money(c.getValue().getOriginalPrice())));
        cOrigPrice.setPrefWidth(100);

        TableColumn<Leftover, String> cFlashPrice = new TableColumn<>("Giá Flash Sale");
        cFlashPrice.setCellValueFactory(c -> new ReadOnlyStringWrapper(Ui.money(c.getValue().getDiscountedPrice())));
        cFlashPrice.setPrefWidth(110);

        TableColumn<Leftover, String> cDiscount = new TableColumn<>("Giảm");
        cDiscount.setCellValueFactory(c -> new ReadOnlyStringWrapper("-" + c.getValue().getDiscountPercent() + "%"));
        cDiscount.setPrefWidth(70);

        TableColumn<Leftover, Integer> cInitial = new TableColumn<>("Ban đầu");
        cInitial.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getInitialQuantity()));
        cInitial.setPrefWidth(80);

        TableColumn<Leftover, Integer> cRemain = new TableColumn<>("Còn lại");
        cRemain.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getRemainingQuantity()));
        cRemain.setPrefWidth(80);

        TableColumn<Leftover, Leftover> cStatus = new TableColumn<>("Trạng thái");
        cStatus.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        cStatus.setPrefWidth(130);
        cStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Leftover item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    String st = item.getStatus();
                    Label badge = new Label(st);
                    badge.getStyleClass().add("badge");
                    switch (st.toUpperCase()) {
                        case "ACTIVE" -> {
                            badge.setText("Đang mở bán");
                            badge.getStyleClass().add("badge-ready");
                        }
                        case "SOLD_OUT" -> {
                            badge.setText("Đã bán hết");
                            badge.getStyleClass().add("badge-completed");
                        }
                        case "DONATED" -> {
                            badge.setText("Đã quyên góp ❤️");
                            badge.getStyleClass().add("badge-accepted");
                        }
                        case "EXPIRED" -> {
                            badge.setText("Đã hủy bỏ 🗑️");
                            badge.getStyleClass().add("badge-cancelled");
                        }
                        default -> badge.getStyleClass().add("badge-pending");
                    }
                    setGraphic(badge);
                }
            }
        });

        table.getColumns().addAll(List.of(cName, cSize, cOrigPrice, cFlashPrice, cDiscount, cInitial, cRemain, cStatus));
        VBox.setVgrow(table, Priority.ALWAYS);

        VBox box = new VBox(8, Ui.label("Danh sách theo dõi các đợt Flash Sale hôm nay:", "h2"), table);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    private Node buildFooterActions() {
        Button refreshBtn = Ui.button("🔄 Làm mới", "btn-small");
        refreshBtn.setOnAction(e -> refresh());

        Button donateBtn = Ui.button("❤️ Quyên góp các phần thừa còn lại", "btn-accent");
        donateBtn.setOnAction(e -> {
            boolean confirm = Ui.confirm("Xác nhận quyên góp",
                    "Bạn có chắc muốn chốt sổ và chuyển toàn bộ các phần ăn còn tồn sang trạng thái QUYÊN GÓP từ thiện?");
            if (confirm) {
                int count = leftoverService.endOfDayProcess(LocalDate.now(), true);
                Ui.info("Chốt sổ thành công", "Đã ghi nhận quyên góp cho " + count + " suất ăn còn thừa.");
                refresh();
            }
        });

        Button wasteBtn = Ui.button("🗑️ Hủy & Ghi nhận hao hụt", "btn-danger");
        wasteBtn.setOnAction(e -> {
            boolean confirm = Ui.confirm("Xác nhận ghi nhận hao hụt",
                    "Bạn có chắc muốn chốt sổ và hủy bỏ các phần ăn còn tồn? Dữ liệu sẽ được ghi vào nhật ký hao hụt.");
            if (confirm) {
                int count = leftoverService.endOfDayProcess(LocalDate.now(), false);
                Ui.info("Chốt sổ thành công", "Đã ghi nhận hủy bỏ " + count + " suất ăn thừa.");
                refresh();
            }
        });

        HBox actions = new HBox(12,
                refreshBtn,
                Ui.spacer(),
                Ui.label("Chốt sổ cuối ngày (17:30):", "field-label"),
                donateBtn,
                wasteBtn
        );
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(10));
        actions.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #e5e7eb; -fx-border-radius: 10;");
        return actions;
    }

    public void refresh() {
        tableData.setAll(leftoverService.getLeftoversByDate(LocalDate.now()));
    }
}
