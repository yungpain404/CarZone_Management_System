package entity;

import java.util.Objects;
import enums.TrangThaiYeuCau;
import enums.VaiTro;

public class PhieuYeuCauNhapLinhKien {
    private String maYeuCau;
    private LinhKien linhKien;
    private NhanVien nhanVienLap;
    private NhanVien nhanVienXuLy;
    private java.util.Date ngayTao;
    private int soLuongYeuCau;
    private TrangThaiYeuCau trangThai;
    private String ghiChu;

    public PhieuYeuCauNhapLinhKien() {
    }
    public PhieuYeuCauNhapLinhKien(String maYeuCau) {
    	setMaYeuCau(maYeuCau);
    }
    public PhieuYeuCauNhapLinhKien(String maYeuCau, LinhKien linhKien, NhanVien nhanVienLap, NhanVien nhanVienXuLy, java.util.Date ngayTao, int soLuongYeuCau, TrangThaiYeuCau trangThai, String ghiChu) {
        setMaYeuCau(maYeuCau);
        setLinhKien(linhKien);
        setNhanVienLap(nhanVienLap);
        this.trangThai = TrangThaiYeuCau.CHO_DUYET;
        if (nhanVienXuLy != null) setNhanVienXuLy(nhanVienXuLy);
        setNgayTao(ngayTao);
        setSoLuongYeuCau(soLuongYeuCau);
        if (trangThai != null) setTrangThai(trangThai);
        this.ghiChu = ghiChu;
    }

    public String getMaYeuCau() {
    	return maYeuCau;
    }
    public void setMaYeuCau(String maYeuCau) {
        if (maYeuCau == null || !maYeuCau.matches("^PYC\\d{8}[AP]\\d{2,}$")) throw new IllegalArgumentException("Mã yêu cầu nhập linh kiện không đúng định dạng.");
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
        if (trangThai == TrangThaiYeuCau.CHO_DUYET) throw new IllegalStateException("Yêu cầu chưa được xử lý.");
        if (nhanVienXuLy == null || nhanVienXuLy.getVaiTro() != VaiTro.CHU_CUA_HANG) throw new IllegalArgumentException("Người xử lý yêu cầu phải là chủ cửa hàng.");
    	this.nhanVienXuLy = nhanVienXuLy;
    }
    public java.util.Date getNgayTao() {
    	return ngayTao;
    }
    public void setNgayTao(java.util.Date ngayTao) {
        if (ngayTao == null) throw new IllegalArgumentException("Ngày tạo không hợp lệ.");
    	this.ngayTao = ngayTao;
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
    	this.ghiChu = ghiChu;
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
    	return "PhieuYeuCauNhapLinhKien [" + "maYeuCau=" + maYeuCau + ", " + "linhKien=" + linhKien + ", " + "nhanVienLap=" + nhanVienLap + ", " + "nhanVienXuLy=" + nhanVienXuLy + ", " + "ngayTao=" + ngayTao + ", " + "soLuongYeuCau=" + soLuongYeuCau + ", " + "trangThai=" + trangThai + ", " + "ghiChu=" + ghiChu + "]";
    }
}
