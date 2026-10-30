package entity;

import java.util.Objects;

import enums.HuongXuLyDoiTra;

/**
 * Dòng chi tiết của phiếu đổi trả. Bảng ChiTietDoiTra (V1), khóa chính (maPhieuDT, stt).
 * Mỗi dòng trỏ tới ĐÚNG MỘT dòng dịch vụ HOẶC một dòng linh kiện của phiếu sửa chữa gốc
 * (DB: CK_ChiTietDoiTra_MotTrongHai, khóa ngoại ghép (maPhieu, maDV) / (maPhieu, maLK)).
 * Các cột maPhieu, maDV, maLK được giữ dạng chuỗi (không tạo quan hệ tới ChiTietDichVu/ChiTietLinhKien) để
 * tránh trùng cột khóa ghép; trigger trg_CTDT_KiemTra trong DB kiểm tra dòng thuộc hóa đơn gốc và số lượng đổi trả.
 */
public class ChiTietDoiTra {
    private PhieuDoiTra phieuDT;
    private int stt;
    /** Mã phiếu sửa chữa gốc chứa dòng dịch vụ/linh kiện được đổi trả. */
    private String maPhieu;
    private String maDV;
    private String maLK;
    private int soLuong;
    private double donGia;
    private HuongXuLyDoiTra huongXuLy;

    public ChiTietDoiTra() {
        soLuong = 1;
    }
    public ChiTietDoiTra(PhieuDoiTra phieuDT) {
    	this();
    	setPhieuDT(phieuDT);
    }
    public ChiTietDoiTra(PhieuDoiTra phieuDT, int stt, int soLuong, double donGia, HuongXuLyDoiTra huongXuLy) {
        setPhieuDT(phieuDT);
        setStt(stt);
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
    /** Số thứ tự dòng trong phiếu, từ 1. */
    public int getStt() {
    	return stt;
    }
    public void setStt(int stt) {
        if (stt <= 0 || stt > Short.MAX_VALUE) throw new IllegalArgumentException("Số thứ tự dòng đổi trả không hợp lệ.");
    	this.stt = stt;
    }
    public String getMaPhieu() {
    	return maPhieu;
    }
    public String getMaDV() {
    	return maDV;
    }
    public String getMaLK() {
    	return maLK;
    }
    /** Chọn dòng dịch vụ của phiếu sửa chữa gốc; tự bỏ chọn linh kiện. */
    public void chonDichVu(String maPhieu, String maDV) {
        if (maPhieu == null || maPhieu.isBlank() || maDV == null || maDV.isBlank()) throw new IllegalArgumentException("Phải chọn dịch vụ cần đổi trả.");
    	this.maPhieu = maPhieu;
    	this.maDV = maDV;
    	this.maLK = null;
    }
    /** Chọn dòng linh kiện của phiếu sửa chữa gốc; tự bỏ chọn dịch vụ. */
    public void chonLinhKien(String maPhieu, String maLK) {
        if (maPhieu == null || maPhieu.isBlank() || maLK == null || maLK.isBlank()) throw new IllegalArgumentException("Phải chọn linh kiện cần đổi trả.");
    	this.maPhieu = maPhieu;
    	this.maLK = maLK;
    	this.maDV = null;
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

    /** Thuộc tính suy diễn: chỉ tính tiền khi hoàn tiền (trùng cột tính toán thanhTien trong DB). */
    public double getThanhTien() {
        return huongXuLy == HuongXuLyDoiTra.HOAN_TIEN ? soLuong * donGia : 0;
    }

    @Override public int hashCode() {
    	return Objects.hash(phieuDT, stt);
    }
    @Override public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ChiTietDoiTra o = (ChiTietDoiTra) obj;
        return stt == o.stt && Objects.equals(phieuDT, o.phieuDT);
    }
    @Override public String toString() {
    	return "ChiTietDoiTra [" + "phieuDT=" + (phieuDT == null ? null : phieuDT.getMaPhieuDT()) + ", " + "stt=" + stt + ", " + "maPhieu=" + maPhieu + ", " + "maDV=" + maDV + ", " + "maLK=" + maLK + ", " + "soLuong=" + soLuong + ", " + "donGia=" + donGia + ", " + "huongXuLy=" + huongXuLy + "]";
    }
}
