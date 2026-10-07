# Xác minh UI tài chính

## Ma trận kiểm thử

| Khu vực | Kiểm tra | Trạng thái |
|---|---|---|
| Thanh điều hướng | Chỉ có Tổng quan, Duyệt, Trợ lý, Cài đặt | Đã kiểm tra source |
| Duyệt | Badge số lượng; notification READY chưa xử lý tự mở bảng xác nhận; có thể mở lại bằng Xem và lưu | Đã kiểm tra source |
| Cài đặt | Hộp thư/Đã bắt/Đã lưu nằm trong điều hướng phụ | Đã kiểm tra source |
| Nhập thủ công | Mặc định tiền mặt; app/người nhận tùy chọn; lỗi hiển thị inline | 3 unit test đạt khi biên dịch độc lập bằng Kotlin 2.2.10 + JUnit 4.13.2 |
| Trạng thái | rememberSaveableStateHolder giữ state theo tab; form/filter dùng rememberSaveable | Đã kiểm tra source |
| Build/unit test | `bash ./gradlew :app:testDebugUnitTest` | Bị chặn bởi thiếu `OpenAiCompatibleEndpoint` và `TransactionChatClient` theo README |

## Known blockers

- Repository hiện tham chiếu `OpenAiCompatibleEndpoint` và `TransactionChatClient` nhưng chưa có định nghĩa. Không thêm client giả hoặc thay đổi tích hợp mạng trong đợt UI này.
- Chưa chạy được Compose instrumentation trên thiết bị/emulator trong môi trường này.
