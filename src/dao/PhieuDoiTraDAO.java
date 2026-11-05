package dao;

import connectDB.DbUtil;
import entity.*;
import enums.HuongXuLyDoiTra;
import enums.VaiTro;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Triển khai PhieuDoiTraDao bằng JDBC thuần.
 */
public class PhieuDoiTraDAO {

    private static final String SELECT_BASE =
            "SELECT p.maPhieuDT, p.maHoaDon, p.maNV, p.ngayLap, p.lyDoDoiTra, " +
            "       v.soTienHoanTra, nv.hoTen AS tenNhanVien, " +
            "       h.maPhieu, pt.bienSoXe, kh.hoTen AS tenKhachHang " +
            "FROM PhieuDoiTra p " +
            "LEFT JOIN vw_PhieuDoiTra_HoanTien v ON p.maPhieuDT = v.maPhieuDT " +
            "LEFT JOIN NhanVien nv ON p.maNV = nv.maNV " +
            "LEFT JOIN HoaDon h ON p.maHoaDon = h.maHoaDon " +
            "LEFT JOIN PhieuTiepNhanSuaChua pt ON h.maPhieu = pt.maPhieu " +
            "LEFT JOIN Xe x ON pt.bienSoXe = x.bienSoXe " +
            "LEFT JOIN KhachHang kh ON x.maKH = kh.maKH ";
    public PhieuDoiTra timTheoMa(String maPhieuDT) {
        String sql = SELECT_BASE + "WHERE p.maPhieuDT = ?";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maPhieuDT);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm phiếu đổi trả: " + e.getMessage(), e);
        }
    }
    public List<PhieuDoiTra> layTatCa() {
        String sql = SELECT_BASE + "ORDER BY p.ngayLap DESC";
        List<PhieuDoiTra> list = new ArrayList<>();
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy danh sách phiếu đổi trả: " + e.getMessage(), e);
        }
        return list;
    }
    public List<PhieuDoiTra> layTheoTiepNhanVien(String maNV) {
        String sql = SELECT_BASE + "WHERE p.maNV = ? ORDER BY p.ngayLap DESC";
        List<PhieuDoiTra> list = new ArrayList<>();
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maNV);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy phiếu đổi trả của nhân viên: " + e.getMessage(), e);
        }
        return list;
    }
    public List<ChiTietDoiTra> layChiTiet(String maPhieuDT) {
        String sql = "SELECT c.maPhieuDT, c.stt, c.maPhieu, c.maDV, c.maLK, c.soLuong, c.donGia, c.huongXuLy, " +
                     "       COALESCE(d.tenDV, l.tenLK) AS tenHangMuc, " +
                     "       COALESCE(d.donViTinh, l.donViTinh) AS donViTinh " +
                     "FROM ChiTietDoiTra c " +
                     "LEFT JOIN DichVu d ON c.maDV = d.maDV " +
                     "LEFT JOIN LinhKien l ON c.maLK = l.maLK " +
                     "WHERE c.maPhieuDT = ? ORDER BY c.stt ASC";
        List<ChiTietDoiTra> list = new ArrayList<>();
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maPhieuDT);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ChiTietDoiTra ct = new ChiTietDoiTra();
                    ct.setPhieuDT(new PhieuDoiTra(rs.getString("maPhieuDT")));
                    ct.setSttRaw(rs.getInt("stt"));
                    ct.setMaPhieuRaw(rs.getString("maPhieu"));
                    ct.setMaDVRaw(rs.getString("maDV"));
                    ct.setMaLKRaw(rs.getString("maLK"));
                    ct.setSoLuongRaw(rs.getInt("soLuong"));
                    ct.setDonGiaRaw(rs.getDouble("donGia"));
                    String hx = rs.getString("huongXuLy");
                    if (hx != null) ct.setHuongXuLyRaw(HuongXuLyDoiTra.valueOf(hx));
                    ct.setTenHangMucRaw(rs.getString("tenHangMuc"));
                    ct.setDonViTinhRaw(rs.getString("donViTinh"));
                    list.add(ct);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy chi tiết đổi trả: " + e.getMessage(), e);
        }
        return list;
    }
    public String lapPhieuDoiTra(PhieuDoiTra pdt, List<ChiTietDoiTra> dsChiTiet) {
        if (dsChiTiet == null || dsChiTiet.isEmpty()) {
            throw new IllegalArgumentException("Phiếu đổi trả phải có ít nhất một hạng mục đổi trả.");
        }
        String callProc = "{call sp_LapPhieuDoiTra(?, ?, ?, ?)}";
        try (Connection con = DbUtil.getConnection()) {
            con.setAutoCommit(false);
            String maPhieuDT;
            try (CallableStatement cs = con.prepareCall(callProc)) {
                cs.setString(1, pdt.getHoaDon().getMaHoaDon());
                cs.setString(2, pdt.getNhanVien().getMaNV());
                cs.setString(3, pdt.getLyDoDoiTra());
                cs.registerOutParameter(4, Types.VARCHAR);
                cs.execute();
                maPhieuDT = cs.getString(4).trim();
            }

            // Insert chi tiết đổi trả
            String insertCt = "INSERT INTO ChiTietDoiTra (maPhieuDT, stt, maPhieu, maDV, maLK, soLuong, donGia, huongXuLy) " +
                              "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(insertCt)) {
                int stt = 1;
                for (ChiTietDoiTra ct : dsChiTiet) {
                    ps.setString(1, maPhieuDT);
                    ps.setInt(2, stt++);
                    ps.setString(3, ct.getMaPhieu());
                    ps.setString(4, ct.getMaDV());
                    ps.setString(5, ct.getMaLK());
                    ps.setInt(6, ct.getSoLuong());
                    ps.setDouble(7, ct.getDonGia());
                    ps.setString(8, ct.getHuongXuLy().name());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            con.commit();
            pdt.setMaPhieuDTRaw(maPhieuDT);
            return maPhieuDT;
        } catch (SQLException e) {
            if (e.getErrorCode() == 50151) {
                throw new RuntimeException("Chỉ tiếp nhận viên mới có quyền tạo phiếu đổi trả.");
            } else if (e.getErrorCode() == 50152) {
                throw new RuntimeException("Chỉ hóa đơn đã thanh toán mới được phép đổi trả.");
            } else if (e.getErrorCode() == 50153) {
                throw new RuntimeException("Hóa đơn đã quá hạn đổi trả quy định (7 ngày).");
            } else if (e.getErrorCode() == 50162 || e.getErrorCode() == 50163) {
                throw new RuntimeException("Số lượng đổi trả vượt quá số lượng trên hóa đơn.");
            }
            throw new RuntimeException("Lỗi lập phiếu đổi trả: " + e.getMessage(), e);
        }
    }
    public List<PhieuDoiTra> timKiem(String tuKhoa, String maNV) {
        StringBuilder sql = new StringBuilder(SELECT_BASE).append("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (maNV != null && !maNV.isBlank()) {
            sql.append("AND p.maNV = ? ");
            params.add(maNV);
        }
        if (tuKhoa != null && !tuKhoa.isBlank()) {
            sql.append("AND (p.maPhieuDT LIKE ? OR p.maHoaDon LIKE ? OR h.maPhieu LIKE ? OR pt.bienSoXe LIKE ? OR kh.hoTen LIKE ?) ");
            String p = "%" + tuKhoa.trim() + "%";
            for (int i = 0; i < 5; i++) params.add(p);
        }
        sql.append("ORDER BY p.ngayLap DESC");

        List<PhieuDoiTra> list = new ArrayList<>();
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
            throw new RuntimeException("Lỗi tìm kiếm phiếu đổi trả: " + e.getMessage(), e);
        }
        return list;
    }

    private PhieuDoiTra mapRow(ResultSet rs) throws SQLException {
        PhieuDoiTra p = new PhieuDoiTra();
        p.setMaPhieuDTRaw(rs.getString("maPhieuDT").trim());

        String maHD = rs.getString("maHoaDon");
        if (maHD != null) {
            HoaDon hd = new HoaDon(maHD.trim());
            String maPhieu = rs.getString("maPhieu");
            if (maPhieu != null) {
                PhieuTiepNhanSuaChua pt = new PhieuTiepNhanSuaChua(maPhieu);
                String bienSo = rs.getString("bienSoXe");
                if (bienSo != null) {
                    Xe xe = new Xe(bienSo);
                    String tenKh = rs.getString("tenKhachHang");
                    if (tenKh != null) {
                        KhachHang kh = new KhachHang();
                        kh.setHoTenRaw(tenKh);
                        xe.setChuXeRaw(kh);
                    }
                    pt.setXeRaw(xe);
                }
                hd.setPhieuSuaChuaRaw(pt);
            }
            p.setHoaDonRaw(hd);
        }

        String maNV = rs.getString("maNV");
        if (maNV != null) {
            NhanVien nv = new NhanVien(maNV);
            nv.setHoTenRaw(rs.getString("tenNhanVien"));
            nv.setVaiTroRaw(VaiTro.TIEP_NHAN_VIEN);
            p.setNhanVienRaw(nv);
        }

        Timestamp ts = rs.getTimestamp("ngayLap");
        if (ts != null) {
            p.setNgayLapRaw(new java.util.Date(ts.getTime()));
        }

        p.setLyDoDoiTraRaw(rs.getString("lyDoDoiTra"));
        p.setSoTienHoanTraRaw(rs.getDouble("soTienHoanTra"));

        return p;
    }
}

