package entity;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

/**
 * Dòng dịch vụ của một phiếu sửa chữa. Bảng ChiTietDichVu (V1), khóa chính (maPhieu, maDV).
 * Cột thanhTien trong DB là cột tính toán, không ánh xạ; dùng {@link #tinhThanhTien()} (cùng công thức).
 */
@Entity
@Table(name = "ChiTietDichVu")
public class ChiTietDichVu {
    @EmbeddedId
    private ChiTietDichVuId id = new ChiTietDichVuId();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("maPhieu")
    @JoinColumn(name = "maPhieu")
    private PhieuTiepNhanSuaChua phieuSuaChua;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("maDV")
    @JoinColumn(name = "maDV")
    private DichVu dichVu;

    @Column(name = "soLuong", nullable = false)
    private int soLuong;

    @Column(name = "donGiaThucHien", nullable = false)
    private double donGiaThucHien;

    @Column(name = "ghiChu", length = 300)
    private String ghiChu;

    public ChiTietDichVu() {
        soLuong = 1;
    }
    public ChiTietDichVu(PhieuTiepNhanSuaChua phieuSuaChua) {
    	this();
    	setPhieuSuaChua(phieuSuaChua);
    }
    public ChiTietDichVu(PhieuTiepNhanSuaChua phieuSuaChua, DichVu dichVu, int soLuong, double donGiaThucHien, String ghiChu) {
        setPhieuSuaChua(phieuSuaChua);
        setDichVu(dichVu);
        setSoLuong(soLuong);
        setDonGiaThucHien(donGiaThucHien);
        setGhiChu(ghiChu);
    }

    public PhieuTiepNhanSuaChua getPhieuSuaChua() {
    	return phieuSuaChua;
    }
    public void setPhieuSuaChua(PhieuTiepNhanSuaChua phieuSuaChua) {
        if (phieuSuaChua == null) throw new IllegalArgumentException("Chi tiết dịch vụ phải thuộc phiếu sửa chữa.");
    	this.phieuSuaChua = phieuSuaChua;
    	this.id.setMaPhieu(phieuSuaChua.getMaPhieu());
    }
    public DichVu getDichVu() {
    	return dichVu;
    }
    public void setDichVu(DichVu dichVu) {
        if (dichVu == null) throw new IllegalArgumentException("Phải chọn dịch vụ.");
    	this.dichVu = dichVu;
    	this.id.setMaDV(dichVu.getMaDV());
    }
    public int getSoLuong() {
    	return soLuong;
    }
    public void setSoLuong(int soLuong) {
        if (soLuong <= 0) throw new IllegalArgumentException("Số lượng dịch vụ phải lớn hơn 0.");
    	this.soLuong = soLuong;
    }
    public double getDonGiaThucHien() {
    	return donGiaThucHien;
    }
    public void setDonGiaThucHien(double donGiaThucHien) {
        if (!Double.isFinite(donGiaThucHien) || donGiaThucHien < 0) throw new IllegalArgumentException("Đơn giá thực hiện không được âm.");
    	this.donGiaThucHien = donGiaThucHien;
    }

    /** Thuộc tính suy diễn /thanhTien = soLuong * donGiaThucHien (trùng cột tính toán trong DB). */
    public double tinhThanhTien() {
        return soLuong * donGiaThucHien;
    }
    public String getGhiChu() {
    	return ghiChu;
    }
    public void setGhiChu(String ghiChu) {
        if (ghiChu != null && ghiChu.length() > 300) throw new IllegalArgumentException("Ghi chú tối đa 300 ký tự.");
    	this.ghiChu = ghiChu;
    }

    @Override public int hashCode() {
    	return Objects.hash(phieuSuaChua, dichVu);
    }
    @Override public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ChiTietDichVu o = (ChiTietDichVu) obj;
        return Objects.equals(phieuSuaChua, o.phieuSuaChua) && Objects.equals(dichVu, o.dichVu);
    }
    @Override public String toString() {
    	return "ChiTietDichVu [" + "phieuSuaChua=" + (phieuSuaChua == null ? null : phieuSuaChua.getMaPhieu()) + ", " + "dichVu=" + dichVu + ", " + "soLuong=" + soLuong + ", " + "donGiaThucHien=" + donGiaThucHien + ", " + "ghiChu=" + ghiChu + "]";
    }
}
