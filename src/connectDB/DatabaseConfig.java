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
 * Khóa: SQLSERVER_DATABASE, SQLSERVER_SA_PASSWORD, SQLSERVER_PORT. Máy chạy ứng dụng là máy đang chạy Docker (localhost).
 */
public final class DatabaseConfig {
    /** Đường dẫn .env, tính từ thư mục gốc dự án (thư mục chạy ứng dụng). */
    private static final Path ENV_FILE = Path.of("infra", "docker", ".env");

    private DatabaseConfig() {
    }

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
                if (v.length() >= 2 && (v.startsWith("\"") && v.endsWith("\"") || v.startsWith("'") && v.endsWith("'"))) {
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
            throw new IllegalStateException("Thiếu cấu hình " + khoa + " (biến môi trường hoặc " + ENV_FILE + ").");
        }
        return v;
    }

    /** Các thuộc tính JPA để truyền vào Persistence.createEntityManagerFactory("CarZonePU", ...). */
    public static Map<String, Object> thuocTinhJpa() {
        Map<String, String> env = docEnv();
        Map<String, Object> p = new HashMap<>();
        p.put("jakarta.persistence.jdbc.url", "jdbc:sqlserver://localhost:" + lay(env, "SQLSERVER_PORT")
                + ";databaseName=" + lay(env, "SQLSERVER_DATABASE") + ";encrypt=true;trustServerCertificate=true");
        p.put("jakarta.persistence.jdbc.user", "sa");
        p.put("jakarta.persistence.jdbc.password", lay(env, "SQLSERVER_SA_PASSWORD"));
        return p;
    }
}
