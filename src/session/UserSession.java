package session;

import entity.NhanVien;
import entity.TaiKhoan;
import enums.CheDoTruyCap;
import enums.VaiTro;

import java.util.Date;
import java.util.Set;

/**
 * Quản lý phiên làm việc (Session) của người dùng hiện tại sau khi xác thực thành công.
 * Áp dụng mô hình Singleton để toàn bộ ứng dụng (Header, Navigation, ContentPanel, Controller)
 * có thể truy xuất thông tin nhân viên và vai trò một cách nhất quán.
 */
public class UserSession {

    private static volatile UserSession instance;

    private TaiKhoan taiKhoan;
    private NhanVien nhanVien;
    private Date thoiDiemDangNhap;
    private Set<String> maChucNangDuocPhep = Set.of();
    private CheDoTruyCap cheDoTruyCap = CheDoTruyCap.THEO_VAI_TRO;

    private UserSession() {
    }

    public static UserSession getInstance() {
        if (instance == null) {
            synchronized (UserSession.class) {
                if (instance == null) {
                    instance = new UserSession();
                }
            }
        }
        return instance;
    }

    /**
     * Khởi tạo phiên làm việc sau khi xác thực thành công từ cơ sở dữ liệu.
     *
     * @param taiKhoan thực thể TaiKhoan lấy trực tiếp từ database
     * @param nhanVien thực thể NhanVien tương ứng lấy từ database
     */
    public synchronized void batDauPhien(TaiKhoan taiKhoan, NhanVien nhanVien) {
        this.taiKhoan = taiKhoan;
        this.nhanVien = nhanVien;
        this.thoiDiemDangNhap = new Date();
        this.cheDoTruyCap = CheDoTruyCap.THEO_VAI_TRO;
        this.maChucNangDuocPhep = Set.of();
    }

    /**
     * Hủy phiên làm việc hiện tại khi người dùng đăng xuất.
     */
    public synchronized void huyPhien() {
        this.taiKhoan = null;
        this.nhanVien = null;
        this.thoiDiemDangNhap = null;
        this.maChucNangDuocPhep = Set.of();
        this.cheDoTruyCap = CheDoTruyCap.THEO_VAI_TRO;
    }

    /** Kiểm tra xem phiên có đang hoạt động không. */
    public boolean daDangNhap() {
        return this.taiKhoan != null && this.nhanVien != null;
    }

    public TaiKhoan getTaiKhoan() {
        return taiKhoan;
    }

    public NhanVien getNhanVien() {
        return nhanVien;
    }

    public VaiTro getVaiTro() {
        return nhanVien != null ? nhanVien.getVaiTro() : null;
    }

    public String getHoTen() {
        return nhanVien != null ? nhanVien.getHoTen() : "Khách";
    }

    public String getMaNV() {
        return nhanVien != null ? nhanVien.getMaNV() : "";
    }

    public String getTenDangNhap() {
        return taiKhoan != null ? taiKhoan.getTenDangNhap() : "";
    }

    public Date getThoiDiemDangNhap() {
        return thoiDiemDangNhap;
    }

    /**
     * Kiểm tra người dùng hiện tại có vai trò tương ứng không.
     */
    public boolean coVaiTro(VaiTro vaiTroKiemTra) {
        return getVaiTro() == vaiTroKiemTra;
    }

    /** Áp dụng cấu hình truy cập cá nhân đã đọc từ CSDL tại thời điểm đăng nhập. */
    public synchronized void datCauHinhTruyCap(CheDoTruyCap mode, Set<String> maChucNang) {
        this.cheDoTruyCap = mode == null ? CheDoTruyCap.THEO_VAI_TRO : mode;
        this.maChucNangDuocPhep = maChucNang == null ? Set.of() : Set.copyOf(maChucNang);
    }

    public CheDoTruyCap getCheDoTruyCap() { return cheDoTruyCap; }

    public boolean dangDungQuyenRieng() { return cheDoTruyCap != CheDoTruyCap.THEO_VAI_TRO; }

    /** Kiểm tra quyền menu theo hồ sơ mặc định hoặc bộ quyền riêng của tài khoản. */
    public boolean coQuyenChucNang(String maChucNang) {
        VaiTro role = getVaiTro();
        if (role == VaiTro.CHU_CUA_HANG) return true;
        if ("NAV_DASHBOARD".equals(maChucNang) || "NAV_THONG_TIN_CA_NHAN".equals(maChucNang)) return true;
        return switch (cheDoTruyCap) {
            case THEO_VAI_TRO -> roleChucNangMacDinh(role).contains(maChucNang);
            case TOAN_BO_TNV -> chucNangTNV().contains(maChucNang);
            case TOAN_BO_KTV -> chucNangKTV().contains(maChucNang);
            case TUY_CHINH -> maChucNangDuocPhep.contains(maChucNang);
        };
    }

    private static Set<String> roleChucNangMacDinh(VaiTro role) {
        if (role == VaiTro.TIEP_NHAN_VIEN) return chucNangTNV();
        if (role == VaiTro.KY_THUAT_VIEN) return chucNangKTV();
        return Set.of();
    }

    private static Set<String> chucNangTNV() { return Set.of("NAV_THONG_KE_TIEP_NHAN", "NAV_KHACH_HANG_XE",
            "NAV_PHIEU_TIEP_NHAN", "NAV_HOA_DON", "NAV_DOI_TRA", "NAV_TRA_CUU"); }

    private static Set<String> chucNangKTV() { return Set.of("NAV_THONG_KE", "NAV_PHIEU_PHAN_CONG",
            "NAV_CAP_NHAT_SUA_CHUA", "NAV_YC_LINH_KIEN", "NAV_TRA_CUU"); }
}
