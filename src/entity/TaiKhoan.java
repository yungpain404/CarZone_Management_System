package entity;

import java.util.Objects;

/**
 * Tài khoản đăng nhập. Bảng TaiKhoan (V1): khóa chính là maNV (cũng là khóa ngoại tới NhanVien).
 * tenDangNhap do sp_TaoTaiKhoan sinh theo vai trò (CCH/TNV/KTV + 3 số).
 */
public class TaiKhoan {

    private String maNV;

    /** Phía sở hữu của quan hệ 1-1, dùng chung khóa chính với NhanVien. */
    private NhanVien nhanVien;

    private String tenDangNhap;

    private String matKhau;

    private boolean trangThaiHoatDong = true;

    public TaiKhoan() {
    }

    public TaiKhoan(String tenDangNhap) {
		super();
		setTenDangNhap(tenDangNhap);
	}

	public TaiKhoan(String tenDangNhap, String matKhau, boolean trangThaiHoatDong) {
        setTenDangNhap(tenDangNhap);
        setMatKhau(matKhau);
        this.trangThaiHoatDong = trangThaiHoatDong;
    }

    public String getMaNV() {
        return maNV;
    }

    public NhanVien getNhanVien() {
        return nhanVien;
    }

    /** Chỉ gán tham chiếu; phía NhanVien.setTaiKhoan đã đồng bộ hai chiều. */
    public void setNhanVien(NhanVien nhanVien) {
        this.nhanVien = nhanVien;
        if (nhanVien != null) this.maNV = nhanVien.getMaNV();
    }

    public String getTenDangNhap() {
        return tenDangNhap;
    }

    public void setTenDangNhap(String tenDangNhap) {
        if (tenDangNhap == null || tenDangNhap.length() < 5) {
            throw new IllegalArgumentException(
                "Tên đăng nhập phải có ít nhất 5 ký tự."
            );
        }

        if (tenDangNhap.contains(" ")) {
            throw new IllegalArgumentException(
                "Tên đăng nhập không được chứa khoảng trắng."
            );
        }

        // Định dạng: CCHXXX, TNVXXX hoặc KTVXXX
        if (!tenDangNhap.matches("^(CCH|TNV|KTV)\\d{3}$")) {
            throw new IllegalArgumentException(
                "Tên đăng nhập phải có dạng CCHXXX, TNVXXX hoặc KTVXXX, "
                + "trong đó XXX là 3 chữ số."
            );
        }

        this.tenDangNhap = tenDangNhap;
    }

    public String getMatKhau() {
        return matKhau;
    }

    /**
     * Nhận mật khẩu GỐC, kiểm tra độ mạnh rồi lưu bản BĂM SHA-256 (64 ký tự hex in hoa), cùng dạng
     * với dữ liệu mẫu trong DB (HASHBYTES('SHA2_256', ...)). Không truyền chuỗi đã băm vào hàm này.
     * Giá trị đọc từ DB do Hibernate gán thẳng vào trường, không đi qua hàm này.
     */
    public void setMatKhau(String matKhau) {
        if (matKhau == null || matKhau.length() < 8) {
            throw new IllegalArgumentException(
                "Mật khẩu phải có ít nhất 8 ký tự."
            );
        }

        boolean coChuHoa = matKhau.matches("(?s).*\\p{Lu}.*");
        boolean coChuThuong = matKhau.matches("(?s).*\\p{Ll}.*");
        boolean coSo = matKhau.matches("(?s).*\\d.*");
        boolean coKyTuDacBiet =
                matKhau.matches("(?s).*[^\\p{L}\\d\\s].*");

        if (!coChuHoa || !coChuThuong || !coSo || !coKyTuDacBiet) {
            throw new IllegalArgumentException(
                "Mật khẩu phải có chữ hoa, chữ thường, "
                + "chữ số và ký tự đặc biệt."
            );
        }

        this.matKhau = bamSha256(matKhau);
    }

    /** Đăng nhập: so mật khẩu người dùng nhập với bản băm đã lưu. */
    public boolean khopMatKhau(String matKhauNhap) {
        return matKhau != null && matKhauNhap != null && matKhau.equalsIgnoreCase(bamSha256(matKhauNhap));
    }

    private static String bamSha256(String s) {
        try {
            byte[] h = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : h) sb.append(String.format("%02X", b));
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean isTrangThaiHoatDong() {
        return trangThaiHoatDong;
    }

    public void setTrangThaiHoatDong(boolean trangThaiHoatDong) {
        this.trangThaiHoatDong = trangThaiHoatDong;
    }

	// ── Raw setters dùng trong DAO khi map từ ResultSet ──────────────────────

	/** Gán NhanVien trực tiếp (không validate, không gọi setTaiKhoan ngược). Dùng trong DAO. */
	public void setNhanVienRaw(NhanVien nhanVien) {
		this.nhanVien = nhanVien;
		if (nhanVien != null) this.maNV = nhanVien.getMaNV();
	}

	/** Gán tenDangNhap trực tiếp (không validate). Dùng trong DAO khi đọc từ DB. */
	public void setTenDangNhapRaw(String tenDangNhap) {
		this.tenDangNhap = tenDangNhap;
	}

	/**
	 * Gán chuỗi băm SHA-256 đọc từ DB trực tiếp (không băm lại).
	 * Dùng trong DAO – giá trị đã là hash 64 ký tự hex.
	 */
	public void setMatKhauHash(String hash) {
		this.matKhau = hash;
	}

	@Override
	public int hashCode() {
		return Objects.hash(tenDangNhap);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TaiKhoan other = (TaiKhoan) obj;
		return Objects.equals(tenDangNhap, other.tenDangNhap);
	}

	/** Không in mật khẩu và không in nhanVien (tránh lộ dữ liệu và lặp vô hạn). */
	@Override
	public String toString() {
		return "TaiKhoan [tenDangNhap=" + tenDangNhap + ", trangThaiHoatDong=" + trangThaiHoatDong + "]";
	}

}

