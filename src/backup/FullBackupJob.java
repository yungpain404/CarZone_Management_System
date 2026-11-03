package backup;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quartz Job: Backup FULL.
 * Chạy 00:00 và 12:00 mỗi ngày.
 * Dùng @DisallowConcurrentExecution chống chạy chồng chính nó.
 * Khoá dùng chung (ReentrantLock) trong BackupService điều phối ưu tiên với DiffBackupJob và LogBackupJob.
 */
@DisallowConcurrentExecution
public class FullBackupJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(FullBackupJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        // Chỉ chạy trên máy của Chủ cửa hàng
        if (!session.UserSession.getInstance().coVaiTro(enums.VaiTro.CHU_CUA_HANG)) {
            log.info("[FullBackupJob] Bỏ qua: Phiên làm việc hiện tại không phải Chủ cửa hàng.");
            return;
        }

        log.info("[FullBackupJob] Trigger kích hoạt – bắt đầu FULL backup");
        BackupResult result = BackupService.getInstance().thucHienBackup(BackupType.FULL);
        if (!result.thanhCong()) {
            // Ném JobExecutionException để Quartz ghi nhận job thất bại (không retry vì refireImmediately=false)
            throw new JobExecutionException("FULL backup thất bại: " + result.thongBaoLoi(), false);
        }
    }
}

