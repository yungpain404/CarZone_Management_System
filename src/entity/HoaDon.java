package entity;

import java.util.Date;
import java.util.Objects;

import enums.PhuongThucThanhToan;
import enums.TrangThaiPhieu;
import enums.TrangThaiThanhToan;
import enums.VaiTro;

/**
 * Hóa đơn. Bảng HoaDon (V1). maHoaDon do sp_LapHoaDon sinh (HD + ddMMyyyy + A/P + 3 số);
 * thanh toán qua sp_ThanhToanHoaDon. Hóa đơn đã thanh toán bị khóa ở DB (trigger).
 *
 * Các số tiền (tongTienDichVu, tongTienLinhKien, tienGiam, thue, tongThanhToan) là thuộc tính SUY DIỄN:
 * chỉ có getter, được Hibernate đọc từ view vw_HoaDon_TongTien mỗi lần nạp hóa đơn từ DB ( * để công thức thuế/khuyến mãi chỉ tồn tại ở MỘT nơi (view). Với đối tượng mới tạo chưa lưu, giá trị là 0;
 * sau khi lưu hoặc cập nhật cần refresh để đọc lại.
 */
public class HoaDon {
	private String maHoaDon;

	/** Mỗi phiếu sửa chữa tối đa một hóa đơn (DB: UQ_HoaDon_maPhieu). */
	private PhieuTiepNhanSuaChua phieuSuaChua;

	private NhanVien nhanVien;

	private KhuyenMai khuyenMai;

	private Date ngayLap;

	/** NULL khi chưa thanh toán; bắt buộc có khi đã thanh toán (DB: CK_HoaDon_DaThanhToan). */
	private PhuongThucThanhToan phuongThucTT;

	private TrangThaiThanhToan trangThaiThanhToan;

	private Date thoiGianThanhToan;

	private String maGiaoDich;

	// ---- Thuộc tính suy diễn: đọc từ view, không có setter ----
	private double tongTienDichVu;

	private double tongTienLinhKien;

	private double tienGiam;

	private double thue;

	private double tongThanhToan;

	public HoaDon() {
		trangThaiThanhToan = TrangThaiThanhToan.CHUA_THANH_TOAN;
	}

	public HoaDon(String maHoaDon) {
		this();
		this.maHoaDon = maHoaDon;
	}

	public HoaDon(String maHoaDon, PhieuTiepNhanSuaChua phieuSuaChua, NhanVien nhanVien, KhuyenMai khuyenMai,
			Date ngayLap, PhuongThucThanhToan phuongThucTT, TrangThaiThanhToan trangThaiThanhToan,
			Date thoiGianThanhToan, String maGiaoDich) {
		this();
		setMaHoaDon(maHoaDon);
		setPhieuSuaChua(phieuSuaChua);
		setNhanVien(nhanVien);
		setNgayLap(ngayLap);
		if (khuyenMai != null) setKhuyenMai(khuyenMai);
		if (trangThaiThanhToan != null) setTrangThaiThanhToan(trangThaiThanhToan);
		setPhuongThucTT(phuongThucTT);
		if (thoiGianThanhToan != null) setThoiGianThanhToan(thoiGianThanhToan);
		setMaGiaoDich(maGiaoDich);
	}

	public String getMaHoaDon() {
		return maHoaDon;
	}
	public void setMaHoaDon(String maHoaDon) {
		if (maHoaDon == null || !maHoaDon.matches("^HD\\d{8}[AP]\\d{3}$")) throw new IllegalArgumentException("Mã hóa đơn không đúng định dạng.");
		this.maHoaDon = maHoaDon;
	}
	public PhieuTiepNhanSuaChua getPhieuSuaChua() {
		return phieuSuaChua;
	}
	public void setPhieuSuaChua(PhieuTiepNhanSuaChua phieuSuaChua) {
		if (phieuSuaChua == null) throw new IllegalArgumentException("Hóa đơn phải gắn với một phiếu sửa chữa.");
		if (phieuSuaChua.getTrangThai() != TrangThaiPhieu.HOAN_TAT) {
			throw new IllegalStateException("Chỉ phiếu sửa chữa đã Hoàn tất mới được lập hóa đơn.");
		}
		this.phieuSuaChua = phieuSuaChua;
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
		if (khuyenMai != null && !khuyenMai.conHieuLucTai(ngayLap)) {
			throw new IllegalArgumentException("Khuyến mãi không còn hiệu lực áp dụng.");
		}
		this.khuyenMai = khuyenMai;
	}
	public Date getNgayLap() {
		return ngayLap;
	}
	public void setNgayLap(Date ngayLap) {
		if (ngayLap == null || ngayLap.after(new Date())) throw new IllegalArgumentException("Ngày lập hóa đơn không hợp lệ.");
		if (khuyenMai != null && !khuyenMai.conHieuLucTai(ngayLap)) {
			throw new IllegalArgumentException("Khuyến mãi không còn hiệu lực áp dụng.");
		}
		this.ngayLap = ngayLap;
	}
	public PhuongThucThanhToan getPhuongThucTT() {
		return phuongThucTT;
	}
	/** Cho phép null khi hóa đơn chưa thanh toán; bắt buộc chọn khi đã thanh toán. */
	public void setPhuongThucTT(PhuongThucThanhToan phuongThucTT) {
		if (phuongThucTT == null && trangThaiThanhToan == TrangThaiThanhToan.DA_THANH_TOAN) {
			throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ.");
		}
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
		if (ngayLap != null && thoiGianThanhToan.before(ngayLap)) {
			throw new IllegalArgumentException("Thời gian thanh toán không được trước ngày lập hóa đơn.");
		}
		this.thoiGianThanhToan = thoiGianThanhToan;
	}
	public String getMaGiaoDich() {
		return maGiaoDich;
	}
	/** Đặt phương thức thanh toán TRƯỚC khi đặt mã giao dịch. Chỉ chuyển khoản mới có mã giao dịch (DB: CK_HoaDon_GiaoDich). */
	public void setMaGiaoDich(String maGiaoDich) {
		boolean coMa = maGiaoDich != null && !maGiaoDich.isBlank();
		if (phuongThucTT == PhuongThucThanhToan.CHUYEN_KHOAN && !coMa) {
			throw new IllegalArgumentException("Thiếu mã giao dịch chuyển khoản.");
		}
		if (phuongThucTT != PhuongThucThanhToan.CHUYEN_KHOAN && coMa) {
			throw new IllegalArgumentException("Chỉ hóa đơn chuyển khoản mới có mã giao dịch.");
		}
		this.maGiaoDich = coMa ? maGiaoDich : null;
	}

	// ---- Chỉ getter (thuộc tính suy diễn từ view vw_HoaDon_TongTien) ----
	public double getTongTienDichVu() {
		return tongTienDichVu;
	}
	public double getTongTienLinhKien() {
		return tongTienLinhKien;
	}
	/** Số tiền được giảm bởi khuyến mãi (0 nếu không có khuyến mãi). */
	public double getTienGiam() {
		return tienGiam;
	}
	public double getThue() {
		return thue;
	}
	public double getTongThanhToan() {
		return tongThanhToan;
	}

	// ── Raw setters dùng trong DAO khi đọc từ CSDL / view ──
	public void setTongTienDichVuRaw(double tongTienDichVu) { this.tongTienDichVu = tongTienDichVu; }
	public void setTongTienLinhKienRaw(double tongTienLinhKien) { this.tongTienLinhKien = tongTienLinhKien; }
	public void setTienGiamRaw(double tienGiam) { this.tienGiam = tienGiam; }
	public void setThueRaw(double thue) { this.thue = thue; }
	public void setTongThanhToanRaw(double tongThanhToan) { this.tongThanhToan = tongThanhToan; }

	@Override public int hashCode() {
		return Objects.hash(maHoaDon);
	}
	@Override public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		return Objects.equals(maHoaDon, ((HoaDon) obj).maHoaDon);
	}
	@Override public String toString() {
		return "HoaDon [maHoaDon=" + maHoaDon + ", phieu=" + (phieuSuaChua == null ? null : phieuSuaChua.getMaPhieu())
				+ ", nhanVien=" + nhanVien + ", khuyenMai=" + (khuyenMai == null ? null : khuyenMai.getMaKM())
				+ ", ngayLap=" + ngayLap + ", phuongThucTT=" + phuongThucTT + ", trangThaiThanhToan="
				+ trangThaiThanhToan + ", thoiGianThanhToan=" + thoiGianThanhToan + ", maGiaoDich="
				+ maGiaoDich + ", tongTienDichVu=" + tongTienDichVu + ", tongTienLinhKien="
				+ tongTienLinhKien + ", tienGiam=" + tienGiam + ", thue=" + thue + ", tongThanhToan="
				+ tongThanhToan + "]";
	}
}
