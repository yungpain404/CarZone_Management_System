package connectDB;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.net.InetSocketAddress;
import java.net.Socket;

/** Giữ một EntityManagerFactory dùng chung cho cả ứng dụng. dao mở EntityManager cho mỗi thao tác. */
public final class JpaUtil {
    private static final String PERSISTENCE_UNIT = "CarZonePU";
    private static EntityManagerFactory emf;
    private static Boolean online = null;

    private JpaUtil() {
    }

    /** Kiểm tra nhanh (300ms) cổng DB có đang lắng nghe không để tránh bị treo 30s khi DB offline. */
    public static boolean isDatabaseOnline() {
        if (online != null) return online;
        try (Socket socket = new Socket()) {
            int port = Integer.parseInt(DatabaseConfig.layCongSql());
            socket.connect(new InetSocketAddress("localhost", port), 300);
            online = true;
        } catch (Exception e) {
            online = false;
        }
        return online;
    }

    public static synchronized EntityManagerFactory getEntityManagerFactory() {
        if (!isDatabaseOnline()) {
            throw new IllegalStateException("Cơ sở dữ liệu đang ngoại tuyến.");
        }
        if (emf == null) {
            emf = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT, DatabaseConfig.thuocTinhJpa());
        }
        return emf;
    }

    public static EntityManager createEntityManager() {
        return getEntityManagerFactory().createEntityManager();
    }

    /** Gọi khi thoát ứng dụng. */
    public static synchronized void close() {
        if (emf != null && emf.isOpen()) emf.close();
        emf = null;
    }
}
