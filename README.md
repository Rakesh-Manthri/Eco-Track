# 🌿 Eco-Track — Carbon Footprint & Sustainability Tracker

A robust JavaFX desktop application that helps students and campus organizations track, analyze, and reduce their carbon footprint. Originally featuring simple data persistence, the application now sports a gorgeous glassmorphism UI entirely driven by an intricate 13-table SQL database incorporating Views and Triggers for automatic gamification and statistical tracking!

---

## ✨ System Features

| Module | Description |
|---|---|
| **Identity Management** | Role-based registration distinguishing individual students from office-level organizations. Office accounts conditionally unlock the Organization Portal. |
| **Organization Portal** | Create or join organizations, and access centralized global dashboards fed natively by the `org_monthly_summary` view. |
| **Dynamic Tracking** | Select `Activity Categories` securely tied to backend `Emission_Factors` multipliers for precise logging. |
| **Footprint Verification** | View historical logs and seamlessly delete anomalies, automatically reverting net emissions. |
| **Sustainability Goals** | Create daily/weekly/monthly goals to securely restrict and monitor your output. |
| **Offset Tracker** | Let's you log real world eco-friendly actions (e.g. Planting Trees) affecting your "Net" footprint. |
| **Gamification** | Hit targets and let MySQL triggers silently grant you achievements visible in your Rewards Tab! |
| **Visual Analytics** | Built in JavaFX 12-month Trend BarCharts & Category distribution PieCharts. |

---

## 🛠️ Tech Stack

- **Language:** Java 11+
- **UI Framework:** JavaFX (modular SDK 25.0.2)
- **Database:** MySQL 8.4+
- **JDBC Driver:** MySQL Connector/J 9.6.0
- **Styling:** Vanilla JavaFX CSS (Light-theme Glassmorphism + Dynamic Neon Accents)

---

## 📂 Project Structure

```
ecocampus/
├── lib/                        # External libraries
│   └── mysql-connector-j-9.6.0.jar
├── src/
│   ├── Main.java               # Application entry point
│   ├── Launcher.java           # Non-JavaFX launcher wrapper
│   ├── DatabaseUtil.java       # DB Connection & seed data bootstrapper
│   ├── SessionManager.java     # Singleton auth user cache
│   ├── ViewManager.java        # Initial Routing 
│   ├── MainLayout.java         # Global App container & Sidebar
│   ├── LoginView.java          # Auth Panel
│   ├── RegisterView.java       # User / Organization registration 
│   ├── DashboardHome.java      # Dashboard (Net Limits + History)
│   ├── ActivityLogger.java     # Logging mechanism
│   ├── HistoryView.java        # Activity log removal tool
│   ├── GoalsView.java          # CO2 limitation tracker
│   ├── OffsetsView.java        # CO2 offset logger
│   ├── RewardsView.java        # Badges and Achievements
│   ├── AnalyticsView.java      # Trends and Charts (Pie & Bar)
│   ├── ReportsView.java        # Generation and logging of formal reports
│   ├── OrganizationView.java   # Centralized organization dashboard
│   └── style.css               # Light Glassmorphism UI Toolkit
├── setup_db.sql                # The 13-Table Advanced Database Schema!
└── README.md
```

---

## 🚀 Execution Steps

### Step 1: Install MySQL and Configure the Database
This application is strictly bound to its relational database logic. 
1. Open MySQL Workbench.
2. Open and Execute the `setup_db.sql` file natively inside MySQL. This builds out the 13 tables, Views, and Triggers heavily relied on.
3. Update your credentials in `src/DatabaseUtil.java` (lines 9-11) to match your local installation.

### Step 2: Download the JavaFX SDK
Ensure you have downloaded an active unzipped JavaFX-SDK (e.g., version 25.0.2). Keep track of the absolute path where you unzipped the `lib` folder.

### Step 3: Compilation
Open PowerShell in the project root and compile the raw source code pointing to your external jar files. We will output the compiled files to a `bin` directory:
```powershell
mkdir bin
javac --module-path "C:\Program Files\Java\javafx-sdk-25.0.2\lib" --add-modules javafx.controls,javafx.fxml -cp "lib/*" -d bin src/*.java
```

### Step 4: Run the Application
Launch the compiled classes. **Be sure to replace** the module path below with your downloaded JavaFX SDK location!
```powershell
java --module-path "C:\Program Files\Java\javafx-sdk-25.0.2\lib" --add-modules javafx.controls,javafx.fxml -cp "bin;src;lib/mysql-connector-j-9.6.0.jar" Launcher
```

---

## 🗄️ Advanced Database Environment

Instead of basic logging, the application maps real-world architecture enforcing foreign key constraints.

### The 4 Tiers
1. **Tier 1 (Base Data)**: `Organizations`, `Activity_Categories`, `Emission_Factors`, `Rewards`
2. **Tier 2 (Constraints)**: `Users`, `Activity_Types` (Depends on Tier 1 multipliers)
3. **Tier 3 (Logistics)**: `Activities` tracked securely via users, alongside `Goals`, `Offsets`, `User_Rewards`, and `Reports`
4. **Tier 4 (Heavy Data computation)**: `Emission_Calculations` - Every activity maps sequentially against its emission factor, generating a final calculation row stored immutably.

### Realtime Views
The Java application relies heavily on dynamic SQL `VIEWS` such as `user_net_emissions` & `monthly_emission_summary` allowing the math load to sit with your Database Engine rather than inside primitive Java calculations!

### Auto-Triggers
When an emission calculation is entered, MySQL evaluates the `trg_check_reward` trigger natively! The application locally scopes your counts, and pushes immediate localized UI notifications natively to visually alert you that you have unlocked a shiny new badge!

---

## 🚫 Known Limitations

- **Email Deliverability**: The application assumes valid email credentials during registration but does not currently incorporate an SMTP library to dispatch formal confirmation emails or reset tokens.
- **Reporting Intervals**: Automatic generation via `.pdf` or formal export isn't coded; users must review statistics intra-app.
- **Password Hashes**: The MVP uses a basic `hashCode` dummy-hash scheme rather than BCrypt or Argon2 for simplicity in demonstration.
