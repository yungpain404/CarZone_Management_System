package entity;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;

import enums.VaiTro;

/**
 * Nhân viên. Bảng NhanVien (V1). maNV do sp_ThemNhanVien sinh theo vai trò (CCH/TNV/KTV + 3 số).
 * Cột tuoi trong DB là cột tính toán, không ánh xạ; dùng {@link #getTuoi()}.
 */
public class NhanVien {
	private String maNV;
	/** Phía nghịch của quan hệ 1-1; khóa nằm ở bảng TaiKhoan (TaiKhoan.maNV). */
	private TaiKhoan taiKhoan;
	private String hoTen;
	private LocalDate ngaySinh;
	private String soDienThoai;
	private String CCCD;
	private String diaChi;
	private LocalDate ngayVaoLam;
	private VaiTro vaiTro;

	public NhanVien() {
		super();
	}

	public NhanVien(String maNV) {
		super();
		setMaNV(maNV);
	}

	public NhanVien(String maNV, TaiKhoan taiKhoan, String hoTen, LocalDate ngaySinh, String soDienThoai,
			String CCCD, String diaChi, LocalDate ngayVaoLam, VaiTro vaiTro) {
		super();
		setMaNV(maNV);
		setTaiKhoan(taiKhoan);
		setHoTen(hoTen);
		setNgaySinh(ngaySinh);
		setSoDienThoai(soDienThoai);
		setCCCD(CCCD);
		setDiaChi(diaChi);
		setNgayVaoLam(ngayVaoLam);
		setVaiTro(vaiTro);
	}

	public String getMaNV() {
		return maNV;
	}
	public void setMaNV(String maNV) {
		if (maNV == null || !maNV.matches("^(CCH|TNV|KTV)\\d{3}$")) {
			throw new IllegalArgumentException("Mã nhân viên phải có dạng CCHXXX, TNVXXX hoặc KTVXXX.");
		}
		this.maNV = maNV;
	}
	public TaiKhoan getTaiKhoan() {
		return taiKhoan;
	}
	public void setTaiKhoan(TaiKhoan taiKhoan) {
		this.taiKhoan = taiKhoan;
		if (taiKhoan != null) taiKhoan.setNhanVien(this);
	}
	public String getHoTen() {
		return hoTen;
	}
	public void setHoTen(String hoTen) {
		if (hoTen == null || hoTen.isBlank() || hoTen.length() > 100) {
			throw new IllegalArgumentException("Họ tên không được để trống và tối đa 100 ký tự.");
		}
		this.hoTen = hoTen;
	}
	public LocalDate getNgaySinh() {
		return ngaySinh;
	}
	public void setNgaySinh(LocalDate ngaySinh) {
		if (ngaySinh == null || ngaySinh.isAfter(LocalDate.now())) {
			throw new IllegalArgumentException("Ngày sinh không hợp lệ.");
		}
		if (ngayVaoLam != null && ngaySinh.isAfter(ngayVaoLam.minusYears(18))) {
			throw new IllegalArgumentException("Nhân viên phải đủ 18 tuổi tại ngày vào làm.");
		}
		this.ngaySinh = ngaySinh;
	}
	public String getSoDienThoai() {
		return soDienThoai;
	}
	public void setSoDienThoai(String soDienThoai) {
		// DB: CK_NhanVien_SDT = chỉ số, 10-11 ký tự, bắt đầu bằng 0
		if (soDienThoai == null || !soDienThoai.matches("^0\\d{9,10}$")) {
			throw new IllegalArgumentException("Số điện thoại phải gồm 10-11 chữ số và bắt đầu bằng 0.");
		}
		this.soDienThoai = soDienThoai;
	}
	public String getCCCD() {
		return CCCD;
	}
	public void setCCCD(String CCCD) {
		if (CCCD == null || !CCCD.matches("^\\d{12}$")) {
			throw new IllegalArgumentException("CCCD phải gồm 12 chữ số.");
		}
		this.CCCD = CCCD;
	}
	public String getDiaChi() {
		return diaChi;
	}
	public void setDiaChi(String diaChi) {
		if (diaChi != null && diaChi.length() > 200) {
			throw new IllegalArgumentException("Địa chỉ tối đa 200 ký tự.");
		}
		this.diaChi = diaChi;
	}
	public LocalDate getNgayVaoLam() {
		return ngayVaoLam;
	}
	public void setNgayVaoLam(LocalDate ngayVaoLam) {
		if (ngayVaoLam == null || ngayVaoLam.isAfter(LocalDate.now())) {
			throw new IllegalArgumentException("Ngày vào làm không hợp lệ.");
		}
		// DB: CK_NhanVien_DuTuoi = ngaySinh <= ngayVaoLam - 18 năm
		if (ngaySinh != null && ngaySinh.isAfter(ngayVaoLam.minusYears(18))) {
			throw new IllegalArgumentException("Nhân viên phải đủ 18 tuổi tại ngày vào làm.");
		}
		this.ngayVaoLam = ngayVaoLam;
	}
	public VaiTro getVaiTro() {
		return vaiTro;
	}
	public void setVaiTro(VaiTro vaiTro) {
		if (vaiTro == null) {
			throw new IllegalArgumentException("Vai trò nhân viên không được để trống.");
		}
		this.vaiTro = vaiTro;
	}

	/** Thuộc tính suy diễn (/tuoi): chỉ getter, không lưu. */
	public int getTuoi() {
		return ngaySinh == null ? 0 : Period.between(ngaySinh, LocalDate.now()).getYears();
	}

	// ── Raw setters dùng trong DAO khi map từ ResultSet ──────────────────────
	// DB đã đảm bảo dữ liệu hợp lệ; bỏ qua validation để tránh side effect.

	/** Gán maNV trực tiếp (không validate). Dùng trong DAO khi đọc từ DB. */
	public void setMaNVRaw(String maNV)           { this.maNV = maNV; }
	/** Gán hoTen trực tiếp. Dùng trong DAO. */
	public void setHoTenRaw(String hoTen)         { this.hoTen = hoTen; }
	/** Gán ngaySinh trực tiếp. Dùng trong DAO. */
	public void setNgaySinhRaw(LocalDate d)       { this.ngaySinh = d; }
	/** Gán soDienThoai trực tiếp. Dùng trong DAO. */
	public void setSoDienThoaiRaw(String sdt)     { this.soDienThoai = sdt; }
	/** Gán CCCD trực tiếp. Dùng trong DAO. */
	public void setCCCDRaw(String cccd)           { this.CCCD = cccd; }
	/** Gán ngayVaoLam trực tiếp. Dùng trong DAO. */
	public void setNgayVaoLamRaw(LocalDate d)     { this.ngayVaoLam = d; }
	/** Gán vaiTro trực tiếp. Dùng trong DAO. */
	public void setVaiTroRaw(VaiTro v)            { this.vaiTro = v; }

	@Override public int hashCode() {
		return Objects.hash(maNV);
	}
	@Override public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		return Objects.equals(maNV, ((NhanVien) obj).maNV);
	}
	@Override public String toString() {
		return "NhanVien [maNV=" + maNV + ", taiKhoan=" + taiKhoan + ", hoTen=" + hoTen
				+ ", ngaySinh=" + ngaySinh + ", soDienThoai=" + soDienThoai + ", diaChi=" + diaChi
				+ ", ngayVaoLam=" + ngayVaoLam + ", vaiTro=" + vaiTro + "]";
	}

}

