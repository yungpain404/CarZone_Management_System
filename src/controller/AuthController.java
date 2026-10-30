package controller;

import connectDB.DbUtil;
import dao.TaiKhoanDao;
import entity.NhanVien;
import entity.TaiKhoan;
import enums.VaiTro;
import session.UserSession;
import ui.DangNhapUI;

import javax.swing.*;
import java.util.List;

/**
 * Controller phụ trách xác thực người dùng và quản lý phiên làm việc.
 * - Kiểm tra tên đăng nhập, mật khẩu trực tiếp dưới cơ sở dữ liệu (không mockup dữ liệu).
 * - Cấp quyền và danh sách navigation menu theo role xác thực từ session.
 * - Cung cấp dữ liệu định danh cho Header và xử lý luồng đăng xuất.
 */
public class AuthController {

    private final TaiKhoanDao taiKhoanDao;

    public AuthController() {
        this.taiKhoanDao = new TaiKhoanDao();
    }

    /**
     * DTO kết quả xác thực trả về cho tầng giao diện UI.
     */
    public static class KetQuaXacThuc {
        private final boolean thanhCong;
        private final String thongBao;
        private final NhanVien nhanVien;

        public KetQuaXacThuc(boolean thanhCong, String thongBao, NhanVien nhanVien) {
            this.thanhCong = thanhCong;
            this.thongBao = thongBao;
            this.nhanVien = nhanVien;
        }

        public boolean isThanhCong() {
            return thanhCong;
        }

        public String getThongBao() {
            return thongBao;
        }

        public NhanVien getNhanVien() {
            return nhanVien;
        }
    }

    /**
     * Item đại diện cho một mục Navigation bên thanh menu trái.
     */
    public static class NavigationItem {
        private final String id;
        private final String icon;
        private final String tieuDe;
        private final String breadcrumb;

        public NavigationItem(String id, String icon, String tieuDe, String breadcrumb) {
            this.id = id;
            this.icon = icon;
            this.tieuDe = tieuDe;
            this.breadcrumb = breadcrumb;
        }

        public String getId() {
            return id;
        }

        public String getIcon() {
            return icon;
        }

        public String getTieuDe() {
            return tieuDe;
        }

        public String getBreadcrumb() {
            return breadcrumb;
        }
    }

    /**
     * Kiểm tra tài khoản và mật khẩu trực tiếp dưới cơ sở dữ liệu.
     * TUYỆT ĐỐI KHÔNG MOCKUP DỮ LIỆU.
     *
     * @param tenDangNhap Tên đăng nhập nhập từ form UI (định dạng CCHxxx, TNVxxx, KTVxxx)
     * @param matKhau Mật khẩu gốc người dùng nhập
     * @return KetQuaXacThuc chứa trạng thái và thông báo chi tiết
     */
    public KetQuaXacThuc dangNhap(String tenDangNhap, String matKhau) {
        // 1. Kiểm tra đầu vào sớm ở phía Java
        if (tenDangNhap == null || tenDangNhap.isBlank()) {
            return new KetQuaXacThuc(false, "Vui lòng nhập tên đăng nhập.", null);
        }
        if (matKhau == null || matKhau.isBlank()) {
            return new KetQuaXacThuc(false, "Vui lòng nhập mật khẩu.", null);
        }

        String username = tenDangNhap.trim().toUpperCase();
        if (!username.matches("^(CCH|TNV|KTV)\\d{3}$")) {
            return new KetQuaXacThuc(false, "Tên đăng nhập không đúng định dạng (VD: CCH001, TNV001, KTV001).", null);
        }

        // 2. Kiểm tra trạng thái kết nối CSDL thực tế
        if (!DbUtil.isDatabaseOnline()) {
            return new KetQuaXacThuc(false, "Không thể kết nối đến cơ sở dữ liệu SQL Server. Vui lòng kiểm tra Docker.", null);
        }

        // 3. Truy vấn trực tiếp từ Database thông qua TaiKhoanDao (KHÔNG MOCKUP)
        TaiKhoan taiKhoan;
        try {
            taiKhoan = taiKhoanDao.timTheoTenDangNhap(username);
        } catch (Exception e) {
            return new KetQuaXacThuc(false, "Lỗi truy vấn cơ sở dữ liệu: " + e.getMessage(), null);
        }

        if (taiKhoan == null) {
            return new KetQuaXacThuc(false, "Tên đăng nhập hoặc mật khẩu không chính xác.", null);
        }

        // 4. Kiểm tra trạng thái hoạt động của tài khoản
        if (!taiKhoan.isTrangThaiHoatDong()) {
            return new KetQuaXacThuc(false, "Tài khoản đã bị vô hiệu hóa. Vui lòng liên hệ Chủ cửa hàng.", null);
        }

        // 5. Kiểm tra mật khẩu (đối chiếu hash SHA-256 đã lưu trong DB)
        if (!taiKhoan.khopMatKhau(matKhau)) {
            return new KetQuaXacThuc(false, "Tên đăng nhập hoặc mật khẩu không chính xác.", null);
        }

        NhanVien nhanVien = taiKhoan.getNhanVien();
        if (nhanVien == null) {
            return new KetQuaXacThuc(false, "Tài khoản chưa được liên kết với hồ sơ nhân viên trong hệ thống.", null);
        }

        // 6. Khởi tạo phiên làm việc (Session) cho người dùng
        UserSession.getInstance().batDauPhien(taiKhoan, nhanVien);

        return new KetQuaXacThuc(true, "Chào mừng " + nhanVien.getHoTen() + "!", nhanVien);
    }

    /**
     * Xử lý đăng xuất: xác nhận với người dùng, xóa phiên làm việc và mở lại màn hình đăng nhập.
     *
     * @param currentFrame Cửa sổ chính hiện tại cần đóng
     */
    public void dangXuat(JFrame currentFrame) {
        int opt = JOptionPane.showConfirmDialog(currentFrame,
                "Bạn có chắc chắn muốn đăng xuất khỏi hệ thống CarZone?",
                "Xác nhận đăng xuất",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (opt == JOptionPane.YES_OPTION) {
            // Hủy phiên làm việc
            UserSession.getInstance().huyPhien();

            // Đóng cửa sổ chính
            if (currentFrame != null) {
                currentFrame.dispose();
            }

            // Mở lại màn hình Đăng nhập
            SwingUtilities.invokeLater(() -> {
                DangNhapUI loginFrame = new DangNhapUI();
                loginFrame.setDangNhapListener(nhanVien -> {
                    // Khi đăng nhập lại thì mở lại MainFrame
                    app.App.moManHinhChinh(nhanVien);
                });
                loginFrame.setVisible(true);
            });
        }
    }

    /**
     * Lấy danh sách Navigation Item theo đúng vai trò được xác thực trong Session.
     * Bám sát chuẩn cấu trúc các phân hệ trong Figma.
     */
    public List<NavigationItem> layDanhSachNavigationTheoSession() {
        VaiTro vaiTro = UserSession.getInstance().getVaiTro();
        return layDanhSachNavigationTheoRole(vaiTro);
    }

    /**
     * Lấy danh sách Navigation Item theo vai trò cụ thể.
     */
    public List<NavigationItem> layDanhSachNavigationTheoRole(VaiTro vaiTro) {
        if (vaiTro == null) {
            vaiTro = VaiTro.CHU_CUA_HANG;
        }

        return switch (vaiTro) {
            case CHU_CUA_HANG -> List.of(
                    new NavigationItem("NAV_DASHBOARD", "📊", "Dashboard", "Trang chủ / Tổng quan"),
                    new NavigationItem("NAV_THONG_KE", "📈", "Thống kê doanh thu", "Kinh doanh / Báo cáo doanh thu"),
                    new NavigationItem("NAV_QUAN_LY_DICH_VU", "🔧", "Quản lý dịch vụ", "Danh mục / Dịch vụ sửa chữa"),
                    new NavigationItem("NAV_LINH_KIEN_KHO", "📦", "Linh kiện và tồn kho", "Kho hàng / Quản lý linh kiện"),
                    new NavigationItem("NAV_YC_LINH_KIEN", "📋", "Yêu cầu cập nhật linh kiện", "Kho hàng / Yêu cầu phê duyệt"),
                    new NavigationItem("NAV_KHUYEN_MAI", "🎁", "Chương trình khuyến mãi", "Kinh doanh / Khuyến mãi"),
                    new NavigationItem("NAV_NHAN_VIEN", "👥", "Quản lý nhân viên", "Hệ thống / Hồ sơ nhân viên"),
                    new NavigationItem("NAV_TAI_KHOAN", "🔐", "Tài khoản và phân quyền", "Hệ thống / Quản lý tài khoản"),
                    new NavigationItem("NAV_TRA_CUU", "🔍", "Tra cứu", "Tra cứu / Tìm kiếm tổng hợp"),
                    new NavigationItem("NAV_THONG_TIN_CA_NHAN", "👤", "Thông tin cá nhân", "Tài khoản / Hồ sơ cá nhân")
            );

            case TIEP_NHAN_VIEN -> List.of(
                    new NavigationItem("NAV_DASHBOARD", "📊", "Dashboard", "Trang chủ / Tổng quan tiếp nhận"),
                    new NavigationItem("NAV_KHACH_HANG_XE", "👥", "Hồ sơ khách hàng và xe", "Khách hàng / Quản lý khách & xe"),
                    new NavigationItem("NAV_PHIEU_TIEP_NHAN", "📝", "Lập phiếu tiếp nhận – sửa chữa", "Nghiệp vụ / Tiếp nhận xe"),
                    new NavigationItem("NAV_HOA_DON", "🧾", "Lập hóa đơn thanh toán", "Thu ngân / Hóa đơn thanh toán"),
                    new NavigationItem("NAV_DOI_TRA", "🔄", "Tạo phiếu đổi trả", "Nghiệp vụ / Đổi trả linh kiện"),
                    new NavigationItem("NAV_TRA_CUU", "🔍", "Tra cứu", "Tra cứu / Tìm kiếm tổng hợp"),
                    new NavigationItem("NAV_THONG_TIN_CA_NHAN", "👤", "Thông tin cá nhân", "Tài khoản / Hồ sơ cá nhân")
            );

            case KY_THUAT_VIEN -> List.of(
                    new NavigationItem("NAV_DASHBOARD", "📊", "Dashboard", "Trang chủ / Tổng quan kỹ thuật"),
                    new NavigationItem("NAV_PHIEU_PHAN_CONG", "📋", "Phiếu được phân công", "Kỹ thuật / Danh sách phiếu được giao"),
                    new NavigationItem("NAV_CAP_NHAT_SUA_CHUA", "🛠️", "Cập nhật phiếu sửa chữa", "Kỹ thuật / Tiến độ & Thay thế linh kiện"),
                    new NavigationItem("NAV_YC_LINH_KIEN", "📦", "Yêu cầu cập nhật linh kiện", "Kỹ thuật / Yêu cầu xuất/nhập linh kiện"),
                    new NavigationItem("NAV_TRA_CUU", "🔍", "Tra cứu", "Tra cứu / Tìm kiếm kỹ thuật"),
                    new NavigationItem("NAV_THONG_TIN_CA_NHAN", "👤", "Thông tin cá nhân", "Tài khoản / Hồ sơ cá nhân")
            );
        };
    }

    /**
     * Tên tiếng Việt hiển thị của vai trò.
     */
    public static String layTenVaiTro(VaiTro vaiTro) {
        if (vaiTro == null) return "Chủ cửa hàng";
        return switch (vaiTro) {
            case CHU_CUA_HANG   -> "Chủ cửa hàng";
            case TIEP_NHAN_VIEN -> "Tiếp nhận viên";
            case KY_THUAT_VIEN  -> "Kỹ thuật viên";
        };
    }
}
