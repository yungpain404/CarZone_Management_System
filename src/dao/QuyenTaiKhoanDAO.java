package dao;

import connectDB.DbUtil;
import enums.CheDoTruyCap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

/** Lưu cấu hình truy cập màn hình riêng theo tài khoản nhân viên. */
public class QuyenTaiKhoanDAO {
    public CauHinh layCauHinh(String maNV) {
        String sql = "SELECT cheDoTruyCap FROM CauHinhQuyenTaiKhoan WHERE maNV = ?";
        try (Connection con = DbUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maNV);
            CheDoTruyCap mode;
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                mode = CheDoTruyCap.valueOf(rs.getString("cheDoTruyCap"));
            }
            Set<String> functions = new LinkedHashSet<>();
            if (mode == CheDoTruyCap.TUY_CHINH) {
                try (PreparedStatement detail = con.prepareStatement(
                        "SELECT maChucNang FROM ChiTietQuyenTaiKhoan WHERE maNV = ? ORDER BY maChucNang")) {
                    detail.setString(1, maNV);
                    try (ResultSet items = detail.executeQuery()) {
                        while (items.next()) functions.add(items.getString("maChucNang"));
                    }
                }
            }
            return new CauHinh(mode, Set.copyOf(functions));
        } catch (SQLException e) {
            throw new RuntimeException("Không thể tải quyền riêng của tài khoản: " + e.getMessage(), e);
        }
    }

    /** Lưu lựa chọn quyền theo tài khoản; THEO_VAI_TRO xóa cấu hình riêng để dùng quyền mặc định. */
    public void luuCauHinh(String maNV, CheDoTruyCap mode, Set<String> functions) {
        try (Connection con = DbUtil.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement check = con.prepareStatement(
                        "SELECT 1 FROM TaiKhoan tk JOIN NhanVien nv ON nv.maNV = tk.maNV "
                                + "WHERE tk.maNV = ? AND nv.vaiTro <> 'CHU_CUA_HANG'")) {
                    check.setString(1, maNV);
                    try (ResultSet rs = check.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Chỉ tài khoản Tiếp nhận viên hoặc Kỹ thuật viên mới cấu hình được.");
                    }
                }

                if (mode == CheDoTruyCap.THEO_VAI_TRO) {
                    try (PreparedStatement delete = con.prepareStatement("DELETE FROM CauHinhQuyenTaiKhoan WHERE maNV = ?")) {
                        delete.setString(1, maNV);
                        delete.executeUpdate();
                    }
                } else {
                    try (PreparedStatement upsert = con.prepareStatement(
                            "UPDATE CauHinhQuyenTaiKhoan SET cheDoTruyCap = ?, ngayCapNhat = SYSDATETIME() WHERE maNV = ?")) {
                        upsert.setString(1, mode.name());
                        upsert.setString(2, maNV);
                        if (upsert.executeUpdate() == 0) {
                            try (PreparedStatement insert = con.prepareStatement(
                                    "INSERT INTO CauHinhQuyenTaiKhoan (maNV, cheDoTruyCap) VALUES (?, ?)")) {
                                insert.setString(1, maNV);
                                insert.setString(2, mode.name());
                                insert.executeUpdate();
                            }
                        }
                    }
                    try (PreparedStatement delete = con.prepareStatement("DELETE FROM ChiTietQuyenTaiKhoan WHERE maNV = ?")) {
                        delete.setString(1, maNV);
                        delete.executeUpdate();
                    }
                    if (mode == CheDoTruyCap.TUY_CHINH) {
                        try (PreparedStatement insert = con.prepareStatement(
                                "INSERT INTO ChiTietQuyenTaiKhoan (maNV, maChucNang) VALUES (?, ?)")) {
                            for (String function : functions) {
                                insert.setString(1, maNV);
                                insert.setString(2, function);
                                insert.addBatch();
                            }
                            insert.executeBatch();
                        }
                    }
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } catch (RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Không thể lưu quyền riêng của tài khoản: " + e.getMessage(), e);
        }
    }

    public record CauHinh(CheDoTruyCap cheDo, Set<String> maChucNang) { }
}
