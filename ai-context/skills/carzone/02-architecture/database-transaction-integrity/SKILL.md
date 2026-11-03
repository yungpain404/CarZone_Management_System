---
name: database-transaction-integrity
description: Dùng khi viết DAO gọi SQL/procedure, xử lý nhiều bảng, tồn kho, sinh mã, khóa dữ liệu, migration Flyway hoặc cần bảo đảm transaction và toàn vẹn dữ liệu trong CarZone.
---

# Database và toàn vẹn transaction

## Mục đích

Bảo đảm dữ liệu đúng dù có lỗi giữa chừng, nhiều người thao tác, hoặc trigger/procedure chặn. Không biến Java thành nơi thực hiện lại việc của DB.

## Nguyên tắc phân vai

DB chịu trách nhiệm:

- Sinh mã cho các đối tượng đã có `sp_*`/sequence. Ngoại lệ: `DichVu.maDV`, `LinhKien.maLK` chưa có cơ chế sinh mã (xem D19).
- Trừ/cộng tồn kho (`trg_CTLK_KiemTra`, `trg_PYCNLK_KiemTra`).
- Khóa phiếu `HOAN_TAT`, hóa đơn `DA_THANH_TOAN`.
- Kiểm tra trạng thái, quyền, khóa khuyến mại.
- Tính tiền qua view.

Java chịu trách nhiệm:

- Validate sớm để báo lỗi thân thiện.
- Kiểm tra quyền theo phiên.
- Gọi đúng procedure/DAO.
- Dịch lỗi DB.
- Không làm lại các việc trên.

## Quy tắc kết nối

- Mỗi phương thức DAO mở connection từ `DbUtil.getConnection()`.
- Dùng `try-with-resources` cho `Connection`, `PreparedStatement`, `ResultSet`.
- Không giữ `Connection` làm field.
- Nhiều thao tác ghi trong cùng một nghiệp vụ: dùng một connection, `setAutoCommit(false)`, commit/rollback trong cùng phương thức; hoặc gọi một stored procedure.
- Không mở transaction trong UI và giữ qua nhiều lần bấm nút.
- Không gọi DB trên EDT.

## Mẫu transaction

```java
try (Connection con = DbUtil.getConnection()) {
    con.setAutoCommit(false);
    try {
        // 1. ghi header
        // 2. ghi chi tiết
        con.commit();
    } catch (SQLException | RuntimeException e) {
        con.rollback();
        throw e;
    } finally {
        con.setAutoCommit(true);
    }
} catch (SQLException e) {
    throw new RuntimeException("Lỗi lưu dữ liệu: " + e.getMessage(), e);
}
```

Chỉ dùng khi không có procedure phù hợp. Nếu DB đã có procedure, ưu tiên gọi procedure.

## Gọi stored procedure

- `sp_ThemNhanVien`, `sp_TaoTaiKhoan`: sinh mã theo vai trò.
- `sp_LapPhieuTiepNhan`: sinh `maPhieu`.
- `sp_LapHoaDon`: sinh `maHoaDon`, kiểm tra phiếu `HOAN_TAT` qua trigger.
- `sp_ThanhToanHoaDon`: chuyển trạng thái thanh toán.
- `sp_LapPhieuDoiTra`: sinh `maPhieuDT`.
- `sp_TaoYeuCauNhapLinhKien`: sinh `maYeuCau`.

Dùng `CallableStatement`, đăng ký tham số `OUTPUT`, đọc mã trả về. Không tự sinh mã ở Java.

## Ánh xạ lỗi

- Giữ nguyên `SQLException` gốc.
- Đọc `getErrorCode()` nếu driver trả mã; nếu không, đọc message chứa `50xxx`.
- Dịch theo bảng:

| Mã | Nhóm |
|---|---|
| 50001 | Tài khoản không khớp vai trò |
| 50101–50104 | Phiếu sửa chữa: vai trò/trạng thái/bất biến |
| 50111–50112 | Chi tiết dịch vụ |
| 50121–50123 | Chi tiết linh kiện/tồn kho |
| 50131–50133 | Hóa đơn |
| 50141–50142 | Khóa hóa đơn đã thanh toán |
| 50151–50163 | Đổi trả |
| 50171–50182 | Khuyến mại |
| 50191–50194 | Yêu cầu nhập linh kiện |
| 50201–50261 | Procedure sinh mã/thanh toán |

Không thay lỗi DB bằng thông báo chung chung nếu có thể hiển thị đúng lý do.

## Migration

- Không sửa V1/V2.
- Thay đổi schema bằng file mới `V3__...sql` trong `infra/database/fly_way_migrations/`.
- Migration phải chạy được trên DB đã có dữ liệu.
- Không xóa cột/bảng khi chưa có kế hoạch dữ liệu.
- Không đổi collation/kiểu tiền tùy tiện.
- Cập nhật `PROJECT_CONTEXT.md` và skill liên quan sau khi thêm migration.

## Kiểm tra toàn vẹn

- [ ] Không tự sinh mã ở Java.
- [ ] Không tự cập nhật `soLuongTon`.
- [ ] Không tính lại tiền/thuế/khuyến mại.
- [ ] Có transaction cho chuỗi ghi nhiều bảng.
- [ ] Có rollback.
- [ ] Không N+1 query.
- [ ] Câu SQL dùng đúng tên cột.
- [ ] Lỗi DB được giữ nguyên nhân.
- [ ] Migration mới, không sửa cũ.
- [ ] Đã kiểm tra trigger/procedure liên quan.

## Lỗi thường gặp

- Cập nhật tồn kho trong DAO, rồi trigger cập nhật lần nữa.
- Sinh `maHoaDon` bằng `LocalDateTime` ở Java, lệch định dạng ca A/P.
- Dùng `maXe`/`maNVSuaChua` (sai) thay `bienSoXe`/`maNVKyThuat`.
- Commit từng bước, để lại header mồ côi khi chi tiết lỗi.
- Bắt `SQLException` rồi trả `false` mà không log nguyên nhân.

## Liên quan

- `01-analysis/business-rules-workflow`
- `04-features/invoice-payment-promotion`
- `04-features/repair-order-workflow`
- skill cũ `ai-context/skills/jdbc-dao-mapping.md`
- skill cũ `ai-context/skills/flyway-migration.md`
