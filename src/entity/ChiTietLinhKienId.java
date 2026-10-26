package entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Khóa chính ghép của ChiTietLinhKien: (maPhieu, maLK). */
@Embeddable
public class ChiTietLinhKienId implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "maPhieu", length = 15)
    private String maPhieu;

    @Column(name = "maLK", length = 10)
    private String maLK;

    public ChiTietLinhKienId() {
    }
    public ChiTietLinhKienId(String maPhieu, String maLK) {
        this.maPhieu = maPhieu;
        this.maLK = maLK;
    }

    public String getMaPhieu() {
        return maPhieu;
    }
    public void setMaPhieu(String maPhieu) {
        this.maPhieu = maPhieu;
    }
    public String getMaLK() {
        return maLK;
    }
    public void setMaLK(String maLK) {
        this.maLK = maLK;
    }

    @Override public int hashCode() {
        return Objects.hash(maPhieu, maLK);
    }
    @Override public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ChiTietLinhKienId o = (ChiTietLinhKienId) obj;
        return Objects.equals(maPhieu, o.maPhieu) && Objects.equals(maLK, o.maLK);
    }
}
