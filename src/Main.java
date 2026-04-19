import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Initialize Database Tables
        DatabaseUtil.initializeDatabase();

        // Setup View Manager
        ViewManager.setStage(primaryStage);
        primaryStage.setTitle("Carbon Footprint & Sustainability Tracker");
        
        // Show initial login view
        ViewManager.showLogin();
        
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}