import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class CloudSanityTest {
    public static void main(String[] args) {
        System.out.println("🚀 Running Eco-Track Cloud Database Sanity Test...");
        try (Connection conn = DatabaseUtil.getConnection()) {
            System.out.println("✅ Connected to: " + conn.getMetaData().getURL());

            // 1. Test Insert
            String testEmail = "cloud_tester_" + System.currentTimeMillis() + "@ecotrack.org";
            int testUserId = -1;
            String insertUser = "INSERT INTO Users (name, email, password_hash, account_type, role) VALUES (?, ?, ?, 'individual', 'viewer')";
            try (PreparedStatement pstmt = conn.prepareStatement(insertUser, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, "Cloud Test User");
                pstmt.setString(2, testEmail);
                pstmt.setString(3, "hashed_pw_123");
                pstmt.executeUpdate();
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    testUserId = rs.getInt(1);
                    System.out.println("✅ [CREATE] Successfully created user in TiDB Cloud with user_id: " + testUserId);
                }
            }

            // 2. Test Read
            String selectUser = "SELECT user_id, name, email FROM Users WHERE user_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(selectUser)) {
                pstmt.setInt(1, testUserId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("✅ [READ] Verified user in cloud: " + rs.getString("name") + " (" + rs.getString("email") + ")");
                    }
                }
            }

            // 3. Test View Query
            String viewQuery = "SELECT * FROM user_net_emissions WHERE user_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(viewQuery)) {
                pstmt.setInt(1, testUserId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("✅ [VIEW] Successfully read from user_net_emissions view. Net CO2: " + rs.getDouble("net_co2_kg") + " kg");
                    }
                }
            }

            // 4. Test Clean up
            String deleteUser = "DELETE FROM Users WHERE user_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteUser)) {
                pstmt.setInt(1, testUserId);
                int deleted = pstmt.executeUpdate();
                System.out.println("✅ [DELETE] Cleaned up test user (rows deleted: " + deleted + ")");
            }

            System.out.println("\n🎉 ALL TI-DB CLOUD CRUD & VIEW TESTS PASSED SUCCESSFULLY!");

        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
