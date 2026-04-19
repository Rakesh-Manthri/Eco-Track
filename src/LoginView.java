import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginView {

    public Scene getView() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        root.getStyleClass().add("root");

        VBox glassPane = new VBox(15);
        glassPane.setAlignment(Pos.CENTER);
        glassPane.setMaxWidth(400);
        glassPane.getStyleClass().add("glass-pane");

        Label titleLabel = new Label("Carbon Tracker");
        titleLabel.getStyleClass().add("label-header");
        
        Label subLabel = new Label("Login or Register to track emissions");
        subLabel.getStyleClass().add("label-subheader");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.getStyleClass().add("text-field-dark");
        usernameField.setMaxWidth(300);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.getStyleClass().add("text-field-dark");
        passwordField.setMaxWidth(300);

        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #ff3333;");

        Button loginBtn = new Button("Login");
        loginBtn.getStyleClass().add("button-neon");
        loginBtn.setMaxWidth(300);

        Button registerBtn = new Button("Register");
        registerBtn.getStyleClass().add("button-neon");
        registerBtn.setMaxWidth(300);

        loginBtn.setOnAction(e -> {
            String uname = usernameField.getText().trim();
            String pwd = passwordField.getText();
            if (uname.isEmpty() || pwd.isEmpty()) {
                errorLabel.setText("Please enter username and password");
                return;
            }
            int userId = authenticate(uname, pwd);
            if (userId != -1) {
                SessionManager.setSession(userId, uname);
                ViewManager.showDashboard();
            } else {
                errorLabel.setText("Invalid username or password");
            }
        });

        registerBtn.setOnAction(e -> {
            String uname = usernameField.getText().trim();
            String pwd = passwordField.getText();
            if (uname.isEmpty() || pwd.isEmpty()) {
                errorLabel.setText("Please enter username and password");
                return;
            }
            boolean success = register(uname, pwd);
            if (success) {
                errorLabel.setStyle("-fx-text-fill: #39ff14;");
                errorLabel.setText("Registration successful! Please login.");
            } else {
                errorLabel.setStyle("-fx-text-fill: #ff3333;");
                errorLabel.setText("Username already exists or error occurred.");
            }
        });

        glassPane.getChildren().addAll(titleLabel, subLabel, usernameField, passwordField, loginBtn, registerBtn, errorLabel);
        root.getChildren().add(glassPane);

        return new Scene(root, 600, 500);
    }

    private int authenticate(String username, String password) {
        String query = "SELECT id, password_hash FROM users WHERE username = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String hash = rs.getString("password_hash");
                // Simple string equals for demo
                if (hash.equals(hashPassword(password))) {
                    return rs.getInt("id");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private boolean register(String username, String password) {
        String query = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, hashPassword(password));
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private String hashPassword(String password) {
        // Dummy hash for demo. In production use BCrypt.
        return Integer.toHexString(password.hashCode());
    }
}
