package entity;

import java.util.Objects;

/**
 * Dòng linh kiện đã dùng của một phiếu sửa chữa. Bảng ChiTietLinhKien (V1), khóa chính (maPhieu, maLK).
 * Trigger trg_CTLK_KiemTra trong DB tự trừ tồn kho và chặn dùng vượt tồn (mã lỗi 50123): Java KHÔNG tự trừ tồn.
 * Cột thanhTien trong DB là cột tính toán, không ánh xạ; dùng {@link #tinhThanhTien()}.
 */
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
    	this();
    	setPhieuSuaChua(phieuSuaChua);
    }
    public ChiTietLinhKien(PhieuTiepNhanSuaChua phieuSuaChua, LinhKien linhKien, int soLuong, double donGiaXuat, String ghiChu) {
        setPhieuSuaChua(phieuSuaChua);
        setLinhKien(linhKien);
        setSoLuong(soLuong);
        setDonGiaXuat(donGiaXuat);
        setGhiChu(ghiChu);
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
    /**
     * Kiểm tra sớm với tồn kho hiện tại để báo lỗi thân thiện. Chỉ đúng khi THÊM dòng mới; khi sửa số lượng
     * của dòng đã lưu thì tồn kho đã bị trừ rồi, trigger trong DB mới là nơi quyết định (mã 50123).
     */
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

    /** Thuộc tính suy diễn /thanhTien = soLuong * donGiaXuat (trùng cột tính toán trong DB). */
    public double tinhThanhTien() {
        return soLuong * donGiaXuat;
    }
    public String getGhiChu() {
    	return ghiChu;
    }
    public void setGhiChu(String ghiChu) {
        if (ghiChu != null && ghiChu.length() > 300) throw new IllegalArgumentException("Ghi chú tối đa 300 ký tự.");
    	this.ghiChu = ghiChu;
    }

    @Override public int hashCode() {
    	return Objects.hash(phieuSuaChua, linhKien);
    }
    @Override public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ChiTietLinhKien o = (ChiTietLinhKien) obj;
        return Objects.equals(phieuSuaChua, o.phieuSuaChua) && Objects.equals(linhKien, o.linhKien);
    }
    @Override public String toString() {
    	return "ChiTietLinhKien [" + "phieuSuaChua=" + (phieuSuaChua == null ? null : phieuSuaChua.getMaPhieu()) + ", " + "linhKien=" + linhKien + ", " + "soLuong=" + soLuong + ", " + "donGiaXuat=" + donGiaXuat + ", " + "ghiChu=" + ghiChu + "]";
    }
}
