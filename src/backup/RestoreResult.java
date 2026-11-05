package backup;

/**
 * Kết quả thực thi một phiên phục hồi cơ sở dữ liệu (RESTORE DATABASE).
 */
public record RestoreResult(
        boolean success,
        String filePath,
        long durationMs,
        String errorMessage) {
    public static RestoreResult thanhCong(String filePath, long durationMs) {
        return new RestoreResult(true, filePath, durationMs, null);
    }

    public static RestoreResult thatBai(String filePath, long durationMs, String errorMessage) {
        return new RestoreResult(false, filePath, durationMs, errorMessage);
    }
}
