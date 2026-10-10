package iu.canteen.ui;

import iu.canteen.logic.Store;
import iu.canteen.logic.Store.Dish;
import iu.canteen.model.MenuItemSize;
import javafx.animation.PauseTransition;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.WeakListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import javafx.util.StringConverter;

/**
 * Giao diện thực đơn sinh viên hỗ trợ chọn Size (S/M/L) và số lượng trực tiếp.
 */
final class StudentMenuView {
    @FunctionalInterface
    public interface AddToCartHandler {
        void add(Dish dish, MenuItemSize size, int quantity);
    }

    private ListChangeListener<Dish> menuListener;

    Node build(AddToCartHandler onAddToCart) {
        ComboBox<String> category = new ComboBox<>();
        category.getItems().add("Tất cả");
        category.getItems().addAll(Store.CATEGORIES);
        category.getSelectionModel().selectFirst();

        TextField search = new TextField();
        search.setPromptText("🔍  Tìm món...");
        search.setPrefWidth(260);

        FlowPane grid = new FlowPane(16, 16);
        grid.setPadding(new Insets(4));
        Runnable render = () -> {
            grid.getChildren().clear();
            String keyword = search.getText().trim().toLowerCase();
            for (Dish dish : Store.menu) {
                boolean matchesCategory = category.getValue() == null
                        || category.getValue().equals("Tất cả")
                        || category.getValue().equals(dish.category.get());
                boolean matchesSearch = keyword.isEmpty()
                        || dish.name.get().toLowerCase().contains(keyword);
                if (matchesCategory && matchesSearch) {
                    grid.getChildren().add(dishCard(dish, onAddToCart));
                }
            }
        };
        category.setOnAction(event -> render.run());
        search.textProperty().addListener((observable, oldValue, newValue) -> render.run());
        menuListener = change -> render.run();
        Store.menu.addListener(new WeakListChangeListener<>(menuListener));
        render.run();

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("transparent-scroll");

        HBox filters = new HBox(10, Ui.label("Thực đơn hôm nay", "h1"), Ui.spacer(),
                Ui.label("Danh mục:", "muted"), category, search);
        filters.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(14, filters, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        box.setPadding(new Insets(16));
        return box;
    }

    private String emoji(String category) {
        return switch (category) {
            case "Cơm" -> "🍚";
            case "Món nước" -> "🍜";
            case "Ăn nhẹ" -> "🥖";
            case "Đồ uống" -> "🧋";
            default -> "🍴";
        };
    }

    private Node dishCard(Dish dish, AddToCartHandler onAddToCart) {
        StackPane thumb = new StackPane(Ui.label(emoji(dish.category.get()), "menu-emoji"));
        thumb.getStyleClass().add("menu-thumb");

        Label name = Ui.label(dish.name.get(), "dish-name");
        name.setWrapText(true);
        Label tag = Ui.label(dish.category.get(), "tag");
        Label stock = dish.orderable()
                ? Ui.label("Còn " + dish.remaining.get() + " phần", "stock-ok")
                : Ui.label(dish.available.get() ? "Hết món" : "Tạm ngưng", "soldout");

        // Bộ chọn Size (S / M / L)
        ComboBox<MenuItemSize> sizeCombo = new ComboBox<>();
        if (!dish.sizes.isEmpty()) {
            sizeCombo.getItems().addAll(dish.sizes);
            MenuItemSize defaultSize = dish.getSize("M");
            sizeCombo.getSelectionModel().select(defaultSize != null ? defaultSize : dish.sizes.get(0));
        } else {
            MenuItemSize fallback = new MenuItemSize(0, dish.id, "M", dish.price.get(), 1.0, 0);
            sizeCombo.getItems().add(fallback);
            sizeCombo.getSelectionModel().selectFirst();
        }
        sizeCombo.setMaxWidth(Double.MAX_VALUE);
        sizeCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(MenuItemSize s) {
                if (s == null) return "";
                return "Size " + s.getSizeLabel() + " • " + Ui.money(s.getPrice());
            }

            @Override
            public MenuItemSize fromString(String string) {
                return null;
            }
        });

        Label price = Ui.label("", "price");
        Label portionHint = Ui.label("", "portion-hint");
        portionHint.setWrapText(true);

        Runnable updateSizeView = () -> {
            MenuItemSize selected = sizeCombo.getValue();
            if (selected != null) {
                price.setText(Ui.money(selected.getPrice()));
                switch (selected.getSizeLabel().toUpperCase()) {
                    case "S" -> portionHint.setText("🌱 Size S: -15% cơm/thịt (chống lãng phí)");
                    case "M" -> portionHint.setText("⭐ Size M: Tiêu chuẩn vừa vặn");
                    case "L" -> portionHint.setText("💪 Size L: No nê +30% khẩu phần");
                    default -> portionHint.setText("Hệ số định lượng: " + selected.getPortionMultiplier() + "x");
                }
            } else {
                price.setText(Ui.money(dish.price.get()));
                portionHint.setText("");
            }
        };
        sizeCombo.setOnAction(e -> updateSizeView.run());
        updateSizeView.run();

        // Bộ chọn Số lượng: [-] [qty] [+]
        IntegerProperty selectedQty = new SimpleIntegerProperty(1);
        Button minusBtn = Ui.button("−", "btn-small");
        Label qtyVal = Ui.label("1", "qty-label");
        Button plusBtn = Ui.button("+", "btn-small");

        minusBtn.setOnAction(e -> {
            if (selectedQty.get() > 1) {
                selectedQty.set(selectedQty.get() - 1);
                qtyVal.setText(String.valueOf(selectedQty.get()));
            }
        });

        plusBtn.setOnAction(e -> {
            if (selectedQty.get() < dish.remaining.get()) {
                selectedQty.set(selectedQty.get() + 1);
                qtyVal.setText(String.valueOf(selectedQty.get()));
            } else {
                Ui.warn("Đạt giới hạn", "Món này chỉ còn " + dish.remaining.get() + " phần.");
            }
        });

        HBox stepper = new HBox(6, minusBtn, qtyVal, plusBtn);
        stepper.setAlignment(Pos.CENTER);

        // Nút thêm vào giỏ
        Button add = Ui.button("＋ Thêm vào giỏ", "btn-primary");
        add.setMaxWidth(Double.MAX_VALUE);
        add.setDisable(!dish.orderable());
        add.setOnAction(event -> {
            MenuItemSize chosenSize = sizeCombo.getValue();
            int q = selectedQty.get();
            onAddToCart.add(dish, chosenSize, q);

            // Visual feedback khi bấm thêm
            String originalText = add.getText();
            add.setText("✓ Đã thêm " + q + " phần!");
            add.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white;");
            PauseTransition pause = new PauseTransition(Duration.millis(1000));
            pause.setOnFinished(ev -> {
                add.setText(originalText);
                add.setStyle("");
            });
            pause.play();
        });

        VBox card = Ui.card(
                thumb,
                name,
                new HBox(8, tag, Ui.spacer(), stock),
                sizeCombo,
                portionHint,
                new HBox(8, price, Ui.spacer(), stepper),
                add
        );
        card.getStyleClass().add("menu-card");
        card.setPrefWidth(240);
        if (!dish.orderable()) {
            card.setOpacity(0.5);
            sizeCombo.setDisable(true);
            minusBtn.setDisable(true);
            plusBtn.setDisable(true);
        }
        return card;
    }
}
