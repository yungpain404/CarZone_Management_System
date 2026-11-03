SET ANSI_NULLS ON;
GO
SET QUOTED_IDENTIFIER ON;
GO

-- Sinh mã LK và ghi tồn ban đầu trong SQL Server; tồn về sau do luồng nhập/xuất DB quản lý.
CREATE OR ALTER PROCEDURE sp_ThemLinhKien
    @tenLK NVARCHAR(150),
    @donGia DECIMAL(18,0),
    @donViTinh NVARCHAR(30),
    @soLuongTon INT = 0,
    @dangKinhDoanh BIT = 1,
    @maLK VARCHAR(10) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE @lockResult INT;
    DECLARE @stt INT;

    BEGIN TRANSACTION;
    EXEC @lockResult = sys.sp_getapplock
        @Resource = 'CarZone.LinhKien.MaLK',
        @LockMode = 'Exclusive',
        @LockOwner = 'Transaction',
        @LockTimeout = 10000;

    IF @lockResult < 0
    BEGIN
        ;THROW 50272, N'Không thể cấp mã linh kiện lúc này. Vui lòng thử lại.', 1;
    END;

    SELECT @stt = ISNULL(MAX(TRY_CONVERT(INT, SUBSTRING(maLK, 3, 8))), 0) + 1
    FROM dbo.LinhKien;

    IF @stt > 99999999
    BEGIN
        ;THROW 50273, N'Đã vượt quá số lượng mã linh kiện có thể cấp.', 1;
    END;

    SET @maLK = 'LK' + CASE WHEN @stt < 100000
        THEN RIGHT('00000' + CAST(@stt AS VARCHAR(8)), 5)
        ELSE CAST(@stt AS VARCHAR(8)) END;

    INSERT INTO dbo.LinhKien (maLK, tenLK, donGia, donViTinh, soLuongTon, dangKinhDoanh)
    VALUES (@maLK, @tenLK, @donGia, @donViTinh, @soLuongTon, @dangKinhDoanh);

    COMMIT TRANSACTION;
END;
GO
