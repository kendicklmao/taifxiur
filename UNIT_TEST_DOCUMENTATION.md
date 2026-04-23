# Unit Test Documentation

Dự án này bao gồm một bộ unit test toàn diện được viết bằng jUnit 4 để kiểm tra các thành phần chính của hệ thống đấu giá.

## Cấu trúc Unit Test

### 1. Shared Module Tests (`shared/src/test/java/shared/models/`)

#### WalletTest.java
Kiểm tra chức năng của lớp Wallet:
- **testDepositSuccess()**: Kiểm tra nạp tiền thành công
- **testMultipleDeposits()**: Kiểm tra nạp tiền nhiều lần
- **testDepositNullAmount()**: Kiểm tra nạp tiền với số tiền null
- **testDepositZeroAmount()**: Kiểm tra nạp tiền với số tiền bằng 0
- **testWithdrawSuccess()**: Kiểm tra rút tiền thành công
- **testWithdrawInsufficientBalance()**: Kiểm tra rút tiền khi không đủ số dư
- **testInitialBalance()**: Kiểm tra số dư ban đầu bằng 0

#### UserTest.java
Kiểm tra chức năng của lớp User và các subclass (Bidder, Seller, Admin):
- **testGetUsername()**: Kiểm tra lấy tên người dùng
- **testBanUser()**: Kiểm tra chặn người dùng
- **testUnbanUser()**: Kiểm tra bỏ chặn người dùng
- **testCheckPasswordCorrect()**: Kiểm tra mật khẩu đúng
- **testCheckPasswordIncorrect()**: Kiểm tra mật khẩu sai
- **testSetValidEmail()**: Kiểm tra đặt email hợp lệ
- **testChangePasswordSuccess()**: Kiểm tra thay đổi mật khẩu thành công
- **testVerifySecurityAnswersCorrect()**: Kiểm tra câu trả lời bảo mật
- **testBidderHasWallet()**: Kiểm tra Bidder có ví

#### ItemTest.java
Kiểm tra chức năng của các lớp Item (Electronic, Vehicle, Art, Fashion, Collectible):
- **testElectronicItemCreation()**: Kiểm tra tạo mặt hàng điện tử
- **testItemNameGetterSetter()**: Kiểm tra get/set tên mặt hàng
- **testValidItem()**: Kiểm tra mặt hàng hợp lệ
- **testInvalidItemNullName()**: Kiểm tra mặt hàng không hợp lệ (tên null)
- **testInvalidItemBlankDescription()**: Kiểm tra mặt hàng không hợp lệ (mô tả trắng)
- **testElectronicMinIncrement()**: Kiểm tra mức tăng giá tối thiểu

### 2. Server Module Tests (`server/src/test/java/server/service/`)

#### WalletServiceTest.java
Kiểm tra chức năng của lớp WalletService:
- **testCreateDepositRequest()**: Kiểm tra tạo yêu cầu nạp tiền
- **testCreateDepositRequestNullBidder()**: Kiểm tra yêu cầu nạp tiền với bidder null
- **testCreateWithdrawRequest()**: Kiểm tra tạo yêu cầu rút tiền
- **testApproveDeposit()**: Kiểm tra phê duyệt yêu cầu nạp tiền
- **testRejectDeposit()**: Kiểm tra từ chối yêu cầu nạp tiền
- **testApproveWithdraw()**: Kiểm tra phê duyệt yêu cầu rút tiền
- **testRejectWithdraw()**: Kiểm tra từ chối yêu cầu rút tiền

#### AuctionServiceTest.java
Kiểm tra chức năng của lớp AuctionService:
- **testCreateAuction()**: Kiểm tra tạo phiên đấu giá
- **testCreateAuctionNullSeller()**: Kiểm tra tạo phiên với người bán null
- **testCreateAuctionBannedSeller()**: Kiểm tra tạo phiên với người bán bị chặn
- **testGetAuction()**: Kiểm tra lấy phiên đấu giá
- **testPlaceBid()**: Kiểm tra đặt giá
- **testGetAuctionsByStatus()**: Kiểm tra lấy phiên theo trạng thái
- **testGetAuctionsBySeller()**: Kiểm tra lấy phiên của một người bán
- **testRegisterAutoBid()**: Kiểm tra đăng ký đấu giá tự động

#### UserServiceTest.java
Kiểm tra chức năng của lớp UserService:
- **testRegisterNewUser()**: Kiểm tra đăng ký người dùng mới
- **testRegisterInvalidUsername()**: Kiểm tra đăng ký với tên người dùng không hợp lệ
- **testRegisterInvalidPassword()**: Kiểm tra đăng ký với mật khẩu không hợp lệ
- **testRegisterInvalidEmail()**: Kiểm tra đăng ký với email không hợp lệ
- **testLoginValidCredentials()**: Kiểm tra đăng nhập với thông tin đúng
- **testLoginInvalidPassword()**: Kiểm tra đăng nhập với mật khẩu sai
- **testUserExists()**: Kiểm tra người dùng tồn tại
- **testBanUser()**: Kiểm tra chặn người dùng
- **testUnbanUser()**: Kiểm tra bỏ chặn người dùng

## Cách Chạy Unit Test

### Sử dụng Maven
```bash
# Chạy tất cả các test
mvn test

# Chạy test cho một module cụ thể
mvn test -pl shared    # Chạy test cho shared module
mvn test -pl server    # Chạy test cho server module
mvn test -pl client    # Chạy test cho client module

# Chạy test cho một lớp cụ thể
mvn test -Dtest=WalletTest

# Chạy test cho một phương thức cụ thể
mvn test -Dtest=WalletTest#testDepositSuccess
```

### Sử dụng IDE (IntelliJ IDEA, Eclipse, VS Code)
1. Right-click vào file test
2. Chọn "Run 'ClassName Tests'" hoặc "Run with Coverage"
3. Hoặc chạy cả folder test bằng cách right-click vào folder `test`

## Dependencies

Các thư viện test được sử dụng:

```xml
<!-- JUnit 4 -->
<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <version>4.13.2</version>
    <scope>test</scope>
</dependency>

<!-- Mockito - cho mocking objects -->
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>5.2.0</version>
    <scope>test</scope>
</dependency>
```

## Phạm vi Kiểm Tra (Test Coverage)

### Wallet Class
- ✓ Deposit operations (positive, negative, zero, null)
- ✓ Withdraw operations (successful, insufficient balance, null)
- ✓ Balance tracking
- ✓ Thread-safe operations

### User Classes (Bidder, Seller, Admin)
- ✓ User creation
- ✓ Password management (set, change, reset)
- ✓ Email management
- ✓ Ban/unban operations
- ✓ Security answer verification
- ✓ Wallet access (for Bidder/Seller)

### Item Classes (Electronic, Vehicle, Art, Fashion, Collectible)
- ✓ Item creation and properties
- ✓ Validation
- ✓ Minimum increment pricing
- ✓ Category classification

### Auction Service
- ✓ Auction creation with validation
- ✓ Bid placement and bid history
- ✓ Multiple bidders handling
- ✓ Auto-bid registration
- ✓ Auction status management
- ✓ Seller and status filtering

### User Service
- ✓ User registration with validation
- ✓ Login with authentication
- ✓ User lookup
- ✓ User banning/unbanning
- ✓ Failed login attempt tracking

### Wallet Service
- ✓ Deposit request creation
- ✓ Withdraw request creation
- ✓ Request approval/rejection
- ✓ Pending request tracking
- ✓ Wallet balance updates

## Ghi Chú Quan Trọng

1. **Database Dependencies**: UserService và các Service liên quan đến database có thể cần cấu hình kết nối database thực tế để chạy test hoàn toàn.

2. **Async Operations**: Auction class sử dụng ScheduledExecutorService cho các hoạt động bất đồng bộ. Test có thể cần thêm đợi (sleep/wait) để kiểm tra trạng thái cuối cùng.

3. **Thread Safety**: Một số test kiểm tra thread-safety của Wallet class sử dụng synchronized methods.

4. **Exception Testing**: Sử dụng `@Test(expected = ExceptionType.class)` để kiểm tra các trường hợp ném ngoại lệ.

## Best Practices Được Áp Dụng

1. **AAA Pattern**: Arrange (chuẩn bị), Act (thực hiện), Assert (kiểm tra)
2. **Descriptive Test Names**: Tên test mô tả rõ ràng những gì được kiểm tra
3. **One Assertion Focus**: Mỗi test kiểm tra một điều gì đó cụ thể
4. **Setup/Teardown**: Sử dụng `@Before` để chuẩn bị dữ liệu test
5. **Isolation**: Mỗi test độc lập với test khác

## Tương Lai

Có thể thêm:
- Integration tests với database thực tế
- Performance tests cho các hoạt động IO
- Security tests cho xác thực và phân quyền
- Concurrency tests cho các hoạt động song song

