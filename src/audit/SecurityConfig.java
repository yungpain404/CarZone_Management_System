package audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalTime;
import java.util.Properties;

/**
 * Cấu hình an ninh, mã PIN cấp 2 và ngưỡng giám sát hành vi phi lý.
 */
public final class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private static String pinHash = "92925488B28AB12584AC8FCAA8A27A0F497B2C62940C8F4FBC8EF19EBC87C43E"; // Mặc định mã PIN: 888888
    private static int maxAttempts = 3;
    private static int velocityDeleteThreshold = 3;
    private static long velocityDeleteWindowMs = 2000L;
    private static LocalTime offHoursStart = LocalTime.of(0, 0);
    private static LocalTime offHoursEnd = LocalTime.of(5, 0);
    private static String ntfyUrl = "https://ntfy.sh";
    private static String ntfyTopic = "carzone-security-alerts";
    private static long whitelistDurationMs = 60_000L; // 60 giây suppression window
    private static boolean autoRestoreEnabled = true; // Bật tự động Auto-Restore khi phát hiện phá hoại CSDL

    static {
        napCauHinh();
    }

    private SecurityConfig() {}

    public static void napCauHinh() {
        try (InputStream is = SecurityConfig.class.getResourceAsStream("/backup.properties")) {
            if (is != null) {
                Properties p = new Properties();
                p.load(is);

                String pHash = p.getProperty("security.pin.hash");
                if (pHash != null && !pHash.isBlank()) {
                    pinHash = pHash.trim().toUpperCase();
                } else {
                    String rawPin = p.getProperty("security.pin.default");
                    if (rawPin != null && !rawPin.isBlank()) {
                        pinHash = bamSha256(rawPin.trim());
                    }
                }

                String maxAttStr = p.getProperty("security.pin.max.attempts");
                if (maxAttStr != null && !maxAttStr.isBlank()) {
                    maxAttempts = Integer.parseInt(maxAttStr.trim());
                }

                String velThres = p.getProperty("security.anomaly.velocity.delete.threshold");
                if (velThres != null && !velThres.isBlank()) {
                    velocityDeleteThreshold = Integer.parseInt(velThres.trim());
                }

                String velWindow = p.getProperty("security.anomaly.velocity.delete.window.ms");
                if (velWindow != null && !velWindow.isBlank()) {
                    velocityDeleteWindowMs = Long.parseLong(velWindow.trim());
                }

                String offStart = p.getProperty("security.anomaly.offhours.start");
                if (offStart != null && !offStart.isBlank()) {
                    offHoursStart = LocalTime.parse(offStart.trim());
                }

                String offEnd = p.getProperty("security.anomaly.offhours.end");
                if (offEnd != null && !offEnd.isBlank()) {
                    offHoursEnd = LocalTime.parse(offEnd.trim());
                }

                String nUrl = p.getProperty("security.alert.ntfy.server");
                if (nUrl != null && !nUrl.isBlank()) {
                    ntfyUrl = nUrl.trim();
                }

                String nTopic = p.getProperty("security.alert.ntfy.topic");
                if (nTopic != null && !nTopic.isBlank()) {
                    ntfyTopic = nTopic.trim();
                }

                String wlDur = p.getProperty("security.whitelist.duration.ms");
                if (wlDur != null && !wlDur.isBlank()) {
                    whitelistDurationMs = Long.parseLong(wlDur.trim());
                }

                String arStr = p.getProperty("security.auto.restore.enabled");
                if (arStr != null && !arStr.isBlank()) {
                    autoRestoreEnabled = Boolean.parseBoolean(arStr.trim());
                }
            }
        } catch (Throwable t) {
            log.warn("Không thể nạp cấu hình an ninh từ backup.properties, dùng mặc định: {}", t.getMessage());
        }

        // Biến môi trường hệ điều hành được ưu tiên
        String envPin = System.getenv("CARZONE_SECURITY_PIN");
        if (envPin != null && !envPin.isBlank()) {
            pinHash = bamSha256(envPin.trim());
        }
    }

    public static boolean kiemTraPin(String rawPin) {
        if (rawPin == null || rawPin.isBlank()) return false;
        String hashed = bamSha256(rawPin.trim());
        return pinHash.equalsIgnoreCase(hashed);
    }

    public static String bamSha256(String s) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256")
                    .digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : h) sb.append(String.format("%02X", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static int getMaxAttempts() {
        return maxAttempts;
    }

    public static int getVelocityDeleteThreshold() {
        return velocityDeleteThreshold;
    }

    public static long getVelocityDeleteWindowMs() {
        return velocityDeleteWindowMs;
    }

    public static LocalTime getOffHoursStart() {
        return offHoursStart;
    }

    public static LocalTime getOffHoursEnd() {
        return offHoursEnd;
    }

    public static String getNtfyUrl() {
        return ntfyUrl;
    }

    public static String getNtfyTopic() {
        return ntfyTopic;
    }

    public static long getWhitelistDurationMs() {
        return whitelistDurationMs;
    }

    public static boolean isAutoRestoreEnabled() {
        return autoRestoreEnabled;
    }
}

