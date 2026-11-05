package controller;

import dao.QuyenTaiKhoanDAO;
import enums.CheDoTruyCap;
import enums.VaiTro;
import session.UserSession;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Cấu hình các màn hình được phép mở riêng cho từng tài khoản. */
public class PhanQuyenController {
    private static final List<ChucNang> CHUC_NANG = List.of(
            new ChucNang("NAV_THONG_KE_TIEP_NHAN", "Thống kê tiếp nhận", VaiTro.TIEP_NHAN_VIEN),
            new ChucNang("NAV_KHACH_HANG_XE", "Hồ sơ khách hàng và xe", VaiTro.TIEP_NHAN_VIEN),
            new ChucNang("NAV_PHIEU_TIEP_NHAN", "Quản lý phiếu tiếp nhận", VaiTro.TIEP_NHAN_VIEN),
            new ChucNang("NAV_HOA_DON", "Lập hóa đơn thanh toán", VaiTro.TIEP_NHAN_VIEN),
            new ChucNang("NAV_DOI_TRA", "Quản lý phiếu đổi trả", VaiTro.TIEP_NHAN_VIEN),
            new ChucNang("NAV_TRA_CUU", "Tra cứu", null),
            new ChucNang("NAV_THONG_KE", "Thống kê sửa chữa", VaiTro.KY_THUAT_VIEN),
            new ChucNang("NAV_PHIEU_PHAN_CONG", "Phiếu được phân công", VaiTro.KY_THUAT_VIEN),
            new ChucNang("NAV_CAP_NHAT_SUA_CHUA", "Cập nhật phiếu sửa chữa", VaiTro.KY_THUAT_VIEN),
            new ChucNang("NAV_YC_LINH_KIEN", "Yêu cầu cập nhật linh kiện", VaiTro.KY_THUAT_VIEN));

    private final QuyenTaiKhoanDAO dao;

    public PhanQuyenController() { this(new QuyenTaiKhoanDAO()); }
    public PhanQuyenController(QuyenTaiKhoanDAO dao) { this.dao = dao; }

    public List<ChucNang> danhSachChucNang() {
        yeuCauChuCuaHang();
        return CHUC_NANG;
    }

    public QuyenTaiKhoanDAO.CauHinh layCauHinh(String maNV) {
        yeuCauChuCuaHang();
        return dao.layCauHinh(maNV);
    }

    public void luu(String maNV, CheDoTruyCap mode, Set<String> functions) {
        yeuCauChuCuaHang();
        if (maNV == null || maNV.isBlank() || mode == null)
            throw new IllegalArgumentException("Chọn một tài khoản và chế độ truy cập.");
        Set<String> allowed = CHUC_NANG.stream().map(ChucNang::ma).collect(java.util.stream.Collectors.toSet());
        if (functions == null || !allowed.containsAll(functions))
            throw new IllegalArgumentException("Danh sách chức năng được chọn không hợp lệ.");
        if (mode == CheDoTruyCap.TUY_CHINH && functions.isEmpty())
            throw new IllegalArgumentException("Chọn ít nhất một chức năng hoặc chuyển về quyền theo vai trò.");
        dao.luuCauHinh(maNV, mode, new LinkedHashSet<>(functions));
    }

    public static Set<String> chucNangTheoVaiTro(VaiTro role) {
        return CHUC_NANG.stream().filter(item -> item.vaiTro() == null || item.vaiTro() == role)
                .map(ChucNang::ma).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public static Set<String> chucNangTheoProfil(CheDoTruyCap mode, VaiTro role, Set<String> selected) {
        return switch (mode) {
            case THEO_VAI_TRO -> chucNangTheoVaiTro(role);
            case TOAN_BO_TNV -> chucNangTheoVaiTro(VaiTro.TIEP_NHAN_VIEN);
            case TOAN_BO_KTV -> chucNangTheoVaiTro(VaiTro.KY_THUAT_VIEN);
            case TUY_CHINH -> selected == null ? Set.of() : Set.copyOf(selected);
        };
    }

    private static void yeuCauChuCuaHang() {
        if (UserSession.getInstance().getVaiTro() != VaiTro.CHU_CUA_HANG)
            throw new SecurityException("Chỉ Chủ cửa hàng được phân quyền tài khoản.");
    }

    public record ChucNang(String ma, String ten, VaiTro nhomVaiTro) {
        private VaiTro vaiTro() { return nhomVaiTro; }
    }
}
