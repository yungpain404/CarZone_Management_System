package dao;

import java.util.List;

/**
 * Interface DAO cho chi tiết các khoản mục trên hóa đơn (linh kiện & dịch vụ).
 */
public interface ChiTietHoaDonDao {

    /**
     * DTO đại diện cho một dòng chi tiết hóa đơn (dịch vụ hoặc linh kiện).
     */
    class ChiTietItem {
        private String maHoaDon;
        private String loai; // "DICH_VU" hoặc "LINH_KIEN"
        private String maKhoanMuc;
        private String tenKhoanMuc;
        private int soLuong;
        private double donGia;
        private double thanhTien;

        public ChiTietItem(String maHoaDon, String loai, String maKhoanMuc, String tenKhoanMuc, int soLuong, double donGia, double thanhTien) {
            this.maHoaDon = maHoaDon;
            this.loai = loai;
            this.maKhoanMuc = maKhoanMuc;
            this.tenKhoanMuc = tenKhoanMuc;
            this.soLuong = soLuong;
            this.donGia = donGia;
            this.thanhTien = thanhTien;
        }

        public String getMaHoaDon() { return maHoaDon; }
        public String getLoai() { return loai; }
        public String getMaKhoanMuc() { return maKhoanMuc; }
        public String getTenKhoanMuc() { return tenKhoanMuc; }
        public int getSoLuong() { return soLuong; }
        public double getDonGia() { return donGia; }
        public double getThanhTien() { return thanhTien; }
    }

    /**
     * Lấy toàn bộ các dòng chi tiết của một hóa đơn theo mã hóa đơn.
     *
     * @param maHoaDon mã hóa đơn
     * @return danh sách các khoản mục chi tiết
     */
    List<ChiTietItem> layChiTietTheoHoaDon(String maHoaDon);
}
