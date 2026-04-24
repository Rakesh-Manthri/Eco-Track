import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
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
import java.sql.Statement;

public class OrganizationView {

    public Pane getView() {
        VBox root = new VBox(30);
        root.setPadding(new Insets(40));
        root.setAlignment(Pos.TOP_LEFT);

        String role = SessionManager.getRole();

        // ── Route by role ──────────────────────────────────────────────────
        if (SessionManager.getOrgId() == null) {
            // No org yet — show create/join options (admin only reaches here)
            renderJoinCreateOptions(root);
        } else if ("admin".equals(role)) {
            renderAdminDashboard(root);
        } else {
            // employee or office without specific role
            renderEmployeeDashboard(root);
        }

        return root;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ADMIN VIEW — org stats + employee list
    // ═══════════════════════════════════════════════════════════════════════
    private void renderAdminDashboard(VBox root) {
        String orgName = getOrgName();

        Label pageTitle = new Label(orgName + " — Admin Dashboard");
        pageTitle.getStyleClass().add("label-title");

        Label subTitle = new Label("Manage your organization and monitor employee footprints");
        subTitle.getStyleClass().add("label-subheader");

        // Org summary stats
        VBox statsCard = new VBox(15);
        statsCard.getStyleClass().add("glass-pane");
        statsCard.setMaxWidth(700);

        Label statsHeader = new Label("Org Summary (This Month)");
        statsHeader.getStyleClass().add("label-header");

        HBox statsRow = new HBox(20);
        statsRow.setAlignment(Pos.CENTER_LEFT);

        String viewQuery =
            "SELECT active_users, total_gross_co2_kg " +
            "FROM org_monthly_summary WHERE org_id = ? ORDER BY year DESC, month DESC LIMIT 1";
        int activeUsers = 0;
        double totalCo2 = 0;
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(viewQuery)) {
            stmt.setInt(1, SessionManager.getOrgId());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                activeUsers = rs.getInt("active_users");
                totalCo2    = rs.getDouble("total_gross_co2_kg");
            }
        } catch (Exception e) { e.printStackTrace(); }

        Label orgIdTag = new Label("Org ID: " + SessionManager.getOrgId());
        orgIdTag.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280; -fx-background-color: #f3f4f6; " +
                          "-fx-padding: 4 12; -fx-background-radius: 999px;");

        statsRow.getChildren().addAll(
            createStatCard("Active Users", String.valueOf(activeUsers)),
            createStatCard("Org Emissions (kg)", String.format("%.2f", totalCo2)),
            orgIdTag
        );

        statsCard.getChildren().addAll(statsHeader, statsRow);

        // Employee list
        Label empHeader = new Label("All Employees");
        empHeader.getStyleClass().add("label-header");

        VBox empList = new VBox(12);
        loadEmployeeList(empList);

        ScrollPane scroll = new ScrollPane(empList);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scroll.setMaxHeight(400);

        root.getChildren().addAll(pageTitle, subTitle, statsCard, empHeader, scroll);
    }

    private void loadEmployeeList(VBox empList) {
        String query =
            "SELECT u.user_id, u.name, u.email, u.role, " +
            "COALESCE(SUM(ec.co2_result), 0) AS total_co2 " +
            "FROM Users u " +
            "LEFT JOIN Activities a  ON u.user_id = a.user_id " +
            "  AND MONTH(a.activity_date) = MONTH(CURDATE()) " +
            "  AND YEAR(a.activity_date)  = YEAR(CURDATE()) " +
            "LEFT JOIN Emission_Calculations ec ON a.activity_id = ec.activity_id " +
            "WHERE u.org_id = ? " +
            "GROUP BY u.user_id, u.name, u.email, u.role " +
            "ORDER BY total_co2 DESC";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, SessionManager.getOrgId());
            ResultSet rs = stmt.executeQuery();
            boolean found = false;
            while (rs.next()) {
                found = true;
                String uName   = rs.getString("name");
                String uEmail  = rs.getString("email");
                String uRole   = rs.getString("role") != null ? rs.getString("role") : "individual";
                double uCo2    = rs.getDouble("total_co2");

                HBox row = new HBox(15);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(12, 20, 12, 20));
                row.getStyleClass().add("glass-pane");

                VBox info = new VBox(3);
                Label nameLbl  = new Label(uName);
                nameLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #111827;");
                Label emailLbl = new Label(uEmail + " · " + uRole.toUpperCase());
                emailLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
                info.getChildren().addAll(nameLbl, emailLbl);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Label co2Lbl = new Label(String.format("%.2f kg CO₂", uCo2));
                co2Lbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " +
                                (uCo2 > 30 ? "#ef4444" : "#10b981") + ";");

                row.getChildren().addAll(info, spacer, co2Lbl);
                empList.getChildren().add(row);
            }
            if (!found) {
                Label empty = new Label("No employees found in your organization yet.");
                empty.getStyleClass().add("label-normal");
                empList.getChildren().add(empty);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // EMPLOYEE VIEW — org details only
    // ═══════════════════════════════════════════════════════════════════════
    private void renderEmployeeDashboard(VBox root) {
        String orgName = getOrgName();

        Label pageTitle = new Label("My Organization");
        pageTitle.getStyleClass().add("label-title");

        Label subTitle = new Label("You are a member of " + orgName);
        subTitle.getStyleClass().add("label-subheader");

        VBox detailCard = new VBox(15);
        detailCard.getStyleClass().add("glass-pane");
        detailCard.setMaxWidth(600);

        String industry = "";
        int memberCount = 0;
        double orgCo2 = 0;

        try (Connection conn = DatabaseUtil.getConnection()) {
            PreparedStatement s1 = conn.prepareStatement(
                "SELECT industry FROM Organizations WHERE org_id = ?");
            s1.setInt(1, SessionManager.getOrgId());
            ResultSet r1 = s1.executeQuery();
            if (r1.next()) industry = r1.getString("industry");

            PreparedStatement s2 = conn.prepareStatement(
                "SELECT COUNT(*) FROM Users WHERE org_id = ?");
            s2.setInt(1, SessionManager.getOrgId());
            ResultSet r2 = s2.executeQuery();
            if (r2.next()) memberCount = r2.getInt(1);

            PreparedStatement s3 = conn.prepareStatement(
                "SELECT COALESCE(SUM(total_gross_co2_kg),0) AS co2 " +
                "FROM org_monthly_summary WHERE org_id = ? ORDER BY year DESC, month DESC LIMIT 1");
            s3.setInt(1, SessionManager.getOrgId());
            ResultSet r3 = s3.executeQuery();
            if (r3.next()) orgCo2 = r3.getDouble("co2");
        } catch (Exception e) { e.printStackTrace(); }

        Label header = new Label(orgName);
        header.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #111827;");

        Label orgIdLbl = new Label("Organization ID: " + SessionManager.getOrgId());
        orgIdLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");

        Label industryLbl = new Label("Industry: " + (industry.isEmpty() ? "Not specified" : industry));
        industryLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");

        HBox statsRow = new HBox(20);
        statsRow.setAlignment(Pos.CENTER_LEFT);
        statsRow.setPadding(new Insets(15, 0, 0, 0));
        statsRow.getChildren().addAll(
            createStatCard("Total Members", String.valueOf(memberCount)),
            createStatCard("Org Emissions (kg)", String.format("%.2f", orgCo2))
        );

        detailCard.getChildren().addAll(header, orgIdLbl, industryLbl, statsRow);
        root.getChildren().addAll(pageTitle, subTitle, detailCard);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // NO ORG YET — Create or join (for admins who haven't created one)
    // ═══════════════════════════════════════════════════════════════════════
    private void renderJoinCreateOptions(VBox root) {
        Label pageTitle = new Label("Organization Portal");
        pageTitle.getStyleClass().add("label-title");

        Label subTitle = new Label("You are not part of an organization yet.");
        subTitle.getStyleClass().add("label-subheader");

        HBox options = new HBox(30);
        options.setAlignment(Pos.TOP_LEFT);

        // Create Org
        VBox createBox = new VBox(12);
        createBox.getStyleClass().add("glass-pane");
        createBox.setMaxWidth(300);

        Label createTitle = new Label("Register New Organization");
        createTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        TextField orgNameField = new TextField();
        orgNameField.setPromptText("Organization Name");
        orgNameField.getStyleClass().add("text-field-glass");

        TextField industryField = new TextField();
        industryField.setPromptText("Industry (e.g. Tech)");
        industryField.getStyleClass().add("text-field-glass");

        Button createBtn = new Button("Create Organization");
        createBtn.getStyleClass().add("button-primary");
        createBtn.setMaxWidth(Double.MAX_VALUE);

        Label createError = new Label("");
        createError.setStyle("-fx-text-fill: #e74c3c;");

        createBtn.setOnAction(e -> {
            String name = orgNameField.getText().trim();
            String industry = industryField.getText().trim();
            if (name.isEmpty()) { createError.setText("Name is required"); return; }
            if (createOrganization(name, industry)) ViewManager.showMainLayout();
            else createError.setText("Creation failed.");
        });

        createBox.getChildren().addAll(createTitle, orgNameField, industryField, createBtn, createError);

        // Join Org
        VBox joinBox = new VBox(12);
        joinBox.getStyleClass().add("glass-pane");
        joinBox.setMaxWidth(300);

        Label joinTitle = new Label("Join Existing Organization");
        joinTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        TextField orgIdField = new TextField();
        orgIdField.setPromptText("Organization ID Code");
        orgIdField.getStyleClass().add("text-field-glass");

        Button joinBtn = new Button("Join Organization");
        joinBtn.getStyleClass().add("button-primary");
        joinBtn.setMaxWidth(Double.MAX_VALUE);

        Label joinError = new Label("");
        joinError.setStyle("-fx-text-fill: #e74c3c;");

        joinBtn.setOnAction(e -> {
            try {
                int orgId = Integer.parseInt(orgIdField.getText().trim());
                if (joinOrganization(orgId)) ViewManager.showMainLayout();
                else joinError.setText("Invalid Org ID.");
            } catch (NumberFormatException ex) {
                joinError.setText("Must be a number.");
            }
        });

        joinBox.getChildren().addAll(joinTitle, orgIdField, joinBtn, joinError);

        options.getChildren().addAll(createBox, joinBox);
        root.getChildren().addAll(pageTitle, subTitle, options);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════════
    private String getOrgName() {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT org_name FROM Organizations WHERE org_id = ?")) {
            stmt.setInt(1, SessionManager.getOrgId());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getString("org_name");
        } catch (Exception e) { e.printStackTrace(); }
        return "Unknown Organization";
    }

    private VBox createStatCard(String title, String value) {
        VBox card = new VBox(5);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER);
        card.setMinWidth(180);
        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("label-subheader");
        Label valueLbl = new Label(value);
        valueLbl.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #10b981;");
        card.getChildren().addAll(titleLbl, valueLbl);
        return card;
    }

    private boolean createOrganization(String name, String industry) {
        try (Connection conn = DatabaseUtil.getConnection()) {
            conn.setAutoCommit(false);
            PreparedStatement stmt1 = conn.prepareStatement(
                "INSERT INTO Organizations (org_name, industry) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS);
            stmt1.setString(1, name);
            stmt1.setString(2, industry);
            stmt1.executeUpdate();
            ResultSet rs = stmt1.getGeneratedKeys();
            if (rs.next()) {
                int orgId = rs.getInt(1);
                PreparedStatement stmt2 = conn.prepareStatement(
                    "UPDATE Users SET org_id = ?, role = 'admin' WHERE user_id = ?");
                stmt2.setInt(1, orgId);
                stmt2.setInt(2, SessionManager.getUserId());
                stmt2.executeUpdate();
                SessionManager.setSession(SessionManager.getUserId(), SessionManager.getName(),
                    SessionManager.getEmail(), orgId, SessionManager.getAccountType(), "admin");
                conn.commit();
                return true;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    private boolean joinOrganization(int orgId) {
        String query = "UPDATE Users SET org_id = ? WHERE user_id = ? " +
                       "AND EXISTS (SELECT 1 FROM Organizations WHERE org_id = ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, orgId);
            stmt.setInt(2, SessionManager.getUserId());
            stmt.setInt(3, orgId);
            if (stmt.executeUpdate() > 0) {
                SessionManager.setSession(SessionManager.getUserId(), SessionManager.getName(),
                    SessionManager.getEmail(), orgId, SessionManager.getAccountType(),
                    SessionManager.getRole());
                return true;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }
}
