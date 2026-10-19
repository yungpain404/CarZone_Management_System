package entity;

import java.util.Objects;
import enums.HuongXuLyDoiTra;

public class ChiTietDoiTra {
    private PhieuDoiTra phieuDT;
    private int soLuong;
    private double donGia;
    private HuongXuLyDoiTra huongXuLy;

    public ChiTietDoiTra() {
        soLuong = 1;
    }
    public ChiTietDoiTra(PhieuDoiTra phieuDT) {
    	setPhieuDT(phieuDT);
    }
    public ChiTietDoiTra(PhieuDoiTra phieuDT, int soLuong, double donGia, HuongXuLyDoiTra huongXuLy) {
        setPhieuDT(phieuDT);
        setSoLuong(soLuong);
        setDonGia(donGia);
        setHuongXuLy(huongXuLy);
    }

    public PhieuDoiTra getPhieuDT() {
    	return phieuDT;
    }
    public void setPhieuDT(PhieuDoiTra phieuDT) {
        if (phieuDT == null) throw new IllegalArgumentException("Chi tiết đổi trả phải gắn với một phiếu đổi trả.");
    	this.phieuDT = phieuDT;
    }
    public int getSoLuong() {
    	return soLuong;
    }
    public void setSoLuong(int soLuong) {
        if (soLuong <= 0) throw new IllegalArgumentException("Số lượng đổi trả phải lớn hơn 0.");
    	this.soLuong = soLuong;
    }
    public double getDonGia() {
    	return donGia;
    }
    public void setDonGia(double donGia) {
        if (!Double.isFinite(donGia) || donGia < 0) throw new IllegalArgumentException("Đơn giá không hợp lệ.");
    	this.donGia = donGia;
    }
    public HuongXuLyDoiTra getHuongXuLy() {
    	return huongXuLy;
    }
    public void setHuongXuLy(HuongXuLyDoiTra huongXuLy) {
        if (huongXuLy == null) throw new IllegalArgumentException("Hướng xử lý đổi trả không hợp lệ.");
    	this.huongXuLy = huongXuLy;
    }

    public double getThanhTien() {
        return huongXuLy == HuongXuLyDoiTra.HOAN_TIEN ? soLuong * donGia : 0;
    }

    @Override public int hashCode() {
    	return Objects.hash(phieuDT);
    }
    @Override public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        return Objects.equals(phieuDT, ((ChiTietDoiTra) obj).phieuDT);
    }
    @Override public String toString() {
    	return "ChiTietDoiTra [" + "phieuDT=" + phieuDT + ", " + "soLuong=" + soLuong + ", " + "donGia=" + donGia + ", " + "huongXuLy=" + huongXuLy + "]";
    }
}
