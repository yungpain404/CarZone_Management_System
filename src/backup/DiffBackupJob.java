package backup;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quartz Job: Backup DIFFERENTIAL.
 * Chạy 04:00, 08:00, 16:00, 20:00 mỗi ngày.
 * Dùng @DisallowConcurrentExecution chống chạy chồng chính nó.
 * Khoá dùng chung (ReentrantLock) trong BackupService điều phối ưu tiên với FullBackupJob và LogBackupJob.
 */
@DisallowConcurrentExecution
public class DiffBackupJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(DiffBackupJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        // Chỉ chạy trên máy của Chủ cửa hàng
        if (!session.UserSession.getInstance().coVaiTro(enums.VaiTro.CHU_CUA_HANG)) {
            log.info("[DiffBackupJob] Bỏ qua: Phiên làm việc hiện tại không phải Chủ cửa hàng.");
            return;
        }

        log.info("[DiffBackupJob] Trigger kích hoạt – bắt đầu DIFFERENTIAL backup");
        BackupResult result = BackupService.getInstance().thucHienBackup(BackupType.DIFF);
        if (!result.thanhCong()) {
            throw new JobExecutionException("DIFF backup thất bại: " + result.thongBaoLoi(), false);
        }
    }
}

