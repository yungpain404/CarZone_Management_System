package backup;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

/**
 * Khởi tạo và quản lý Quartz Scheduler cho các job backup tự động.
 *
 * Lịch:
 *   FULL        → 00:00 và 12:00 hàng ngày    (misfire: FIRE_NOW)
 *   DIFF        → 04:00, 08:00, 16:00, 20:00   (misfire: DO_NOTHING)
 *   LOG         → mỗi 15 phút                  (misfire: DO_NOTHING)
 *
 * Khoá ưu tiên (ReentrantLock fair=true) nằm trong BackupService:
 *   Full/Diff   → chờ tối đa 10 phút rồi chạy
 *   Log         → tryLock 200ms, nếu bận thì skip
 */
public class BackupScheduler {

    private static final Logger log = LoggerFactory.getLogger(BackupScheduler.class);

    // Singleton
    private static volatile BackupScheduler instance;
    private Scheduler scheduler;

    private BackupScheduler() {}

    public static BackupScheduler getInstance() {
        if (instance == null) {
            synchronized (BackupScheduler.class) {
                if (instance == null) {
                    instance = new BackupScheduler();
                }
            }
        }
        return instance;
    }

    /**
     * Khởi động BackupService (kiểm tra msdb, recovery model, v.v.) rồi khởi động Quartz.
     * Gọi từ thread nền khi app startup – KHÔNG gọi trên EDT.
     */
    public void start() {
        // Chỉ chạy scheduler sao lưu tự động khi người dùng là Chủ cửa hàng
        if (!session.UserSession.getInstance().coVaiTro(enums.VaiTro.CHU_CUA_HANG)) {
            log.info("[BackupScheduler] Bỏ qua khởi động scheduler: Chỉ chạy sao lưu tự động trên máy Chủ cửa hàng (vai trò hiện tại: {}).",
                    session.UserSession.getInstance().getVaiTro());
            return;
        }

        try {
            if (scheduler != null && scheduler.isStarted() && !scheduler.isShutdown()) {
                log.info("[BackupScheduler] Quartz Scheduler đã đang chạy.");
                return;
            }

            // 1. Kiểm tra môi trường backup (recovery model, first full, v.v.)
            BackupService.getInstance().khoiTaoStartup();

            // 2. Load quartz.properties
            Properties props = new Properties();
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("quartz.properties")) {
                if (is != null) {
                    props.load(is);
                } else {
                    log.warn("[BackupScheduler] Không tìm thấy quartz.properties trên classpath – dùng cấu hình mặc định");
                }
            }

            StdSchedulerFactory factory = new StdSchedulerFactory(props);
            scheduler = factory.getScheduler();

            // 3. Tạo JobDetails
            JobDetail fullJob = JobBuilder.newJob(FullBackupJob.class)
                    .withIdentity("fullBackup", "carzone")
                    .storeDurably(true)
                    .withDescription("Backup FULL – 00:00 và 12:00 hàng ngày")
                    .build();

            JobDetail diffJob = JobBuilder.newJob(DiffBackupJob.class)
                    .withIdentity("diffBackup", "carzone")
                    .storeDurably(true)
                    .withDescription("Backup DIFFERENTIAL – 04:00, 08:00, 16:00, 20:00")
                    .build();

            JobDetail logJob = JobBuilder.newJob(LogBackupJob.class)
                    .withIdentity("logBackup", "carzone")
                    .storeDurably(true)
                    .withDescription("Backup TRANSACTION LOG – mỗi 15 phút")
                    .build();

            // 4. Tạo CronTriggers với misfire instruction phù hợp

            // Full: FIRE_NOW nếu bị missed – không để mất bản full
            CronTrigger fullTrigger = TriggerBuilder.newTrigger()
                    .withIdentity("fullTrigger", "carzone")
                    .forJob(fullJob)
                    .withSchedule(CronScheduleBuilder
                            .cronSchedule("0 0 0,12 * * ?")
                            .withMisfireHandlingInstructionFireAndProceed())
                    .build();

            // Diff: DO_NOTHING nếu missed – bản diff kế tiếp sẽ bao gồm đủ thay đổi
            CronTrigger diffTrigger = TriggerBuilder.newTrigger()
                    .withIdentity("diffTrigger", "carzone")
                    .forJob(diffJob)
                    .withSchedule(CronScheduleBuilder
                            .cronSchedule("0 0 4,8,16,20 * * ?")
                            .withMisfireHandlingInstructionDoNothing())
                    .build();

            // Log: DO_NOTHING nếu missed – bản log kế tiếp đã chứa đủ giao dịch
            CronTrigger logTrigger = TriggerBuilder.newTrigger()
                    .withIdentity("logTrigger", "carzone")
                    .forJob(logJob)
                    .withSchedule(CronScheduleBuilder
                            .cronSchedule("0 0/15 * * * ?")
                            .withMisfireHandlingInstructionDoNothing())
                    .build();

            // 5. Đăng ký vào scheduler
            scheduler.scheduleJob(fullJob, fullTrigger);
            scheduler.scheduleJob(diffJob, diffTrigger);
            scheduler.scheduleJob(logJob, logTrigger);

            // 6. Start
            scheduler.start();
            log.info("[BackupScheduler] Quartz Scheduler đã khởi động. FULL(0/12h), DIFF(4/8/16/20h), LOG(mỗi 15 phút)");

        } catch (Exception e) {
            log.error("[BackupScheduler] Không thể khởi động Quartz Scheduler: {}", e.getMessage(), e);
        }
    }

    /**
     * Dừng scheduler – chờ các job đang chạy hoàn tất trước khi trả về.
     * Gọi từ shutdown hook (AppShutdownManager).
     */
    public void shutdown() {
        if (scheduler != null) {
            try {
                if (!scheduler.isShutdown()) {
                    log.info("[BackupScheduler] Đang dừng Quartz Scheduler (chờ job hoàn tất)…");
                    scheduler.shutdown(true); // waitForJobsToComplete = true
                    log.info("[BackupScheduler] Quartz Scheduler đã dừng.");
                }
            } catch (SchedulerException e) {
                log.error("[BackupScheduler] Lỗi khi dừng Quartz Scheduler: {}", e.getMessage(), e);
            }
        }
    }

    /** Kiểm tra scheduler còn sống không – dùng trong kiểm tra trạng thái. */
    public boolean dangChay() {
        try {
            return scheduler != null && scheduler.isStarted() && !scheduler.isShutdown();
        } catch (SchedulerException e) {
            return false;
        }
    }
}

