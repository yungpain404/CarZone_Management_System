package entity;

import java.util.Objects;
import enums.TrangThaiPhieu;
import enums.VaiTro;

public class PhieuTiepNhanSuaChua {
    private String maPhieu;
    private NhanVien nhanVienLap;
    private NhanVien nhanVienSuaChua;
    private java.util.Date ngayGioTiepNhan;
    private String yeuCauBanDau;
    private TrangThaiPhieu trangThai;

    public PhieuTiepNhanSuaChua() {
    }
    public PhieuTiepNhanSuaChua(String maPhieu) {
    	setMaPhieu(maPhieu);
    }
    public PhieuTiepNhanSuaChua(String maPhieu, NhanVien nhanVienLap, NhanVien nhanVienSuaChua, java.util.Date ngayGioTiepNhan, String yeuCauBanDau, TrangThaiPhieu trangThai) {
        setMaPhieu(maPhieu);
        setNhanVienLap(nhanVienLap);
        setNhanVienSuaChua(nhanVienSuaChua);
        setNgayGioTiepNhan(ngayGioTiepNhan);
        setYeuCauBanDau(yeuCauBanDau);
        setTrangThai(trangThai);
    }

    public String getMaPhieu() {
    	return maPhieu;
    }
    public void setMaPhieu(String maPhieu) {
        if (maPhieu == null || !maPhieu.matches("^PSC\\d{8}[AP]\\d{3}$")) throw new IllegalArgumentException("Mã phiếu sửa chữa không đúng định dạng.");
    	this.maPhieu = maPhieu;
    }
    public NhanVien getNhanVienLap() {
    	return nhanVienLap;
    }
    public void setNhanVienLap(NhanVien nhanVienLap) {
        if (nhanVienLap == null || nhanVienLap.getVaiTro() != VaiTro.TIEP_NHAN_VIEN) throw new IllegalArgumentException("Nhân viên lập phiếu phải là tiếp nhận viên.");
    	this.nhanVienLap = nhanVienLap;
    }
    public NhanVien getNhanVienSuaChua() {
    	return nhanVienSuaChua;
    }
    public void setNhanVienSuaChua(NhanVien nhanVienSuaChua) {
        if (nhanVienSuaChua == null || nhanVienSuaChua.getVaiTro() != VaiTro.KY_THUAT_VIEN) throw new IllegalArgumentException("Nhân viên sửa chữa phải là kỹ thuật viên.");
    	this.nhanVienSuaChua = nhanVienSuaChua;
    }
    public java.util.Date getNgayGioTiepNhan() {
    	return ngayGioTiepNhan;
    }
    public void setNgayGioTiepNhan(java.util.Date ngayGioTiepNhan) {
        if (ngayGioTiepNhan == null || ngayGioTiepNhan.after(new java.util.Date())) throw new IllegalArgumentException("Ngày giờ tiếp nhận không hợp lệ.");
    	this.ngayGioTiepNhan = ngayGioTiepNhan;
    }
    public String getYeuCauBanDau() {
    	return yeuCauBanDau;
    }
    public void setYeuCauBanDau(String yeuCauBanDau) {
        if (yeuCauBanDau == null || yeuCauBanDau.isBlank()) throw new IllegalArgumentException("Yêu cầu ban đầu không được để trống.");
    	this.yeuCauBanDau = yeuCauBanDau;
    }
    public TrangThaiPhieu getTrangThai() {
    	return trangThai;
    }
    public void setTrangThai(TrangThaiPhieu trangThai) {
        if (trangThai == null) throw new IllegalArgumentException("Trạng thái phiếu không được để trống.");
        boolean hopLe = this.trangThai == null
                ? trangThai == TrangThaiPhieu.CHO_SUA
                : this.trangThai == TrangThaiPhieu.CHO_SUA
                    ? trangThai == TrangThaiPhieu.DANG_SUA
                    : this.trangThai == TrangThaiPhieu.DANG_SUA
                        ? trangThai == TrangThaiPhieu.CHO_LINH_KIEN || trangThai == TrangThaiPhieu.HOAN_TAT
                        : this.trangThai == TrangThaiPhieu.CHO_LINH_KIEN && trangThai == TrangThaiPhieu.DANG_SUA;
        if (!hopLe) throw new IllegalArgumentException("Chuyển trạng thái phiếu không đúng quy trình.");
    	this.trangThai = trangThai;
    }

    @Override public int hashCode() {
    	return Objects.hash(maPhieu);
    }
    @Override public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        return Objects.equals(maPhieu, ((PhieuTiepNhanSuaChua) obj).maPhieu);
    }
    @Override public String toString() {
    	return "PhieuTiepNhanSuaChua [" + "maPhieu=" + maPhieu + ", " + "nhanVienLap=" + nhanVienLap + ", " + "nhanVienSuaChua=" + nhanVienSuaChua + ", " + "ngayGioTiepNhan=" + ngayGioTiepNhan + ", " + "yeuCauBanDau=" + yeuCauBanDau + ", " + "trangThai=" + trangThai + "]";
    }
}
