================================================================================
                    MALIBU CASINO MANAGEMENT SYSTEM
================================================================================

1. HOW TO RUN
--------------------------------------------------------------------------------
Open your command prompt or terminal in the project root directory:

Step 1: Compile the application
    javac -cp "lib/*" src/CasinoApp.java

Step 2: Run the application
    java -cp "src;lib/*" CasinoApp

(Note: On Linux or macOS, use a colon ':' instead of a semicolon ';' in the 
classpath: java -cp "src:lib/*" CasinoApp)


2. CONNECTION HANDLING
--------------------------------------------------------------------------------
The application connects to MySQL using the getConnection() method:
- It reads the database URL, username, and password from the "db.properties" 
  file using FileInputStream and Properties.
- This keeps sensitive passwords out of the Java source code.
- It then opens and returns a database connection using:
  DriverManager.getConnection(url, user, password).


3. CRUD OPERATIONS
--------------------------------------------------------------------------------
All database operations use PreparedStatement to securely execute queries and 
prevent SQL injection:

- viewAllPlayers (READ):
  Runs a SELECT query with NATURAL JOIN on Players and VIP_Tiers to display 
  all registered players, their balance, points, and VIP tier.

- registerPlayer (CREATE):
  Runs an INSERT query to add a new player. Uses Statement.RETURN_GENERATED_KEYS 
  to get and display the newly created player ID.

- updatePlayerBalance (UPDATE):
  Runs an UPDATE query to change a player's chip balance for a given player ID.

- deleteGameTable (DELETE):
  Runs a DELETE query to remove a table from Game_Tables using its table code.


4. STORED PROCEDURE EXECUTION
--------------------------------------------------------------------------------
The method callProcessBuyIn() (processChipBuyIn) runs the stored procedure 
ProcessChipBuyIn using CallableStatement:
- It prepares the call: {CALL ProcessChipBuyIn(?, ?, ?, ?, ?)}
- It sets 4 input parameters (player ID, cashier ID, amount, and bonus flag).
- It registers 1 output parameter (Types.VARCHAR) to capture the return message.
- After execution, it reads and displays the confirmation message.


5. TRANSACTION CONTROL (ACID)
--------------------------------------------------------------------------------
The method runTransactionDemo() (transferChipsTransaction) shows how to transfer 
chips safely between two players:
- setAutoCommit(false): Disables auto-commit so both steps run as one atomic 
  transaction.
- Step 1: Deducts chips from the sender.
- Step 2: Adds chips to the receiver.
- commit(): If both steps succeed, con.commit() saves the changes permanently.
- rollback(): If an error happens (or is simulated), con.rollback() restores 
  both balances back to their starting values so no chips are lost.


6. RESOURCE SAFETY
--------------------------------------------------------------------------------
All database calls are wrapped in try-with-resources blocks:
- Every Connection, PreparedStatement, CallableStatement, and ResultSet is 
  automatically closed when the block finishes.
- This happens even if an error occurs, preventing database connection leaks 
  and saving system memory.
================================================================================
