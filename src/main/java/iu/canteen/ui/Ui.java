package iu.canteen.ui;

import iu.canteen.logic.Store;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Locale;

/** Các hàm tiện ích dựng UI dùng chung. */
public final class Ui {
    private Ui() {}

    public static String money(double v) {
        return String.format(Locale.US, "%,.0f", v).replace(',', '.') + "đ";
    }

    public static String num(double v) {
        return (v == Math.floor(v)) ? String.valueOf((long) v) : String.format(Locale.US, "%.1f", v);
    }

    public static Double parse(String s) {
        try {
            return Double.parseDouble(s.trim().replace(',', '.'));
        } catch (Exception e) {
            return null;
        }
    }

    public static Label label(String text, String... styles) {
        Label l = new Label(text);
        l.getStyleClass().addAll(styles);
        return l;
    }

    public static Button button(String text, String... styles) {
        Button b = new Button(text);
        b.getStyleClass().addAll(styles);
        return b;
    }

    public static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    public static VBox card(Node... children) {
        VBox v = new VBox(10, children);
        v.getStyleClass().add("card");
        return v;
    }

    public static Label badge(Store.Status s) {
        Label l = new Label(s.label);
        l.getStyleClass().addAll("badge", "badge-" + s.name().toLowerCase());
        return l;
    }

    public static void info(String header, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(header);
        a.showAndWait();
    }

    public static void warn(String header, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(header);
        a.showAndWait();
    }

    public static boolean confirm(String header, String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        a.setHeaderText(header);
        return a.showAndWait().filter(b -> b == ButtonType.YES).isPresent();
    }

    /** Thanh tiêu đề phía trên cùng của mỗi vai trò. */
    public static HBox topBar(String user, String roleLabel, Runnable onLogout) {
        Label logo = label("🍜  IU Canteen", "topbar-title");
        Label who = label(user, "topbar-user");
        Label role = label(roleLabel, "topbar-role");
        Button out = button("Đăng xuất", "btn-ghost");
        out.setOnAction(e -> onLogout.run());
        HBox bar = new HBox(12, logo, spacer(), who, role, out);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10, 20, 10, 20));
        bar.getStyleClass().add("topbar");
        return bar;
    }
}
