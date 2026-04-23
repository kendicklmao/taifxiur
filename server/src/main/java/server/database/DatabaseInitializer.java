package server.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database schema initialization for Supabase
 */
public class DatabaseInitializer {

    public static void initializeDatabase() throws Exception {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             Statement stmt = conn.createStatement()) {

            System.out.println("Initializing database schema...");

            // Create users table
            createUsersTable(stmt);

            // Create wallet table
            createWalletTable(stmt);

            // Create auction table
            createAuctionTable(stmt);

            // Create items table
            createItemsTable(stmt);

            // Create bids table
            createBidsTable(stmt);

            // Create auto_bids table
            createAutoBidsTable(stmt);

            // Create deposit_requests table
            createDepositRequestsTable(stmt);

            // Create withdraw_requests table
            createWithdrawRequestsTable(stmt);

            // Create admin_logs table
            createAdminLogsTable(stmt);

            // Create admin_action_logs table
            createAdminActionLogsTable(stmt);

            System.out.println("Database schema initialized successfully");

        } catch (SQLException e) {
            System.err.println("SQL error initializing database: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Database initialization failed: " + e.getMessage(), e);
        }
    }

    private static void createUsersTable(Statement stmt) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS users (
                    id SERIAL PRIMARY KEY,
                    username VARCHAR(255) UNIQUE NOT NULL,
                    password VARCHAR(255) NOT NULL,
                    email VARCHAR(255) UNIQUE NOT NULL,
                    role VARCHAR(50) NOT NULL,
                    is_banned BOOLEAN DEFAULT FALSE,
                    question_1 TEXT,
                    answer_1 TEXT,
                    question_2 TEXT,
                    answer_2 TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
        stmt.execute(sql);
        System.out.println("Users table created or already exists");
    }

    private static void createWalletTable(Statement stmt) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS wallets (
                    id SERIAL PRIMARY KEY,
                    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    balance DECIMAL(15, 2) DEFAULT 0,
                    currency VARCHAR(10) DEFAULT 'USD',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
        stmt.execute(sql);
        System.out.println("Wallets table created or already exists");
    }

    private static void createAuctionTable(Statement stmt) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS auctions (
                    id SERIAL PRIMARY KEY,
                    item_id INTEGER NOT NULL,
                    seller_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    start_price DECIMAL(15, 2) NOT NULL,
                    current_price DECIMAL(15, 2) NOT NULL,
                    status VARCHAR(50) DEFAULT 'UPCOMING',
                    start_time TIMESTAMP NOT NULL,
                    end_time TIMESTAMP NOT NULL,
                    winner_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
        stmt.execute(sql);
        System.out.println("Auctions table created or already exists");
    }

    private static void createItemsTable(Statement stmt) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS items (
                    id SERIAL PRIMARY KEY,
                    seller_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    name VARCHAR(255) NOT NULL,
                    description TEXT,
                    category VARCHAR(100),
                    status VARCHAR(50) DEFAULT 'AVAILABLE',
                    start_time TIMESTAMP,
                    end_time TIMESTAMP,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
        stmt.execute(sql);
        System.out.println("Items table created or already exists");
    }

    private static void createBidsTable(Statement stmt) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS bids (
                    id SERIAL PRIMARY KEY,
                    auction_id INTEGER NOT NULL REFERENCES auctions(id) ON DELETE CASCADE,
                    bidder_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    bid_amount DECIMAL(15, 2) NOT NULL,
                    bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
        stmt.execute(sql);
        System.out.println("Bids table created or already exists");
    }

    private static void createAutoBidsTable(Statement stmt) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS auto_bids (
                    id SERIAL PRIMARY KEY,
                    auction_id INTEGER NOT NULL REFERENCES auctions(id) ON DELETE CASCADE,
                    bidder_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    max_bid_amount DECIMAL(15, 2) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
        stmt.execute(sql);
        System.out.println("Auto bids table created or already exists");
    }
    
    private static void createDepositRequestsTable(Statement stmt) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS deposit_requests (
                    id VARCHAR(36) PRIMARY KEY,
                    bidder_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    amount DECIMAL(15, 2) NOT NULL,
                    status VARCHAR(50) DEFAULT 'PENDING',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
        stmt.execute(sql);
        System.out.println("Deposit requests table created or already exists");
    }
    
    private static void createWithdrawRequestsTable(Statement stmt) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS withdraw_requests (
                    id VARCHAR(36) PRIMARY KEY,
                    seller_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    amount DECIMAL(15, 2) NOT NULL,
                    bank_account VARCHAR(255),
                    status VARCHAR(50) DEFAULT 'PENDING',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
        stmt.execute(sql);
        System.out.println("Withdraw requests table created or already exists");
    }

    private static void createAdminLogsTable(Statement stmt) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS admin_logs (
                    id SERIAL PRIMARY KEY,
                    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    status VARCHAR(50) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
        stmt.execute(sql);
        System.out.println("Admin logs table created or already exists");
    }

    private static void createAdminActionLogsTable(Statement stmt) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS admin_action_logs (
                    id SERIAL PRIMARY KEY,
                    admin_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    target_user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    action VARCHAR(50) NOT NULL,
                    action_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
        stmt.execute(sql);
        System.out.println("Admin action logs table created or already exists");
    }
}
