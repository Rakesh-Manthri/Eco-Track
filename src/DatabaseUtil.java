import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

public class DatabaseUtil {

    private static final String URL = "jdbc:mysql://localhost:3306/carbon_tracker";
    private static final String USER = "root";
    private static final String PASSWORD = "rakes75045";

    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection()) {
            System.out.println("✅ Connected to database automatically. Schema is managed by setup_db.sql");
            seedDefaultData(conn);
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("❌ Error connecting to database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void seedDefaultData(Connection conn) {
        try (Statement stmt = conn.createStatement()) {

            // ── Seed Categories ──────────────────────────────────────────────
            stmt.executeUpdate(
                "INSERT IGNORE INTO Activity_Categories (category_id, category_name, description) VALUES " +
                "(1,'Transportation','All travel and commute related emissions')," +
                "(2,'Energy','Electricity and fuel consumption at home or office')," +
                "(3,'Diet','Food and beverage consumption patterns')," +
                "(4,'Waste','Garbage, recycling and disposal habits')," +
                "(5,'Shopping','Clothing, electronics and consumer goods purchases')," +
                "(6,'Water','Water heating and usage emissions')"
            );

            // ── Seed Emission Factors ─────────────────────────────────────────
            stmt.executeUpdate(
                "INSERT IGNORE INTO Emission_Factors (factor_id, factor_name, co2_per_unit, unit, region, source) VALUES " +
                "(1, 'Petrol Car',             0.1700, 'km',    'India',  'IPCC AR6')," +
                "(2, 'Diesel Bus',             0.0550, 'km',    'India',  'IPCC AR6')," +
                "(3, 'Grid Electricity India', 0.7080, 'kWh',   'India',  'CEA India 2023')," +
                "(4, 'Beef Meal',              3.0000, 'meal',  'Global', 'Our World in Data')," +
                "(5, 'Petrol Motorbike',       0.1030, 'km',    'India',  'IPCC AR6')," +
                "(6, 'Domestic Flight India',  0.2550, 'km',    'India',  'DEFRA 2023')," +
                "(7, 'Metro Rail',             0.0310, 'km',    'India',  'DMRC Report')," +
                "(8, 'Auto Rickshaw CNG',      0.0630, 'km',    'India',  'CPCB India')," +
                "(9, 'Electric Vehicle',       0.0850, 'km',    'India',  'CEA India 2023')," +
                "(10,'LPG Cooking',            1.5100, 'hour',  'India',  'IPCC AR6')," +
                "(11,'Natural Gas Heating',    2.0400, 'hour',  'India',  'IPCC AR6')," +
                "(12,'Chicken Meal',           0.6500, 'meal',  'Global', 'Our World in Data')," +
                "(13,'Vegetarian Meal',        0.1700, 'meal',  'Global', 'Our World in Data')," +
                "(14,'Vegan Meal',             0.0900, 'meal',  'Global', 'Our World in Data')," +
                "(15,'Dairy Consumption',      0.9400, 'kg',    'Global', 'Our World in Data')," +
                "(16,'Landfill Waste',         0.4580, 'kg',    'India',  'CPCB India')," +
                "(17,'Recycled Waste',         0.0210, 'kg',    'Global', 'IPCC AR6')," +
                "(18,'Fast Fashion Item',      5.5000, 'item',  'Global', 'UNEP 2023')," +
                "(19,'Electronics Purchase',  70.0000, 'item',  'Global', 'EPA 2023')," +
                "(20,'Hot Water Usage',        0.1380, 'litre', 'India',  'IPCC AR6')," +
                "(21,'Diesel Generator',       0.8200, 'kWh',   'India',  'CPCB India')," +
                "(22,'Train Travel India',     0.0410, 'km',    'India',  'Indian Railways 2023')," +
                "(23,'Bottled Water',          0.2100, 'litre', 'Global', 'IPCC AR6')," +
                "(24,'Food Waste',             2.5300, 'kg',    'Global', 'FAO 2023')"
            );

            // ── Seed Activity Types ───────────────────────────────────────────
            stmt.executeUpdate(
                "INSERT IGNORE INTO Activity_Types (activity_type_id, category_id, name, default_unit, typical_emission_factor_id) VALUES " +
                "(1, 1,'Driving (Car)',          'km',    1)," +
                "(2, 1,'Taking the Bus',         'km',    2)," +
                "(3, 2,'Home Electricity',       'kWh',   3)," +
                "(4, 3,'Eating Meat',            'meal',  4)," +
                "(5, 1,'Riding Motorbike',       'km',    5)," +
                "(6, 1,'Domestic Flight',        'km',    6)," +
                "(7, 1,'Metro Travel',           'km',    7)," +
                "(8, 1,'Auto Rickshaw',          'km',    8)," +
                "(9, 1,'Electric Vehicle',       'km',    9)," +
                "(10,1,'Train Travel',           'km',    22)," +
                "(11,2,'LPG Cooking',            'hour',  10)," +
                "(12,2,'Diesel Generator Usage', 'kWh',   21)," +
                "(13,2,'Natural Gas Heating',    'hour',  11)," +
                "(14,3,'Eating Chicken',         'meal',  12)," +
                "(15,3,'Vegetarian Meal',        'meal',  13)," +
                "(16,3,'Vegan Meal',             'meal',  14)," +
                "(17,3,'Dairy Consumption',      'kg',    15)," +
                "(18,3,'Food Waste',             'kg',    24)," +
                "(19,3,'Bottled Water',          'litre', 23)," +
                "(20,4,'General Waste Disposal', 'kg',    16)," +
                "(21,4,'Recycling',              'kg',    17)," +
                "(22,5,'Buying Clothes',         'item',  18)," +
                "(23,5,'Buying Electronics',     'item',  19)," +
                "(24,6,'Hot Water Usage',        'litre', 20)"
            );

            // ── Seed Rewards ──────────────────────────────────────────────────
            stmt.executeUpdate(
                "INSERT IGNORE INTO Rewards (reward_id, reward_name, description, condition_type, condition_value, badge_icon_url) VALUES " +
                "(1, 'Eco Starter',       'Keep total emissions below 50kg for a month',          'below_threshold', 50.00,  'https://cdn.jsdelivr.net/npm/twemoji@14.0.2/assets/svg/1f331.svg')," +
                "(2, 'Carbon Saver',      'Keep total emissions below 30kg for a month',          'below_threshold', 30.00,  'https://cdn.jsdelivr.net/npm/twemoji@14.0.2/assets/svg/1f343.svg')," +
                "(3, 'Earth Champion',    'Keep total emissions below 10kg for a month',          'below_threshold', 10.00,  'https://cdn.jsdelivr.net/npm/twemoji@14.0.2/assets/svg/1f3c6.svg')," +
                "(4, 'First Step',        'Log your very first carbon activity',                  'total_saved',      1.00,  'https://cdn.jsdelivr.net/npm/twemoji@14.0.2/assets/svg/1f463.svg')," +
                "(5, 'Week Warrior',      'Stay under your goal for 7 consecutive days',          'streak_days',      7.00,  'https://cdn.jsdelivr.net/npm/twemoji@14.0.2/assets/svg/1f525.svg')," +
                "(6, 'Fortnight Fighter', 'Stay under your goal for 14 consecutive days',         'streak_days',     14.00,  'https://cdn.jsdelivr.net/npm/twemoji@14.0.2/assets/svg/26a1.svg')," +
                "(7, 'Month Master',      'Stay under your goal for 30 consecutive days',         'streak_days',     30.00,  'https://cdn.jsdelivr.net/npm/twemoji@14.0.2/assets/svg/1f451.svg')," +
                "(8, 'Carbon Cutter',     'Save a cumulative total of 25kg CO2 through offsets',  'total_saved',     25.00,  'https://cdn.jsdelivr.net/npm/twemoji@14.0.2/assets/svg/2702.svg')," +
                "(9, 'Offset Hero',       'Save a cumulative total of 100kg CO2 through offsets', 'total_saved',    100.00,  'https://cdn.jsdelivr.net/npm/twemoji@14.0.2/assets/svg/1f9a8.svg')," +
                "(10,'Green Legend',      'Save a cumulative total of 500kg CO2 through offsets', 'total_saved',    500.00,  'https://cdn.jsdelivr.net/npm/twemoji@14.0.2/assets/svg/1f30d.svg')," +
                "(11,'Solar Soul',        'Keep total emissions below 20kg for a month',          'below_threshold', 20.00,  'https://cdn.jsdelivr.net/npm/twemoji@14.0.2/assets/svg/2600.svg')," +
                "(12,'Net Zero',          'Achieve net zero emissions in any single month',        'below_threshold',  0.01,  'https://cdn.jsdelivr.net/npm/twemoji@14.0.2/assets/svg/1f9f2.svg')," +
                "(13,'Tree Hugger',       'Save a cumulative total of 10kg CO2 through offsets',  'total_saved',     10.00,  'https://cdn.jsdelivr.net/npm/twemoji@14.0.2/assets/svg/1f333.svg')," +
                "(14,'Consistency King',  'Stay under your goal for 21 consecutive days',         'streak_days',     21.00,  'https://cdn.jsdelivr.net/npm/twemoji@14.0.2/assets/svg/1f4aa.svg')," +
                "(15,'Planet Protector',  'Keep total emissions below 5kg for a month',           'below_threshold',  5.00,  'https://cdn.jsdelivr.net/npm/twemoji@14.0.2/assets/svg/1f6e1.svg')"
            );

            System.out.println("✅ Seed data verified / applied.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
