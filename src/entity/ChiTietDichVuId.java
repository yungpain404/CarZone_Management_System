package entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Khóa chính ghép của ChiTietDichVu: (maPhieu, maDV). */
@Embeddable
public class ChiTietDichVuId implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "maPhieu", length = 15)
    private String maPhieu;

    @Column(name = "maDV", length = 10)
    private String maDV;

    public ChiTietDichVuId() {
    }
    public ChiTietDichVuId(String maPhieu, String maDV) {
        this.maPhieu = maPhieu;
        this.maDV = maDV;
    }

    public String getMaPhieu() {
        return maPhieu;
    }
    public void setMaPhieu(String maPhieu) {
        this.maPhieu = maPhieu;
    }
    public String getMaDV() {
        return maDV;
    }
    public void setMaDV(String maDV) {
        this.maDV = maDV;
    }

    @Override public int hashCode() {
        return Objects.hash(maPhieu, maDV);
    }
    @Override public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ChiTietDichVuId o = (ChiTietDichVuId) obj;
        return Objects.equals(maPhieu, o.maPhieu) && Objects.equals(maDV, o.maDV);
    }
}
