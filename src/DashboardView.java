import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

public class DashboardView {

    private Label totalEmissionLabel;
    private VBox historyBox;

    public Scene getView() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        // Header
        HBox header = new HBox();
        header.setPadding(new Insets(20));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #1e1e1e; -fx-border-color: #333; -fx-border-width: 0 0 1 0;");

        Label welcomeLabel = new Label("Welcome, " + SessionManager.getUsername());
        welcomeLabel.getStyleClass().add("label-header");
        welcomeLabel.setStyle("-fx-font-size: 20px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button logoutBtn = new Button("Logout");
        logoutBtn.getStyleClass().add("button-neon");
        logoutBtn.setOnAction(e -> {
            SessionManager.clearSession();
            ViewManager.showLogin();
        });

        header.getChildren().addAll(welcomeLabel, spacer, logoutBtn);
        root.setTop(header);

        // Center Output Section
        VBox centerPane = new VBox(20);
        centerPane.setPadding(new Insets(30));
        centerPane.setAlignment(Pos.TOP_CENTER);

        VBox statsPane = new VBox(10);
        statsPane.getStyleClass().add("glass-pane");
        Label summaryTitle = new Label("Your Carbon Footprint");
        summaryTitle.getStyleClass().add("label-header");
        summaryTitle.setStyle("-fx-font-size: 22px;");

        totalEmissionLabel = new Label("Total Emissions: 0.0 kg CO2");
        totalEmissionLabel.getStyleClass().add("label-normal");
        totalEmissionLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #ffaa00;");

        statsPane.getChildren().addAll(summaryTitle, totalEmissionLabel);

        Label historyTitle = new Label("Recent Activities");
        historyTitle.getStyleClass().add("label-header");
        historyTitle.setStyle("-fx-font-size: 18px; -fx-padding: 20 0 0 0;");

        historyBox = new VBox(5);
        historyBox.setAlignment(Pos.TOP_LEFT);
        
        ScrollPane scrollPane = new ScrollPane(historyBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setPrefHeight(200);

        centerPane.getChildren().addAll(statsPane, historyTitle, scrollPane);
        root.setCenter(centerPane);

        // Right Input Section
        VBox rightPane = new VBox(15);
        rightPane.setPadding(new Insets(30));
        rightPane.setMaxWidth(300);
        rightPane.getStyleClass().add("glass-pane");
        rightPane.setAlignment(Pos.TOP_CENTER);

        Label logTitle = new Label("Log Activity");
        logTitle.getStyleClass().add("label-header");
        logTitle.setStyle("-fx-font-size: 20px;");

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("car_gasoline", "bus", "electricity_kwh", "meat_meal");
        typeCombo.setPromptText("Activity Type");
        typeCombo.setStyle("-fx-background-color: #1e1e1e; -fx-text-fill: white;");

        TextField valueField = new TextField();
        valueField.setPromptText("Amount (km, kWh, meals)");
        valueField.getStyleClass().add("text-field-dark");

        Button submitBtn = new Button("Log Emission");
        submitBtn.getStyleClass().add("button-neon");
        
        Label msgLabel = new Label();

        submitBtn.setOnAction(e -> {
            String type = typeCombo.getValue();
            String valStr = valueField.getText();
            if (type == null || valStr.isEmpty()) {
                msgLabel.setText("Fill all fields");
                msgLabel.setStyle("-fx-text-fill: red;");
                return;
            }
            try {
                double value = Double.parseDouble(valStr);
                logActivity(type, value);
                msgLabel.setText("Logged successfully!");
                msgLabel.setStyle("-fx-text-fill: #39ff14;");
                valueField.clear();
                refreshData();
            } catch (NumberFormatException ex) {
                msgLabel.setText("Invalid amount");
                msgLabel.setStyle("-fx-text-fill: red;");
            }
        });

        rightPane.getChildren().addAll(logTitle, typeCombo, valueField, submitBtn, msgLabel);
        root.setRight(rightPane);

        refreshData(); // Initial load

        return new Scene(root, 900, 600);
    }

    private void logActivity(String type, double value) {
        // Calculate emission
        double factor = 0;
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT co2_per_unit FROM emission_factors WHERE activity_type = ?")) {
            stmt.setString(1, type);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) factor = rs.getDouble(1);
        } catch (Exception e) { e.printStackTrace(); }

        double calcCo2 = value * factor;
        String date = LocalDate.now().toString();

        // Insert into activity_logs
        try (Connection conn = DatabaseUtil.getConnection()) {
            conn.setAutoCommit(false);

            PreparedStatement logStmt = conn.prepareStatement(
                    "INSERT INTO activity_logs (user_id, activity_category, activity_type, value, date) VALUES (?, ?, ?, ?, ?)",
                    java.sql.Statement.RETURN_GENERATED_KEYS);
            logStmt.setInt(1, SessionManager.getUserId());
            logStmt.setString(2, "General");
            logStmt.setString(3, type);
            logStmt.setDouble(4, value);
            logStmt.setString(5, date);
            logStmt.executeUpdate();
            
            ResultSet keys = logStmt.getGeneratedKeys();
            if (keys.next()) {
                int logId = keys.getInt(1);
                PreparedStatement emitStmt = conn.prepareStatement(
                        "INSERT INTO carbon_emissions (user_id, activity_log_id, calculated_co2, date) VALUES (?, ?, ?, ?)");
                emitStmt.setInt(1, SessionManager.getUserId());
                emitStmt.setInt(2, logId);
                emitStmt.setDouble(3, calcCo2);
                emitStmt.setString(4, date);
                emitStmt.executeUpdate();
            }
            conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void refreshData() {
        double total = 0;
        historyBox.getChildren().clear();

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT a.activity_type, a.value, a.date, c.calculated_co2 " +
                     "FROM activity_logs a JOIN carbon_emissions c ON a.id = c.activity_log_id " +
                     "WHERE a.user_id = ? ORDER BY a.id DESC")) {
            stmt.setInt(1, SessionManager.getUserId());
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                String type = rs.getString("activity_type");
                double val = rs.getDouble("value");
                String date = rs.getString("date");
                double co2 = rs.getDouble("calculated_co2");
                total += co2;

                Label row = new Label(String.format("[%s] %s: %.1f units => +%.2f kg CO2", date, type, val, co2));
                row.getStyleClass().add("label-normal");
                historyBox.getChildren().add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        totalEmissionLabel.setText(String.format("Total Emissions: %.2f kg CO2", total));
    }
}
