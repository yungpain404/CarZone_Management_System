package entity;

import java.util.Objects;

/**
 * Dịch vụ sửa chữa. Bảng DichVu (V1). Mã dạng DV + từ 5 chữ số trở lên.
 */
public class DichVu {
    private String maDV;
    private String tenDV;
    private double donGia;
    private String donViTinh;
    private String moTa;
    private boolean dangCungCap;

    public DichVu() {
        dangCungCap = true;
    }
    public DichVu(String maDV) {
    	setMaDV(maDV);
    }
    public DichVu(String maDV, String tenDV, double donGia, String donViTinh, String moTa, boolean dangCungCap) {
        this();
        setMaDV(maDV);
        setTenDV(tenDV);
        setDonGia(donGia);
        setDonViTinh(donViTinh);
        setMoTa(moTa);
        this.dangCungCap = dangCungCap;
    }

    public String getMaDV() {
    	return maDV;
    }
    public void setMaDV(String maDV) {
        if (maDV == null || !maDV.matches("^DV\\d{5,}$")) {
            throw new IllegalArgumentException("Mã dịch vụ phải có dạng DVXXXXX.");
        }
    	this.maDV = maDV;
    }
    public String getTenDV() {
    	return tenDV;
    }
    public void setTenDV(String tenDV) {
        if (tenDV == null || tenDV.isBlank() || tenDV.length() > 100) {
            throw new IllegalArgumentException("Tên dịch vụ không được để trống và tối đa 100 ký tự.");
        }
    	this.tenDV = tenDV;
    }
    public double getDonGia() {
    	return donGia;
    }
    public void setDonGia(double donGia) {
        if (!Double.isFinite(donGia) || donGia <= 0) {
            throw new IllegalArgumentException("Đơn giá dịch vụ phải lớn hơn 0.");
        }
    	this.donGia = donGia;
    }
    public String getDonViTinh() {
    	return donViTinh;
    }
    public void setDonViTinh(String donViTinh) {
        if (donViTinh == null || donViTinh.isBlank()) {
            throw new IllegalArgumentException("Đơn vị tính không được để trống.");
        }
    	this.donViTinh = donViTinh;
    }
    public String getMoTa() {
    	return moTa;
    }
    public void setMoTa(String moTa) {
        if (moTa != null && moTa.length() > 255) {
            throw new IllegalArgumentException("Mô tả không được vượt quá 255 ký tự.");
        }
    	this.moTa = moTa;
    }
    public boolean isDangCungCap() {
    	return dangCungCap;
    }
    public void setDangCungCap(boolean dangCungCap) {
    	this.dangCungCap = dangCungCap;
    }

    @Override public int hashCode() {
    	return Objects.hash(maDV);
    }
    @Override public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        return Objects.equals(maDV, ((DichVu) obj).maDV);
    }
    @Override public String toString() {
    	return "DichVu [" + "maDV=" + maDV + ", " + "tenDV=" + tenDV + ", " + "donGia=" + donGia + ", " + "donViTinh=" + donViTinh + ", " + "moTa=" + moTa + ", " + "dangCungCap=" + dangCungCap + "]";
    }
}
