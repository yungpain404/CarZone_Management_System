package dao;

import connectDB.DbUtil;
import entity.LinhKien;
import entity.NhanVien;
import entity.PhieuYeuCauNhapLinhKien;
import enums.TrangThaiYeuCau;
import enums.VaiTro;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** JDBC DAO tra cứu và xử lý yêu cầu nhập linh kiện. */
public class PhieuYeuCauNhapLinhKienDAO {

    private static final String SELECT_COLUMNS = "SELECT p.maYeuCau, p.maLK, lk.tenLK, lk.donViTinh, lk.soLuongTon, "
            + "p.maNVLap, nvLap.hoTen AS tenNVLap, p.maNVXuLy, nvXuLy.hoTen AS tenNVXuLy, "
            + "p.soLuongYeuCau, p.soLuongNhapThucTe, p.trangThai, p.ngayTao, p.ngayXuLy, p.ghiChu, p.ghiChuXuLy "
            + "FROM PhieuYeuCauNhapLinhKien p JOIN LinhKien lk ON lk.maLK=p.maLK "
            + "JOIN NhanVien nvLap ON nvLap.maNV=p.maNVLap "
            + "LEFT JOIN NhanVien nvXuLy ON nvXuLy.maNV=p.maNVXuLy ";

    /** Tìm theo mã yêu cầu, mã linh kiện hoặc tên linh kiện; trạng thái null lấy mọi trạng thái. */
    public List<PhieuYeuCauNhapLinhKien> timKiem(String tuKhoa, TrangThaiYeuCau trangThai) {
        String sql = SELECT_COLUMNS + "WHERE (? IS NULL OR p.maYeuCau LIKE ? OR p.maLK LIKE ? OR lk.tenLK LIKE ?) "
                + "AND (? IS NULL OR p.trangThai=?) ORDER BY p.ngayTao DESC, p.maYeuCau DESC";
        String pattern = tuKhoa == null || tuKhoa.isBlank() ? null : "%" + tuKhoa.trim() + "%";
        try (Connection con = DbUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            ps.setNString(4, pattern);
            if (trangThai == null) {
                ps.setNull(5, java.sql.Types.VARCHAR);
                ps.setNull(6, java.sql.Types.VARCHAR);
            } else {
                ps.setString(5, trangThai.name());
                ps.setString(6, trangThai.name());
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<PhieuYeuCauNhapLinhKien> result = new ArrayList<>();
                while (rs.next()) result.add(mapRow(rs));
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm yêu cầu nhập linh kiện: " + e.getMessage(), e);
        }
    }

    /** Đọc đầy đủ một yêu cầu để hiển thị chi tiết và kiểm tra trạng thái trước khi xử lý. */
    public PhieuYeuCauNhapLinhKien timTheoMa(String maYeuCau) {
        try (Connection con = DbUtil.getConnection(); PreparedStatement ps = con.prepareStatement(
                SELECT_COLUMNS + "WHERE p.maYeuCau=?")) {
            ps.setString(1, maYeuCau);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi đọc yêu cầu nhập linh kiện: " + e.getMessage(), e);
        }
    }

    /** Danh sách yêu cầu đã duyệt đang chờ nhập cho một linh kiện cụ thể. */
    public List<PhieuYeuCauNhapLinhKien> timYeuCauDaDuyetTheoMaLinhKien(String maLK) {
        String sql = SELECT_COLUMNS + "WHERE p.maLK=? AND p.trangThai='DA_DUYET' ORDER BY p.ngayTao, p.maYeuCau";
        try (Connection con = DbUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maLK);
            try (ResultSet rs = ps.executeQuery()) {
                List<PhieuYeuCauNhapLinhKien> result = new ArrayList<>();
                while (rs.next()) result.add(mapRow(rs));
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm yêu cầu chờ nhập kho: " + e.getMessage(), e);
        }
    }

    /** Cập nhật có điều kiện theo trạng thái cũ để ngăn xử lý trùng; trigger DB quản lý luật và tồn kho. */
    public void capNhatTrangThai(String maYeuCau, TrangThaiYeuCau trangThaiCu, TrangThaiYeuCau trangThaiMoi,
                                String maNVXuLy, String ghiChuXuLy, Integer soLuongNhapThucTe) {
        String sql = "UPDATE PhieuYeuCauNhapLinhKien SET trangThai=?, maNVXuLy=?, ngayXuLy=SYSDATETIME(), "
                + "ghiChuXuLy=COALESCE(?, ghiChuXuLy), soLuongNhapThucTe=? WHERE maYeuCau=? AND trangThai=?";
        try (Connection con = DbUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, trangThaiMoi.name());
            ps.setString(2, maNVXuLy);
            if (ghiChuXuLy == null || ghiChuXuLy.isBlank()) ps.setNull(3, java.sql.Types.NVARCHAR);
            else ps.setNString(3, ghiChuXuLy.trim());
            if (soLuongNhapThucTe == null) ps.setNull(4, java.sql.Types.INTEGER);
            else ps.setInt(4, soLuongNhapThucTe);
            ps.setString(5, maYeuCau);
            ps.setString(6, trangThaiCu.name());
            if (ps.executeUpdate() == 0) throw new IllegalStateException("Yêu cầu đã được xử lý hoặc trạng thái vừa thay đổi. Hãy tìm lại dữ liệu.");
        } catch (SQLException e) {
            throw new RuntimeException("Không thể cập nhật yêu cầu: " + e.getMessage(), e);
        }
    }

    private static PhieuYeuCauNhapLinhKien mapRow(ResultSet rs) throws SQLException {
        LinhKien lk = new LinhKien();
        lk.setMaLKRaw(rs.getString("maLK"));
        lk.setTenLKRaw(rs.getString("tenLK"));
        lk.setDonViTinhRaw(rs.getString("donViTinh"));
        lk.setSoLuongTonRaw(rs.getInt("soLuongTon"));

        NhanVien nguoiLap = nhanVien(rs.getString("maNVLap"), rs.getNString("tenNVLap"), VaiTro.KY_THUAT_VIEN);
        PhieuYeuCauNhapLinhKien item = new PhieuYeuCauNhapLinhKien();
        item.setMaYeuCau(rs.getString("maYeuCau"));
        item.setLinhKien(lk);
        item.setNhanVienLap(nguoiLap);
        Timestamp ngayTao = rs.getTimestamp("ngayTao");
        item.setNgayTao(ngayTao == null ? null : new Date(ngayTao.getTime()));
        item.setSoLuongYeuCau(rs.getInt("soLuongYeuCau"));
        int actualQuantity = rs.getInt("soLuongNhapThucTe");
        if (!rs.wasNull()) item.setSoLuongNhapThucTeRaw(actualQuantity);
        item.setGhiChu(rs.getNString("ghiChu"));
        TrangThaiYeuCau state = TrangThaiYeuCau.valueOf(rs.getString("trangThai"));
        item.setTrangThai(TrangThaiYeuCau.CHO_DUYET);
        if (state == TrangThaiYeuCau.DA_NHAP_KHO) item.setTrangThai(TrangThaiYeuCau.DA_DUYET);
        if (state != TrangThaiYeuCau.CHO_DUYET) item.setTrangThai(state);
        String maXuLy = rs.getString("maNVXuLy");
        if (maXuLy != null) {
            item.setNhanVienXuLy(nhanVien(maXuLy, rs.getNString("tenNVXuLy"), VaiTro.CHU_CUA_HANG));
            Timestamp ngayXuLy = rs.getTimestamp("ngayXuLy");
            if (ngayXuLy != null) item.setNgayXuLy(new Date(ngayXuLy.getTime()));
        }
        item.setGhiChuXuLy(rs.getNString("ghiChuXuLy"));
        return item;
    }

    private static NhanVien nhanVien(String maNV, String hoTen, VaiTro vaiTro) {
        NhanVien nv = new NhanVien();
        nv.setMaNVRaw(maNV);
        nv.setHoTenRaw(hoTen);
        nv.setVaiTroRaw(vaiTro);
        return nv;
    }
}
