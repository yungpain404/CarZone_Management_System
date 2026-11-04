package dao.impl;

import connectDB.DbUtil;
import dao.ThongKeTiepNhanDao;

import java.sql.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Triển khai ThongKeTiepNhanDao bằng JDBC thuần.
 */
public class ThongKeTiepNhanDaoImpl implements ThongKeTiepNhanDao {

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd/MM/yyyy HH:mm");
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
            Calendar cTu = Calendar.getInstance();
            cTu.setTime(tuNgay);
            cTu.set(Calendar.HOUR_OF_DAY, 0);
            cTu.set(Calendar.MINUTE, 0);
            cTu.set(Calendar.SECOND, 0);
            cTu.set(Calendar.MILLISECOND, 0);
            sql.append("AND p.ngayGioTiepNhan >= ? ");
            params.add(new Timestamp(cTu.getTimeInMillis()));
        }
        if (denNgay != null) {
            // Cho đến hết ngày (23:59:59.999)
            Calendar cDen = Calendar.getInstance();
            cDen.setTime(denNgay);
            cDen.set(Calendar.HOUR_OF_DAY, 23);
            cDen.set(Calendar.MINUTE, 59);
            cDen.set(Calendar.SECOND, 59);
            cDen.set(Calendar.MILLISECOND, 999);
            sql.append("AND p.ngayGioTiepNhan <= ? ");
            params.add(new Timestamp(cDen.getTimeInMillis()));
        }
        if (trangThai != null && !trangThai.isBlank() && !trangThai.contains("Tất cả")) {
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

    @Override
    public SoSanhThangData laySoSanhThang(String maNVTiepNhan) {
        SoSanhThangData data = new SoSanhThangData();
        Calendar now = Calendar.getInstance();
        data.thangNay = now.get(Calendar.MONTH) + 1;
        data.namNay = now.get(Calendar.YEAR);

        Calendar calTruoc = (Calendar) now.clone();
        calTruoc.add(Calendar.MONTH, -1);
        data.thangTruoc = calTruoc.get(Calendar.MONTH) + 1;
        data.namTruoc = calTruoc.get(Calendar.YEAR);

        if (!DbUtil.isDatabaseOnline()) {
            return data;
        }

        String sqlThang = "SELECT p.bienSoXe, x.maKH " +
                "FROM PhieuTiepNhanSuaChua p " +
                "JOIN Xe x ON p.bienSoXe = x.bienSoXe " +
                "WHERE MONTH(p.ngayGioTiepNhan) = ? AND YEAR(p.ngayGioTiepNhan) = ? " +
                (maNVTiepNhan != null && !maNVTiepNhan.isBlank() ? "AND p.maNVTiepNhan = ? " : "");

        try (Connection con = DbUtil.getConnection()) {
            // 1. Thống kê Tháng này
            Set<String> setXeThangNay = new HashSet<>();
            Set<String> setKhachThangNay = new HashSet<>();
            try (PreparedStatement ps = con.prepareStatement(sqlThang)) {
                ps.setInt(1, data.thangNay);
                ps.setInt(2, data.namNay);
                if (maNVTiepNhan != null && !maNVTiepNhan.isBlank()) {
                    ps.setString(3, maNVTiepNhan);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        data.phieuThangNay++;
                        String bs = rs.getString("bienSoXe");
                        String mkh = rs.getString("maKH");
                        if (bs != null) setXeThangNay.add(bs);
                        if (mkh != null) setKhachThangNay.add(mkh);
                    }
                }
            }
            data.xeThangNay = setXeThangNay.size();
            data.khachThangNay = setKhachThangNay.size();

            // 2. Thống kê Tháng trước
            Set<String> setXeThangTruoc = new HashSet<>();
            Set<String> setKhachThangTruoc = new HashSet<>();
            try (PreparedStatement ps = con.prepareStatement(sqlThang)) {
                ps.setInt(1, data.thangTruoc);
                ps.setInt(2, data.namTruoc);
                if (maNVTiepNhan != null && !maNVTiepNhan.isBlank()) {
                    ps.setString(3, maNVTiepNhan);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        data.phieuThangTruoc++;
                        String bs = rs.getString("bienSoXe");
                        String mkh = rs.getString("maKH");
                        if (bs != null) setXeThangTruoc.add(bs);
                        if (mkh != null) setKhachThangTruoc.add(mkh);
                    }
                }
            }
            data.xeThangTruoc = setXeThangTruoc.size();
            data.khachThangTruoc = setKhachThangTruoc.size();

            data.chenhLechPhieu = data.phieuThangNay - data.phieuThangTruoc;
            if (data.phieuThangTruoc > 0) {
                data.tyLeTangTruong = ((double) data.chenhLechPhieu / data.phieuThangTruoc) * 100.0;
                data.coDuLieuKyTruoc = true;
            } else {
                data.tyLeTangTruong = 0.0;
                data.coDuLieuKyTruoc = false;
            }

            data.luotKhachHaiThang = data.khachThangNay + data.khachThangTruoc;
            data.soXeHaiThang = data.xeThangNay + data.xeThangTruoc;

        } catch (SQLException e) {
            System.err.println("Lỗi thống kê so sánh tháng: " + e.getMessage());
        }

        return data;
    }

    @Override
    public SoSanhNamData laySoSanhNam(String maNVTiepNhan) {
        SoSanhNamData data = new SoSanhNamData();
        Calendar now = Calendar.getInstance();
        data.namNay = now.get(Calendar.YEAR);
        data.namTruoc = data.namNay - 1;

        if (!DbUtil.isDatabaseOnline()) {
            return data;
        }

        String sqlNam = "SELECT MONTH(p.ngayGioTiepNhan) AS thang, p.bienSoXe, x.maKH " +
                "FROM PhieuTiepNhanSuaChua p " +
                "JOIN Xe x ON p.bienSoXe = x.bienSoXe " +
                "WHERE YEAR(p.ngayGioTiepNhan) = ? " +
                (maNVTiepNhan != null && !maNVTiepNhan.isBlank() ? "AND p.maNVTiepNhan = ? " : "");

        try (Connection con = DbUtil.getConnection()) {
            // 1. Thống kê Năm nay
            Set<String> setXeNamNay = new HashSet<>();
            Set<String> setKhachNamNay = new HashSet<>();
            try (PreparedStatement ps = con.prepareStatement(sqlNam)) {
                ps.setInt(1, data.namNay);
                if (maNVTiepNhan != null && !maNVTiepNhan.isBlank()) {
                    ps.setString(2, maNVTiepNhan);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int thang = rs.getInt("thang");
                        if (thang >= 1 && thang <= 12) {
                            data.phieuTheoThangNamNay[thang - 1]++;
                        }
                        data.phieuNamNay++;
                        String bs = rs.getString("bienSoXe");
                        String mkh = rs.getString("maKH");
                        if (bs != null) setXeNamNay.add(bs);
                        if (mkh != null) setKhachNamNay.add(mkh);
                    }
                }
            }
            data.xeNamNay = setXeNamNay.size();
            data.khachNamNay = setKhachNamNay.size();

            // 2. Thống kê Năm trước
            Set<String> setXeNamTruoc = new HashSet<>();
            Set<String> setKhachNamTruoc = new HashSet<>();
            try (PreparedStatement ps = con.prepareStatement(sqlNam)) {
                ps.setInt(1, data.namTruoc);
                if (maNVTiepNhan != null && !maNVTiepNhan.isBlank()) {
                    ps.setString(2, maNVTiepNhan);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int thang = rs.getInt("thang");
                        if (thang >= 1 && thang <= 12) {
                            data.phieuTheoThangNamTruoc[thang - 1]++;
                        }
                        data.phieuNamTruoc++;
                        String bs = rs.getString("bienSoXe");
                        String mkh = rs.getString("maKH");
                        if (bs != null) setXeNamTruoc.add(bs);
                        if (mkh != null) setKhachNamTruoc.add(mkh);
                    }
                }
            }
            data.xeNamTruoc = setXeNamTruoc.size();
            data.khachNamTruoc = setKhachNamTruoc.size();

            data.chenhLechPhieu = data.phieuNamNay - data.phieuNamTruoc;
            if (data.phieuNamTruoc > 0) {
                data.tyLeTangTruong = ((double) data.chenhLechPhieu / data.phieuNamTruoc) * 100.0;
                data.coDuLieuKyTruoc = true;
            } else {
                data.tyLeTangTruong = 0.0;
                data.coDuLieuKyTruoc = false;
            }

            data.luotKhachHaiNam = data.khachNamNay + data.khachNamTruoc;
            data.soXeHaiNam = data.xeNamNay + data.xeNamTruoc;

        } catch (SQLException e) {
            System.err.println("Lỗi thống kê so sánh năm: " + e.getMessage());
        }

        return data;
    }

    @Override
    public TinhHinhXuLyData layTinhHinhXuLy(String maNVTiepNhan) {
        TinhHinhXuLyData data = new TinhHinhXuLyData();

        if (!DbUtil.isDatabaseOnline()) {
            return data;
        }

        String sqlDem = "SELECT p.trangThai, COUNT(*) AS sl " +
                "FROM PhieuTiepNhanSuaChua p " +
                "WHERE 1=1 " +
                (maNVTiepNhan != null && !maNVTiepNhan.isBlank() ? "AND p.maNVTiepNhan = ? " : "") +
                "GROUP BY p.trangThai";

        try (Connection con = DbUtil.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(sqlDem)) {
                if (maNVTiepNhan != null && !maNVTiepNhan.isBlank()) {
                    ps.setString(1, maNVTiepNhan);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String tt = rs.getString("trangThai");
                        int sl = rs.getInt("sl");
                        switch (tt) {
                            case "CHO_SUA" -> data.choSua = sl;
                            case "DANG_SUA" -> data.dangSua = sl;
                            case "CHO_LINH_KIEN" -> data.choLinhKien = sl;
                            case "HOAN_TAT" -> data.hoanTat = sl;
                        }
                    }
                }
            }
            data.tongCong = data.choSua + data.dangSua + data.choLinhKien + data.hoanTat;

            // Danh sách các phiếu tồn đọng lâu (chưa hoàn tất, sắp xếp từ cũ nhất)
            String sqlTonDong = "SELECT TOP 20 p.maPhieu, p.bienSoXe, p.ngayGioTiepNhan, p.trangThai, " +
                    "       kh.hoTen AS tenKhach, nvkt.hoTen AS tenKtv " +
                    "FROM PhieuTiepNhanSuaChua p " +
                    "JOIN Xe x ON p.bienSoXe = x.bienSoXe " +
                    "JOIN KhachHang kh ON x.maKH = kh.maKH " +
                    "LEFT JOIN NhanVien nvkt ON p.maNVKyThuat = nvkt.maNV " +
                    "WHERE p.trangThai IN ('CHO_SUA', 'DANG_SUA', 'CHO_LINH_KIEN') " +
                    (maNVTiepNhan != null && !maNVTiepNhan.isBlank() ? "AND p.maNVTiepNhan = ? " : "") +
                    "ORDER BY p.ngayGioTiepNhan ASC";

            try (PreparedStatement psTon = con.prepareStatement(sqlTonDong)) {
                if (maNVTiepNhan != null && !maNVTiepNhan.isBlank()) {
                    psTon.setString(1, maNVTiepNhan);
                }
                try (ResultSet rs = psTon.executeQuery()) {
                    long now = System.currentTimeMillis();
                    while (rs.next()) {
                        String ma = rs.getString("maPhieu");
                        String bs = rs.getString("bienSoXe");
                        Timestamp ts = rs.getTimestamp("ngayGioTiepNhan");
                        String tt = rs.getString("trangThai");
                        String kh = rs.getString("tenKhach");
                        String ktv = rs.getString("tenKtv");
                        if (ktv == null) ktv = "Chưa phân công";

                        String ttLabel = switch (tt) {
                            case "CHO_SUA" -> "Chờ sửa chữa";
                            case "DANG_SUA" -> "Đang sửa chữa";
                            case "CHO_LINH_KIEN" -> "Chờ linh kiện";
                            default -> tt;
                        };

                        String thoiGianTon = "Mới tiếp nhận";
                        if (ts != null) {
                            long diff = now - ts.getTime();
                            if (diff < 0) diff = 0;
                            long days = TimeUnit.MILLISECONDS.toDays(diff);
                            long hours = TimeUnit.MILLISECONDS.toHours(diff) % 24;
                            if (days > 0) {
                                thoiGianTon = days + " ngày " + hours + " giờ";
                            } else if (hours > 0) {
                                thoiGianTon = hours + " giờ";
                            } else {
                                thoiGianTon = "< 1 giờ";
                            }
                        }

                        String ngayStr = ts != null ? DATE_FMT.format(ts) : "--/--/----";
                        data.danhSachTonDong.add(new PhieuTonDongRow(ma, ngayStr, bs, kh, ttLabel, ktv, thoiGianTon));
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Lỗi thống kê tình hình xử lý: " + e.getMessage());
        }

        return data;
    }

    @Override
    public ThongKeDoiTraData layThongKeDoiTra(String maNVTiepNhan) {
        ThongKeDoiTraData data = new ThongKeDoiTraData();
        data.xuHuong7Ngay = new LinkedHashMap<>();
        data.danhSachGanDay = new ArrayList<>();

        if (!DbUtil.isDatabaseOnline()) return data;

        // Khởi tạo 7 ngày gần nhất (kể cả ngày không có phiếu)
        Calendar cal = Calendar.getInstance();
        for (int i = 6; i >= 0; i--) {
            Calendar tmp = (Calendar) cal.clone();
            tmp.add(Calendar.DAY_OF_YEAR, -i);
            data.xuHuong7Ngay.put(DAY_KEY_FMT.format(tmp.getTime()), 0);
        }

        // ── 1. Thống kê số phiếu & tiền hoàn hôm nay và tháng này ──────────
        String sqlKpi = "SELECT " +
            " SUM(CASE WHEN CAST(p.ngayLap AS DATE) = CAST(SYSDATETIME() AS DATE) THEN 1 ELSE 0 END) AS soPhieuHomNay," +
            " ISNULL(SUM(CASE WHEN CAST(p.ngayLap AS DATE) = CAST(SYSDATETIME() AS DATE) THEN v.soTienHoanTra ELSE 0 END), 0) AS tienHoanHomNay," +
            " COUNT(*) AS soPhieuThangNay," +
            " ISNULL(SUM(v.soTienHoanTra), 0) AS tienHoanThangNay" +
            " FROM PhieuDoiTra p" +
            " JOIN vw_PhieuDoiTra_HoanTien v ON v.maPhieuDT = p.maPhieuDT" +
            " WHERE YEAR(p.ngayLap) = YEAR(SYSDATETIME()) AND MONTH(p.ngayLap) = MONTH(SYSDATETIME())";

        List<Object> kpiParams = new ArrayList<>();
        String kpiFilter = sqlKpi;
        if (maNVTiepNhan != null && !maNVTiepNhan.isBlank()) {
            kpiFilter = "SELECT " +
                " SUM(CASE WHEN CAST(p.ngayLap AS DATE) = CAST(SYSDATETIME() AS DATE) THEN 1 ELSE 0 END) AS soPhieuHomNay," +
                " ISNULL(SUM(CASE WHEN CAST(p.ngayLap AS DATE) = CAST(SYSDATETIME() AS DATE) THEN v.soTienHoanTra ELSE 0 END), 0) AS tienHoanHomNay," +
                " COUNT(*) AS soPhieuThangNay," +
                " ISNULL(SUM(v.soTienHoanTra), 0) AS tienHoanThangNay" +
                " FROM PhieuDoiTra p" +
                " JOIN vw_PhieuDoiTra_HoanTien v ON v.maPhieuDT = p.maPhieuDT" +
                " WHERE YEAR(p.ngayLap) = YEAR(SYSDATETIME()) AND MONTH(p.ngayLap) = MONTH(SYSDATETIME())" +
                " AND p.maNV = ?";
            kpiParams.add(maNVTiepNhan);
        }

        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(kpiFilter)) {
            for (int i = 0; i < kpiParams.size(); i++) ps.setObject(i + 1, kpiParams.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    data.soPhieuHomNay   = rs.getInt("soPhieuHomNay");
                    data.tienHoanHomNay  = rs.getDouble("tienHoanHomNay");
                    data.soPhieuThangNay = rs.getLong("soPhieuThangNay");
                    data.tienHoanThangNay = rs.getDouble("tienHoanThangNay");
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi thống kê đổi trả KPI: " + e.getMessage());
        }

        // ── 2. Phân bố hướng xử lý (tháng này) ─────────────────────────────
        String sqlHX = "SELECT c.huongXuLy, COUNT(*) AS soLuong" +
            " FROM ChiTietDoiTra c" +
            " JOIN PhieuDoiTra p ON p.maPhieuDT = c.maPhieuDT" +
            " WHERE YEAR(p.ngayLap) = YEAR(SYSDATETIME()) AND MONTH(p.ngayLap) = MONTH(SYSDATETIME())" +
            (maNVTiepNhan != null && !maNVTiepNhan.isBlank() ? " AND p.maNV = ?" : "") +
            " GROUP BY c.huongXuLy";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sqlHX)) {
            if (maNVTiepNhan != null && !maNVTiepNhan.isBlank()) ps.setString(1, maNVTiepNhan);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    switch (rs.getString("huongXuLy")) {
                        case "HOAN_TIEN"      -> data.soHoanTien += rs.getInt("soLuong");
                        case "DOI_MOI"        -> data.soDoiMoi   += rs.getInt("soLuong");
                        case "SUA_LAI_MIEN_PHI" -> data.soSuaLai += rs.getInt("soLuong");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi phân bố hướng xử lý đổi trả: " + e.getMessage());
        }

        // ── 3. Xu hướng 7 ngày gần nhất ─────────────────────────────────────
        String sqlXH = "SELECT CAST(p.ngayLap AS DATE) AS ngay, COUNT(*) AS soPhieu" +
            " FROM PhieuDoiTra p" +
            " WHERE p.ngayLap >= DATEADD(DAY, -6, CAST(SYSDATETIME() AS DATE))" +
            (maNVTiepNhan != null && !maNVTiepNhan.isBlank() ? " AND p.maNV = ?" : "") +
            " GROUP BY CAST(p.ngayLap AS DATE)" +
            " ORDER BY ngay";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sqlXH)) {
            if (maNVTiepNhan != null && !maNVTiepNhan.isBlank()) ps.setString(1, maNVTiepNhan);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.sql.Date ngay = rs.getDate("ngay");
                    if (ngay != null) {
                        String key = DAY_KEY_FMT.format(ngay);
                        data.xuHuong7Ngay.put(key, rs.getInt("soPhieu"));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi xu hướng 7 ngày đổi trả: " + e.getMessage());
        }

        // ── 4. Danh sách 10 phiếu gần nhất ──────────────────────────────────
        String sqlDs = "SELECT TOP 10 p.maPhieuDT, p.ngayLap, p.maHoaDon," +
            "   kh.hoTen AS tenKhach, x.bienSoXe," +
            "   ISNULL(v.soTienHoanTra, 0) AS soTienHoanTra," +
            "   (SELECT STRING_AGG(c.huongXuLy, ' / ') FROM ChiTietDoiTra c WHERE c.maPhieuDT = p.maPhieuDT) AS huongXuLy" +
            " FROM PhieuDoiTra p" +
            " JOIN vw_PhieuDoiTra_HoanTien v ON v.maPhieuDT = p.maPhieuDT" +
            " JOIN HoaDon h ON h.maHoaDon = p.maHoaDon" +
            " JOIN PhieuTiepNhanSuaChua pt ON pt.maPhieu = h.maPhieu" +
            " JOIN Xe x ON x.bienSoXe = pt.bienSoXe" +
            " JOIN KhachHang kh ON kh.maKH = x.maKH" +
            (maNVTiepNhan != null && !maNVTiepNhan.isBlank() ? " WHERE p.maNV = ?" : "") +
            " ORDER BY p.ngayLap DESC";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sqlDs)) {
            if (maNVTiepNhan != null && !maNVTiepNhan.isBlank()) ps.setString(1, maNVTiepNhan);
            DecimalFormat mf = new DecimalFormat("#,### đ");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("ngayLap");
                    String ngayStr = ts != null ? DATE_FMT.format(ts) : "--";
                    String tienStr = mf.format(rs.getDouble("soTienHoanTra"));
                    String hx = rs.getString("huongXuLy");
                    if (hx != null) {
                        hx = hx.replace("HOAN_TIEN", "Hoàn tiền")
                               .replace("DOI_MOI", "Đổi mới")
                               .replace("SUA_LAI_MIEN_PHI", "Sửa lại");
                    }
                    data.danhSachGanDay.add(new DoiTraRow(
                        rs.getString("maPhieuDT"),
                        ngayStr,
                        rs.getString("maHoaDon"),
                        rs.getString("tenKhach"),
                        rs.getString("bienSoXe"),
                        tienStr,
                        hx != null ? hx : "--"
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi danh sách đổi trả gần đây: " + e.getMessage());
        }

        return data;
    }
}
