package dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Interface DAO phục vụ nghiệp vụ Thống kê tiếp nhận (UC013 - Tiếp nhận viên và Quản lý).
 */
public interface ThongKeTiepNhanDao {

    // ─────────────────────────────────────────────────────────────────────────
    // DTO PHẦN 1: THỐNG KÊ TỔNG QUAN
    // ─────────────────────────────────────────────────────────────────────────
    class ThongKeData {
        public int tongPhieu;
        public int soXe;
        public int khachHangMoi;
        public int choXuLy;

        // Map ngày (dd/MM) -> số lượng phiếu
        public Map<String, Integer> xuHuongNgay;
        // Map tên trạng thái -> số lượng
        public Map<String, Integer> phanBoTrangThai;
        // Danh sách phiếu chi tiết
        public List<PhieuRow> danhSachPhieu;
    }

    class PhieuRow {
        public String maPhieu;
        public String ngayTiepNhan;
        public String khachHang;
        public String bienSo;
        public String trangThai;
        public String ktv;

        public PhieuRow(String maPhieu, String ngayTiepNhan, String khachHang, String bienSo, String trangThai, String ktv) {
            this.maPhieu = maPhieu;
            this.ngayTiepNhan = ngayTiepNhan;
            this.khachHang = khachHang;
            this.bienSo = bienSo;
            this.trangThai = trangThai;
            this.ktv = ktv;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DTO PHẦN 2: SO SÁNH THEO THỜI GIAN
    // ─────────────────────────────────────────────────────────────────────────
    class SoSanhThangData {
        public int thangNay;
        public int namNay;
        public int thangTruoc;
        public int namTruoc;

        public int phieuThangNay;
        public int phieuThangTruoc;
        public int chenhLechPhieu;
        public double tyLeTangTruong; // % (dương = tăng, âm = giảm)
        public boolean coDuLieuKyTruoc; // false nếu tháng trước = 0

        public int luotKhachHaiThang;
        public int soXeHaiThang;
        public int khachThangNay;
        public int khachThangTruoc;
        public int xeThangNay;
        public int xeThangTruoc;
    }

    class SoSanhNamData {
        public int namNay;
        public int namTruoc;

        public int phieuNamNay;
        public int phieuNamTruoc;
        public int chenhLechPhieu;
        public double tyLeTangTruong; // % (dương = tăng, âm = giảm)
        public boolean coDuLieuKyTruoc;

        public int luotKhachHaiNam;
        public int soXeHaiNam;
        public int khachNamNay;
        public int khachNamTruoc;
        public int xeNamNay;
        public int xeNamTruoc;

        // Phân bố số lượng phiếu theo 12 tháng (index 0 = Tháng 1, ... 11 = Tháng 12)
        public int[] phieuTheoThangNamNay = new int[12];
        public int[] phieuTheoThangNamTruoc = new int[12];
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DTO PHẦN 3: TÌNH HÌNH XỬ LÝ VÀ TỒN ĐỌNG PHIẾU
    // ─────────────────────────────────────────────────────────────────────────
    class TinhHinhXuLyData {
        public int choSua;
        public int dangSua;
        public int choLinhKien;
        public int hoanTat;
        public int tongCong;

        public List<PhieuTonDongRow> danhSachTonDong = new ArrayList<>();
    }

    class PhieuTonDongRow {
        public String maPhieu;
        public String ngayTiepNhan;
        public String bienSo;
        public String khachHang;
        public String trangThai;
        public String ktv;
        public String thoiGianTonDong; // "3 ngày 5 giờ", "18 giờ"...

        public PhieuTonDongRow(String maPhieu, String ngayTiepNhan, String bienSo, String khachHang, String trangThai, String ktv, String thoiGianTonDong) {
            this.maPhieu = maPhieu;
            this.ngayTiepNhan = ngayTiepNhan;
            this.bienSo = bienSo;
            this.khachHang = khachHang;
            this.trangThai = trangThai;
            this.ktv = ktv;
            this.thoiGianTonDong = thoiGianTonDong;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DTO PHẦN 4: THỐNG KÊ ĐỔI TRẢ
    // ─────────────────────────────────────────────────────────────────────────
    class ThongKeDoiTraData {
        /** Số phiếu đổi trả trong ngày hôm nay */
        public int soPhieuHomNay;
        /** Tổng tiền hoàn trả trong ngày hôm nay */
        public double tienHoanHomNay;
        /** Số phiếu đổi trả trong tháng hiện tại */
        public long soPhieuThangNay;
        /** Tổng tiền hoàn trả trong tháng hiện tại */
        public double tienHoanThangNay;
        /** Phân bố hướng xử lý: DOI_MOI, HOAN_TIEN, SUA_LAI_MIEN_PHI */
        public int soHoanTien;
        public int soDoiMoi;
        public int soSuaLai;
        /** 7 ngày gần nhất: key = dd/MM, value = số phiếu */
        public Map<String, Integer> xuHuong7Ngay;
        /** Danh sách phiếu đổi trả gần đây (tối đa 10) */
        public List<DoiTraRow> danhSachGanDay;
    }

    class DoiTraRow {
        public String maPhieuDT;
        public String ngayLap;
        public String maHoaDon;
        public String khachHang;
        public String bienSo;
        public String tienHoan;
        public String huongXuLy; // Tóm tắt các hướng xử lý

        public DoiTraRow(String maPhieuDT, String ngayLap, String maHoaDon,
                         String khachHang, String bienSo, String tienHoan, String huongXuLy) {
            this.maPhieuDT = maPhieuDT;
            this.ngayLap = ngayLap;
            this.maHoaDon = maHoaDon;
            this.khachHang = khachHang;
            this.bienSo = bienSo;
            this.tienHoan = tienHoan;
            this.huongXuLy = huongXuLy;
        }
    }

    /**
     * Lấy dữ liệu thống kê đổi trả (Phần 4): hôm nay, tháng này, xu hướng 7 ngày, phân bố hướng xử lý.
     */
    ThongKeDoiTraData layThongKeDoiTra(String maNVTiepNhan);

    /**
     * Lấy dữ liệu thống kê tiếp nhận theo nhân viên tiếp nhận, khoảng ngày và trạng thái.
     */
    ThongKeData layThongKe(String maNVTiepNhan, Date tuNgay, Date denNgay, String trangThai);

    /**
     * Lấy dữ liệu so sánh tiếp nhận giữa tháng này và tháng trước.
     */
    SoSanhThangData laySoSanhThang(String maNVTiepNhan);

    /**
     * Lấy dữ liệu so sánh tiếp nhận giữa năm nay và năm trước (kèm phân bố 12 tháng).
     */
    SoSanhNamData laySoSanhNam(String maNVTiepNhan);

    /**
     * Lấy dữ liệu tình hình xử lý phiếu (4 trạng thái) và danh sách phiếu tồn đọng lâu.
     */
    TinhHinhXuLyData layTinhHinhXuLy(String maNVTiepNhan);
}
