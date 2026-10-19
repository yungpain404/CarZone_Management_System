package entity;

import java.util.Objects;
import enums.TrangThaiThanhToan;

public class PhieuDoiTra {
    private String maPhieuDT;
    private HoaDon hoaDon;
    private java.util.Date ngayLap;
    private String lyDoDoiTra;
    private double soTienHoanTra;

    public PhieuDoiTra() {
    }
    public PhieuDoiTra(String maPhieuDT) {
    	setMaPhieuDT(maPhieuDT);
    }
    public PhieuDoiTra(String maPhieuDT, HoaDon hoaDon, java.util.Date ngayLap, String lyDoDoiTra, double soTienHoanTra) {
        setMaPhieuDT(maPhieuDT);
        setHoaDon(hoaDon);
        setNgayLap(ngayLap);
        setLyDoDoiTra(lyDoDoiTra);
        setSoTienHoanTra(soTienHoanTra);
    }

    public String getMaPhieuDT() {
    	return maPhieuDT;
    }
    public void setMaPhieuDT(String maPhieuDT) {
        if (maPhieuDT == null || !maPhieuDT.matches("^PDT\\d{8}[AP]\\d{2,}$")) throw new IllegalArgumentException("Mã phiếu đổi trả không đúng định dạng.");
    	this.maPhieuDT = maPhieuDT;
    }
    public HoaDon getHoaDon() {
    	return hoaDon;
    }
    public void setHoaDon(HoaDon hoaDon) {
        if (hoaDon == null || hoaDon.getTrangThaiThanhToan() != TrangThaiThanhToan.DA_THANH_TOAN) throw new IllegalArgumentException("Hóa đơn gốc không hợp lệ.");
    	this.hoaDon = hoaDon;
    }
    public java.util.Date getNgayLap() {
    	return ngayLap;
    }
    public void setNgayLap(java.util.Date ngayLap) {
        if (ngayLap == null || hoaDon == null || hoaDon.getNgayLap() == null) throw new IllegalArgumentException("Ngày lập phiếu đổi trả không hợp lệ.");
        long quaHan = hoaDon.getNgayLap().getTime() + 7L * 24 * 60 * 60 * 1000;
        if (ngayLap.before(hoaDon.getNgayLap()) || ngayLap.getTime() > quaHan) throw new IllegalArgumentException("Hóa đơn đã quá hạn đổi trả (7 ngày).");
    	this.ngayLap = ngayLap;
    }
    public String getLyDoDoiTra() {
    	return lyDoDoiTra;
    }
    public void setLyDoDoiTra(String lyDoDoiTra) {
        if (lyDoDoiTra == null || lyDoDoiTra.isBlank()) throw new IllegalArgumentException("Lý do đổi trả không được để trống.");
    	this.lyDoDoiTra = lyDoDoiTra;
    }
    public double getSoTienHoanTra() {
    	return soTienHoanTra;
    }
    public void setSoTienHoanTra(double soTienHoanTra) {
        if (!Double.isFinite(soTienHoanTra) || soTienHoanTra < 0) throw new IllegalArgumentException("Số tiền hoàn trả không hợp lệ.");
    	this.soTienHoanTra = soTienHoanTra;
    }

    @Override public int hashCode() {
    	return Objects.hash(maPhieuDT);
    }
    @Override public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        return Objects.equals(maPhieuDT, ((PhieuDoiTra) obj).maPhieuDT);
    }
    @Override public String toString() {
    	return "PhieuDoiTra [" + "maPhieuDT=" + maPhieuDT + ", " + "hoaDon=" + hoaDon + ", " + "ngayLap=" + ngayLap + ", " + "lyDoDoiTra=" + lyDoDoiTra + ", " + "soTienHoanTra=" + soTienHoanTra + "]";
    }
}
