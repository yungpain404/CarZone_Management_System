package dao.impl;

import connectDB.DbUtil;
import dao.PhieuTiepNhanSuaChuaDao;
import entity.*;
import enums.TrangThaiPhieu;
import enums.VaiTro;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Triển khai PhieuTiepNhanSuaChuaDao bằng JDBC thuần.
 */
public class PhieuTiepNhanSuaChuaDaoImpl implements PhieuTiepNhanSuaChuaDao {

    private static final String SELECT_BASE =
            "SELECT p.maPhieu, p.bienSoXe, p.maNVTiepNhan, p.maNVKyThuat, p.ngayGioTiepNhan, p.yeuCauBanDau, p.trangThai, " +
            "       x.hangXe, x.model, x.mauSac, x.namSanXuat, x.soKmHienTai, x.maKH, " +
            "       kh.hoTen AS tenChuXe, kh.soDienThoai AS sdtChuXe, " +
            "       nvtn.hoTen AS tenNVTiepNhan, " +
            "       nvkt.hoTen AS tenNVKyThuat " +
            "FROM PhieuTiepNhanSuaChua p " +
            "JOIN Xe x ON p.bienSoXe = x.bienSoXe " +
            "JOIN KhachHang kh ON x.maKH = kh.maKH " +
            "JOIN NhanVien nvtn ON p.maNVTiepNhan = nvtn.maNV " +
            "LEFT JOIN NhanVien nvkt ON p.maNVKyThuat = nvkt.maNV ";

    @Override
    public PhieuTiepNhanSuaChua timTheoMa(String maPhieu) {
        String sql = SELECT_BASE + "WHERE p.maPhieu = ?";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maPhieu);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm phiếu tiếp nhận: " + e.getMessage(), e);
        }
    }

    @Override
    public List<PhieuTiepNhanSuaChua> layTatCa() {
        String sql = SELECT_BASE + "ORDER BY p.ngayGioTiepNhan DESC";
        List<PhieuTiepNhanSuaChua> list = new ArrayList<>();
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy danh sách phiếu: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<PhieuTiepNhanSuaChua> layTheoTiepNhanVien(String maNVTiepNhan) {
        String sql = SELECT_BASE + "WHERE p.maNVTiepNhan = ? ORDER BY p.ngayGioTiepNhan DESC";
        List<PhieuTiepNhanSuaChua> list = new ArrayList<>();
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maNVTiepNhan);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy phiếu theo nhân viên tiếp nhận: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<PhieuTiepNhanSuaChua> layDanhSachDuDieuKienLapHoaDon() {
        String sql = SELECT_BASE +
                     "WHERE p.trangThai = 'HOAN_TAT' " +
                     "  AND NOT EXISTS (SELECT 1 FROM HoaDon hd WHERE hd.maPhieu = p.maPhieu) " +
                     "ORDER BY p.ngayGioTiepNhan DESC";
        List<PhieuTiepNhanSuaChua> list = new ArrayList<>();
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy danh sách phiếu đủ điều kiện lập hóa đơn: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public String lapPhieuTiepNhan(PhieuTiepNhanSuaChua phieu, List<ChiTietDichVu> dsDichVu) {
        String callProc = "{call sp_LapPhieuTiepNhan(?, ?, ?, ?, ?)}";
        try (Connection con = DbUtil.getConnection()) {
            con.setAutoCommit(false);
            String maPhieu;
            try (CallableStatement cs = con.prepareCall(callProc)) {
                cs.setString(1, phieu.getXe().getBienSoXe());
                cs.setString(2, phieu.getNhanVienLap().getMaNV());
                if (phieu.getNhanVienSuaChua() != null && phieu.getNhanVienSuaChua().getMaNV() != null) {
                    cs.setString(3, phieu.getNhanVienSuaChua().getMaNV());
                } else {
                    cs.setNull(3, Types.VARCHAR);
                }
                cs.setString(4, phieu.getYeuCauBanDau());
                cs.registerOutParameter(5, Types.VARCHAR);
                cs.execute();
                maPhieu = cs.getString(5);
            }

            // Lưu các dịch vụ đề nghị ban đầu (nếu có)
            if (dsDichVu != null && !dsDichVu.isEmpty()) {
                String insertDv = "INSERT INTO ChiTietDichVu (maPhieu, maDV, soLuong, donGiaThucHien, ghiChu) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement psDv = con.prepareStatement(insertDv)) {
                    for (ChiTietDichVu ct : dsDichVu) {
                        psDv.setString(1, maPhieu);
                        psDv.setString(2, ct.getDichVu().getMaDV());
                        psDv.setInt(3, ct.getSoLuong() > 0 ? ct.getSoLuong() : 1);
                        psDv.setDouble(4, ct.getDonGiaThucHien());
                        psDv.setString(5, ct.getGhiChu());
                        psDv.addBatch();
                    }
                    psDv.executeBatch();
                }
            }

            con.commit();
            phieu.setMaPhieuRaw(maPhieu);
            return maPhieu;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tạo phiếu tiếp nhận sửa chữa: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean capNhatTrangThai(String maPhieu, TrangThaiPhieu trangThai) {
        String sql = "UPDATE PhieuTiepNhanSuaChua SET trangThai = ? WHERE maPhieu = ?";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, trangThai.name());
            ps.setString(2, maPhieu);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật trạng thái phiếu: " + e.getMessage(), e);
        }
    }

    @Override
    public List<PhieuTiepNhanSuaChua> timKiem(String tuKhoa, String maNVTiepNhan, TrangThaiPhieu trangThai) {
        StringBuilder sql = new StringBuilder(SELECT_BASE).append("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (maNVTiepNhan != null && !maNVTiepNhan.isBlank()) {
            sql.append("AND p.maNVTiepNhan = ? ");
            params.add(maNVTiepNhan);
        }
        if (trangThai != null) {
            sql.append("AND p.trangThai = ? ");
            params.add(trangThai.name());
        }
        if (tuKhoa != null && !tuKhoa.isBlank()) {
            sql.append("AND (p.maPhieu LIKE ? OR p.bienSoXe LIKE ? OR kh.hoTen LIKE ? OR kh.soDienThoai LIKE ?) ");
            String p = "%" + tuKhoa.trim() + "%";
            params.add(p);
            params.add(p);
            params.add(p);
            params.add(p);
        }
        sql.append("ORDER BY p.ngayGioTiepNhan DESC");

        List<PhieuTiepNhanSuaChua> list = new ArrayList<>();
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
            throw new RuntimeException("Lỗi tìm kiếm phiếu tiếp nhận: " + e.getMessage(), e);
        }
        return list;
    }

    private PhieuTiepNhanSuaChua mapRow(ResultSet rs) throws SQLException {
        PhieuTiepNhanSuaChua p = new PhieuTiepNhanSuaChua();
        p.setMaPhieuRaw(rs.getString("maPhieu"));

        Xe xe = new Xe();
        xe.setBienSoXeRaw(rs.getString("bienSoXe"));
        xe.setHangXeRaw(rs.getString("hangXe"));
        xe.setModelRaw(rs.getString("model"));
        xe.setMauSacRaw(rs.getString("mauSac"));
        xe.setNamSanXuatRaw(rs.getInt("namSanXuat"));
        xe.setSoKmHienTaiRaw(rs.getInt("soKmHienTai"));

        KhachHang kh = new KhachHang();
        kh.setMaKHRaw(rs.getString("maKH"));
        kh.setHoTenRaw(rs.getString("tenChuXe"));
        kh.setSoDienThoaiRaw(rs.getString("sdtChuXe"));
        xe.setChuXeRaw(kh);
        p.setXeRaw(xe);

        NhanVien nvtn = new NhanVien(rs.getString("maNVTiepNhan"));
        nvtn.setHoTenRaw(rs.getString("tenNVTiepNhan"));
        nvtn.setVaiTroRaw(VaiTro.TIEP_NHAN_VIEN);
        p.setNhanVienLapRaw(nvtn);

        String maKt = rs.getString("maNVKyThuat");
        if (maKt != null) {
            NhanVien nvkt = new NhanVien(maKt);
            nvkt.setHoTenRaw(rs.getString("tenNVKyThuat"));
            nvkt.setVaiTroRaw(VaiTro.KY_THUAT_VIEN);
            p.setNhanVienSuaChuaRaw(nvkt);
        }

        Timestamp ts = rs.getTimestamp("ngayGioTiepNhan");
        if (ts != null) {
            p.setNgayGioTiepNhanRaw(new java.util.Date(ts.getTime()));
        }

        p.setYeuCauBanDauRaw(rs.getString("yeuCauBanDau"));

        String tt = rs.getString("trangThai");
        if (tt != null) {
            p.setTrangThaiRaw(TrangThaiPhieu.valueOf(tt));
        }

        return p;
    }
}
