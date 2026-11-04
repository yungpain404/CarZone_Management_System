package backup;

import java.util.Date;

/**
 * Đại diện cho một bản sao lưu trong danh sách lịch sử sao lưu (đọc từ
 * msdb.dbo.backupset).
 */
public record BanSaoLuuItem(
        long backupSetId,
        String databaseName,
        BackupType type,
        Date thoiDiemTao,
        long dungLuongBytes,
        String duongDanTep,
        String tenTep,
        String nguoiThucHien) {
    public String getLoaiHienThi() {
        return type != null ? type.getTenHienThi() : "Khác";
    }
}
