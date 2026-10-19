package entity;

import java.util.Objects;

public class ChiTietDichVu {
    private PhieuTiepNhanSuaChua phieuSuaChua;
    private DichVu dichVu;
    private int soLuong;
    private double donGiaThucHien;
    private String ghiChu;

    public ChiTietDichVu() {
        soLuong = 1;
    }
    public ChiTietDichVu(PhieuTiepNhanSuaChua phieuSuaChua) {
    	setPhieuSuaChua(phieuSuaChua);
    }
    public ChiTietDichVu(PhieuTiepNhanSuaChua phieuSuaChua, DichVu dichVu, int soLuong, double donGiaThucHien, String ghiChu) {
        setPhieuSuaChua(phieuSuaChua);
        setDichVu(dichVu);
        setSoLuong(soLuong);
        setDonGiaThucHien(donGiaThucHien);
        this.ghiChu = ghiChu;
    }

    public PhieuTiepNhanSuaChua getPhieuSuaChua() {
    	return phieuSuaChua;
    }
    public void setPhieuSuaChua(PhieuTiepNhanSuaChua phieuSuaChua) {
        if (phieuSuaChua == null) throw new IllegalArgumentException("Chi tiết dịch vụ phải thuộc phiếu sửa chữa.");
    	this.phieuSuaChua = phieuSuaChua;
    }
    public DichVu getDichVu() {
    	return dichVu;
    }
    public void setDichVu(DichVu dichVu) {
        if (dichVu == null) throw new IllegalArgumentException("Phải chọn dịch vụ.");
    	this.dichVu = dichVu;
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

    public double tinhThanhTien() {
        return soLuong * donGiaThucHien;
    }
    public String getGhiChu() {
    	return ghiChu;
    }
    public void setGhiChu(String ghiChu) {
    	this.ghiChu = ghiChu;
    }

    @Override public int hashCode() {
    	return Objects.hash(phieuSuaChua);
    }
    @Override public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        return Objects.equals(phieuSuaChua, ((ChiTietDichVu) obj).phieuSuaChua);
    }
    @Override public String toString() {
    	return "ChiTietDichVu [" + "phieuSuaChua=" + phieuSuaChua + ", " + "dichVu=" + dichVu + ", " + "soLuong=" + soLuong + ", " + "donGiaThucHien=" + donGiaThucHien + ", " + "ghiChu=" + ghiChu + "]";
    }
}
