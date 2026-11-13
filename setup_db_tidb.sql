-- ============================================================
--   PERSONAL CARBON FOOTPRINT & SUSTAINABILITY TRACKER
--   Database: carbon_tracker (TiDB Cloud Compatible)
--   Author  : Rakesh, Ashish
-- ============================================================

CREATE DATABASE IF NOT EXISTS carbon_tracker;
USE carbon_tracker;

-- ============================================================
-- TIER 1 — No foreign key dependencies
-- Order: Organizations, Activity_Categories, Emission_Factors, Rewards
-- ============================================================

-- 1. Organizations
CREATE TABLE IF NOT EXISTS Organizations (
    org_id          INT             AUTO_INCREMENT PRIMARY KEY,
    org_name        VARCHAR(100)    NOT NULL,
    industry        VARCHAR(50),
    employee_count  INT             CHECK (employee_count > 0),
    location        VARCHAR(100),
    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

-- 2. Activity_Categories
CREATE TABLE IF NOT EXISTS Activity_Categories (
    category_id     INT             AUTO_INCREMENT PRIMARY KEY,
    category_name   VARCHAR(50)     NOT NULL UNIQUE,
    description     TEXT,
    icon_url        VARCHAR(255)
);

-- 3. Emission_Factors
CREATE TABLE IF NOT EXISTS Emission_Factors (
    factor_id       INT             AUTO_INCREMENT PRIMARY KEY,
    factor_name     VARCHAR(100)    NOT NULL,
    co2_per_unit    DECIMAL(10,4)   NOT NULL CHECK (co2_per_unit > 0),
    unit            VARCHAR(20)     NOT NULL,
    region          VARCHAR(50),
    source          VARCHAR(100),
    valid_from      DATE,
    valid_to        DATE,
    CONSTRAINT chk_validity CHECK (valid_to IS NULL OR valid_to > valid_from)
);

-- 4. Rewards
CREATE TABLE IF NOT EXISTS Rewards (
    reward_id           INT             AUTO_INCREMENT PRIMARY KEY,
    reward_name         VARCHAR(100)    NOT NULL UNIQUE,
    description         TEXT,
    condition_type      ENUM(
                            'below_threshold',
                            'streak_days',
                            'total_saved'
                        )               NOT NULL,
    condition_value     DECIMAL(10,2)   NOT NULL CHECK (condition_value > 0),
    badge_icon_url      VARCHAR(255)
);

-- ============================================================
-- TIER 2 — Depend on Tier 1
-- Order: Users, Activity_Types
-- ============================================================

-- 5. Users
CREATE TABLE IF NOT EXISTS Users (
    user_id         INT             AUTO_INCREMENT PRIMARY KEY,
    org_id          INT,
    name            VARCHAR(100)    NOT NULL,
    email           VARCHAR(100)    NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    role            ENUM(
                        'admin',
                        'employee',
                        'viewer'
                    )               DEFAULT 'employee',
    account_type    ENUM(
                        'individual',
                        'office'
                    )               NOT NULL,
    location        VARCHAR(100),
    household_size  INT             CHECK (household_size > 0),
    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_org
        FOREIGN KEY (org_id) REFERENCES Organizations(org_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
);

-- 6. Activity_Types
CREATE TABLE IF NOT EXISTS Activity_Types (
    activity_type_id            INT             AUTO_INCREMENT PRIMARY KEY,
    category_id                 INT             NOT NULL,
    name                        VARCHAR(100)    NOT NULL UNIQUE,
    default_unit                VARCHAR(20)     NOT NULL,
    description                 TEXT,
    typical_emission_factor_id  INT,
    CONSTRAINT fk_type_category
        FOREIGN KEY (category_id) REFERENCES Activity_Categories(category_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_type_factor
        FOREIGN KEY (typical_emission_factor_id) REFERENCES Emission_Factors(factor_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
);

-- ============================================================
-- TIER 3 — Depend on Tier 2
-- Order: Activities, Goals, Offsets, User_Rewards, Reports
-- ============================================================

-- 7. Activities
CREATE TABLE IF NOT EXISTS Activities (
    activity_id         INT             AUTO_INCREMENT PRIMARY KEY,
    user_id             INT             NOT NULL,
    activity_type_id    INT             NOT NULL,
    quantity            DECIMAL(10,2)   NOT NULL CHECK (quantity > 0),
    unit                VARCHAR(20)     NOT NULL,
    activity_date       DATE            NOT NULL,
    notes               TEXT,
    created_at          TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_activity_user
        FOREIGN KEY (user_id) REFERENCES Users(user_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_activity_type
        FOREIGN KEY (activity_type_id) REFERENCES Activity_Types(activity_type_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

-- 8. Goals
CREATE TABLE IF NOT EXISTS Goals (
    goal_id         INT             AUTO_INCREMENT PRIMARY KEY,
    user_id         INT             NOT NULL,
    target_co2_kg   DECIMAL(10,2)   NOT NULL CHECK (target_co2_kg > 0),
    period_type     ENUM(
                        'daily',
                        'weekly',
                        'monthly'
                    )               NOT NULL,
    start_date      DATE            NOT NULL,
    end_date        DATE            NOT NULL,
    status          ENUM(
                        'active',
                        'completed',
                        'failed'
                    )               DEFAULT 'active',
    CONSTRAINT chk_goal_dates CHECK (end_date > start_date),
    CONSTRAINT fk_goal_user
        FOREIGN KEY (user_id) REFERENCES Users(user_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- 9. Offsets
CREATE TABLE IF NOT EXISTS Offsets (
    offset_id       INT             AUTO_INCREMENT PRIMARY KEY,
    user_id         INT             NOT NULL,
    offset_type     VARCHAR(100)    NOT NULL,
    co2_offset_kg   DECIMAL(10,2)   NOT NULL CHECK (co2_offset_kg > 0),
    cost            DECIMAL(10,2)   CHECK (cost >= 0),
    offset_date     DATE            NOT NULL,
    verified        BOOLEAN         DEFAULT FALSE,
    CONSTRAINT fk_offset_user
        FOREIGN KEY (user_id) REFERENCES Users(user_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- 10. User_Rewards (junction: Users <-> Rewards)
CREATE TABLE IF NOT EXISTS User_Rewards (
    id              INT             AUTO_INCREMENT PRIMARY KEY,
    user_id         INT             NOT NULL,
    reward_id       INT             NOT NULL,
    earned_at       TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    triggered_by    VARCHAR(100),
    CONSTRAINT uq_user_reward UNIQUE (user_id, reward_id),
    CONSTRAINT fk_ur_user
        FOREIGN KEY (user_id) REFERENCES Users(user_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_ur_reward
        FOREIGN KEY (reward_id) REFERENCES Rewards(reward_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- 11. Reports
CREATE TABLE IF NOT EXISTS Reports (
    report_id       INT             AUTO_INCREMENT PRIMARY KEY,
    user_id         INT             NOT NULL,
    org_id          INT,
    period_type     ENUM(
                        'daily',
                        'weekly',
                        'monthly'
                    )               NOT NULL,
    period_start    DATE            NOT NULL,
    period_end      DATE            NOT NULL,
    gross_co2_kg    DECIMAL(10,2)   DEFAULT 0,
    offset_co2_kg   DECIMAL(10,2)   DEFAULT 0,
    net_co2_kg      DECIMAL(10,2)   DEFAULT 0,
    goal_met        BOOLEAN,
    generated_at    TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_report_dates CHECK (period_end >= period_start),
    CONSTRAINT fk_report_user
        FOREIGN KEY (user_id) REFERENCES Users(user_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_report_org
        FOREIGN KEY (org_id) REFERENCES Organizations(org_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
);

-- ============================================================
-- TIER 4 — Depend on Tier 2 + Tier 3
-- Order: Activity_EmissionFactors, Emission_Calculations
-- ============================================================

-- 12. Activity_EmissionFactors
CREATE TABLE IF NOT EXISTS Activity_EmissionFactors (
    id                  INT     AUTO_INCREMENT PRIMARY KEY,
    activity_type_id    INT     NOT NULL,
    factor_id           INT     NOT NULL,
    CONSTRAINT uq_type_factor UNIQUE (activity_type_id, factor_id),
    CONSTRAINT fk_aef_type
        FOREIGN KEY (activity_type_id) REFERENCES Activity_Types(activity_type_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_aef_factor
        FOREIGN KEY (factor_id) REFERENCES Emission_Factors(factor_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- 13. Emission_Calculations
CREATE TABLE IF NOT EXISTS Emission_Calculations (
    calculation_id      INT             AUTO_INCREMENT PRIMARY KEY,
    activity_id         INT             NOT NULL,
    factor_id           INT             NOT NULL,
    quantity            DECIMAL(10,2)   NOT NULL,
    multiplier          DECIMAL(5,2)    DEFAULT 1.0 CHECK (multiplier > 0),
    co2_result          DECIMAL(10,4)   NOT NULL,
    calculation_method  VARCHAR(100),
    calculated_at       TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ec_activity
        FOREIGN KEY (activity_id) REFERENCES Activities(activity_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_ec_factor
        FOREIGN KEY (factor_id) REFERENCES Emission_Factors(factor_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

-- ============================================================
-- VIEWS
-- ============================================================

-- 1. Monthly emission summary per user
CREATE OR REPLACE VIEW monthly_emission_summary AS
SELECT
    u.user_id,
    u.name,
    MONTH(a.activity_date)  AS month,
    YEAR(a.activity_date)   AS year,
    SUM(ec.co2_result)      AS gross_co2_kg
FROM Users u
JOIN Activities a           ON u.user_id = a.user_id
JOIN Emission_Calculations ec ON a.activity_id = ec.activity_id
GROUP BY u.user_id, u.name, YEAR(a.activity_date), MONTH(a.activity_date);

-- 2. Net emissions per user (gross minus offsets)
CREATE OR REPLACE VIEW user_net_emissions AS
SELECT
    u.user_id,
    u.name,
    COALESCE(SUM(ec.co2_result), 0)     AS gross_co2_kg,
    COALESCE(SUM(o.co2_offset_kg), 0)   AS offset_co2_kg,
    COALESCE(SUM(ec.co2_result), 0)
        - COALESCE(SUM(o.co2_offset_kg), 0) AS net_co2_kg
FROM Users u
LEFT JOIN Activities a              ON u.user_id = a.user_id
LEFT JOIN Emission_Calculations ec  ON a.activity_id = ec.activity_id
LEFT JOIN Offsets o                 ON u.user_id = o.user_id
GROUP BY u.user_id, u.name;

-- 3. Organisation-level monthly summary
CREATE OR REPLACE VIEW org_monthly_summary AS
SELECT
    org.org_id,
    org.org_name,
    MONTH(a.activity_date)  AS month,
    YEAR(a.activity_date)   AS year,
    COUNT(DISTINCT u.user_id)   AS active_users,
    SUM(ec.co2_result)          AS total_gross_co2_kg
FROM Organizations org
JOIN Users u                    ON org.org_id = u.org_id
JOIN Activities a               ON u.user_id = a.user_id
JOIN Emission_Calculations ec   ON a.activity_id = ec.activity_id
GROUP BY org.org_id, org.org_name, YEAR(a.activity_date), MONTH(a.activity_date);

-- ============================================================
-- DEFAULT SEED DATA
-- ============================================================

-- Seed Categories
INSERT IGNORE INTO Activity_Categories (category_id, category_name, description) VALUES
(1,'Transportation','All travel and commute related emissions'),
(2,'Energy','Electricity and fuel consumption at home or office'),
(3,'Diet','Food and beverage consumption patterns'),
(4,'Waste','Garbage, recycling and disposal habits'),
(5,'Shopping','Clothing, electronics and consumer goods purchases'),
(6,'Water','Water heating and usage emissions');

-- Seed Emission Factors
INSERT IGNORE INTO Emission_Factors (factor_id, factor_name, co2_per_unit, unit, region, source) VALUES
(1, 'Petrol Car',             0.1700, 'km',    'India',  'IPCC AR6'),
(2, 'Diesel Bus',             0.0550, 'km',    'India',  'IPCC AR6'),
(3, 'Grid Electricity India', 0.7080, 'kWh',   'India',  'CEA India 2023'),
(4, 'Beef Meal',              3.0000, 'meal',  'Global', 'Our World in Data'),
(5, 'Petrol Motorbike',       0.1030, 'km',    'India',  'IPCC AR6'),
(6, 'Domestic Flight India',  0.2550, 'km',    'India',  'DEFRA 2023'),
(7, 'Metro Rail',             0.0310, 'km',    'India',  'DMRC Report'),
(8, 'Auto Rickshaw CNG',      0.0630, 'km',    'India',  'CPCB India'),
(9, 'Electric Vehicle',       0.0850, 'km',    'India',  'CEA India 2023'),
(10,'LPG Cooking',            1.5100, 'hour',  'India',  'IPCC AR6'),
(11,'Natural Gas Heating',    2.0400, 'hour',  'India',  'IPCC AR6'),
(12,'Chicken Meal',           0.6500, 'meal',  'Global', 'Our World in Data'),
(13,'Vegetarian Meal',        0.1700, 'meal',  'Global', 'Our World in Data'),
(14,'Vegan Meal',             0.0900, 'meal',  'Global', 'Our World in Data'),
(15,'Dairy Consumption',      0.9400, 'kg',    'Global', 'Our World in Data'),
(16,'Landfill Waste',         0.4580, 'kg',    'India',  'CPCB India'),
(17,'Recycled Waste',         0.0210, 'kg',    'Global', 'IPCC AR6'),
(18,'Fast Fashion Item',      5.5000, 'item',  'Global', 'UNEP 2023'),
(19,'Electronics Purchase',  70.0000, 'item',  'Global', 'EPA 2023'),
(20,'Hot Water Usage',        0.1380, 'litre', 'India',  'IPCC AR6'),
(21,'Diesel Generator',       0.8200, 'kWh',   'India',  'CPCB India'),
(22,'Train Travel India',     0.0410, 'km',    'India',  'Indian Railways 2023'),
(23,'Bottled Water',          0.2100, 'litre', 'Global', 'IPCC AR6'),
(24,'Food Waste',             2.5300, 'kg',    'Global', 'FAO 2023');

-- Seed Activity Types
INSERT IGNORE INTO Activity_Types (activity_type_id, category_id, name, default_unit, typical_emission_factor_id) VALUES
(1, 1,'Driving (Car)',          'km',    1),
(2, 1,'Taking the Bus',         'km',    2),
(3, 2,'Home Electricity',       'kWh',   3),
(4, 3,'Eating Meat',            'meal',  4),
(5, 1,'Riding Motorbike',       'km',    5),
(6, 1,'Domestic Flight',        'km',    6),
(7, 1,'Metro Travel',           'km',    7),
(8, 1,'Auto Rickshaw',          'km',    8),
(9, 1,'Electric Vehicle',       'km',    9),
(10,1,'Train Travel',           'km',    22),
(11,2,'LPG Cooking',            'hour',  10),
(12,2,'Diesel Generator Usage', 'kWh',   21),
(13,2,'Natural Gas Heating',    'hour',  11),
(14,3,'Eating Chicken',         'meal',  12),
(15,3,'Vegetarian Meal',        'meal',  13),
(16,3,'Vegan Meal',             'meal',  14),
(17,3,'Dairy Consumption',      'kg',    15),
(18,3,'Food Waste',             'kg',    24),
(19,3,'Bottled Water',          'litre', 23),
(20,4,'General Waste Disposal', 'kg',    16),
(21,4,'Recycling',              'kg',    17),
(22,5,'Buying Clothes',         'item',  18),
(23,5,'Buying Electronics',     'item',  19),
(24,6,'Hot Water Usage',        'litre', 20);

-- Seed Rewards
INSERT IGNORE INTO Rewards (reward_id, reward_name, description, condition_type, condition_value, badge_icon_url) VALUES
(1, 'Eco Starter',       'Keep total emissions below 50kg for a month',          'below_threshold', 50.00,  'https://cdn.jsdelivr.net/npm/twemoji@14.0.2/assets/svg/1f331.svg'),
(2, 'Carbon Saver',      'Keep total emissions below 30kg for a month',          'below_threshold', 30.00,  'https://cdn.jsdelivr.net/npm/twemoji@14.0.2/assets/svg/1f343.svg'),
(3, 'Earth Champion',    'Keep total emissions below 10kg for a month',          'below_threshold', 10.00,  'https://cdn.jsdelivr.net/npm/twemoji@14.0.2/assets/svg/1f3c6.svg'),
(4, 'First Step',        'Log your very first carbon activity',                  'total_saved',      1.00,  'https://cdn.jsdelivr.net/npm/twemoji@14.0.2/assets/svg/1f463.svg'),
(5, 'Week Warrior',      'Stay under your goal for 7 consecutive days',          'streak_days',      7.00,  'https://cdn.jsdelivr.net/npm/twemoji@14.0.2/assets/svg/1f525.svg'),
(6, 'Fortnight Fighter', 'Stay under your goal for 14 consecutive days',         'streak_days',     14.00,  'https://cdn.jsdelivr.net/npm/twemoji@14.0.2/assets/svg/26a1.svg'),
(7, 'Month Master',      'Stay under your goal for 30 consecutive days',         'streak_days',     30.00,  'https://cdn.jsdelivr.net/npm/twemoji@14.0.2/assets/svg/1f451.svg'),
(8, 'Carbon Cutter',     'Save a cumulative total of 25kg CO2 through offsets',  'total_saved',     25.00,  'https://cdn.jsdelivr.net/npm/twemoji@14.0.2/assets/svg/2702.svg'),
(9, 'Offset Hero',       'Save a cumulative total of 100kg CO2 through offsets', 'total_saved',    100.00,  'https://cdn.jsdelivr.net/npm/twemoji@14.0.2/assets/svg/1f9a8.svg'),
(10,'Green Legend',      'Save a cumulative total of 500kg CO2 through offsets', 'total_saved',    500.00,  'https://cdn.jsdelivr.net/npm/twemoji@14.0.2/assets/svg/1f30d.svg'),
(11,'Solar Soul',        'Keep total emissions below 20kg for a month',          'below_threshold', 20.00,  'https://cdn.jsdelivr.net/npm/twemoji@14.0.2/assets/svg/2600.svg'),
(12,'Net Zero',          'Achieve net zero emissions in any single month',        'below_threshold',  0.01,  'https://cdn.jsdelivr.net/npm/twemoji@14.0.2/assets/svg/1f9f2.svg'),
(13,'Tree Hugger',       'Save a cumulative total of 10kg CO2 through offsets',  'total_saved',     10.00,  'https://cdn.jsdelivr.net/npm/twemoji@14.0.2/assets/svg/1f333.svg'),
(14,'Consistency King',  'Stay under your goal for 21 consecutive days',         'streak_days',     21.00,  'https://cdn.jsdelivr.net/npm/twemoji@14.0.2/assets/svg/1f4aa.svg'),
(15,'Planet Protector',  'Keep total emissions below 5kg for a month',           'below_threshold',  5.00,  'https://cdn.jsdelivr.net/npm/twemoji@14.0.2/assets/svg/1f6e1.svg');
