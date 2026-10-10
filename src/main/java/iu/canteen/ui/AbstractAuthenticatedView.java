package iu.canteen.ui;

import iu.canteen.App;
import iu.canteen.logic.Store;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;

/** Template for authenticated screens with a shared role header and logout flow. */
public abstract class AbstractAuthenticatedView implements Screen {
    protected abstract String roleLabel();

    protected abstract Parent buildContent();

    protected void onLogout() {}

    @Override
    public final Parent build() {
        BorderPane root = new BorderPane();
        root.setTop(Ui.topBar(Store.currentUser, roleLabel(), () -> {
            onLogout();
            App.showLogin();
        }));
        root.setCenter(buildContent());
        return root;
    }
}
