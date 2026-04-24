import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

public class GoalsView {

    private VBox goalsList;

    public Pane getView() {
        VBox root = new VBox(30);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(50, 80, 30, 80));

        // ── Form Card (matches ActivityLogger style) ──────────────────────────
        VBox formCard = new VBox(15);
        formCard.setAlignment(Pos.CENTER);
        formCard.setMaxWidth(500);
        formCard.getStyleClass().add("glass-pane");

        Label pageTitle = new Label("My Sustainability Goals");
        pageTitle.getStyleClass().add("label-title");
        
        Label subTitle = new Label("Set limits on your carbon footprint");
        subTitle.getStyleClass().add("label-subheader");

        TextField targetField = new TextField();
        targetField.setPromptText("Target CO2 (kg)");
        targetField.getStyleClass().add("text-field-glass");
        targetField.setMaxWidth(400);

        ComboBox<String> periodCombo = new ComboBox<>();
        periodCombo.getItems().addAll("daily", "weekly", "monthly");
        periodCombo.setValue("monthly");
        periodCombo.getStyleClass().add("combo-box-glass");
        periodCombo.setMaxWidth(400);

        Label startLbl = new Label("Start Date");
        startLbl.getStyleClass().add("label-normal");
        startLbl.setStyle("-fx-font-weight: bold;");
        DatePicker startDatePicker = new DatePicker(LocalDate.now());
        startDatePicker.setStyle("-fx-font-size: 14px;");
        startDatePicker.setMaxWidth(400);

        Label endLbl = new Label("End Date");
        endLbl.getStyleClass().add("label-normal");
        endLbl.setStyle("-fx-font-weight: bold;");
        DatePicker endDatePicker = new DatePicker(LocalDate.now().plusMonths(1));
        endDatePicker.setStyle("-fx-font-size: 14px;");
        endDatePicker.setMaxWidth(400);

        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");

        Button submitBtn = new Button("Set Goal");
        submitBtn.getStyleClass().add("button-primary");
        submitBtn.setMaxWidth(400);

        submitBtn.setOnAction(e -> {
            try {
                double target = Double.parseDouble(targetField.getText());
                String period = periodCombo.getValue();
                LocalDate start = startDatePicker.getValue();
                LocalDate end = endDatePicker.getValue();

                if (target <= 0 || start == null || end == null || end.isBefore(start) || end.isEqual(start)) {
                    errorLabel.setText("Invalid input or dates");
                    return;
                }

                saveGoal(target, period, start.toString(), end.toString());
                targetField.clear();
                errorLabel.setText("");
                loadGoals();
            } catch (NumberFormatException ex) {
                errorLabel.setText("Invalid target number");
            }
        });

        formCard.getChildren().addAll(pageTitle, subTitle, targetField, periodCombo, startLbl, startDatePicker, endLbl, endDatePicker, submitBtn, errorLabel);
        root.getChildren().add(formCard);

        // ── Goals List ────────────────────────────────────────────────────────
        Label listTitle = new Label("Active Goals");
        listTitle.getStyleClass().add("label-header");

        goalsList = new VBox(15);
        ScrollPane scroll = new ScrollPane(goalsList);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox listSection = new VBox(15);
        listSection.setPadding(new Insets(20, 80, 20, 80));
        listSection.getChildren().addAll(listTitle, scroll);

        loadGoals();

        root.getChildren().add(listSection);
        return root;
    }

    private void saveGoal(double target, String period, String start, String end) {
        String query = "INSERT INTO Goals (user_id, target_co2_kg, period_type, start_date, end_date) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, SessionManager.getUserId());
            stmt.setDouble(2, target);
            stmt.setString(3, period);
            stmt.setString(4, start);
            stmt.setString(5, end);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadGoals() {
        goalsList.getChildren().clear();
        String query = "SELECT goal_id, target_co2_kg, period_type, start_date, end_date, status " +
                       "FROM Goals WHERE user_id = ? ORDER BY end_date ASC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, SessionManager.getUserId());
            ResultSet rs = stmt.executeQuery();
            
            boolean hasGoals = false;
            while (rs.next()) {
                hasGoals = true;
                int goalId = rs.getInt("goal_id");
                double target = rs.getDouble("target_co2_kg");
                String period = rs.getString("period_type");
                String start = rs.getString("start_date");
                String end = rs.getString("end_date");
                String status = rs.getString("status");

                HBox row = new HBox(15);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(15));
                row.getStyleClass().add("glass-pane");
                
                VBox textInfo = new VBox(5);
                Label goalLabel = new Label(String.format("Target: %.2f kg CO2 (%s)", target, period.toUpperCase()));
                goalLabel.getStyleClass().add("label-header");
                goalLabel.setStyle("-fx-font-size: 16px;");

                String statusColor = status.equals("active") ? "#3498db" : (status.equals("completed") ? "#2ecc71" : "#e74c3c");
                Label statusLabel = new Label(String.format("Status: %s | %s to %s", status.toUpperCase(), start, end));
                statusLabel.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-weight: bold;");

                textInfo.getChildren().addAll(goalLabel, statusLabel);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Button delBtn = new Button("Remove");
                delBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e74c3c; -fx-border-color: #e74c3c; -fx-border-radius: 5px; -fx-cursor: hand;");
                delBtn.setOnAction(e -> deleteGoal(goalId));

                row.getChildren().addAll(textInfo, spacer, delBtn);
                goalsList.getChildren().add(row);
            }
            if (!hasGoals) {
                Label emptyLabel = new Label("No active goals currently set. Create one above!");
                emptyLabel.getStyleClass().add("label-normal");
                goalsList.getChildren().add(emptyLabel);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteGoal(int goalId) {
        String query = "DELETE FROM Goals WHERE goal_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, goalId);
            stmt.executeUpdate();
            loadGoals();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
