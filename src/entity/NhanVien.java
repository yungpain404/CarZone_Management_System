package entity;

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
		this.maNV = maNV;
	}

	public NhanVien(String maNV, TaiKhoan taiKhoan, String hoTen, LocalDate ngaySinh, String diaChi,
			LocalDate ngayVaoLam, VaiTro vaiTro) {
		super();
		this.maNV = maNV;
		this.taiKhoan = taiKhoan;
		this.hoTen = hoTen;
		this.ngaySinh = ngaySinh;
		this.diaChi = diaChi;
		this.ngayVaoLam = ngayVaoLam;
		this.vaiTro = vaiTro;
	}
	
}
