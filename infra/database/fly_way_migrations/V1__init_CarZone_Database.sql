/* =====================================================================
   HỆ THỐNG QUẢN LÝ GARA Ô TÔ CARZONE  -  CƠ SỞ DỮ LIỆU
   Hệ quản trị : Microsoft SQL Server 2016 trở lên (T-SQL)
   Nhóm        : Nhóm 1 - Đồ án môn Phát triển ứng dụng
   Nguồn thiết kế: Class Diagram, Use case UC001-UC016, Q&A làm rõ yêu cầu,
                   Quy định 3.1-3.3 (Chương 1 của báo cáo).

   Cấu trúc file:
     PHẦN 1. Sequence sinh mã (database tạo ở bước riêng, xem ghi chú bên dưới)
     PHẦN 2. Tạo bảng, khóa chính/ngoại, ràng buộc miền giá trị, chỉ mục
     PHẦN 3. Trigger  (ràng buộc liên bảng / quy tắc nghiệp vụ)
     PHẦN 4. Stored procedure (lập hóa đơn, thanh toán)
     PHẦN 5. View     (các thuộc tính dẫn xuất /tongTien, /soTienHoanTra, doanh thu...)
     PHẦN 6. Dữ liệu mẫu (danh mục + nhân sự)

   CHÚ Ý: file này là migration của Flyway nên KHÔNG tạo/xóa database và KHÔNG dùng USE.
   Database `CarZone` (COLLATE Vietnamese_CI_AS) phải được tạo trước khi Flyway chạy.
   Kịch bản chạy thử (demo) nằm ở file riêng: demo/CarZone_demo_scenario.sql
   Sau khi chạy: SSMS > CarZone > Database Diagrams > New Diagram > chọn
   tất cả bảng để có sơ đồ quan hệ (mục II.2 của báo cáo).
   ===================================================================== */

-- =====================================================================
-- PHẦN 1. SEQUENCE
-- =====================================================================
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

-- Sequence dùng để tự sinh mã đơn giản (không theo ngày/ca).
-- Các mã theo ngày+ca (HoaDon/PhieuTiepNhanSuaChua/PhieuDoiTra/PhieuYeuCauNhapLinhKien)
-- và mã theo vai trò (NhanVien/TaiKhoan) được sinh trong stored procedure tương ứng (PHẦN 4)
-- vì cần tính số thứ tự theo ngày/ca hoặc theo vai trò, sequence đơn thuần không đáp ứng được.
CREATE SEQUENCE seq_KhachHang AS INT START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_KhuyenMai AS INT START WITH 1 INCREMENT BY 1;
GO

-- =====================================================================
-- PHẦN 2. TẠO BẢNG VÀ RÀNG BUỘC KHAI BÁO
--   PK_ khóa chính | FK_ khóa ngoại | UQ_ duy nhất | CK_ kiểm tra | DF_ mặc định
-- =====================================================================

CREATE TABLE KhachHang (
    maKH                 VARCHAR(10)    NOT NULL CONSTRAINT DF_KhachHang_maKH DEFAULT ('KH' + RIGHT('000000' + CAST(NEXT VALUE FOR seq_KhachHang AS VARCHAR(10)), 6)),
    hoTen                NVARCHAR(100)  NOT NULL,
    soDienThoai          VARCHAR(11)    NOT NULL,
    ghiChu               NVARCHAR(500)  NULL,
    CCCD                 VARCHAR(12)    NULL,
    CONSTRAINT PK_KhachHang PRIMARY KEY (maKH),
    CONSTRAINT CK_KhachHang_Ma CHECK (maKH LIKE 'KH[0-9][0-9][0-9][0-9][0-9][0-9]'),
    CONSTRAINT CK_KhachHang_HoTen CHECK (LEN(LTRIM(RTRIM(hoTen))) > 0),
    CONSTRAINT UQ_KhachHang_soDienThoai UNIQUE (soDienThoai),
    CONSTRAINT CK_KhachHang_SDT CHECK (soDienThoai NOT LIKE '%[^0-9]%' AND LEN(soDienThoai) BETWEEN 10 AND 11 AND soDienThoai LIKE '0%'),
    CONSTRAINT CK_KhachHang_CCCD CHECK (CCCD NOT LIKE '%[^0-9]%' AND LEN(CCCD) = 12)
);
GO

CREATE UNIQUE NONCLUSTERED INDEX UX_KhachHang_CCCD ON KhachHang(CCCD) WHERE CCCD IS NOT NULL;
GO

CREATE TABLE Xe (
    bienSoXe             VARCHAR(15)    NOT NULL,
    hangXe               NVARCHAR(50)   NOT NULL,
    model                NVARCHAR(50)   NULL,
    mauSac               NVARCHAR(30)   NULL,
    namSanXuat           INT            NULL,
    soKmHienTai          INT            NOT NULL CONSTRAINT DF_Xe_soKmHienTai DEFAULT (0),
    maKH                 VARCHAR(10)    NOT NULL,
    CONSTRAINT PK_Xe PRIMARY KEY (bienSoXe),
    CONSTRAINT CK_Xe_BienSo CHECK (LEN(bienSoXe) BETWEEN 6 AND 15),
    CONSTRAINT CK_Xe_NamSX CHECK (namSanXuat BETWEEN 1900 AND 2100),
    CONSTRAINT CK_Xe_SoKm CHECK (soKmHienTai >= 0),
    CONSTRAINT FK_Xe_KhachHang FOREIGN KEY (maKH) REFERENCES KhachHang(maKH)
);
GO

CREATE NONCLUSTERED INDEX IX_Xe_maKH ON Xe(maKH);
GO

-- maNV: sinh theo vai trò (CCH/TNV/KTV + 3 số tự tăng riêng theo từng vai trò) qua sp_ThemNhanVien,
-- vì DEFAULT constraint không thể tham chiếu cột vaiTro của cùng dòng đang chèn.
CREATE TABLE NhanVien (
    maNV                 VARCHAR(10)    NOT NULL,
    hoTen                NVARCHAR(100)  NOT NULL,
    ngaySinh             DATE           NOT NULL,
    soDienThoai          VARCHAR(11)    NOT NULL,
    CCCD                 VARCHAR(12)    NOT NULL,
    diaChi               NVARCHAR(200)  NULL,
    ngayVaoLam           DATE           NOT NULL CONSTRAINT DF_NhanVien_ngayVaoLam DEFAULT (CAST(GETDATE() AS DATE)),
    vaiTro               VARCHAR(20)    NOT NULL,
    tuoi                 AS (DATEDIFF(YEAR, ngaySinh, GETDATE()) - CASE WHEN DATEADD(YEAR, DATEDIFF(YEAR, ngaySinh, GETDATE()), ngaySinh) > CAST(GETDATE() AS DATE) THEN 1 ELSE 0 END),
    CONSTRAINT PK_NhanVien PRIMARY KEY (maNV),
    CONSTRAINT CK_NhanVien_VaiTro CHECK (vaiTro IN ('CHU_CUA_HANG','TIEP_NHAN_VIEN','KY_THUAT_VIEN')),
    -- Không dùng LIKE '[A-Z]' vì khoảng ký tự chữ cái phụ thuộc collation (Vietnamese_CI_AS) và
    -- có thể khớp sai; CK_NhanVien_MaKhopVaiTro ở dưới đã ràng buộc 3 ký tự đầu đúng bằng CCH/TNV/KTV.
    CONSTRAINT CK_NhanVien_Ma CHECK (LEN(maNV) = 6 AND RIGHT(maNV, 3) NOT LIKE '%[^0-9]%'),
    CONSTRAINT CK_NhanVien_MaKhopVaiTro CHECK (LEFT(maNV, 3) = CASE vaiTro WHEN 'CHU_CUA_HANG' THEN 'CCH' WHEN 'TIEP_NHAN_VIEN' THEN 'TNV' WHEN 'KY_THUAT_VIEN' THEN 'KTV' END),
    CONSTRAINT CK_NhanVien_HoTen CHECK (LEN(LTRIM(RTRIM(hoTen))) > 0),
    CONSTRAINT UQ_NhanVien_soDienThoai UNIQUE (soDienThoai),
    CONSTRAINT CK_NhanVien_SDT CHECK (soDienThoai NOT LIKE '%[^0-9]%' AND LEN(soDienThoai) BETWEEN 10 AND 11 AND soDienThoai LIKE '0%'),
    CONSTRAINT UQ_NhanVien_CCCD UNIQUE (CCCD),
    CONSTRAINT CK_NhanVien_CCCD CHECK (CCCD NOT LIKE '%[^0-9]%' AND LEN(CCCD) = 12),
    CONSTRAINT CK_NhanVien_DuTuoi CHECK (ngaySinh <= DATEADD(YEAR, -18, ngayVaoLam))
);
GO

CREATE UNIQUE NONCLUSTERED INDEX UX_NhanVien_ChuCuaHang ON NhanVien(vaiTro) WHERE vaiTro = 'CHU_CUA_HANG';
GO

-- tenDangNhap: sinh theo vai trò của nhân viên liên kết (CCH/TNV/KTV + 3 số tự tăng riêng theo
-- từng vai trò) qua sp_TaoTaiKhoan; khớp vai trò được kiểm tra ở trg_TaiKhoan_KiemTra (PHẦN 3)
-- vì CHECK constraint không thể tham chiếu bảng khác (NhanVien.vaiTro).
CREATE TABLE TaiKhoan (
    maNV                 VARCHAR(10)    NOT NULL,
    tenDangNhap          VARCHAR(50)    NOT NULL,
    matKhau              VARCHAR(255)   NOT NULL,
    trangThaiHoatDong    BIT            NOT NULL CONSTRAINT DF_TaiKhoan_trangThaiHoatDong DEFAULT (1),
    CONSTRAINT PK_TaiKhoan PRIMARY KEY (maNV),
    CONSTRAINT UQ_TaiKhoan_tenDangNhap UNIQUE (tenDangNhap),
    -- Không dùng LIKE '[A-Z]' vì khoảng ký tự chữ cái phụ thuộc collation (Vietnamese_CI_AS);
    -- khớp vai trò nhân viên được kiểm tra thêm ở trg_TaiKhoan_KiemTra (PHẦN 3).
    CONSTRAINT CK_TaiKhoan_TenDN CHECK (LEN(tenDangNhap) = 6 AND LEFT(tenDangNhap, 3) IN ('CCH','TNV','KTV') AND RIGHT(tenDangNhap, 3) NOT LIKE '%[^0-9]%'),
    CONSTRAINT FK_TaiKhoan_NhanVien FOREIGN KEY (maNV) REFERENCES NhanVien(maNV) ON DELETE CASCADE
);
GO

CREATE TABLE DichVu (
    maDV                 VARCHAR(10)    NOT NULL,
    tenDV                NVARCHAR(150)  NOT NULL,
    donGia               DECIMAL(18,0)  NOT NULL,
    donViTinh            NVARCHAR(30)   NOT NULL CONSTRAINT DF_DichVu_donViTinh DEFAULT (N'lần'),
    moTa                 NVARCHAR(500)  NULL,
    dangCungCap          BIT            NOT NULL CONSTRAINT DF_DichVu_dangCungCap DEFAULT (1),
    CONSTRAINT PK_DichVu PRIMARY KEY (maDV),
    CONSTRAINT CK_DichVu_Ma CHECK (maDV LIKE 'DV[0-9][0-9][0-9][0-9][0-9]%' AND maDV NOT LIKE 'DV%[^0-9]%'),
    CONSTRAINT UQ_DichVu_tenDV UNIQUE (tenDV),
    CONSTRAINT CK_DichVu_DonGia CHECK (donGia > 0)
);
GO

CREATE TABLE LinhKien (
    maLK                 VARCHAR(10)    NOT NULL,
    tenLK                NVARCHAR(150)  NOT NULL,
    donGia               DECIMAL(18,0)  NOT NULL,
    donViTinh            NVARCHAR(30)   NOT NULL CONSTRAINT DF_LinhKien_donViTinh DEFAULT (N'cái'),
    soLuongTon           INT            NOT NULL CONSTRAINT DF_LinhKien_soLuongTon DEFAULT (0),
    dangKinhDoanh        BIT            NOT NULL CONSTRAINT DF_LinhKien_dangKinhDoanh DEFAULT (1),
    CONSTRAINT PK_LinhKien PRIMARY KEY (maLK),
    CONSTRAINT CK_LinhKien_Ma CHECK (maLK LIKE 'LK[0-9][0-9][0-9][0-9][0-9]%' AND maLK NOT LIKE 'LK%[^0-9]%'),
    CONSTRAINT UQ_LinhKien_tenLK UNIQUE (tenLK),
    CONSTRAINT CK_LinhKien_DonGia CHECK (donGia > 0),
    CONSTRAINT CK_LinhKien_Ton CHECK (soLuongTon >= 0)
);
GO

CREATE TABLE KhuyenMai (
    maKM                 VARCHAR(10)    NOT NULL CONSTRAINT DF_KhuyenMai_maKM DEFAULT ('KM' + RIGHT('000' + CAST(NEXT VALUE FOR seq_KhuyenMai AS VARCHAR(10)), 3)),
    maNV                 VARCHAR(10)    NOT NULL,
    tenChuongTrinh       NVARCHAR(150)  NOT NULL,
    mucGiamPhanTram      DECIMAL(5,2)   NOT NULL,
    ngayBatDau           DATE           NOT NULL,
    ngayKetThuc          DATE           NOT NULL,
    ghiChu               NVARCHAR(500)  NULL,
    CONSTRAINT PK_KhuyenMai PRIMARY KEY (maKM),
    CONSTRAINT CK_KhuyenMai_Ma CHECK (maKM LIKE 'KM[0-9][0-9][0-9]'),
    CONSTRAINT UQ_KhuyenMai_tenChuongTrinh UNIQUE (tenChuongTrinh),
    CONSTRAINT CK_KhuyenMai_Muc CHECK (mucGiamPhanTram > 0 AND mucGiamPhanTram <= 100),
    CONSTRAINT CK_KhuyenMai_Ngay CHECK (ngayBatDau <= ngayKetThuc),
    CONSTRAINT FK_KhuyenMai_NhanVien FOREIGN KEY (maNV) REFERENCES NhanVien(maNV)
);
GO

CREATE TABLE KhuyenMai_DichVu (
    maKM                 VARCHAR(10)    NOT NULL,
    maDV                 VARCHAR(10)    NOT NULL,
    CONSTRAINT PK_KhuyenMai_DichVu PRIMARY KEY (maKM, maDV),
    CONSTRAINT FK_KhuyenMai_DichVu_KhuyenMai FOREIGN KEY (maKM) REFERENCES KhuyenMai(maKM) ON DELETE CASCADE,
    CONSTRAINT FK_KhuyenMai_DichVu_DichVu FOREIGN KEY (maDV) REFERENCES DichVu(maDV)
);
GO

CREATE TABLE KhuyenMai_LinhKien (
    maKM                 VARCHAR(10)    NOT NULL,
    maLK                 VARCHAR(10)    NOT NULL,
    CONSTRAINT PK_KhuyenMai_LinhKien PRIMARY KEY (maKM, maLK),
    CONSTRAINT FK_KhuyenMai_LinhKien_KhuyenMai FOREIGN KEY (maKM) REFERENCES KhuyenMai(maKM) ON DELETE CASCADE,
    CONSTRAINT FK_KhuyenMai_LinhKien_LinhKien FOREIGN KEY (maLK) REFERENCES LinhKien(maLK)
);
GO

-- maPhieu: PSC + ngày(ddMMyyyy) + ca(A/P) + 3 số tự tăng trong ngày/ca đó, sinh qua sp_LapPhieuTiepNhan
CREATE TABLE PhieuTiepNhanSuaChua (
    maPhieu              VARCHAR(15)    NOT NULL,
    bienSoXe             VARCHAR(15)    NOT NULL,
    maNVTiepNhan         VARCHAR(10)    NOT NULL,
    maNVKyThuat          VARCHAR(10)    NULL,
    ngayGioTiepNhan      DATETIME2(0)   NOT NULL CONSTRAINT DF_PTNSC_ngayGioTiepNhan DEFAULT (SYSDATETIME()),
    yeuCauBanDau         NVARCHAR(500)  NOT NULL,
    trangThai            VARCHAR(15)    NOT NULL CONSTRAINT DF_PTNSC_trangThai DEFAULT ('CHO_SUA'),
    CONSTRAINT PK_PTNSC PRIMARY KEY (maPhieu),
    CONSTRAINT CK_PTNSC_Ma CHECK (maPhieu LIKE 'PSC[0-3][0-9][0-1][0-9][0-9][0-9][0-9][0-9][AP][0-9][0-9][0-9]'),
    CONSTRAINT CK_PTNSC_MaKhopNgay CHECK (SUBSTRING(maPhieu, 4, 8) = REPLACE(CONVERT(CHAR(10), ngayGioTiepNhan, 103), '/', '') AND SUBSTRING(maPhieu, 12, 1) = CASE WHEN DATEPART(HOUR, ngayGioTiepNhan) < 12 THEN 'A' ELSE 'P' END),
    CONSTRAINT CK_PTNSC_YeuCau CHECK (LEN(LTRIM(RTRIM(yeuCauBanDau))) > 0),
    CONSTRAINT CK_PTNSC_TrangThai CHECK (trangThai IN ('CHO_SUA','DANG_SUA','CHO_LINH_KIEN','HOAN_TAT')),
    CONSTRAINT CK_PTNSC_KTVChinh CHECK (trangThai = 'CHO_SUA' OR maNVKyThuat IS NOT NULL),
    CONSTRAINT CK_PTNSC_KhacNguoi CHECK (maNVKyThuat IS NULL OR maNVKyThuat <> maNVTiepNhan),
    CONSTRAINT FK_PTNSC_Xe FOREIGN KEY (bienSoXe) REFERENCES Xe(bienSoXe),
    CONSTRAINT FK_PTNSC_NhanVien_TiepNhan FOREIGN KEY (maNVTiepNhan) REFERENCES NhanVien(maNV),
    CONSTRAINT FK_PTNSC_NhanVien_KyThuat FOREIGN KEY (maNVKyThuat) REFERENCES NhanVien(maNV)
);
GO

CREATE NONCLUSTERED INDEX IX_PTNSC_bienSoXe ON PhieuTiepNhanSuaChua(bienSoXe, ngayGioTiepNhan);
GO

CREATE NONCLUSTERED INDEX IX_PTNSC_KTV ON PhieuTiepNhanSuaChua(maNVKyThuat, trangThai);
GO

CREATE NONCLUSTERED INDEX IX_PTNSC_TiepNhan ON PhieuTiepNhanSuaChua(maNVTiepNhan, ngayGioTiepNhan);
GO

CREATE TABLE ChiTietDichVu (
    maPhieu              VARCHAR(15)    NOT NULL,
    maDV                 VARCHAR(10)    NOT NULL,
    soLuong              INT            NOT NULL CONSTRAINT DF_ChiTietDichVu_soLuong DEFAULT (1),
    donGiaThucHien       DECIMAL(18,0)  NOT NULL,
    ghiChu               NVARCHAR(300)  NULL,
    thanhTien            AS (CAST(soLuong * donGiaThucHien AS DECIMAL(18,0))) PERSISTED,
    CONSTRAINT PK_ChiTietDichVu PRIMARY KEY (maPhieu, maDV),
    CONSTRAINT CK_ChiTietDichVu_SoLuong CHECK (soLuong > 0),
    CONSTRAINT CK_ChiTietDichVu_DonGia CHECK (donGiaThucHien >= 0),
    CONSTRAINT FK_ChiTietDichVu_PTNSC FOREIGN KEY (maPhieu) REFERENCES PhieuTiepNhanSuaChua(maPhieu) ON DELETE CASCADE,
    CONSTRAINT FK_ChiTietDichVu_DichVu FOREIGN KEY (maDV) REFERENCES DichVu(maDV)
);
GO

CREATE TABLE ChiTietLinhKien (
    maPhieu              VARCHAR(15)    NOT NULL,
    maLK                 VARCHAR(10)    NOT NULL,
    soLuong              INT            NOT NULL CONSTRAINT DF_ChiTietLinhKien_soLuong DEFAULT (1),
    donGiaXuat           DECIMAL(18,0)  NOT NULL,
    ghiChu               NVARCHAR(300)  NULL,
    thanhTien            AS (CAST(soLuong * donGiaXuat AS DECIMAL(18,0))) PERSISTED,
    CONSTRAINT PK_ChiTietLinhKien PRIMARY KEY (maPhieu, maLK),
    CONSTRAINT CK_ChiTietLinhKien_SoLuong CHECK (soLuong > 0),
    CONSTRAINT CK_ChiTietLinhKien_DonGia CHECK (donGiaXuat >= 0),
    CONSTRAINT FK_ChiTietLinhKien_PTNSC FOREIGN KEY (maPhieu) REFERENCES PhieuTiepNhanSuaChua(maPhieu) ON DELETE CASCADE,
    CONSTRAINT FK_ChiTietLinhKien_LinhKien FOREIGN KEY (maLK) REFERENCES LinhKien(maLK)
);
GO

CREATE TABLE HoaDon (
    maHoaDon             CHAR(14)       NOT NULL,
    maPhieu              VARCHAR(15)    NOT NULL,
    maNV                 VARCHAR(10)    NOT NULL,
    maKM                 VARCHAR(10)    NULL,
    ngayLap              DATETIME2(0)   NOT NULL CONSTRAINT DF_HoaDon_ngayLap DEFAULT (SYSDATETIME()),
    phuongThucTT         VARCHAR(15)    NULL,
    trangThaiThanhToan   VARCHAR(20)    NOT NULL CONSTRAINT DF_HoaDon_trangThaiThanhToan DEFAULT ('CHUA_THANH_TOAN'),
    thoiGianThanhToan    DATETIME2(0)   NULL,
    maGiaoDich           VARCHAR(50)    NULL,
    CONSTRAINT PK_HoaDon PRIMARY KEY (maHoaDon),
    CONSTRAINT CK_HoaDon_Ma CHECK (maHoaDon LIKE 'HD[0-3][0-9][0-1][0-9][0-9][0-9][0-9][0-9][AP][0-9][0-9][0-9]'),
    CONSTRAINT CK_HoaDon_MaKhopNgay CHECK (SUBSTRING(maHoaDon, 3, 8) = REPLACE(CONVERT(CHAR(10), ngayLap, 103), '/', '') AND SUBSTRING(maHoaDon, 11, 1) = CASE WHEN DATEPART(HOUR, ngayLap) < 12 THEN 'A' ELSE 'P' END),
    CONSTRAINT UQ_HoaDon_maPhieu UNIQUE (maPhieu),
    CONSTRAINT CK_HoaDon_PhuongThuc CHECK (phuongThucTT IN ('TIEN_MAT','CHUYEN_KHOAN')),
    CONSTRAINT CK_HoaDon_TrangThai CHECK (trangThaiThanhToan IN ('CHUA_THANH_TOAN','DA_THANH_TOAN')),
    CONSTRAINT CK_HoaDon_DaThanhToan CHECK ((trangThaiThanhToan = 'CHUA_THANH_TOAN' AND thoiGianThanhToan IS NULL AND phuongThucTT IS NULL AND maGiaoDich IS NULL) OR (trangThaiThanhToan = 'DA_THANH_TOAN' AND thoiGianThanhToan IS NOT NULL AND phuongThucTT IS NOT NULL)),
    CONSTRAINT CK_HoaDon_GiaoDich CHECK ((phuongThucTT = 'CHUYEN_KHOAN' AND maGiaoDich IS NOT NULL) OR (ISNULL(phuongThucTT, '') <> 'CHUYEN_KHOAN' AND maGiaoDich IS NULL)),
    CONSTRAINT CK_HoaDon_ThoiGian CHECK (thoiGianThanhToan IS NULL OR thoiGianThanhToan >= ngayLap),
    CONSTRAINT FK_HoaDon_PTNSC FOREIGN KEY (maPhieu) REFERENCES PhieuTiepNhanSuaChua(maPhieu),
    CONSTRAINT FK_HoaDon_NhanVien FOREIGN KEY (maNV) REFERENCES NhanVien(maNV),
    CONSTRAINT FK_HoaDon_KhuyenMai FOREIGN KEY (maKM) REFERENCES KhuyenMai(maKM)
);
GO

CREATE NONCLUSTERED INDEX IX_HoaDon_ngayLap ON HoaDon(ngayLap);
GO

CREATE NONCLUSTERED INDEX IX_HoaDon_maNV ON HoaDon(maNV);
GO

-- maPhieuDT: PDT + ngày(ddMMyyyy) + ca(A/P) + 2 số tự tăng trong ngày/ca đó, sinh qua sp_LapPhieuDoiTra
CREATE TABLE PhieuDoiTra (
    maPhieuDT            VARCHAR(14)    NOT NULL,
    maHoaDon             CHAR(14)       NOT NULL,
    maNV                 VARCHAR(10)    NOT NULL,
    ngayLap              DATETIME2(0)   NOT NULL CONSTRAINT DF_PhieuDoiTra_ngayLap DEFAULT (SYSDATETIME()),
    lyDoDoiTra           NVARCHAR(500)  NOT NULL,
    CONSTRAINT PK_PhieuDoiTra PRIMARY KEY (maPhieuDT),
    CONSTRAINT CK_PhieuDoiTra_Ma CHECK (maPhieuDT LIKE 'PDT[0-3][0-9][0-1][0-9][0-9][0-9][0-9][0-9][AP][0-9][0-9]'),
    CONSTRAINT CK_PhieuDoiTra_MaKhopNgay CHECK (SUBSTRING(maPhieuDT, 4, 8) = REPLACE(CONVERT(CHAR(10), ngayLap, 103), '/', '') AND SUBSTRING(maPhieuDT, 12, 1) = CASE WHEN DATEPART(HOUR, ngayLap) < 12 THEN 'A' ELSE 'P' END),
    CONSTRAINT CK_PhieuDoiTra_LyDo CHECK (LEN(LTRIM(RTRIM(lyDoDoiTra))) > 0),
    CONSTRAINT FK_PhieuDoiTra_HoaDon FOREIGN KEY (maHoaDon) REFERENCES HoaDon(maHoaDon),
    CONSTRAINT FK_PhieuDoiTra_NhanVien FOREIGN KEY (maNV) REFERENCES NhanVien(maNV)
);
GO

CREATE NONCLUSTERED INDEX IX_PhieuDoiTra_maHoaDon ON PhieuDoiTra(maHoaDon);
GO

CREATE NONCLUSTERED INDEX IX_PhieuDoiTra_maNV ON PhieuDoiTra(maNV, ngayLap);
GO

CREATE TABLE ChiTietDoiTra (
    maPhieuDT            VARCHAR(14)    NOT NULL,
    stt                  SMALLINT       NOT NULL,
    maPhieu              VARCHAR(15)    NOT NULL,
    maDV                 VARCHAR(10)    NULL,
    maLK                 VARCHAR(10)    NULL,
    soLuong              INT            NOT NULL CONSTRAINT DF_ChiTietDoiTra_soLuong DEFAULT (1),
    donGia               DECIMAL(18,0)  NOT NULL,
    huongXuLy            VARCHAR(20)    NOT NULL,
    -- Chỉ tính tiền khi hướng xử lý là hoàn tiền; đổi mới/sửa miễn phí thì không thu thêm phí
    thanhTien            AS (CAST(CASE WHEN huongXuLy = 'HOAN_TIEN' THEN soLuong * donGia ELSE 0 END AS DECIMAL(18,0))) PERSISTED,
    CONSTRAINT PK_ChiTietDoiTra PRIMARY KEY (maPhieuDT, stt),
    CONSTRAINT CK_ChiTietDoiTra_Stt CHECK (stt > 0),
    CONSTRAINT CK_ChiTietDoiTra_SoLuong CHECK (soLuong > 0),
    CONSTRAINT CK_ChiTietDoiTra_DonGia CHECK (donGia >= 0),
    CONSTRAINT CK_ChiTietDoiTra_HuongXuLy CHECK (huongXuLy IN ('DOI_MOI','HOAN_TIEN','SUA_LAI_MIEN_PHI')),
    CONSTRAINT CK_ChiTietDoiTra_MotTrongHai CHECK ((maDV IS NOT NULL AND maLK IS NULL) OR (maDV IS NULL AND maLK IS NOT NULL)),
    CONSTRAINT FK_ChiTietDoiTra_PhieuDoiTra FOREIGN KEY (maPhieuDT) REFERENCES PhieuDoiTra(maPhieuDT) ON DELETE CASCADE,
    CONSTRAINT FK_CTDT_CTDV FOREIGN KEY (maPhieu, maDV) REFERENCES ChiTietDichVu(maPhieu, maDV),
    CONSTRAINT FK_CTDT_CTLK FOREIGN KEY (maPhieu, maLK) REFERENCES ChiTietLinhKien(maPhieu, maLK)
);
GO

-- maYeuCau: PYC + ngày(ddMMyyyy) + ca(A/P) + 2 số tự tăng trong ngày/ca đó, sinh qua sp_TaoYeuCauNhapLinhKien
CREATE TABLE PhieuYeuCauNhapLinhKien (
    maYeuCau             VARCHAR(14)    NOT NULL,
    maLK                 VARCHAR(10)    NOT NULL,
    maNVLap              VARCHAR(10)    NOT NULL,
    soLuongYeuCau        INT            NOT NULL,
    ghiChu               NVARCHAR(300)  NOT NULL,
    trangThai            VARCHAR(15)    NOT NULL CONSTRAINT DF_PYCNLK_trangThai DEFAULT ('CHO_DUYET'),
    ngayTao              DATETIME2(0)   NOT NULL CONSTRAINT DF_PYCNLK_ngayTao DEFAULT (SYSDATETIME()),
    maNVXuLy             VARCHAR(10)    NULL,
    ngayXuLy             DATETIME2(0)   NULL,
    ghiChuXuLy           NVARCHAR(300)  NULL,
    CONSTRAINT PK_PYCNLK PRIMARY KEY (maYeuCau),
    CONSTRAINT CK_PYCNLK_Ma CHECK (maYeuCau LIKE 'PYC[0-3][0-9][0-1][0-9][0-9][0-9][0-9][0-9][AP][0-9][0-9]'),
    CONSTRAINT CK_PYCNLK_MaKhopNgay CHECK (SUBSTRING(maYeuCau, 4, 8) = REPLACE(CONVERT(CHAR(10), ngayTao, 103), '/', '') AND SUBSTRING(maYeuCau, 12, 1) = CASE WHEN DATEPART(HOUR, ngayTao) < 12 THEN 'A' ELSE 'P' END),
    CONSTRAINT CK_PYCNLK_SoLuong CHECK (soLuongYeuCau > 0),
    CONSTRAINT CK_PYCNLK_TrangThai CHECK (trangThai IN ('CHO_DUYET','DA_DUYET','TU_CHOI','DA_NHAP_KHO')),
    CONSTRAINT CK_PYCNLK_XuLy CHECK (trangThai = 'CHO_DUYET' OR (maNVXuLy IS NOT NULL AND ngayXuLy IS NOT NULL)),
    CONSTRAINT CK_PYCNLK_ThoiGian CHECK (ngayXuLy IS NULL OR ngayXuLy >= ngayTao),
    CONSTRAINT FK_PYCNLK_LinhKien FOREIGN KEY (maLK) REFERENCES LinhKien(maLK),
    CONSTRAINT FK_PYCNLK_NhanVien_Lap FOREIGN KEY (maNVLap) REFERENCES NhanVien(maNV),
    CONSTRAINT FK_PYCNLK_NhanVien_XuLy FOREIGN KEY (maNVXuLy) REFERENCES NhanVien(maNV)
);
GO

CREATE NONCLUSTERED INDEX IX_PYCNLK_trangThai ON PhieuYeuCauNhapLinhKien(trangThai, maLK);
GO

-- =====================================================================
-- PHẦN 3. TRIGGER - RÀNG BUỘC LIÊN BẢNG / QUY TẮC NGHIỆP VỤ
-- (mỗi trigger: nếu vi phạm thì ROLLBACK và báo lỗi bằng THROW, mã lỗi >= 50000)
-- =====================================================================

-- 3.0 Tài khoản: tiền tố tenDangNhap (CCH/TNV/KTV) phải khớp vai trò của nhân viên liên kết
CREATE TRIGGER trg_TaiKhoan_KiemTra ON TaiKhoan
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (SELECT 1 FROM inserted i JOIN NhanVien nv ON nv.maNV = i.maNV
               WHERE LEFT(i.tenDangNhap, 3) <> CASE nv.vaiTro WHEN 'CHU_CUA_HANG' THEN 'CCH' WHEN 'TIEP_NHAN_VIEN' THEN 'TNV' WHEN 'KY_THUAT_VIEN' THEN 'KTV' END)
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50001, N'Tên đăng nhập không khớp vai trò của nhân viên.', 1;
    END;
END;
GO

-- 3.1 Phiếu tiếp nhận - sửa chữa: vai trò nhân viên, luồng trạng thái, phiếu hoàn tất bất biến
CREATE TRIGGER trg_PTNSC_KiemTra ON PhieuTiepNhanSuaChua
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (SELECT 1 FROM inserted i JOIN NhanVien nv ON nv.maNV = i.maNVTiepNhan
               WHERE nv.vaiTro <> 'TIEP_NHAN_VIEN')
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50101, N'Người lập phiếu tiếp nhận phải có vai trò Tiếp nhận viên.', 1;
    END;

    IF EXISTS (SELECT 1 FROM inserted i JOIN NhanVien nv ON nv.maNV = i.maNVKyThuat
               WHERE nv.vaiTro <> 'KY_THUAT_VIEN')
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50102, N'Kỹ thuật viên chính phải có vai trò Kỹ thuật viên.', 1;
    END;

    -- Luồng: CHO_SUA -> DANG_SUA <-> CHO_LINH_KIEN ; DANG_SUA -> HOAN_TAT ; HOAN_TAT là trạng thái cuối
    IF EXISTS (SELECT 1 FROM inserted i JOIN deleted d ON d.maPhieu = i.maPhieu
               WHERE i.trangThai <> d.trangThai
                 AND NOT ( (d.trangThai = 'CHO_SUA'       AND i.trangThai IN ('DANG_SUA', 'CHO_LINH_KIEN'))
                        OR (d.trangThai = 'DANG_SUA'      AND i.trangThai IN ('CHO_LINH_KIEN', 'HOAN_TAT'))
                        OR (d.trangThai = 'CHO_LINH_KIEN' AND i.trangThai = 'DANG_SUA') ))
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50103, N'Chuyển trạng thái phiếu không hợp lệ (Chờ sửa → Đang sửa ⇄ Chờ linh kiện → Hoàn tất).', 1;
    END;

    -- Phiếu đã HOAN_TAT thì không được sửa thông tin tiếp nhận
    IF EXISTS (SELECT 1 FROM inserted i JOIN deleted d ON d.maPhieu = i.maPhieu
               WHERE d.trangThai = 'HOAN_TAT'
                 AND (i.bienSoXe <> d.bienSoXe OR i.maNVTiepNhan <> d.maNVTiepNhan
                      OR ISNULL(i.maNVKyThuat, '') <> ISNULL(d.maNVKyThuat, '')
                      OR i.yeuCauBanDau <> d.yeuCauBanDau OR i.ngayGioTiepNhan <> d.ngayGioTiepNhan))
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50104, N'Phiếu đã Hoàn tất - không được chỉnh sửa.', 1;
    END;
END;
GO

-- 3.2 Chi tiết dịch vụ: khóa khi phiếu đã hoàn tất; không thêm dịch vụ ngừng cung cấp
CREATE TRIGGER trg_CTDV_KiemTra ON ChiTietDichVu
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (SELECT 1 FROM PhieuTiepNhanSuaChua p
               WHERE p.trangThai = 'HOAN_TAT'
                 AND p.maPhieu IN (SELECT maPhieu FROM inserted UNION SELECT maPhieu FROM deleted))
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50111, N'Phiếu đã Hoàn tất - không được thêm/sửa/xóa dịch vụ.', 1;
    END;

    IF EXISTS (SELECT 1 FROM inserted i JOIN DichVu dv ON dv.maDV = i.maDV
               WHERE dv.dangCungCap = 0
                 AND NOT EXISTS (SELECT 1 FROM deleted d WHERE d.maPhieu = i.maPhieu AND d.maDV = i.maDV))
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50112, N'Dịch vụ đã ngừng cung cấp - không thể thêm vào phiếu.', 1;
    END;
END;
GO

-- 3.3 Chi tiết linh kiện: khóa khi hoàn tất; ngừng kinh doanh; TỰ ĐỘNG TRỪ/CỘNG TỒN KHO; không vượt tồn (Q&A số 4 - PA1)
CREATE TRIGGER trg_CTLK_KiemTra ON ChiTietLinhKien
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF EXISTS (SELECT 1 FROM PhieuTiepNhanSuaChua p
               WHERE p.trangThai = 'HOAN_TAT'
                 AND p.maPhieu IN (SELECT maPhieu FROM inserted UNION SELECT maPhieu FROM deleted))
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50121, N'Phiếu đã Hoàn tất - không được thêm/sửa/xóa linh kiện.', 1;
    END;

    IF EXISTS (SELECT 1 FROM inserted i JOIN LinhKien lk ON lk.maLK = i.maLK
               WHERE lk.dangKinhDoanh = 0
                 AND NOT EXISTS (SELECT 1 FROM deleted d WHERE d.maPhieu = i.maPhieu AND d.maLK = i.maLK))
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50122, N'Linh kiện đã ngừng kinh doanh - không thể thêm vào phiếu.', 1;
    END;

    -- delta < 0: xuất kho (thêm/tăng số lượng dùng) ; delta > 0: hoàn kho (giảm/xóa dòng)
    DECLARE @delta TABLE (maLK VARCHAR(10) PRIMARY KEY, delta INT NOT NULL);
    INSERT INTO @delta (maLK, delta)
    SELECT maLK, SUM(delta)
    FROM (SELECT maLK, -soLuong AS delta FROM inserted
          UNION ALL
          SELECT maLK,  soLuong AS delta FROM deleted) t
    GROUP BY maLK
    HAVING SUM(delta) <> 0;

    IF EXISTS (SELECT 1 FROM LinhKien lk JOIN @delta x ON x.maLK = lk.maLK
               WHERE lk.soLuongTon + x.delta < 0)
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50123, N'Số lượng linh kiện sử dụng vượt quá số lượng tồn kho hiện có.', 1;
    END;

    UPDATE lk
    SET    lk.soLuongTon = lk.soLuongTon + x.delta
    FROM   LinhKien lk
    JOIN   @delta x ON x.maLK = lk.maLK;
END;
GO

-- 3.4 Hóa đơn (INSERT/UPDATE): vai trò người lập, chỉ lập cho phiếu HOAN_TAT, khuyến mãi còn hiệu lực
CREATE TRIGGER trg_HoaDon_KiemTra ON HoaDon
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (SELECT 1 FROM inserted i JOIN NhanVien nv ON nv.maNV = i.maNV
               WHERE nv.vaiTro <> 'TIEP_NHAN_VIEN')
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50131, N'Chỉ Tiếp nhận viên mới được lập hóa đơn.', 1;
    END;

    -- Các kiểm tra dưới đây chỉ áp dụng cho hóa đơn mới (không có trong deleted)
    IF EXISTS (SELECT 1 FROM inserted i JOIN PhieuTiepNhanSuaChua p ON p.maPhieu = i.maPhieu
               WHERE p.trangThai <> 'HOAN_TAT'
                 AND NOT EXISTS (SELECT 1 FROM deleted d WHERE d.maHoaDon = i.maHoaDon))
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50132, N'Chỉ được lập hóa đơn cho phiếu ở trạng thái Hoàn tất.', 1;
    END;

    IF EXISTS (SELECT 1 FROM inserted i JOIN KhuyenMai km ON km.maKM = i.maKM
               WHERE CAST(i.ngayLap AS DATE) NOT BETWEEN km.ngayBatDau AND km.ngayKetThuc
                 AND NOT EXISTS (SELECT 1 FROM deleted d WHERE d.maHoaDon = i.maHoaDon))
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50133, N'Chương trình khuyến mãi không còn hiệu lực tại ngày lập hóa đơn.', 1;
    END;
END;
GO

-- 3.5 Hóa đơn đã thanh toán: không sửa, không xóa (bảo toàn số liệu doanh thu)
CREATE TRIGGER trg_HoaDon_KhoaSauThanhToan ON HoaDon
AFTER UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (SELECT 1 FROM deleted d
               WHERE d.trangThaiThanhToan = 'DA_THANH_TOAN'
                 AND NOT EXISTS (SELECT 1 FROM inserted i WHERE i.maHoaDon = d.maHoaDon))
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50141, N'Hóa đơn đã thanh toán - không được xóa.', 1;
    END;

    IF EXISTS (SELECT 1 FROM deleted d JOIN inserted i ON i.maHoaDon = d.maHoaDon
               WHERE d.trangThaiThanhToan = 'DA_THANH_TOAN'
                 AND (i.maPhieu <> d.maPhieu OR i.maNV <> d.maNV
                      OR ISNULL(i.maKM, '') <> ISNULL(d.maKM, '')
                      OR i.ngayLap <> d.ngayLap
                      OR i.trangThaiThanhToan <> d.trangThaiThanhToan
                      OR ISNULL(i.phuongThucTT, '') <> ISNULL(d.phuongThucTT, '')
                      OR ISNULL(i.maGiaoDich, '') <> ISNULL(d.maGiaoDich, '')
                      OR ISNULL(i.thoiGianThanhToan, '19000101') <> ISNULL(d.thoiGianThanhToan, '19000101')))
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50142, N'Hóa đơn đã thanh toán - không được chỉnh sửa.', 1;
    END;
END;
GO

-- 3.6 Phiếu đổi trả: hóa đơn đã thanh toán, trong 7 ngày kể từ ngày lập hóa đơn (Mục 3.1, 3.3)
CREATE TRIGGER trg_PhieuDoiTra_KiemTra ON PhieuDoiTra
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (SELECT 1 FROM inserted i JOIN NhanVien nv ON nv.maNV = i.maNV
               WHERE nv.vaiTro <> 'TIEP_NHAN_VIEN')
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50151, N'Chỉ Tiếp nhận viên mới được lập phiếu đổi trả.', 1;
    END;

    IF EXISTS (SELECT 1 FROM inserted i JOIN HoaDon h ON h.maHoaDon = i.maHoaDon
               WHERE h.trangThaiThanhToan <> 'DA_THANH_TOAN')
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50152, N'Hóa đơn chưa thanh toán - không hợp lệ để lập phiếu đổi trả.', 1;
    END;

    IF EXISTS (SELECT 1 FROM inserted i JOIN HoaDon h ON h.maHoaDon = i.maHoaDon
               WHERE i.ngayLap < h.ngayLap OR i.ngayLap > DATEADD(DAY, 7, h.ngayLap))
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50153, N'Hóa đơn đã quá thời hạn đổi trả 7 ngày.', 1;
    END;
END;
GO

-- 3.7 Chi tiết đổi trả: mục phải thuộc phiếu gốc của hóa đơn; tổng SL đổi trả không vượt SL đã bán
CREATE TRIGGER trg_CTDT_KiemTra ON ChiTietDoiTra
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (SELECT 1 FROM inserted i
               JOIN PhieuDoiTra p ON p.maPhieuDT = i.maPhieuDT
               JOIN HoaDon h      ON h.maHoaDon  = p.maHoaDon
               WHERE h.maPhieu <> i.maPhieu)
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50161, N'Mục đổi trả phải thuộc phiếu sửa chữa của hóa đơn gốc.', 1;
    END;

    IF EXISTS (SELECT 1
               FROM (SELECT DISTINCT maPhieu, maDV FROM inserted WHERE maDV IS NOT NULL) x
               JOIN ChiTietDichVu c ON c.maPhieu = x.maPhieu AND c.maDV = x.maDV
               WHERE (SELECT SUM(t.soLuong) FROM ChiTietDoiTra t
                      WHERE t.maPhieu = x.maPhieu AND t.maDV = x.maDV) > c.soLuong)
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50162, N'Tổng số lượng dịch vụ đổi trả vượt quá số lượng đã thực hiện.', 1;
    END;

    IF EXISTS (SELECT 1
               FROM (SELECT DISTINCT maPhieu, maLK FROM inserted WHERE maLK IS NOT NULL) x
               JOIN ChiTietLinhKien c ON c.maPhieu = x.maPhieu AND c.maLK = x.maLK
               WHERE (SELECT SUM(t.soLuong) FROM ChiTietDoiTra t
                      WHERE t.maPhieu = x.maPhieu AND t.maLK = x.maLK) > c.soLuong)
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50163, N'Tổng số lượng linh kiện đổi trả vượt quá số lượng đã sử dụng.', 1;
    END;
END;
GO

-- 3.8 Khuyến mãi: chỉ Chủ cửa hàng tạo; đã áp dụng cho hóa đơn thì không đổi mức giảm / thời gian
CREATE TRIGGER trg_KhuyenMai_KiemTra ON KhuyenMai
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (SELECT 1 FROM inserted i JOIN NhanVien nv ON nv.maNV = i.maNV
               WHERE nv.vaiTro <> 'CHU_CUA_HANG')
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50171, N'Chỉ Chủ cửa hàng mới được tạo chương trình khuyến mãi.', 1;
    END;

    IF EXISTS (SELECT 1 FROM inserted i JOIN deleted d ON d.maKM = i.maKM
               WHERE (i.mucGiamPhanTram <> d.mucGiamPhanTram OR i.ngayBatDau <> d.ngayBatDau
                      OR i.ngayKetThuc <> d.ngayKetThuc)
                 AND EXISTS (SELECT 1 FROM HoaDon h WHERE h.maKM = i.maKM))
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50172, N'Khuyến mãi đã áp dụng cho hóa đơn - không được đổi mức giảm hoặc thời gian.', 1;
    END;
END;
GO

-- 3.9 Danh sách dịch vụ/linh kiện áp dụng khuyến mãi: khóa khi khuyến mãi đã dùng trong hóa đơn
CREATE TRIGGER trg_KMDV_Khoa ON KhuyenMai_DichVu
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM HoaDon h
               WHERE h.maKM IN (SELECT maKM FROM inserted UNION SELECT maKM FROM deleted))
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50181, N'Khuyến mãi đã áp dụng cho hóa đơn - không được đổi danh sách dịch vụ áp dụng.', 1;
    END;
END;
GO

CREATE TRIGGER trg_KMLK_Khoa ON KhuyenMai_LinhKien
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM HoaDon h
               WHERE h.maKM IN (SELECT maKM FROM inserted UNION SELECT maKM FROM deleted))
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50182, N'Khuyến mãi đã áp dụng cho hóa đơn - không được đổi danh sách linh kiện áp dụng.', 1;
    END;
END;
GO

-- 3.10 Yêu cầu nhập linh kiện: vai trò, luồng trạng thái, tự cộng tồn khi DA_NHAP_KHO
CREATE TRIGGER trg_PYCNLK_KiemTra ON PhieuYeuCauNhapLinhKien
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF EXISTS (SELECT 1 FROM inserted i JOIN NhanVien nv ON nv.maNV = i.maNVLap
               WHERE nv.vaiTro <> 'KY_THUAT_VIEN')
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50191, N'Chỉ Kỹ thuật viên mới được gửi yêu cầu nhập linh kiện.', 1;
    END;

    IF EXISTS (SELECT 1 FROM inserted i JOIN NhanVien nv ON nv.maNV = i.maNVXuLy
               WHERE nv.vaiTro <> 'CHU_CUA_HANG')
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50192, N'Chỉ Chủ cửa hàng mới được duyệt/từ chối yêu cầu nhập linh kiện.', 1;
    END;

    IF EXISTS (SELECT 1 FROM inserted i
               WHERE i.trangThai <> 'CHO_DUYET'
                 AND NOT EXISTS (SELECT 1 FROM deleted d WHERE d.maYeuCau = i.maYeuCau))
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50193, N'Yêu cầu nhập linh kiện mới tạo phải ở trạng thái Chờ duyệt.', 1;
    END;

    IF EXISTS (SELECT 1 FROM inserted i JOIN deleted d ON d.maYeuCau = i.maYeuCau
               WHERE i.trangThai <> d.trangThai
                 AND NOT ( (d.trangThai = 'CHO_DUYET' AND i.trangThai IN ('DA_DUYET', 'TU_CHOI'))
                        OR (d.trangThai = 'DA_DUYET'  AND i.trangThai = 'DA_NHAP_KHO') ))
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50194, N'Chuyển trạng thái yêu cầu không hợp lệ (Chờ duyệt → Đã duyệt/Từ chối → Đã nhập kho).', 1;
    END;

    -- Khi yêu cầu chuyển sang DA_NHAP_KHO: cộng số lượng yêu cầu vào tồn kho
    UPDATE lk
    SET    lk.soLuongTon = lk.soLuongTon + s.tong
    FROM   LinhKien lk
    JOIN  (SELECT i.maLK, SUM(i.soLuongYeuCau) AS tong
           FROM inserted i JOIN deleted d ON d.maYeuCau = i.maYeuCau
           WHERE i.trangThai = 'DA_NHAP_KHO' AND d.trangThai <> 'DA_NHAP_KHO'
           GROUP BY i.maLK) s ON s.maLK = lk.maLK;
END;
GO

-- =====================================================================
-- PHẦN 4. STORED PROCEDURE
-- =====================================================================

-- 4.1 UC014 - Lập hóa đơn: sinh mã HD + ddMMyyyyAXXX (A: ca sáng < 12h, P: ca chiều; XXX tăng dần theo ngày + ca)
CREATE PROCEDURE sp_LapHoaDon
    @maPhieu   VARCHAR(15),
    @maNV      VARCHAR(10),
    @maKM      VARCHAR(10) = NULL,
    @maHoaDon  CHAR(14) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE @now    DATETIME2(0) = SYSDATETIME();
    DECLARE @prefix CHAR(11) = 'HD' + REPLACE(CONVERT(CHAR(10), @now, 103), '/', '')
                              + CASE WHEN DATEPART(HOUR, @now) < 12 THEN 'A' ELSE 'P' END;
    DECLARE @stt INT;

    BEGIN TRANSACTION;

    SELECT @stt = ISNULL(MAX(CAST(RIGHT(maHoaDon, 3) AS INT)), 0) + 1
    FROM   HoaDon WITH (UPDLOCK, HOLDLOCK)
    WHERE  LEFT(maHoaDon, 11) = @prefix;

    IF @stt > 999
    BEGIN
        ;THROW 50201, N'Đã vượt quá 999 hóa đơn trong một ca.', 1;
    END;

    SET @maHoaDon = @prefix + RIGHT('000' + CAST(@stt AS VARCHAR(3)), 3);

    INSERT INTO HoaDon (maHoaDon, maPhieu, maNV, maKM, ngayLap)
    VALUES (@maHoaDon, @maPhieu, @maNV, @maKM, @now);

    COMMIT TRANSACTION;
END;
GO

-- 4.2 UC015 - Thanh toán hóa đơn
CREATE PROCEDURE sp_ThanhToanHoaDon
    @maHoaDon      CHAR(14),
    @phuongThucTT  VARCHAR(15),
    @maGiaoDich    VARCHAR(50) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF NOT EXISTS (SELECT 1 FROM HoaDon
                   WHERE maHoaDon = @maHoaDon AND trangThaiThanhToan = 'CHUA_THANH_TOAN')
    BEGIN
        ;THROW 50211, N'Hóa đơn không tồn tại hoặc đã được thanh toán.', 1;
    END;

    UPDATE HoaDon
    SET    phuongThucTT       = @phuongThucTT,
           maGiaoDich         = @maGiaoDich,
           trangThaiThanhToan = 'DA_THANH_TOAN',
           thoiGianThanhToan  = SYSDATETIME()
    WHERE  maHoaDon = @maHoaDon;
END;
GO

-- 4.3 UC002 - Thêm nhân viên: sinh mã maNV = tiền tố vai trò (CCH/TNV/KTV) + 3 số tự tăng riêng theo vai trò
CREATE PROCEDURE sp_ThemNhanVien
    @hoTen        NVARCHAR(100),
    @ngaySinh     DATE,
    @soDienThoai  VARCHAR(11),
    @CCCD         VARCHAR(12),
    @diaChi       NVARCHAR(200) = NULL,
    @ngayVaoLam   DATE = NULL,
    @vaiTro       VARCHAR(20),
    @maNV         VARCHAR(10) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE @prefix CHAR(3) = CASE @vaiTro WHEN 'CHU_CUA_HANG' THEN 'CCH' WHEN 'TIEP_NHAN_VIEN' THEN 'TNV' WHEN 'KY_THUAT_VIEN' THEN 'KTV' END;
    IF @prefix IS NULL
    BEGIN
        ;THROW 50221, N'Vai trò nhân viên không hợp lệ.', 1;
    END;

    DECLARE @stt INT;

    BEGIN TRANSACTION;

    SELECT @stt = ISNULL(MAX(CAST(RIGHT(maNV, 3) AS INT)), 0) + 1
    FROM   NhanVien WITH (UPDLOCK, HOLDLOCK)
    WHERE  LEFT(maNV, 3) = @prefix;

    IF @stt > 999
    BEGIN
        ;THROW 50222, N'Đã vượt quá 999 nhân viên trong vai trò này.', 1;
    END;

    SET @maNV = @prefix + RIGHT('000' + CAST(@stt AS VARCHAR(3)), 3);

    INSERT INTO NhanVien (maNV, hoTen, ngaySinh, soDienThoai, CCCD, diaChi, ngayVaoLam, vaiTro)
    VALUES (@maNV, @hoTen, @ngaySinh, @soDienThoai, @CCCD, @diaChi, ISNULL(@ngayVaoLam, CAST(GETDATE() AS DATE)), @vaiTro);

    COMMIT TRANSACTION;
END;
GO

-- 4.4 Tạo tài khoản đăng nhập cho nhân viên: sinh mã tenDangNhap = tiền tố vai trò + 3 số tự tăng riêng theo vai trò
CREATE PROCEDURE sp_TaoTaiKhoan
    @maNV          VARCHAR(10),
    @matKhau       VARCHAR(255),
    @tenDangNhap   VARCHAR(50) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE @vaiTro VARCHAR(20);
    SELECT @vaiTro = vaiTro FROM NhanVien WHERE maNV = @maNV;
    IF @vaiTro IS NULL
    BEGIN
        ;THROW 50231, N'Nhân viên không tồn tại.', 1;
    END;

    DECLARE @prefix CHAR(3) = CASE @vaiTro WHEN 'CHU_CUA_HANG' THEN 'CCH' WHEN 'TIEP_NHAN_VIEN' THEN 'TNV' WHEN 'KY_THUAT_VIEN' THEN 'KTV' END;
    DECLARE @stt INT;

    BEGIN TRANSACTION;

    SELECT @stt = ISNULL(MAX(CAST(RIGHT(t.tenDangNhap, 3) AS INT)), 0) + 1
    FROM   TaiKhoan t WITH (UPDLOCK, HOLDLOCK)
    WHERE  LEFT(t.tenDangNhap, 3) = @prefix;

    IF @stt > 999
    BEGIN
        ;THROW 50232, N'Đã vượt quá 999 tài khoản trong vai trò này.', 1;
    END;

    SET @tenDangNhap = @prefix + RIGHT('000' + CAST(@stt AS VARCHAR(3)), 3);

    INSERT INTO TaiKhoan (maNV, tenDangNhap, matKhau)
    VALUES (@maNV, @tenDangNhap, @matKhau);

    COMMIT TRANSACTION;
END;
GO

-- 4.5 UC010 - Tiếp nhận xe: sinh mã maPhieu = PSC + ddMMyyyyAXXX
CREATE PROCEDURE sp_LapPhieuTiepNhan
    @bienSoXe       VARCHAR(15),
    @maNVTiepNhan   VARCHAR(10),
    @maNVKyThuat    VARCHAR(10) = NULL,
    @yeuCauBanDau   NVARCHAR(500),
    @maPhieu        VARCHAR(15) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE @now    DATETIME2(0) = SYSDATETIME();
    DECLARE @prefix CHAR(12) = 'PSC' + REPLACE(CONVERT(CHAR(10), @now, 103), '/', '')
                              + CASE WHEN DATEPART(HOUR, @now) < 12 THEN 'A' ELSE 'P' END;
    DECLARE @stt INT;

    BEGIN TRANSACTION;

    SELECT @stt = ISNULL(MAX(CAST(RIGHT(maPhieu, 3) AS INT)), 0) + 1
    FROM   PhieuTiepNhanSuaChua WITH (UPDLOCK, HOLDLOCK)
    WHERE  LEFT(maPhieu, 12) = @prefix;

    IF @stt > 999
    BEGIN
        ;THROW 50241, N'Đã vượt quá 999 phiếu tiếp nhận trong một ca.', 1;
    END;

    SET @maPhieu = @prefix + RIGHT('000' + CAST(@stt AS VARCHAR(3)), 3);

    INSERT INTO PhieuTiepNhanSuaChua (maPhieu, bienSoXe, maNVTiepNhan, maNVKyThuat, ngayGioTiepNhan, yeuCauBanDau)
    VALUES (@maPhieu, @bienSoXe, @maNVTiepNhan, @maNVKyThuat, @now, @yeuCauBanDau);

    COMMIT TRANSACTION;
END;
GO

-- 4.6 UC012 - Lập phiếu đổi trả: sinh mã maPhieuDT = PDT + ddMMyyyyAXX
CREATE PROCEDURE sp_LapPhieuDoiTra
    @maHoaDon     CHAR(14),
    @maNV         VARCHAR(10),
    @lyDoDoiTra   NVARCHAR(500),
    @maPhieuDT    VARCHAR(14) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE @now    DATETIME2(0) = SYSDATETIME();
    DECLARE @prefix CHAR(12) = 'PDT' + REPLACE(CONVERT(CHAR(10), @now, 103), '/', '')
                              + CASE WHEN DATEPART(HOUR, @now) < 12 THEN 'A' ELSE 'P' END;
    DECLARE @stt INT;

    BEGIN TRANSACTION;

    SELECT @stt = ISNULL(MAX(CAST(RIGHT(maPhieuDT, 2) AS INT)), 0) + 1
    FROM   PhieuDoiTra WITH (UPDLOCK, HOLDLOCK)
    WHERE  LEFT(maPhieuDT, 12) = @prefix;

    IF @stt > 99
    BEGIN
        ;THROW 50251, N'Đã vượt quá 99 phiếu đổi trả trong một ca.', 1;
    END;

    SET @maPhieuDT = @prefix + RIGHT('00' + CAST(@stt AS VARCHAR(2)), 2);

    INSERT INTO PhieuDoiTra (maPhieuDT, maHoaDon, maNV, ngayLap, lyDoDoiTra)
    VALUES (@maPhieuDT, @maHoaDon, @maNV, @now, @lyDoDoiTra);

    COMMIT TRANSACTION;
END;
GO

-- 4.7 UC013 - Tạo yêu cầu nhập linh kiện: sinh mã maYeuCau = PYC + ddMMyyyyAXX
CREATE PROCEDURE sp_TaoYeuCauNhapLinhKien
    @maLK            VARCHAR(10),
    @maNVLap         VARCHAR(10),
    @soLuongYeuCau   INT,
    @ghiChu          NVARCHAR(300),
    @maYeuCau        VARCHAR(14) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE @now    DATETIME2(0) = SYSDATETIME();
    DECLARE @prefix CHAR(12) = 'PYC' + REPLACE(CONVERT(CHAR(10), @now, 103), '/', '')
                              + CASE WHEN DATEPART(HOUR, @now) < 12 THEN 'A' ELSE 'P' END;
    DECLARE @stt INT;

    BEGIN TRANSACTION;

    SELECT @stt = ISNULL(MAX(CAST(RIGHT(maYeuCau, 2) AS INT)), 0) + 1
    FROM   PhieuYeuCauNhapLinhKien WITH (UPDLOCK, HOLDLOCK)
    WHERE  LEFT(maYeuCau, 12) = @prefix;

    IF @stt > 99
    BEGIN
        ;THROW 50261, N'Đã vượt quá 99 yêu cầu nhập linh kiện trong một ca.', 1;
    END;

    SET @maYeuCau = @prefix + RIGHT('00' + CAST(@stt AS VARCHAR(2)), 2);

    INSERT INTO PhieuYeuCauNhapLinhKien (maYeuCau, maLK, maNVLap, soLuongYeuCau, ghiChu, ngayTao)
    VALUES (@maYeuCau, @maLK, @maNVLap, @soLuongYeuCau, @ghiChu, @now);

    COMMIT TRANSACTION;
END;
GO

-- =====================================================================
-- PHẦN 5. VIEW - THUỘC TÍNH DẪN XUẤT (không lưu trong bảng)
--   /thue tính theo 3 loại: bán lẻ (chỉ có linh kiện) 1.5%, dịch vụ (chỉ có dịch vụ) 7%,
--   trọn gói (có cả hai) 4.5% - áp trên tongTienLinhKien/tongTienDichVu (chưa trừ khuyến mãi).
-- =====================================================================

-- /tongTienDichVu, /tongTienLinhKien, /tongTien của PhieuTiepNhanSuaChua
CREATE VIEW vw_PhieuSuaChua_TongTien
AS
SELECT p.maPhieu,
       dv.tong        AS tongTienDichVu,
       lk.tong        AS tongTienLinhKien,
       dv.tong + lk.tong AS tongTien
FROM   PhieuTiepNhanSuaChua p
CROSS APPLY (SELECT ISNULL(SUM(thanhTien), 0) AS tong FROM ChiTietDichVu  WHERE maPhieu = p.maPhieu) dv
CROSS APPLY (SELECT ISNULL(SUM(thanhTien), 0) AS tong FROM ChiTietLinhKien WHERE maPhieu = p.maPhieu) lk;
GO

-- /tongTienDichVu, /tongTienLinhKien, /tienGiam, /thue, /tongThanhToan của HoaDon
--   Khuyến mãi giảm theo % trên các dòng thuộc danh sách áp dụng; nếu chương trình không liệt kê
--   dịch vụ/linh kiện nào thì áp dụng cho toàn bộ hóa đơn.
--   tongThanhToan = (tongTienDichVu + tongTienLinhKien - tienGiam) + thue
CREATE VIEW vw_HoaDon_TongTien
AS
SELECT h.maHoaDon, h.maPhieu, h.maKM,
       t.tongTienDichVu,
       t.tongTienLinhKien,
       ISNULL(g.tienGiam, 0)                                          AS tienGiam,
       th.thue                                                        AS thue,
       t.tongTien - ISNULL(g.tienGiam, 0) + th.thue                   AS tongThanhToan
FROM   HoaDon h
JOIN   vw_PhieuSuaChua_TongTien t ON t.maPhieu = h.maPhieu
CROSS APPLY (
    SELECT CAST(CASE WHEN t.tongTienDichVu = 0 AND t.tongTienLinhKien > 0 THEN t.tongTienLinhKien * 0.015
                      WHEN t.tongTienLinhKien = 0 AND t.tongTienDichVu > 0 THEN t.tongTienDichVu * 0.07
                      WHEN t.tongTienDichVu > 0 AND t.tongTienLinhKien > 0 THEN (t.tongTienLinhKien + t.tongTienDichVu) * 0.045
                      ELSE 0 END AS DECIMAL(18,0)) AS thue
) th
OUTER APPLY (
    SELECT CAST(ROUND(km.mucGiamPhanTram / 100.0 * (
              ( SELECT ISNULL(SUM(c.thanhTien), 0) FROM ChiTietDichVu c
                WHERE c.maPhieu = h.maPhieu
                  AND ( x.apDungTatCa = 1
                        OR EXISTS (SELECT 1 FROM KhuyenMai_DichVu kd WHERE kd.maKM = km.maKM AND kd.maDV = c.maDV) ) )
            + ( SELECT ISNULL(SUM(c.thanhTien), 0) FROM ChiTietLinhKien c
                WHERE c.maPhieu = h.maPhieu
                  AND ( x.apDungTatCa = 1
                        OR EXISTS (SELECT 1 FROM KhuyenMai_LinhKien kl WHERE kl.maKM = km.maKM AND kl.maLK = c.maLK) ) )
           ), 0) AS DECIMAL(18,0)) AS tienGiam
    FROM   KhuyenMai km
    CROSS APPLY (SELECT CASE WHEN NOT EXISTS (SELECT 1 FROM KhuyenMai_DichVu  WHERE maKM = km.maKM)
                              AND NOT EXISTS (SELECT 1 FROM KhuyenMai_LinhKien WHERE maKM = km.maKM)
                             THEN 1 ELSE 0 END AS apDungTatCa) x
    WHERE  km.maKM = h.maKM
) g;
GO

-- /soTienHoanTra của PhieuDoiTra (chỉ tính các mục có hướng xử lý HOAN_TIEN)
CREATE VIEW vw_PhieuDoiTra_HoanTien
AS
SELECT p.maPhieuDT, p.maHoaDon, p.ngayLap,
       ISNULL(SUM(CASE WHEN c.huongXuLy = 'HOAN_TIEN' THEN c.thanhTien END), 0) AS soTienHoanTra
FROM   PhieuDoiTra p
LEFT JOIN ChiTietDoiTra c ON c.maPhieuDT = p.maPhieuDT
GROUP BY p.maPhieuDT, p.maHoaDon, p.ngayLap;
GO

-- UC001 - Doanh thu theo ngày: hoàn tiền đổi trả được trừ vào ĐÚNG NGÀY phát sinh (Mục 3.1)
CREATE VIEW vw_DoanhThuTheoNgay
AS
SELECT ngay,
       SUM(doanhThu)                AS doanhThu,
       SUM(hoanTra)                 AS hoanTra,
       SUM(doanhThu) - SUM(hoanTra) AS doanhThuThuan
FROM (
    SELECT CAST(h.thoiGianThanhToan AS DATE) AS ngay,
           v.tongThanhToan                   AS doanhThu,
           CAST(0 AS DECIMAL(18,0))          AS hoanTra
    FROM   HoaDon h
    JOIN   vw_HoaDon_TongTien v ON v.maHoaDon = h.maHoaDon
    WHERE  h.trangThaiThanhToan = 'DA_THANH_TOAN'
    UNION ALL
    SELECT CAST(r.ngayLap AS DATE), CAST(0 AS DECIMAL(18,0)), r.soTienHoanTra
    FROM   vw_PhieuDoiTra_HoanTien r
) x
GROUP BY ngay;
GO

-- UC007 - Tra cứu lịch sử sửa chữa (tìm theo biển số xe / số điện thoại)
CREATE VIEW vw_LichSuSuaChua
AS
SELECT x.bienSoXe, x.hangXe, x.model,
       kh.maKH, kh.hoTen AS tenKhachHang, kh.soDienThoai,
       p.maPhieu, p.ngayGioTiepNhan, p.yeuCauBanDau, p.trangThai,
       nvtn.hoTen AS tiepNhanVien,
       nvkt.hoTen AS kyThuatVien,
       t.tongTienDichVu, t.tongTienLinhKien, t.tongTien
FROM   PhieuTiepNhanSuaChua p
JOIN   Xe x        ON x.bienSoXe = p.bienSoXe
JOIN   KhachHang kh ON kh.maKH = x.maKH
JOIN   NhanVien nvtn ON nvtn.maNV = p.maNVTiepNhan
LEFT JOIN NhanVien nvkt ON nvkt.maNV = p.maNVKyThuat
JOIN   vw_PhieuSuaChua_TongTien t ON t.maPhieu = p.maPhieu;
GO

-- =====================================================================
-- PHẦN 6. DỮ LIỆU MẪU
-- (mã KH/NV/KM/phiếu... do sequence tự sinh; mật khẩu mẫu 'Carzone@123' băm SHA-256
--  CHỈ để thử nghiệm - ứng dụng thật nên dùng bcrypt/PBKDF2 và không lưu mật khẩu mẫu)
-- =====================================================================

-- Danh mục dịch vụ / linh kiện lấy từ Bảng 1.2 (bảng giá dán tại quầy)
INSERT INTO DichVu (maDV, tenDV, donGia, donViTinh, moTa) VALUES
 ('DV00001', N'Thay dầu động cơ',        250000, N'lần', N'Đã gồm công'),
 ('DV00002', N'Bảo dưỡng phanh',         300000, N'lần', N'Chưa gồm má phanh'),
 ('DV00003', N'Sửa hệ thống điện (theo giờ)', 150000, N'giờ', NULL);

INSERT INTO LinhKien (maLK, tenLK, donGia, donViTinh, soLuongTon) VALUES
 ('LK00001', N'Lọc dầu',        80000, N'cái', 20),
 ('LK00002', N'Má phanh (bộ)', 450000, N'bộ',   5),
 ('LK00003', N'Dầu phanh',     120000, N'chai', 12);

-- Nhân sự (1 chủ cửa hàng, 2 tiếp nhận viên, 2 kỹ thuật viên)
-- maNV do sp_ThemNhanVien tự sinh theo vai trò (CCH/TNV/KTV + 3 số); tenDangNhap do sp_TaoTaiKhoan tự sinh tương tự
DECLARE @nv VARCHAR(10), @tk VARCHAR(50), @matKhauMau VARCHAR(255) = CONVERT(VARCHAR(64), HASHBYTES('SHA2_256', 'Carzone@123'), 2);

EXEC sp_ThemNhanVien N'Trần Minh Quân', '1980-05-12', '0901000001', '079080000001', N'Gò Vấp, TP.HCM',    '2018-01-02', 'CHU_CUA_HANG',   @nv OUTPUT;
EXEC sp_TaoTaiKhoan @maNV = @nv, @matKhau = @matKhauMau, @tenDangNhap = @tk OUTPUT;

EXEC sp_ThemNhanVien N'Lê Thị Hoa',     '1995-03-20', '0901000002', '079095000002', N'Tân Bình, TP.HCM',  '2021-06-01', 'TIEP_NHAN_VIEN', @nv OUTPUT;
EXEC sp_TaoTaiKhoan @maNV = @nv, @matKhau = @matKhauMau, @tenDangNhap = @tk OUTPUT;

EXEC sp_ThemNhanVien N'Phạm Ngọc Lan',  '1997-09-08', '0901000003', '079097000003', N'Quận 12, TP.HCM',   '2022-02-15', 'TIEP_NHAN_VIEN', @nv OUTPUT;
EXEC sp_TaoTaiKhoan @maNV = @nv, @matKhau = @matKhauMau, @tenDangNhap = @tk OUTPUT;

EXEC sp_ThemNhanVien N'Vũ Thanh Tùng',  '1990-11-25', '0901000004', '079090000004', N'Bình Thạnh, TP.HCM','2019-08-01', 'KY_THUAT_VIEN',  @nv OUTPUT;
EXEC sp_TaoTaiKhoan @maNV = @nv, @matKhau = @matKhauMau, @tenDangNhap = @tk OUTPUT;

EXEC sp_ThemNhanVien N'Đỗ Văn Hùng',    '1988-07-14', '0901000005', '079088000005', N'Thủ Đức, TP.HCM',   '2020-03-10', 'KY_THUAT_VIEN',  @nv OUTPUT;
EXEC sp_TaoTaiKhoan @maNV = @nv, @matKhau = @matKhauMau, @tenDangNhap = @tk OUTPUT;
GO

-- Chương trình khuyến mãi mẫu (do Chủ cửa hàng tạo): giảm 10% cho dịch vụ bảo dưỡng phanh
INSERT INTO KhuyenMai (maNV, tenChuongTrinh, mucGiamPhanTram, ngayBatDau, ngayKetThuc, ghiChu)
SELECT maNV, N'Ưu đãi bảo dưỡng phanh', 10, '2026-09-01', '2026-12-31', N'Giảm 10% dịch vụ bảo dưỡng phanh'
FROM   NhanVien WHERE vaiTro = 'CHU_CUA_HANG';

INSERT INTO KhuyenMai_DichVu (maKM, maDV)
SELECT maKM, 'DV00002' FROM KhuyenMai WHERE tenChuongTrinh = N'Ưu đãi bảo dưỡng phanh';
GO