/* =====================================================================
   V2 - CẬP NHẬT THỰC THỂ KhachHang (Flyway migration, chạy sau V1)
   Thay đổi so với V1:
     1. soDienThoai : VARCHAR(11) -> VARCHAR(10), ràng buộc đúng 10 chữ số, bắt đầu bằng 0
     2. Thêm cột thanhVienThanThiet (BIT, mặc định 0)
     3. Thêm cột diemTichLuy (DECIMAL(18,2), mặc định 0, >= 0)

   Lưu ý: nếu bảng KhachHang đã có số điện thoại 11 chữ số thì bước 1 sẽ lỗi;
   cần làm sạch dữ liệu đó trước khi migrate.
   ===================================================================== */

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

-- 1. Đổi kiểu/ràng buộc của soDienThoai
--    (phải gỡ CHECK và UNIQUE phụ thuộc cột trước khi ALTER COLUMN, rồi tạo lại)
ALTER TABLE KhachHang DROP CONSTRAINT CK_KhachHang_SDT;
ALTER TABLE KhachHang DROP CONSTRAINT UQ_KhachHang_soDienThoai;
GO

ALTER TABLE KhachHang ALTER COLUMN soDienThoai VARCHAR(10) NOT NULL;
GO

ALTER TABLE KhachHang ADD CONSTRAINT UQ_KhachHang_soDienThoai UNIQUE (soDienThoai);
ALTER TABLE KhachHang ADD CONSTRAINT CK_KhachHang_SDT
    CHECK (soDienThoai NOT LIKE '%[^0-9]%' AND LEN(soDienThoai) = 10 AND soDienThoai LIKE '0%');
GO

-- 2 & 3. Thêm cột mới (tách batch để ràng buộc bên dưới thấy được cột mới)
ALTER TABLE KhachHang ADD
    thanhVienThanThiet BIT           NOT NULL CONSTRAINT DF_KhachHang_thanhVienThanThiet DEFAULT (0),
    diemTichLuy        DECIMAL(18,2) NOT NULL CONSTRAINT DF_KhachHang_diemTichLuy DEFAULT (0);
GO

ALTER TABLE KhachHang ADD CONSTRAINT CK_KhachHang_DiemTichLuy CHECK (diemTichLuy >= 0);
GO

-- View vw_LichSuSuaChua đọc KhachHang.soDienThoai: làm mới metadata sau khi đổi kiểu cột
EXEC sp_refreshview N'vw_LichSuSuaChua';
GO