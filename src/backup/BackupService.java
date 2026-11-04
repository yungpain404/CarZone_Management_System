package backup;

import audit.AuditLogger;
import connectDB.DatabaseConfig;
import connectDB.DbUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import session.UserSession;

import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Dịch vụ điều phối sao lưu CSDL SQL Server (Full, Differential, Transaction
 * Log).
 * Hỗ trợ môi trường ĐA MÁY TRẠM (Tiếp nhận viên, Kỹ thuật viên, Chủ cửa hàng):
 * 1. Khóa phân tán trên SQL Server qua sp_getapplock (chống xung đột giữa các
 * máy trạm).
 * 2. "Ai backup đầu tiên thì lấy của người đó": Tự động kiểm tra lịch sử trên
 * msdb.dbo.backupset,
 * nếu mốc này đã có máy trạm khác thực hiện trước thì lấy kết quả và bỏ qua,
 * không ghi đè file.
 * 3. Khóa cục bộ ReentrantLock công bằng để bảo vệ luồng nội bộ trên từng máy.
 * 4. Áp dụng thứ tự ưu tiên: Full > Diff > Log.
 */
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger("app");
    private static volatile BackupService instance;

    /**
     * Tên tài nguyên khóa phân tán dùng chung cho toàn bộ hệ thống CarZone trên SQL
     * Server.
     */
    private static final String APP_LOCK_RESOURCE = "CarZone_Backup_Distributed_Lock";

    /** Khóa dùng chung nội bộ JVM chống chạy chồng chéo giữa các luồng cục bộ. */
    private final ReentrantLock backupLock = new ReentrantLock(true);

    /** Thời điểm thành công gần nhất của từng loại backup trên máy cục bộ. */
    private volatile long lastFullBackupTime = 0;
    private volatile long lastDiffBackupTime = 0;
    private volatile long lastLogBackupTime = 0;

    /** Giới hạn tần suất log lỗi lặp lại của LogBackupJob. */
    private String lastLogErrorMessage = null;
    private long lastLogErrorTime = 0;

    private boolean recoveryModelWarningLogged = false;

    private BackupService() {
    }

    public static BackupService getInstance() {
        if (instance == null) {
            synchronized (BackupService.class) {
                if (instance == null) {
                    instance = new BackupService();
                }
            }
        }
        return instance;
    }

    /**
     * Record đại diện cho thông tin bản sao lưu gần nhất ghi nhận trên SQL Server.
     */
    public record ThongTinBackupGanNhat(
            String loai,
            Date thoiDiem,
            long phutTruoc,
            String tenTep,
            String duongDan,
            long dungLuong) {
        /**
         * Kiểm tra xem bản backup này có đủ mới để đại diện cho mốc lịch hiện tại hay
         * không.
         * Nếu đã có máy khác thực hiện trong khoảng thời gian này, máy hiện tại không
         * cần chạy lại.
         */
        public boolean hopLeTrongKhoang(BackupType type) {
            if (phutTruoc < 0)
                return false;
            return switch (type) {
                case FULL -> phutTruoc < 60; // Trong vòng 60 phút (mốc Full cách nhau 12 tiếng)
                case DIFF -> phutTruoc < 30; // Trong vòng 30 phút (mốc Diff cách nhau 4 tiếng)
                case LOG -> phutTruoc < 10; // Trong vòng 10 phút (mốc Log cách nhau 15 phút)
            };
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // KHỞI ĐỘNG VÀ KIỂM TRA ĐIỀU KIỆN BAN ĐẦU
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Kiểm tra điều kiện CSDL khi ứng dụng khởi động:
     * 1. Nạp mốc thời gian backup gần nhất từ msdb.dbo.backupset.
     * 2. Kiểm tra Recovery Model (phải là FULL/BULK_LOGGED).
     * 3. Nếu chưa từng có bản Full, tự động chạy 1 bản Full trước khi các job khác
     * kích hoạt.
     */
    public void khoiTaoStartup() {
        log.info("Bắt đầu kiểm tra trạng thái CSDL và lịch sử sao lưu tại khởi động...");
        String dbName = DatabaseConfig.layTenDatabase();

        try (Connection con = DbUtil.getRawConnection()) {
            // 1. Nạp thời điểm backup gần nhất từ msdb
            lastFullBackupTime = layThoiDiemBackupGanNhatTuMsdb(con, dbName, "D");
            lastDiffBackupTime = layThoiDiemBackupGanNhatTuMsdb(con, dbName, "I");
            lastLogBackupTime = layThoiDiemBackupGanNhatTuMsdb(con, dbName, "L");

            log.info("Lịch sử CSDL [{}]: Full gần nhất={}, Diff gần nhất={}, Log gần nhất={}",
                    dbName,
                    lastFullBackupTime > 0 ? new Date(lastFullBackupTime) : "Chưa có",
                    lastDiffBackupTime > 0 ? new Date(lastDiffBackupTime) : "Chưa có",
                    lastLogBackupTime > 0 ? new Date(lastLogBackupTime) : "Chưa có");

            // 2. Kiểm tra Recovery Model
            String recoveryModel = layRecoveryModel(con, dbName);
            if (!"FULL".equalsIgnoreCase(recoveryModel) && !"BULK_LOGGED".equalsIgnoreCase(recoveryModel)) {
                log.warn("CẢNH BÁO: CSDL [{}] đang ở chế độ Recovery Model '{}'. "
                        + "Sao lưu Transaction Log yêu cầu FULL hoặc BULK_LOGGED. "
                        + "Hệ thống KHÔNG tự đổi cấu hình. Vui lòng chạy câu lệnh SQL: ALTER DATABASE [{}] SET RECOVERY FULL;",
                        dbName, recoveryModel, dbName);
                recoveryModelWarningLogged = true;
            }

            // 3. Kiểm tra đã có bản Full nào chưa
            if (lastFullBackupTime <= 0) {
                log.warn(
                        "CSDL [{}] chưa từng có bản Full Backup nào! Tiến hành tạo bản Full ban đầu ngay tại khởi động...",
                        dbName);
                BackupResult r = thucHienBackup(BackupType.FULL);
                if (r.success()) {
                    log.info("Tạo bản Full ban đầu thành công: {}", r.fileName());
                } else {
                    log.error("Tạo bản Full ban đầu thất bại: {}", r.errorMessage());
                }
            }
        } catch (Throwable t) {
            log.error("Lỗi khi kiểm tra khởi tạo CSDL: {}", t.getMessage(), t);
        }
    }

    /**
     * Kiểm tra xem CSDL có đủ điều kiện sao lưu Log hay không.
     */
    public boolean kiemTraDieuKienBackupLog() {
        String dbName = DatabaseConfig.layTenDatabase();
        try (Connection con = DbUtil.getRawConnection()) {
            String rm = layRecoveryModel(con, dbName);
            if (!"FULL".equalsIgnoreCase(rm) && !"BULK_LOGGED".equalsIgnoreCase(rm)) {
                if (!recoveryModelWarningLogged) {
                    log.warn(
                            "Bỏ qua sao lưu Log: CSDL [{}] đang ở Recovery Model '{}'. Cần: ALTER DATABASE [{}] SET RECOVERY FULL;",
                            dbName, rm, dbName);
                    recoveryModelWarningLogged = true;
                }
                return false;
            }

            if (lastFullBackupTime <= 0 && layThoiDiemBackupGanNhatTuMsdb(con, dbName, "D") <= 0) {
                log.warn("Bỏ qua sao lưu Log: CSDL [{}] chưa có bản Full Backup nào.", dbName);
                return false;
            }
            return true;
        } catch (Throwable t) {
            log.warn("Không thể kiểm tra điều kiện backup Log: {}", t.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // THỰC THI SAO LƯU PHÂN TÁN (MULTI-CLIENT CHỐNG ĐÈ NHAU)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Thực hiện sao lưu CSDL theo loại (FULL, DIFF, LOG).
     * Áp dụng quy tắc: "Ai backup đầu tiên thì lấy của người đó, các máy sau bỏ qua
     * để tránh đè dữ liệu".
     */
    public BackupResult thucHienBackup(BackupType type) {
        long startTime = System.currentTimeMillis();
        String dbName = DatabaseConfig.layTenDatabase();

        // BƯỚC 1: Kiểm tra nhanh trước xem trên máy chủ đã có máy trạm khác backup mốc
        // này chưa
        try (Connection checkCon = DbUtil.getRawConnection()) {
            ThongTinBackupGanNhat info = layThongTinBackupGanNhatServer(checkCon, dbName, type);
            if (info != null && info.hopLeTrongKhoang(type)) {
                log.info(
                        "[Multi-Client] Đã có máy trạm khác thực hiện backup {} lúc {} (cách đây {} phút) trên server. Bỏ qua để tránh bị đè dữ liệu.",
                        type.getTenHienThi(), info.thoiDiem(), info.phutTruoc());
                capNhatTimestampCucBo(type, info.thoiDiem().getTime());
                return BackupResult.thanhCong(type, info.tenTep(), info.duongDan(), info.dungLuong(), 0);
            }
        } catch (Throwable t) {
            log.warn("Không thể kiểm tra trạng thái sao lưu phân tán trước khóa: {}", t.getMessage());
        }

        // BƯỚC 2: Quản lý khóa nội bộ JVM trên máy hiện tại
        boolean localLockAcquired = false;
        try {
            if (type == BackupType.LOG) {
                localLockAcquired = backupLock.tryLock(200, TimeUnit.MILLISECONDS);
                if (!localLockAcquired) {
                    log.warn("Bỏ qua mốc sao lưu Log [{}]: Máy cục bộ đang bận thực hiện Full/Diff.", dbName);
                    return BackupResult.thatBai(type, null, null, 0, "Bỏ qua do khóa cục bộ bận");
                }
            } else {
                localLockAcquired = backupLock.tryLock(10, TimeUnit.MINUTES);
                if (!localLockAcquired) {
                    String err = "Timeout 10 phút khi chờ khóa cục bộ sao lưu " + type.getTenHienThi();
                    log.error(err);
                    return BackupResult.thatBai(type, null, null, System.currentTimeMillis() - startTime, err);
                }
            }

            // BƯỚC 3: Quản lý khóa phân tán trên SQL Server (chống xung đột giữa các máy
            // trạm TNV, KTV, CCH)
            try (Connection distCon = DbUtil.getRawConnection()) {
                int timeoutMs = (type == BackupType.LOG) ? 200 : (10 * 60 * 1000); // Log: 200ms; Full/Diff: 10 phút
                boolean serverLockAcquired = xinKhoaServer(distCon, timeoutMs);
                if (!serverLockAcquired) {
                    if (type == BackupType.LOG) {
                        log.warn(
                                "[Multi-Client] Bỏ qua mốc sao lưu Log: Máy trạm khác đang thực hiện sao lưu trên server.");
                        return BackupResult.thatBai(type, null, null, 0,
                                "Bỏ qua do server đang bận sao lưu từ máy khác");
                    } else {
                        String err = "Timeout khi chờ khóa phân tán trên server để sao lưu " + type.getTenHienThi();
                        log.error(err);
                        return BackupResult.thatBai(type, null, null, System.currentTimeMillis() - startTime, err);
                    }
                }

                try {
                    // BƯỚC 4: Double-checked locking trên Server
                    // Sau khi nhận khóa server, kiểm tra lại xem máy vừa giữ khóa trước đó có vừa
                    // hoàn tất backup mốc này không
                    ThongTinBackupGanNhat info = layThongTinBackupGanNhatServer(distCon, dbName, type);
                    if (info != null && info.hopLeTrongKhoang(type)) {
                        log.info(
                                "[Multi-Client] Máy trạm đầu tiên đã hoàn thành backup {} lúc {} (cách đây {} phút). Lấy kết quả từ máy đó, bỏ qua để tránh ghi đè.",
                                type.getTenHienThi(), info.thoiDiem(), info.phutTruoc());
                        capNhatTimestampCucBo(type, info.thoiDiem().getTime());
                        return BackupResult.thanhCong(type, info.tenTep(), info.duongDan(), info.dungLuong(), 0);
                    }

                    // BƯỚC 5: Kiểm tra điều kiện Log nếu là Log backup
                    if (type == BackupType.LOG && !kiemTraDieuKienBackupLog()) {
                        return BackupResult.thatBai(type, null, null, System.currentTimeMillis() - startTime,
                                "CSDL chưa đủ điều kiện (cần Recovery Model FULL và đã có bản Full)");
                    }

                    // BƯỚC 6: Máy này là máy ĐẦU TIÊN -> Thực thi câu lệnh BACKUP thật trên SQL
                    // Server
                    return thucThiSqlBackup(type, dbName);

                } finally {
                    nhanKhoaServer(distCon);
                }
            }

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            String err = "Bị ngắt luồng trong khi chờ sao lưu " + type.getTenHienThi();
            log.error(err);
            return BackupResult.thatBai(type, null, null, System.currentTimeMillis() - startTime, err);
        } catch (Throwable t) {
            log.error("Lỗi sao lưu phân tán: {}", t.getMessage(), t);
            return BackupResult.thatBai(type, null, null, System.currentTimeMillis() - startTime, t.getMessage());
        } finally {
            if (localLockAcquired) {
                backupLock.unlock();
            }
        }
    }

    /**
     * Thực thi câu lệnh BACKUP DATABASE / BACKUP LOG trên SQL Server.
     */
    private BackupResult thucThiSqlBackup(BackupType type, String dbName) {
        long start = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmm");
        String timestamp = sdf.format(new Date());

        String prefix = switch (type) {
            case FULL -> "DB_FULL_";
            case DIFF -> "DB_DIFF_";
            case LOG -> "DB_LOG_";
        };
        String fileName = prefix + timestamp + "." + type.getDuoiTep();
        String subDir = BackupConfig.getSubdirectory(type);
        String fullPath = subDir + "/" + fileName;

        // Định danh nhân viên và máy trạm khởi tạo backup
        String clientIdentity = layDinhDanhMayTram();
        log.info("Bắt đầu sao lưu {} CSDL [{}] bởi [{}] -> {}", type.getTenHienThi(), dbName, clientIdentity, fullPath);

        try (Connection con = DbUtil.getRawConnection()) {
            // 1. Tạo thư mục con nếu chưa có (gọi thủ tục xp_create_subdir của SQL Server)
            try (PreparedStatement psDir = con.prepareStatement("EXEC master.sys.xp_create_subdir ?")) {
                psDir.setString(1, subDir);
                psDir.execute();
            } catch (Throwable t) {
                log.debug("xp_create_subdir note: {}", t.getMessage());
            }

            // 2. Chuẩn bị câu lệnh SQL Backup kèm mô tả người thực hiện
            String backupName = "CarZone " + type.name() + " by " + clientIdentity;
            String sql = switch (type) {
                case FULL -> "BACKUP DATABASE [" + dbName + "] TO DISK = ? WITH INIT, CHECKSUM, NAME = ?";
                case DIFF -> "BACKUP DATABASE [" + dbName + "] TO DISK = ? WITH DIFFERENTIAL, INIT, CHECKSUM, NAME = ?";
                case LOG -> "BACKUP LOG [" + dbName + "] TO DISK = ? WITH INIT, CHECKSUM, NAME = ?";
            };

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, fullPath);
                ps.setString(2, backupName);
                ps.setQueryTimeout(300); // 5 phút timeout cho sao lưu
                ps.execute();
            }

            long duration = System.currentTimeMillis() - start;

            // 3. Lấy dung lượng file backup từ msdb
            long fileSize = layDungLuongBackupMoiNhat(con, dbName, fullPath);

            // Cập nhật timestamp thành công
            long now = System.currentTimeMillis();
            capNhatTimestampCucBo(type, now);
            if (type == BackupType.LOG) {
                lastLogErrorMessage = null;
            }

            log.info("Sao lưu {} thành công bởi [{}]! File: {} ({}), thời gian: {}ms",
                    type.getTenHienThi(), clientIdentity, fileName, AuditLogger.formatDungLuong(fileSize), duration);

            // Ghi Audit Log với nhãn riêng
            AuditLogger.logBackupOperation(type.name(), dbName, fullPath, fileSize, duration, true, null);

            return BackupResult.thanhCong(type, fileName, fullPath, fileSize, duration);

        } catch (Throwable t) {
            long duration = System.currentTimeMillis() - start;
            String errMsg = t.getMessage();

            // Nếu là Log backup và lỗi lặp lại giống nhau, giới hạn tần suất in stack trace
            if (type == BackupType.LOG) {
                long now = System.currentTimeMillis();
                if (errMsg != null && errMsg.equals(lastLogErrorMessage) && (now - lastLogErrorTime < 3600000)) {
                    log.warn("Sao lưu Log tiếp tục thất bại (lỗi lặp lại): {}", errMsg);
                } else {
                    log.error("Sao lưu Log thất bại: {}", errMsg, t);
                    lastLogErrorMessage = errMsg;
                    lastLogErrorTime = now;
                }
            } else {
                log.error("Sao lưu {} thất bại sau {}ms: {}", type.getTenHienThi(), duration, errMsg, t);
            }

            AuditLogger.logBackupOperation(type.name(), dbName, fullPath, -1, duration, false, errMsg);
            return BackupResult.thatBai(type, fileName, fullPath, duration, errMsg);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // KHÓA PHÂN TÁN (SQL SERVER APPLICATION LOCKS)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Xin khóa độc quyền trên SQL Server qua thủ tục sp_getapplock.
     * 
     * @param con       Connection mở tới SQL Server
     * @param timeoutMs Thời gian chờ khóa (mili-giây)
     * @return true nếu lấy được khóa, false nếu hết hạn chờ hoặc lỗi
     */
    private boolean xinKhoaServer(Connection con, int timeoutMs) {
        String sql = "DECLARE @res INT; "
                + "EXEC @res = sp_getapplock @Resource = ?, @LockMode = 'Exclusive', "
                + "@LockOwner = 'Session', @LockTimeout = ?; "
                + "SELECT @res;";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, APP_LOCK_RESOURCE);
            ps.setInt(2, timeoutMs);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int res = rs.getInt(1);
                    return res >= 0; // 0: thành công ngay, 1: thành công sau khi đợi
                }
            }
        } catch (Throwable t) {
            log.warn("Không thể gọi sp_getapplock trên SQL Server: {}", t.getMessage());
        }
        return false;
    }

    /**
     * Nhả khóa độc quyền trên SQL Server qua thủ tục sp_releaseapplock.
     */
    private void nhanKhoaServer(Connection con) {
        String sql = "EXEC sp_releaseapplock @Resource = ?, @LockOwner = 'Session';";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, APP_LOCK_RESOURCE);
            ps.execute();
        } catch (Throwable ignored) {
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CÁC HÀM TIỆN ÍCH TRUY VẤN METADATA SQL SERVER
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Lấy thông tin bản backup gần nhất của loại tương ứng từ msdb.dbo.backupset
     * trên SQL Server.
     */
    public ThongTinBackupGanNhat layThongTinBackupGanNhatServer(Connection con, String dbName, BackupType type) {
        String typeFilter = switch (type) {
            case FULL -> "bs.type = 'D'";
            case DIFF -> "bs.type IN ('D', 'I')";
            case LOG -> "bs.type IN ('D', 'I', 'L')";
        };

        String sql = "SELECT TOP 1 bs.type, bs.backup_finish_date, "
                + "DATEDIFF(MINUTE, bs.backup_finish_date, GETDATE()) AS phut_truoc, "
                + "bmf.physical_device_name, bs.backup_size "
                + "FROM msdb.dbo.backupset bs "
                + "LEFT JOIN msdb.dbo.backupmediafamily bmf ON bs.media_set_id = bmf.media_set_id "
                + "WHERE bs.database_name = ? AND " + typeFilter + " "
                + "ORDER BY bs.backup_finish_date DESC";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, dbName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String loai = rs.getString("type");
                    Date thoiDiem = rs.getTimestamp("backup_finish_date");
                    long phutTruoc = rs.getLong("phut_truoc");
                    String duongDan = rs.getString("physical_device_name");
                    long dungLuong = rs.getLong("backup_size");
                    String tenTep = null;
                    if (duongDan != null) {
                        int idx = Math.max(duongDan.lastIndexOf('/'), duongDan.lastIndexOf('\\'));
                        tenTep = idx >= 0 ? duongDan.substring(idx + 1) : duongDan;
                    }
                    return new ThongTinBackupGanNhat(loai, thoiDiem, phutTruoc, tenTep, duongDan, dungLuong);
                }
            }
        } catch (Throwable t) {
            log.debug("layThongTinBackupGanNhatServer note: {}", t.getMessage());
        }
        return null;
    }

    private String layRecoveryModel(Connection con, String dbName) {
        try (PreparedStatement ps = con
                .prepareStatement("SELECT recovery_model_desc FROM sys.databases WHERE name = ?")) {
            ps.setString(1, dbName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        } catch (Throwable ignored) {
        }
        return "UNKNOWN";
    }

    private long layThoiDiemBackupGanNhatTuMsdb(Connection con, String dbName, String typeChar) {
        String sql = "SELECT TOP 1 backup_finish_date FROM msdb.dbo.backupset "
                + "WHERE database_name = ? AND type = ? ORDER BY backup_finish_date DESC";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, dbName);
            ps.setString(2, typeChar);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Date d = rs.getTimestamp(1);
                    return d != null ? d.getTime() : 0;
                }
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    private long layDungLuongBackupMoiNhat(Connection con, String dbName, String physicalPath) {
        String sql = "SELECT TOP 1 backup_size FROM msdb.dbo.backupset bs "
                + "JOIN msdb.dbo.backupmediafamily bmf ON bs.media_set_id = bmf.media_set_id "
                + "WHERE bs.database_name = ? AND bmf.physical_device_name = ? "
                + "ORDER BY bs.backup_finish_date DESC";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, dbName);
            ps.setString(2, physicalPath);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (Throwable ignored) {
        }
        return -1;
    }

    /**
     * Kiểm tra xem có bản sao lưu nào vừa thành công trong vòng X phút gần đây
     * không.
     * TRUY VẤN TRỰC TIẾP TRÊN MÁY CHỦ CSDL để đồng bộ trạng thái giữa tất cả các
     * máy trạm:
     * Nếu tiếp nhận viên, kỹ thuật viên hoặc chủ cửa hàng đã có máy thoát trước và
     * backup thành công,
     * các máy thoát sau sẽ nhận biết ngay và thoát tức thì, không backup lặp lại.
     */
    public boolean vuaCoBackupGanDay() {
        int thresholdMin = BackupConfig.getRecentThresholdMinutes();
        String dbName = DatabaseConfig.layTenDatabase();

        // 1. Kiểm tra trực tiếp trên SQL Server để đồng bộ giữa mọi máy trạm
        try (Connection con = DbUtil.getRawConnection()) {
            String sql = "SELECT TOP 1 DATEDIFF(SECOND, backup_finish_date, GETDATE()) AS diff_sec, backup_finish_date "
                    + "FROM msdb.dbo.backupset "
                    + "WHERE database_name = ? AND type IN ('D', 'I', 'L') "
                    + "ORDER BY backup_finish_date DESC";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, dbName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        long diffSec = rs.getLong("diff_sec");
                        if (diffSec >= 0 && diffSec < (thresholdMin * 60L)) {
                            Date finishDate = rs.getTimestamp("backup_finish_date");
                            log.info(
                                    "[Multi-Client] Trên SQL Server đã có bản sao lưu lúc {} (cách đây {} giây bởi máy trạm khác hoặc tiến trình trước). Bỏ qua sao lưu khi thoát.",
                                    finishDate, diffSec);
                            return true;
                        }
                    }
                }
            }
        } catch (Throwable t) {
            log.warn("Không thể truy vấn lịch sử backup từ SQL Server: {} – chuyển sang kiểm tra bộ nhớ cục bộ.",
                    t.getMessage());
        }

        // 2. Fallback kiểm tra bộ nhớ RAM cục bộ
        long thresholdMs = thresholdMin * 60 * 1000L;
        long now = System.currentTimeMillis();
        boolean fullRecent = (now - lastFullBackupTime) < thresholdMs;
        boolean diffRecent = (now - lastDiffBackupTime) < thresholdMs;
        boolean logRecent = (now - lastLogBackupTime) < thresholdMs;

        return fullRecent || diffRecent || logRecent;
    }

    private void capNhatTimestampCucBo(BackupType type, long timestamp) {
        switch (type) {
            case FULL -> lastFullBackupTime = timestamp;
            case DIFF -> lastDiffBackupTime = timestamp;
            case LOG -> lastLogBackupTime = timestamp;
        }
    }

    private String layDinhDanhMayTram() {
        String host = "workstation";
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (Throwable ignored) {
        }

        String user = "SYSTEM";
        try {
            if (UserSession.getInstance().daDangNhap()) {
                user = UserSession.getInstance().getMaNV();
            }
        } catch (Throwable ignored) {
        }

        return user + "@" + host;
    }

    public long getLastFullBackupTime() {
        return lastFullBackupTime;
    }

    public long getLastDiffBackupTime() {
        return lastDiffBackupTime;
    }

    public long getLastLogBackupTime() {
        return lastLogBackupTime;
    }

    public ReentrantLock getBackupLock() {
        return backupLock;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TÁC VỤ SAO LƯU & PHỤC HỒI THỦ CÔNG DÀNH CHO CHỦ CỬA HÀNG
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Lấy danh sách toàn bộ các bản sao lưu đã thực hiện từ msdb.dbo.backupset.
     */
    public java.util.List<BanSaoLuuItem> layDanhSachBanSaoLuu() {
        java.util.List<BanSaoLuuItem> list = new java.util.ArrayList<>();
        String dbName = DatabaseConfig.layTenDatabase();
        String sql = "SELECT bs.backup_set_id, bs.database_name, bs.type, bs.backup_finish_date, "
                + "bs.backup_size, bmf.physical_device_name, bs.name AS backup_name "
                + "FROM msdb.dbo.backupset bs "
                + "LEFT JOIN msdb.dbo.backupmediafamily bmf ON bs.media_set_id = bmf.media_set_id "
                + "WHERE bs.database_name = ? AND bs.type IN ('D', 'I', 'L') "
                + "ORDER BY bs.backup_finish_date DESC";

        try (Connection con = DbUtil.getRawConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, dbName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("backup_set_id");
                    String db = rs.getString("database_name");
                    String tStr = rs.getString("type");
                    BackupType type = switch (tStr == null ? "" : tStr) {
                        case "D" -> BackupType.FULL;
                        case "I" -> BackupType.DIFF;
                        case "L" -> BackupType.LOG;
                        default -> null;
                    };
                    Date date = rs.getTimestamp("backup_finish_date");
                    long size = rs.getLong("backup_size");
                    String path = rs.getString("physical_device_name");
                    String name = rs.getString("backup_name");
                    String fileName = path;
                    if (path != null) {
                        int idx = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
                        if (idx >= 0)
                            fileName = path.substring(idx + 1);
                    }
                    list.add(new BanSaoLuuItem(id, db, type, date, size, path, fileName, name));
                }
            }
        } catch (Throwable t) {
            log.error("Lỗi truy vấn danh sách bản sao lưu: {}", t.getMessage(), t);
        }
        return list;
    }

    /**
     * Thực hiện sao lưu thủ công theo yêu cầu của Chủ cửa hàng.
     */
    public BackupResult thucHienSaoLuuThuCong(BackupType type, String ghiChu) {
        if (!UserSession.getInstance().coVaiTro(enums.VaiTro.CHU_CUA_HANG)) {
            return BackupResult.thatBai(type, null, null, 0,
                    "Chỉ Chủ cửa hàng mới có quyền thực hiện sao lưu thủ công!");
        }

        long startTime = System.currentTimeMillis();
        String dbName = DatabaseConfig.layTenDatabase();

        boolean localLockAcquired = false;
        try {
            localLockAcquired = backupLock.tryLock(10, TimeUnit.MINUTES);
            if (!localLockAcquired) {
                return BackupResult.thatBai(type, null, null, System.currentTimeMillis() - startTime,
                        "Hệ thống đang bận thực hiện sao lưu khác. Vui lòng thử lại sau.");
            }

            try (Connection distCon = DbUtil.getRawConnection()) {
                boolean serverLockAcquired = xinKhoaServer(distCon, 30_000);
                if (!serverLockAcquired) {
                    return BackupResult.thatBai(type, null, null, System.currentTimeMillis() - startTime,
                            "Máy chủ đang bận thực hiện sao lưu khác. Vui lòng thử lại sau.");
                }

                try {
                    if (type == BackupType.LOG && !kiemTraDieuKienBackupLog()) {
                        return BackupResult.thatBai(type, null, null, System.currentTimeMillis() - startTime,
                                "CSDL chưa đủ điều kiện (cần Recovery Model FULL và đã có bản Full)");
                    }

                    return thucThiSqlBackupThuCong(type, dbName, ghiChu);
                } finally {
                    nhanKhoaServer(distCon);
                }
            }
        } catch (Throwable t) {
            log.error("Lỗi sao lưu thủ công: {}", t.getMessage(), t);
            return BackupResult.thatBai(type, null, null, System.currentTimeMillis() - startTime, t.getMessage());
        } finally {
            if (localLockAcquired) {
                backupLock.unlock();
            }
        }
    }

    private BackupResult thucThiSqlBackupThuCong(BackupType type, String dbName, String ghiChu) {
        long start = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmm");
        String timestamp = sdf.format(new Date());

        String prefix = switch (type) {
            case FULL -> "DB_MANUAL_FULL_";
            case DIFF -> "DB_MANUAL_DIFF_";
            case LOG -> "DB_MANUAL_LOG_";
        };
        String fileName = prefix + timestamp + "." + type.getDuoiTep();
        String subDir = BackupConfig.getSubdirectory(type);
        String fullPath = subDir + "/" + fileName;

        String clientIdentity = layDinhDanhMayTram();
        String backupName = "CarZone MANUAL " + type.name() + " by " + clientIdentity
                + (ghiChu != null && !ghiChu.isBlank() ? " (" + ghiChu.trim() + ")" : "");

        try (Connection con = DbUtil.getRawConnection()) {
            try (PreparedStatement psDir = con.prepareStatement("EXEC master.sys.xp_create_subdir ?")) {
                psDir.setString(1, subDir);
                psDir.execute();
            } catch (Throwable ignored) {
            }

            String sql = switch (type) {
                case FULL -> "BACKUP DATABASE [" + dbName + "] TO DISK = ? WITH INIT, CHECKSUM, NAME = ?";
                case DIFF -> "BACKUP DATABASE [" + dbName + "] TO DISK = ? WITH DIFFERENTIAL, INIT, CHECKSUM, NAME = ?";
                case LOG -> "BACKUP LOG [" + dbName + "] TO DISK = ? WITH INIT, CHECKSUM, NAME = ?";
            };

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, fullPath);
                ps.setString(2, backupName);
                ps.setQueryTimeout(300);
                ps.execute();
            }

            long duration = System.currentTimeMillis() - start;
            long fileSize = layDungLuongBackupMoiNhat(con, dbName, fullPath);

            capNhatTimestampCucBo(type, System.currentTimeMillis());
            log.info("Sao lưu thủ công {} thành công! File: {} ({}), thời gian: {}ms",
                    type.getTenHienThi(), fileName, AuditLogger.formatDungLuong(fileSize), duration);

            AuditLogger.logBackupOperation("MANUAL_" + type.name(), dbName, fullPath, fileSize, duration, true, null);
            return BackupResult.thanhCong(type, fileName, fullPath, fileSize, duration);

        } catch (Throwable t) {
            long duration = System.currentTimeMillis() - start;
            log.error("Sao lưu thủ công {} thất bại: {}", type.getTenHienThi(), t.getMessage(), t);
            AuditLogger.logBackupOperation("MANUAL_" + type.name(), dbName, fullPath, -1, duration, false,
                    t.getMessage());
            return BackupResult.thatBai(type, fileName, fullPath, duration, t.getMessage());
        }
    }

    public record RestoreStep(
            String loai,          // "FULL", "DIFF", "LOG"
            String duongDan,      // Đường dẫn file sao lưu
            boolean recovery,     // true: WITH RECOVERY, false: WITH NORECOVERY
            String moTa           // Diễn giải từng bước
    ) {}

    /**
     * Tự động xác định chuỗi phục hồi hợp lệ theo chuẩn Microsoft SQL Server:
     * - FULL: 1 bước (RESTORE DATABASE WITH REPLACE, RECOVERY)
     * - DIFF: 2 bước (RESTORE FULL gốc WITH REPLACE, NORECOVERY -> RESTORE DIFF WITH RECOVERY [KHÔNG có REPLACE])
     * - LOG: Chuỗi liên hoàn từ bản FULL gốc (RESTORE FULL gốc WITH REPLACE, NORECOVERY -> các bản LOG liên tiếp WITH NORECOVERY -> bản LOG đích WITH RECOVERY)
     */
    public List<RestoreStep> xacDinhChuoiPhucHoi(Connection masterCon, String dbName, String targetPath) throws SQLException {
        List<RestoreStep> steps = new ArrayList<>();
        if (targetPath == null || targetPath.isBlank()) {
            return steps;
        }

        String fileName = targetPath;
        int idx = Math.max(targetPath.lastIndexOf('/'), targetPath.lastIndexOf('\\'));
        if (idx >= 0) {
            fileName = targetPath.substring(idx + 1);
        }

        // 1. Tra cứu thông tin của file chỉ định trong msdb.dbo.backupset
        String sqlFindTarget = "SELECT TOP 1 bs.backup_set_id, bs.type, bs.backup_finish_date, bmf.physical_device_name "
                + "FROM msdb.dbo.backupset bs "
                + "JOIN msdb.dbo.backupmediafamily bmf ON bs.media_set_id = bmf.media_set_id "
                + "WHERE (bs.database_name = ? OR ? = '') "
                + "  AND (bmf.physical_device_name = ? OR bmf.physical_device_name LIKE ? OR bmf.physical_device_name LIKE ?) "
                + "ORDER BY bs.backup_set_id DESC";

        String targetType = null;
        long targetSetId = -1;
        String canonicalTargetPath = targetPath;

        try (PreparedStatement ps = masterCon.prepareStatement(sqlFindTarget)) {
            ps.setString(1, dbName);
            ps.setString(2, dbName);
            ps.setString(3, targetPath);
            ps.setString(4, "%/" + fileName);
            ps.setString(5, "%\\" + fileName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    targetSetId = rs.getLong("backup_set_id");
                    targetType = rs.getString("type");
                    canonicalTargetPath = rs.getString("physical_device_name");
                }
            }
        }

        // Tìm kiếm dự phòng theo tên tệp nếu đường dẫn tuyệt đối bị lệch giữa host và container
        if (targetType == null) {
            String sqlFallback = "SELECT TOP 1 bs.backup_set_id, bs.type, bs.backup_finish_date, bmf.physical_device_name "
                    + "FROM msdb.dbo.backupset bs "
                    + "JOIN msdb.dbo.backupmediafamily bmf ON bs.media_set_id = bmf.media_set_id "
                    + "WHERE bmf.physical_device_name LIKE ? "
                    + "ORDER BY bs.backup_set_id DESC";
            try (PreparedStatement ps = masterCon.prepareStatement(sqlFallback)) {
                ps.setString(1, "%" + fileName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        targetSetId = rs.getLong("backup_set_id");
                        targetType = rs.getString("type");
                        canonicalTargetPath = rs.getString("physical_device_name");
                    }
                }
            }
        }

        // Trường hợp không tìm thấy trong msdb (file copy ngoài vào):
        if (targetType == null) {
            if (fileName.toLowerCase().endsWith(".trn") || fileName.toLowerCase().endsWith(".log")) {
                throw new SQLException("Không tìm thấy thông tin của tệp Transaction Log [" + fileName + "] trong lịch sử msdb để dựng chuỗi phục hồi. "
                        + "Vui lòng chọn bản sao lưu trực tiếp từ bảng lịch sử trên giao diện.");
            } else {
                steps.add(new RestoreStep("FULL", targetPath, true, "Phục hồi cơ sở dữ liệu (.bak)"));
                return steps;
            }
        }

        // Trường hợp A: Bản FULL ('D')
        if ("D".equalsIgnoreCase(targetType)) {
            steps.add(new RestoreStep("FULL", canonicalTargetPath, true, "Phục hồi toàn phần bản Full Backup"));
            return steps;
        }

        // Trường hợp B: Bản DIFF ('I')
        if ("I".equalsIgnoreCase(targetType)) {
            // Cần bản FULL gần nhất trước mốc DIFF này (dựa vào backup_set_id)
            String sqlBaseFull = "SELECT TOP 1 bmf.physical_device_name "
                    + "FROM msdb.dbo.backupset bs "
                    + "JOIN msdb.dbo.backupmediafamily bmf ON bs.media_set_id = bmf.media_set_id "
                    + "WHERE (bs.database_name = ? OR ? = '') AND bs.type = 'D' AND bs.backup_set_id < ? "
                    + "ORDER BY bs.backup_set_id DESC";

            String baseFullPath = null;
            try (PreparedStatement ps = masterCon.prepareStatement(sqlBaseFull)) {
                ps.setString(1, dbName);
                ps.setString(2, dbName);
                ps.setLong(3, targetSetId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        baseFullPath = rs.getString("physical_device_name");
                    }
                }
            }

            if (baseFullPath == null) {
                throw new SQLException("Không tìm thấy bản sao lưu FULL gốc trong msdb để làm mốc phục hồi vi sai (Differential).");
            }

            steps.add(new RestoreStep("FULL", baseFullPath, false, "Phục hồi bản FULL gốc (chế độ chờ NORECOVERY)"));
            steps.add(new RestoreStep("DIFF", canonicalTargetPath, true, "Phục hồi bản vi sai DIFF (hoàn tất RECOVERY)"));
            return steps;
        }

        // Trường hợp C: Bản LOG ('L')
        if ("L".equalsIgnoreCase(targetType)) {
            // 1. Tìm bản FULL gần nhất trước mốc LOG này (dựa vào backup_set_id)
            String sqlBaseFull = "SELECT TOP 1 bs.backup_set_id, bs.backup_finish_date, bmf.physical_device_name "
                    + "FROM msdb.dbo.backupset bs "
                    + "JOIN msdb.dbo.backupmediafamily bmf ON bs.media_set_id = bmf.media_set_id "
                    + "WHERE (bs.database_name = ? OR ? = '') AND bs.type = 'D' AND bs.backup_set_id < ? "
                    + "ORDER BY bs.backup_set_id DESC";

            long baseFullSetId = -1;
            String baseFullPath = null;
            try (PreparedStatement ps = masterCon.prepareStatement(sqlBaseFull)) {
                ps.setString(1, dbName);
                ps.setString(2, dbName);
                ps.setLong(3, targetSetId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        baseFullSetId = rs.getLong("backup_set_id");
                        baseFullPath = rs.getString("physical_device_name");
                    }
                }
            }

            if (baseFullPath == null) {
                throw new SQLException("Không tìm thấy bản sao lưu FULL gốc trong msdb để làm mốc phục hồi chuỗi nhật ký (Transaction Log).");
            }

            // Bước 1: Phục hồi FULL gốc ở trạng thái NORECOVERY
            steps.add(new RestoreStep("FULL", baseFullPath, false, "Phục hồi bản FULL gốc (chế độ chờ NORECOVERY)"));

            // 2. Lấy toàn bộ các bản LOG từ sau baseFullSetId đến targetSetId theo đúng thứ tự backup_set_id ASC
            String sqlLogs = "SELECT bs.backup_set_id, bmf.physical_device_name "
                    + "FROM msdb.dbo.backupset bs "
                    + "JOIN msdb.dbo.backupmediafamily bmf ON bs.media_set_id = bmf.media_set_id "
                    + "WHERE (bs.database_name = ? OR ? = '') AND bs.type = 'L' "
                    + "  AND bs.backup_set_id > ? AND bs.backup_set_id <= ? "
                    + "ORDER BY bs.backup_set_id ASC";

            List<String> logPaths = new ArrayList<>();
            try (PreparedStatement ps = masterCon.prepareStatement(sqlLogs)) {
                ps.setString(1, dbName);
                ps.setString(2, dbName);
                ps.setLong(3, baseFullSetId);
                ps.setLong(4, targetSetId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String p = rs.getString("physical_device_name");
                        if (p != null && !p.isBlank() && !logPaths.contains(p)) {
                            logPaths.add(p);
                        }
                    }
                }
            }

            if (!logPaths.contains(canonicalTargetPath)) {
                logPaths.add(canonicalTargetPath);
            }

            for (int i = 0; i < logPaths.size(); i++) {
                boolean isFinal = (i == logPaths.size() - 1);
                String path = logPaths.get(i);
                steps.add(new RestoreStep("LOG", path, isFinal,
                        "Phục hồi chuỗi nhật ký LOG [" + (i + 1) + "/" + logPaths.size() + "] "
                                + (isFinal ? "(hoàn tất RECOVERY)" : "(chế độ chờ NORECOVERY)")));
            }

            return steps;
        }

        // Mặc định
        steps.add(new RestoreStep("FULL", canonicalTargetPath, true, "Phục hồi CSDL"));
        return steps;
    }

    /**
     * Thực hiện phục hồi cơ sở dữ liệu từ tệp sao lưu chỉ định.
     * Tự động điều phối chuỗi phục hồi: FULL gốc -> DIFF trung gian -> các LOG tuần tự.
     */
    public RestoreResult thucHienPhucHoi(String duongDanTepBak) {
        if (!UserSession.getInstance().coVaiTro(enums.VaiTro.CHU_CUA_HANG)) {
            return RestoreResult.thatBai(duongDanTepBak, 0, "Chỉ Chủ cửa hàng mới có quyền phục hồi cơ sở dữ liệu!");
        }
        if (duongDanTepBak == null || duongDanTepBak.isBlank()) {
            return RestoreResult.thatBai(null, 0, "Đường dẫn tệp sao lưu không được để trống!");
        }

        long start = System.currentTimeMillis();
        String dbName = DatabaseConfig.layTenDatabase();
        log.warn("CHỦ CỬA HÀNG YÊU CẦU PHỤC HỒI CSDL [{}] TỪ TỆP: {}", dbName, duongDanTepBak);

        boolean localLockAcquired = false;
        try {
            localLockAcquired = backupLock.tryLock(15, TimeUnit.MINUTES);
            if (!localLockAcquired) {
                return RestoreResult.thatBai(duongDanTepBak, 0, "Hệ thống đang bận thực hiện tác vụ khác.");
            }

            // Dùng kết nối master để thực hiện restore
            try (Connection masterCon = DbUtil.getMasterConnection()) {
                // 1. Phân tích chuỗi phục hồi tự động
                List<RestoreStep> steps = xacDinhChuoiPhucHoi(masterCon, dbName, duongDanTepBak);
                if (steps.isEmpty()) {
                    return RestoreResult.thatBai(duongDanTepBak, 0, "Không xác định được các bước phục hồi phù hợp.");
                }

                // 2. Chuyển database sang SINGLE_USER WITH ROLLBACK IMMEDIATE để ngắt toàn bộ phiên kết nối khác
                log.info("Đang chuyển CSDL [{}] sang SINGLE_USER để ngắt kết nối...", dbName);
                try (Statement st = masterCon.createStatement()) {
                    st.execute("ALTER DATABASE [" + dbName + "] SET SINGLE_USER WITH ROLLBACK IMMEDIATE");
                }

                // 3. Thực thi tuần tự từng bước trong chuỗi phục hồi
                for (int i = 0; i < steps.size(); i++) {
                    RestoreStep step = steps.get(i);
                    String sql;
                    if ("FULL".equalsIgnoreCase(step.loai())) {
                        sql = "RESTORE DATABASE [" + dbName + "] FROM DISK = ? WITH REPLACE, "
                                + (step.recovery() ? "RECOVERY" : "NORECOVERY");
                    } else if ("DIFF".equalsIgnoreCase(step.loai())) {
                        sql = "RESTORE DATABASE [" + dbName + "] FROM DISK = ? WITH "
                                + (step.recovery() ? "RECOVERY" : "NORECOVERY");
                    } else { // LOG
                        sql = "RESTORE LOG [" + dbName + "] FROM DISK = ? WITH "
                                + (step.recovery() ? "RECOVERY" : "NORECOVERY");
                    }

                    log.info("Thực thi bước [{}/{}]: {} (File: {})", i + 1, steps.size(), step.moTa(), step.duongDan());
                    try (PreparedStatement ps = masterCon.prepareStatement(sql)) {
                        ps.setString(1, step.duongDan());
                        ps.setQueryTimeout(600); // 10 phút timeout
                        ps.execute();
                    }
                }

                // 4. Chuyển database về MULTI_USER
                log.info("Phục hồi thành công toàn bộ chuỗi! Chuyển CSDL [{}] về MULTI_USER...", dbName);
                try (Statement st = masterCon.createStatement()) {
                    st.execute("ALTER DATABASE [" + dbName + "] SET MULTI_USER");
                }

                long duration = System.currentTimeMillis() - start;
                log.info("Phục hồi CSDL [{}] hoàn tất thành công trong {}ms với {} bước!", dbName, duration, steps.size());
                AuditLogger.logBackupOperation("RESTORE", dbName, duongDanTepBak, 0, duration, true, null);

                return RestoreResult.thanhCong(duongDanTepBak, duration);

            } catch (Throwable t) {
                // Đảm bảo cố gắng đưa DB về MULTI_USER nếu có lỗi
                try (Connection rescueCon = DbUtil.getMasterConnection();
                     Statement st = rescueCon.createStatement()) {
                    st.execute("ALTER DATABASE [" + dbName + "] SET MULTI_USER");
                } catch (Throwable ignored) {}

                long duration = System.currentTimeMillis() - start;
                String err = "Lỗi khi phục hồi CSDL: " + t.getMessage();
                log.error(err, t);
                AuditLogger.logBackupOperation("RESTORE", dbName, duongDanTepBak, -1, duration, false, t.getMessage());
                return RestoreResult.thatBai(duongDanTepBak, duration, err);
            }

        } catch (Throwable t) {
            long duration = System.currentTimeMillis() - start;
            return RestoreResult.thatBai(duongDanTepBak, duration, t.getMessage());
        } finally {
            if (localLockAcquired) {
                backupLock.unlock();
            }
        }
    }
}
