# Supabase Integration Guide

## Overview
This project has been configured to use **Supabase PostgreSQL** as the backend database instead of in-memory storage. Supabase provides a secure, scalable cloud-hosted PostgreSQL database with automatic backups and monitoring.

## Configuration Details

### Supabase Connection Settings
**Using Session Pooler for IPv4 Compatibility**
- **Host**: `aws-1-ap-northeast-1.pooler.supabase.com`
- **Port**: `5432`
- **Database**: `postgres`
- **User**: `postgres.uxmbyzqylbtuqyyatzwj`
- **Password**: `Hd0Ykh1LCtzbw4X6` 

**Note**: This configuration uses Supabase's **Session Pooler** endpoint instead of the direct database connection. The Session Pooler provides IPv4 compatibility and is recommended for most network environments, especially corporate networks with IPv4-only connectivity.

### Connection Pool (HikariCP)
- **Pool Name**: Database Connection Pool
- **Max Pool Size**: 10
- **Min Idle Connections**: 5
- **Connection Timeout**: 30 seconds
- **Idle Timeout**: 10 minutes
- **Max Lifetime**: 30 minutes

## Project Structure

### Database Configuration
**File**: `server/src/main/java/server/database/DatabaseConfig.java`
- Manages HikariCP connection pooling
- Initializes and maintains database connections
- Provides thread-safe access to connection pool

### Database Initialization
**File**: `server/src/main/java/server/database/DatabaseInitializer.java`
- Creates all required database tables on startup
- Initializes schema automatically
- Tables created:
  - `users` - User account information
  - `wallets` - User wallet balances
  - `auctions` - Auction listings
  - `items` - Item catalog
  - `bids` - Bid history
  - `auto_bids` - Automatic bid settings
  - `deposit_requests` - Wallet deposit requests
  - `withdraw_requests` - Wallet withdrawal requests

### Updated Services

#### UserService
**File**: `server/src/main/java/server/service/UserService.java`
- All user registration and login data stored in Supabase PostgreSQL
- Failed login attempts tracked in local cache (temporary)
- Account lockout mechanism retained
- Features:
  - User registration with validation
  - Login authentication
  - User banning/unbanning
  - User lookup and listing

#### WalletService
**File**: `server/src/main/java/server/service/WalletService.java`
- Deposit and withdrawal requests stored in database
- Wallet balance management
- Request approval/rejection tracking
- Features:
  - Create deposit requests
  - Create withdrawal requests
  - Approve/reject requests
  - Update wallet balances

#### AuctionService
**File**: `server/src/main/java/server/service/AuctionService.java`
- Auction data persisted to PostgreSQL
- Bid history tracking
- Auto-bid registration
- Features:
  - Create auctions
  - Place bids
  - Register auto-bids
  - Query auctions by status/seller

### Updated ServerApplication
**File**: `server/src/main/java/server/ServerApplication.java`
- Initializes database on server startup
- Gracefully closes connection pool on shutdown

## Dependencies Added

### pom.xml Updates
```xml
<!-- PostgreSQL JDBC Driver -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.1</version>
</dependency>

<!-- HikariCP Connection Pool -->
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>5.1.0</version>
</dependency>
```

### JDBC Connection String
```
jdbc:postgresql://aws-1-ap-northeast-1.pooler.supabase.com:5432/postgres?sslmode=require&tcpKeepAlives=true
```

**Authentication**:
- User: `postgres.uxmbyzqylbtuqyyatzwj`
- Password: `Hd0Ykh1LCtzbw4X6`

## Database Tables

CREATE TABLE users (
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
);

CREATE TABLE wallets (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    balance DECIMAL(15, 2) DEFAULT 0,
    currency VARCHAR(10) DEFAULT 'USD',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE auctions (
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
);

CREATE TABLE items (
    id SERIAL PRIMARY KEY,
    seller_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    status VARCHAR(50) DEFAULT 'AVAILABLE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE bids (
    id SERIAL PRIMARY KEY,
    auction_id INTEGER NOT NULL REFERENCES auctions(id) ON DELETE CASCADE,
    bidder_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    bid_amount DECIMAL(15, 2) NOT NULL,
    bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE auto_bids (
    id SERIAL PRIMARY KEY,
    auction_id INTEGER NOT NULL REFERENCES auctions(id) ON DELETE CASCADE,
    bidder_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    max_bid_amount DECIMAL(15, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE deposit_requests (
    id VARCHAR(36) PRIMARY KEY,
    bidder_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount DECIMAL(15, 2) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE withdraw_requests (
    id VARCHAR(36) PRIMARY KEY,
    seller_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount DECIMAL(15, 2) NOT NULL,
    bank_account VARCHAR(255),
    status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


## Security Notes

⚠️ **IMPORTANT**: The database credentials are currently hardcoded. For production deployment:

1. **Move credentials to environment variables**:
   ```java
   String dbHost = System.getenv("DB_HOST");
   String dbPassword = System.getenv("DB_PASSWORD");
   ```

2. **Use configuration files** (application.yml, application.properties)

3. **Implement secrets management** (AWS Secrets Manager, HashiCorp Vault, etc.)

4. **Rotate passwords regularly** in Supabase dashboard

5. **Restrict network access** to specific IP addresses in Supabase

6. **Use SSL/TLS** for all database connections (already enabled by default)

## Usage

### Starting the Server
```bash
mvn clean install
mvn exec:java -Dexec.mainClass="server.ServerApplication"
```

The server will:
1. Initialize the HikariCP connection pool
2. Create all database tables (idempotent - safe to run multiple times)
3. Start listening for client connections on port 54321

### Example: Creating a User
```java
UserService userService = new UserService();
userService.register(
    "john_doe",
    "SecurePassword123",
    "john@example.com",
    "What is your favorite color?",
    "Blue",
    "What is your pet's name?",
    "Max",
    Role.BIDDER
);
```

The user data will be automatically persisted to Supabase PostgreSQL.

## Monitoring and Maintenance

### View Database Logs
1. Go to Supabase Dashboard
2. Navigate to "Logs" → "Database Logs"
3. Monitor connections, errors, and performance metrics

### Backup Strategy
- Supabase provides automatic daily backups
- Access via Supabase Dashboard → "Backups"
- Retains backups for 7 days (default)

### Performance Optimization
- Connection pool reduces overhead
- Prepared statements prevent SQL injection
- Indexes automatically created on foreign keys
- Consider adding indexes on frequently queried columns

## Troubleshooting

### IPv4/IPv6 Compatibility Issues

If you encounter DNS resolution errors like:
```
DNS lookup failed for host 'db.uxmbyzqylbtuqyyatzwj.supabase.co'
```

**Solution**: Use the **Session Pooler** endpoint instead:
- **Session Pooler Host**: `aws-1-ap-northeast-1.pooler.supabase.com`
- **Session Pooler User**: `postgres.{PROJECT_ID}` (e.g., `postgres.uxmbyzqylbtuqyyatzwj`)

The Session Pooler provides:
- ✅ IPv4 compatibility (recommended for corporate networks)
- ✅ Connection pooling
- ✅ Better performance
- ✅ Automatic connection management

The direct database connection may resolve to IPv6-only addresses in some network environments, which can cause connectivity issues. The Session Pooler is available by default in all Supabase projects.

### Connection Issues
```
Error: connection refused
- Check Supabase project is active
- Verify network connectivity
- Check firewall rules
```

### Authentication Errors
```
Error: role "postgres" does not exist
- Verify password in DatabaseConfig.java
- Check credentials in Supabase dashboard
```

### Pool Exhaustion
```
Error: Cannot get JDBC Connection
- Increase MAX_POOL_SIZE in DatabaseConfig
- Review connection leaks in code
- Check for long-running queries
```

## Next Steps

1. **Implement connection pooling monitoring** using HikariCP metrics
2. **Add database migrations** for future schema changes
3. **Create backup/restore procedures**
4. **Implement query caching** for frequently accessed data
5. **Add database auditing** for compliance
6. **Set up automatic alerts** for connection failures
7. **Implement transaction management** for complex operations

## References

- [Supabase Documentation](https://supabase.com/docs)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [HikariCP Documentation](https://github.com/brettwooldridge/HikariCP)
- [JDBC Best Practices](https://docs.oracle.com/javase/tutorial/jdbc/)

