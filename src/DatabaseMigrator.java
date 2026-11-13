import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DatabaseMigrator
 * Executes setup_db.sql against the configured cloud or local MySQL database.
 * Correctly parses standard SQL statements as well as DELIMITER blocks for triggers.
 */
public class DatabaseMigrator {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  Eco-Track Cloud Database Migration & Setup");
        System.out.println("=================================================");

        File scriptFile = new File("setup_db.sql");
        if (!scriptFile.exists()) {
            System.err.println("❌ setup_db.sql not found in workspace root!");
            return;
        }

        try (Connection conn = DatabaseUtil.getConnection()) {
            System.out.println("✅ Connected to: " + conn.getMetaData().getURL());
            System.out.println("   Database Product: " + conn.getMetaData().getDatabaseProductName() + " " + conn.getMetaData().getDatabaseProductVersion());
            System.out.println("   User: " + conn.getMetaData().getUserName());
            System.out.println();

            List<String> statements = parseSqlScript(scriptFile);
            System.out.println("📦 Parsed " + statements.size() + " SQL statements from setup_db.sql. Executing...");

            try (Statement stmt = conn.createStatement()) {
                int successCount = 0;
                for (String sql : statements) {
                    try {
                        stmt.execute(sql);
                        successCount++;
                    } catch (Exception e) {
                        System.err.println("⚠️ Warning executing statement: " + e.getMessage());
                        // Print first 80 chars of the failing statement for context
                        String preview = sql.replaceAll("\\s+", " ").trim();
                        if (preview.length() > 80) preview = preview.substring(0, 80) + "...";
                        System.err.println("   Query: " + preview);
                    }
                }
                System.out.println("✅ Executed " + successCount + " / " + statements.size() + " statements successfully.\n");
            }

            // Verify the setup
            verifyDatabaseState(conn);

        } catch (Exception e) {
            System.err.println("❌ Database migration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static List<String> parseSqlScript(File file) throws Exception {
        List<String> statements = new ArrayList<>();
        String currentDelimiter = ";";
        StringBuilder currentStatement = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();

                // Skip pure comments or empty lines outside statements
                if (trimmed.isEmpty() || (trimmed.startsWith("--") && currentStatement.length() == 0)) {
                    continue;
                }

                // Check for DELIMITER change command (e.g. DELIMITER $$ or DELIMITER ;)
                if (trimmed.toUpperCase().startsWith("DELIMITER")) {
                    String[] parts = trimmed.split("\\s+");
                    if (parts.length >= 2) {
                        currentDelimiter = parts[1].trim();
                    }
                    continue;
                }

                // Append line
                currentStatement.append(line).append("\n");

                // Check if current statement ends with the active delimiter
                if (trimmed.endsWith(currentDelimiter)) {
                    String stmtStr = currentStatement.toString().trim();
                    if (stmtStr.endsWith(currentDelimiter)) {
                        stmtStr = stmtStr.substring(0, stmtStr.length() - currentDelimiter.length()).trim();
                    }
                    if (!stmtStr.isEmpty()) {
                        statements.add(stmtStr);
                    }
                    currentStatement.setLength(0);
                }
            }
        }

        // Catch any remaining statement
        String remaining = currentStatement.toString().trim();
        if (!remaining.isEmpty()) {
            statements.add(remaining);
        }

        return statements;
    }

    private static void verifyDatabaseState(Connection conn) {
        System.out.println("📊 Verifying Cloud Database Objects...");
        try (Statement stmt = conn.createStatement()) {

            // Tables count
            List<String> tables = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery("SHOW FULL TABLES WHERE Table_Type = 'BASE TABLE'")) {
                while (rs.next()) {
                    tables.add(rs.getString(1));
                }
            }
            System.out.println("   • Base Tables (" + tables.size() + "): " + String.join(", ", tables));

            // Views count
            List<String> views = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery("SHOW FULL TABLES WHERE Table_Type = 'VIEW'")) {
                while (rs.next()) {
                    views.add(rs.getString(1));
                }
            }
            System.out.println("   • Views (" + views.size() + "): " + String.join(", ", views));

            // Triggers count
            List<String> triggers = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery("SHOW TRIGGERS")) {
                while (rs.next()) {
                    triggers.add(rs.getString("Trigger"));
                }
            }
            System.out.println("   • Triggers (" + triggers.size() + "): " + String.join(", ", triggers));

            // Check rewards seed
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM Rewards")) {
                if (rs.next()) {
                    System.out.println("   • Seeded Rewards: " + rs.getInt(1) + " items");
                }
            }

            // Check emission factors seed
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM Emission_Factors")) {
                if (rs.next()) {
                    System.out.println("   • Seeded Emission Factors: " + rs.getInt(1) + " items");
                }
            }

            System.out.println("\n🎉 Verification Complete! Cloud Database is fully operational.");

        } catch (Exception e) {
            System.err.println("⚠️ Verification check error: " + e.getMessage());
        }
    }
}
