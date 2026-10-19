package entity;

import java.util.Date;
import java.util.Objects;
import enums.PhuongThucThanhToan;
import enums.TrangThaiThanhToan;
import enums.VaiTro;

/** Entity representing a garage invoice. */
public class HoaDon {
	private String maHoaDon;
	private NhanVien nhanVien;
	private KhuyenMai khuyenMai;
	private Date ngayLap;
	private PhuongThucThanhToan phuongThucTT;
	private TrangThaiThanhToan trangThaiThanhToan;
	private Date thoiGianThanhToan;
	private String maGiaoDich;
	private double tongTienDichVu;
	private double tongTienLinhKien;
	private double thue;
	private double tongThanhToan;

	public HoaDon() {
		trangThaiThanhToan = TrangThaiThanhToan.CHUA_THANH_TOAN;
	}

	public HoaDon(String maHoaDon) {
		this.maHoaDon = maHoaDon;
	}

	public HoaDon(String maHoaDon, NhanVien nhanVien, KhuyenMai khuyenMai, Date ngayLap,
			PhuongThucThanhToan phuongThucTT, TrangThaiThanhToan trangThaiThanhToan,
			Date thoiGianThanhToan, String maGiaoDich, double tongTienDichVu,
			double tongTienLinhKien, double thue, double tongThanhToan) {
		this();
		setMaHoaDon(maHoaDon);
		setNhanVien(nhanVien);
		setNgayLap(ngayLap);
		setPhuongThucTT(phuongThucTT);
		if (khuyenMai != null) setKhuyenMai(khuyenMai);
		if (trangThaiThanhToan != null) setTrangThaiThanhToan(trangThaiThanhToan);
		if (thoiGianThanhToan != null) setThoiGianThanhToan(thoiGianThanhToan);
		setMaGiaoDich(maGiaoDich);
		setTongTienDichVu(tongTienDichVu);
		setTongTienLinhKien(tongTienLinhKien);
		setThue(thue);
		setTongThanhToan(tongThanhToan);
	}

	public String getMaHoaDon() {
		return maHoaDon;
	}
	public void setMaHoaDon(String maHoaDon) {
		if (maHoaDon == null || !maHoaDon.matches("^HD\\d{8}[AP]\\d{3}$")) throw new IllegalArgumentException("Mã hóa đơn không đúng định dạng.");
		this.maHoaDon = maHoaDon;
	}
	public NhanVien getNhanVien() {
		return nhanVien;
	}
	public void setNhanVien(NhanVien nhanVien) {
		if (nhanVien == null || nhanVien.getVaiTro() != VaiTro.TIEP_NHAN_VIEN) throw new IllegalArgumentException("Nhân viên lập hóa đơn không hợp lệ.");
		this.nhanVien = nhanVien;
	}
	public KhuyenMai getKhuyenMai() {
		return khuyenMai;
	}
	public void setKhuyenMai(KhuyenMai khuyenMai) {
		if (khuyenMai != null && (ngayLap == null || khuyenMai.getNgayBatDau() == null || khuyenMai.getNgayKetThuc() == null
				|| ngayLap.before(khuyenMai.getNgayBatDau()) || ngayLap.after(khuyenMai.getNgayKetThuc()))) {
			throw new IllegalArgumentException("Khuyến mãi không còn hiệu lực áp dụng.");
		}
		this.khuyenMai = khuyenMai;
	}
	public Date getNgayLap() {
		return ngayLap;
	}
	public void setNgayLap(Date ngayLap) {
		if (ngayLap == null || ngayLap.after(new Date())) throw new IllegalArgumentException("Ngày lập hóa đơn không hợp lệ.");
		if (khuyenMai != null && (khuyenMai.getNgayBatDau() == null || khuyenMai.getNgayKetThuc() == null
				|| ngayLap.before(khuyenMai.getNgayBatDau()) || ngayLap.after(khuyenMai.getNgayKetThuc()))) {
			throw new IllegalArgumentException("Khuyến mãi không còn hiệu lực áp dụng.");
		}
		this.ngayLap = ngayLap;
	}
	public PhuongThucThanhToan getPhuongThucTT() {
		return phuongThucTT;
	}
	public void setPhuongThucTT(PhuongThucThanhToan phuongThucTT) {
		if (phuongThucTT == null) throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ.");
		this.phuongThucTT = phuongThucTT;
	}
	public TrangThaiThanhToan getTrangThaiThanhToan() {
		return trangThaiThanhToan;
	}
	public void setTrangThaiThanhToan(TrangThaiThanhToan trangThaiThanhToan) {
		if (trangThaiThanhToan == null) throw new IllegalArgumentException("Trạng thái thanh toán không hợp lệ.");
		if (this.trangThaiThanhToan == TrangThaiThanhToan.DA_THANH_TOAN
				&& trangThaiThanhToan != TrangThaiThanhToan.DA_THANH_TOAN) {
			throw new IllegalStateException("Không thể hủy trạng thái đã thanh toán.");
		}
		this.trangThaiThanhToan = trangThaiThanhToan;
	}
	public Date getThoiGianThanhToan() {
		return thoiGianThanhToan;
	}
	public void setThoiGianThanhToan(Date thoiGianThanhToan) {
		if (trangThaiThanhToan != TrangThaiThanhToan.DA_THANH_TOAN || thoiGianThanhToan == null) {
			throw new IllegalStateException("Hóa đơn chưa thanh toán hoặc thời gian thanh toán không hợp lệ.");
		}
		this.thoiGianThanhToan = thoiGianThanhToan;
	}
	public String getMaGiaoDich() {
		return maGiaoDich;
	}
	public void setMaGiaoDich(String maGiaoDich) {
		if (phuongThucTT == PhuongThucThanhToan.CHUYEN_KHOAN && (maGiaoDich == null || maGiaoDich.isBlank())) {
			throw new IllegalArgumentException("Thiếu mã giao dịch chuyển khoản.");
		}
		this.maGiaoDich = maGiaoDich;
	}
	public double getTongTienDichVu() {
		return tongTienDichVu;
	}
	public void setTongTienDichVu(double tongTienDichVu) {
		if (!Double.isFinite(tongTienDichVu) || tongTienDichVu < 0) throw new IllegalArgumentException("Tổng tiền dịch vụ không hợp lệ.");
		this.tongTienDichVu = tongTienDichVu;
	}
	public double getTongTienLinhKien() {
		return tongTienLinhKien;
	}
	public void setTongTienLinhKien(double tongTienLinhKien) {
		if (!Double.isFinite(tongTienLinhKien) || tongTienLinhKien < 0) throw new IllegalArgumentException("Tổng tiền linh kiện không hợp lệ.");
		this.tongTienLinhKien = tongTienLinhKien;
	}
	public double getThue() {
		return thue;
	}
	public void setThue(double thue) {
		if (!Double.isFinite(thue) || thue < 0) throw new IllegalArgumentException("Tiền thuế không hợp lệ.");
		this.thue = thue;
	}
	public double getTongThanhToan() {
		return tongThanhToan;
	}
	public void setTongThanhToan(double tongThanhToan) {
		if (!Double.isFinite(tongThanhToan) || tongThanhToan < 0) throw new IllegalArgumentException("Tổng thanh toán không hợp lệ.");
		this.tongThanhToan = tongThanhToan;
	}

	@Override public int hashCode() {
		return Objects.hash(maHoaDon);
	}
	@Override public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		return Objects.equals(maHoaDon, ((HoaDon) obj).maHoaDon);
	}
	@Override public String toString() {
		return "HoaDon [maHoaDon=" + maHoaDon + ", nhanVien=" + nhanVien + ", khuyenMai=" + khuyenMai
				+ ", ngayLap=" + ngayLap + ", phuongThucTT=" + phuongThucTT + ", trangThaiThanhToan="
				+ trangThaiThanhToan + ", thoiGianThanhToan=" + thoiGianThanhToan + ", maGiaoDich="
				+ maGiaoDich + ", tongTienDichVu=" + tongTienDichVu + ", tongTienLinhKien="
				+ tongTienLinhKien + ", thue=" + thue + ", tongThanhToan=" + tongThanhToan + "]";
	}
}
