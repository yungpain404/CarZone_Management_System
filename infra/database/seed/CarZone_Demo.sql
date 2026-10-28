/* Kịch bản chạy thử CarZone - Dữ liệu mở rộng phục vụ Demo đầy đủ các màn hình
   Chạy trong SSMS/Azure Data Studio hoặc qua sqlcmd sau khi V1 và V2 đã migrate xong.
   Database: CarZone (USE CarZone; trước khi chạy). */
USE CarZone;
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

PRINT N'=== BẮT ĐẦU NẠP DỮ LIỆU SEED DEMO CARZONE ===';
GO

-- =====================================================================
-- 1. BỔ SUNG DANH MỤC DỊCH VỤ VÀ LINH KIỆN MỞ RỘNG
-- =====================================================================
IF NOT EXISTS (SELECT 1 FROM DichVu WHERE maDV = 'DV00004')
    INSERT INTO DichVu (maDV, tenDV, donGia, donViTinh, moTa)
    VALUES ('DV00004', N'Cân chỉnh góc đặt bánh xe', 350000, N'lần', N'Cân chỉnh thước lái bằng máy laser');

IF NOT EXISTS (SELECT 1 FROM DichVu WHERE maDV = 'DV00005')
    INSERT INTO DichVu (maDV, tenDV, donGia, donViTinh, moTa)
    VALUES ('DV00005', N'Vệ sinh kim phun buồng đốt', 400000, N'lần', N'Súc rửa bằng dung dịch chuyên dụng');

IF NOT EXISTS (SELECT 1 FROM DichVu WHERE maDV = 'DV00006')
    INSERT INTO DichVu (maDV, tenDV, donGia, donViTinh, moTa)
    VALUES ('DV00006', N'Bảo dưỡng hệ thống làm mát', 200000, N'lần', N'Súc két nước và kiểm tra rò rỉ van hằng nhiệt');

IF NOT EXISTS (SELECT 1 FROM DichVu WHERE maDV = 'DV00007')
    INSERT INTO DichVu (maDV, tenDV, donGia, donViTinh, moTa)
    VALUES ('DV00007', N'Kiểm tra tổng quát toàn diện', 150000, N'lần', N'Kiểm tra 32 hạng mục an toàn vận hành');
GO

-- Linh kiện bổ sung (bao gồm LK tồn 0 để kích hoạt cảnh báo hết hàng trên Dashboard Chủ cửa hàng)
IF NOT EXISTS (SELECT 1 FROM LinhKien WHERE maLK = 'LK00004')
    INSERT INTO LinhKien (maLK, tenLK, donGia, donViTinh, soLuongTon)
    VALUES ('LK00004', N'Má phanh trước Brembo', 650000, N'bộ', 0); -- Tồn kho 0 -> Cảnh báo hết hàng!

IF NOT EXISTS (SELECT 1 FROM LinhKien WHERE maLK = 'LK00005')
    INSERT INTO LinhKien (maLK, tenLK, donGia, donViTinh, soLuongTon)
    VALUES ('LK00005', N'Lọc gió động cơ K&N', 180000, N'cái', 15);

IF NOT EXISTS (SELECT 1 FROM LinhKien WHERE maLK = 'LK00006')
    INSERT INTO LinhKien (maLK, tenLK, donGia, donViTinh, soLuongTon)
    VALUES ('LK00006', N'Bugi đánh lửa Denso Iridium', 220000, N'cái', 0); -- Tồn kho 0 -> Cảnh báo hết hàng!

IF NOT EXISTS (SELECT 1 FROM LinhKien WHERE maLK = 'LK00007')
    INSERT INTO LinhKien (maLK, tenLK, donGia, donViTinh, soLuongTon)
    VALUES ('LK00007', N'Nước làm mát động cơ Motul', 160000, N'can', 8);

IF NOT EXISTS (SELECT 1 FROM LinhKien WHERE maLK = 'LK00008')
    INSERT INTO LinhKien (maLK, tenLK, donGia, donViTinh, soLuongTon)
    VALUES ('LK00008', N'Cần gạt mưa Bosch', 250000, N'cái', 2);

IF NOT EXISTS (SELECT 1 FROM LinhKien WHERE maLK = 'LK00009')
    INSERT INTO LinhKien (maLK, tenLK, donGia, donViTinh, soLuongTon)
    VALUES ('LK00009', N'Dầu động cơ Castrol 5W-30', 550000, N'can', 25);
GO

-- =====================================================================
-- 2. HỒ SƠ KHÁCH HÀNG VÀ XE PHỤC VỤ DEMO (ĐA DẠNG HÃNG XE)
-- =====================================================================
-- Khách 1: Nguyễn Văn A (Vios)
IF NOT EXISTS (SELECT 1 FROM KhachHang WHERE soDienThoai = '0912345678')
    INSERT INTO KhachHang (hoTen, soDienThoai, ghiChu, thanhVienThanThiet, diemTichLuy)
    VALUES (N'Nguyễn Văn A', '0912345678', N'Hay yêu cầu gọi điện trước khi làm hạng mục phát sinh', 0, 0);

IF NOT EXISTS (SELECT 1 FROM Xe WHERE bienSoXe = '51G-123.45')
    INSERT INTO Xe (bienSoXe, hangXe, model, mauSac, namSanXuat, soKmHienTai, maKH)
    SELECT '51G-123.45', N'Toyota', N'Vios', N'Bạc', 2019, 68500, maKH FROM KhachHang WHERE soDienThoai = '0912345678';

-- Khách 2: Trần Thị Mai (Mazda CX-5 - Khách hàng thân thiết)
IF NOT EXISTS (SELECT 1 FROM KhachHang WHERE soDienThoai = '0987654321')
    INSERT INTO KhachHang (hoTen, soDienThoai, ghiChu, thanhVienThanThiet, diemTichLuy)
    VALUES (N'Trần Thị Mai', '0987654321', N'Khách VIP bảo dưỡng định kỳ đúng hạn', 1, 150);

IF NOT EXISTS (SELECT 1 FROM Xe WHERE bienSoXe = '51H-888.88')
    INSERT INTO Xe (bienSoXe, hangXe, model, mauSac, namSanXuat, soKmHienTai, maKH)
    SELECT '51H-888.88', N'Mazda', N'CX-5', N'Đỏ', 2021, 35000, maKH FROM KhachHang WHERE soDienThoai = '0987654321';

-- Khách 3: Lê Hoàng Nam (Honda City)
IF NOT EXISTS (SELECT 1 FROM KhachHang WHERE soDienThoai = '0903112233')
    INSERT INTO KhachHang (hoTen, soDienThoai, ghiChu, thanhVienThanThiet, diemTichLuy)
    VALUES (N'Lê Hoàng Nam', '0903112233', N'Xe công tác nhiều, cần kiểm tra kỹ gầm phanh', 1, 85);

IF NOT EXISTS (SELECT 1 FROM Xe WHERE bienSoXe = '59A-777.66')
    INSERT INTO Xe (bienSoXe, hangXe, model, mauSac, namSanXuat, soKmHienTai, maKH)
    SELECT '59A-777.66', N'Honda', N'City', N'Trắng', 2020, 48000, maKH FROM KhachHang WHERE soDienThoai = '0903112233';

-- Khách 4: Phạm Thu Hương (Hyundai SantaFe)
IF NOT EXISTS (SELECT 1 FROM KhachHang WHERE soDienThoai = '0938445566')
    INSERT INTO KhachHang (hoTen, soDienThoai, ghiChu, thanhVienThanThiet, diemTichLuy)
    VALUES (N'Phạm Thu Hương', '0938445566', N'Khách mới đến gara lần đầu', 0, 0);

IF NOT EXISTS (SELECT 1 FROM Xe WHERE bienSoXe = '51K-999.11')
    INSERT INTO Xe (bienSoXe, hangXe, model, mauSac, namSanXuat, soKmHienTai, maKH)
    SELECT '51K-999.11', N'Hyundai', N'SantaFe', N'Đen', 2022, 28000, maKH FROM KhachHang WHERE soDienThoai = '0938445566';

-- Khách 5: Hoàng Minh Tuấn (Kia Seltos)
IF NOT EXISTS (SELECT 1 FROM KhachHang WHERE soDienThoai = '0972667788')
    INSERT INTO KhachHang (hoTen, soDienThoai, ghiChu, thanhVienThanThiet, diemTichLuy)
    VALUES (N'Hoàng Minh Tuấn', '0972667788', N'Yêu cầu phụ tùng chính hãng cao cấp', 0, 0);

IF NOT EXISTS (SELECT 1 FROM Xe WHERE bienSoXe = '60B-333.22')
    INSERT INTO Xe (bienSoXe, hangXe, model, mauSac, namSanXuat, soKmHienTai, maKH)
    SELECT '60B-333.22', N'Kia', N'Seltos', N'Xám', 2021, 41000, maKH FROM KhachHang WHERE soDienThoai = '0972667788';

-- Khách 6: Đặng Quốc Bảo (Ford Ranger)
IF NOT EXISTS (SELECT 1 FROM KhachHang WHERE soDienThoai = '0918990011')
    INSERT INTO KhachHang (hoTen, soDienThoai, ghiChu, thanhVienThanThiet, diemTichLuy)
    VALUES (N'Đặng Quốc Bảo', '0918990011', N'Xe bán tải chở hàng nặng, kiểm tra lốp và phanh định kỳ', 1, 210);

IF NOT EXISTS (SELECT 1 FROM Xe WHERE bienSoXe = '51F-555.44')
    INSERT INTO Xe (bienSoXe, hangXe, model, mauSac, namSanXuat, soKmHienTai, maKH)
    SELECT '51F-555.44', N'Ford', N'Ranger', N'Cam', 2018, 92000, maKH FROM KhachHang WHERE soDienThoai = '0918990011';
GO

-- =====================================================================
-- 3. KỊCH BẢN CHẠY THỬ MẪU VÀ KIỂM THỬ RÀNG BUỘC (Xe 51G-123.45)
-- =====================================================================
DECLARE @tnv VARCHAR(10), @ktv VARCHAR(10), @maPhieuA VARCHAR(15);
SELECT @tnv = maNV FROM NhanVien WHERE soDienThoai = '0901000002'; -- Lê Thị Hoa (TNV001)
SELECT @ktv = maNV FROM NhanVien WHERE soDienThoai = '0901000004'; -- Vũ Thanh Tùng (KTV001)

IF NOT EXISTS (SELECT 1 FROM PhieuTiepNhanSuaChua WHERE bienSoXe = '51G-123.45')
BEGIN
    EXEC sp_LapPhieuTiepNhan
         @bienSoXe = '51G-123.45',
         @maNVTiepNhan = @tnv,
         @maNVKyThuat  = @ktv,
         @yeuCauBanDau = N'Xe có tiếng kêu lạ khi phanh',
         @maPhieu = @maPhieuA OUTPUT;

    UPDATE PhieuTiepNhanSuaChua SET trangThai = 'DANG_SUA' WHERE maPhieu = @maPhieuA;

    INSERT INTO ChiTietDichVu (maPhieu, maDV, soLuong, donGiaThucHien)
    SELECT @maPhieuA, dv.maDV, 1, dv.donGia FROM DichVu dv WHERE dv.maDV = 'DV00002';

    INSERT INTO ChiTietLinhKien (maPhieu, maLK, soLuong, donGiaXuat)
    SELECT @maPhieuA, lk.maLK, 2, lk.donGia FROM LinhKien lk WHERE lk.maLK = 'LK00002';

    UPDATE PhieuTiepNhanSuaChua SET trangThai = 'HOAN_TAT' WHERE maPhieu = @maPhieuA;

    DECLARE @km VARCHAR(10), @maHDA CHAR(14);
    SELECT @km = maKM FROM KhuyenMai WHERE tenChuongTrinh = N'Ưu đãi bảo dưỡng phanh';

    EXEC sp_LapHoaDon @maPhieu = @maPhieuA, @maNV = @tnv, @maKM = @km, @maHoaDon = @maHDA OUTPUT;
    EXEC sp_ThanhToanHoaDon @maHoaDon = @maHDA, @phuongThucTT = 'CHUYEN_KHOAN', @maGiaoDich = 'FT26265123456';

    -- Đổi trả má phanh hoàn tiền
    DECLARE @maPhieuDT VARCHAR(14);
    EXEC sp_LapPhieuDoiTra @maHoaDon = @maHDA, @maNV = @tnv, @lyDoDoiTra = N'Má phanh mới lắp bị kêu bất thường', @maPhieuDT = @maPhieuDT OUTPUT;
    INSERT INTO ChiTietDoiTra (maPhieuDT, stt, maPhieu, maLK, soLuong, donGia, huongXuLy)
    VALUES (@maPhieuDT, 1, @maPhieuA, 'LK00002', 1, 450000, 'HOAN_TIEN');
END;
GO

-- =====================================================================
-- 4. TẠO CÁC PHIẾU TIẾP NHẬN Ở ĐỦ 4 TRẠNG THÁI ĐỂ DEMO
--    - Chờ sửa (CHO_SUA)
--    - Đang sửa (DANG_SUA)
--    - Chờ linh kiện (CHO_LINH_KIEN)
--    - Hoàn tất chờ lập hóa đơn (HOAN_TAT)
--    - Hoàn tất đã thanh toán tiền mặt (HOAN_TAT + TIEN_MAT)
-- =====================================================================
DECLARE @tnv1 VARCHAR(10), @tnv2 VARCHAR(10), @ktv1 VARCHAR(10), @ktv2 VARCHAR(10);
SELECT @tnv1 = maNV FROM NhanVien WHERE soDienThoai = '0901000002'; -- Lê Thị Hoa (TNV001)
SELECT @tnv2 = maNV FROM NhanVien WHERE soDienThoai = '0901000003'; -- Phạm Ngọc Lan (TNV002)
SELECT @ktv1 = maNV FROM NhanVien WHERE soDienThoai = '0901000004'; -- Vũ Thanh Tùng (KTV001)
SELECT @ktv2 = maNV FROM NhanVien WHERE soDienThoai = '0901000005'; -- Đỗ Văn Hùng (KTV002)

-- Phiếu 2: Xe 51H-888.88 -> Trạng thái: CHỜ SỬA (CHO_SUA)
IF NOT EXISTS (SELECT 1 FROM PhieuTiepNhanSuaChua WHERE bienSoXe = '51H-888.88' AND trangThai = 'CHO_SUA')
BEGIN
    DECLARE @p2 VARCHAR(15);
    EXEC sp_LapPhieuTiepNhan
         @bienSoXe = '51H-888.88',
         @maNVTiepNhan = @tnv1,
         @maNVKyThuat  = @ktv1,
         @yeuCauBanDau = N'Bảo dưỡng định kỳ 40.000 km, thay dầu máy và kiểm tra tiếng kêu gầm',
         @maPhieu = @p2 OUTPUT;
    PRINT N'Tạo phiếu Chờ sửa: ' + @p2;
END;

-- Phiếu 3: Xe 59A-777.66 -> Trạng thái: ĐANG SỬA (DANG_SUA)
IF NOT EXISTS (SELECT 1 FROM PhieuTiepNhanSuaChua WHERE bienSoXe = '59A-777.66' AND trangThai = 'DANG_SUA')
BEGIN
    DECLARE @p3 VARCHAR(15);
    EXEC sp_LapPhieuTiepNhan
         @bienSoXe = '59A-777.66',
         @maNVTiepNhan = @tnv2,
         @maNVKyThuat  = @ktv1,
         @yeuCauBanDau = N'Bảo dưỡng hệ thống phanh, súc rửa két nước làm mát',
         @maPhieu = @p3 OUTPUT;

    UPDATE PhieuTiepNhanSuaChua SET trangThai = 'DANG_SUA' WHERE maPhieu = @p3;

    -- Thêm dịch vụ và linh kiện đang làm
    INSERT INTO ChiTietDichVu (maPhieu, maDV, soLuong, donGiaThucHien)
    SELECT @p3, 'DV00002', 1, donGia FROM DichVu WHERE maDV = 'DV00002'; -- Bảo dưỡng phanh

    INSERT INTO ChiTietDichVu (maPhieu, maDV, soLuong, donGiaThucHien)
    SELECT @p3, 'DV00006', 1, donGia FROM DichVu WHERE maDV = 'DV00006'; -- Bảo dưỡng hệ thống làm mát

    INSERT INTO ChiTietLinhKien (maPhieu, maLK, soLuong, donGiaXuat)
    SELECT @p3, 'LK00003', 1, donGia FROM LinhKien WHERE maLK = 'LK00003'; -- Dầu phanh
    PRINT N'Tạo phiếu Đang sửa: ' + @p3;
END;

-- Phiếu 4: Xe 60B-333.22 -> Trạng thái: CHỜ LINH KIỆN (CHO_LINH_KIEN)
IF NOT EXISTS (SELECT 1 FROM PhieuTiepNhanSuaChua WHERE bienSoXe = '60B-333.22' AND trangThai = 'CHO_LINH_KIEN')
BEGIN
    DECLARE @p4 VARCHAR(15);
    EXEC sp_LapPhieuTiepNhan
         @bienSoXe = '60B-333.22',
         @maNVTiepNhan = @tnv1,
         @maNVKyThuat  = @ktv2,
         @yeuCauBanDau = N'Thay bộ má phanh trước Brembo và 4 bugi đánh lửa Iridium',
         @maPhieu = @p4 OUTPUT;

    UPDATE PhieuTiepNhanSuaChua SET trangThai = 'CHO_LINH_KIEN' WHERE maPhieu = @p4;
    PRINT N'Tạo phiếu Chờ linh kiện: ' + @p4;
END;

-- Phiếu 5: Xe 51K-999.11 -> Trạng thái: HOÀN TẤT CHỜ THU TIỀN (HOAN_TAT, CHƯA LẬP HÓA ĐƠN)
IF NOT EXISTS (SELECT 1 FROM PhieuTiepNhanSuaChua WHERE bienSoXe = '51K-999.11' AND trangThai = 'HOAN_TAT')
BEGIN
    DECLARE @p5 VARCHAR(15);
    EXEC sp_LapPhieuTiepNhan
         @bienSoXe = '51K-999.11',
         @maNVTiepNhan = @tnv1,
         @maNVKyThuat  = @ktv2,
         @yeuCauBanDau = N'Cân chỉnh góc đặt bánh xe laser và thay dầu máy, lọc dầu',
         @maPhieu = @p5 OUTPUT;

    UPDATE PhieuTiepNhanSuaChua SET trangThai = 'DANG_SUA' WHERE maPhieu = @p5;

    INSERT INTO ChiTietDichVu (maPhieu, maDV, soLuong, donGiaThucHien)
    SELECT @p5, 'DV00001', 1, donGia FROM DichVu WHERE maDV = 'DV00001'; -- Thay dầu

    INSERT INTO ChiTietDichVu (maPhieu, maDV, soLuong, donGiaThucHien)
    SELECT @p5, 'DV00004', 1, donGia FROM DichVu WHERE maDV = 'DV00004'; -- Cân chỉnh góc đặt bánh xe

    INSERT INTO ChiTietLinhKien (maPhieu, maLK, soLuong, donGiaXuat)
    SELECT @p5, 'LK00001', 1, donGia FROM LinhKien WHERE maLK = 'LK00001'; -- Lọc dầu

    UPDATE PhieuTiepNhanSuaChua SET trangThai = 'HOAN_TAT' WHERE maPhieu = @p5;
    PRINT N'Tạo phiếu Hoàn tất chờ lập hóa đơn: ' + @p5;
END;

-- Phiếu 6: Xe 51F-555.44 -> Trạng thái: HOÀN TẤT + ĐÃ LẬP HÓA ĐƠN + THANH TOÁN TIỀN MẶT
IF NOT EXISTS (SELECT 1 FROM PhieuTiepNhanSuaChua WHERE bienSoXe = '51F-555.44' AND trangThai = 'HOAN_TAT')
BEGIN
    DECLARE @p6 VARCHAR(15);
    EXEC sp_LapPhieuTiepNhan
         @bienSoXe = '51F-555.44',
         @maNVTiepNhan = @tnv2,
         @maNVKyThuat  = @ktv1,
         @yeuCauBanDau = N'Vệ sinh kim phun buồng đốt và kiểm tra tổng quát toàn diện',
         @maPhieu = @p6 OUTPUT;

    UPDATE PhieuTiepNhanSuaChua SET trangThai = 'DANG_SUA' WHERE maPhieu = @p6;

    INSERT INTO ChiTietDichVu (maPhieu, maDV, soLuong, donGiaThucHien)
    SELECT @p6, 'DV00005', 1, donGia FROM DichVu WHERE maDV = 'DV00005'; -- Vệ sinh buồng đốt

    INSERT INTO ChiTietDichVu (maPhieu, maDV, soLuong, donGiaThucHien)
    SELECT @p6, 'DV00007', 1, donGia FROM DichVu WHERE maDV = 'DV00007'; -- Kiểm tra tổng quát

    UPDATE PhieuTiepNhanSuaChua SET trangThai = 'HOAN_TAT' WHERE maPhieu = @p6;

    DECLARE @hd6 CHAR(14);
    EXEC sp_LapHoaDon @maPhieu = @p6, @maNV = @tnv2, @maKM = NULL, @maHoaDon = @hd6 OUTPUT;
    EXEC sp_ThanhToanHoaDon @maHoaDon = @hd6, @phuongThucTT = 'TIEN_MAT', @maGiaoDich = NULL;
    PRINT N'Tạo hóa đơn thanh toán tiền mặt: ' + @hd6;
END;
GO

-- =====================================================================
-- 5. PHIẾU YÊU CẦU NHẬP LINH KIỆN PHỤC VỤ DASHBOARD CHỦ CỬA HÀNG
--    - 3 Yêu cầu chờ duyệt (CHO_DUYET)
--    - 1 Yêu cầu chờ xác nhận nhập kho (DA_DUYET)
--    - 1 Yêu cầu đã nhập kho (DA_NHAP_KHO)
--    - 1 Yêu cầu từ chối (TU_CHOI)
-- =====================================================================
DECLARE @ktv1 VARCHAR(10), @ktv2 VARCHAR(10), @cch VARCHAR(10);
SELECT @ktv1 = maNV FROM NhanVien WHERE soDienThoai = '0901000004'; -- Vũ Thanh Tùng (KTV001)
SELECT @ktv2 = maNV FROM NhanVien WHERE soDienThoai = '0901000005'; -- Đỗ Văn Hùng (KTV002)
SELECT @cch  = maNV FROM NhanVien WHERE soDienThoai = '0901000001'; -- Trần Minh Quân (CCH001)

-- YC 1: Bugi Denso (LK00006 - hết hàng) -> CHO_DUYET
IF NOT EXISTS (SELECT 1 FROM PhieuYeuCauNhapLinhKien WHERE maLK = 'LK00006' AND trangThai = 'CHO_DUYET')
BEGIN
    DECLARE @yc1 VARCHAR(14);
    EXEC sp_TaoYeuCauNhapLinhKien
         @maLK = 'LK00006',
         @maNVLap = @ktv1,
         @soLuongYeuCau = 12,
         @ghiChu = N'Tồn kho bằng 0. Cần nhập gấp 12 cái phục vụ khách thay bugi định kỳ',
         @maYeuCau = @yc1 OUTPUT;
    PRINT N'Tạo yêu cầu nhập linh kiện: ' + @yc1;
END;

-- YC 2: Má phanh trước Brembo (LK00004 - hết hàng) -> CHO_DUYET
IF NOT EXISTS (SELECT 1 FROM PhieuYeuCauNhapLinhKien WHERE maLK = 'LK00004' AND trangThai = 'CHO_DUYET')
BEGIN
    DECLARE @yc2 VARCHAR(14);
    EXEC sp_TaoYeuCauNhapLinhKien
         @maLK = 'LK00004',
         @maNVLap = @ktv2,
         @soLuongYeuCau = 10,
         @ghiChu = N'Má phanh trước Brembo đang chờ phục vụ khách xe 60B-333.22',
         @maYeuCau = @yc2 OUTPUT;
    PRINT N'Tạo yêu cầu nhập linh kiện: ' + @yc2;
END;

-- YC 3: Lọc dầu (LK00001 - sắp hết) -> CHO_DUYET
IF NOT EXISTS (SELECT 1 FROM PhieuYeuCauNhapLinhKien WHERE maLK = 'LK00001' AND trangThai = 'CHO_DUYET')
BEGIN
    DECLARE @yc3 VARCHAR(14);
    EXEC sp_TaoYeuCauNhapLinhKien
         @maLK = 'LK00001',
         @maNVLap = @ktv1,
         @soLuongYeuCau = 20,
         @ghiChu = N'Nhập bổ sung lượng tồn kho định kỳ cho hạng mục bảo dưỡng nhanh',
         @maYeuCau = @yc3 OUTPUT;
    PRINT N'Tạo yêu cầu nhập linh kiện: ' + @yc3;
END;

-- YC 4: Nước làm mát Motul (LK00007) -> ĐÃ DUYỆT (Chờ xác nhận nhập kho)
IF NOT EXISTS (SELECT 1 FROM PhieuYeuCauNhapLinhKien WHERE maLK = 'LK00007' AND trangThai = 'DA_DUYET')
BEGIN
    DECLARE @yc4 VARCHAR(14);
    EXEC sp_TaoYeuCauNhapLinhKien
         @maLK = 'LK00007',
         @maNVLap = @ktv2,
         @soLuongYeuCau = 15,
         @ghiChu = N'Nước làm mát Motul chuẩn bị hết, cần nhập thêm',
         @maYeuCau = @yc4 OUTPUT;

    UPDATE PhieuYeuCauNhapLinhKien
    SET trangThai = 'DA_DUYET', maNVXuLy = @cch, ngayXuLy = SYSDATETIME(), ghiChuXuLy = N'Đã duyệt đặt hàng từ đại lý Motul'
    WHERE maYeuCau = @yc4;
    PRINT N'Tạo yêu cầu Đã duyệt: ' + @yc4;
END;

-- YC 5: Dầu Castrol 5W-30 (LK00009) -> ĐÃ NHẬP KHO (đi qua DA_DUYET rồi mới DA_NHAP_KHO)
IF NOT EXISTS (SELECT 1 FROM PhieuYeuCauNhapLinhKien WHERE maLK = 'LK00009' AND trangThai = 'DA_NHAP_KHO')
BEGIN
    DECLARE @yc5 VARCHAR(14);
    EXEC sp_TaoYeuCauNhapLinhKien
         @maLK = 'LK00009',
         @maNVLap = @ktv1,
         @soLuongYeuCau = 20,
         @ghiChu = N'Nhập dầu động cơ Castrol cao cấp',
         @maYeuCau = @yc5 OUTPUT;

    -- Bước 1: CHO_DUYET -> DA_DUYET
    UPDATE PhieuYeuCauNhapLinhKien
    SET trangThai = 'DA_DUYET', maNVXuLy = @cch, ngayXuLy = SYSDATETIME(), ghiChuXuLy = N'Hàng đã về kho và đã kiểm đếm đầy đủ'
    WHERE maYeuCau = @yc5;

    -- Bước 2: DA_DUYET -> DA_NHAP_KHO (tồn kho tự động cộng thêm 20 can)
    UPDATE PhieuYeuCauNhapLinhKien
    SET trangThai = 'DA_NHAP_KHO'
    WHERE maYeuCau = @yc5;

    PRINT N'Tạo yêu cầu Đã nhập kho: ' + @yc5;
END;

-- YC 6: Cần gạt mưa Bosch (LK00008) -> TỪ CHỐI
IF NOT EXISTS (SELECT 1 FROM PhieuYeuCauNhapLinhKien WHERE maLK = 'LK00008' AND trangThai = 'TU_CHOI')
BEGIN
    DECLARE @yc6 VARCHAR(14);
    EXEC sp_TaoYeuCauNhapLinhKien
         @maLK = 'LK00008',
         @maNVLap = @ktv2,
         @soLuongYeuCau = 10,
         @ghiChu = N'Yêu cầu nhập cần gạt mưa dự phòng',
         @maYeuCau = @yc6 OUTPUT;

    UPDATE PhieuYeuCauNhapLinhKien
    SET trangThai = 'TU_CHOI', maNVXuLy = @cch, ngayXuLy = SYSDATETIME(), ghiChuXuLy = N'Tồn kho hiện tại vẫn đủ đáp ứng trong tuần'
    WHERE maYeuCau = @yc6;
    PRINT N'Tạo yêu cầu Từ chối: ' + @yc6;
END;
GO

-- =====================================================================
-- 6. TỔNG KẾT DỮ LIỆU ĐÃ NẠP ĐỂ KIỂM TRA
-- =====================================================================
PRINT N'-------------------------------------------------------';
PRINT N'TỔNG HỢP SỐ LIỆU ĐÃ NẠP THÀNH CÔNG:';
SELECT 'KhachHang' AS Bang, COUNT(*) AS SoLuong FROM KhachHang
UNION ALL
SELECT 'Xe', COUNT(*) FROM Xe
UNION ALL
SELECT 'DichVu', COUNT(*) FROM DichVu
UNION ALL
SELECT 'LinhKien', COUNT(*) FROM LinhKien
UNION ALL
SELECT 'LinhKien_HetHang (Ton = 0)', COUNT(*) FROM LinhKien WHERE soLuongTon = 0 AND dangKinhDoanh = 1
UNION ALL
SELECT 'PhieuTiepNhan_ChoSua', COUNT(*) FROM PhieuTiepNhanSuaChua WHERE trangThai = 'CHO_SUA'
UNION ALL
SELECT 'PhieuTiepNhan_DangSua', COUNT(*) FROM PhieuTiepNhanSuaChua WHERE trangThai = 'DANG_SUA'
UNION ALL
SELECT 'PhieuTiepNhan_ChoLinhKien', COUNT(*) FROM PhieuTiepNhanSuaChua WHERE trangThai = 'CHO_LINH_KIEN'
UNION ALL
SELECT 'PhieuTiepNhan_HoanTat', COUNT(*) FROM PhieuTiepNhanSuaChua WHERE trangThai = 'HOAN_TAT'
UNION ALL
SELECT 'HoaDon', COUNT(*) FROM HoaDon
UNION ALL
SELECT 'YeuCauNhapLK_ChoDuyet', COUNT(*) FROM PhieuYeuCauNhapLinhKien WHERE trangThai = 'CHO_DUYET'
UNION ALL
SELECT 'YeuCauNhapLK_ChoNhapKho', COUNT(*) FROM PhieuYeuCauNhapLinhKien WHERE trangThai = 'DA_DUYET';
GO
PRINT N'=== HOÀN TẤT NẠP SEED DEMO CARZONE ===';
GO