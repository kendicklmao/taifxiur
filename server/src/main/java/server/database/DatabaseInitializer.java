package server.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void initializeDatabase() throws Exception {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            if (isDatabaseInitialized(conn)) {
                System.out.println("Database already initialized. Skipping schema creation.");
                return;
            }
            System.out.println("Initializing database schema...");
        } catch (SQLException e) {
            System.err.println("SQL error initializing database: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Database initialization failed: " + e.getMessage(), e);
        }
    }

    private static boolean isDatabaseInitialized(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeQuery("SELECT 1 FROM db_version WHERE version = 1");
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}