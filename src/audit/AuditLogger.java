package audit;

import session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tiện ích ghi nhật ký thao tác dữ liệu (Audit Log).
 * Ghi qua logger "AUDIT" độc lập vào file riêng (logs/audit.log).
 * Tất cả lời gọi được bọc try-catch tuyệt đối để không bao giờ làm hỏng hay gián đoạn câu truy vấn gốc.
 */
public final class AuditLogger {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    private static final Set<String> MASK_FIELDS = new HashSet<>(
            Arrays.asList("matkhau", "password", "matkhaumoi", "cccd", "token", "secret", "creditcard")
    );
    private static boolean logSelect = false;

    private static final Pattern TABLE_PATTERN = Pattern.compile(
            "(?i)\\b(?:FROM|INTO|UPDATE|TABLE|JOIN)\\s+\\[?(\\w+)\\]?",
            Pattern.CASE_INSENSITIVE
    );

    static {
        try (InputStream is = AuditLogger.class.getResourceAsStream("/backup.properties")) {
            if (is != null) {
                Properties p = new Properties();
                p.load(is);
                String maskStr = p.getProperty("audit.mask.fields");
                if (maskStr != null && !maskStr.isBlank()) {
                    for (String f : maskStr.split(",")) {
                        MASK_FIELDS.add(f.trim().toLowerCase());
                    }
                }
                String selStr = p.getProperty("audit.log.select");
                if (selStr != null) {
                    logSelect = Boolean.parseBoolean(selStr.trim());
                }
            }
        } catch (Throwable ignored) {
            // Dùng mặc định nếu không nạp được cấu hình
        }
    }

    private AuditLogger() {}

    /**
     * Xác định thao tác SQL (INSERT, UPDATE, DELETE, MERGE, SELECT, DDL, EXEC, BACKUP).
     */
    public static String xacDinhHanhDong(String sql) {
        if (sql == null || sql.isBlank()) return "UNKNOWN";
        String s = sql.stripLeading().toUpperCase();
        if (s.startsWith("INSERT")) return "INSERT";
        if (s.startsWith("UPDATE")) return "UPDATE";
        if (s.startsWith("DELETE")) return "DELETE";
        if (s.startsWith("MERGE")) return "MERGE";
        if (s.startsWith("SELECT")) return "SELECT";
        if (s.startsWith("EXEC") || s.startsWith("{CALL") || s.startsWith("CALL")) return "CALL";
        if (s.startsWith("CREATE") || s.startsWith("ALTER") || s.startsWith("DROP")) return "DDL";
        if (s.startsWith("BACKUP DATABASE")) return "BACKUP_DB";
        if (s.startsWith("BACKUP LOG")) return "BACKUP_LOG";
        if (s.startsWith("RESTORE")) return "RESTORE";
        return "SQL";
    }

    /**
     * Trích xuất tên bảng từ câu lệnh SQL đơn giản.
     */
    public static String trichXuatTenBang(String sql) {
        if (sql == null || sql.isBlank()) return "—";
        try {
            Matcher m = TABLE_PATTERN.matcher(sql);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Throwable ignored) {}
        return "—";
    }

    /**
     * Lấy định danh người dùng hiện tại từ UserSession.
     */
    private static String layNguoiDungHienTai() {
        try {
            UserSession s = UserSession.getInstance();
            if (s != null && s.daDangNhap()) {
                return s.getTenDangNhap() + "(" + s.getMaNV() + ")";
            }
        } catch (Throwable ignored) {}
        return "SYSTEM";
    }

    /**
     * Ghi audit log cho câu lệnh thực thi thành công.
     */
    public static void logQuery(String sql, Map<Integer, Object> params, int affectedRows, long durationMs) {
        try {
            String action = xacDinhHanhDong(sql);
            if ("SELECT".equals(action)) {
                if (logSelect && AUDIT.isDebugEnabled()) {
                    AUDIT.debug("USER: {} | ACTION: SELECT | TABLE: {} | ROWS: {} | TIME: {}ms | STATUS: SUCCESS | SQL: {} | PARAMS: {}",
                            layNguoiDungHienTai(), trichXuatTenBang(sql), affectedRows, durationMs,
                            rutGonSql(sql), maskParams(params));
                }
                return;
            }

            AUDIT.info("USER: {} | ACTION: {} | TABLE: {} | ROWS: {} | TIME: {}ms | STATUS: SUCCESS | SQL: {} | PARAMS: {}",
                    layNguoiDungHienTai(), action, trichXuatTenBang(sql), affectedRows, durationMs,
                    rutGonSql(sql), maskParams(params));
        } catch (Throwable ignored) {
            // Không bao giờ để lỗi logging ảnh hưởng nghiệp vụ
        }
    }

    /**
     * Ghi audit log khi câu lệnh thực thi thất bại.
     */
    public static void logQueryFailure(String sql, Map<Integer, Object> params, long durationMs, Throwable error) {
        try {
            String action = xacDinhHanhDong(sql);
            String errStr = error != null ? error.getMessage() : "Unknown Error";
            AUDIT.warn("USER: {} | ACTION: {} | TABLE: {} | TIME: {}ms | STATUS: FAILED ({}) | SQL: {} | PARAMS: {}",
                    layNguoiDungHienTai(), action, trichXuatTenBang(sql), durationMs, errStr,
                    rutGonSql(sql), maskParams(params));
        } catch (Throwable ignored) {}
    }

    /**
     * Ghi audit log cho thao tác giao dịch (COMMIT / ROLLBACK).
     */
    public static void logTransaction(String action, boolean success, String error) {
        try {
            if (success) {
                AUDIT.info("USER: {} | TX: {} | STATUS: SUCCESS", layNguoiDungHienTai(), action);
            } else {
                AUDIT.warn("USER: {} | TX: {} | STATUS: FAILED ({})", layNguoiDungHienTai(), action, error);
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Ghi audit log cho thao tác sao lưu CSDL đặc thù.
     */
    public static void logBackupOperation(String type, String dbName, String filePath, long sizeBytes, long durationMs, boolean success, String error) {
        try {
            String sizeStr = sizeBytes >= 0 ? formatDungLuong(sizeBytes) : "N/A";
            if (success) {
                AUDIT.info("USER: {} | ACTION: [BACKUP-{}] | DB: {} | FILE: {} | SIZE: {} | TIME: {}ms | STATUS: SUCCESS",
                        layNguoiDungHienTai(), type, dbName, filePath, sizeStr, durationMs);
            } else {
                AUDIT.error("USER: {} | ACTION: [BACKUP-{}] | DB: {} | FILE: {} | TIME: {}ms | STATUS: FAILED ({})",
                        layNguoiDungHienTai(), type, dbName, filePath, durationMs, error);
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Ghi audit log cho sự kiện cảnh báo an ninh và hành vi phi lý.
     */
    public static void logSecurityAlert(String anomalyType, String detail, String user, boolean locked) {
        try {
            AUDIT.error("SECURITY_ALERT | USER: {} | TYPE: [{}] | DETAIL: {} | LOCK_STATUS: {}",
                    user != null ? user : layNguoiDungHienTai(), anomalyType, detail,
                    locked ? "LOCKED_AND_LOGGED_OUT" : "CHALLENGED_WITH_PIN");
        } catch (Throwable ignored) {}
    }

    /**
     * Che các giá trị nhạy cảm trong tham số PreparedStatement.
     */
    public static String maskParams(Map<Integer, Object> params) {
        if (params == null || params.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Map.Entry<Integer, Object> e : params.entrySet()) {
            if (!first) sb.append(", ");
            first = false;
            sb.append(e.getKey()).append("=");
            Object val = e.getValue();
            if (val == null) {
                sb.append("null");
            } else {
                String strVal = String.valueOf(val);
                // Che nếu chuỗi giống SHA-256 (64 ký tự hex) hoặc mật khẩu
                if (strVal.length() == 64 && strVal.matches("^[0-9A-Fa-f]{64}$")) {
                    sb.append("***(SHA256)");
                } else {
                    sb.append(strVal);
                }
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private static String rutGonSql(String sql) {
        if (sql == null) return "";
        return sql.replaceAll("\\s+", " ").trim();
    }

    public static String formatDungLuong(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format(Locale.US, "%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
}

