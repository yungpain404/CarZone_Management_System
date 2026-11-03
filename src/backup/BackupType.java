package backup;

/**
 * Các loại sao lưu cơ sở dữ liệu SQL Server.
 */
public enum BackupType {
    FULL("Full Backup", "bak"),
    DIFF("Differential Backup", "bak"),
    LOG("Transaction Log Backup", "trn");

    private final String tenHienThi;
    private final String duoiTep;

    BackupType(String tenHienThi, String duoiTep) {
        this.tenHienThi = tenHienThi;
        this.duoiTep = duoiTep;
    }

    public String getTenHienThi() {
        return tenHienThi;
    }

    public String getDuoiTep() {
        return duoiTep;
    }
}

