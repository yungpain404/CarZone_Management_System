package dao;

import connectDB.DbUtil;
import entity.DichVu;
import entity.KhuyenMai;
import entity.LinhKien;
import entity.NhanVien;
import enums.VaiTro;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;

/** JDBC DAO quản lý khuyến mãi và các danh sách mặt hàng được áp dụng. */
public class KhuyenMaiDAO {

    /** Tóm tắt một chương trình để hiển thị bảng; cờ đã dùng cho biết có hóa đơn tham chiếu. */
    public record KhuyenMaiTongQuan(KhuyenMai khuyenMai, int soDichVu, int soLinhKien, boolean daApDungHoaDon) {
        public String phamVi() {
            if (soDichVu == 0 && soLinhKien == 0) return "Tất cả";
            if (soDichVu > 0 && soLinhKien > 0) return "Dịch vụ, linh kiện";
            return soDichVu > 0 ? "Dịch vụ" : "Linh kiện";
        }
    }

    /** Tìm theo mã/tên và khoảng ngày; chương trình phải giao với khoảng thời gian được chọn. */
    public List<KhuyenMaiTongQuan> timKiem(String tuKhoa, java.util.Date tuNgay, java.util.Date denNgay) {
        String sql = "SELECT k.maKM, k.maNV, k.tenChuongTrinh, k.mucGiamPhanTram, k.ngayBatDau, k.ngayKetThuc, k.ghiChu, "
                + "(SELECT COUNT(*) FROM KhuyenMai_DichVu kd WHERE kd.maKM=k.maKM) soDichVu, "
                + "(SELECT COUNT(*) FROM KhuyenMai_LinhKien kl WHERE kl.maKM=k.maKM) soLinhKien, "
                + "CASE WHEN EXISTS (SELECT 1 FROM HoaDon h WHERE h.maKM=k.maKM) THEN 1 ELSE 0 END daDung "
                + "FROM KhuyenMai k WHERE (k.maKM LIKE ? OR k.tenChuongTrinh LIKE ?) "
                + "AND k.ngayBatDau <= ? AND k.ngayKetThuc >= ? ORDER BY k.ngayBatDau DESC, k.maKM";
        String pattern = tuKhoa == null || tuKhoa.isBlank() ? "%" : "%" + tuKhoa.trim() + "%";
        try (Connection con = DbUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setNString(2, pattern);
            ps.setDate(3, toSqlDate(denNgay));
            ps.setDate(4, toSqlDate(tuNgay));
            try (ResultSet rs = ps.executeQuery()) {
                List<KhuyenMaiTongQuan> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(new KhuyenMaiTongQuan(mapKhuyenMai(rs), rs.getInt("soDichVu"),
                            rs.getInt("soLinhKien"), rs.getBoolean("daDung")));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm chương trình khuyến mãi: " + e.getMessage(), e);
        }
    }

    /** Nạp giá trị hiện tại và đầy đủ dịch vụ/linh kiện đã chọn để hiển thị form Sửa/chi tiết. */
    public KhuyenMai timTheoMa(String maKM) {
        String sql = "SELECT maKM, maNV, tenChuongTrinh, mucGiamPhanTram, ngayBatDau, ngayKetThuc, ghiChu FROM KhuyenMai WHERE maKM=?";
        try (Connection con = DbUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maKM);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                KhuyenMai item = mapKhuyenMai(rs);
                item.setDichVuApDung(lookupDichVu(con, maKM));
                item.setLinhKienApDung(lookupLinhKien(con, maKM));
                return item;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi đọc chương trình khuyến mãi: " + e.getMessage(), e);
        }
    }

    /** Khuyến mãi đang có hiệu lực để chọn khi lập hóa đơn. */
    public List<KhuyenMai> layKhuyenMaiHieuLuc() {
        return layDanhSach("WHERE ngayBatDau <= GETDATE() AND ngayKetThuc >= GETDATE() ORDER BY ngayBatDau DESC, maKM");
    }

    public List<KhuyenMai> layTatCa() {
        return layDanhSach("ORDER BY ngayBatDau DESC, maKM");
    }

    private List<KhuyenMai> layDanhSach(String dieuKien) {
        String sql = "SELECT maKM, maNV, tenChuongTrinh, mucGiamPhanTram, ngayBatDau, ngayKetThuc, ghiChu "
                + "FROM KhuyenMai " + dieuKien;
        List<KhuyenMai> result = new ArrayList<>();
        try (Connection con = DbUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(mapKhuyenMai(rs));
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy danh sách khuyến mãi: " + e.getMessage(), e);
        }
    }

    /** Kiểm tra xem chương trình đã được gắn vào hóa đơn chưa. */
    public boolean daApDungHoaDon(String maKM) {
        try (Connection con = DbUtil.getConnection(); PreparedStatement ps = con.prepareStatement(
                "SELECT CASE WHEN EXISTS (SELECT 1 FROM HoaDon WHERE maKM=?) THEN 1 ELSE 0 END")) {
            ps.setString(1, maKM);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() && rs.getBoolean(1); }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi kiểm tra lịch sử áp dụng khuyến mãi: " + e.getMessage(), e);
        }
    }

    /** Tạo chương trình và các bảng nối trong một giao dịch; mã KM do sequence mặc định ở DB cấp. */
    public KhuyenMai them(KhuyenMai item) {
        String sql = "SET NOCOUNT ON; DECLARE @maMoi TABLE(maKM VARCHAR(10)); "
                + "INSERT INTO KhuyenMai (maNV, tenChuongTrinh, mucGiamPhanTram, ngayBatDau, ngayKetThuc, ghiChu) "
                + "OUTPUT INSERTED.maKM INTO @maMoi VALUES (?, ?, ?, ?, ?, ?); SELECT maKM FROM @maMoi";
        try (Connection con = DbUtil.getConnection()) {
            con.setAutoCommit(false);
            try {
                String maKM;
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    bindHeader(ps, item);
                    boolean hasResult = ps.execute();
                    while (!hasResult && ps.getUpdateCount() != -1) hasResult = ps.getMoreResults();
                    if (!hasResult) throw new IllegalStateException("Cơ sở dữ liệu không trả mã khuyến mãi.");
                    try (ResultSet rs = ps.getResultSet()) {
                        if (!rs.next()) throw new IllegalStateException("Cơ sở dữ liệu không trả mã khuyến mãi.");
                        maKM = rs.getString(1);
                    }
                }
                replaceTargets(con, maKM, item.getDichVuApDung(), item.getLinhKienApDung());
                con.commit();
                return timTheoMa(maKM);
            } catch (Exception e) {
                rollback(con, e);
                throw e;
            }
        } catch (SQLException e) {
            throw writeError("thêm", e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi thêm chương trình khuyến mãi: " + e.getMessage(), e);
        }
    }

    /** Cập nhật chương trình; với chương trình đã dùng, chỉ đổi tên/ghi chú theo trigger V1. */
    public KhuyenMai capNhat(KhuyenMai item) {
        try (Connection con = DbUtil.getConnection()) {
            con.setAutoCommit(false);
            try {
                boolean daDung = daApDungHoaDon(con, item.getMaKM());
                String sql = daDung
                        ? "UPDATE KhuyenMai SET tenChuongTrinh=?, ghiChu=? WHERE maKM=?"
                        : "UPDATE KhuyenMai SET tenChuongTrinh=?, mucGiamPhanTram=?, ngayBatDau=?, ngayKetThuc=?, ghiChu=? WHERE maKM=?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    if (daDung) {
                        ps.setNString(1, item.getTenChuongTrinh());
                        setNote(ps, 2, item.getGhiChu());
                        ps.setString(3, item.getMaKM());
                    } else {
                        ps.setNString(1, item.getTenChuongTrinh());
                        ps.setDouble(2, item.getMucGiamPhanTram());
                        ps.setDate(3, toSqlDate(item.getNgayBatDau()));
                        ps.setDate(4, toSqlDate(item.getNgayKetThuc()));
                        setNote(ps, 5, item.getGhiChu());
                        ps.setString(6, item.getMaKM());
                    }
                    if (ps.executeUpdate() == 0) throw new IllegalArgumentException("Chương trình khuyến mãi không còn tồn tại.");
                }
                if (!daDung) replaceTargets(con, item.getMaKM(), item.getDichVuApDung(), item.getLinhKienApDung());
                con.commit();
                return timTheoMa(item.getMaKM());
            } catch (Exception e) {
                rollback(con, e);
                throw e;
            }
        } catch (SQLException e) {
            throw writeError("cập nhật", e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi cập nhật chương trình khuyến mãi: " + e.getMessage(), e);
        }
    }

    private static void bindHeader(PreparedStatement ps, KhuyenMai item) throws SQLException {
        ps.setString(1, item.getNhanVien().getMaNV());
        ps.setNString(2, item.getTenChuongTrinh());
        ps.setDouble(3, item.getMucGiamPhanTram());
        ps.setDate(4, toSqlDate(item.getNgayBatDau()));
        ps.setDate(5, toSqlDate(item.getNgayKetThuc()));
        setNote(ps, 6, item.getGhiChu());
    }

    private static void replaceTargets(Connection con, String maKM, Set<DichVu> services, Set<LinhKien> parts) throws SQLException {
        try (PreparedStatement dvDelete = con.prepareStatement("DELETE FROM KhuyenMai_DichVu WHERE maKM=?");
             PreparedStatement lkDelete = con.prepareStatement("DELETE FROM KhuyenMai_LinhKien WHERE maKM=?")) {
            dvDelete.setString(1, maKM);
            dvDelete.executeUpdate();
            lkDelete.setString(1, maKM);
            lkDelete.executeUpdate();
        }
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO KhuyenMai_DichVu(maKM,maDV) VALUES(?,?)")) {
            for (DichVu dv : services) { ps.setString(1, maKM); ps.setString(2, dv.getMaDV()); ps.addBatch(); }
            ps.executeBatch();
        }
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO KhuyenMai_LinhKien(maKM,maLK) VALUES(?,?)")) {
            for (LinhKien lk : parts) { ps.setString(1, maKM); ps.setString(2, lk.getMaLK()); ps.addBatch(); }
            ps.executeBatch();
        }
    }

    private static Set<DichVu> lookupDichVu(Connection con, String maKM) throws SQLException {
        String sql = "SELECT d.maDV,d.tenDV,d.donGia,d.donViTinh,d.moTa,d.dangCungCap FROM KhuyenMai_DichVu k JOIN DichVu d ON d.maDV=k.maDV WHERE k.maKM=? ORDER BY d.maDV";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maKM);
            try (ResultSet rs = ps.executeQuery()) {
                Set<DichVu> result = new LinkedHashSet<>();
                while (rs.next()) {
                    DichVu dv = new DichVu();
                    dv.setMaDVRaw(rs.getString("maDV")); dv.setTenDVRaw(rs.getString("tenDV"));
                    dv.setDonGiaRaw(rs.getDouble("donGia")); dv.setDonViTinhRaw(rs.getString("donViTinh"));
                    dv.setMoTaRaw(rs.getString("moTa")); dv.setDangCungCapRaw(rs.getBoolean("dangCungCap"));
                    result.add(dv);
                }
                return result;
            }
        }
    }

    private static Set<LinhKien> lookupLinhKien(Connection con, String maKM) throws SQLException {
        String sql = "SELECT l.maLK,l.tenLK,l.donGia,l.donViTinh,l.soLuongTon,l.dangKinhDoanh FROM KhuyenMai_LinhKien k JOIN LinhKien l ON l.maLK=k.maLK WHERE k.maKM=? ORDER BY l.maLK";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maKM);
            try (ResultSet rs = ps.executeQuery()) {
                Set<LinhKien> result = new LinkedHashSet<>();
                while (rs.next()) {
                    LinhKien lk = new LinhKien();
                    lk.setMaLKRaw(rs.getString("maLK")); lk.setTenLKRaw(rs.getString("tenLK"));
                    lk.setDonGiaRaw(rs.getDouble("donGia")); lk.setDonViTinhRaw(rs.getString("donViTinh"));
                    lk.setSoLuongTonRaw(rs.getInt("soLuongTon")); lk.setDangKinhDoanhRaw(rs.getBoolean("dangKinhDoanh"));
                    result.add(lk);
                }
                return result;
            }
        }
    }

    private static KhuyenMai mapKhuyenMai(ResultSet rs) throws SQLException {
        NhanVien nhanVien = new NhanVien();
        nhanVien.setMaNVRaw(rs.getString("maNV"));
        nhanVien.setVaiTroRaw(VaiTro.CHU_CUA_HANG);
        KhuyenMai item = new KhuyenMai();
        item.setMaKM(rs.getString("maKM"));
        item.setNhanVien(nhanVien);
        item.setTenChuongTrinh(rs.getNString("tenChuongTrinh"));
        item.setMucGiamPhanTram(rs.getDouble("mucGiamPhanTram"));
        item.setNgayBatDau(rs.getDate("ngayBatDau"));
        item.setNgayKetThuc(rs.getDate("ngayKetThuc"));
        item.setGhiChu(rs.getNString("ghiChu"));
        return item;
    }

    private static Date toSqlDate(java.util.Date value) { return new Date(value.getTime()); }
    private static void setNote(PreparedStatement ps, int index, String note) throws SQLException {
        if (note == null) ps.setNull(index, Types.NVARCHAR); else ps.setNString(index, note);
    }
    private static boolean daApDungHoaDon(Connection con, String maKM) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT CASE WHEN EXISTS (SELECT 1 FROM HoaDon WHERE maKM=?) THEN 1 ELSE 0 END")) {
            ps.setString(1, maKM);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() && rs.getBoolean(1); }
        }
    }
    private static void rollback(Connection con, Exception cause) {
        try { con.rollback(); } catch (SQLException rollbackError) { cause.addSuppressed(rollbackError); }
    }
    private static RuntimeException writeError(String action, SQLException e) {
        if (e.getErrorCode() == 2627 || e.getErrorCode() == 2601) return new IllegalArgumentException("Tên chương trình khuyến mãi đã tồn tại.", e);
        if (e.getErrorCode() == 50172 || e.getErrorCode() == 50181 || e.getErrorCode() == 50182) {
            return new IllegalStateException(e.getMessage(), e);
        }
        return new RuntimeException("Lỗi " + action + " chương trình khuyến mãi: " + e.getMessage(), e);
    }
}
