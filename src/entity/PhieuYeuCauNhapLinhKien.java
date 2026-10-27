package entity;

import java.util.Date;
import java.util.Objects;

import enums.TrangThaiYeuCau;
import enums.VaiTro;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 * Phiếu yêu cầu nhập linh kiện. Bảng PhieuYeuCauNhapLinhKien (V1). maYeuCau do sp_TaoYeuCauNhapLinhKien sinh
 * (PYC + ddMMyyyy + A/P + 2 số). Khi chuyển sang DA_NHAP_KHO, trigger trong DB tự cộng tồn kho: Java KHÔNG cộng.
 * DB bắt buộc khi trạng thái khác CHO_DUYET: phải có nhanVienXuLy và ngayXuLy (CK_PYCNLK_XuLy).
 */
@Entity
@Table(name = "PhieuYeuCauNhapLinhKien")
public class PhieuYeuCauNhapLinhKien {
    @Id
    @Column(name = "maYeuCau", length = 14)
    private String maYeuCau;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maLK", nullable = false)
    private LinhKien linhKien;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maNVLap", nullable = false)
    private NhanVien nhanVienLap;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maNVXuLy")
    private NhanVien nhanVienXuLy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "ngayTao", nullable = false)
    private Date ngayTao;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "ngayXuLy")
    private Date ngayXuLy;

    @Column(name = "soLuongYeuCau", nullable = false)
    private int soLuongYeuCau;

    @Enumerated(EnumType.STRING)
    @Column(name = "trangThai", nullable = false, length = 15)
    private TrangThaiYeuCau trangThai;

    /** Lý do yêu cầu của kỹ thuật viên (DB: NOT NULL). */
    @Column(name = "ghiChu", nullable = false, length = 300)
    private String ghiChu;

    /** Ghi chú của Chủ cửa hàng khi duyệt/từ chối (ví dụ lý do từ chối). */
    @Column(name = "ghiChuXuLy", length = 300)
    private String ghiChuXuLy;

    public PhieuYeuCauNhapLinhKien() {
    }
    public PhieuYeuCauNhapLinhKien(String maYeuCau) {
    	setMaYeuCau(maYeuCau);
    }
    public PhieuYeuCauNhapLinhKien(String maYeuCau, LinhKien linhKien, NhanVien nhanVienLap, NhanVien nhanVienXuLy, Date ngayTao, int soLuongYeuCau, TrangThaiYeuCau trangThai, String ghiChu) {
        setMaYeuCau(maYeuCau);
        setLinhKien(linhKien);
        setNhanVienLap(nhanVienLap);
        this.trangThai = (trangThai == null) ? TrangThaiYeuCau.CHO_DUYET : trangThai;
        if (nhanVienXuLy != null) setNhanVienXuLy(nhanVienXuLy);
        setNgayTao(ngayTao);
        setSoLuongYeuCau(soLuongYeuCau);
        setGhiChu(ghiChu);
    }

    public String getMaYeuCau() {
    	return maYeuCau;
    }
    public void setMaYeuCau(String maYeuCau) {
        // DB: CK_PYCNLK_Ma = PYC + 8 số + A/P + đúng 2 số
        if (maYeuCau == null || !maYeuCau.matches("^PYC\\d{8}[AP]\\d{2}$")) throw new IllegalArgumentException("Mã yêu cầu nhập linh kiện không đúng định dạng.");
    	this.maYeuCau = maYeuCau;
    }
    public LinhKien getLinhKien() {
    	return linhKien;
    }
    public void setLinhKien(LinhKien linhKien) {
        if (linhKien == null) throw new IllegalArgumentException("Phải chọn linh kiện cần nhập.");
    	this.linhKien = linhKien;
    }
    public NhanVien getNhanVienLap() {
    	return nhanVienLap;
    }
    public void setNhanVienLap(NhanVien nhanVienLap) {
        if (nhanVienLap == null || nhanVienLap.getVaiTro() != VaiTro.KY_THUAT_VIEN) throw new IllegalArgumentException("Người lập yêu cầu phải là kỹ thuật viên.");
    	this.nhanVienLap = nhanVienLap;
    }
    public NhanVien getNhanVienXuLy() {
    	return nhanVienXuLy;
    }
    public void setNhanVienXuLy(NhanVien nhanVienXuLy) {
        if (trangThai == null || trangThai == TrangThaiYeuCau.CHO_DUYET) throw new IllegalStateException("Yêu cầu chưa được xử lý.");
        if (nhanVienXuLy == null || nhanVienXuLy.getVaiTro() != VaiTro.CHU_CUA_HANG) throw new IllegalArgumentException("Người xử lý yêu cầu phải là chủ cửa hàng.");
    	this.nhanVienXuLy = nhanVienXuLy;
    }
    public Date getNgayTao() {
    	return ngayTao;
    }
    public void setNgayTao(Date ngayTao) {
        if (ngayTao == null) throw new IllegalArgumentException("Ngày tạo không hợp lệ.");
    	this.ngayTao = ngayTao;
    }
    public Date getNgayXuLy() {
    	return ngayXuLy;
    }
    public void setNgayXuLy(Date ngayXuLy) {
        if (ngayXuLy == null) throw new IllegalArgumentException("Ngày xử lý không hợp lệ.");
        if (ngayTao != null && ngayXuLy.before(ngayTao)) throw new IllegalArgumentException("Ngày xử lý không được trước ngày tạo.");
    	this.ngayXuLy = ngayXuLy;
    }
    public int getSoLuongYeuCau() {
    	return soLuongYeuCau;
    }
    public void setSoLuongYeuCau(int soLuongYeuCau) {
        if (soLuongYeuCau <= 0) throw new IllegalArgumentException("Số lượng yêu cầu phải lớn hơn 0.");
    	this.soLuongYeuCau = soLuongYeuCau;
    }
    public TrangThaiYeuCau getTrangThai() {
    	return trangThai;
    }
    public void setTrangThai(TrangThaiYeuCau trangThai) {
        if (trangThai == null) throw new IllegalArgumentException("Trạng thái yêu cầu không được để trống.");
        boolean hopLe = this.trangThai == null
                ? trangThai == TrangThaiYeuCau.CHO_DUYET
                : this.trangThai == TrangThaiYeuCau.CHO_DUYET
                    ? trangThai == TrangThaiYeuCau.DA_DUYET || trangThai == TrangThaiYeuCau.TU_CHOI
                    : this.trangThai == TrangThaiYeuCau.DA_DUYET && trangThai == TrangThaiYeuCau.DA_NHAP_KHO;
        if (!hopLe) throw new IllegalArgumentException("Chuyển trạng thái yêu cầu không hợp lệ.");
    	this.trangThai = trangThai;
    }
    public String getGhiChu() {
    	return ghiChu;
    }
    public void setGhiChu(String ghiChu) {
        if (ghiChu == null || ghiChu.isBlank()) throw new IllegalArgumentException("Phải ghi rõ lý do yêu cầu nhập linh kiện.");
        if (ghiChu.length() > 300) throw new IllegalArgumentException("Ghi chú tối đa 300 ký tự.");
    	this.ghiChu = ghiChu;
    }
    public String getGhiChuXuLy() {
    	return ghiChuXuLy;
    }
    public void setGhiChuXuLy(String ghiChuXuLy) {
        if (ghiChuXuLy != null && ghiChuXuLy.length() > 300) throw new IllegalArgumentException("Ghi chú xử lý tối đa 300 ký tự.");
    	this.ghiChuXuLy = ghiChuXuLy;
    }

    @Override public int hashCode() {
    	return Objects.hash(maYeuCau);
    }
    @Override public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        return Objects.equals(maYeuCau, ((PhieuYeuCauNhapLinhKien) obj).maYeuCau);
    }
    @Override public String toString() {
    	return "PhieuYeuCauNhapLinhKien [" + "maYeuCau=" + maYeuCau + ", " + "linhKien=" + linhKien + ", " + "nhanVienLap=" + nhanVienLap + ", " + "nhanVienXuLy=" + nhanVienXuLy + ", " + "ngayTao=" + ngayTao + ", " + "ngayXuLy=" + ngayXuLy + ", " + "soLuongYeuCau=" + soLuongYeuCau + ", " + "trangThai=" + trangThai + ", " + "ghiChu=" + ghiChu + ", " + "ghiChuXuLy=" + ghiChuXuLy + "]";
    }
}
