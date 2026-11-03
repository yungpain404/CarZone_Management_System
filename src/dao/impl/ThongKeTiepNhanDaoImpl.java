package dao.impl;

import connectDB.DbUtil;
import dao.ThongKeTiepNhanDao;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

/**
 * Triển khai ThongKeTiepNhanDao bằng JDBC thuần.
 */
public class ThongKeTiepNhanDaoImpl implements ThongKeTiepNhanDao {

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd/MM/yyyy");
    private static final SimpleDateFormat DAY_KEY_FMT = new SimpleDateFormat("dd/MM");

    @Override
    public ThongKeData layThongKe(String maNVTiepNhan, Date tuNgay, Date denNgay, String trangThai) {
        ThongKeData data = new ThongKeData();
        data.xuHuongNgay = new LinkedHashMap<>();
        data.phanBoTrangThai = new LinkedHashMap<>();
        data.danhSachPhieu = new ArrayList<>();

        // Khởi tạo các nhóm trạng thái
        data.phanBoTrangThai.put("Chờ sửa chữa", 0);
        data.phanBoTrangThai.put("Đang sửa chữa", 0);
        data.phanBoTrangThai.put("Chờ linh kiện", 0);
        data.phanBoTrangThai.put("Hoàn tất", 0);

        if (!DbUtil.isDatabaseOnline()) {
            return data;
        }

        StringBuilder sql = new StringBuilder(
                "SELECT p.maPhieu, p.bienSoXe, p.ngayGioTiepNhan, p.trangThai, " +
                "       kh.hoTen AS tenKhach, nvkt.hoTen AS tenKtv " +
                "FROM PhieuTiepNhanSuaChua p " +
                "JOIN Xe x ON p.bienSoXe = x.bienSoXe " +
                "JOIN KhachHang kh ON x.maKH = kh.maKH " +
                "LEFT JOIN NhanVien nvkt ON p.maNVKyThuat = nvkt.maNV " +
                "WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if (maNVTiepNhan != null && !maNVTiepNhan.isBlank()) {
            sql.append("AND p.maNVTiepNhan = ? ");
            params.add(maNVTiepNhan);
        }
        if (tuNgay != null) {
            sql.append("AND p.ngayGioTiepNhan >= ? ");
            params.add(new Timestamp(tuNgay.getTime()));
        }
        if (denNgay != null) {
            // Cho đến hết ngày (23:59:59.999)
            Calendar c = Calendar.getInstance();
            c.setTime(denNgay);
            c.set(Calendar.HOUR_OF_DAY, 23);
            c.set(Calendar.MINUTE, 59);
            c.set(Calendar.SECOND, 59);
            sql.append("AND p.ngayGioTiepNhan <= ? ");
            params.add(new Timestamp(c.getTimeInMillis()));
        }
        if (trangThai != null && !trangThai.isBlank() && !"Tất cả".equalsIgnoreCase(trangThai)) {
            String dbStatus = switch (trangThai) {
                case "Chờ sửa chữa" -> "CHO_SUA";
                case "Đang sửa chữa" -> "DANG_SUA";
                case "Chờ linh kiện" -> "CHO_LINH_KIEN";
                case "Hoàn tất" -> "HOAN_TAT";
                default -> trangThai;
            };
            sql.append("AND p.trangThai = ? ");
            params.add(dbStatus);
        }
        sql.append("ORDER BY p.ngayGioTiepNhan DESC");

        Set<String> setXe = new HashSet<>();
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String ma = rs.getString("maPhieu");
                    String bienSo = rs.getString("bienSoXe");
                    Timestamp ts = rs.getTimestamp("ngayGioTiepNhan");
                    String tt = rs.getString("trangThai");
                    String kh = rs.getString("tenKhach");
                    String ktv = rs.getString("tenKtv");
                    if (ktv == null) ktv = "Chưa phân công";

                    data.tongPhieu++;
                    if (bienSo != null) setXe.add(bienSo);

                    String ttLabel = switch (tt) {
                        case "CHO_SUA" -> "Chờ sửa chữa";
                        case "DANG_SUA" -> "Đang sửa chữa";
                        case "CHO_LINH_KIEN" -> "Chờ linh kiện";
                        case "HOAN_TAT" -> "Hoàn tất";
                        default -> tt;
                    };
                    data.phanBoTrangThai.put(ttLabel, data.phanBoTrangThai.getOrDefault(ttLabel, 0) + 1);

                    if ("CHO_SUA".equals(tt)) {
                        data.choXuLy++;
                    }

                    String dayKey = ts != null ? DAY_KEY_FMT.format(ts) : "N/A";
                    data.xuHuongNgay.put(dayKey, data.xuHuongNgay.getOrDefault(dayKey, 0) + 1);

                    String ngayStr = ts != null ? DATE_FMT.format(ts) : "--/--/----";
                    data.danhSachPhieu.add(new PhieuRow(ma, ngayStr, kh, bienSo, ttLabel, ktv));
                }
            }
            data.soXe = setXe.size();

            // Đếm khách hàng mới
            String countKhSql = "SELECT COUNT(*) FROM KhachHang";
            try (PreparedStatement psKh = con.prepareStatement(countKhSql);
                 ResultSet rsKh = psKh.executeQuery()) {
                if (rsKh.next()) {
                    data.khachHangMoi = rsKh.getInt(1);
                }
            }

        } catch (SQLException e) {
            System.err.println("Lỗi thống kê tiếp nhận: " + e.getMessage());
        }

        return data;
    }
}
