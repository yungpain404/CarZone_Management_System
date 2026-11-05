SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

CREATE TABLE QuyenChucNang (
    vaiTro VARCHAR(20) NOT NULL,
    maChucNang VARCHAR(40) NOT NULL,
    duocPhep BIT NOT NULL CONSTRAINT DF_QuyenChucNang_duocPhep DEFAULT (1),
    ngayCapNhat DATETIME2(0) NOT NULL CONSTRAINT DF_QuyenChucNang_ngayCapNhat DEFAULT (SYSDATETIME()),
    CONSTRAINT PK_QuyenChucNang PRIMARY KEY (vaiTro, maChucNang),
    CONSTRAINT CK_QuyenChucNang_vaiTro CHECK (vaiTro IN ('TIEP_NHAN_VIEN', 'KY_THUAT_VIEN')),
    CONSTRAINT CK_QuyenChucNang_maChucNang CHECK (maChucNang IN (
        'NAV_THONG_KE_TIEP_NHAN', 'NAV_KHACH_HANG_XE', 'NAV_PHIEU_TIEP_NHAN',
        'NAV_HOA_DON', 'NAV_DOI_TRA', 'NAV_TRA_CUU', 'NAV_THONG_KE',
        'NAV_PHIEU_PHAN_CONG', 'NAV_CAP_NHAT_SUA_CHUA', 'NAV_YC_LINH_KIEN'
    ))
);
GO

-- Cấp sẵn quyền theo menu hiện hành. Chủ cửa hàng và các mục Dashboard/Hồ sơ
-- luôn được giữ cố định, không lưu trong bảng cấu hình này.
INSERT INTO QuyenChucNang (vaiTro, maChucNang, duocPhep) VALUES
('TIEP_NHAN_VIEN', 'NAV_THONG_KE_TIEP_NHAN', 1),
('TIEP_NHAN_VIEN', 'NAV_KHACH_HANG_XE', 1),
('TIEP_NHAN_VIEN', 'NAV_PHIEU_TIEP_NHAN', 1),
('TIEP_NHAN_VIEN', 'NAV_HOA_DON', 1),
('TIEP_NHAN_VIEN', 'NAV_DOI_TRA', 1),
('TIEP_NHAN_VIEN', 'NAV_TRA_CUU', 1),
('KY_THUAT_VIEN', 'NAV_THONG_KE', 1),
('KY_THUAT_VIEN', 'NAV_PHIEU_PHAN_CONG', 1),
('KY_THUAT_VIEN', 'NAV_CAP_NHAT_SUA_CHUA', 1),
('KY_THUAT_VIEN', 'NAV_YC_LINH_KIEN', 1),
('KY_THUAT_VIEN', 'NAV_TRA_CUU', 1);
GO
