package entity;

import java.util.Objects;

/**
 * Khách hàng. Bảng KhachHang (V1 + V2). Mã KH có DEFAULT lấy từ seq_KhachHang trong DB.
 */
public class KhachHang {
    private String maKH;
    private String hoTen;
    private String soDienThoai;
    private String ghiChu;
    private String CCCD;
    private boolean thanhVienThanThiet;
    private double diemTichLuy;

    public KhachHang() {
    }
    public KhachHang(String maKH) {
    	setMaKH(maKH);
    }
    public KhachHang(String maKH, String hoTen, String soDienThoai, String ghiChu, String CCCD, boolean thanhVienThanThiet, double diemTichLuy) {
        setMaKH(maKH);
        setHoTen(hoTen);
        setSoDienThoai(soDienThoai);
        setGhiChu(ghiChu);
        setCCCD(CCCD);
        this.thanhVienThanThiet = thanhVienThanThiet;
        setDiemTichLuy(diemTichLuy);
    }

    public String getMaKH() {
    	return maKH;
    }
    public void setMaKH(String maKH) {
        // DB: CK_KhachHang_Ma = 'KH' + đúng 6 chữ số
        if (maKH == null || !maKH.matches("^KH\\d{6}$")) {
            throw new IllegalArgumentException("Mã khách hàng phải có dạng KH000001.");
        }
    	this.maKH = maKH;
    }
    public String getHoTen() {
    	return hoTen;
    }
    public void setHoTen(String hoTen) {
        if (hoTen == null || hoTen.isBlank() || hoTen.length() > 100) {
            throw new IllegalArgumentException("Họ tên không được để trống và tối đa 100 ký tự.");
        }
    	this.hoTen = hoTen;
    }
    public String getSoDienThoai() {
    	return soDienThoai;
    }
    public void setSoDienThoai(String soDienThoai) {
        if (soDienThoai == null || !soDienThoai.matches("0\\d{9}")) {
            throw new IllegalArgumentException("Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0.");
        }
    	this.soDienThoai = soDienThoai;
    }
    public String getGhiChu() {
    	return ghiChu;
    }
    public void setGhiChu(String ghiChu) {
        // DB: NVARCHAR(500)
        if (ghiChu != null && ghiChu.length() > 500) {
            throw new IllegalArgumentException("Ghi chú không được vượt quá 500 ký tự.");
        }
    	this.ghiChu = ghiChu;
    }
    public String getCCCD() {
    	return CCCD;
    }
    public void setCCCD(String CCCD) {
        if (CCCD != null && !CCCD.isBlank() && !CCCD.matches("\\d{12}")) {
            throw new IllegalArgumentException("CCCD phải gồm 12 chữ số.");
        }
        // DB: cột CCCD NULL được và unique có điều kiện; chuỗi rỗng phải lưu thành NULL
        this.CCCD = (CCCD == null || CCCD.isBlank()) ? null : CCCD;
    }
    public boolean isThanhVienThanThiet() {
    	return thanhVienThanThiet;
    }
    public void setThanhVienThanThiet(boolean thanhVienThanThiet) {
    	this.thanhVienThanThiet = thanhVienThanThiet;
    }
    public double getDiemTichLuy() {
    	return diemTichLuy;
    }
    public void setDiemTichLuy(double diemTichLuy) {
        if (!Double.isFinite(diemTichLuy) || diemTichLuy < 0) {
            throw new IllegalArgumentException("Điểm tích lũy không được âm.");
        }
    	this.diemTichLuy = diemTichLuy;
    }

    @Override public int hashCode() {
    	return Objects.hash(maKH);
    }
    @Override public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        return Objects.equals(maKH, ((KhachHang) obj).maKH);
    }
    @Override public String toString() {
    	return "KhachHang [" + "maKH=" + maKH + ", " + "hoTen=" + hoTen + ", " + "soDienThoai=" + soDienThoai + ", " + "ghiChu=" + ghiChu + ", " + "CCCD=" + CCCD + ", " + "thanhVienThanThiet=" + thanhVienThanThiet + ", " + "diemTichLuy=" + diemTichLuy + "]";
    }
}
