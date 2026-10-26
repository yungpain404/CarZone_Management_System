package connectDB;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

/** Giữ một EntityManagerFactory dùng chung cho cả ứng dụng. dao mở EntityManager cho mỗi thao tác. */
public final class JpaUtil {
    private static final String PERSISTENCE_UNIT = "CarZonePU";
    private static EntityManagerFactory emf;

    private JpaUtil() {
    }

    public static synchronized EntityManagerFactory getEntityManagerFactory() {
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
