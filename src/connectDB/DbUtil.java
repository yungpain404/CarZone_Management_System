package connectDB;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Tiện ích kết nối JDBC thuần (DriverManager).
 * Mỗi lần gọi {@link #getConnection()} mở một Connection mới; người gọi chịu trách nhiệm đóng
 * (try-with-resources).
 *
 * <pre>
 *   try (Connection con = DbUtil.getConnection()) {
 *       // dùng con...
 *   }
 * </pre>
 */
public final class DbUtil {

    private static Boolean online = null;

    private DbUtil() {}

    // ── Kiểm tra DB online ────────────────────────────────────────────────────

    /**
     * Kiểm tra nhanh (300 ms) cổng DB có lắng nghe không, tránh treo 30 giây khi DB offline.
     * Kết quả được cache trong phiên; reset bằng {@link #resetOnlineCache()}.
     */
    public static boolean isDatabaseOnline() {
        if (online != null) return online;
        try (Socket s = new Socket()) {
            int port = Integer.parseInt(DatabaseConfig.layCongSql());
            s.connect(new InetSocketAddress("localhost", port), 300);
            online = true;
        } catch (Exception e) {
            online = false;
        }
        return online;
    }

    /** Xóa cache trạng thái để lần sau kiểm tra lại (dùng sau khi kết nối thất bại). */
    public static void resetOnlineCache() {
        online = null;
    }

    // ── Mở Connection ─────────────────────────────────────────────────────────

    /**
     * Mở và trả về một Connection JDBC mới. Ném {@link RuntimeException} nếu DB offline hoặc lỗi.
     */
    public static Connection getConnection() {
        if (!isDatabaseOnline()) {
            throw new RuntimeException("Cơ sở dữ liệu đang ngoại tuyến.");
        }
        try {
            return DriverManager.getConnection(
                    DatabaseConfig.layJdbcUrl(),
                    "sa",
                    DatabaseConfig.layMatKhau()
            );
        } catch (SQLException e) {
            throw new RuntimeException("Không thể kết nối cơ sở dữ liệu: " + e.getMessage(), e);
        }
    }

    // ── Tương thích ngược với code gọi JpaUtil.close() ───────────────────────

    /**
     * Không còn cần thiết (không có pool cần đóng), giữ lại để code UI cũ không lỗi biên dịch.
     */
    public static void close() {
        // no-op: DriverManager không có factory cần đóng
    }
}
