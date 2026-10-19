package entity;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;
import java.time.LocalDate;

import enums.VaiTro;

public class NhanVien {
	private String maNV;
	private TaiKhoan taiKhoan;
	private String hoTen;
	private LocalDate ngaySinh;
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

	public NhanVien(String maNV, TaiKhoan taiKhoan, String hoTen, LocalDate ngaySinh, String diaChi,
			LocalDate ngayVaoLam, VaiTro vaiTro) {
		super();
		setMaNV(maNV);
		this.taiKhoan = taiKhoan;
		setHoTen(hoTen);
		setNgaySinh(ngaySinh);
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
		if (ngayVaoLam != null && ngaySinh.isAfter(ngayVaoLam)) {
			throw new IllegalArgumentException("Ngày sinh phải trước ngày vào làm.");
		}
		this.ngaySinh = ngaySinh;
	}
	public String getDiaChi() {
		return diaChi;
	}
	public void setDiaChi(String diaChi) {
		this.diaChi = diaChi;
	}
	public LocalDate getNgayVaoLam() {
		return ngayVaoLam;
	}
	public void setNgayVaoLam(LocalDate ngayVaoLam) {
		if (ngayVaoLam == null || ngayVaoLam.isAfter(LocalDate.now())) {
			throw new IllegalArgumentException("Ngày vào làm không hợp lệ.");
		}
		if (ngaySinh != null && (ngayVaoLam.isBefore(ngaySinh) || ngayVaoLam.isBefore(ngaySinh.plusYears(16)))) {
			throw new IllegalArgumentException("Ngày vào làm không hợp lý so với ngày sinh.");
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

	public int getTuoi() {
		return ngaySinh == null ? 0 : Period.between(ngaySinh, LocalDate.now()).getYears();
	}

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
				+ ", ngaySinh=" + ngaySinh + ", diaChi=" + diaChi + ", ngayVaoLam=" + ngayVaoLam
				+ ", vaiTro=" + vaiTro + "]";
	}
	
}
