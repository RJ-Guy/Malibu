import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Properties;
import java.util.Scanner;

/**
 * Malibu: Casino Floor & Cage Management System
 * JDBC Command-Line Interface (CLI)
 *
 * Implements:
 * - Credentials loaded via db.properties (FileInputStream & Properties)
 * - Automatic resource closing via try-with-resources
 * - Full CRUD with PreparedStatement (SELECT, INSERT, UPDATE, DELETE)
 * - Stored procedure execution (ProcessChipBuyIn) via CallableStatement
 * - Cash-out system with trigger AML check & alert notification
 * - ACID transaction handling (setAutoCommit, commit, rollback)
 * - Analytical reporting with GROUP BY on Game_Tables
 */
public class CasinoApp {

    private static Properties dbProps = new Properties();

    public static void main(String[] args) {
        // Load database configuration
        if (!loadConfig()) {
            System.err.println("[ERROR] Failed to load db.properties. Exiting.");
            return;
        }

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            printMenu();
            System.out.print("Enter your choice (1-9): ");
            String input = scanner.nextLine().trim();

            System.out.println();
            switch (input) {
                case "1":
                    viewAllPlayers();
                    break;
                case "2":
                    registerPlayer(scanner);
                    break;
                case "3":
                    updatePlayerBalance(scanner);
                    break;
                case "4":
                    deleteGameTable(scanner);
                    break;
                case "5":
                    processChipBuyIn(scanner);
                    break;
                case "6":
                    processCashOut(scanner);
                    break;
                case "7":
                    transferChipsTransaction(scanner);
                    break;
                case "8":
                    viewFloorInventoryReport();
                    break;
                case "9":
                    System.out.println("Thank you for using Malibu Casino Management System. Goodbye!");
                    running = false;
                    break;
                default:
                    System.out.println("[!] Invalid choice. Please enter a number between 1 and 9.");
            }

            if (running) {
                System.out.println();
                System.out.print("Press [Enter] to return to menu...");
                scanner.nextLine();
                System.out.println();
            }
        }
        scanner.close();
    }

    // Load credentials from db.properties using FileInputStream and Properties
    private static boolean loadConfig() {
        File file = new File("src/db.properties");
        if (!file.exists()) {
            file = new File("db.properties");
        }
        if (!file.exists()) {
            System.err.println("File db.properties was not found in 'src/' or current directory.");
            return false;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            dbProps.load(fis);
            return true;
        } catch (IOException e) {
            System.err.println("Error reading db.properties: " + e.getMessage());
            return false;
        }
    }

    // Get a new database connection
    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
            dbProps.getProperty("db.url"),
            dbProps.getProperty("db.user"),
            dbProps.getProperty("db.password")
        );
    }

    // Display simple CLI menu
    private static void printMenu() {
        System.out.println("=========================================");
        System.out.println("  MALIBU CASINO & CAGE MANAGEMENT SYSTEM ");
        System.out.println("=========================================");
        System.out.println("1. View All Players (SELECT)");
        System.out.println("2. Register New Player (INSERT)");
        System.out.println("3. Update Player Chip Balance (UPDATE)");
        System.out.println("4. Decommission Game Table (DELETE)");
        System.out.println("5. Process Chip Buy-In (Stored Procedure)");
        System.out.println("6. Process Cash-Out (AML Trigger Alert)");
        System.out.println("7. Transfer Chips (Transaction Demo)");
        System.out.println("8. View Floor Inventory Report (Analytical)");
        System.out.println("9. Exit");
        System.out.println("=========================================");
    }

    // -------------------------------------------------------------------------
    // 1. SELECT: View All Registered Players (NATURAL JOIN)
    // -------------------------------------------------------------------------
    private static void viewAllPlayers() {
        System.out.println("--- All Registered Players ---");
        String sql = "SELECT player_id, full_name, govt_id, age, chip_balance, comp_points, tier_name " +
                     "FROM Players NATURAL JOIN VIP_Tiers ORDER BY player_id";

        try (Connection con = getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            System.out.printf("%-6s %-20s %-14s %-5s %-16s %-10s %-15s\n",
                    "ID", "Full Name", "Govt ID", "Age", "Chip Balance", "Points", "VIP Tier");
            System.out.println("----------------------------------------------------------------------------------------");

            int count = 0;
            while (rs.next()) {
                System.out.printf("%-6d %-20s %-14s %-5d %-16.2f %-10d %-15s\n",
                        rs.getInt("player_id"),
                        rs.getString("full_name"),
                        rs.getString("govt_id"),
                        rs.getInt("age"),
                        rs.getBigDecimal("chip_balance"),
                        rs.getInt("comp_points"),
                        rs.getString("tier_name"));
                count++;
            }
            System.out.println("----------------------------------------------------------------------------------------");
            System.out.println("Total players found: " + count);

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // 2. INSERT: Register a New Player (Statement.RETURN_GENERATED_KEYS)
    // -------------------------------------------------------------------------
    private static void registerPlayer(Scanner scanner) {
        System.out.println("--- Register New Player ---");
        try {
            System.out.print("Enter Full Name: ");
            String fullName = scanner.nextLine().trim();

            System.out.print("Enter Govt KYC ID (e.g. PAN-X1234Z): ");
            String govtId = scanner.nextLine().trim();

            System.out.print("Enter Age (minimum 21): ");
            int age = Integer.parseInt(scanner.nextLine().trim());

            System.out.print("Enter Initial Chip Balance: ");
            BigDecimal balance = new BigDecimal(scanner.nextLine().trim());

            System.out.print("Enter VIP Tier ID (1-10): ");
            int tierId = Integer.parseInt(scanner.nextLine().trim());

            String sql = "INSERT INTO Players (full_name, govt_id, age, chip_balance, comp_points, tier_id) " +
                         "VALUES (?, ?, ?, ?, 0, ?)";

            try (Connection con = getConnection();
                 PreparedStatement pstmt = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                pstmt.setString(1, fullName);
                pstmt.setString(2, govtId);
                pstmt.setInt(3, age);
                pstmt.setBigDecimal(4, balance);
                pstmt.setInt(5, tierId);

                int rows = pstmt.executeUpdate();
                if (rows > 0) {
                    try (ResultSet keys = pstmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            int newId = keys.getInt(1);
                            System.out.println("[SUCCESS] Player registered successfully with Player ID: " + newId);
                        }
                    }
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("[ERROR] Invalid number format entered.");
        } catch (SQLException e) {
            System.err.println("[ERROR] Registration failed: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // 3. UPDATE: Modify a Player's Chip Balance
    // -------------------------------------------------------------------------
    private static void updatePlayerBalance(Scanner scanner) {
        System.out.println("--- Update Player Chip Balance ---");
        try {
            System.out.print("Enter Player ID: ");
            int playerId = Integer.parseInt(scanner.nextLine().trim());

            System.out.print("Enter New Chip Balance: ");
            BigDecimal newBalance = new BigDecimal(scanner.nextLine().trim());

            String sql = "UPDATE Players SET chip_balance = ? WHERE player_id = ?";

            try (Connection con = getConnection();
                 PreparedStatement pstmt = con.prepareStatement(sql)) {

                pstmt.setBigDecimal(1, newBalance);
                pstmt.setInt(2, playerId);

                int rows = pstmt.executeUpdate();
                if (rows > 0) {
                    System.out.printf("[SUCCESS] Player ID %d chip balance updated to INR %,.2f\n", playerId, newBalance);
                } else {
                    System.out.println("[NOT FOUND] No player found with ID: " + playerId);
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("[ERROR] Invalid number format entered.");
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to update balance: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // 4. DELETE: Decommission a Table by Table Code
    // -------------------------------------------------------------------------
    private static void deleteGameTable(Scanner scanner) {
        System.out.println("--- Decommission Game Table ---");
        System.out.print("Enter Table Code to delete (e.g. BJ-104): ");
        String tableCode = scanner.nextLine().trim();

        String sql = "DELETE FROM Game_Tables WHERE table_code = ?";

        try (Connection con = getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setString(1, tableCode);

            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                System.out.println("[SUCCESS] Table '" + tableCode + "' decommissioned and removed.");
            } else {
                System.out.println("[NOT FOUND] Table code '" + tableCode + "' does not exist.");
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Cannot delete table: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // 5. CALLABLE: Execute Stored Procedure ProcessChipBuyIn
    // -------------------------------------------------------------------------
    private static void processChipBuyIn(Scanner scanner) {
        System.out.println("--- Process Chip Buy-In (Stored Procedure) ---");
        try {
            System.out.print("Enter Player ID: ");
            int playerId = Integer.parseInt(scanner.nextLine().trim());

            System.out.print("Enter Cashier Staff ID (e.g. 11 or 12): ");
            int cashierId = Integer.parseInt(scanner.nextLine().trim());

            System.out.print("Enter Buy-In Amount: ");
            BigDecimal amount = new BigDecimal(scanner.nextLine().trim());

            System.out.print("Apply Promotional Bonus? (yes/no): ");
            boolean giveBonus = scanner.nextLine().trim().toLowerCase().startsWith("y");

            String sql = "{CALL ProcessChipBuyIn(?, ?, ?, ?, ?)}";

            try (Connection con = getConnection();
                 CallableStatement cstmt = con.prepareCall(sql)) {

                cstmt.setInt(1, playerId);
                cstmt.setInt(2, cashierId);
                cstmt.setBigDecimal(3, amount);
                cstmt.setBoolean(4, giveBonus);
                cstmt.registerOutParameter(5, Types.VARCHAR);

                cstmt.execute();

                String status = cstmt.getString(5);
                System.out.println("\n[PROCEDURE RESULT] " + status);
            }
        } catch (NumberFormatException e) {
            System.out.println("[ERROR] Invalid number format entered.");
        } catch (SQLException e) {
            System.err.println("[ERROR] Stored procedure execution failed: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // 6. CASH-OUT SYSTEM: Test Trigger FlagForHighCashOut & Alert Notification
    // -------------------------------------------------------------------------
    private static void processCashOut(Scanner scanner) {
        System.out.println("--- Cash-Out System (AML Trigger Alert) ---");
        try {
            System.out.print("Enter Player ID: ");
            int playerId = Integer.parseInt(scanner.nextLine().trim());

            System.out.print("Enter Cashier Staff ID (e.g. 11 or 12): ");
            int cashierId = Integer.parseInt(scanner.nextLine().trim());

            System.out.print("Enter Cash-Out Amount: ");
            BigDecimal amount = new BigDecimal(scanner.nextLine().trim());

            try (Connection con = getConnection()) {
                // Check player's existing chip balance
                String checkSql = "SELECT chip_balance FROM Players WHERE player_id = ?";
                BigDecimal currentBalance;
                try (PreparedStatement checkStmt = con.prepareStatement(checkSql)) {
                    checkStmt.setInt(1, playerId);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next()) {
                            currentBalance = rs.getBigDecimal("chip_balance");
                        } else {
                            System.out.println("[NOT FOUND] Player ID " + playerId + " not found.");
                            return;
                        }
                    }
                }

                if (currentBalance.compareTo(amount) < 0) {
                    System.out.printf("[ERROR] Insufficient chips! Player balance: INR %,.2f | Requested: INR %,.2f\n",
                            currentBalance, amount);
                    return;
                }

                // Insert into Cage_Transactions with default is_flagged = FALSE.
                // MySQL BEFORE INSERT trigger 'FlagForHighCashOut' will automatically
                // set is_flagged = TRUE if amount meets/exceeds high cash-out threshold.
                String insertSql = "INSERT INTO Cage_Transactions (player_id, cashier_id, txn_type, amount, is_flagged) " +
                                   "VALUES (?, ?, 'CASH_OUT', ?, FALSE)";
                int txnId;
                try (PreparedStatement insertStmt = con.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    insertStmt.setInt(1, playerId);
                    insertStmt.setInt(2, cashierId);
                    insertStmt.setBigDecimal(3, amount);
                    insertStmt.executeUpdate();

                    try (ResultSet keys = insertStmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            txnId = keys.getInt(1);
                        } else {
                            System.out.println("[ERROR] Failed to record transaction.");
                            return;
                        }
                    }
                }

                // Query the inserted record to verify whether the trigger flagged it
                String verifySql = "SELECT is_flagged FROM Cage_Transactions WHERE txn_id = ?";
                boolean isFlagged = false;
                try (PreparedStatement verifyStmt = con.prepareStatement(verifySql)) {
                    verifyStmt.setInt(1, txnId);
                    try (ResultSet rs = verifyStmt.executeQuery()) {
                        if (rs.next()) {
                            isFlagged = rs.getBoolean("is_flagged");
                        }
                    }
                }

                // If flagged by the trigger: send an alert notification and DO NOT complete payout
                if (isFlagged) {
                    System.out.println();
                    System.out.println("************************************************************");
                    System.out.println("               *** ALERT NOTIFICATION ***                   ");
                    System.out.println("  SUSPICIOUS HIGH VALUE CASH-OUT FLAGGED BY AML TRIGGER!     ");
                    System.out.println("------------------------------------------------------------");
                    System.out.printf ("  Transaction ID : %d\n", txnId);
                    System.out.printf ("  Player ID      : %d\n", playerId);
                    System.out.printf ("  Cash-Out Amount: INR %,.2f\n", amount);
                    System.out.println("  Trigger Flag   : is_flagged = 1 (FLAGGED)");
                    System.out.println("  NOTIFICATION   : TRANSACTION WILL NOT BE DONE.");
                    System.out.println("                   Audit review required before release.");
                    System.out.println("************************************************************");
                } else {
                    // Normal transaction: deduct chip balance from player
                    String updateSql = "UPDATE Players SET chip_balance = chip_balance - ? WHERE player_id = ?";
                    try (PreparedStatement updateStmt = con.prepareStatement(updateSql)) {
                        updateStmt.setBigDecimal(1, amount);
                        updateStmt.setInt(2, playerId);
                        updateStmt.executeUpdate();
                    }

                    BigDecimal remaining = currentBalance.subtract(amount);
                    System.out.println("\n[SUCCESS] Cash-out transaction processed successfully.");
                    System.out.printf("  Transaction ID : %d\n", txnId);
                    System.out.printf("  Amount Paid    : INR %,.2f\n", amount);
                    System.out.printf("  Remaining Chips: INR %,.2f\n", remaining);
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("[ERROR] Invalid number format entered.");
        } catch (SQLException e) {
            System.err.println("[ERROR] Database error during cash-out: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // 7. TRANSACTION: Chip Transfer with ACID Control (Commit & Rollback)
    // -------------------------------------------------------------------------
    private static void transferChipsTransaction(Scanner scanner) {
        System.out.println("--- Transfer Chips Between Players (Transaction Demo) ---");
        try {
            System.out.print("Enter Sender Player ID: ");
            int senderId = Integer.parseInt(scanner.nextLine().trim());

            System.out.print("Enter Receiver Player ID: ");
            int receiverId = Integer.parseInt(scanner.nextLine().trim());

            if (senderId == receiverId) {
                System.out.println("[ERROR] Sender and receiver player IDs cannot be identical.");
                return;
            }

            System.out.print("Enter Chip Transfer Amount: ");
            BigDecimal amount = new BigDecimal(scanner.nextLine().trim());

            System.out.print("Simulate system/network failure during transfer? (yes/no): ");
            boolean simulateError = scanner.nextLine().trim().toLowerCase().startsWith("y");

            Connection con = null;
            try {
                con = getConnection();
                // Disable auto-commit to start transaction
                con.setAutoCommit(false);

                // Step 1: Deduct from sender
                String deductSql = "UPDATE Players SET chip_balance = chip_balance - ? WHERE player_id = ? AND chip_balance >= ?";
                try (PreparedStatement deductStmt = con.prepareStatement(deductSql)) {
                    deductStmt.setBigDecimal(1, amount);
                    deductStmt.setInt(2, senderId);
                    deductStmt.setBigDecimal(3, amount);
                    int rows = deductStmt.executeUpdate();
                    if (rows == 0) {
                        throw new SQLException("Sender has insufficient chips or does not exist.");
                    }
                }

                // Step 2: Simulate failure if requested
                if (simulateError) {
                    throw new RuntimeException("Simulated crash! Connection lost before receiver could be credited.");
                }

                // Step 3: Credit to receiver
                String creditSql = "UPDATE Players SET chip_balance = chip_balance + ? WHERE player_id = ?";
                try (PreparedStatement creditStmt = con.prepareStatement(creditSql)) {
                    creditStmt.setBigDecimal(1, amount);
                    creditStmt.setInt(2, receiverId);
                    int rows = creditStmt.executeUpdate();
                    if (rows == 0) {
                        throw new SQLException("Receiver does not exist.");
                    }
                }

                // Step 4: Commit transaction
                con.commit();
                System.out.println("\n[SUCCESS] Transaction committed successfully!");
                System.out.printf("  Transferred INR %,.2f from Player %d to Player %d\n", amount, senderId, receiverId);

            } catch (Exception e) {
                System.err.println("\n[ERROR] Transaction aborted: " + e.getMessage());
                if (con != null) {
                    try {
                        con.rollback();
                        System.err.println("[ROLLBACK] con.rollback() called. Balances restored to original state.");
                    } catch (SQLException rollbackEx) {
                        System.err.println("Rollback failed: " + rollbackEx.getMessage());
                    }
                }
            } finally {
                if (con != null) {
                    try {
                        con.setAutoCommit(true);
                        con.close();
                    } catch (SQLException ex) {
                        System.err.println("Error closing connection: " + ex.getMessage());
                    }
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("[ERROR] Invalid number format entered.");
        }
    }

    // -------------------------------------------------------------------------
    // 8. ANALYTICAL: Grouped Floor Inventory Summary on Game_Tables
    // -------------------------------------------------------------------------
    private static void viewFloorInventoryReport() {
        System.out.println("--- Casino Floor Inventory Report (Analytical) ---");
        String sql = "SELECT game_type, " +
                     "       COUNT(table_id) AS total_tables, " +
                     "       MIN(min_bet) AS lowest_min_bet, " +
                     "       MAX(max_bet) AS highest_max_bet, " +
                     "       SUM(table_chip_float) AS total_chip_inventory, " +
                     "       AVG(table_chip_float) AS avg_table_float " +
                     "FROM Game_Tables " +
                     "GROUP BY game_type " +
                     "ORDER BY total_chip_inventory DESC";

        try (Connection con = getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            System.out.printf("%-14s %-14s %-16s %-16s %-20s %-18s\n",
                    "Game Type", "Total Tables", "Min Bet (INR)", "Max Bet (INR)", "Total Float (INR)", "Avg Float (INR)");
            System.out.println("------------------------------------------------------------------------------------------------------");

            while (rs.next()) {
                System.out.printf("%-14s %-14d %-16.2f %-16.2f %-20.2f %-18.2f\n",
                        rs.getString("game_type"),
                        rs.getInt("total_tables"),
                        rs.getBigDecimal("lowest_min_bet"),
                        rs.getBigDecimal("highest_max_bet"),
                        rs.getBigDecimal("total_chip_inventory"),
                        rs.getBigDecimal("avg_table_float"));
            }
            System.out.println("------------------------------------------------------------------------------------------------------");

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
    }
}
