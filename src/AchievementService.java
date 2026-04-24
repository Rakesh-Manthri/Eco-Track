import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * AchievementService
 * Evaluates all reward conditions and grants any newly unlocked rewards.
 *
 * Supported condition types:
 *   - below_threshold : monthly gross CO2 < condition_value
 *   - total_saved     : cumulative CO2 offset_kg >= condition_value
 *   - streak_days     : consecutive calendar days with activity < daily goal
 */
public class AchievementService {

    /**
     * Check all unearned rewards for the current user and grant any that are now satisfied.
     * @return list of newly earned reward names (empty if none)
     */
    public static List<String> checkAndGrantRewards() {
        List<String> newlyEarned = new ArrayList<>();
        int userId = SessionManager.getUserId();

        String query =
            "SELECT r.reward_id, r.reward_name, r.condition_type, r.condition_value " +
            "FROM Rewards r " +
            "WHERE r.reward_id NOT IN " +
            "  (SELECT reward_id FROM User_Rewards WHERE user_id = ?)";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int    rewardId    = rs.getInt("reward_id");
                String rewardName  = rs.getString("reward_name");
                String condType    = rs.getString("condition_type");
                double condValue   = rs.getDouble("condition_value");

                boolean earned = false;

                switch (condType) {
                    case "below_threshold":
                        earned = checkBelowThreshold(conn, userId, condValue);
                        break;
                    case "total_saved":
                        earned = checkTotalSaved(conn, userId, condValue);
                        break;
                    case "streak_days":
                        earned = checkStreakDays(conn, userId, (int) condValue);
                        break;
                }

                if (earned) {
                    grantReward(conn, userId, rewardId, condType);
                    newlyEarned.add(rewardName);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return newlyEarned;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Condition: Monthly gross CO2 < threshold
    // ─────────────────────────────────────────────────────────────────────────
    private static boolean checkBelowThreshold(Connection conn, int userId, double threshold) {
        String sql =
            "SELECT COALESCE(SUM(ec.co2_result), 0) AS monthly_co2 " +
            "FROM Emission_Calculations ec " +
            "JOIN Activities a ON ec.activity_id = a.activity_id " +
            "WHERE a.user_id = ? " +
            "  AND MONTH(a.activity_date) = MONTH(CURDATE()) " +
            "  AND YEAR(a.activity_date)  = YEAR(CURDATE())";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                double monthly = rs.getDouble("monthly_co2");
                // Only award if user has logged at least one activity this month
                return monthly > 0 && monthly < threshold;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Condition: Cumulative offsets >= condition_value (kg)
    // ─────────────────────────────────────────────────────────────────────────
    private static boolean checkTotalSaved(Connection conn, int userId, double target) {
        // Special case: "First Step" (target=1) = user has logged at least 1 activity
        if (target <= 1.0) {
            String sql = "SELECT COUNT(*) FROM Activities WHERE user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) return rs.getInt(1) >= 1;
            } catch (Exception e) { e.printStackTrace(); }
            return false;
        }

        String sql = "SELECT COALESCE(SUM(co2_offset_kg), 0) AS total FROM Offsets WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getDouble("total") >= target;
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Condition: Consecutive days where daily emissions < active daily goal
    // ─────────────────────────────────────────────────────────────────────────
    private static boolean checkStreakDays(Connection conn, int userId, int requiredDays) {
        // Fetch the user's active daily goal target
        double dailyTarget = 0;
        String goalSql =
            "SELECT target_co2_kg FROM Goals " +
            "WHERE user_id = ? AND status = 'active' AND period_type = 'daily' " +
            "ORDER BY start_date DESC LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(goalSql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) dailyTarget = rs.getDouble("target_co2_kg");
            else return false; // No daily goal = can't have a streak
        } catch (Exception e) { e.printStackTrace(); return false; }

        // Get per-day totals ordered descending (most recent first)
        String daysSql =
            "SELECT a.activity_date, SUM(ec.co2_result) AS daily_co2 " +
            "FROM Activities a " +
            "JOIN Emission_Calculations ec ON a.activity_id = ec.activity_id " +
            "WHERE a.user_id = ? " +
            "GROUP BY a.activity_date " +
            "ORDER BY a.activity_date DESC";

        try (PreparedStatement stmt = conn.prepareStatement(daysSql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            int streak = 0;
            java.time.LocalDate expected = java.time.LocalDate.now();

            while (rs.next()) {
                java.time.LocalDate day = rs.getDate("activity_date").toLocalDate();
                double dailyCo2 = rs.getDouble("daily_co2");

                // Streak must be on consecutive calendar days
                if (!day.equals(expected)) break;
                if (dailyCo2 >= dailyTarget) break; // day exceeded target → streak broken

                streak++;
                expected = expected.minusDays(1);

                if (streak >= requiredDays) return true;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Grant the reward to the user
    // ─────────────────────────────────────────────────────────────────────────
    private static void grantReward(Connection conn, int userId, int rewardId, String trigger) {
        String sql = "INSERT IGNORE INTO User_Rewards (user_id, reward_id, triggered_by) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, rewardId);
            stmt.setString(3, "java:" + trigger);
            stmt.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
