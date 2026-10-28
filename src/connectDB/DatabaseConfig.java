package connectDB;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Đọc cấu hình kết nối từ infra/docker/.env (cùng file Docker Compose đang dùng) để không ghi cứng
 * địa chỉ/mật khẩu trong mã nguồn. Thứ tự ưu tiên: biến môi trường của hệ điều hành, rồi file .env.
 * Khóa: SQLSERVER_DATABASE, SQLSERVER_SA_PASSWORD, SQLSERVER_PORT.
 */
public final class DatabaseConfig {
    /** Đường dẫn .env, tính từ thư mục gốc dự án (thư mục chạy ứng dụng). */
    private static final Path ENV_FILE = Path.of("infra", "docker", ".env");

    private DatabaseConfig() {}

    private static Map<String, String> docEnv() {
        Map<String, String> m = new HashMap<>();
        try {
            List<String> lines = Files.readAllLines(ENV_FILE, StandardCharsets.UTF_8);
            for (String line : lines) {
                String t = line.strip();
                if (t.isEmpty() || t.startsWith("#")) continue;
                int i = t.indexOf('=');
                if (i <= 0) continue;
                String v = t.substring(i + 1).strip();
                if (v.length() >= 2 && (v.startsWith("\"") && v.endsWith("\"")
                        || v.startsWith("'") && v.endsWith("'"))) {
                    v = v.substring(1, v.length() - 1);
                }
                m.put(t.substring(0, i).strip(), v);
            }
        } catch (IOException e) {
            // Không có file .env: dùng biến môi trường hệ điều hành (xem lay()).
        }
        return m;
    }

    private static String lay(Map<String, String> env, String khoa) {
        String v = System.getenv(khoa);
        if (v == null || v.isBlank()) v = env.get(khoa);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Thiếu cấu hình " + khoa
                    + " (biến môi trường hoặc " + ENV_FILE + ").");
        }
        return v;
    }

    public static String layCongSql() {
        try {
            return lay(docEnv(), "SQLSERVER_PORT");
        } catch (Exception e) {
            return "1433";
        }
    }

    /** JDBC URL đầy đủ để truyền vào DriverManager.getConnection(). */
    public static String layJdbcUrl() {
        Map<String, String> env = docEnv();
        return "jdbc:sqlserver://localhost:" + lay(env, "SQLSERVER_PORT")
                + ";databaseName=" + lay(env, "SQLSERVER_DATABASE")
                + ";encrypt=true;trustServerCertificate=true;loginTimeout=3;connectRetryCount=0";
    }

    /** Mật khẩu SA – đọc từ .env, không bao giờ hard-code. */
    public static String layMatKhau() {
        return lay(docEnv(), "SQLSERVER_SA_PASSWORD");
    }
}
