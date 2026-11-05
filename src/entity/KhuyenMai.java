package entity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import enums.VaiTro;

/**
 * Chương trình khuyến mãi. Bảng KhuyenMai (V1) + hai bảng nối KhuyenMai_DichVu, KhuyenMai_LinhKien.
 *
 * Quy tắc áp dụng (nhóm chốt 25/10/2026):
 *  - Cả hai danh sách rỗng  = giảm tất cả, không điều kiện.
 *  - Có liệt kê mặt hàng    = chỉ các mặt hàng nằm trong danh sách được giảm.
 * Khi khuyến mãi đã gắn vào hóa đơn, trigger trong DB không cho đổi mức giảm, thời gian, danh sách.
 */
public class KhuyenMai {
	private String maKM;
	private NhanVien nhanVien;
	private String tenChuongTrinh;
	private Set<LinhKien> linhKienApDung = new HashSet<>();
	private Set<DichVu> dichVuApDung = new HashSet<>();
	private double mucGiamPhanTram;
	private Date ngayBatDau;
	private Date ngayKetThuc;
	private String ghiChu;

	public KhuyenMai() {
	}
	public KhuyenMai(String maKM) {
		this.maKM = maKM;
	}
	public KhuyenMai(String maKM, NhanVien nhanVien, String tenChuongTrinh, Set<LinhKien> linhKienApDung,
			Set<DichVu> dichVuApDung, double mucGiamPhanTram, Date ngayBatDau, Date ngayKetThuc, String ghiChu) {
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
		// DB: CK_KhuyenMai_Ma = 'KM' + đúng 3 chữ số
		if (maKM == null || !maKM.matches("^KM\\d{3}$")) throw new IllegalArgumentException("Mã khuyến mãi không đúng định dạng.");
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
		if (tenChuongTrinh.length() > 150) throw new IllegalArgumentException("Tên chương trình khuyến mãi tối đa 150 ký tự.");
		this.tenChuongTrinh = tenChuongTrinh;
	}

	/** Danh sách linh kiện áp dụng (chỉ đọc). Rỗng nghĩa là không giới hạn theo linh kiện. */
	public Set<LinhKien> getLinhKienApDung() {
		return Collections.unmodifiableSet(linhKienApDung);
	}
	/** Thay toàn bộ danh sách. Giữ nguyên tập đang quản lý (không gán lại tham chiếu) để Hibernate theo dõi thay đổi. */
	public void setLinhKienApDung(Set<LinhKien> linhKien) {
		if (linhKien != null && linhKien.contains(null)) throw new IllegalArgumentException("Danh sách linh kiện áp dụng có phần tử rỗng.");
		this.linhKienApDung.clear();
		if (linhKien != null) this.linhKienApDung.addAll(linhKien);
	}
	/** Danh sách dịch vụ áp dụng (chỉ đọc). Rỗng nghĩa là không giới hạn theo dịch vụ. */
	public Set<DichVu> getDichVuApDung() {
		return Collections.unmodifiableSet(dichVuApDung);
	}
	public void setDichVuApDung(Set<DichVu> dichVu) {
		if (dichVu != null && dichVu.contains(null)) throw new IllegalArgumentException("Danh sách dịch vụ áp dụng có phần tử rỗng.");
		this.dichVuApDung.clear();
		if (dichVu != null) this.dichVuApDung.addAll(dichVu);
	}
	/** True khi không liệt kê dịch vụ/linh kiện nào: giảm tất cả. */
	public boolean apDungTatCa() {
		return linhKienApDung.isEmpty() && dichVuApDung.isEmpty();
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
		if (ghiChu != null && ghiChu.length() > 500) throw new IllegalArgumentException("Ghi chú không được vượt quá 500 ký tự.");
		this.ghiChu = ghiChu;
	}

	/**
	 * Khuyến mãi còn hiệu lực tại ngày đã cho không? So sánh THEO NGÀY, gồm cả ngày bắt đầu và ngày kết thúc
	 * (giống DB: CAST(ngayLap AS DATE) BETWEEN ngayBatDau AND ngayKetThuc). So sánh theo mili-giây sẽ loại nhầm
	 * hóa đơn lập trong ngày cuối vì ngày kết thúc là 00:00.
	 */
	public boolean conHieuLucTai(Date ngay) {
		if (ngay == null || ngayBatDau == null || ngayKetThuc == null) return false;
		LocalDate d = toLocalDate(ngay);
		return !d.isBefore(toLocalDate(ngayBatDau)) && !d.isAfter(toLocalDate(ngayKetThuc));
	}

	// Dùng getTime(): java.sql.Date (Hibernate có thể trả về) ném UnsupportedOperationException ở toInstant().
	private static LocalDate toLocalDate(Date d) {
		return Instant.ofEpochMilli(d.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
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
