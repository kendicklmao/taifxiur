# Ứng Dụng Quản Lý Đấu Giá Trực Tuyến (Auction House) - LTNC Nhóm 1

## Nhóm 1 - K70I-CS5
- Ngô Đại Lâm - MSV 25021839
- Nguyễn Tri Khoa - MSV 25021835
- Nguyễn Đình Huy - MSV 25021799
- Trịnh Minh Khánh - MSV 25021831

## Giới Thiệu

Ứng dụng quản lý đấu giá trực tuyến hiện đại, cho phép người dùng tham gia đấu giá, mua bán hàng hóa, và quản lý tài khoản của mình. Ứng dụng được xây dựng với kiến trúc client-server, sử dụng JavaFX cho giao diện người dùng và PostgreSQL cho lưu trữ dữ liệu.

### Đặc Điểm Nổi Bật
- Giao diện đồ họa hiện đại với JavaFX
- Hệ thống xác thực và phân quyền người dùng
- Quản lý ví điện tử
- Theo dõi đấu giá theo thời gian thực
- Các vai trò người dùng khác nhau (Admin, Người Bán, Người Mua)
- Biểu đồ thống kê đấu giá
- Âm thanh nền và hiệu ứng âm thanh

---

## Tính Năng Chính

### Cho Người Dùng
- *Đăng Ký & Đăng Nhập*: Tạo tài khoản mới hoặc đăng nhập vào tài khoản hiện có
- *Quay Lại Mật Khẩu*: Phục hồi mật khẩu thông qua câu hỏi bảo mật
- *Tham Gia Đấu Giá*: Tham gia các cuộc đấu giá và đặt giá
- *Quản Lý Tài Khoản*: Cập nhật thông tin cá nhân
- *Quản Lý Ví*: Nạp tiền vào ví điện tử

### Cho Người Bán
- *Tạo Cuộc Đấu Giá*: Đăng ký những mục hàng để đấu giá
- *Quản Lý Đấu Giá*: Theo dõi tình trạng các cuộc đấu giá
- *Nhận Khoản Thanh Toán*: Nhận tiền từ các cuộc đấu giá thành công

### Cho Quản Trị Viên
- *Quản Lý Người Dùng*: Xem danh sách tất cả người dùng
- *Quản Lý Đấu Giá*: Giám sát tất cả cuộc đấu giá
- *Thống Kê*: Xem các biểu đồ thống kê chi tiết

---

## Yêu Cầu Hệ Thống

### Phần Mềm Cần Thiết
- *Java JDK 17+*: Công nghệ chính của ứng dụng
- *Apache Maven 3.6.0+*: Công cụ xây dựng dự án
- *PostgreSQL 12+*: Cơ sở dữ liệu chính
- *Git*: Để clone dự án

### Phần Cứng
- *CPU*: Processor hiện đại (Intel i5 hoặc tương đương)
- *RAM*: Tối thiểu 4GB (8GB khuyến nghị)
- *Ổ Cứng*: Tối thiểu 2GB không gian trống
- *Kết Nối Mạng*: Internet để kết nối client-server

### Hệ Điều Hành Hỗ Trợ
- Windows 10/11
- macOS 10.15+
- Linux (Ubuntu 20.04+)

---

## Cấu Trúc Dự Án

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


---

## Hướng Dẫn Cài Đặt

### Bước 1: Chuẩn Bị Môi Trường

#### Trên Windows:

1. *Cài đặt Java JDK 17*

   # Tải từ: https://www.oracle.com/java/technologies/downloads/
   # Hoặc dùng WinGet (nếu có)
   winget install Oracle.JDK.17


2. *Cài đặt Maven*

   # Tải từ: https://maven.apache.org/download.cgi
   # Hoặc dùng Chocolatey
   choco install maven


3. *Xác nhận cài đặt*

   java -version
   mvn -version


#### Trên macOS:

# Cài đặt Homebrew (nếu chưa có)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Cài đặt Java JDK 17 và Maven
brew install openjdk@17 maven

# Xác nhận
java -version
mvn -version

#### Trên Linux (Ubuntu):

# Cập nhật package manager
sudo apt update

# Cài đặt Java JDK 17
sudo apt install openjdk-17-jdk

# Cài đặt Maven
sudo apt install maven

# Xác nhận
java -version
mvn -version

### Bước 2: Clone Dự Án

# Clone dự án (nếu trên Git)
git clone https://github.com/kendicklmao/taifxiur

### Bước 3: Cài Đặt Cơ Sở Dữ Liệu

#### Sử Dụng PostgreSQL & Supabase

1. *Cài đặt PostgreSQL 12+*
    - Tải từ: https://www.postgresql.org/download/

2. *Tạo cơ sở dữ liệu*

   CREATE DATABASE auction_platform;


3. *Cập nhật file cấu hình database* tại:

   server/src/main/java/server/database/DatabaseConfig.java


### Bước 4: Xây Dựng Dự Án

# Xây dựng toàn bộ dự án
mvn clean install

# Hoặc chỉ xây dựng từng module
mvn clean install -pl shared
mvn clean install -pl server
mvn clean install -pl client

Quá trình xây dựng sẽ:
- Tải xuống tất cả các dependencies
- Biên dịch mã nguồn Java
- Chạy các bài kiểm tra
- Tạo các tệp JAR

---

## Hướng Dẫn Chạy Ứng Dụng

### Phương Pháp 1: Chạy Server và Client Cùng Lúc (Windows PowerShell)

# Mở 2 PowerShell windows

# Window 1: Chạy Server
mvn -pl server exec:java -Dexec.mainClass="server.ServerApplication"

# Window 2: Chạy Client
mvn -pl client exec:java -Dexec.mainClass="client.Launcher"

### Phương Pháp 2: Chạy từ JAR File

# Xây dựng JAR
mvn clean package

# Chạy Server
java -jar server\target\server-1.0-SNAPSHOT.jar

# Chạy Client (trong window khác)
java -jar client\target\client-1.0-SNAPSHOT.jar

### Phương Pháp 3: Chạy từ IDE (IntelliJ IDEA / Eclipse)

#### IntelliJ IDEA:
1. Mở dự án trong IntelliJ
2. Chọn *File → Open* → Chọn thư mục taifxiur
3. *Run → Edit Configurations*
4. Tạo 2 configurations:
    - Server: Main class = server.ServerApplication
    - Client: Main class = client.Launcher
5. Chạy Server (trong ServerApplication.java) trước, sau đó chạy Client (Launcher.java)

#### Eclipse:
1. Mở dự án: *File → Import → Maven → Existing Maven Projects*
2. Chọn thư mục taifxiur
3. Click chuột phải vào project → *Run As → Maven Build*
4. Cấu hình tương tự

### Phương Pháp 4: Chạy Đa Instances Client

Bạn có thể mở nhiều client cùng một lúc:

# Window 1: Server
mvn -pl server exec:java -Dexec.mainClass="server.ServerApplication"

# Window 2: Client 1
mvn -pl client exec:java -Dexec.mainClass="client.Launcher"

# Window 3: Client 2
mvn -pl client exec:java -Dexec.mainClass="client.Launcher"

# Window 4: Client 3
mvn -pl client exec:java -Dexec.mainClass="client.Launcher"

---

## Hướng Dẫn Sử Dụng

### Màn Hình Đăng Nhập

1. Khi ứng dụng khởi động, bạn sẽ thấy màn hình *Login*
2. Nhập tên đăng nhập và mật khẩu
3. Nhấp nút *Login* để đăng nhập
4. Nếu chưa có tài khoản, nhấp *Register*

### Màn Hình Đăng Ký

1. Nhập thông tin cá nhân:
    - Tên đầy đủ
    - Tên đăng nhập (unique)
    - Mật khẩu
    - Email
2. Chọn vai trò: *Seller* (Người Bán) hoặc *Bidder* (Người Mua)
3. Thiết lập câu hỏi bảo mật
4. Nhấp *Register* để tạo tài khoản

### Màn Hình Trang Chủ

#### Cho Người Bán (Seller Home):
- *Danh Sách Đấu Giá*: Xem các cuộc đấu giá của bạn
- *Tạo Đấu Giá Mới*: Thêm mục hàng mới để đấu giá
- *Quản Lý Ví*: Nạp tiền hoặc xem số dư

#### Cho Người Mua (Bidder Home):
- *Danh Sách Đấu Giá Có Sẵn*: Xem tất cả cuộc đấu giá
- *Đặt Giá*: Tham gia đấu giá cho mục hàng yêu thích
- *Lịch Sử Đấu Giá*: Xem các cuộc đấu giá đã tham gia
- *Quản Lý Ví*: Nạp tiền hoặc xem số dư

#### Cho Quản Trị Viên (Admin Home):
- *Quản Lý Người Dùng*: Xem và quản lý tất cả người dùng
- *Quản Lý Đấu Giá*: Xem tất cả cuộc đấu giá
- *Thống Kê*: Xem biểu đồ và số liệu thống kê
- *Báo Cáo*: Xuất báo cáo

### Tham Gia Đấu Giá

1. Xem danh sách đấu giá có sẵn
2. Chọn mục hàng muốn đấu giá
3. Nhập số tiền đặt giá
4. Nhấp *Place Bid* để đặt giá
5. Xem câu chuyện đấu giá thời gian thực

---

## Tài Khoản Mặc Định

Khi ứng dụng khởi động lần đầu, các tài khoản này sẽ được tạo tự động:

| Vai Trò | Tên Đăng Nhập | Mật Khẩu | Mô Tả |
|--------|---------------|---------|------|
| Admin  | admin         | Admin@123| Quản trị viên hệ thống |
| Seller | seller       | Seller@123 | Người bán hàng |
| Bidder | bidder       | Bidder@123 | Người mua/Người đấu giá |

*Lưu ý*: Vì lý do bảo mật, bạn nên thay đổi mật khẩu mặc định sau khi đăng nhập lần đầu.

---

## Cấu Trúc Cơ Sở Dữ Liệu

### Các Bảng Chính

#### 1. *users* - Bảng Người Dùng
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
);

#### 2. *auctions* - Bảng Cuộc Đấu Giá
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
);

#### 3. *bids* - Bảng Giá Đấu
CREATE TABLE bids (
id SERIAL PRIMARY KEY,
auction_id INTEGER NOT NULL REFERENCES auctions(id),
bidder_id INTEGER NOT NULL REFERENCES users(id),
bid_amount DECIMAL(10, 2),
bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

#### 4. *wallets* - Bảng Ví Điện Tử
CREATE TABLE wallets (
id SERIAL PRIMARY KEY,
user_id INTEGER UNIQUE NOT NULL REFERENCES users(id),
balance DECIMAL(10, 2) DEFAULT 0,
updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

---

## Công Nghệ Sử Dụng

### Backend
- *Java 17*: Ngôn ngữ lập trình chính
- *Apache Maven*: Công cụ xây dựng
- *PostgreSQL*: Cơ sở dữ liệu (Supabase)
- *HikariCP*: Connection Pool cho database
- *AWS S3*: Lưu trữ hình ảnh trên Supabase Storage

### Frontend
- *JavaFX 17*: Framework UI
- *FXML*: Markup language cho layout
- *CSS*: Styling
- *GSON*: JSON serialization/deserialization

### Testing
- *JUnit 5*: Testing framework
- *Mockito*: Mocking library

### Networking
- *Java Socket*: Kết nối client-server
- *Custom Protocol*: Giao thức truyền thông tùy chỉnh

---

## Khắc Phục Sự Cố

### Lỗi: "Port 54321 already in use"
*Giải pháp*:
# Tìm và kết thúc process đang dùng port 54321
netstat -ano | findstr :54321
# Kết thúc process
taskkill /PID <PID> /F

### Lỗi: "Database connection failed"
*Giải pháp*:
1. Kiểm tra PostgreSQL đang chạy (nếu dùng PostgreSQL)
2. Kiểm tra cấu hình database trong DatabaseConfig.java
3. Kiểm tra quyền truy cập database

### Lỗi: "JavaFX modules not found"
*Giải pháp*:
# Xóa cache Maven`
mvn clean

# Cài đặt lại dependencies
mvn install -DskipTests

### Lỗi: "Cannot find JAVA_HOME"
*Giải pháp*:
# Thiết lập JAVA_HOME
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"

### Client không kết nối được Server
*Giải pháp*:
1. Kiểm tra server đã khởi động: netstat -ano | findstr :54321
2. Kiểm tra firewall: Thêm exception cho port 54321
3. Kiểm tra cấu hình host/port trong code client
4. Xem logs để tìm lỗi

### Ứng Dụng Bị Lag hoặc Chậm
*Giải pháp*:
1. Tăng heap memory:

   $env:_JAVA_OPTIONS = "-Xmx2G"

2. Đóng các ứng dụng khác đang chạy
3. Kiểm tra hiệu năng database
4. Tăng kích thước connection pool

---

## Các Lệnh Hữu Ích

### Maven Commands

# Xây dựng toàn bộ dự án
mvn clean install

# Chỉ xây dựng 1 module
mvn -pl server clean install
mvn -pl client clean install

# Chạy tests
mvn test

# Bỏ qua tests khi xây dựng
mvn clean install -DskipTests

# Xóa tệp được xây dựng
mvn clean

# Xem dependencies tree
mvn dependency:tree

# Kiểm tra cập nhật dependencies
mvn versions:display-dependency-updates

### IDE Terminal Commands

# Biên dịch module
javac -d target/classes src/main/java/**/*.java

# Chạy class cụ thể
java -cp "target/classes:lib/*" server.ServerApplication

# Xem version Java
java -version

# Liệt kê các jar trong thư mục
dir *.jar /s



## Tài Liệu Tham Khảo

### Tài Liệu Chính Thức
- *Java Documentation*: https://docs.oracle.com/en/java/javase/17/
- *JavaFX Documentation*: https://gluonhq.com/products/javafx/
- *Maven Documentation*: https://maven.apache.org/
- *PostgreSQL Documentation*: https://www.postgresql.org/docs/