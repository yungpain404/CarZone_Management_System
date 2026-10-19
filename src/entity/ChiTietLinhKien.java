package entity;

import java.util.Objects;

public class ChiTietLinhKien {
    private PhieuTiepNhanSuaChua phieuSuaChua;
    private LinhKien linhKien;
    private int soLuong;
    private double donGiaXuat;
    private String ghiChu;

    public ChiTietLinhKien() {
        soLuong = 1;
    }
    public ChiTietLinhKien(PhieuTiepNhanSuaChua phieuSuaChua) {
    	setPhieuSuaChua(phieuSuaChua);
    }
    public ChiTietLinhKien(PhieuTiepNhanSuaChua phieuSuaChua, LinhKien linhKien, int soLuong, double donGiaXuat, String ghiChu) {
        setPhieuSuaChua(phieuSuaChua);
        setLinhKien(linhKien);
        setSoLuong(soLuong);
        setDonGiaXuat(donGiaXuat);
        this.ghiChu = ghiChu;
    }

    public PhieuTiepNhanSuaChua getPhieuSuaChua() {
    	return phieuSuaChua;
    }
    public void setPhieuSuaChua(PhieuTiepNhanSuaChua phieuSuaChua) {
        if (phieuSuaChua == null) throw new IllegalArgumentException("Chi tiết linh kiện phải thuộc phiếu sửa chữa.");
    	this.phieuSuaChua = phieuSuaChua;
    }
    public LinhKien getLinhKien() {
    	return linhKien;
    }
    public void setLinhKien(LinhKien linhKien) {
        if (linhKien == null) throw new IllegalArgumentException("Phải chọn linh kiện.");
    	this.linhKien = linhKien;
    }
    public int getSoLuong() {
    	return soLuong;
    }
    public void setSoLuong(int soLuong) {
        if (soLuong <= 0) throw new IllegalArgumentException("Số lượng phải lớn hơn 0.");
        if (linhKien != null && soLuong > linhKien.getSoLuongTon()) throw new IllegalArgumentException("Số lượng sử dụng vượt quá tồn kho.");
    	this.soLuong = soLuong;
    }
    public double getDonGiaXuat() {
    	return donGiaXuat;
    }
    public void setDonGiaXuat(double donGiaXuat) {
        if (!Double.isFinite(donGiaXuat) || donGiaXuat < 0) throw new IllegalArgumentException("Đơn giá xuất không được âm.");
    	this.donGiaXuat = donGiaXuat;
    }

    public double tinhThanhTien() {
        return soLuong * donGiaXuat;
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
        return Objects.equals(phieuSuaChua, ((ChiTietLinhKien) obj).phieuSuaChua);
    }
    @Override public String toString() {
    	return "ChiTietLinhKien [" + "phieuSuaChua=" + phieuSuaChua + ", " + "linhKien=" + linhKien + ", " + "soLuong=" + soLuong + ", " + "donGiaXuat=" + donGiaXuat + ", " + "ghiChu=" + ghiChu + "]";
    }
}
