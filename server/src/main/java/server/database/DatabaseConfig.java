package server.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;


/**
 * Database configuration for Supabase PostgreSQL connection using HikariCP
 * <p>
 * This class reads connection info from environment variables if available.
 */
public class DatabaseConfig {
    private static volatile HikariDataSource dataSource;

    // Defaults (can be overridden with environment variables)
    // Using Supabase Session Pooler for IPv4 compatibility
    private static final String DEFAULT_DB_HOST = "aws-1-ap-northeast-1.pooler.supabase.com";
    private static final int DEFAULT_DB_PORT = 5432;
    private static final String DEFAULT_DB_NAME = "postgres";
    private static final String DEFAULT_DB_USER = "postgres.uxmbyzqylbtuqyyatzwj";
    private static final String DEFAULT_DB_PASSWORD = "Hd0Ykh1LCtzbw4X6";

    private DatabaseConfig() {
        // utility
    }

    /**
     * Lazy-initialize the HikariCP data source.
     * DNS resolution is handled by the PostgreSQL JDBC driver which supports IPv4 and IPv6.
     */
    public static HikariDataSource getDataSource() {
        if (dataSource == null) {
            synchronized (DatabaseConfig.class) {
                if (dataSource == null) {
                    initializeDataSource();
                }
            }
        }
        return dataSource;
    }

    private static void initializeDataSource() {
        String host = System.getenv("DB_HOST");
        if (host == null || host.isEmpty()) host = DEFAULT_DB_HOST;

        String portStr = System.getenv("DB_PORT");
        int port = DEFAULT_DB_PORT;
        if (portStr != null && !portStr.isEmpty()) {
            try { port = Integer.parseInt(portStr); } catch (NumberFormatException ignored) {}
        }

        String dbName = System.getenv("DB_NAME");
        if (dbName == null || dbName.isEmpty()) dbName = DEFAULT_DB_NAME;

        String user = System.getenv("DB_USER");
        if (user == null || user.isEmpty()) user = DEFAULT_DB_USER;

        String password = System.getenv("DB_PASSWORD");
        if (password == null || password.isEmpty()) password = DEFAULT_DB_PASSWORD;

        String sslmode = System.getenv("DB_SSLMODE");
        if (sslmode == null || sslmode.isEmpty()) sslmode = "require";

        // Build JDBC URL - DNS resolution is handled by the PostgreSQL driver which supports IPv4 and IPv6
        String jdbcUrl = String.format(
            "jdbc:postgresql://%s:%d/%s?sslmode=%s&tcpKeepAlives=true&loggerLevel=OFF",
            host, port, dbName, sslmode
        );

        System.out.println("Connecting to database at " + host + ":" + port);


        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setAutoCommit(true);

        try {
            dataSource = new HikariDataSource(config);
            System.out.println("Database connection pool initialized successfully");
        } catch (Exception e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Close the connection pool
     */
    public static void closeDataSource() {
        if (dataSource != null) {
            dataSource.close();
            System.out.println("Database connection pool closed");
        }
    }
}

