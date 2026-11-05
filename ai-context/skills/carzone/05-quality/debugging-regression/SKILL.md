---
name: debugging-regression
description: Dùng khi CarZone có lỗi chức năng, màn hình trống, ngoại lệ JDBC, dữ liệu sai, trạng thái không chuyển hoặc cần chống hồi quy sau khi sửa.
---

# Debug và chống hồi quy

## Quy trình

1. Tái hiện.
2. Khoanh vùng tầng.
3. Kiểm tra session/quyền.
4. Kiểm tra DB/trigger.
5. Kiểm tra transaction/tồn kho.
6. Kiểm tra UI/thread.
7. Xác định nguyên nhân gốc.
8. Sửa ở gốc.
9. Test hồi quy.
10. Báo cáo.

Chi tiết từng bước: `workflows/debug-regression.md`.

## Dấu hiệu lỗi theo tầng

### UI

- Bảng trống dù DB có dữ liệu.
- Nút bấm không phản hồi, treo.
- Dữ liệu cũ không refresh.
- Layout lệch, nút rớt dòng.
- Đóng/mở frame trùng.

### Controller/session

- `NullPointerException` vì `UserSession` rỗng.
- Vai trò sai do dùng nhân viên mặc định.
- Dữ liệu người khác hiển thị.
- Thông báo lỗi chung chung, mất nguyên nhân.

### DAO/JDBC

- `SQLServerException`: sai tên cột, sai kiểu, thiếu tham số.
- `Connection is closed` / rò rỉ connection.
- `Invalid object name`: thiếu bảng/view/procedure.
- `String or binary data would be truncated`.
- `Cannot insert duplicate key`.
- `Conversion failed` khi parse ngày/số.

### DB/trigger

- Mã 50xxx: vi phạm nghiệp vụ.
- Tồn kho bị trừ/cộng hai lần.
- Trạng thái không đổi do trigger chặn.
- Khóa khuyến mại/hóa đơn.
- Đổi trả quá hạn/số lượng.

## Lỗi đã biết trong repo

| Hiện tượng | Nguyên nhân | Xử lý |
|---|---|---|
| Dashboard TNV/KTV trống | `DashboardController` dùng `maXe`, `maNVSuaChua` | Đổi thành `bienSoXe`, `maNVKyThuat`; chuyển query về DAO |
| Vào thẳng giao diện quản trị | `MainFrame`/`DashboardUI` tạo nhân viên mặc định | Bỏ fallback, chuyển về đăng nhập |
| UI chạy chậm/treo | Query trên EDT | Dùng `SwingWorker` |
| Không tìm thấy bảng/view | Flyway chưa chạy hoặc DB sai | Chạy compose/migrate, kiểm tra schema |
| Mật khẩu không khớp | Hash/plaintext hoặc hoa/thường | Kiểm tra `TaiKhoan.setMatKhau`, chuẩn hóa hex |
| Đổi trả bị chặn | Quá 7 ngày/chưa thanh toán/vượt số lượng | Đọc mã 50151–50163 |
| Hóa đơn không lập được | Phiếu chưa `HOAN_TAT`/đã có HĐ/KM hết hạn | Đọc mã 50131–50133 |

## Kiểm tra nhanh

```powershell
mvn -q compile
docker compose -f infra/docker/compose.yml ps
```

- Kiểm tra DB online bằng `DbUtil.isDatabaseOnline()`.
- Chạy câu SQL tương đương trong SSMS.
- So sánh tên cột với V1/V2.
- Kiểm tra enum `valueOf` với chuỗi DB.
- Kiểm tra `ResultSet` có dòng không trước khi đọc.

## Nguyên tắc sửa

- Không thêm `try/catch` rỗng.
- Không trả `null`/`false` để che lỗi.
- Không bỏ validation để pass.
- Không sửa dữ liệu thật.
- Không đổi trigger/CHECK để né lỗi nghiệp vụ.
- Không sửa migration cũ.
- Nếu cần đổi nghiệp vụ: hỏi.

## Test hồi quy

- Tái hiện lỗi cũ.
- Kiểm tra luồng thành công lân cận.
- Kiểm tra quyền nếu liên quan.
- Kiểm tra dữ liệu biên nếu liên quan tiền/trạng thái/tồn.
- Kiểm tra UC dùng chung bảng/view.

## Báo cáo

```text
Hiện tượng:
Tái hiện:
Nguyên nhân gốc:
Tầng lỗi:
Đã sửa:
Test hồi quy:
Ảnh hưởng:
Rủi ro:
```

## Checklist

- [ ] Tái hiện được.
- [ ] Có nguyên nhân gốc, không chỉ triệu chứng.
- [ ] Sửa đúng tầng.
- [ ] Không che lỗi.
- [ ] Có test hồi quy.
- [ ] Không ảnh hưởng UC khác.
- [ ] Cập nhật `KNOWN_DISCREPANCIES.md` nếu phát hiện lệch mới.

## Liên quan

- `workflows/debug-regression.md`
- `02-architecture/database-transaction-integrity`
- `03-ui/swing-threading-responsiveness`
- `05-quality/testing-acceptance`
