# Ứng Dụng Quản Lý Đấu Giá Trực Tuyến (VinAuction)

## Nhóm 1 - K70I-CS5
- Ngô Đại Lâm - MSV 25021839
- Nguyễn Tri Khoa - MSV 25021835
- Nguyễn Đình Huy - MSV 25021799
- Trịnh Minh Khánh - MSV 25021831

## Video Demo: https://youtu.be/5gwSjSRg0VY

## Báo cáo dự án (PDF): https://drive.google.com/file/d/1kAx3t-fHDYYuSRzmM1lEXdKvmI9J2uyX/view?usp=sharing

## Giới Thiệu

Ứng dụng quản lý đấu giá trực tuyến hiện đại, cho phép người dùng tham gia đấu giá, mua bán hàng hóa, và quản lý tài khoản của mình. Ứng dụng được xây dựng với kiến trúc client - server, sử dụng JavaFX cho giao diện người dùng và PostgreSQL cho lưu trữ dữ liệu.

### Đặc Điểm Nổi Bật
- Giao diện đồ họa hiện đại với JavaFX
- Hệ thống xác thực và phân quyền người dùng
- Quản lý ví điện tử
- Theo dõi đấu giá theo thời gian thực
- Các vai trò người dùng khác nhau (Admin, Người Bán, Người Mua)
- Biểu đồ thống kê đấu giá
- Âm thanh nền và hiệu ứng âm thanh
- Tính năng đặt giá tự động (Auto-Bid)
- Xử lí lỗi và thông báo người dùng thân thiện
- Tính năng bảo mật nâng cao (mã hóa mật khẩu, câu hỏi bảo mật)
- Xử lí ngoại lệ và logic chi tiết

---

## Tính Năng Chính

### Cho Người Dùng
- **Đăng Ký & Đăng Nhập**: Tạo tài khoản mới hoặc đăng nhập vào tài khoản hiện có
- **Quay Lại Mật Khẩu**: Phục hồi mật khẩu thông qua câu hỏi bảo mật
- **Tham Gia Đấu Giá**: Tham gia các cuộc đấu giá và đặt giá
- **Quản Lý Tài Khoản**: Cập nhật thông tin cá nhân
- **Quản Lý Ví**: Nạp tiền vào ví điện tử

### Cho Người Bán
- **Tạo Cuộc Đấu Giá**: Đăng ký những mục hàng để đấu giá
- **Quản Lý Đấu Giá**: Theo dõi tình trạng các cuộc đấu giá
- **Nhận Khoản Thanh Toán**: Nhận tiền từ các cuộc đấu giá thành công

### Cho Quản Trị Viên
- **Quản Lý Người Dùng**: Xem danh sách tất cả người dùng
- **Quản Lý Đấu Giá**: Giám sát tất cả cuộc đấu giá
- **Thống Kê**: Xem các biểu đồ thống kê chi tiết

---

## Yêu Cầu Hệ Thống

### Phần Mềm Cần Thiết
- **Java JDK 17+**: Công nghệ chính của ứng dụng
- **Apache Maven 3.6.0+**: Công cụ xây dựng dự án
- **PostgreSQL 12+**: Cơ sở dữ liệu chính
- **Git**: Để clone dự án

### Phần Cứng
- **CPU**: Processor hiện đại (Intel i5 hoặc tương đương)
- **RAM**: Tối thiểu 4GB (8GB khuyến nghị)
- **Ổ Cứng**: Tối thiểu 2GB không gian trống
- **Kết Nối Mạng**: Internet để kết nối client-server

### Hệ Điều Hành Hỗ Trợ
- Windows 10/11
- macOS 10.15+
- Linux (Ubuntu 20.04+)

---

## Cấu Trúc Dự Án

```
baitap/
├── pom.xml                              # File cấu hình Maven chính
├── shared/                              # Module dùng chung (Models, Enums, Network)
│   ├── pom.xml
│   └── src/
│       ├── main/java/shared/
│       │   ├── enums/                  # Các enum dùng chung
│       │   ├── models/                 # Các model/entity dùng chung
│       │   ├── network/                # Các class network (Protocol, Message)
│       │   └── utils/                  # Tiện ích chung
│       └── test/java/shared/
├── client/                              # Module client (JavaFX Desktop)
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/client/
│       │   │   ├── Launcher.java       # Điểm khởi động
│       │   │   ├── MainApp.java        # Ứng dụng JavaFX chính
│       │   │   ├── Navigator.java      # Quản lý điều hướng
│       │   │   ├── AppContext.java     # Bối cảnh ứng dụng
│       │   │   ├── controller/         # Controllers cho các màn hình
│       │   │   ├── service/            # Dịch vụ client
│       │   │   └── support/            # Hỗ trợ (utils, helpers)
│       │   └── resources/
│       │       ├── *.fxml              # File bố cục JavaFX
│       │       ├── styles.css          # Stylesheet CSS
│       │       ├── *.png               # Hình ảnh
│       │       └── sounds/             # Tệp âm thanh
│       └── test/java/client/
├── server/                              # Module server (Socket-based)
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   └── java/server/
│       │       ├── ServerApplication.java  # Main server
│       │       ├── controller/             # Request handlers
│       │       ├── database/               # Database config & initializer
│       │       └── service/                # Business logic services
│       └── test/java/server/
├── init_server.sql                      # Khởi tạo database bằng SQL
└── README.md                            # File này

```

---

## Hướng Dẫn Cài Đặt

### Bước 1: Chuẩn Bị Môi Trường

#### Trên Windows:

1. **Cài đặt Java JDK 17**
   ```powershell
   # Tải từ: https://www.oracle.com/java/technologies/downloads/
   # Hoặc dùng WinGet (nếu có)
   winget install Oracle.JDK.17
   ```

2. **Cài đặt Maven**
   ```powershell
   # Tải từ: https://maven.apache.org/download.cgi
   # Hoặc dùng Chocolatey
   choco install maven
   ```

3. **Xác nhận cài đặt**
   ```powershell
   java -version
   mvn -version
   ```

#### Trên macOS:

```bash
# Cài đặt Homebrew (nếu chưa có)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Cài đặt Java JDK 17 và Maven
brew install openjdk@17 maven

# Xác nhận
java -version
mvn -version
```

#### Trên Linux (Ubuntu):

```bash
# Cập nhật package manager
sudo apt update

# Cài đặt Java JDK 17
sudo apt install openjdk-17-jdk

# Cài đặt Maven
sudo apt install maven

# Xác nhận
java -version
mvn -version
```

### Bước 2: Clone Dự Án

```powershell
# Clone dự án (nếu trên Git)
git clone https://github.com/kendicklmao/taifxiur
```

### Bước 3: Cài Đặt Cơ Sở Dữ Liệu

#### Sử Dụng PostgreSQL & Supabase

1. **Cài đặt PostgreSQL 12+**
   - Tải từ: https://www.postgresql.org/download/

2. **Tạo cơ sở dữ liệu**

3. **Cập nhật file cấu hình database** tại:
   ```
   server/src/main/java/server/database/DatabaseConfig.java
   ```

### Bước 4: Xây Dựng Dự Án

```powershell
# Xây dựng toàn bộ dự án
mvn clean install

# Hoặc chỉ xây dựng từng module
mvn clean install -pl shared
mvn clean install -pl server
mvn clean install -pl client
```

Quá trình xây dựng sẽ:
- Tải xuống tất cả các dependencies
- Biên dịch mã nguồn Java
- Chạy các bài kiểm tra
- Tạo các tệp JAR

---

## Hướng Dẫn Chạy Ứng Dụng

### Phương Pháp 1: Chạy từ Terminal

```powershell
# Xây dựng JAR
mvn clean package -DskipTests

# Chạy Server
java -jar server\target\server-1.0-SNAPSHOT.jar

# Chạy Client (trong window khác)
java -jar client\target\client-1.0-SNAPSHOT.jar
```

### Phương Pháp 2: Chạy từ IDE (IntelliJ IDEA / Eclipse)

#### IntelliJ IDEA:
1. Mở dự án trong IntelliJ
2. Chọn **File → Open** → Chọn thư mục `taifxiur`
3. **Run → Edit Configurations**
4. Tạo 2 configurations:
   - Server: Main class = `server.ServerApplication`
   - Client: Main class = `client.Launcher`
5. Chạy Server (trong ServerApplication.java) trước, sau đó chạy Client (Launcher.java)

#### Eclipse:
1. Mở dự án: **File → Import → Maven → Existing Maven Projects**
2. Chọn thư mục `taifxiur`
3. Click chuột phải vào project → **Run As → Maven Build**
4. Cấu hình tương tự

### Phương Pháp 3: Chạy Đa Instances Client

Bạn có thể mở nhiều client cùng một lúc:

```powershell
# Window 1: Server
java -jar server\target\server-1.0-SNAPSHOT.jar

# Window 2: Client 1
java -jar client\target\client-1.0-SNAPSHOT.jar

# Window 3: Client 2
java -jar client\target\client-1.0-SNAPSHOT.jar

# Window 4: Client 3
java -jar client\target\client-1.0-SNAPSHOT.jar
```

---

## Hướng Dẫn Sử Dụng

### Màn Hình Đăng Nhập

1. Khi ứng dụng khởi động, bạn sẽ thấy màn hình **Login**
2. Nhập tên đăng nhập và mật khẩu
3. Nhấp nút **Login** để đăng nhập
4. Nếu chưa có tài khoản, nhấp **Register**

### Màn Hình Đăng Ký

1. Nhập thông tin cá nhân:
   - Tên đầy đủ
   - Tên đăng nhập (unique)
   - Mật khẩu
   - Email
2. Chọn vai trò: **Seller** (Người Bán) hoặc **Bidder** (Người Mua)
3. Thiết lập câu hỏi bảo mật
4. Nhấp **Register** để tạo tài khoản

### Màn Hình Trang Chủ

#### Cho Người Bán (Seller Home):
- **Danh Sách Đấu Giá**: Xem các cuộc đấu giá đã tạo
- **Tạo Đấu Giá Mới**: Thêm mục hàng mới để đấu giá
- **Quản Lý Ví**: Nạp tiền hoặc xem số dư

#### Cho Người Mua (Bidder Home):
- **Danh Sách Đấu Giá Có Sẵn**: Xem tất cả cuộc đấu giá
- **Đặt Giá**: Tham gia đấu giá cho mục hàng yêu thích
- **Quản Lý Ví**: Nạp tiền hoặc xem số dư

#### Cho Quản Trị Viên (Admin Home):
- **Quản Lý Người Dùng**: Xem và quản lý tất cả người dùng
- **Quản Lý Đấu Giá**: Xem tất cả cuộc đấu giá
- **Thống Kê**: Xem biểu đồ và số liệu thống kê

### Tham Gia Đấu Giá

1. Xem danh sách đấu giá có sẵn
2. Chọn mục hàng muốn đấu giá
3. Nhập số tiền đặt giá
4. Nhấp **Place Bid** để đặt giá
5. Nhấp **Auto-Bid** để kích hoạt tính năng đặt giá tự động 
5. Xem lịch sử đấu giá thời gian thực

---

## Tài Khoản Mặc Định

Khi ứng dụng khởi động lần đầu, các tài khoản này sẽ được tạo tự động:

| Vai Trò | Tên Đăng Nhập | Mật Khẩu | Mô Tả |
|--------|---------------|---------|------|
| Admin  | admin         | Admin@123| Quản trị viên hệ thống |
| Seller | seller       | Seller@123 | Người bán hàng |
| Bidder | bidder       | Bidder@123 | Người mua/Người đấu giá |

**Lưu ý**: Vì lý do bảo mật, bạn nên thay đổi mật khẩu mặc định sau khi đăng nhập lần đầu.

---

## Cấu Trúc Cơ Sở Dữ Liệu

### Các Bảng Chính

#### 1. **users** - Bảng Người Dùng
```sql
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    email VARCHAR(255),
    role VARCHAR(50) NOT NULL,
    security_question VARCHAR(255),
    security_answer VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    is_online BOOLEAN DEFAULT FALSE
);
```

#### 2. **auctions** - Bảng Cuộc Đấu Giá
```sql
CREATE TABLE auctions (
    id SERIAL PRIMARY KEY,
    seller_id INTEGER NOT NULL REFERENCES users(id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    starting_price DECIMAL(10, 2),
    current_price DECIMAL(10, 2),
    status VARCHAR(50),
    image_url VARCHAR(255),
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    highest_name VARCHAR(255) DEFAULT NULL,
);
```

#### 3. **bids** - Bảng Giá Đấu
```sql
CREATE TABLE bids (
    id SERIAL PRIMARY KEY,
    auction_id INTEGER NOT NULL REFERENCES auctions(id),
    bidder_id INTEGER NOT NULL REFERENCES users(id),
    bid_amount DECIMAL(10, 2),
    bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 4. **wallets** - Bảng Ví Điện Tử
```sql
CREATE TABLE wallets (
    id SERIAL PRIMARY KEY,
    user_id INTEGER UNIQUE NOT NULL REFERENCES users(id),
    balance DECIMAL(10, 2) DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 5. **auto_bids** - Bảng Đặt Giá Tự Động
```sql
CREATE TABLE auto_bids (
    id SERIAL PRIMARY KEY,
    auction_id INTEGER NOT NULL REFERENCES auctions(id),
    bidder_id INTEGER NOT NULL REFERENCES users(id),
    max_bid_amount DECIMAL(10, 2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## Công Nghệ Sử Dụng

### Backend
- **Java 17**: Ngôn ngữ lập trình chính
- **Apache Maven**: Công cụ xây dựng
- **PostgreSQL**: Cơ sở dữ liệu (Supabase)
- **HikariCP**: Connection Pool cho database
- **AWS S3**: Lưu trữ hình ảnh trên Supabase Storage

### Frontend
- **JavaFX 17**: Framework UI
- **FXML**: Markup language cho layout
- **CSS**: Styling
- **GSON**: JSON serialization/deserialization

### Testing
- **JUnit 5**: Testing framework
- **Mockito**: Mocking library

### Networking
- **Java Socket**: Kết nối client-server
- **Custom Protocol**: Giao thức truyền thông tùy chỉnh
