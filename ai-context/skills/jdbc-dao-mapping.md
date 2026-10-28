---
name: jdbc-dao-mapping
description: Dùng khi viết hoặc sửa DAO (interface và JDBC impl) và ánh xạ dữ liệu ResultSet vào entity POJO trong CarZone.
---

# Kỹ năng xây dựng DAO & Ánh xạ JDBC thuần trong CarZone

> Dự án CarZone sử dụng **JDBC thuần (DriverManager)**, đã loại bỏ hoàn toàn Hibernate/JPA.
> Kết nối được quản lý qua `connectDB.DbUtil` và `connectDB.DatabaseConfig`.

---

## 1. Cấu trúc thư mục tầng DAO (BẮT BUỘC)

1. **Interface DAO**:
   - Đặt trực tiếp trong package `dao` (`src/dao/`).
   - Tên file **KHÔNG có tiền tố `I`**, định dạng `<TênEntity>Dao.java` (ví dụ: `TaiKhoanDao.java`, `NhanVienDao.java`, `HoaDonDao.java`, `ChiTietHoaDonDao.java`, `PasswordResetDao.java`).
   - Chỉ định nghĩa các chữ ký phương thức nghiệp vụ đọc/ghi CSDL. Không chứa SQL, không chứa xử lý kết nối.

2. **Lớp thực thi (Implementation)**:
   - Đặt trong subpackage `dao.impl` (`src/dao/impl/`).
   - Tên file có hậu tố `Impl`, định dạng `<TênEntity>DaoImpl.java` (ví dụ: `TaiKhoanDaoImpl.java`, `NhanVienDaoImpl.java`, `HoaDonDaoImpl.java`).
   - Triển khai interface tương ứng (`public class TaiKhoanDaoImpl implements TaiKhoanDao`).

---

## 2. Quy tắc quản lý Kết nối và Tài nguyên JDBC

- **Luôn dùng try-with-resources**:
  Mỗi phương thức DAO tự mở kết nối từ `DbUtil.getConnection()`, tạo `PreparedStatement` và mở `ResultSet` trong khối try-with-resources để bảo đảm tự động đóng tài nguyên, không gây rò rỉ kết nối (connection leak):
  ```java
  String sql = "SELECT maNV, hoTen, ngaySinh FROM NhanVien WHERE maNV = ?";
  try (Connection con = DbUtil.getConnection();
       PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, maNV);
      try (ResultSet rs = ps.executeQuery()) {
          if (!rs.next()) return null;
          return mapRow(rs);
      }
  } catch (SQLException e) {
      throw new RuntimeException("Lỗi truy vấn nhân viên: " + e.getMessage(), e);
  }
  ```
- **Không giữ Connection ở cấp độ trường (field)** của DAO; mở và đóng theo từng thao tác.

---

## 3. Quy tắc ánh xạ ResultSet → Entity POJO

1. **Entity giữ vai trò POJO**:
   - Entity chứa logic nghiệp vụ và validation trong các setter chuẩn (ví dụ `setHoTen`, `setMatKhau`).
   - Khi DAO ánh xạ dữ liệu đã tồn tại trong CSDL (đã được DB đảm bảo tính hợp lệ), sử dụng các **raw setters** (`setXxxRaw(...)`) để gán trực tiếp vào thuộc tính, tránh việc validation lặp lại gây lỗi không mong muốn (ví dụ ngày sinh, CCCD, trạng thái).
   - Ví dụ trong DAO:
     ```java
     private NhanVien mapRow(ResultSet rs) throws SQLException {
         NhanVien nv = new NhanVien();
         nv.setMaNV(rs.getString("maNV"));
         nv.setHoTenRaw(rs.getString("hoTen"));
         Date ns = rs.getDate("ngaySinh");
         if (ns != null) nv.setNgaySinhRaw(ns.toLocalDate());
         nv.setSoDienThoaiRaw(rs.getString("soDienThoai"));
         nv.setCCCDRaw(rs.getString("CCCD"));
         nv.setDiaChi(rs.getString("diaChi"));
         nv.setVaiTroRaw(VaiTro.valueOf(rs.getString("vaiTro")));
         return nv;
     }
     ```

2. **Thuộc tính suy diễn (Computed / Derived Fields)**:
   - Các trường tổng tiền hóa đơn (`tongTienDichVu`, `tongTienLinhKien`, `tienGiam`, `thue`, `tongThanhToan`) do view CSDL `vw_HoaDon_TongTien` tính toán.
   - Trong `HoaDonDaoImpl`, thực hiện `LEFT JOIN vw_HoaDon_TongTien v ON h.maHoaDon = v.maHoaDon` và gọi các raw setters (`setTongThanhToanRaw(...)`) để nạp giá trị vào entity.
   - Không tự tính lại công thức thuế/khuyến mãi ở tầng Java.

---

## 4. Luật nghiệp vụ và Stored Procedure / Trigger

1. **Mã tự sinh**:
   - Các mã hóa đơn, phiếu tiếp nhận, phiếu đổi trả, yêu cầu nhập, nhân viên do Stored Procedure sinh trong CSDL (`sp_LapHoaDon`, `sp_LapPhieuTiepNhan`, `sp_ThemNhanVien`, ...).
   - Khi tạo mới, DAO gọi Stored Procedure qua `CallableStatement` hoặc `PreparedStatement`, lấy mã trả về rồi nạp entity. Tuyệt đối không tự sinh mã ở Java.
2. **Quản lý tồn kho**:
   - Trigger CSDL (`trg_CTLK_KiemTra`, `trg_PYCNLK_KiemTra`) tự động trừ và cộng tồn kho. Java không thực hiện lệnh UPDATE trừ tồn kho để tránh trừ 2 lần.
3. **Xử lý lỗi từ CSDL (Mã 50xxx)**:
   - CSDL ném lỗi nghiệp vụ qua `THROW` với mã từ 50001 đến 50261.
   - Khi bắt `SQLException`, đọc mã lỗi `e.getErrorCode()` hoặc nội dung lỗi, bọc hoặc chuyển tiếp về `controller` để hiển thị thông báo tiếng Việt thân thiện lên giao diện UI.

---

## 5. Danh sách các DAO chuẩn trong hệ thống

- `TaiKhoanDao` / `TaiKhoanDaoImpl`: Quản lý tài khoản, đổi mật khẩu SHA-256, khóa/mở tài khoản.
- `NhanVienDao` / `NhanVienDaoImpl`: Quản lý danh sách, hồ sơ và phân quyền nhân viên.
- `HoaDonDao` / `HoaDonDaoImpl`: Quản lý hóa đơn và đọc số liệu từ view `vw_HoaDon_TongTien`.
- `ChiTietHoaDonDao` / `ChiTietHoaDonDaoImpl`: Truy vấn chi tiết linh kiện & dịch vụ theo hóa đơn.
- `PasswordResetDao` / `PasswordResetDaoImpl`: Xác minh thông tin CCCD/SĐT và đặt lại mật khẩu.
