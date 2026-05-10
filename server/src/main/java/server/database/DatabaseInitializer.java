package server.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

// Khởi tạo cơ sở dữ liệu
public class DatabaseInitializer {

    public static void initializeDatabase() throws Exception {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             Statement stmt = conn.createStatement()) {

            if (isDatabaseInitialized(conn)) {
                System.out.println("Database already initialized. Skipping schema creation.");
                return;
            }

            System.out.println("Initializing database schema...");

            // // Tạo bảng db_version để theo dõi phiên bản schema createDbVersionTable(stmt);
            //
            // // Tạo bảng users createUsersTable(stmt);
            //
            // // Tạo bảng wallets createWalletTable(stmt);
            //
            // // Tạo bảng items createItemsTable(stmt);
            //
            // // Tạo bảng bids createBidsTable(stmt);
            //
            // // Tạo bảng auto_bids createAutoBidsTable(stmt);
            //
            // // Tạo bảng deposit_requests createDepositRequestsTable(stmt);
            //
            // // Tạo bảng withdraw_requests createWithdrawRequestsTable(stmt);
            //
            // // Tạo bảng admin_logs createAdminLogsTable(stmt);
            //
            // // Tạo bảng admin_action_logs createAdminActionLogsTable(stmt);
            //
            // // Tạo bảng wallet_holds createWalletHoldsTable(stmt);
            //
            // // Đánh dấu là đã khởi tạo stampDatabaseVersion(conn);
            //
            // System.out.println("Database schema initialized successfully");

        } catch (SQLException e) {
            System.err.println("SQL error initializing database: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Database initialization failed: " + e.getMessage(), e);
        }
    }

    private static boolean isDatabaseInitialized(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            // Kiểm tra sự tồn tại của bảng db_version
            stmt.executeQuery("SELECT 1 FROM db_version WHERE version = 1");
            return true; // Nếu không có lỗi, bảng đã tồn tại và có version = 1
        } catch (SQLException e) {
            return false; // Bảng không tồn tại hoặc có lỗi
        }
    }
}