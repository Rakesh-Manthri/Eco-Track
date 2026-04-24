import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class RewardsView {

    public Pane getView() {
        VBox root = new VBox(30);
        root.setPadding(new Insets(40));
        root.setAlignment(Pos.TOP_LEFT);

        Label pageTitle = new Label("My Achievements");
        pageTitle.getStyleClass().add("label-title");
        
        Label subTitle = new Label("Earn badges by hitting your sustainability targets!");
        subTitle.getStyleClass().add("label-subheader");

        FlowPane badgesPane = new FlowPane(20, 20);
        badgesPane.setAlignment(Pos.TOP_LEFT);

        ScrollPane scroll = new ScrollPane(badgesPane);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        // Load badges on a background thread to prevent UI freeze
        Thread loaderThread = new Thread(() -> loadBadges(badgesPane));
        loaderThread.setDaemon(true);
        loaderThread.start();

        root.getChildren().addAll(pageTitle, subTitle, scroll);
        return root;
    }

    private void loadBadges(FlowPane badgesPane) {
        String query = "SELECT r.reward_id, r.reward_name, r.description, r.badge_icon_url, r.condition_type, ur.earned_at " +
                       "FROM Rewards r " +
                       "LEFT JOIN User_Rewards ur ON r.reward_id = ur.reward_id AND ur.user_id = ? " +
                       "ORDER BY r.reward_id ASC";
                       
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, SessionManager.getUserId());
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                String name       = rs.getString("reward_name");
                String desc       = rs.getString("description");
                String iconUrl    = rs.getString("badge_icon_url");
                String condType   = rs.getString("condition_type");
                String earnedAt   = rs.getString("earned_at");
                boolean isEarned  = earnedAt != null;

                VBox badgeCard = new VBox(10);
                badgeCard.setAlignment(Pos.CENTER);
                badgeCard.setPrefWidth(200);
                badgeCard.setPrefHeight(240);
                badgeCard.setPadding(new Insets(20));
                badgeCard.getStyleClass().add("glass-pane");

                if (isEarned) {
                    badgeCard.setStyle("-fx-background-color: #ffffff; -fx-border-color: #10b981; -fx-border-width: 2px; -fx-background-radius: 12px; -fx-border-radius: 12px;");
                } else {
                    badgeCard.setStyle("-fx-background-color: #f9fafb; -fx-border-color: #e5e7eb; -fx-border-width: 1px; -fx-background-radius: 12px; -fx-border-radius: 12px; -fx-opacity: 0.6;");
                }

                // Icon area with circle background
                StackPane iconStack = new StackPane();
                Circle circle = new Circle(36);
                circle.setFill(isEarned ? Color.web("#ecfdf5") : Color.web("#f3f4f6"));
                circle.setStroke(isEarned ? Color.web("#10b981") : Color.web("#d1d5db"));
                circle.setStrokeWidth(2);

                // Try to load SVG from URL, fallback to emoji text
                javafx.scene.Node iconNode;
                try {
                    Image img = new Image(iconUrl, 40, 40, true, true, true); // true = background loading
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(40);
                    iv.setFitHeight(40);
                    if (!isEarned) iv.setOpacity(0.4);
                    iconNode = iv;
                } catch (Exception ex) {
                    Label fallback = new Label("🏅");
                    fallback.setStyle("-fx-font-size: 28px;");
                    iconNode = fallback;
                }

                iconStack.getChildren().addAll(circle, iconNode);

                // Badge type tag
                String tag = condType.equals("below_threshold") ? "Threshold" :
                             condType.equals("streak_days")     ? "Streak"    : "Offset";
                Label tagLabel = new Label(tag);
                tagLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #6b7280; " +
                                  "-fx-background-color: #f3f4f6; -fx-padding: 2 8; -fx-background-radius: 999px;");

                Label nameLabel = new Label(name);
                nameLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: " +
                                   (isEarned ? "#111827" : "#9ca3af") + ";");
                nameLabel.setWrapText(true);
                nameLabel.setAlignment(Pos.CENTER);

                Label descLabel = new Label(desc);
                descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " +
                                   (isEarned ? "#6b7280" : "#d1d5db") + ";");
                descLabel.setWrapText(true);
                descLabel.setAlignment(Pos.CENTER);

                Label statusLabel = new Label(isEarned ? "✅ Earned: " + earnedAt.split(" ")[0] : "🔒 Locked");
                statusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " +
                                     (isEarned ? "#10b981" : "#9ca3af") + ";");

                badgeCard.getChildren().addAll(iconStack, tagLabel, nameLabel, descLabel, statusLabel);
                // Must add to UI on the JavaFX Application Thread
                Platform.runLater(() -> badgesPane.getChildren().add(badgeCard));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
