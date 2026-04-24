import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActivityLogger {

    private Map<String, List<String>> categoryToTypesMap = new HashMap<>();
    private Map<String, ActivityTypeInfo> typeInfoMap = new HashMap<>();

    public Pane getView() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(50, 80, 50, 80));

        VBox glassPane = new VBox(20);
        glassPane.setAlignment(Pos.CENTER);
        glassPane.setMaxWidth(500);
        glassPane.getStyleClass().add("glass-pane");

        Label titleLabel = new Label("Log New Activity");
        titleLabel.getStyleClass().add("label-title");
        
        Label subLabel = new Label("Record your actions to calculate emissions");
        subLabel.getStyleClass().add("label-subheader");

        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.setPromptText("Select Category");
        categoryCombo.getStyleClass().add("combo-box-glass");
        categoryCombo.setMaxWidth(400);

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.setPromptText("Select Activity Type");
        typeCombo.getStyleClass().add("combo-box-glass");
        typeCombo.setMaxWidth(400);
        typeCombo.setDisable(true);
        
        loadLookupData(categoryCombo);

        TextField quantityField = new TextField();
        quantityField.setPromptText("Quantity");
        quantityField.getStyleClass().add("text-field-glass");
        quantityField.setMaxWidth(400);

        Label unitLabel = new Label("Unit: -");
        unitLabel.getStyleClass().add("label-normal");
        
        Label factorLabel = new Label("Emission Factor: -");
        factorLabel.getStyleClass().add("label-subheader");

        categoryCombo.setOnAction(e -> {
            String selectedCat = categoryCombo.getValue();
            if (selectedCat != null) {
                typeCombo.getItems().clear();
                typeCombo.getItems().addAll(categoryToTypesMap.getOrDefault(selectedCat, new ArrayList<>()));
                typeCombo.setDisable(false);
                unitLabel.setText("Unit: -");
                factorLabel.setText("Emission Factor: -");
            }
        });

        typeCombo.setOnAction(e -> {
            String selectedType = typeCombo.getValue();
            if (selectedType != null && typeInfoMap.containsKey(selectedType)) {
                ActivityTypeInfo info = typeInfoMap.get(selectedType);
                unitLabel.setText("Unit: " + info.unit);
                factorLabel.setText(String.format("Emission Factor: %.4f kg CO2 / %s", info.co2PerUnit, info.unit));
            }
        });

        Label dateLabel = new Label("Activity Date");
        dateLabel.getStyleClass().add("label-normal");
        dateLabel.setStyle("-fx-font-weight: bold;");

        DatePicker datePicker = new DatePicker(java.time.LocalDate.now());
        datePicker.setPromptText("Select Date");
        datePicker.setStyle("-fx-font-size: 14px;");
        datePicker.setMaxWidth(400);

        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");

        Button submitBtn = new Button("Calculate & Log");
        submitBtn.getStyleClass().add("button-primary");
        submitBtn.setMaxWidth(400);

        submitBtn.setOnAction(e -> {
            String selectedType = typeCombo.getValue();
            String qtyStr = quantityField.getText().trim();
            
            if (selectedType == null || qtyStr.isEmpty()) {
                errorLabel.setText("Please select a type and enter quantity");
                errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                return;
            }

            if (datePicker.getValue() == null) {
                errorLabel.setText("Please select a date");
                errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                return;
            }

            try {
                double quantity = Double.parseDouble(qtyStr);
                if (quantity <= 0) {
                    errorLabel.setText("Quantity must be positive");
                    return;
                }
                
                logActivity(typeInfoMap.get(selectedType), quantity, datePicker.getValue().toString());

                // Check achievements on a background thread
                new Thread(() -> {
                    java.util.List<String> earned = AchievementService.checkAndGrantRewards();
                    javafx.application.Platform.runLater(() -> {
                        if (!earned.isEmpty()) {
                            errorLabel.setText("🏅 New badge(s) earned: " + String.join(", ", earned));
                            errorLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                        } else {
                            errorLabel.setText("Activity logged successfully!");
                            errorLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                        }
                    });
                }).start();

                quantityField.clear();
                
            } catch (NumberFormatException ex) {
                errorLabel.setText("Invalid quantity number");
                errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            }
        });

        glassPane.getChildren().addAll(titleLabel, subLabel, categoryCombo, typeCombo, quantityField, unitLabel, factorLabel, dateLabel, datePicker, submitBtn, errorLabel);
        root.getChildren().add(glassPane);
        return root;
    }

    private void loadLookupData(ComboBox<String> categoryCombo) {
        String query = "SELECT ac.category_name, at.activity_type_id, at.name, at.default_unit, at.typical_emission_factor_id, ef.co2_per_unit " +
                       "FROM Activity_Categories ac " +
                       "JOIN Activity_Types at ON ac.category_id = at.category_id " +
                       "LEFT JOIN Emission_Factors ef ON at.typical_emission_factor_id = ef.factor_id";
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                String catName = rs.getString("category_name");
                String typeName = rs.getString("name");
                
                categoryToTypesMap.putIfAbsent(catName, new ArrayList<>());
                categoryToTypesMap.get(catName).add(typeName);
                
                if (!categoryCombo.getItems().contains(catName)) {
                    categoryCombo.getItems().add(catName);
                }

                ActivityTypeInfo info = new ActivityTypeInfo(
                    rs.getInt("activity_type_id"),
                    rs.getString("default_unit"),
                    rs.getInt("typical_emission_factor_id"),
                    rs.getDouble("co2_per_unit")
                );
                typeInfoMap.put(typeName, info);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getUserRewardCount() {
        String query = "SELECT COUNT(*) FROM User_Rewards WHERE user_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, SessionManager.getUserId());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private void logActivity(ActivityTypeInfo info, double quantity, String date) {
        double co2Result = quantity * info.co2PerUnit;

        String insertActivity = "INSERT INTO Activities (user_id, activity_type_id, quantity, unit, activity_date) VALUES (?, ?, ?, ?, ?)";
        String insertCalc = "INSERT INTO Emission_Calculations (activity_id, factor_id, quantity, co2_result) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DatabaseUtil.getConnection()) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement astmt = conn.prepareStatement(insertActivity, Statement.RETURN_GENERATED_KEYS)) {
                astmt.setInt(1, SessionManager.getUserId());
                astmt.setInt(2, info.typeId);
                astmt.setDouble(3, quantity);
                astmt.setString(4, info.unit);
                astmt.setString(5, date);
                astmt.executeUpdate();
                
                ResultSet rs = astmt.getGeneratedKeys();
                if (rs.next() && info.factorId != 0) {
                    int activityId = rs.getInt(1);
                    try (PreparedStatement cstmt = conn.prepareStatement(insertCalc)) {
                        cstmt.setInt(1, activityId);
                        cstmt.setInt(2, info.factorId);
                        cstmt.setDouble(3, quantity);
                        cstmt.setDouble(4, co2Result);
                        cstmt.executeUpdate();
                    }
                }
            }
            conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class ActivityTypeInfo {
        int typeId;
        String unit;
        int factorId;
        double co2PerUnit;

        ActivityTypeInfo(int typeId, String unit, int factorId, double co2PerUnit) {
            this.typeId = typeId;
            this.unit = unit;
            this.factorId = factorId;
            this.co2PerUnit = co2PerUnit;
        }
    }
}
