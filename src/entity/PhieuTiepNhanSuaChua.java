package entity;

import java.util.Date;
import java.util.Objects;

import enums.TrangThaiPhieu;
import enums.VaiTro;

/**
 * Phiếu tiếp nhận - sửa chữa. Bảng PhieuTiepNhanSuaChua (V1). maPhieu do sp_LapPhieuTiepNhan sinh
 * (PSC + ddMMyyyy + A/P + 3 số). Phiếu HOAN_TAT bị khóa ở DB (trigger).
 */
public class PhieuTiepNhanSuaChua {
    private String maPhieu;

    private Xe xe;

    private NhanVien nhanVienLap;

    /** Kỹ thuật viên chính. DB cho phép NULL khi phiếu còn CHO_SUA, bắt buộc có khi sang DANG_SUA. */
    private NhanVien nhanVienSuaChua;

    private Date ngayGioTiepNhan;

    private String yeuCauBanDau;

    private TrangThaiPhieu trangThai;

    public PhieuTiepNhanSuaChua() {
    }
    public PhieuTiepNhanSuaChua(String maPhieu) {
    	setMaPhieu(maPhieu);
    }
    public PhieuTiepNhanSuaChua(String maPhieu, Xe xe, NhanVien nhanVienLap, NhanVien nhanVienSuaChua, Date ngayGioTiepNhan, String yeuCauBanDau, TrangThaiPhieu trangThai) {
        setMaPhieu(maPhieu);
        setXe(xe);
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
    public Xe getXe() {
    	return xe;
    }
    public void setXe(Xe xe) {
        if (xe == null) throw new IllegalArgumentException("Phiếu sửa chữa phải gắn với một xe.");
    	this.xe = xe;
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
    public Date getNgayGioTiepNhan() {
    	return ngayGioTiepNhan;
    }
    public void setNgayGioTiepNhan(Date ngayGioTiepNhan) {
        if (ngayGioTiepNhan == null || ngayGioTiepNhan.after(new Date())) throw new IllegalArgumentException("Ngày giờ tiếp nhận không hợp lệ.");
    	this.ngayGioTiepNhan = ngayGioTiepNhan;
    }
    public String getYeuCauBanDau() {
    	return yeuCauBanDau;
    }
    public void setYeuCauBanDau(String yeuCauBanDau) {
        if (yeuCauBanDau == null || yeuCauBanDau.isBlank()) throw new IllegalArgumentException("Yêu cầu ban đầu không được để trống.");
        if (yeuCauBanDau.length() > 500) throw new IllegalArgumentException("Yêu cầu ban đầu tối đa 500 ký tự.");
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
        // DB: CK_PTNSC_KTVChinh = ngoài CHO_SUA thì phải có kỹ thuật viên chính
        if (trangThai == TrangThaiPhieu.DANG_SUA && nhanVienSuaChua == null) {
            throw new IllegalStateException("Phiếu phải có kỹ thuật viên chính trước khi chuyển sang Đang sửa.");
        }
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
    	return "PhieuTiepNhanSuaChua [" + "maPhieu=" + maPhieu + ", " + "xe=" + (xe == null ? null : xe.getBienSoXe()) + ", " + "nhanVienLap=" + nhanVienLap + ", " + "nhanVienSuaChua=" + nhanVienSuaChua + ", " + "ngayGioTiepNhan=" + ngayGioTiepNhan + ", " + "yeuCauBanDau=" + yeuCauBanDau + ", " + "trangThai=" + trangThai + "]";
    }
}
