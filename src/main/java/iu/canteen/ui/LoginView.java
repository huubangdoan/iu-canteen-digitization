package iu.canteen.ui;

import iu.canteen.App;
import iu.canteen.logic.Store;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Màn hình Đăng nhập / Đăng ký (chỉ dành cho IUers: email @iu.edu.vn). */
public class LoginView implements Screen {
    private TabPane tabs;

    @Override
    public Parent build() {
        Label logo = new Label("🍜");
        logo.setStyle("-fx-font-size: 72px;");
        VBox brand = new VBox(14, logo,
                Ui.label("IU Canteen", "brand-title"),
                Ui.label("Đặt món nhanh – không xếp hàng", "brand-sub"),
                Ui.label("MADE BY BRAND", "brand-chip"));
        brand.getStyleClass().add("brand-panel");
        brand.setAlignment(Pos.CENTER);
        brand.setPrefWidth(440);
        brand.setPadding(new Insets(40));

        tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(new Tab("Đăng nhập", loginForm()), new Tab("Đăng ký", registerForm()));

        VBox card = Ui.card(tabs);
        card.setMaxWidth(440);
        card.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        StackPane right = new StackPane(card);
        right.getStyleClass().add("login-bg");
        right.setPadding(new Insets(30));

        BorderPane root = new BorderPane();
        root.setLeft(brand);
        root.setCenter(right);
        return root;
    }

    private Node loginForm() {
        TextField email = new TextField();
        email.setPromptText("email@iu.edu.vn");
        PasswordField pw = new PasswordField();
        pw.setPromptText("Mật khẩu");
        Label err = Ui.label("", "error-text");

        Button btn = Ui.button("Đăng nhập", "btn-primary");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setDefaultButton(true);
        btn.setOnAction(e -> {
            String em = email.getText().trim().toLowerCase();
            if (em.isEmpty() || pw.getText().isEmpty()) {
                err.setText("Vui lòng nhập email và mật khẩu.");
                return;
            }
            if (!em.endsWith("iu.edu.vn")) {
                err.setText("Chỉ IUers (email @iu.edu.vn) mới đăng nhập được.");
                return;
            }
            // TODO: gọi AuthService.login(email, password) và đọc role từ bảng users
            Store.currentUser = em;
            Store.currentRole = em.startsWith("vendor") ? Store.Role.VENDOR
                    : em.startsWith("admin") ? Store.Role.ADMIN : Store.Role.STUDENT;
            App.showMain();
        });

        Label hint = Ui.label("Demo: student@iu.edu.vn • vendor@iu.edu.vn • admin@iu.edu.vn (mật khẩu bất kỳ)", "muted");
        hint.setWrapText(true);

        VBox form = new VBox(8,
                Ui.label("Chào mừng trở lại 👋", "h2"),
                Ui.label("Email", "field-label"), email,
                Ui.label("Mật khẩu", "field-label"), pw,
                err, btn, hint);
        form.setPadding(new Insets(18, 4, 4, 4));
        return form;
    }

    private Node registerForm() {
        TextField name = new TextField();
        name.setPromptText("Nguyễn Văn A");
        TextField email = new TextField();
        email.setPromptText("email@iu.edu.vn");
        TextField id = new TextField();
        id.setPromptText("Mã sinh viên / mã nhân viên");
        ComboBox<String> type = new ComboBox<>();
        type.getItems().addAll("Sinh viên", "Nhân viên");
        type.getSelectionModel().selectFirst();
        type.setMaxWidth(Double.MAX_VALUE);
        PasswordField pw = new PasswordField();
        pw.setPromptText("Mật khẩu (≥ 6 ký tự)");
        PasswordField pw2 = new PasswordField();
        pw2.setPromptText("Nhập lại mật khẩu");
        Label msg = Ui.label("", "error-text");

        Button btn = Ui.button("Tạo tài khoản", "btn-primary");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> {
            msg.getStyleClass().setAll("label", "error-text");
            if (name.getText().isBlank() || email.getText().isBlank() || id.getText().isBlank()) {
                msg.setText("Vui lòng điền đầy đủ thông tin.");
            } else if (!email.getText().trim().toLowerCase().endsWith("iu.edu.vn")) {
                msg.setText("Chỉ chấp nhận email @iu.edu.vn.");
            } else if (pw.getText().length() < 6) {
                msg.setText("Mật khẩu phải có ít nhất 6 ký tự.");
            } else if (!pw.getText().equals(pw2.getText())) {
                msg.setText("Mật khẩu nhập lại không khớp.");
            } else {
                // TODO: AuthService.register(...) – hash mật khẩu rồi lưu vào bảng users
                Ui.info("Đăng ký thành công", "Bạn có thể đăng nhập bằng email vừa tạo.");
                tabs.getSelectionModel().select(0);
            }
        });

        VBox form = new VBox(6,
                Ui.label("Tạo tài khoản IUer", "h2"),
                Ui.label("Họ và tên", "field-label"), name,
                Ui.label("Email trường", "field-label"), email,
                Ui.label("Đối tượng", "field-label"), type,
                Ui.label("Mã số", "field-label"), id,
                Ui.label("Mật khẩu", "field-label"), pw, pw2,
                msg, btn);
        form.setPadding(new Insets(18, 4, 4, 4));
        return form;
    }
}
