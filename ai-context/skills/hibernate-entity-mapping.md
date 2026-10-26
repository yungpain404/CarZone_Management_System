---
name: hibernate-entity-mapping
description: Dùng khi chuyển entity CarZone (POJO) sang ánh xạ Hibernate/JPA khớp schema SQL Server do Flyway quản lý, hoặc khi viết dao dùng EntityManager. Không dùng để tạo/sửa schema.
---

# Ánh xạ Hibernate cho CarZone

Quyết định đã chốt: dùng Hibernate, sửa entity cho khớp DB, luật nghiệp vụ kết hợp DB + Java (xem `ai-context/decisions.md`). Đã chốt: Hibernate 7.4.13.Final, giữ `java.util.Date`/`double`, cấu hình `src/META-INF/persistence.xml` (unit `CarZonePU`). Entity đã refactor xong; khi thêm/sửa entity theo đúng mẫu đó. Mục còn mở: xem "Câu hỏi mở" trong `ai-context/decisions.md`.

## Nguyên tắc
1. **Schema do Flyway sở hữu.** Đặt `hibernate.hbm2ddl.auto` = `none` (hoặc `validate`). Không bao giờ để Hibernate tạo/sửa bảng.
2. **Đối chiếu từng entity với V1 + migration sau đó** (tên cột, kiểu, null, unique, CHECK). Entity không được cho phép giá trị mà DB sẽ từ chối, và ngược lại thì để DB chặn.
3. **Khóa chính là mã nghiệp vụ gán sẵn (`String`), không dùng `@GeneratedValue`.** Mã hóa đơn, phiếu, đổi trả, yêu cầu nhập, nhân viên, tài khoản do stored procedure sinh (`sp_LapHoaDon`, `sp_LapPhieuTiepNhan`, `sp_LapPhieuDoiTra`, `sp_TaoYeuCauNhapLinhKien`, `sp_ThemNhanVien`, `sp_TaoTaiKhoan`). Cách lập bản ghi mới: gọi procedure (JPA `StoredProcedureQuery`), nhận mã trả về, rồi nạp entity. Không `persist()` với mã tự đặt ở Java cho các đối tượng này.
4. `KhachHang.maKH` và `KhuyenMai.maKM` có `DEFAULT` lấy từ sequence, không có procedure. Cách đã chốt (xem `decisions.md`): dao INSERT bằng native query `DECLARE @t TABLE(ma VARCHAR(10)); INSERT ... OUTPUT inserted.maKH INTO @t ...; SELECT ma FROM @t;` (bắt buộc `INTO` vì `KhuyenMai` có trigger), lấy mã trả về rồi `find`. Không đặt mã ở Java.
5. **Không trừ/cộng `LinhKien.soLuongTon` ở Java.** Trigger `trg_CTLK_KiemTra` trừ tồn khi ghi chi tiết linh kiện, `trg_PYCNLK_KiemTra` cộng tồn khi yêu cầu sang `DA_NHAP_KHO`. Sau khi ghi các thay đổi này, `refresh` entity `LinhKien` (hoặc đọc lại trong session mới) vì giá trị trong persistence context đã cũ.
6. **Trường dẫn xuất không có setter và không lưu.** Tổng tiền của hóa đơn (`tongTienDichVu`, `tongTienLinhKien`, `tienGiam`, `thue`, `tongThanhToan`) và `PhieuDoiTra.soTienHoanTra` dùng `@Formula` đọc từ view `vw_HoaDon_TongTien`, `vw_PhieuDoiTra_HoanTien` (chỉ getter; đối tượng mới chưa lưu = 0, cần `refresh`). Không lặp công thức thuế/khuyến mãi ở Java. Thành tiền từng dòng chi tiết (`ChiTiet*`) tính bằng Java vì DB chỉ có cột tính toán không map được.
7. **Enum:** `@Enumerated(EnumType.STRING)`, tên enum trùng chuỗi trong CHECK của DB.
8. **Khóa ghép:** đã dùng `@EmbeddedId` + `@MapsId` cho `ChiTietDichVu`, `ChiTietLinhKien`, `ChiTietDoiTra` (các lớp `*Id`); `KhuyenMai_*` là `@JoinTable`. `TaiKhoan` dùng chung khóa với `NhanVien` (`@OneToOne @MapsId`). Đọc khóa thật trong V1 trước khi thêm bảng mới.
9. **Danh sách áp dụng của khuyến mãi:** DB dùng hai bảng nối. Thay `String[]` bằng quan hệ nhiều-nhiều hoặc tập mã; danh sách rỗng = giảm tất cả.
10. **`HoaDon` ↔ `PhieuTiepNhanSuaChua`:** một-một, `maPhieu` unique trong DB.
11. Quan hệ dùng `LAZY` mặc định; chú ý `LazyInitializationException` khi `ui` giữ entity ngoài session. Cân nhắc mở `EntityManager` theo một thao tác ở `service`.
12. Giữ `equals/hashCode` theo mã nghiệp vụ như entity hiện tại.

## Lỗi từ DB
Trigger/procedure ném `THROW` mã 50001–50261. Hibernate bọc lỗi này trong `PersistenceException`; lấy `SQLException` gốc và đọc mã lỗi, rồi dịch sang thông báo tiếng Việt ở `service` (bảng mã lỗi nằm trong V1, tham khảo `verify-business-rules`). Kiểm tra bằng test thật xem mã lấy ra có đúng không trước khi dựa vào nó.
Các trigger/procedure trong V1 đều có `SET NOCOUNT ON`, nên không làm Hibernate hiểu sai số dòng bị ảnh hưởng; migration mới cũng phải giữ điều này.

## Cấu hình
- `src/META-INF/persistence.xml` được đưa vào classpath nhờ `<resources>` trong pom (dự án dùng `src` làm thư mục nguồn). Thêm entity mới thì khai báo `<class>` ở đó.
- Không ghi url/user/password trong persistence.xml: truyền qua map khi tạo `EntityManagerFactory` từ cấu hình máy (`.env`/biến môi trường). Chuỗi kết nối tương tự compose: `jdbc:sqlserver://localhost:<port>;databaseName=CarZone;encrypt=true;trustServerCertificate=true`. 

## Kiểm tra
Dựng DB sạch bằng Docker, chạy thử lưu/đọc từng entity qua Hibernate, rồi chạy các bước của `CarZone_Demo.sql` qua dao để chắc rằng kết quả trùng với kịch bản mẫu. Nếu không dựng được DB trong môi trường làm việc thì nói rõ là chưa chạy.
