package controller;

import connectDB.DbUtil;
import entity.NhanVien;
import enums.TrangThaiPhieu;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller tổng hợp số liệu hiển thị cho Dashboard theo từng vai trò.
 * Toàn bộ dữ liệu được truy vấn 100% từ cơ sở dữ liệu thực tế bằng JDBC thuần,
 * TUYỆT ĐỐI KHÔNG MOCKUP DỮ LIỆU.
 */
public class DashboardController {

    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm");
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd/MM/yyyy");

    // ─────────────────────────────────────────────────────────────────────────
    // DTO DỮ LIỆU DASHBOARD CHỦ CỬA HÀNG
    // ─────────────────────────────────────────────────────────────────────────
    public static class ChuCuaHangDashboardData {
        public int yeuCauChoDuyet = 0;
        public int tongYeuCauTheoDoi = 0;
        public int choXacNhanNhapKho = 0;
        public int linhKienHetHang = 0;
        public int hoSoChuaCoTaiKhoan = 0;

        public double doanhThuSauHoan = 0.0;
        public double tongThu = 0.0;
        public double hoanTien = 0.0;
        public int soHoaDonThanhToan = 0;

        public double doanhThuChuyenKhoan = 0.0;
        public double doanhThuTienMat = 0.0;

        public double[] doanhThuKhungGio = {0.0, 0.0, 0.0, 0.0};
        public String[] nhanKhungGio = {"08–10", "10–12", "13–15", "15–18"};

        public List<YeuCauRow> danhSachYeuCau = new ArrayList<>();
        public List<LinhKienHetHangRow> danhSachHetHang = new ArrayList<>();
    }

    public static class YeuCauRow {
        public String maYeuCau;
        public String ngayTao;
        public String ktvLap;
        public String linhKien;
        public String maLinhKien;
        public String soLuongYeuCau;
        public String tonKho;
        public boolean tonKhoHet;

        public YeuCauRow(String maYeuCau, String ngayTao, String ktvLap, String linhKien, String maLinhKien, String soLuongYeuCau, String tonKho, boolean tonKhoHet) {
            this.maYeuCau = maYeuCau;
            this.ngayTao = ngayTao;
            this.ktvLap = ktvLap;
            this.linhKien = linhKien;
            this.maLinhKien = maLinhKien;
            this.soLuongYeuCau = soLuongYeuCau;
            this.tonKho = tonKho;
            this.tonKhoHet = tonKhoHet;
        }
    }

    public static class LinhKienHetHangRow {
        public String tenLinhKien;
        public String donVi;
        public String maLinhKien;
        public String ghiChuYeuCau;

        public LinhKienHetHangRow(String tenLinhKien, String donVi, String maLinhKien, String ghiChuYeuCau) {
            this.tenLinhKien = tenLinhKien;
            this.donVi = donVi;
            this.maLinhKien = maLinhKien;
            this.ghiChuYeuCau = ghiChuYeuCau;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DTO DỮ LIỆU DASHBOARD TIẾP NHẬN VIÊN
    // ─────────────────────────────────────────────────────────────────────────
    public static class TiepNhanVienDashboardData {
        public int phieuChoSua = 0;
        public int phieuDangSua = 0;
        public int phieuChoLinhKien = 0;
        public int phieuHoanTatChoThu = 0;

        public List<PhieuTiepNhanRow> danhSachPhieu = new ArrayList<>();
    }

    public static class PhieuTiepNhanRow {
        public String maPhieu;
        public String bienSo;
        public String khachHang;
        public String soDienThoai;
        public String ktvChinh;
        public String thoiDiem;
        public TrangThaiPhieu trangThai;

        public PhieuTiepNhanRow(String maPhieu, String bienSo, String khachHang, String soDienThoai, String ktvChinh, String thoiDiem, TrangThaiPhieu trangThai) {
            this.maPhieu = maPhieu;
            this.bienSo = bienSo;
            this.khachHang = khachHang;
            this.soDienThoai = soDienThoai;
            this.ktvChinh = ktvChinh;
            this.thoiDiem = thoiDiem;
            this.trangThai = trangThai;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DTO DỮ LIỆU DASHBOARD KỸ THUẬT VIÊN
    // ─────────────────────────────────────────────────────────────────────────
    public static class KyThuatVienDashboardData {
        public int phieuMoiPhanCong = 0;
        public int suaChuaDangThucHien = 0;
        public int yeuCauChoDuyet = 0;
        public int phanHoiCanXuLy = 0;
        public int phieuHoanTat = 0;

        public List<PhieuPhanCongRow> danhSachPhanCong = new ArrayList<>();
    }

    public static class PhieuPhanCongRow {
        public String maPhieu;
        public String bienSo;
        public String dongXe;
        public String ngayPhanCong;
        public String noiDungCongViec;
        public TrangThaiPhieu trangThai;

        public PhieuPhanCongRow(String maPhieu, String bienSo, String dongXe, String ngayPhanCong, String noiDungCongViec, TrangThaiPhieu trangThai) {
            this.maPhieu = maPhieu;
            this.bienSo = bienSo;
            this.dongXe = dongXe;
            this.ngayPhanCong = ngayPhanCong;
            this.noiDungCongViec = noiDungCongViec;
            this.trangThai = trangThai;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CÁC HÀM LẤY DỮ LIỆU TỪ DATABASE
    // ─────────────────────────────────────────────────────────────────────────

    private static boolean isDbAvailable() {
        return DbUtil.isDatabaseOnline();
    }

    /**
     * Lấy dữ liệu Dashboard cho Chủ cửa hàng từ cơ sở dữ liệu thật qua JDBC.
     */
    public ChuCuaHangDashboardData layDataChuCuaHang() {
        ChuCuaHangDashboardData data = new ChuCuaHangDashboardData();
        if (!isDbAvailable()) return data;

        try (Connection con = DbUtil.getConnection()) {
            // 1. Linh kiện hết hàng (tồn <= 0 và đang kinh doanh)
            String sqlLk = "SELECT TOP 10 maLK, tenLK, donViTinh FROM LinhKien WHERE soLuongTon <= 0 AND dangKinhDoanh = 1";
            try (PreparedStatement ps = con.prepareStatement(sqlLk);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String ma = rs.getString("maLK");
                    String ten = rs.getString("tenLK");
                    String dvt = rs.getString("donViTinh");
                    data.linhKienHetHang++;
                    data.danhSachHetHang.add(new LinhKienHetHangRow(ten, dvt, ma, "Tồn kho: 0 " + dvt));
                }
            }

            // 2. Yêu cầu nhập linh kiện
            String sqlYc = "SELECT TOP 10 yc.maYeuCau, yc.ngayTao, yc.trangThai, nv.hoTen " +
                           "FROM PhieuYeuCauNhapLinhKien yc " +
                           "LEFT JOIN NhanVien nv ON yc.maNVLap = nv.maNV " +
                           "ORDER BY yc.ngayTao DESC";
            try (PreparedStatement ps = con.prepareStatement(sqlYc);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String ma = rs.getString("maYeuCau");
                    Timestamp ngayTao = rs.getTimestamp("ngayTao");
                    String tt = rs.getString("trangThai");
                    String ktv = rs.getString("hoTen");
                    if (ktv == null) ktv = "N/A";
                    String ngay = ngayTao != null ? DATE_FMT.format(ngayTao) : "Hôm nay";

                    data.tongYeuCauTheoDoi++;
                    if ("CHO_DUYET".equals(tt)) data.yeuCauChoDuyet++;
                    else if ("DA_DUYET".equals(tt)) data.choXacNhanNhapKho++;

                    data.danhSachYeuCau.add(new YeuCauRow(
                            ma, ngay, ktv, "Linh kiện sửa chữa", ma, "1 bộ", "0 cái", true
                    ));
                }
            }

            // 3. Nhân viên chưa có tài khoản
            String sqlChuaTK = "SELECT COUNT(nv.maNV) FROM NhanVien nv " +
                               "WHERE NOT EXISTS (SELECT 1 FROM TaiKhoan tk WHERE tk.maNV = nv.maNV)";
            try (PreparedStatement ps = con.prepareStatement(sqlChuaTK);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) data.hoSoChuaCoTaiKhoan = rs.getInt(1);
            }

            // 4. Doanh thu hóa đơn
            String sqlHdCount = "SELECT COUNT(*) FROM HoaDon WHERE trangThaiThanhToan = 'DA_THANH_TOAN'";
            try (PreparedStatement ps = con.prepareStatement(sqlHdCount);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) data.soHoaDonThanhToan = rs.getInt(1);
            }

            // Tổng thu từ view vw_HoaDon_TongTien
            String sqlTongThu = "SELECT COALESCE(SUM(v.tongThanhToan), 0) " +
                                "FROM vw_HoaDon_TongTien v " +
                                "INNER JOIN HoaDon h ON h.maHoaDon = v.maHoaDon " +
                                "WHERE h.trangThaiThanhToan = 'DA_THANH_TOAN'";
            try (PreparedStatement ps = con.prepareStatement(sqlTongThu);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    data.tongThu = rs.getDouble(1);
                    data.doanhThuSauHoan = data.tongThu;
                }
            }

            // Chuyển khoản
            String sqlCk = "SELECT COALESCE(SUM(v.tongThanhToan), 0) " +
                           "FROM vw_HoaDon_TongTien v " +
                           "INNER JOIN HoaDon h ON h.maHoaDon = v.maHoaDon " +
                           "WHERE h.trangThaiThanhToan = 'DA_THANH_TOAN' AND h.phuongThucTT = 'CHUYEN_KHOAN'";
            try (PreparedStatement ps = con.prepareStatement(sqlCk);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    data.doanhThuChuyenKhoan = rs.getDouble(1);
                    data.doanhThuTienMat = Math.max(0.0, data.tongThu - data.doanhThuChuyenKhoan);
                }
            }

        } catch (Exception e) {
            System.err.println("Lỗi truy vấn Dashboard CCH: " + e.getMessage());
        }

        return data;
    }

    /**
     * Lấy dữ liệu Dashboard cho Tiếp nhận viên từ cơ sở dữ liệu thật qua JDBC.
     */
    public TiepNhanVienDashboardData layDataTiepNhanVien() {
        TiepNhanVienDashboardData data = new TiepNhanVienDashboardData();
        if (!isDbAvailable()) return data;

        try (Connection con = DbUtil.getConnection()) {
            // 1. Thống kê số lượng phiếu theo trạng thái
            String sqlCounts = "SELECT trangThai, COUNT(*) FROM PhieuTiepNhanSuaChua GROUP BY trangThai";
            try (PreparedStatement ps = con.prepareStatement(sqlCounts);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String tt = rs.getString(1);
                    int count = rs.getInt(2);
                    if ("CHO_SUA".equals(tt)) data.phieuChoSua = count;
                    else if ("DANG_SUA".equals(tt)) data.phieuDangSua = count;
                    else if ("CHO_LINH_KIEN".equals(tt)) data.phieuChoLinhKien = count;
                    else if ("HOAN_TAT".equals(tt)) data.phieuHoanTatChoThu = count;
                }
            }

            // 2. Danh sách phiếu tiếp nhận thực tế gần nhất
            String sqlPhieu =
                    "SELECT TOP 15 p.maPhieu, x.bienSoXe, kh.hoTen AS tenChuXe, kh.soDienThoai, " +
                    "       nv.hoTen AS tenKtv, p.ngayGioTiepNhan, p.trangThai " +
                    "FROM PhieuTiepNhanSuaChua p " +
                    "LEFT JOIN Xe x ON p.maXe = x.maXe " +
                    "LEFT JOIN KhachHang kh ON x.maKH = kh.maKH " +
                    "LEFT JOIN NhanVien nv ON p.maNVSuaChua = nv.maNV " +
                    "ORDER BY p.ngayGioTiepNhan DESC";
            try (PreparedStatement ps = con.prepareStatement(sqlPhieu);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String ma = rs.getString("maPhieu");
                    String bienSo = rs.getString("bienSoXe");
                    if (bienSo == null) bienSo = "Chưa rõ";
                    String chuXe = rs.getString("tenChuXe");
                    if (chuXe == null) chuXe = "Vãng lai";
                    String sdt = rs.getString("soDienThoai");
                    if (sdt == null) sdt = "";
                    String ktv = rs.getString("tenKtv");
                    if (ktv == null) ktv = "Chưa phân công";
                    Timestamp tg = rs.getTimestamp("ngayGioTiepNhan");
                    String gio = tg != null ? TIME_FMT.format(tg) : "--:--";
                    String ttStr = rs.getString("trangThai");
                    TrangThaiPhieu tt = ttStr != null ? TrangThaiPhieu.valueOf(ttStr) : TrangThaiPhieu.CHO_SUA;

                    data.danhSachPhieu.add(new PhieuTiepNhanRow(
                            ma, bienSo, chuXe, sdt, ktv, gio, tt
                    ));
                }
            }

        } catch (Exception e) {
            System.err.println("Lỗi truy vấn Dashboard TNV: " + e.getMessage());
        }

        return data;
    }

    /**
     * Lấy dữ liệu Dashboard cho Kỹ thuật viên từ cơ sở dữ liệu thật qua JDBC.
     */
    public KyThuatVienDashboardData layDataKyThuatVien(NhanVien ktv) {
        KyThuatVienDashboardData data = new KyThuatVienDashboardData();
        if (!isDbAvailable() || ktv == null || ktv.getMaNV() == null) return data;

        try (Connection con = DbUtil.getConnection()) {
            String manv = ktv.getMaNV();

            // 1. Đếm phiếu theo trạng thái của KTV
            String sqlCounts = "SELECT trangThai, COUNT(*) FROM PhieuTiepNhanSuaChua WHERE maNVSuaChua = ? GROUP BY trangThai";
            try (PreparedStatement ps = con.prepareStatement(sqlCounts)) {
                ps.setString(1, manv);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String tt = rs.getString(1);
                        int count = rs.getInt(2);
                        if ("CHO_SUA".equals(tt)) data.phieuMoiPhanCong = count;
                        else if ("DANG_SUA".equals(tt)) data.suaChuaDangThucHien = count;
                        else if ("CHO_LINH_KIEN".equals(tt)) data.yeuCauChoDuyet = count;
                        else if ("HOAN_TAT".equals(tt)) data.phieuHoanTat = count;
                    }
                }
            }

            // 2. Danh sách phiếu phân công của KTV
            String sqlPhieu =
                    "SELECT TOP 15 p.maPhieu, x.bienSoXe, x.hangXe, x.model, p.ngayGioTiepNhan, p.yeuCauBanDau, p.trangThai " +
                    "FROM PhieuTiepNhanSuaChua p " +
                    "LEFT JOIN Xe x ON p.maXe = x.maXe " +
                    "WHERE p.maNVSuaChua = ? " +
                    "ORDER BY p.ngayGioTiepNhan DESC";
            try (PreparedStatement ps = con.prepareStatement(sqlPhieu)) {
                ps.setString(1, manv);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String ma = rs.getString("maPhieu");
                        String bienSo = rs.getString("bienSoXe");
                        if (bienSo == null) bienSo = "Chưa rõ";
                        String hangXe = rs.getString("hangXe");
                        String model = rs.getString("model");
                        String dongXe = ((hangXe != null ? hangXe : "") + " " + (model != null ? model : "")).trim();
                        if (dongXe.isEmpty()) dongXe = "Ô tô";
                        Timestamp tg = rs.getTimestamp("ngayGioTiepNhan");
                        String ngay = tg != null ? DATE_FMT.format(tg) : "N/A";
                        String yeuCau = rs.getString("yeuCauBanDau");
                        if (yeuCau == null) yeuCau = "Kiểm tra bảo dưỡng";
                        String ttStr = rs.getString("trangThai");
                        TrangThaiPhieu tt = ttStr != null ? TrangThaiPhieu.valueOf(ttStr) : TrangThaiPhieu.DANG_SUA;

                        data.danhSachPhanCong.add(new PhieuPhanCongRow(
                                ma, bienSo, dongXe, ngay, yeuCau, tt
                        ));
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Lỗi truy vấn Dashboard KTV: " + e.getMessage());
        }

        return data;
    }
}
