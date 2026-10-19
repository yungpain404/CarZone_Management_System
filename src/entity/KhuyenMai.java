package entity;

import java.util.Date;
import java.util.Objects;
import java.util.HashSet;
import enums.VaiTro;

public class KhuyenMai {
	private String maKM;
	private NhanVien nhanVien;
	private String tenChuongTrinh;
	private String[] linhKienApDung;
	private String[] dichVuApDung;
	private double mucGiamPhanTram;
	private Date ngayBatDau;
	private Date ngayKetThuc;
	private String ghiChu;

	public KhuyenMai() {
	}
	public KhuyenMai(String maKM) {
		this.maKM = maKM;
	}
	public KhuyenMai(String maKM, NhanVien nhanVien, String tenChuongTrinh, String[] linhKienApDung,
			String[] dichVuApDung, double mucGiamPhanTram, Date ngayBatDau, Date ngayKetThuc, String ghiChu) {
		setMaKM(maKM);
		setNhanVien(nhanVien);
		setTenChuongTrinh(tenChuongTrinh);
		setLinhKienApDung(linhKienApDung);
		setDichVuApDung(dichVuApDung);
		setMucGiamPhanTram(mucGiamPhanTram);
		setNgayBatDau(ngayBatDau);
		setNgayKetThuc(ngayKetThuc);
		setGhiChu(ghiChu);
	}
	public String getMaKM() {
		return maKM;
	}
	public void setMaKM(String maKM) {
		if (maKM == null || !maKM.matches("^KM\\d{3,}$")) throw new IllegalArgumentException("Mã khuyến mãi không đúng định dạng.");
		this.maKM = maKM;
	}
	public NhanVien getNhanVien() {
		return nhanVien;
	}
	public void setNhanVien(NhanVien nhanVien) {
		if (nhanVien == null || nhanVien.getVaiTro() != VaiTro.CHU_CUA_HANG) throw new IllegalArgumentException("Chỉ Chủ cửa hàng mới được tạo khuyến mãi.");
		this.nhanVien = nhanVien;
	}
	public String getTenChuongTrinh() {
		return tenChuongTrinh;
	}
	public void setTenChuongTrinh(String tenChuongTrinh) {
		if (tenChuongTrinh == null || tenChuongTrinh.isBlank()) throw new IllegalArgumentException("Tên chương trình khuyến mãi không được để trống.");
		this.tenChuongTrinh = tenChuongTrinh;
	}
	public String[] getLinhKienApDung() {
		return linhKienApDung == null ? null : linhKienApDung.clone();
	}
	public void setLinhKienApDung(String[] linhKienApDung) {
		validateUniqueCodes(linhKienApDung, "LK", "linh kiện");
		this.linhKienApDung = linhKienApDung == null ? null : linhKienApDung.clone();
	}
	public String[] getDichVuApDung() {
		return dichVuApDung == null ? null : dichVuApDung.clone();
	}
	public void setDichVuApDung(String[] dichVuApDung) {
		validateUniqueCodes(dichVuApDung, "DV", "dịch vụ");
		this.dichVuApDung = dichVuApDung == null ? null : dichVuApDung.clone();
	}
	public double getMucGiamPhanTram() {
		return mucGiamPhanTram;
	}
	public void setMucGiamPhanTram(double mucGiamPhanTram) {
		if (!Double.isFinite(mucGiamPhanTram) || mucGiamPhanTram <= 0 || mucGiamPhanTram > 100) {
			throw new IllegalArgumentException("Mức giảm phải trong khoảng (0, 100]%.");
		}
		this.mucGiamPhanTram = mucGiamPhanTram;
	}
	public Date getNgayBatDau() {
		return ngayBatDau;
	}
	public void setNgayBatDau(Date ngayBatDau) {
		if (ngayBatDau == null) throw new IllegalArgumentException("Ngày bắt đầu không hợp lệ.");
		if (ngayKetThuc != null && ngayKetThuc.before(ngayBatDau)) throw new IllegalArgumentException("Ngày bắt đầu không được sau ngày kết thúc.");
		this.ngayBatDau = ngayBatDau;
	}
	public Date getNgayKetThuc() {
		return ngayKetThuc;
	}
	public void setNgayKetThuc(Date ngayKetThuc) {
		if (ngayKetThuc == null || (ngayBatDau != null && ngayKetThuc.before(ngayBatDau))) {
			throw new IllegalArgumentException("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu.");
		}
		this.ngayKetThuc = ngayKetThuc;
	}
	public String getGhiChu() {
		return ghiChu;
	}
	public void setGhiChu(String ghiChu) {
		if (ghiChu != null && ghiChu.length() > 255) throw new IllegalArgumentException("Ghi chú không được vượt quá 255 ký tự.");
		this.ghiChu = ghiChu;
	}

	private void validateUniqueCodes(String[] codes, String prefix, String label) {
		if (codes == null) return;
		HashSet<String> unique = new HashSet<>();
		for (String code : codes) {
			if (code == null || !code.matches("^" + prefix + "\\d{5,}$")) {
				throw new IllegalArgumentException("Mã " + label + " không đúng định dạng: " + code);
			}
			if (!unique.add(code)) throw new IllegalArgumentException("Mã " + label + " bị trùng: " + code);
		}
	}
	@Override public int hashCode() {
		return Objects.hash(maKM);
	}
	@Override public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		return Objects.equals(maKM, ((KhuyenMai) obj).maKM);
	}
	@Override public String toString() {
		return "KhuyenMai [maKM=" + maKM + ", nhanVien=" + nhanVien + ", tenChuongTrinh=" + tenChuongTrinh
				+ ", mucGiamPhanTram=" + mucGiamPhanTram + ", ngayBatDau=" + ngayBatDau
				+ ", ngayKetThuc=" + ngayKetThuc + ", ghiChu=" + ghiChu + "]";
	}
}
