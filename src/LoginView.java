import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Pane;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginView {

    public Pane getView() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));

        VBox glassPane = new VBox(20);
        glassPane.setAlignment(Pos.CENTER);
        glassPane.setMaxWidth(450);
        glassPane.getStyleClass().add("glass-pane");

        Label titleLabel = new Label("Eco-Track");
        titleLabel.getStyleClass().add("label-title");

        Label subLabel = new Label("Sign in to your account");
        subLabel.getStyleClass().add("label-subheader");

        TextField emailField = new TextField();
        emailField.setPromptText("Email Address");
        emailField.getStyleClass().add("text-field-glass");
        emailField.setMaxWidth(350);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.getStyleClass().add("text-field-glass");
        passwordField.setMaxWidth(350);

        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");

        Button loginBtn = new Button("Login");
        loginBtn.getStyleClass().add("button-primary");
        loginBtn.setMaxWidth(350);

        Button registerBtn = new Button("Create an Account");
        registerBtn.getStyleClass().add("button-secondary");
        registerBtn.setMaxWidth(350);

        loginBtn.setOnAction(e -> {
            String email = emailField.getText().trim();
            String pwd = passwordField.getText();
            if (email.isEmpty() || pwd.isEmpty()) {
                errorLabel.setText("Please enter email and password");
                return;
            }
            if (authenticate(email, pwd)) {
                ViewManager.showMainLayout();
            } else {
                errorLabel.setText("Invalid email or password");
            }
        });

        registerBtn.setOnAction(e -> ViewManager.showRegister());

        glassPane.getChildren().addAll(titleLabel, subLabel, emailField, passwordField, loginBtn, registerBtn,
                errorLabel);
        root.getChildren().add(glassPane);
        return root;
    }

    private boolean authenticate(String email, String password) {
        String query = "SELECT user_id, org_id, name, password_hash, account_type, role FROM Users WHERE email = ?";
        try (Connection conn = DatabaseUtil.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String hash = rs.getString("password_hash");
                // Dummy hash for demo matching the one used in register
                if (hash.equals(Integer.toHexString(password.hashCode()))) {
                    Integer orgId = rs.getObject("org_id") != null ? rs.getInt("org_id") : null;
                    SessionManager.setSession(
                            rs.getInt("user_id"),
                            rs.getString("name"),
                            email,
                            orgId,
                            rs.getString("account_type"),
                            rs.getString("role"));
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
