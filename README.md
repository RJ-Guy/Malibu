# Malibu Casino Management System

A simple Java application using JDBC to connect to a MySQL database and manage casino operations like players, chip balances, tables, and transactions.

---

## 1. How to Run

Open your command prompt or terminal in the project root directory (`D:\Malibu`):

### Step 1: Compile
```bash
javac -cp "lib/*" src/CasinoApp.java
```

### Step 2: Run
```bash
java -cp "src;lib/*" CasinoApp
```

> **Note:** On Linux or macOS, use a colon (`:`) instead of a semicolon (`;`) in the classpath:  
> `java -cp "src:lib/*" CasinoApp`

---

## 2. Connection Handling

The application connects to MySQL using the `getConnection()` method:
- It reads database details (`db.url`, `db.user`, and `db.password`) from `db.properties` using `FileInputStream` and `Properties`.
- This keeps database passwords safe and out of the Java source code.
- It then opens and returns a connection using:
  ```java
  DriverManager.getConnection(url, user, password);
  ```

---

## 3. CRUD Operations

All database operations use `PreparedStatement` with `?` placeholders to safely pass values and protect against SQL injection:

- **Read (`viewAllPlayers`)**:  
  Runs a `SELECT` query with `NATURAL JOIN` on `Players` and `VIP_Tiers` to list all registered players, their chip balance, comp points, and VIP tier.

- **Create (`registerPlayer`)**:  
  Runs an `INSERT` query to add a new player. Uses `Statement.RETURN_GENERATED_KEYS` to get and display the newly created `player_id`.

- **Update (`updatePlayerBalance`)**:  
  Runs an `UPDATE` query to modify a player's chip balance by their `player_id`.

- **Delete (`deleteGameTable`)**:  
  Runs a `DELETE` query to remove a table from `Game_Tables` using its `table_code`.

---

## 4. Stored Procedure Execution

The method `callProcessBuyIn()` (or `processChipBuyIn`) runs the stored procedure `sp_ProcessChipBuyIn` using `CallableStatement`:
- Prepares the call: `{CALL ProcessChipBuyIn(?, ?, ?, ?, ?)}`
- Sets 4 input values: Player ID, Cashier ID, Buy-in amount, and a bonus flag.
- Registers 1 output parameter (`Types.VARCHAR`) for the return status message.
- Reads and prints the returned confirmation message after execution.

---

## 5. Transaction Control (ACID)

The method `runTransactionDemo()` (or `transferChipsTransaction`) tests atomic chip transfers between two players:
1. **`setAutoCommit(false)`**: Turns off automatic saving so all transfer steps are treated as a single unit.
2. **Transfer Steps**: Deducts chips from the sender and adds them to the receiver.
3. **`commit()`**: If all steps succeed without errors, `con.commit()` saves the changes permanently.
4. **`rollback()`**: If an error occurs (or is simulated), `con.rollback()` undoes all steps and restores original balances, ensuring no chips are lost or duplicated.

---

## 6. Resource Safety

- All database operations are wrapped inside Java `try-with-resources` blocks.
- This ensures that every `Connection`, `PreparedStatement`, `CallableStatement`, and `ResultSet` is automatically closed as soon as the block finishes.
- Resources are cleaned up safely even if an error or exception occurs, preventing database connection leaks.
