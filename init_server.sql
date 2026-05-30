-- Consolidated SQL statements to initialize the server database

-- Table to mark DB schema version (checked by DatabaseInitializer)
CREATE TABLE IF NOT EXISTS db_version (
                                          version INTEGER PRIMARY KEY,
                                          applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Core users table
CREATE TABLE IF NOT EXISTS users (
                                     id SERIAL PRIMARY KEY,
                                     username VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    password_salt VARCHAR(255),
    email VARCHAR(255) UNIQUE,
    role VARCHAR(50) NOT NULL,
    question_1 VARCHAR(255),
    answer_1 VARCHAR(255),
    answer_salt_1 VARCHAR(255),
    question_2 VARCHAR(255),
    answer_2 VARCHAR(255),
    answer_salt_2 VARCHAR(255),
    is_banned BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Items table (also stores auction metadata via auction_id)
CREATE TABLE IF NOT EXISTS items (
                                     id SERIAL PRIMARY KEY,
                                     seller_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    status VARCHAR(50),
    base_price DECIMAL(14,2),
    current_price DECIMAL(14,2),
    seller_name VARCHAR(100),
    brand VARCHAR(100),
    item_status VARCHAR(50),
    model_year INTEGER,
    km_travel INTEGER,
    artist VARCHAR(255),
    year_created INTEGER,
    is_original BOOLEAN,
    image_url VARCHAR(512),
    min_increment DECIMAL(14,2) DEFAULT 0,
    auction_id VARCHAR(100), -- UUID string used by the app
    auction_status VARCHAR(50),
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Bids history
CREATE TABLE IF NOT EXISTS bids (
                                    id SERIAL PRIMARY KEY,
                                    auction_id VARCHAR(100) NOT NULL,
    bidder_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    bid_amount DECIMAL(14,2) NOT NULL,
    bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Auto-bid configurations
CREATE TABLE IF NOT EXISTS auto_bids (
                                         id SERIAL PRIMARY KEY,
                                         auction_id VARCHAR(100) NOT NULL,
    bidder_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    max_bid_amount DECIMAL(14,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (auction_id, bidder_id)
    );

-- Wallets
CREATE TABLE IF NOT EXISTS wallets (
                                       id SERIAL PRIMARY KEY,
                                       user_id INTEGER UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    balance DECIMAL(14,2) DEFAULT 0,
    currency VARCHAR(10) DEFAULT 'USD',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Deposit requests (submitted by bidders)
CREATE TABLE IF NOT EXISTS deposit_requests (
                                                id VARCHAR(100) PRIMARY KEY,
    bidder_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount DECIMAL(14,2) NOT NULL,
    bank_name VARCHAR(255),
    account_number VARCHAR(255),
    status VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Withdraw requests (submitted by sellers)
CREATE TABLE IF NOT EXISTS withdraw_requests (
                                                 id VARCHAR(100) PRIMARY KEY,
    seller_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount DECIMAL(14,2) NOT NULL,
    bank_name VARCHAR(255),
    account_number VARCHAR(255),
    status VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Wallet holds (reservation for bidding). Use composite PK to support ON CONFLICT upsert by (auction_id, bidder_id)
CREATE TABLE IF NOT EXISTS wallet_holds (
                                            auction_id VARCHAR(100) NOT NULL,
    bidder_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount DECIMAL(14,2) NOT NULL,
    status VARCHAR(50) DEFAULT 'HELD',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (auction_id, bidder_id)
    );

-- Admin login logs
CREATE TABLE IF NOT EXISTS admin_logs (
                                          id SERIAL PRIMARY KEY,
                                          user_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
    status VARCHAR(50),
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Admin action logs (ban/unban/etc.)
CREATE TABLE IF NOT EXISTS admin_action_logs (
                                                 id SERIAL PRIMARY KEY,
                                                 admin_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
    target_user_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100),
    action_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Optional: seed db_version row so DatabaseInitializer sees version=1 exists
INSERT INTO db_version (version) SELECT 1 WHERE NOT EXISTS (SELECT 1 FROM db_version WHERE version = 1);