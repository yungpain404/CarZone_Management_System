package dao.impl;

import connectDB.DbUtil;
import dao.HoaDonDao;
import entity.*;
import enums.PhuongThucThanhToan;
import enums.TrangThaiThanhToan;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Triển khai {@link HoaDonDao} bằng JDBC thuần (DriverManager).
 */
public class HoaDonDaoImpl implements HoaDonDao {

    private static final String SELECT_BASE =
            "SELECT h.maHoaDon, h.maPhieu, h.maNV, h.maKM, h.ngayLap, h.phuongThucTT, " +
            "       h.trangThaiThanhToan, h.thoiGianThanhToan, h.maGiaoDich, " +
            "       v.tongTienDichVu, v.tongTienLinhKien, v.tienGiam, v.thue, v.tongThanhToan, " +
            "       p.bienSoXe, x.hangXe, x.model, kh.maKH, kh.hoTen AS tenKhachHang, kh.soDienThoai, nv.hoTen AS tenNhanVien " +
            "FROM HoaDon h " +
            "LEFT JOIN vw_HoaDon_TongTien v ON h.maHoaDon = v.maHoaDon " +
            "LEFT JOIN PhieuTiepNhanSuaChua p ON h.maPhieu = p.maPhieu " +
            "LEFT JOIN Xe x ON p.bienSoXe = x.bienSoXe " +
            "LEFT JOIN KhachHang kh ON x.maKH = kh.maKH " +
            "LEFT JOIN NhanVien nv ON h.maNV = nv.maNV ";

    @Override
    public HoaDon timTheoMa(String maHoaDon) {
        String sql = SELECT_BASE + "WHERE h.maHoaDon = ?";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maHoaDon);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm hóa đơn theo mã: " + e.getMessage(), e);
        }
    }

    @Override
    public HoaDon timTheoPhieu(String maPhieu) {
        String sql = SELECT_BASE + "WHERE h.maPhieu = ?";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maPhieu);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm hóa đơn theo phiếu: " + e.getMessage(), e);
        }
    }

    @Override
    public List<HoaDon> layTatCa() {
        String sql = SELECT_BASE + "ORDER BY h.ngayLap DESC";
        List<HoaDon> list = new ArrayList<>();
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy danh sách hóa đơn: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<HoaDon> timTheoTrangThai(TrangThaiThanhToan trangThai) {
        String sql = SELECT_BASE + "WHERE h.trangThaiThanhToan = ? ORDER BY h.ngayLap DESC";
        List<HoaDon> list = new ArrayList<>();
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, trangThai.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm hóa đơn theo trạng thái: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<HoaDon> layHoaDonDuDieuKienDoiTra() {
        // Hóa đơn đã thanh toán và trong vòng 7 ngày kể từ ngayLap
        String sql = SELECT_BASE +
                     "WHERE h.trangThaiThanhToan = 'DA_THANH_TOAN' " +
                     "  AND DATEDIFF(day, h.ngayLap, SYSDATETIME()) <= 7 " +
                     "ORDER BY h.ngayLap DESC";
        List<HoaDon> list = new ArrayList<>();
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy hóa đơn đủ điều kiện đổi trả: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public String lapHoaDon(String maPhieu, String maNV, String maKM) {
        String sql = "{call sp_LapHoaDon(?, ?, ?, ?)}";
        try (Connection con = DbUtil.getConnection();
             CallableStatement cs = con.prepareCall(sql)) {
            cs.setString(1, maPhieu);
            cs.setString(2, maNV);
            if (maKM != null && !maKM.isBlank()) {
                cs.setString(3, maKM);
            } else {
                cs.setNull(3, Types.VARCHAR);
            }
            cs.registerOutParameter(4, Types.CHAR);
            cs.execute();
            return cs.getString(4).trim();
        } catch (SQLException e) {
            if (e.getErrorCode() == 50131) {
                throw new RuntimeException("Chỉ tiếp nhận viên mới có quyền lập hóa đơn.");
            } else if (e.getErrorCode() == 50132) {
                throw new RuntimeException("Chỉ phiếu sửa chữa đã hoàn tất mới được phép lập hóa đơn.");
            } else if (e.getErrorCode() == 50133) {
                throw new RuntimeException("Chương trình khuyến mãi không còn hiệu lực tại ngày lập.");
            }
            throw new RuntimeException("Lỗi lập hóa đơn: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean thanhToanHoaDon(String maHoaDon, PhuongThucThanhToan phuongThucTT, String maGiaoDich) {
        String sql = "{call sp_ThanhToanHoaDon(?, ?, ?)}";
        try (Connection con = DbUtil.getConnection();
             CallableStatement cs = con.prepareCall(sql)) {
            cs.setString(1, maHoaDon);
            cs.setString(2, phuongThucTT.name());
            if (phuongThucTT == PhuongThucThanhToan.CHUYEN_KHOAN) {
                if (maGiaoDich == null || maGiaoDich.isBlank()) {
                    throw new RuntimeException("Thanh toán chuyển khoản bắt buộc phải có mã giao dịch.");
                }
                cs.setString(3, maGiaoDich.trim());
            } else {
                cs.setNull(3, Types.VARCHAR);
            }
            cs.execute();
            return true;
        } catch (SQLException e) {
            if (e.getErrorCode() == 50211) {
                throw new RuntimeException("Hóa đơn không tồn tại hoặc đã được thanh toán trước đó.");
            }
            throw new RuntimeException("Lỗi thanh toán hóa đơn: " + e.getMessage(), e);
        }
    }

    @Override
    public List<HoaDon> timKiem(String tuKhoa, TrangThaiThanhToan trangThai) {
        StringBuilder sql = new StringBuilder(SELECT_BASE).append("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (trangThai != null) {
            sql.append("AND h.trangThaiThanhToan = ? ");
            params.add(trangThai.name());
        }
        if (tuKhoa != null && !tuKhoa.isBlank()) {
            sql.append("AND (h.maHoaDon LIKE ? OR h.maPhieu LIKE ? OR p.bienSoXe LIKE ? OR kh.hoTen LIKE ? OR kh.soDienThoai LIKE ?) ");
            String p = "%" + tuKhoa.trim() + "%";
            for (int i = 0; i < 5; i++) params.add(p);
        }
        sql.append("ORDER BY h.ngayLap DESC");

        List<HoaDon> list = new ArrayList<>();
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm kiếm hóa đơn: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public boolean them(HoaDon hd) {
        String sql = "INSERT INTO HoaDon (maHoaDon, maPhieu, maNV, maKM, ngayLap, phuongThucTT, trangThaiThanhToan, thoiGianThanhToan, maGiaoDich) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, hd.getMaHoaDon());
            ps.setString(2, hd.getPhieuSuaChua() != null ? hd.getPhieuSuaChua().getMaPhieu() : null);
            ps.setString(3, hd.getNhanVien() != null ? hd.getNhanVien().getMaNV() : null);
            ps.setString(4, hd.getKhuyenMai() != null ? hd.getKhuyenMai().getMaKM() : null);
            ps.setTimestamp(5, hd.getNgayLap() != null ? new Timestamp(hd.getNgayLap().getTime()) : new Timestamp(System.currentTimeMillis()));
            ps.setString(6, hd.getPhuongThucTT() != null ? hd.getPhuongThucTT().name() : null);
            ps.setString(7, hd.getTrangThaiThanhToan() != null ? hd.getTrangThaiThanhToan().name() : TrangThaiThanhToan.CHUA_THANH_TOAN.name());
            ps.setTimestamp(8, hd.getThoiGianThanhToan() != null ? new Timestamp(hd.getThoiGianThanhToan().getTime()) : null);
            ps.setString(9, hd.getMaGiaoDich());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi thêm hóa đơn: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean capNhatKhuyenMai(String maHoaDon, String maKM) {
        String sql = "UPDATE HoaDon SET maKM = ? WHERE maHoaDon = ? AND trangThaiThanhToan = 'CHUA_THANH_TOAN'";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (maKM != null && !maKM.isBlank()) {
                ps.setString(1, maKM.trim());
            } else {
                ps.setNull(1, Types.VARCHAR);
            }
            ps.setString(2, maHoaDon.trim());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật khuyến mãi: " + e.getMessage(), e);
        }
    }

    private HoaDon mapRow(ResultSet rs) throws SQLException {
        HoaDon hd = new HoaDon(rs.getString("maHoaDon").trim());

        String maPhieu = rs.getString("maPhieu");
        if (maPhieu != null) {
            PhieuTiepNhanSuaChua p = new PhieuTiepNhanSuaChua(maPhieu);
            String bienSo = rs.getString("bienSoXe");
            if (bienSo != null) {
                Xe xe = new Xe(bienSo);
                xe.setHangXeRaw(rs.getString("hangXe"));
                xe.setModelRaw(rs.getString("model"));

                String maKH = rs.getString("maKH");
                if (maKH != null) {
                    KhachHang kh = new KhachHang();
                    kh.setMaKHRaw(maKH);
                    kh.setHoTenRaw(rs.getString("tenKhachHang"));
                    kh.setSoDienThoaiRaw(rs.getString("soDienThoai"));
                    xe.setChuXeRaw(kh);
                }
                p.setXeRaw(xe);
            }
            hd.setPhieuSuaChuaRaw(p);
        }

        String maNV = rs.getString("maNV");
        if (maNV != null) {
            NhanVien nv = new NhanVien(maNV);
            String tenNV = rs.getString("tenNhanVien");
            if (tenNV != null) nv.setHoTenRaw(tenNV);
            hd.setNhanVienRaw(nv);
        }

        String maKM = rs.getString("maKM");
        if (maKM != null) hd.setKhuyenMaiRaw(new KhuyenMai(maKM));

        Timestamp ngayLap = rs.getTimestamp("ngayLap");
        if (ngayLap != null) hd.setNgayLapRaw(new java.util.Date(ngayLap.getTime()));

        String pt = rs.getString("phuongThucTT");
        if (pt != null) hd.setPhuongThucTTRaw(PhuongThucThanhToan.valueOf(pt));

        String tt = rs.getString("trangThaiThanhToan");
        if (tt != null) hd.setTrangThaiThanhToanRaw(TrangThaiThanhToan.valueOf(tt));

        Timestamp thoiGianTT = rs.getTimestamp("thoiGianThanhToan");
        if (thoiGianTT != null) hd.setThoiGianThanhToanRaw(new java.util.Date(thoiGianTT.getTime()));

        String maGD = rs.getString("maGiaoDich");
        if (maGD != null) hd.setMaGiaoDichRaw(maGD);

        // Computed values from view vw_HoaDon_TongTien
        hd.setTongTienDichVuRaw(rs.getDouble("tongTienDichVu"));
        hd.setTongTienLinhKienRaw(rs.getDouble("tongTienLinhKien"));
        hd.setTienGiamRaw(rs.getDouble("tienGiam"));
        hd.setThueRaw(rs.getDouble("thue"));
        hd.setTongThanhToanRaw(rs.getDouble("tongThanhToan"));

        return hd;
    }
}
