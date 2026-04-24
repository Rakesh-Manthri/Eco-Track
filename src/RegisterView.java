import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Pane;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

public class RegisterView {

    // Map: org name → org_id (for dropdown)
    private final Map<String, Integer> orgMap = new HashMap<>();

    public Pane getView() {
        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));

        VBox glassPane = new VBox(15);
        glassPane.setAlignment(Pos.CENTER);
        glassPane.setMaxWidth(450);
        glassPane.getStyleClass().add("glass-pane");

        Label titleLabel = new Label("Create Account");
        titleLabel.getStyleClass().add("label-title");
        
        Label subLabel = new Label("Join Eco-Track to track your footprint");
        subLabel.getStyleClass().add("label-subheader");

        TextField nameField = new TextField();
        nameField.setPromptText("Full Name");
        nameField.getStyleClass().add("text-field-glass");
        nameField.setMaxWidth(350);

        TextField emailField = new TextField();
        emailField.setPromptText("Email Address");
        emailField.getStyleClass().add("text-field-glass");
        emailField.setMaxWidth(350);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.getStyleClass().add("text-field-glass");
        passwordField.setMaxWidth(350);

        // ── Account Type ─────────────────────────────────────────────────────
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("individual", "office");
        typeCombo.setPromptText("Account Type");
        typeCombo.getStyleClass().add("combo-box-glass");
        typeCombo.setMaxWidth(350);

        // ── Role (visible only for office) ───────────────────────────────────
        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("admin", "employee");
        roleCombo.setPromptText("Select Role");
        roleCombo.getStyleClass().add("combo-box-glass");
        roleCombo.setMaxWidth(350);
        roleCombo.setVisible(false);
        roleCombo.setManaged(false);

        // ── Organization (visible only for employee) ─────────────────────────
        Label orgLabel = new Label("Select Your Organization");
        orgLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #374151;");
        orgLabel.setVisible(false);
        orgLabel.setManaged(false);

        ComboBox<String> orgCombo = new ComboBox<>();
        orgCombo.setPromptText("Choose Organization");
        orgCombo.getStyleClass().add("combo-box-glass");
        orgCombo.setMaxWidth(350);
        orgCombo.setVisible(false);
        orgCombo.setManaged(false);

        // Load organizations lazily when employee is selected
        roleCombo.setOnAction(e -> {
            boolean isEmployee = "employee".equals(roleCombo.getValue());
            orgLabel.setVisible(isEmployee);
            orgLabel.setManaged(isEmployee);
            orgCombo.setVisible(isEmployee);
            orgCombo.setManaged(isEmployee);
            if (isEmployee && orgCombo.getItems().isEmpty()) {
                loadOrganizations(orgCombo);
            }
        });

        typeCombo.setOnAction(e -> {
            boolean isOffice = "office".equals(typeCombo.getValue());
            roleCombo.setVisible(isOffice);
            roleCombo.setManaged(isOffice);
            if (!isOffice) {
                // Hide dependent fields if switching back to individual
                orgLabel.setVisible(false);
                orgLabel.setManaged(false);
                orgCombo.setVisible(false);
                orgCombo.setManaged(false);
                roleCombo.setValue(null);
            }
        });

        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");

        Button registerBtn = new Button("Create Account");
        registerBtn.getStyleClass().add("button-primary");
        registerBtn.setMaxWidth(350);

        Button backBtn = new Button("Back to Login");
        backBtn.getStyleClass().add("button-secondary");
        backBtn.setMaxWidth(350);

        registerBtn.setOnAction(e -> {
            String name     = nameField.getText().trim();
            String email    = emailField.getText().trim();
            String pwd      = passwordField.getText();
            String accType  = typeCombo.getValue();
            String role     = null;
            Integer orgId   = null;

            if (name.isEmpty() || email.isEmpty() || pwd.isEmpty() || accType == null) {
                errorLabel.setText("Please fill all required fields");
                return;
            }

            if ("office".equals(accType)) {
                role = roleCombo.getValue();
                if (role == null) {
                    errorLabel.setText("Please select a role");
                    return;
                }
                if ("employee".equals(role)) {
                    String selectedOrg = orgCombo.getValue();
                    if (selectedOrg == null) {
                        errorLabel.setText("Please select your organization");
                        return;
                    }
                    orgId = orgMap.get(selectedOrg);
                }
            }

            if (registerUser(name, email, pwd, accType, role, orgId)) {
                ViewManager.showLogin();
            } else {
                errorLabel.setText("Registration failed. Email may already exist.");
            }
        });

        backBtn.setOnAction(e -> ViewManager.showLogin());

        glassPane.getChildren().addAll(
            titleLabel, subLabel,
            nameField, emailField, passwordField,
            typeCombo, roleCombo,
            orgLabel, orgCombo,
            registerBtn, backBtn, errorLabel
        );
        root.getChildren().add(glassPane);
        return root;
    }

    private void loadOrganizations(ComboBox<String> orgCombo) {
        String query = "SELECT org_id, org_name FROM Organizations ORDER BY org_name";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String orgName = rs.getString("org_name");
                int orgId = rs.getInt("org_id");
                orgMap.put(orgName, orgId);
                orgCombo.getItems().add(orgName);
            }
            if (orgCombo.getItems().isEmpty()) {
                orgCombo.setPromptText("No organizations found");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean registerUser(String name, String email, String password,
                                  String accType, String role, Integer orgId) {
        String query = "INSERT INTO Users (name, email, password_hash, account_type, role, org_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, name);
            stmt.setString(2, email);
            stmt.setString(3, Integer.toHexString(password.hashCode()));
            stmt.setString(4, accType);
            // individual → role is NULL; office → admin/employee
            if (role != null) stmt.setString(5, role);
            else              stmt.setNull(5, java.sql.Types.VARCHAR);
            // employee selects org; others get NULL
            if (orgId != null) stmt.setInt(6, orgId);
            else               stmt.setNull(6, java.sql.Types.INTEGER);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
