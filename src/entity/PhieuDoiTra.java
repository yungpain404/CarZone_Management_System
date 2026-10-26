package entity;

import java.util.Date;
import java.util.Objects;

import org.hibernate.annotations.Formula;

import enums.TrangThaiThanhToan;
import enums.VaiTro;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 * Phiếu đổi trả. Bảng PhieuDoiTra (V1). maPhieuDT do sp_LapPhieuDoiTra sinh (PDT + ddMMyyyy + A/P + 2 số).
 * soTienHoanTra là thuộc tính SUY DIỄN, đọc từ view vw_PhieuDoiTra_HoanTien (@Formula), chỉ có getter.
 */
@Entity
@Table(name = "PhieuDoiTra")
public class PhieuDoiTra {
    @Id
    @Column(name = "maPhieuDT", length = 14)
    private String maPhieuDT;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maHoaDon", nullable = false)
    private HoaDon hoaDon;

    /** Tiếp nhận viên lập phiếu (DB: cột maNV). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maNV", nullable = false)
    private NhanVien nhanVien;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "ngayLap", nullable = false)
    private Date ngayLap;

    @Column(name = "lyDoDoiTra", nullable = false, length = 500)
    private String lyDoDoiTra;

    @Formula("(SELECT r.soTienHoanTra FROM vw_PhieuDoiTra_HoanTien r WHERE r.maPhieuDT = maPhieuDT)")
    private double soTienHoanTra;

    public PhieuDoiTra() {
    }
    public PhieuDoiTra(String maPhieuDT) {
    	setMaPhieuDT(maPhieuDT);
    }
    public PhieuDoiTra(String maPhieuDT, HoaDon hoaDon, NhanVien nhanVien, Date ngayLap, String lyDoDoiTra) {
        setMaPhieuDT(maPhieuDT);
        setHoaDon(hoaDon);
        setNhanVien(nhanVien);
        setNgayLap(ngayLap);
        setLyDoDoiTra(lyDoDoiTra);
    }

    public String getMaPhieuDT() {
    	return maPhieuDT;
    }
    public void setMaPhieuDT(String maPhieuDT) {
        // DB: CK_PhieuDoiTra_Ma = PDT + 8 số + A/P + đúng 2 số
        if (maPhieuDT == null || !maPhieuDT.matches("^PDT\\d{8}[AP]\\d{2}$")) throw new IllegalArgumentException("Mã phiếu đổi trả không đúng định dạng.");
    	this.maPhieuDT = maPhieuDT;
    }
    public HoaDon getHoaDon() {
    	return hoaDon;
    }
    public void setHoaDon(HoaDon hoaDon) {
        if (hoaDon == null || hoaDon.getTrangThaiThanhToan() != TrangThaiThanhToan.DA_THANH_TOAN) throw new IllegalArgumentException("Hóa đơn gốc không hợp lệ.");
    	this.hoaDon = hoaDon;
    }
    public NhanVien getNhanVien() {
    	return nhanVien;
    }
    public void setNhanVien(NhanVien nhanVien) {
        if (nhanVien == null || nhanVien.getVaiTro() != VaiTro.TIEP_NHAN_VIEN) throw new IllegalArgumentException("Chỉ Tiếp nhận viên mới được lập phiếu đổi trả.");
    	this.nhanVien = nhanVien;
    }
    public Date getNgayLap() {
    	return ngayLap;
    }
    public void setNgayLap(Date ngayLap) {
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
        if (lyDoDoiTra.length() > 500) throw new IllegalArgumentException("Lý do đổi trả tối đa 500 ký tự.");
    	this.lyDoDoiTra = lyDoDoiTra;
    }
    /** Chỉ getter: tổng thành tiền các dòng có hướng xử lý HOAN_TIEN (tính ở view). */
    public double getSoTienHoanTra() {
    	return soTienHoanTra;
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
    	return "PhieuDoiTra [" + "maPhieuDT=" + maPhieuDT + ", " + "hoaDon=" + (hoaDon == null ? null : hoaDon.getMaHoaDon()) + ", " + "nhanVien=" + nhanVien + ", " + "ngayLap=" + ngayLap + ", " + "lyDoDoiTra=" + lyDoDoiTra + ", " + "soTienHoanTra=" + soTienHoanTra + "]";
    }
}
