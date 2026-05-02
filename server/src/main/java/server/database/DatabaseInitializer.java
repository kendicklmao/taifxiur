package server.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

// Khởi tạo cơ sở dữ liệu
public class DatabaseInitializer {

    public static void initializeDatabase() throws Exception {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                Statement stmt = conn.createStatement()) {
            System.out.println("Initializing database schema...");

            // Drop tables to ensure schema is updated
            stmt.execute("DROP TABLE IF EXISTS deposit_requests;");
            stmt.execute("DROP TABLE IF EXISTS withdraw_requests;");
            System.out.println("Dropped deposit and withdraw request tables for recreation.");

            // Tạo bảng users
            createUsersTable(stmt);

            // Tạo bảng wallets
            createWalletTable(stmt);

            // Tạo bảng items
            createItemsTable(stmt);

            // Tạo bảng bids
            createBidsTable(stmt);

            // Tạo bảng auto_bids
            createAutoBidsTable(stmt);

            // Tạo bảng deposit_requests
            createDepositRequestsTable(stmt);

            // Tạo bảng withdraw_requests
            createWithdrawRequestsTable(stmt);

            // Tạo bảng admin_logs
            createAdminLogsTable(stmt);

            // Tạo bảng admin_action_logs
            createAdminActionLogsTable(stmt);

            // Tạo bảng wallet_holds
            createWalletHoldsTable(stmt);

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
                    password_salt VARCHAR(255) NOT NULL,
                    email VARCHAR(255) UNIQUE NOT NULL,
                    role VARCHAR(50) NOT NULL,
                    is_banned BOOLEAN DEFAULT FALSE,
                    question_1 TEXT,
                    answer_1 TEXT,
                    answer_salt_1 VARCHAR(255),
                    question_2 TEXT,
                    answer_2 TEXT,
                    answer_salt_2 VARCHAR(255),
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

    private static void createItemsTable(Statement stmt) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS items (
                    id SERIAL PRIMARY KEY,
                    seller_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    name VARCHAR(255) NOT NULL,
                    description TEXT,
                    category VARCHAR(100),
                    status VARCHAR(50) DEFAULT 'AVAILABLE',
                    item_type VARCHAR(50),
                    base_price DECIMAL(15, 2),
                    current_price DECIMAL(15, 2),
                    legit_check BOOLEAN DEFAULT FALSE,
                    seller_name VARCHAR(255),
                    brand VARCHAR(255),
                    item_status VARCHAR(50),
                    model_year INTEGER,
                    km_travel INTEGER,
                    artist VARCHAR(255),
                    year_created INTEGER,
                    is_original BOOLEAN,
                    image_url TEXT,
                    min_increment DECIMAL(15, 2),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    auction_id VARCHAR(36) UNIQUE,
                    auction_status VARCHAR(50),
                    start_time TIMESTAMP,
                    end_time TIMESTAMP,
                    winner_id INTEGER REFERENCES users(id) ON DELETE SET NULL
                )
                """;
        stmt.execute(sql);
        System.out.println("Items table created or already exists");
    }

    private static void createBidsTable(Statement stmt) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS bids (
                    id SERIAL PRIMARY KEY,
                    auction_id VARCHAR(36) NOT NULL,
                    bidder_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    bid_amount DECIMAL(15, 2) NOT NULL,
                    bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (auction_id) REFERENCES items(auction_id) ON DELETE CASCADE
                )
                """;
        stmt.execute(sql);
        System.out.println("Bids table created or already exists");
    }

    private static void createAutoBidsTable(Statement stmt) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS auto_bids (
                    id SERIAL PRIMARY KEY,
                    auction_id VARCHAR(36) NOT NULL,
                    bidder_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    max_bid_amount DECIMAL(15, 2) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (auction_id) REFERENCES items(auction_id) ON DELETE CASCADE
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
                    bank_name VARCHAR(255),
                    account_number VARCHAR(255),
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
                    bank_name VARCHAR(255),
                    account_number VARCHAR(255),
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

    private static void createWalletHoldsTable(Statement stmt) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS wallet_holds (
                    id SERIAL PRIMARY KEY,
                    auction_id VARCHAR(36) NOT NULL,
                    bidder_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    amount DECIMAL(15, 2) NOT NULL,
                    status VARCHAR(50) DEFAULT 'HELD',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE(auction_id, bidder_id)
                )
                """;
        stmt.execute(sql);
        System.out.println("Wallet holds table created or already exists");
    }
}
