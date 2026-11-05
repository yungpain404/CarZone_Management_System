SET ANSI_NULLS ON;
GO
SET QUOTED_IDENTIFIER ON;
GO

-- Sinh mã DV trong SQL Server; khóa ứng dụng tránh trùng mã khi tạo đồng thời.
CREATE OR ALTER PROCEDURE sp_ThemDichVu
    @tenDV NVARCHAR(150),
    @donGia DECIMAL(18,0),
    @donViTinh NVARCHAR(30),
    @moTa NVARCHAR(500) = NULL,
    @dangCungCap BIT = 1,
    @maDV VARCHAR(10) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE @lockResult INT;
    DECLARE @stt INT;

    BEGIN TRANSACTION;
    EXEC @lockResult = sys.sp_getapplock
        @Resource = 'CarZone.DichVu.MaDV',
        @LockMode = 'Exclusive',
        @LockOwner = 'Transaction',
        @LockTimeout = 10000;

    IF @lockResult < 0
    BEGIN
        ;THROW 50270, N'Không thể cấp mã dịch vụ lúc này. Vui lòng thử lại.', 1;
    END;

    SELECT @stt = ISNULL(MAX(TRY_CONVERT(INT, SUBSTRING(maDV, 3, 8))), 0) + 1
    FROM dbo.DichVu;

    IF @stt > 99999999
    BEGIN
        ;THROW 50271, N'Đã vượt quá số lượng mã dịch vụ có thể cấp.', 1;
    END;

    SET @maDV = 'DV' + CASE WHEN @stt < 100000
        THEN RIGHT('00000' + CAST(@stt AS VARCHAR(8)), 5)
        ELSE CAST(@stt AS VARCHAR(8)) END;

    INSERT INTO dbo.DichVu (maDV, tenDV, donGia, donViTinh, moTa, dangCungCap)
    VALUES (@maDV, @tenDV, @donGia, @donViTinh, @moTa, @dangCungCap);

    COMMIT TRANSACTION;
END;
GO
