package iu.canteen;

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
        stage = primaryStage;
        stage.setTitle("IU Canteen");
        stage.setMinWidth(1050);
        stage.setMinHeight(700);
        showLogin();
        stage.show();
    }

    public static void showLogin() {
        setRoot(new LoginView().build());
    }

    /** Điều hướng theo vai trò: STUDENT -> giao diện đặt món, VENDOR/ADMIN -> giao diện quản lý. */
    public static void showMain() {
        Screen screen = Store.currentRole == Store.Role.STUDENT
                ? new StudentView()
                : new VendorView();
        setRoot(screen.build());
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
