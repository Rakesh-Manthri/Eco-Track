import javafx.stage.Stage;
import javafx.scene.Scene;

public class ViewManager {
    private static Stage primaryStage;

    public static void setStage(Stage stage) {
        primaryStage = stage;
    }

    public static void showLogin() {
        Scene scene = new LoginView().getView();
        scene.getStylesheets().add(ViewManager.class.getResource("style.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
    }

    public static void showDashboard() {
        Scene scene = new DashboardView().getView();
        scene.getStylesheets().add(ViewManager.class.getResource("style.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
    }
}
