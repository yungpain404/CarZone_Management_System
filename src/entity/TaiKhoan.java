 package entity;

import java.util.Objects;

public class TaiKhoan {

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

        this.matKhau = matKhau;
    }

    public boolean isTrangThaiHoatDong() {
        return trangThaiHoatDong;
    }

    public void setTrangThaiHoatDong(boolean trangThaiHoatDong) {
        this.trangThaiHoatDong = trangThaiHoatDong;
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

	@Override
	public String toString() {
		return "TaiKhoan [tenDangNhap=" + tenDangNhap + ", matKhau=" + matKhau + ", trangThaiHoatDong="
				+ trangThaiHoatDong + "]";
	}
    
    
}
