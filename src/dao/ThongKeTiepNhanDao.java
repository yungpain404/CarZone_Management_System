package dao;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Interface DAO phục vụ nghiệp vụ Thống kê tiếp nhận (UC013 - Tiếp nhận viên).
 */
public interface ThongKeTiepNhanDao {

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

    /**
     * Lấy dữ liệu thống kê tiếp nhận theo nhân viên tiếp nhận, khoảng ngày và trạng thái.
     * TNV chỉ xem dữ liệu trong phạm vi của mình (maNVTiepNhan).
     */
    ThongKeData layThongKe(String maNVTiepNhan, Date tuNgay, Date denNgay, String trangThai);
}
