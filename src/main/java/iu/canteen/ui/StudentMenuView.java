package iu.canteen.ui;

import iu.canteen.logic.Store;
import iu.canteen.logic.Store.Dish;
import javafx.collections.ListChangeListener;
import javafx.collections.WeakListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/** Builds and refreshes the student-facing menu. */
final class StudentMenuView {
    private ListChangeListener<Dish> menuListener;

    Node build(Consumer<Dish> onAddToCart) {
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
        switch (category) {
            case "Cơm":      return "🍚";
            case "Món nước": return "🍜";
            case "Ăn nhẹ":   return "🥖";
            case "Đồ uống":  return "🧋";
            default:         return "🍴";
        }
    }

    private Node dishCard(Dish dish, Consumer<Dish> onAddToCart) {
        StackPane thumb = new StackPane(Ui.label(emoji(dish.category.get()), "menu-emoji"));
        thumb.getStyleClass().add("menu-thumb");

        Label name = Ui.label(dish.name.get(), "dish-name");
        name.setWrapText(true);
        Label tag = Ui.label(dish.category.get(), "tag");
        Label price = Ui.label(Ui.money(dish.price.get()), "price");
        Label stock = dish.orderable()
                ? Ui.label("Còn " + dish.remaining.get() + " phần", "stock-ok")
                : Ui.label(dish.available.get() ? "Hết món" : "Tạm ngưng bán", "soldout");

        Button add = Ui.button("＋ Thêm vào giỏ", "btn-primary");
        add.setMaxWidth(Double.MAX_VALUE);
        add.setDisable(!dish.orderable());
        add.setOnAction(event -> onAddToCart.accept(dish));

        VBox card = Ui.card(thumb, name, new HBox(8, tag, Ui.spacer(), stock), price, add);
        card.getStyleClass().add("menu-card");
        card.setPrefWidth(220);
        if (!dish.orderable()) card.setOpacity(0.45);
        return card;
    }
}
