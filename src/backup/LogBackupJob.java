package backup;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quartz Job: Backup TRANSACTION LOG.
 * Chạy mỗi 15 phút.
 * Dùng @DisallowConcurrentExecution chống chạy chồng chính nó.
 * Nếu khoá dùng chung đang bận thì BỎ QUA (skip) mốc này – BackupService xử lý logic đó.
 */
@DisallowConcurrentExecution
public class LogBackupJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(LogBackupJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.debug("[LogBackupJob] Trigger kích hoạt – thử TRANSACTION LOG backup");
        BackupResult result = BackupService.getInstance().thucHienBackup(BackupType.LOG);
        // Nếu bị skip (lock bận) → BackupService đã ghi WARN, không ném exception
        if (!result.thanhCong() && result.thongBaoLoi() != null
                && !result.thongBaoLoi().startsWith("SKIP")) {
            // Lỗi thực sự (không phải skip) → ghi ERROR nhưng không retry
            log.error("[LogBackupJob] LOG backup thất bại: {}", result.thongBaoLoi());
        }
    }
}

