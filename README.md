# Money check Android

Ứng dụng Android quản lý giao dịch cá nhân. Money check có thể đọc thông báo giao dịch, trích xuất dữ liệu bằng API tương thích OpenAI và yêu cầu người dùng xác nhận trước khi ghi vào sổ giao dịch. Người dùng cũng có thể thêm giao dịch thủ công hoặc chọn ảnh giao dịch để AI trích xuất thông tin.

> Ứng dụng đang ở giai đoạn phát triển. Không nên xem dữ liệu được AI trích xuất là chính xác tuyệt đối; hãy kiểm tra thông tin trước khi xác nhận giao dịch.

## Trạng thái mã nguồn

README mô tả các luồng đã có trong mã nguồn, không phải cam kết mọi tính năng đã chạy thành công.

Đã chạy thành công `gradlew.bat :app:testDebugUnitTest :app:assembleDebug` trên checkout hiện tại. Chưa kiểm tra hoạt động trên thiết bị.

## Tính năng

- **Tự động đọc thông báo giao dịch** từ các ứng dụng được chọn.
- Lưu title, nội dung mở rộng và payload gốc của notification vào cơ sở dữ liệu SQLite cục bộ.
- Phân tích notification qua API tương thích OpenAI để nhận dạng:
  - chiều giao dịch: tiền vào, tiền ra hoặc chưa xác định;
  - số tiền;
  - người nhận/bên thụ hưởng;
  - mục đích giao dịch.
- **Xác nhận giao dịch** bằng giao diện trong ứng dụng, notification hoặc cửa sổ overlay.
- Thêm và sửa giao dịch thủ công.
- Chọn ảnh giao dịch hoặc biên lai để AI trích xuất thông tin trước khi lưu.
- Tổng quan thu/chi, lọc theo khoảng ngày và xem danh sách giao dịch.
- Chat với dữ liệu giao dịch hiện có qua API tương thích OpenAI.
- Cấu hình prompt phân tích notification.
- Kiểm tra API URL, API key và tải danh sách model từ endpoint `/models`.
- Xuất cơ sở dữ liệu SQLite để sao lưu hoặc kiểm tra.
- API key được mã hóa bằng Android Keystore với AES-GCM.

## Luồng hoạt động

### Notification

```text
NotificationListenerService
        ↓
Trích xuất title/text/big text/messages
        ↓
Lưu vào SQLite
        ↓
Áp dụng rule ứng dụng + tiêu đề
        ↓
WorkManager gọi API phân tích
        ↓
Bản nháp giao dịch
        ↓
Người dùng xác nhận / bỏ qua
        ↓
Transactions
```

Hộp thư hiển thị notification chưa lưu trong **24 giờ** gần nhất. Khi khởi tạo repository hoặc bắt thêm notification, dữ liệu cũ được dọn nếu chưa lưu, không còn được đánh dấu tự động bắt và không ở trạng thái chờ xác nhận (`ready`). Giao dịch đã xác nhận nằm trong bảng riêng.

**Lưu ý:** lựa chọn ứng dụng/tiêu đề chỉ giới hạn việc tự động gửi đi phân tích, không giới hạn việc thu thập vào SQLite. Listener hiện lưu notification Android chuyển tới, trừ notification của chính Money check. Tiêu đề được so khớp toàn bộ sau khi bỏ khoảng trắng đầu/cuối, không phân biệt hoa thường; danh sách tiêu đề rỗng nghĩa là nhận mọi tiêu đề của ứng dụng đã chọn.

## Công nghệ

- Kotlin
- Jetpack Compose và Material 3
- Android Notification Listener Service
- WorkManager
- SQLite thông qua `SQLiteOpenHelper`
- Android Keystore
- Gradle Kotlin DSL
- API chat completions tương thích OpenAI

## Yêu cầu môi trường

- Android Studio bản hỗ trợ Android Gradle Plugin 9.x.
- JDK 25 cho Gradle toolchain của dự án.
- Android SDK Platform 37.
- Thiết bị hoặc emulator Android **API 30 trở lên**.
- Kết nối mạng khi tải dependency và gọi API phân tích.

Các phiên bản chính hiện tại:

- Gradle Wrapper: `9.5.0`
- Android Gradle Plugin: `9.3.2`
- Kotlin: `2.2.10`
- Compile SDK / Target SDK: `37`
- Min SDK: `30`

## Build

Clone repository và mở thư mục dự án bằng Android Studio:

```bash
git clone https://github.com/anhpld/money-check-android.git
cd money-check-android
```

Trong SDK Manager, cài SDK Platform 37, Build Tools phù hợp và Platform Tools. Đặt đường dẫn SDK bằng `sdk.dir` trong `local.properties` (Android Studio thường tự tạo file này) hoặc biến môi trường `ANDROID_HOME`. Không commit đường dẫn SDK cá nhân. JDK daemon là 25; cấu hình Java source/target compatibility trong module vẫn là 11, không phải yêu cầu chạy Gradle bằng JDK 11.

Các ví dụ dùng shell Linux/macOS; trên Windows dùng `gradlew.bat` thay `./gradlew`.

Build debug APK:

```bash
./gradlew assembleDebug
```

Nếu file wrapper chưa có quyền thực thi:

```bash
bash ./gradlew assembleDebug
```

APK debug được tạo tại:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Chạy unit test:

```bash
./gradlew testDebugUnitTest
```

Chạy kiểm tra lint:

```bash
./gradlew lint
```

## Điều hướng tài chính

Thanh điều hướng chính tập trung vào bốn điểm đến: **Tổng quan**, **Duyệt** (có badge số giao dịch đang chờ), **Trợ lý** và **Cài đặt**. Hộp thư notification, nhật ký Đã bắt và mẫu Đã lưu là công cụ phụ trong Cài đặt, không chiếm chỗ trong luồng ghi sổ. Khi có notification đã phân tích sẵn và chưa xử lý, bảng xác nhận tự mở; vào Duyệt rồi chọn **Xem và lưu** để mở lại từng bản nháp. Deep-link xác nhận từ notification Android vẫn mở đúng bản nháp được chỉ định.

Khi thêm giao dịch thủ công, biểu mẫu mặc định là **Tiền mặt**. Ứng dụng và người nhận chỉ là thông tin tùy chọn; số tiền, chiều giao dịch và nội dung được báo lỗi ngay trong biểu mẫu.

Trong Cài đặt, tùy chọn cục bộ (prompt notification, rule notification, popup) được lưu độc lập và không yêu cầu API key hoặc kết nối mạng. URL/key/model AI có luồng kiểm tra và lưu riêng.

Chi tiết ma trận kiểm thử UI và blocker: [`docs/ui-verification.md`](docs/ui-verification.md).

## Cấu hình lần đầu

1. Cài và mở ứng dụng.
2. Vào **Cài đặt**.
3. Nhập **API URL** của dịch vụ tương thích OpenAI, ví dụ:

   ```text
   https://api.openai.com/v1
   ```

4. Nhập API key. Key được lưu cục bộ dưới dạng mã hóa bằng Android Keystore.
5. Nhấn kiểm tra kết nối để xác thực key và lấy danh sách model.
6. Chọn model dùng cho phân tích.
7. Chọn các ứng dụng và tiêu đề notification cần tự động phân tích.
8. Cấp các quyền cần thiết theo tính năng muốn sử dụng.

Giá trị model mặc định trong mã nguồn là `gpt-5.6-luna`; tên này không bảo đảm có trên nhà cung cấp của bạn. Hãy chọn model thực sự có quyền sử dụng. Ngoài `GET /models` và `POST /chat/completions`, dịch vụ/model phân tích phải hỗ trợ `response_format: json_schema` với `strict: true`. Kiểm tra `/models` thành công chưa chứng minh model hỗ trợ schema này.

## Quyền Android

### Đọc thông báo giao dịch

Bật quyền **Đọc thông báo giao dịch** trong phần cài đặt quyền truy cập notification listener của Android. Quyền này cho phép ứng dụng nhận notification từ các app được hệ thống cung cấp cho notification listener.

### Thông báo xác nhận

Trên Android 13 trở lên, cho phép quyền **POST_NOTIFICATIONS** để Money check hiển thị yêu cầu xác nhận giao dịch.

### Hiển thị trên ứng dụng khác

Cấp quyền **Hiển thị trên ứng dụng khác** nếu muốn dùng cửa sổ overlay xác nhận.

## Bảo mật và quyền riêng tư

- Dữ liệu giao dịch và notification được lưu trên thiết bị trong database `money-check.db`.
- API key không được lưu dạng plaintext trong SharedPreferences; ứng dụng mã hóa key bằng AES-GCM và khóa trong Android Keystore.
- Nội dung notification được gửi tới API đã cấu hình khi phân tích. Khi người dùng chọn ảnh giao dịch, ứng dụng gửi ảnh đó tới API để trích xuất thông tin. Nhà cung cấp API có thể xử lý dữ liệu theo chính sách riêng của họ.
- Database và file xuất không được mã hóa ở tầng ứng dụng. Không chia sẻ file `.db` nếu chứa thông tin cá nhân. API key nằm trong kho preferences riêng, không phải database xuất ra. Android backup được tắt trong manifest.
- Tab Chat có UI và luồng truyền danh sách giao dịch cùng hội thoại vào `TransactionChatClient`. Khả năng chat thực tế còn phụ thuộc vào API và model đã cấu hình.
- Prompt có hướng dẫn không tin nội dung chỉ dẫn xuất hiện bên trong notification/XML, nhưng người dùng vẫn phải kiểm tra bản nháp trước khi xác nhận.
- Bản build release hiện dùng debug signing config để thuận tiện cài đặt cục bộ. Cần thay bằng release keystore riêng trước khi phát hành.

## Cấu trúc mã nguồn

```text
app/src/main/java/com/example/moneycheck/
├── data/            Entity và repository SQLite
├── llm/             Client API, phân tích notification và WorkManager
├── notification/    Notification listener, extractor và confirmation notifier
├── settings/        Cài đặt ứng dụng, API key và danh sách app
├── ui/              Giao diện Jetpack Compose và theme
├── MainActivity.kt
├── MainViewModel.kt
└── OverlayConfirmationActivity.kt
```

Các test chính nằm tại:

```text
app/src/test/java/com/example/moneycheck/
```

Bao gồm test cho matcher notification title, trích xuất giao dịch từ ảnh và kiểm tra dữ liệu giao dịch.

## Giới hạn hiện tại

- Ứng dụng cần API key hợp lệ để phân tích bằng LLM và chat.
- Kết quả phân tích phụ thuộc vào chất lượng ảnh, nội dung notification và dịch vụ API.
- Chưa có cơ chế đồng bộ dữ liệu giao dịch lên máy chủ; dữ liệu là cục bộ trên thiết bị.

## License

Repository hiện chưa chứa file `LICENSE`. Hãy bổ sung và công bố giấy phép phù hợp trước khi phân phối cho bên thứ ba.
