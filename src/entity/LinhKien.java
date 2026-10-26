package entity;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Linh kiện. Bảng LinhKien (V1). Mã dạng LK + từ 5 chữ số trở lên.
 * soLuongTon do trigger trong DB tự trừ/cộng (dùng linh kiện, nhập kho): Java KHÔNG tự cộng trừ.
 */
@Entity
@Table(name = "LinhKien")
public class LinhKien {
    @Id
    @Column(name = "maLK", length = 10)
    private String maLK;

    @Column(name = "tenLK", nullable = false, length = 150)
    private String tenLK;

    @Column(name = "donGia", nullable = false)
    private double donGia;

    @Column(name = "donViTinh", nullable = false, length = 30)
    private String donViTinh;

    @Column(name = "soLuongTon", nullable = false)
    private int soLuongTon;

    @Column(name = "dangKinhDoanh", nullable = false)
    private boolean dangKinhDoanh;

    public LinhKien() {
        dangKinhDoanh = true;
    }
    public LinhKien(String maLK) {
    	setMaLK(maLK);
    }
    public LinhKien(String maLK, String tenLK, double donGia, String donViTinh, int soLuongTon, boolean dangKinhDoanh) {
        this();
        setMaLK(maLK);
        setTenLK(tenLK);
        setDonGia(donGia);
        setDonViTinh(donViTinh);
        setSoLuongTon(soLuongTon);
        this.dangKinhDoanh = dangKinhDoanh;
    }

    public String getMaLK() {
    	return maLK;
    }
    public void setMaLK(String maLK) {
        if (maLK == null || !maLK.matches("^LK\\d{5,}$")) {
            throw new IllegalArgumentException("Mã linh kiện phải có dạng LKXXXXX.");
        }
    	this.maLK = maLK;
    }
    public String getTenLK() {
    	return tenLK;
    }
    public void setTenLK(String tenLK) {
        if (tenLK == null || tenLK.isBlank() || tenLK.length() > 100) {
            throw new IllegalArgumentException("Tên linh kiện không được để trống và tối đa 100 ký tự.");
        }
    	this.tenLK = tenLK;
    }
    public double getDonGia() {
    	return donGia;
    }
    public void setDonGia(double donGia) {
        if (!Double.isFinite(donGia) || donGia <= 0) {
            throw new IllegalArgumentException("Đơn giá phải lớn hơn 0.");
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
    public int getSoLuongTon() {
    	return soLuongTon;
    }
    public void setSoLuongTon(int soLuongTon) {
        if (soLuongTon < 0) {
            throw new IllegalArgumentException("Số lượng tồn không được âm.");
        }
    	this.soLuongTon = soLuongTon;
    }
    public boolean isDangKinhDoanh() {
    	return dangKinhDoanh;
    }
    public void setDangKinhDoanh(boolean dangKinhDoanh) {
    	this.dangKinhDoanh = dangKinhDoanh;
    }

    @Override public int hashCode() {
    	return Objects.hash(maLK);
    }
    @Override public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        return Objects.equals(maLK, ((LinhKien) obj).maLK);
    }
    @Override public String toString() {
    	return "LinhKien [" + "maLK=" + maLK + ", " + "tenLK=" + tenLK + ", " + "donGia=" + donGia + ", " + "donViTinh=" + donViTinh + ", " + "soLuongTon=" + soLuongTon + ", " + "dangKinhDoanh=" + dangKinhDoanh + "]";
    }
}
