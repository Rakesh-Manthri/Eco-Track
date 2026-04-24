-- ============================================================
--   PERSONAL CARBON FOOTPRINT & SUSTAINABILITY TRACKER
--   Database: carbon_tracker
--   Author  : Rakesh,Ashish
--   Course  : Database Management Systems
-- ============================================================

DROP DATABASE IF EXISTS carbon_tracker;
CREATE DATABASE carbon_tracker;
USE carbon_tracker;

-- ============================================================
-- TIER 1 — No foreign key dependencies
-- Order: Organizations, Activity_Categories, Emission_Factors, Rewards
-- ============================================================

-- 1. Organizations
CREATE TABLE Organizations (
    org_id          INT             AUTO_INCREMENT PRIMARY KEY,
    org_name        VARCHAR(100)    NOT NULL,
    industry        VARCHAR(50),
    employee_count  INT             CHECK (employee_count > 0),
    location        VARCHAR(100),
    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

-- 2. Activity_Categories
CREATE TABLE Activity_Categories (
    category_id     INT             AUTO_INCREMENT PRIMARY KEY,
    category_name   VARCHAR(50)     NOT NULL UNIQUE,
    description     TEXT,
    icon_url        VARCHAR(255)
);

-- 3. Emission_Factors
CREATE TABLE Emission_Factors (
    factor_id       INT             AUTO_INCREMENT PRIMARY KEY,
    factor_name     VARCHAR(100)    NOT NULL,
    co2_per_unit    DECIMAL(10,4)   NOT NULL CHECK (co2_per_unit > 0),
    unit            VARCHAR(20)     NOT NULL,
    region          VARCHAR(50),
    source          VARCHAR(100),
    valid_from      DATE,
    valid_to        DATE,           -- NULL means currently active
    CONSTRAINT chk_validity CHECK (valid_to IS NULL OR valid_to > valid_from)
);

-- 4. Rewards
CREATE TABLE Rewards (
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
CREATE TABLE Users (
    user_id         INT             AUTO_INCREMENT PRIMARY KEY,
    org_id          INT,            -- NULL for individual accounts
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
--    NOTE: typical_emission_factor_id added after Emission_Factors
--    is created (Tier 1), so safe to reference here
CREATE TABLE Activity_Types (
    activity_type_id            INT             AUTO_INCREMENT PRIMARY KEY,
    category_id                 INT             NOT NULL,
    name                        VARCHAR(100)    NOT NULL UNIQUE,
    default_unit                VARCHAR(20)     NOT NULL,
    description                 TEXT,
    typical_emission_factor_id  INT,            -- soft default, nullable
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
CREATE TABLE Activities (
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
CREATE TABLE Goals (
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
CREATE TABLE Offsets (
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

-- 10. User_Rewards  (junction: Users <-> Rewards)
CREATE TABLE User_Rewards (
    id              INT             AUTO_INCREMENT PRIMARY KEY,
    user_id         INT             NOT NULL,
    reward_id       INT             NOT NULL,
    earned_at       TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    triggered_by    VARCHAR(100),   -- e.g. 'goal_id:3' or 'activity_id:12'
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
CREATE TABLE Reports (
    report_id       INT             AUTO_INCREMENT PRIMARY KEY,
    user_id         INT             NOT NULL,
    org_id          INT,            -- NULL for individual reports
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

-- 12. Activity_EmissionFactors  (junction: Activity_Types <-> Emission_Factors)
CREATE TABLE Activity_EmissionFactors (
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
CREATE TABLE Emission_Calculations (
    calculation_id      INT             AUTO_INCREMENT PRIMARY KEY,
    activity_id         INT             NOT NULL,
    factor_id           INT             NOT NULL,
    quantity            DECIMAL(10,2)   NOT NULL,   -- snapshot at calculation time
    multiplier          DECIMAL(5,2)    DEFAULT 1.0 CHECK (multiplier > 0),
    co2_result          DECIMAL(10,4)   NOT NULL,   -- quantity * co2_per_unit * multiplier
    calculation_method  VARCHAR(100),               -- e.g. 'IPCC AR6 Tier 1'
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

-- Monthly emission summary per user
CREATE VIEW monthly_emission_summary AS
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

-- Net emissions per user (gross minus offsets)
CREATE VIEW user_net_emissions AS
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

-- Organisation-level monthly summary
CREATE VIEW org_monthly_summary AS
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
-- TRIGGER
-- Auto-award reward when user stays below threshold
-- ============================================================

DELIMITER $$

CREATE TRIGGER trg_check_reward
AFTER INSERT ON Emission_Calculations
FOR EACH ROW
BEGIN
    DECLARE total_co2      DECIMAL(10,4);
    DECLARE uid            INT;
    DECLARE rid            INT;
    DECLARE threshold_val  DECIMAL(10,2);

    -- Get the user for this activity
    SELECT user_id INTO uid
    FROM Activities
    WHERE activity_id = NEW.activity_id;

    -- Sum this user's total CO2 this month
    SELECT COALESCE(SUM(ec.co2_result), 0) INTO total_co2
    FROM Emission_Calculations ec
    JOIN Activities a ON ec.activity_id = a.activity_id
    WHERE a.user_id = uid
      AND MONTH(a.activity_date) = MONTH(CURDATE())
      AND YEAR(a.activity_date)  = YEAR(CURDATE());

    -- Check if any below_threshold reward is satisfied and not yet earned
    SELECT r.reward_id, r.condition_value
    INTO rid, threshold_val
    FROM Rewards r
    WHERE r.condition_type = 'below_threshold'
      AND r.condition_value >= total_co2
      AND r.reward_id NOT IN (
          SELECT reward_id FROM User_Rewards WHERE user_id = uid
      )
    LIMIT 1;

    -- Award if matched
    IF rid IS NOT NULL THEN
        INSERT INTO User_Rewards (user_id, reward_id, triggered_by)
        VALUES (uid, rid, CONCAT('auto:emission_calc:', NEW.calculation_id));
    END IF;
END$$

CREATE TRIGGER trg_check_goal_failure
AFTER INSERT ON Emission_Calculations
FOR EACH ROW
BEGIN
    DECLARE total_co2 DECIMAL(10,4);
    DECLARE uid INT;

    SELECT user_id INTO uid
    FROM Activities
    WHERE activity_id = NEW.activity_id;

    -- Update any active goals that have been exceeded
    UPDATE Goals g
    SET g.status = 'failed'
    WHERE g.user_id = uid 
      AND g.status = 'active'
      AND g.target_co2_kg < (
          SELECT COALESCE(SUM(ec.co2_result), 0)
          FROM Emission_Calculations ec
          JOIN Activities a ON ec.activity_id = a.activity_id
          WHERE a.user_id = uid
            AND a.activity_date BETWEEN g.start_date AND g.end_date
      );
END$$

DELIMITER ;
