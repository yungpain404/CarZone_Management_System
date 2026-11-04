package audit;

import enums.LoaiHanhViBatThuong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import session.UserSession;

import java.time.LocalTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Bộ giám sát và phát hiện hành vi phi lý thời gian thực (Behavioral Anomaly Engine).
 * Chặn ngang câu lệnh SQL hoặc hành động UI trước khi thực thi nếu có dấu hiệu bất thường.
 */
public final class PhatHienHanhViPhiLy {

    private static final Logger log = LoggerFactory.getLogger(PhatHienHanhViPhiLy.class);

    /** Các bảng dữ liệu cốt lõi của hệ thống cần giám sát nghiêm ngặt */
    private static final Set<String> BANG_COT_LOI = Set.of(
            "NHANVIEN", "TAIKHOAN", "LINHKIEN", "DICHVU", "HOADON",
            "XE", "KHACHHANG", "PHIEUTIEPNHANSUACHUA", "CHITIETHOADON", "CHITIETLINHKIEN"
    );

    /** Cửa sổ trượt lưu vết thời gian các thao tác xóa gần đây để phát hiện Mass Delete */
    private static final ConcurrentLinkedDeque<DeleteEvent> LICHSU_XOA = new ConcurrentLinkedDeque<>();

    /** Danh sách ngoại lệ tạm thời sau khi xác thực mã PIN thành công (Suppression Window) */
    private static final Map<String, Long> WHITELIST_TAM_THOI = new ConcurrentHashMap<>();

    private record DeleteEvent(String tenBang, long timestamp) {}

    private PhatHienHanhViPhiLy() {}

    /**
     * Đánh chặn và kiểm tra câu lệnh SQL trước khi thực thi tại StatementProxy.
     * Nếu phát hiện bất thường, tạm dừng tiến trình và yêu cầu nhập mã PIN bảo mật cấp 2.
     *
     * @param sql    Câu lệnh SQL chuẩn bị thực thi
     * @param params Danh sách tham số PreparedStatement
     * @throws SecurityException nếu người dùng từ chối hoặc nhập sai mã PIN quá 3 lần
     */
    public static void kiemTraTruocKhiThucThi(String sql, Map<Integer, Object> params) {
        if (sql == null || sql.isBlank()) return;

        String cleanSql = sql.stripLeading().toUpperCase();
        String action = AuditLogger.xacDinhHanhDong(sql);
        String tableName = AuditLogger.trichXuatTenBang(sql).toUpperCase();

        // 1. Kiểm tra quyền miễn trừ tạm thời (Suppression Window)
        if (laNgoaiLeTamThoi(tableName)) {
            return;
        }

        ThongTinViPham viPham = phatHienViPham(cleanSql, action, tableName, params);
        if (viPham != null) {
            log.warn("PHÁT HIỆN HÀNH VI PHI LÝ: [{}] trên bảng [{}] - Chi tiết: {}",
                    viPham.loai().name(), viPham.tenBang(), viPham.chiTiet());

            // Tạm dừng tiến trình và kích hoạt Popup xác thực mã PIN trên EDT
            boolean hopLe = PinVerificationManager.yeuCauXacThuc(viPham);
            if (!hopLe) {
                throw new SecurityException("TIẾN TRÌNH BỊ CHẶN BỞI HỆ THỐNG AN NINH: " + viPham.chiTiet());
            }

            // Người dùng nhập đúng PIN -> thêm vào suppression window 60s
            choPhepTamThoi(tableName);
        }
    }

    /**
     * Phân tích và nhận diện các mẫu hành vi phi lý.
     */
    public static ThongTinViPham phatHienViPham(String cleanSql, String action, String tableName, Map<Integer, Object> params) {
        String tblUpper = tableName != null ? tableName.toUpperCase() : "";
        String actUpper = action != null ? action.toUpperCase() : "";
        String sqlUpper = cleanSql != null ? cleanSql.toUpperCase() : "";

        String nguoiDung = UserSession.getInstance().daDangNhap()
                ? UserSession.getInstance().getMaNV() : "SYSTEM";

        // ─────────────────────────────────────────────────────────────────────
        // 1. BẤT THƯỜNG VỀ CẤU TRÚC HOẶC PHẠM VI DỮ LIỆU
        // ─────────────────────────────────────────────────────────────────────

        // 1.1 Cố gắng phá hủy cấu trúc bảng (DDL / TRUNCATE / DROP)
        if (sqlUpper.startsWith("DROP TABLE") || sqlUpper.startsWith("TRUNCATE TABLE")
                || sqlUpper.contains("DROP COLUMN") || sqlUpper.contains("ALTER TABLE") && sqlUpper.contains("DROP")) {
            return new ThongTinViPham(
                    LoaiHanhViBatThuong.MASS_UPDATE_OR_DDL,
                    "Cố gắng xóa cấu trúc bảng hoặc toàn bộ dữ liệu (DDL: " + rutGon(cleanSql) + ")",
                    tblUpper, cleanSql, nguoiDung
            );
        }

        // 1.2 Can thiệp vào khu vực quản trị an ninh hoặc cấu hình CSDL
        if (sqlUpper.contains("SP_CONFIGURE") || sqlUpper.contains("SYS.DATABASES") && sqlUpper.contains("DROP")
                || sqlUpper.contains("ALTER LOGIN") || sqlUpper.contains("ALTER SERVER ROLE")) {
            return new ThongTinViPham(
                    LoaiHanhViBatThuong.ACCESS_HIGH_SENSITIVITY,
                    "Cố gắng truy xuất / can thiệp trực tiếp vào tài nguyên quản trị SQL Server nhạy cảm",
                    "SYSTEM", cleanSql, nguoiDung
            );
        }

        // 1.3 Xóa dữ liệu có liên kết khóa ngoại sụp đổ (Cascading Danger) - VD: Xóa Chủ cửa hàng CCH
        if ("DELETE".equals(actUpper) && ("NHANVIEN".equals(tblUpper) || "TAIKHOAN".equals(tblUpper))) {
            boolean chuaCCH = sqlUpper.contains("CCH") || sqlUpper.contains("CHU_CUA_HANG");
            if (!chuaCCH && params != null) {
                for (Object val : params.values()) {
                    if (val != null && val.toString().toUpperCase().contains("CCH")) {
                        chuaCCH = true;
                        break;
                    }
                }
            }
            if (chuaCCH) {
                return new ThongTinViPham(
                        LoaiHanhViBatThuong.CASCADING_DANGER_DELETE,
                        "Cố gắng xóa tài khoản hoặc hồ sơ Chủ cửa hàng (CCH), có nguy cơ làm tê liệt toàn bộ hệ thống!",
                        tblUpper, cleanSql, nguoiDung
                );
            }
        }

        // 1.4 Sửa đổi hàng loạt (Mass Update): Vô hiệu hóa tài khoản hàng loạt hoặc đổi vai trò toàn hệ thống
        if ("UPDATE".equals(actUpper) && "TAIKHOAN".equals(tblUpper)) {
            if (sqlUpper.contains("TRANGTHAIHOATDONG") && !sqlUpper.contains("WHERE")) {
                return new ThongTinViPham(
                        LoaiHanhViBatThuong.MASS_UPDATE_OR_DDL,
                        "Cố gắng cập nhật trạng thái hoạt động toàn bộ danh sách tài khoản cùng lúc (không có điều kiện WHERE)",
                        tblUpper, cleanSql, nguoiDung
                );
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // 2. BẤT THƯỜNG VỀ THỜI GIAN (TEMPORAL ANOMALIES)
        // ─────────────────────────────────────────────────────────────────────
        LocalTime now = LocalTime.now();
        LocalTime start = SecurityConfig.getOffHoursStart();
        LocalTime end = SecurityConfig.getOffHoursEnd();
        boolean ngoaiGio = (now.isAfter(start) && now.isBefore(end)) || now.equals(start);

        if (ngoaiGio) {
            // Thao tác xóa hoặc sửa dữ liệu cốt lõi ngoài giờ (00:00 - 05:00 sáng)
            if (("DELETE".equals(actUpper) || "UPDATE".equals(actUpper)) && BANG_COT_LOI.contains(tblUpper)) {
                return new ThongTinViPham(
                        LoaiHanhViBatThuong.OFF_HOURS_SENSITIVE_ACTION,
                        String.format("Thao tác dữ liệu cốt lõi [%s] vào khung giờ nửa đêm / đóng cửa gara (%02d:%02d)",
                                tblUpper, now.getHour(), now.getMinute()),
                        tblUpper, cleanSql, nguoiDung
                );
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // 3. BẤT THƯỜNG VỀ TỐC ĐỘ & SỐ LƯỢNG (VELOCITY & BULK ANOMALIES)
        // ─────────────────────────────────────────────────────────────────────
        if ("DELETE".equals(actUpper) && BANG_COT_LOI.contains(tblUpper)) {
            // Xóa không có WHERE -> Xóa sạch bảng (Mass Delete)
            if (!sqlUpper.contains("WHERE")) {
                return new ThongTinViPham(
                        LoaiHanhViBatThuong.MASS_DELETE,
                        "Lệnh xóa toàn bộ dữ liệu bảng [" + tblUpper + "] không kèm điều kiện lọc WHERE",
                        tblUpper, cleanSql, nguoiDung
                );
            }

            // Ghi nhận sự kiện xóa vào sliding window
            long curTime = System.currentTimeMillis();
            LICHSU_XOA.addLast(new DeleteEvent(tblUpper, curTime));

            // Loại bỏ các sự kiện ngoài cửa sổ trượt (mặc định 2000ms)
            long windowMs = SecurityConfig.getVelocityDeleteWindowMs();
            long thresholdTime = curTime - windowMs;
            while (!LICHSU_XOA.isEmpty() && LICHSU_XOA.peekFirst().timestamp() < thresholdTime) {
                LICHSU_XOA.pollFirst();
            }

            // Kiểm tra số lượng xóa trong vòng 2 giây
            int deleteCount = LICHSU_XOA.size();
            if (deleteCount >= SecurityConfig.getVelocityDeleteThreshold()) {
                LICHSU_XOA.clear(); // Reset sau khi phát hiện
                return new ThongTinViPham(
                        LoaiHanhViBatThuong.MASS_DELETE,
                        String.format("Thực hiện %d lệnh xóa liên tiếp trên các bảng cốt lõi trong thời gian < %.1fs",
                                deleteCount, windowMs / 1000.0),
                        tblUpper, cleanSql, nguoiDung
                );
            }
        }

        return null;
    }

    /**
     * Thêm bảng vào danh sách ngoại lệ tạm thời trong X giây sau khi người dùng xác thực PIN thành công.
     */
    public static void choPhepTamThoi(String tableName) {
        if (tableName == null || tableName.isBlank()) return;
        long hetHan = System.currentTimeMillis() + SecurityConfig.getWhitelistDurationMs();
        WHITELIST_TAM_THOI.put(tableName.toUpperCase(), hetHan);
        log.info("Đã cấp phép ngoại lệ tạm thời cho bảng [{}] trong {} giây",
                tableName, SecurityConfig.getWhitelistDurationMs() / 1000);
    }

    private static boolean laNgoaiLeTamThoi(String tableName) {
        if (tableName == null) return false;
        Long exp = WHITELIST_TAM_THOI.get(tableName.toUpperCase());
        if (exp == null) return false;
        if (System.currentTimeMillis() < exp) {
            return true;
        }
        WHITELIST_TAM_THOI.remove(tableName.toUpperCase());
        return false;
    }

    private static String rutGon(String sql) {
        if (sql.length() <= 80) return sql;
        return sql.substring(0, 77) + "...";
    }
}

