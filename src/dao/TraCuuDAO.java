package dao;

import connectDB.DbUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** Truy vấn chỉ đọc cho tra cứu khách hàng và lịch sử sửa chữa của Chủ cửa hàng. */
public class TraCuuDAO {
    public record KhachHangResult(String maKH, String hoTen, String soDienThoai, String cccd,
                                  String ghiChu, boolean thanhVienThanThiet, double diemTichLuy,
                                  int soXe, int soPhieu) { }
    public record XeResult(String bienSo, String hangXe, String model, String mauSac, Integer namSanXuat, int soKm) { }
    public record LichSuResult(String maPhieu, Timestamp ngayTiepNhan, String bienSo, String xe,
                               String maKH, String tenKhachHang, String soDienThoai, String yeuCau,
                               String trangThai, String tiepNhanVien, String kyThuatVien,
                               double tienDichVu, double tienLinhKien, double tongTien) { }
    public record LichSuTiepNhanResult(String maPhieu, Timestamp ngayTiepNhan, String bienSo, String xe,
                                       String maKH, String tenKhachHang, String soDienThoai, String yeuCau,
                                       String trangThai, String tiepNhanVien) { }
    public record HangMucResult(String loai, String ma, String ten, int soLuong, double donGia, double thanhTien) { }

    /** Tìm hồ sơ khách theo mã, tên, điện thoại hoặc CCCD; không gọi với từ khóa rỗng. */
    public List<KhachHangResult> timKhachHang(String tuKhoa) {
        String sql = "SELECT TOP 200 kh.maKH,kh.hoTen,kh.soDienThoai,kh.CCCD,kh.ghiChu,"
                + "kh.thanhVienThanThiet,kh.diemTichLuy,COUNT(DISTINCT x.bienSoXe) soXe,COUNT(DISTINCT p.maPhieu) soPhieu "
                + "FROM KhachHang kh LEFT JOIN Xe x ON x.maKH=kh.maKH "
                + "LEFT JOIN PhieuTiepNhanSuaChua p ON p.bienSoXe=x.bienSoXe "
                + "WHERE kh.maKH LIKE ? OR kh.hoTen LIKE ? OR kh.soDienThoai LIKE ? OR kh.CCCD LIKE ? "
                + "GROUP BY kh.maKH,kh.hoTen,kh.soDienThoai,kh.CCCD,kh.ghiChu,kh.thanhVienThanThiet,kh.diemTichLuy "
                + "ORDER BY kh.hoTen,kh.maKH";
        String pattern = pattern(tuKhoa); List<KhachHangResult> result = new ArrayList<>();
        try (Connection con = DbUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 1; i <= 4; i++) ps.setString(i, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(new KhachHangResult(rs.getString("maKH"), rs.getString("hoTen"),
                        rs.getString("soDienThoai"), rs.getString("CCCD"), rs.getString("ghiChu"),
                        rs.getBoolean("thanhVienThanThiet"), rs.getDouble("diemTichLuy"), rs.getInt("soXe"), rs.getInt("soPhieu")));
            }
        } catch (SQLException e) { throw new RuntimeException("Lỗi tra cứu khách hàng: " + e.getMessage(), e); }
        return result;
    }

    /** Lấy xe theo mã khách hàng sau khi người dùng chọn một kết quả. */
    public List<XeResult> layXeKhachHang(String maKH) {
        String sql = "SELECT bienSoXe,hangXe,model,mauSac,namSanXuat,soKmHienTai FROM Xe WHERE maKH=? ORDER BY bienSoXe";
        List<XeResult> result = new ArrayList<>();
        try (Connection con = DbUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maKH);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int year = rs.getInt("namSanXuat"); Integer nam = rs.wasNull() ? null : year;
                    result.add(new XeResult(rs.getString("bienSoXe"), rs.getString("hangXe"), rs.getString("model"),
                            rs.getString("mauSac"), nam, rs.getInt("soKmHienTai")));
                }
            }
        } catch (SQLException e) { throw new RuntimeException("Lỗi đọc xe của khách hàng: " + e.getMessage(), e); }
        return result;
    }

    /** Tra lịch sử theo mã phiếu, biển số, mã/tên/số điện thoại khách hàng. */
    public List<LichSuResult> timLichSu(String tuKhoa) {
        String sql = "SELECT TOP 200 maPhieu,ngayGioTiepNhan,bienSoXe,hangXe,model,maKH,tenKhachHang,soDienThoai,"
                + "yeuCauBanDau,trangThai,tiepNhanVien,kyThuatVien,tongTienDichVu,tongTienLinhKien,tongTien "
                + "FROM vw_LichSuSuaChua WHERE maPhieu LIKE ? OR bienSoXe LIKE ? OR maKH LIKE ? "
                + "OR tenKhachHang LIKE ? OR soDienThoai LIKE ? ORDER BY ngayGioTiepNhan DESC,maPhieu DESC";
        String pattern = pattern(tuKhoa); List<LichSuResult> result = new ArrayList<>();
        try (Connection con = DbUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 1; i <= 5; i++) ps.setString(i, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(new LichSuResult(rs.getString("maPhieu"), rs.getTimestamp("ngayGioTiepNhan"),
                        rs.getString("bienSoXe"), joinXe(rs.getString("hangXe"), rs.getString("model")), rs.getString("maKH"),
                        rs.getString("tenKhachHang"), rs.getString("soDienThoai"), rs.getString("yeuCauBanDau"),
                        rs.getString("trangThai"), rs.getString("tiepNhanVien"), rs.getString("kyThuatVien"),
                        rs.getDouble("tongTienDichVu"), rs.getDouble("tongTienLinhKien"), rs.getDouble("tongTien")));
            }
        } catch (SQLException e) { throw new RuntimeException("Lỗi tra cứu lịch sử sửa chữa: " + e.getMessage(), e); }
        return result;
    }

    /** Tra cứu lịch sử tiếp nhận theo mã phiếu, biển số, mã/tên/số điện thoại khách hàng. */
    public List<LichSuTiepNhanResult> timLichSuTiepNhan(String tuKhoa) {
        return timLichSuTiepNhan(tuKhoa, null);
    }

    /** Thêm bộ lọc mã nhân viên tiếp nhận khi vai trò TNV chỉ được xem phiếu của mình. */
    public List<LichSuTiepNhanResult> timLichSuTiepNhan(String tuKhoa, String maNVTiepNhan) {
        String sql = "SELECT TOP 200 p.maPhieu, p.ngayGioTiepNhan, p.bienSoXe, x.hangXe, x.model, kh.maKH, "
                + "kh.hoTen AS tenKhachHang, kh.soDienThoai, p.yeuCauBanDau, p.trangThai, nv.hoTen AS tiepNhanVien "
                + "FROM PhieuTiepNhanSuaChua p "
                + "JOIN Xe x ON x.bienSoXe = p.bienSoXe "
                + "JOIN KhachHang kh ON kh.maKH = x.maKH "
                + "JOIN NhanVien nv ON nv.maNV = p.maNVTiepNhan "
                + "WHERE (p.maPhieu LIKE ? OR p.bienSoXe LIKE ? OR kh.maKH LIKE ? OR kh.hoTen LIKE ? OR kh.soDienThoai LIKE ?) "
                + "AND (? IS NULL OR p.maNVTiepNhan = ?) "
                + "ORDER BY p.ngayGioTiepNhan DESC, p.maPhieu DESC";
        String pattern = pattern(tuKhoa);
        List<LichSuTiepNhanResult> result = new ArrayList<>();
        try (Connection con = DbUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 1; i <= 5; i++) ps.setString(i, pattern);
            ps.setString(6, maNVTiepNhan);
            ps.setString(7, maNVTiepNhan);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new LichSuTiepNhanResult(
                            rs.getString("maPhieu"),
                            rs.getTimestamp("ngayGioTiepNhan"),
                            rs.getString("bienSoXe"),
                            joinXe(rs.getString("hangXe"), rs.getString("model")),
                            rs.getString("maKH"),
                            rs.getString("tenKhachHang"),
                            rs.getString("soDienThoai"),
                            rs.getString("yeuCauBanDau"),
                            rs.getString("trangThai"),
                            rs.getString("tiepNhanVien")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tra cứu lịch sử tiếp nhận: " + e.getMessage(), e);
        }
        return result;
    }

    /** Lấy các dịch vụ và linh kiện đã ghi nhận trong phiếu sửa chữa được chọn. */
    public List<HangMucResult> layHangMucPhieu(String maPhieu) {
        String sql = "SELECT loai,ma,ten,soLuong,donGia,thanhTien FROM ("
                + "SELECT N'Dịch vụ' loai,d.maDV ma,d.tenDV ten,c.soLuong,c.donGiaThucHien donGia,c.thanhTien "
                + "FROM ChiTietDichVu c JOIN DichVu d ON d.maDV=c.maDV WHERE c.maPhieu=? UNION ALL "
                + "SELECT N'Linh kiện',l.maLK,l.tenLK,c.soLuong,c.donGiaXuat,c.thanhTien "
                + "FROM ChiTietLinhKien c JOIN LinhKien l ON l.maLK=c.maLK WHERE c.maPhieu=?"
                + ") x ORDER BY loai,ten";
        List<HangMucResult> result = new ArrayList<>();
        try (Connection con = DbUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maPhieu); ps.setString(2, maPhieu);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(new HangMucResult(rs.getString("loai"), rs.getString("ma"), rs.getString("ten"),
                        rs.getInt("soLuong"), rs.getDouble("donGia"), rs.getDouble("thanhTien")));
            }
        } catch (SQLException e) { throw new RuntimeException("Lỗi đọc hạng mục phiếu sửa chữa: " + e.getMessage(), e); }
        return result;
    }

    private static String pattern(String tuKhoa) {
        if (tuKhoa == null || tuKhoa.isBlank()) throw new IllegalArgumentException("Từ khóa tra cứu không được để trống.");
        return "%" + tuKhoa.trim() + "%";
    }
    private static String joinXe(String hang, String model) { return model == null || model.isBlank() ? hang : hang + " " + model; }
}
