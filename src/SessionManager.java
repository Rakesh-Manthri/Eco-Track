public class SessionManager {
    private static int loggedInUserId = -1;
    private static String loggedInUsername = null;

    public static void setSession(int userId, String username) {
        loggedInUserId = userId;
        loggedInUsername = username;
    }

    public static void clearSession() {
        loggedInUserId = -1;
        loggedInUsername = null;
    }

    public static int getUserId() {
        return loggedInUserId;
    }

    public static String getUsername() {
        return loggedInUsername;
    }

    public static boolean isLoggedIn() {
        return loggedInUserId != -1;
    }
}
