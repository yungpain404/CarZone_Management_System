package controller;

import dao.DichVuDAO;
import dao.KhuyenMaiDAO;
import dao.LinhKienDAO;
import entity.DichVu;
import entity.KhuyenMai;
import entity.LinhKien;
import entity.NhanVien;
import enums.VaiTro;
import session.UserSession;

import java.util.Date;
import java.util.List;
import java.util.Set;

/** Điều phối tra cứu và quản lý khuyến mãi của Chủ cửa hàng. */
public class KhuyenMaiController {
    private final KhuyenMaiDAO khuyenMaiDao;
    private final DichVuDAO dichVuDao;
    private final LinhKienDAO linhKienDao;

    public KhuyenMaiController() { this(new KhuyenMaiDAO(), new DichVuDAO(), new LinhKienDAO()); }

    public KhuyenMaiController(KhuyenMaiDAO khuyenMaiDao, DichVuDAO dichVuDao, LinhKienDAO linhKienDao) {
        this.khuyenMaiDao = khuyenMaiDao;
        this.dichVuDao = dichVuDao;
        this.linhKienDao = linhKienDao;
    }

    public List<KhuyenMaiDAO.KhuyenMaiTongQuan> timKiem(String tuKhoa, Date tuNgay, Date denNgay) {
        yeuCauChuCuaHang();
        if (tuNgay == null || denNgay == null || denNgay.before(tuNgay)) {
            throw new IllegalArgumentException("Khoảng ngày tìm kiếm không hợp lệ.");
        }
        return khuyenMaiDao.timKiem(tuKhoa == null ? "" : tuKhoa.trim(), tuNgay, denNgay);
    }

    public KhuyenMai timTheoMa(String maKM) {
        yeuCauChuCuaHang();
        if (maKM == null || !maKM.matches("^KM\\d{3}$")) throw new IllegalArgumentException("Mã khuyến mãi không hợp lệ.");
        KhuyenMai result = khuyenMaiDao.timTheoMa(maKM);
        if (result == null) throw new IllegalArgumentException("Không tìm thấy chương trình " + maKM + ".");
        return result;
    }

    public DichVu timDichVuTheoMa(String maDV) {
        yeuCauChuCuaHang();
        if (maDV == null || !maDV.trim().matches("^DV\\d{5,}$")) throw new IllegalArgumentException("Nhập mã dịch vụ dạng DVXXXXX.");
        DichVu result = dichVuDao.timTheoMa(maDV.trim());
        if (result == null) throw new IllegalArgumentException("Không tìm thấy dịch vụ " + maDV + ".");
        if (!result.isDangCungCap()) throw new IllegalArgumentException("Dịch vụ đã ngừng cung cấp, không thể thêm vào khuyến mãi.");
        return result;
    }

    public LinhKien timLinhKienTheoMa(String maLK) {
        yeuCauChuCuaHang();
        if (maLK == null || !maLK.trim().matches("^LK\\d{5,}$")) throw new IllegalArgumentException("Nhập mã linh kiện dạng LKXXXXX.");
        LinhKien result = linhKienDao.timTheoMa(maLK.trim());
        if (result == null) throw new IllegalArgumentException("Không tìm thấy linh kiện " + maLK + ".");
        if (!result.isDangKinhDoanh()) throw new IllegalArgumentException("Linh kiện đã ngừng kinh doanh, không thể thêm vào khuyến mãi.");
        return result;
    }

    public KhuyenMai them(String ten, double mucGiam, Date ngayBatDau, Date ngayKetThuc,
                          String ghiChu, Set<DichVu> dichVu, Set<LinhKien> linhKien) {
        yeuCauChuCuaHang();
        KhuyenMai item = taoKhuyenMai(null, ten, mucGiam, ngayBatDau, ngayKetThuc, ghiChu, dichVu, linhKien);
        item.setNhanVien(nhanVienChuCuaHang());
        return khuyenMaiDao.them(item);
    }

    public KhuyenMai capNhat(String maKM, String ten, double mucGiam, Date ngayBatDau, Date ngayKetThuc,
                             String ghiChu, Set<DichVu> dichVu, Set<LinhKien> linhKien) {
        yeuCauChuCuaHang();
        KhuyenMai current = timTheoMa(maKM);
        KhuyenMai updated = taoKhuyenMai(maKM, ten, mucGiam, ngayBatDau, ngayKetThuc, ghiChu, dichVu, linhKien);
        updated.setNhanVien(current.getNhanVien());
        if (khuyenMaiDao.daApDungHoaDon(maKM)) {
            updated.setMucGiamPhanTram(current.getMucGiamPhanTram());
            updated.setNgayBatDau(current.getNgayBatDau());
            updated.setNgayKetThuc(current.getNgayKetThuc());
            updated.setDichVuApDung(current.getDichVuApDung());
            updated.setLinhKienApDung(current.getLinhKienApDung());
        }
        return khuyenMaiDao.capNhat(updated);
    }

    public boolean daApDungHoaDon(String maKM) {
        yeuCauChuCuaHang();
        return khuyenMaiDao.daApDungHoaDon(maKM);
    }

    private static KhuyenMai taoKhuyenMai(String maKM, String ten, double mucGiam, Date batDau, Date ketThuc,
                                          String ghiChu, Set<DichVu> dichVu, Set<LinhKien> linhKien) {
        KhuyenMai item = new KhuyenMai();
        if (maKM != null) item.setMaKM(maKM);
        item.setTenChuongTrinh(ten == null ? null : ten.trim());
        item.setMucGiamPhanTram(mucGiam);
        item.setNgayBatDau(batDau);
        item.setNgayKetThuc(ketThuc);
        item.setGhiChu(ghiChu == null || ghiChu.isBlank() ? null : ghiChu.trim());
        item.setDichVuApDung(dichVu);
        item.setLinhKienApDung(linhKien);
        return item;
    }

    private static NhanVien nhanVienChuCuaHang() {
        NhanVien nhanVien = UserSession.getInstance().getNhanVien();
        if (nhanVien == null || nhanVien.getVaiTro() != VaiTro.CHU_CUA_HANG) {
            throw new SecurityException("Chỉ Chủ cửa hàng được quản lý chương trình khuyến mãi.");
        }
        return nhanVien;
    }

    private static void yeuCauChuCuaHang() { nhanVienChuCuaHang(); }
}
