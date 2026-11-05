package backup;

/**
 * Kết quả thực thi một phiên sao lưu cơ sở dữ liệu.
 */
public record BackupResult(
        BackupType type,
        boolean success,
        String fileName,
        String filePath,
        long sizeBytes,
        long durationMs,
        String errorMessage
) {
    public boolean thanhCong() {
        return success;
    }

    public String thongBaoLoi() {
        return errorMessage;
    }

    public static BackupResult thanhCong(BackupType type, String fileName, String filePath, long sizeBytes, long durationMs) {
        return new BackupResult(type, true, fileName, filePath, sizeBytes, durationMs, null);
    }

    public static BackupResult thatBai(BackupType type, String fileName, String filePath, long durationMs, String errorMessage) {
        return new BackupResult(type, false, fileName, filePath, -1, durationMs, errorMessage);
    }
}

