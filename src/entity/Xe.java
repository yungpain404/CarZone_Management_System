package entity;

import java.time.Year;
import java.util.Objects;

/**
 * Xe của khách hàng. Bảng Xe (V1). Khóa chính là biển số xe.
 */
public class Xe {
    private String bienSoXe;
    private String hangXe;
    private String model;
    private String mauSac;
    private int namSanXuat;
    private int soKmHienTai;
    private KhachHang chuXe;

    public Xe() {
    }
    public Xe(String bienSoXe) {
    	setBienSoXe(bienSoXe);
    }
    public Xe(String bienSoXe, String hangXe, String model, String mauSac, int namSanXuat, int soKmHienTai, KhachHang chuXe) {
        setBienSoXe(bienSoXe);
        setHangXe(hangXe);
        setModel(model);
        setMauSac(mauSac);
        setNamSanXuat(namSanXuat);
        setSoKmHienTai(soKmHienTai);
        setChuXe(chuXe);
    }

    public String getBienSoXe() {
    	return bienSoXe;
    }
    public void setBienSoXe(String bienSoXe) {
        if (bienSoXe == null || bienSoXe.isBlank()) {
            throw new IllegalArgumentException("Biển số xe không được để trống.");
        }
        // DB: CK_Xe_BienSo = độ dài 6..15
        if (bienSoXe.length() < 6 || bienSoXe.length() > 15) {
            throw new IllegalArgumentException("Biển số xe phải từ 6 đến 15 ký tự.");
        }
    	this.bienSoXe = bienSoXe;
    }
    public String getHangXe() {
    	return hangXe;
    }
    public void setHangXe(String hangXe) {
        if (hangXe == null || hangXe.isBlank()) {
            throw new IllegalArgumentException("Hãng xe không được để trống.");
        }
    	this.hangXe = hangXe;
    }
    public String getModel() {
    	return model;
    }
    public void setModel(String model) {
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("Model xe không được để trống.");
        }
    	this.model = model;
    }
    public String getMauSac() {
    	return mauSac;
    }
    public void setMauSac(String mauSac) {
        if (mauSac == null || mauSac.isBlank()) {
            throw new IllegalArgumentException("Màu xe không được để trống.");
        }
    	this.mauSac = mauSac;
    }
    public int getNamSanXuat() {
    	return namSanXuat;
    }
    public void setNamSanXuat(int namSanXuat) {
        // DB: CK_Xe_NamSX = 1900..2100
        if (namSanXuat < 1900 || namSanXuat > Year.now().getValue()) {
            throw new IllegalArgumentException("Năm sản xuất không hợp lệ.");
        }
    	this.namSanXuat = namSanXuat;
    }
    public int getSoKmHienTai() {
    	return soKmHienTai;
    }
    public void setSoKmHienTai(int soKmHienTai) {
        if (soKmHienTai < 0 || soKmHienTai < this.soKmHienTai) {
            throw new IllegalArgumentException("Số km không được âm hoặc nhỏ hơn số km đã ghi nhận.");
        }
    	this.soKmHienTai = soKmHienTai;
    }
    public KhachHang getChuXe() {
    	return chuXe;
    }
    public void setChuXe(KhachHang chuXe) {
        if (chuXe == null) {
            throw new IllegalArgumentException("Xe phải có chủ sở hữu.");
        }
    	this.chuXe = chuXe;
    }

    @Override public int hashCode() {
    	return Objects.hash(bienSoXe);
    }
    @Override public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        return Objects.equals(bienSoXe, ((Xe) obj).bienSoXe);
    }
    @Override public String toString() {
    	return "Xe [" + "bienSoXe=" + bienSoXe + ", " + "hangXe=" + hangXe + ", " + "model=" + model + ", " + "mauSac=" + mauSac + ", " + "namSanXuat=" + namSanXuat + ", " + "soKmHienTai=" + soKmHienTai + ", " + "chuXe=" + chuXe + "]";
    }
}
