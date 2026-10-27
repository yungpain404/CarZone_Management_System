package service;

import connectDB.JpaUtil;
import entity.NhanVien;
import enums.TrangThaiPhieu;
import jakarta.persistence.EntityManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Service tổng hợp số liệu hiển thị cho Dashboard theo từng vai trò (Chủ cửa hàng, Tiếp nhận viên, Kỹ thuật viên).
 * Hỗ trợ truy vấn JPA thực tế và cung cấp dữ liệu fallback chuẩn theo kịch bản thiết kế CarZone.
 */
public class DashboardService {

    // ─────────────────────────────────────────────────────────────────────────
    // DTO DỮ LIỆU DASHBOARD CHỦ CỬA HÀNG
    // ─────────────────────────────────────────────────────────────────────────
    public static class ChuCuaHangDashboardData {
        public int yeuCauChoDuyet = 3;
        public int tongYeuCauTheoDoi = 6;
        public int choXacNhanNhapKho = 1;
        public int linhKienHetHang = 2;
        public int hoSoChuaCoTaiKhoan = 1;

        public double doanhThuSauHoan = 24_000_000.0;
        public double tongThu = 25_200_000.0;
        public double hoanTien = 1_200_000.0;
        public int soHoaDonThanhToan = 18;

        public double doanhThuChuyenKhoan = 17_000_000.0;
        public double doanhThuTienMat = 8_200_000.0;

        // Doanh thu theo khung giờ: 08-10, 10-12, 13-15, 15-18 (đơn vị: triệu VNĐ)
        public double[] doanhThuKhungGio = {6.0, 4.8, 6.2, 7.0};
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
        public int phieuChoSua = 4;
        public int phieuDangSua = 7;
        public int phieuChoLinhKien = 2;
        public int phieuHoanTatChoThu = 5;
        public int hoaDonHomNay = 14;
        public double tongTienThuHomNay = 18_750_000.0;

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
        public int phieuMoiPhanCong = 3;
        public int suaChuaDangThucHien = 2;
        public int yeuCauChoDuyet = 1;
        public int phanHoiCanXuLy = 0;

        // Tương thích ngược
        public int phieuDangSua = 2;
        public int phieuChoLinhKien = 1;
        public int phieuHoanTatHomNay = 3;
        public int yeuCauLinhKienDangGui = 1;

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
    // CÁC HÀM LẤY DỮ LIỆU
    // ─────────────────────────────────────────────────────────────────────────

    private static boolean isDbAvailable() {
        return connectDB.JpaUtil.isDatabaseOnline();
    }

    /** Lấy dữ liệu Dashboard cho Chủ cửa hàng */
    public ChuCuaHangDashboardData layDataChuCuaHang() {
        ChuCuaHangDashboardData data = new ChuCuaHangDashboardData();

        // Nạp danh sách yêu cầu chờ duyệt mặc định theo Figma
        data.danhSachYeuCau.add(new YeuCauRow("YC00021", "04/10/2026", "Lê Quốc Huy", "Lọc dầu", "LK00001", "20 Cái", "8 Cái", false));
        data.danhSachYeuCau.add(new YeuCauRow("YC00020", "04/10/2026", "Trần Văn Nam", "Má phanh", "LK00004", "10 Bộ", "0 Bộ", true));
        data.danhSachYeuCau.add(new YeuCauRow("YC00019", "03/10/2026", "Phạm Anh Tuấn", "Bugi", "LK00006", "12 Cái", "0 Cái", true));

        // Nạp danh sách linh kiện hết hàng mặc định theo Figma
        data.danhSachHetHang.add(new LinhKienHetHangRow("Má phanh trước", "Bộ", "LK00004", "YC00020 đang chờ duyệt"));
        data.danhSachHetHang.add(new LinhKienHetHangRow("Bugi đánh lửa", "Cái", "LK00006", "YC00019 đang chờ duyệt"));

        // Thử truy vấn số liệu thật từ DB nếu có kết nối
        if (isDbAvailable()) {
            try (EntityManager em = JpaUtil.createEntityManager()) {
                Long countHetHang = em.createQuery("SELECT COUNT(l) FROM LinhKien l WHERE l.soLuongTon = 0 AND l.dangKinhDoanh = true", Long.class)
                        .getSingleResult();
                if (countHetHang != null && countHetHang > 0) {
                    data.linhKienHetHang = countHetHang.intValue();
                }
            } catch (Exception ignored) {
            }
        }

        return data;
    }

    /** Lấy dữ liệu Dashboard cho Tiếp nhận viên */
    public TiepNhanVienDashboardData layDataTiepNhanVien() {
        TiepNhanVienDashboardData data = new TiepNhanVienDashboardData();

        data.danhSachPhieu.add(new PhieuTiepNhanRow("PSC00041", "51G-889.23", "Nguyễn Văn An", "0908123456", "Trần Văn Nam", "08:15", TrangThaiPhieu.DANG_SUA));
        data.danhSachPhieu.add(new PhieuTiepNhanRow("PSC00042", "51K-123.45", "Lê Thị Bích", "0912345678", "Lê Quốc Huy", "08:45", TrangThaiPhieu.CHO_LINH_KIEN));
        data.danhSachPhieu.add(new PhieuTiepNhanRow("PSC00043", "59A-678.90", "Trần Quốc Toản", "0934567890", "Phạm Anh Tuấn", "09:30", TrangThaiPhieu.CHO_SUA));
        data.danhSachPhieu.add(new PhieuTiepNhanRow("PSC00044", "60B-456.78", "Hoàng Thu Thảo", "0978901234", "Trần Văn Nam", "10:00", TrangThaiPhieu.HOAN_TAT));
        data.danhSachPhieu.add(new PhieuTiepNhanRow("PSC00045", "72C-234.56", "Vũ Minh Trí", "0989012345", "Lê Quốc Huy", "10:30", TrangThaiPhieu.DANG_SUA));

        if (isDbAvailable()) {
            try (EntityManager em = JpaUtil.createEntityManager()) {
                Long choSua = em.createQuery("SELECT COUNT(p) FROM PhieuTiepNhanSuaChua p WHERE p.trangThai = :st", Long.class)
                        .setParameter("st", TrangThaiPhieu.CHO_SUA).getSingleResult();
                if (choSua != null) data.phieuChoSua = choSua.intValue();

                Long dangSua = em.createQuery("SELECT COUNT(p) FROM PhieuTiepNhanSuaChua p WHERE p.trangThai = :st", Long.class)
                        .setParameter("st", TrangThaiPhieu.DANG_SUA).getSingleResult();
                if (dangSua != null) data.phieuDangSua = dangSua.intValue();

                Long choLk = em.createQuery("SELECT COUNT(p) FROM PhieuTiepNhanSuaChua p WHERE p.trangThai = :st", Long.class)
                        .setParameter("st", TrangThaiPhieu.CHO_LINH_KIEN).getSingleResult();
                if (choLk != null) data.phieuChoLinhKien = choLk.intValue();

                Long hoanTat = em.createQuery("SELECT COUNT(p) FROM PhieuTiepNhanSuaChua p WHERE p.trangThai = :st", Long.class)
                        .setParameter("st", TrangThaiPhieu.HOAN_TAT).getSingleResult();
                if (hoanTat != null) data.phieuHoanTatChoThu = hoanTat.intValue();
            } catch (Exception ignored) {
            }
        }

        return data;
    }

    /** Lấy dữ liệu Dashboard cho Kỹ thuật viên */
    public KyThuatVienDashboardData layDataKyThuatVien(NhanVien ktv) {
        KyThuatVienDashboardData data = new KyThuatVienDashboardData();

        data.danhSachPhanCong.add(new PhieuPhanCongRow("PSC00041", "51G-889.23", "Toyota Camry 2020", "04/10/2026", "Thay dầu máy, lọc nhớt, bảo dưỡng phanh trước", TrangThaiPhieu.DANG_SUA));
        data.danhSachPhanCong.add(new PhieuPhanCongRow("PSC00042", "51K-123.45", "Honda Civic 2019", "04/10/2026", "Kiểm tra hệ thống điện & thay bugi đánh lửa", TrangThaiPhieu.CHO_LINH_KIEN));
        data.danhSachPhanCong.add(new PhieuPhanCongRow("PSC00038", "50E-999.88", "Mazda CX-5 2021", "03/10/2026", "Bảo dưỡng 40.000km toàn diện", TrangThaiPhieu.HOAN_TAT));

        if (isDbAvailable()) {
            try (EntityManager em = JpaUtil.createEntityManager()) {
                if (ktv != null && ktv.getMaNV() != null) {
                    Long dangSua = em.createQuery("SELECT COUNT(p) FROM PhieuTiepNhanSuaChua p WHERE p.kyThuatVienChinh.maNV = :manv AND p.trangThai = :st", Long.class)
                            .setParameter("manv", ktv.getMaNV())
                            .setParameter("st", TrangThaiPhieu.DANG_SUA).getSingleResult();
                    if (dangSua != null) data.phieuDangSua = dangSua.intValue();

                    Long choLk = em.createQuery("SELECT COUNT(p) FROM PhieuTiepNhanSuaChua p WHERE p.kyThuatVienChinh.maNV = :manv AND p.trangThai = :st", Long.class)
                            .setParameter("manv", ktv.getMaNV())
                            .setParameter("st", TrangThaiPhieu.CHO_LINH_KIEN).getSingleResult();
                    if (choLk != null) data.phieuChoLinhKien = choLk.intValue();
                }
            } catch (Exception ignored) {
            }
        }

        return data;
    }
}
