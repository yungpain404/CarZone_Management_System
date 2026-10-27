/* Kịch bản chạy thử CarZone - KHÔNG phải Flyway migration.
   Chạy thủ công trong SSMS/Azure Data Studio sau khi V1 và V2 đã migrate xong,
   trên database CarZone (USE CarZone; trước khi chạy). */
USE CarZone;
GO

-- =====================================================================
-- PHẦN 7. KỊCH BẢN CHẠY THỬ (theo tình huống mục 1.8: khách Nguyễn Văn A, xe 51G-123.45)
--   Mỗi bước là một batch riêng (GO) để một lỗi cố ý không làm dừng các bước sau.
-- =====================================================================

-- Bước 1: UC016 tạo hồ sơ khách + xe ; UC010 lập phiếu (tiếp nhận viên Hoa, KTV Tùng)
INSERT INTO KhachHang (hoTen, soDienThoai, ghiChu)
VALUES (N'Nguyễn Văn A', '0912345678', N'Hay yêu cầu gọi điện trước khi làm hạng mục phát sinh');

INSERT INTO Xe (bienSoXe, hangXe, model, mauSac, namSanXuat, soKmHienTai, maKH)
SELECT '51G-123.45', N'Toyota', N'Vios', N'Bạc', 2019, 68500, maKH FROM KhachHang WHERE soDienThoai = '0912345678';

DECLARE @maPhieuB1 VARCHAR(15);
DECLARE @tnvB1 VARCHAR(10), @ktvB1 VARCHAR(10);
SELECT @tnvB1 = maNV FROM NhanVien WHERE soDienThoai = '0901000002';
SELECT @ktvB1 = maNV FROM NhanVien WHERE soDienThoai = '0901000004';

EXEC sp_LapPhieuTiepNhan
     @bienSoXe = '51G-123.45',
     @maNVTiepNhan = @tnvB1,
     @maNVKyThuat  = @ktvB1,
     @yeuCauBanDau = N'Xe có tiếng kêu lạ khi phanh',
     @maPhieu = @maPhieuB1 OUTPUT;
PRINT N'Mã phiếu tiếp nhận vừa lập: ' + @maPhieuB1;
GO

-- Bước 2: UC011 KTV cập nhật phiếu: Đang sửa -> ghi dịch vụ + linh kiện (tồn kho tự trừ) -> Hoàn tất
UPDATE PhieuTiepNhanSuaChua SET trangThai = 'DANG_SUA' WHERE bienSoXe = '51G-123.45';

INSERT INTO ChiTietDichVu (maPhieu, maDV, soLuong, donGiaThucHien)
SELECT p.maPhieu, dv.maDV, 1, dv.donGia
FROM PhieuTiepNhanSuaChua p, DichVu dv WHERE p.bienSoXe = '51G-123.45' AND dv.maDV = 'DV00002';

INSERT INTO ChiTietLinhKien (maPhieu, maLK, soLuong, donGiaXuat)
SELECT p.maPhieu, lk.maLK, 2, lk.donGia
FROM PhieuTiepNhanSuaChua p, LinhKien lk WHERE p.bienSoXe = '51G-123.45' AND lk.maLK = 'LK00002';

SELECT maLK, tenLK, soLuongTon AS [Tồn sau khi dùng 2 bộ (kỳ vọng 3)] FROM LinhKien WHERE maLK = 'LK00002';
GO

-- Bước 3: THỬ VI PHẠM - dùng vượt tồn kho (kỳ vọng: bị chặn, báo lỗi 50123)
BEGIN TRY
INSERT INTO ChiTietLinhKien (maPhieu, maLK, soLuong, donGiaXuat)
SELECT p.maPhieu, lk.maLK, 100, lk.donGia
FROM PhieuTiepNhanSuaChua p, LinhKien lk WHERE p.bienSoXe = '51G-123.45' AND lk.maLK = 'LK00001';
PRINT N'SAI: lẽ ra phải bị chặn!';
END TRY
BEGIN CATCH
PRINT N'Đã chặn đúng: ' + ERROR_MESSAGE();
END CATCH;
GO

-- Bước 4: Hoàn tất phiếu -> UC014 lập hóa đơn (có khuyến mãi) -> UC015 thanh toán chuyển khoản
UPDATE PhieuTiepNhanSuaChua SET trangThai = 'HOAN_TAT' WHERE bienSoXe = '51G-123.45';

DECLARE @maPhieu VARCHAR(15), @tnv VARCHAR(10), @km VARCHAR(10), @maHD CHAR(14);
SELECT @maPhieu = maPhieu FROM PhieuTiepNhanSuaChua WHERE bienSoXe = '51G-123.45';
SELECT @tnv     = maNV    FROM NhanVien WHERE soDienThoai = '0901000002';
SELECT @km      = maKM    FROM KhuyenMai WHERE tenChuongTrinh = N'Ưu đãi bảo dưỡng phanh';

EXEC sp_LapHoaDon @maPhieu = @maPhieu, @maNV = @tnv, @maKM = @km, @maHoaDon = @maHD OUTPUT;
PRINT N'Mã hóa đơn vừa lập: ' + @maHD;

EXEC sp_ThanhToanHoaDon @maHoaDon = @maHD, @phuongThucTT = 'CHUYEN_KHOAN', @maGiaoDich = 'FT26265123456';

-- kỳ vọng: dịch vụ 300.000 + linh kiện 900.000 - giảm giá 30.000 + thuế trọn gói 4.5%*(1.200.000) 54.000 = 1.224.000
SELECT * FROM vw_HoaDon_TongTien WHERE maHoaDon = @maHD;
GO

-- Bước 5: THỬ VI PHẠM - sửa hóa đơn đã thanh toán (kỳ vọng: bị chặn, lỗi 50142)
BEGIN TRY
UPDATE HoaDon SET maKM = NULL WHERE trangThaiThanhToan = 'DA_THANH_TOAN';
PRINT N'SAI: lẽ ra phải bị chặn!';
END TRY
BEGIN CATCH
PRINT N'Đã chặn đúng: ' + ERROR_MESSAGE();
END CATCH;
GO

-- Bước 6: UC012 khách quay lại đổi trả má phanh (hoàn tiền 1 bộ) -> báo cáo doanh thu trừ đúng ngày
DECLARE @maHDB6 CHAR(14), @maPhieuDTB6 VARCHAR(14);
SELECT @maHDB6 = h.maHoaDon
FROM HoaDon h JOIN PhieuTiepNhanSuaChua p ON p.maPhieu = h.maPhieu WHERE p.bienSoXe = '51G-123.45';

DECLARE @tnvB6 VARCHAR(10);
SELECT @tnvB6 = maNV FROM NhanVien WHERE soDienThoai = '0901000002';

EXEC sp_LapPhieuDoiTra
     @maHoaDon = @maHDB6,
     @maNV = @tnvB6,
     @lyDoDoiTra = N'Má phanh mới lắp bị kêu bất thường',
     @maPhieuDT = @maPhieuDTB6 OUTPUT;
PRINT N'Mã phiếu đổi trả vừa lập: ' + @maPhieuDTB6;

INSERT INTO ChiTietDoiTra (maPhieuDT, stt, maPhieu, maLK, soLuong, donGia, huongXuLy)
SELECT @maPhieuDTB6, 1, h.maPhieu, 'LK00002', 1, 450000, 'HOAN_TIEN'
FROM HoaDon h WHERE h.maHoaDon = @maHDB6;

SELECT * FROM vw_PhieuDoiTra_HoanTien;
SELECT * FROM vw_DoanhThuTheoNgay;
SELECT * FROM vw_LichSuSuaChua WHERE bienSoXe = '51G-123.45';
GO