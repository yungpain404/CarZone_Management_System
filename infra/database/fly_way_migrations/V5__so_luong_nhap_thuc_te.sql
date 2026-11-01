SET ANSI_NULLS ON;
GO
SET QUOTED_IDENTIFIER ON;
GO

-- Số thực nhận được lưu riêng; dữ liệu nhập kho cũ được bảo toàn bằng số lượng yêu cầu.
ALTER TABLE dbo.PhieuYeuCauNhapLinhKien
    ADD soLuongNhapThucTe INT NULL;
GO

UPDATE dbo.PhieuYeuCauNhapLinhKien
SET soLuongNhapThucTe = soLuongYeuCau
WHERE trangThai = 'DA_NHAP_KHO';
GO

ALTER TABLE dbo.PhieuYeuCauNhapLinhKien
    ADD CONSTRAINT CK_PYCNLK_SoLuongNhapThucTe
        CHECK (soLuongNhapThucTe IS NULL OR soLuongNhapThucTe > 0),
        CONSTRAINT CK_PYCNLK_NhapKhoSoLuong
        CHECK ((trangThai = 'DA_NHAP_KHO' AND soLuongNhapThucTe IS NOT NULL)
            OR (trangThai <> 'DA_NHAP_KHO' AND soLuongNhapThucTe IS NULL));
GO

CREATE OR ALTER TRIGGER dbo.trg_PYCNLK_KiemTra ON dbo.PhieuYeuCauNhapLinhKien
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF EXISTS (SELECT 1 FROM inserted i JOIN dbo.NhanVien nv ON nv.maNV = i.maNVLap
               WHERE nv.vaiTro <> 'KY_THUAT_VIEN')
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50191, N'Chỉ Kỹ thuật viên mới được gửi yêu cầu nhập linh kiện.', 1;
    END;

    IF EXISTS (SELECT 1 FROM inserted i JOIN dbo.NhanVien nv ON nv.maNV = i.maNVXuLy
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
                 AND NOT ((d.trangThai = 'CHO_DUYET' AND i.trangThai IN ('DA_DUYET', 'TU_CHOI'))
                       OR (d.trangThai = 'DA_DUYET' AND i.trangThai = 'DA_NHAP_KHO')))
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50194, N'Chuyển trạng thái yêu cầu không hợp lệ (Chờ duyệt → Đã duyệt/Từ chối → Đã nhập kho).', 1;
    END;

    IF EXISTS (SELECT 1 FROM inserted i JOIN deleted d ON d.maYeuCau = i.maYeuCau
               WHERE ISNULL(i.soLuongNhapThucTe, -1) <> ISNULL(d.soLuongNhapThucTe, -1)
                 AND NOT (d.trangThai = 'DA_DUYET' AND i.trangThai = 'DA_NHAP_KHO'))
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50195, N'Số lượng thực nhận chỉ được ghi khi xác nhận nhập kho và không thể sửa sau đó.', 1;
    END;

    -- Chỉ cộng số thực nhận khi trạng thái chuyển lần đầu sang DA_NHAP_KHO.
    UPDATE lk
    SET lk.soLuongTon = lk.soLuongTon + received.totalReceived
    FROM dbo.LinhKien lk
    JOIN (SELECT i.maLK, SUM(i.soLuongNhapThucTe) AS totalReceived
          FROM inserted i JOIN deleted d ON d.maYeuCau = i.maYeuCau
          WHERE i.trangThai = 'DA_NHAP_KHO' AND d.trangThai <> 'DA_NHAP_KHO'
          GROUP BY i.maLK) received ON received.maLK = lk.maLK;
END;
GO
