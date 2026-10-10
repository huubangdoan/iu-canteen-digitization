package iu.canteen;

import iu.canteen.db.Database;
import iu.canteen.logic.Store;
import iu.canteen.ui.LoginView;
import iu.canteen.ui.Screen;
import iu.canteen.ui.StudentView;
import iu.canteen.ui.VendorView;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
    private static Stage stage;
    private static Scene scene;

    @Override
    public void start(Stage primaryStage) {
        // Khởi tạo cơ sở dữ liệu SQLite & dữ liệu mẫu
        Database.init();
        new iu.canteen.service.SchedulerService().start();

        stage = primaryStage;
        stage.setTitle("IU Canteen – Digitization & Smart Forecast");
        stage.setMinWidth(1050);
        stage.setMinHeight(700);
        showLogin();
        stage.show();
    }

    public static void showLogin() {
        setRoot(new LoginView().build());
    }

    /** Điều hướng theo vai trò: STUDENT -> đặt món, VENDOR -> quản lý quán, ADMIN -> dashboard quản trị toàn trường. */
    public static void showMain() {
        Screen screen = switch (Store.currentRole) {
            case STUDENT -> new StudentView();
            case ADMIN -> new iu.canteen.ui.AdminDashboardView();
            case VENDOR -> new VendorView();
        };
        setRoot(screen.build());
    }

    @Override
    public void stop() {
        new iu.canteen.service.SchedulerService().stop();
    }

    private static void setRoot(Parent root) {
        if (scene == null) {
            scene = new Scene(root, 1240, 780);
            scene.getStylesheets().add(App.class.getResource("/style.css").toExternalForm());
            stage.setScene(scene);
        } else {
            scene.setRoot(root);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
