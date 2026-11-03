package backup;

import connectDB.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Quản lý cấu hình sao lưu CSDL (thư mục, ngưỡng thời gian, timeout).
 */
public final class BackupConfig {

    private static final Logger log = LoggerFactory.getLogger("app");
    private static final BackupConfig INSTANCE = new BackupConfig();

    private static String backupDirectory = "/var/opt/mssql/backup";
    private static int recentThresholdMinutes = 5;
    private static int exitTimeoutSeconds = 60;

    static {
        napCauHinh();
    }

    private BackupConfig() {}

    public static BackupConfig getInstance() {
        return INSTANCE;
    }

    private static void napCauHinh() {
        try (InputStream is = BackupConfig.class.getResourceAsStream("/backup.properties")) {
            if (is != null) {
                Properties p = new Properties();
                p.load(is);
                String dir = p.getProperty("backup.directory");
                if (dir != null && !dir.isBlank()) {
                    backupDirectory = dir.trim();
                }
                String recStr = p.getProperty("backup.recent.threshold.minutes");
                if (recStr != null && !recStr.isBlank()) {
                    recentThresholdMinutes = Integer.parseInt(recStr.trim());
                }
                String toStr = p.getProperty("backup.exit.timeout.seconds");
                if (toStr != null && !toStr.isBlank()) {
                    exitTimeoutSeconds = Integer.parseInt(toStr.trim());
                }
            }
        } catch (Throwable t) {
            log.warn("Không thể nạp backup.properties, sử dụng cấu hình mặc định: {}", t.getMessage());
        }

        // Ưu tiên biến môi trường nếu có
        String envDir = System.getenv("SQLSERVER_BACKUP_DIR");
        if (envDir != null && !envDir.isBlank()) {
            backupDirectory = envDir.trim();
        }
    }

    public static String getBackupDirectory() {
        return backupDirectory;
    }

    public static int getRecentThresholdMinutes() {
        return recentThresholdMinutes;
    }

    public static int getExitTimeoutSeconds() {
        return exitTimeoutSeconds;
    }

    /**
     * Tạo đường dẫn thư mục con cho từng loại (FULL, DIFF, LOG).
     */
    public static String getSubdirectory(BackupType type) {
        String base = backupDirectory.replace('\\', '/');
        if (!base.endsWith("/")) {
            base += "/";
        }
        return base + type.name();
    }
}

