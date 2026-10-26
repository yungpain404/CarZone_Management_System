package entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Khóa chính ghép của ChiTietDoiTra: (maPhieuDT, stt). stt là SMALLINT, bắt đầu từ 1. */
@Embeddable
public class ChiTietDoiTraId implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "maPhieuDT", length = 14)
    private String maPhieuDT;

    @Column(name = "stt")
    private short stt;

    public ChiTietDoiTraId() {
    }
    public ChiTietDoiTraId(String maPhieuDT, short stt) {
        this.maPhieuDT = maPhieuDT;
        this.stt = stt;
    }

    public String getMaPhieuDT() {
        return maPhieuDT;
    }
    public void setMaPhieuDT(String maPhieuDT) {
        this.maPhieuDT = maPhieuDT;
    }
    public short getStt() {
        return stt;
    }
    public void setStt(short stt) {
        this.stt = stt;
    }

    @Override public int hashCode() {
        return Objects.hash(maPhieuDT, stt);
    }
    @Override public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ChiTietDoiTraId o = (ChiTietDoiTraId) obj;
        return stt == o.stt && Objects.equals(maPhieuDT, o.maPhieuDT);
    }
}
