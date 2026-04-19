import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseUtil {

    private static final String URL = "jdbc:mysql://localhost:3306/carbon_tracker";
    private static final String USER = "root";
    private static final String PASSWORD = "Ash@13092005";

    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            System.out.println("✅ Connected to database. Initializing tables...");

            // Create users table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS users (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "username VARCHAR(50) NOT NULL UNIQUE," +
                    "password_hash VARCHAR(255) NOT NULL," +
                    "role VARCHAR(20) DEFAULT 'user'," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")");

            // Create activity_logs table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS activity_logs (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "user_id INT NOT NULL," +
                    "activity_category VARCHAR(50) NOT NULL," +
                    "activity_type VARCHAR(50) NOT NULL," +
                    "value DOUBLE NOT NULL," +
                    "date DATE NOT NULL," +
                    "FOREIGN KEY (user_id) REFERENCES users(id)" +
                    ")");

            // Create emission_factors table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS emission_factors (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "activity_type VARCHAR(50) NOT NULL UNIQUE," +
                    "co2_per_unit DOUBLE NOT NULL" +
                    ")");

            // Insert default emission factors (e.g. per km/kWh)
            stmt.executeUpdate("INSERT IGNORE INTO emission_factors (activity_type, co2_per_unit) VALUES " +
                    "('car_gasoline', 0.192), " +
                    "('bus', 0.089), " +
                    "('electricity_kwh', 0.453), " +
                    "('meat_meal', 2.0)"
            );

            // Create carbon_emissions table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS carbon_emissions (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "user_id INT NOT NULL," +
                    "activity_log_id INT NOT NULL," +
                    "calculated_co2 DOUBLE NOT NULL," +
                    "date DATE NOT NULL," +
                    "FOREIGN KEY (user_id) REFERENCES users(id)," +
                    "FOREIGN KEY (activity_log_id) REFERENCES activity_logs(id)" +
                    ")");

            // Create carbon_offsets table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS carbon_offsets (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "user_id INT NOT NULL," +
                    "offset_activity VARCHAR(100) NOT NULL," +
                    "co2_reduced DOUBLE NOT NULL," +
                    "date DATE NOT NULL," +
                    "FOREIGN KEY (user_id) REFERENCES users(id)" +
                    ")");

            // Create goals_and_rewards table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS goals_and_rewards (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "user_id INT NOT NULL," +
                    "goal_description VARCHAR(255) NOT NULL," +
                    "target_co2 DOUBLE NOT NULL," +
                    "achieved BOOLEAN DEFAULT FALSE," +
                    "points_earned INT DEFAULT 0," +
                    "FOREIGN KEY (user_id) REFERENCES users(id)" +
                    ")");

            System.out.println("✅ Database tables checked/created successfully!");

        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("❌ Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
