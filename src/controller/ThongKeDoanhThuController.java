package controller;

import dao.ThongKeDoanhThuDao;
import enums.VaiTro;

import java.time.LocalDate;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

/**
 * Điều phối báo cáo doanh thu cho Chủ cửa hàng: kiểm quyền, kiểm tra đầu vào
 * rồi tổng hợp số liệu từ {@link ThongKeDoanhThuDao} (chỉ đọc, không ghi DB).
 */
public class ThongKeDoanhThuController {
    public static final String DOANH_THU = "Doanh thu";
    public static final String XE_PHIEU = "Xe / phiếu sửa chữa";
    public static final String DICH_VU_LINH_KIEN = "Dịch vụ / linh kiện sử dụng nhiều";
    private static final String[] KHUNG_GIO = {"08:00 – 10:00", "10:00 – 12:00", "13:00 – 15:00", "15:00 – 18:00"};
    private final ThongKeDoanhThuDao dao;

    public ThongKeDoanhThuController() { this(new ThongKeDoanhThuDao()); }
    public ThongKeDoanhThuController(ThongKeDoanhThuDao dao) { this.dao = dao; }

    public BaoCao layBaoCao(VaiTro vaiTro, String loai, LocalDate tuNgay, LocalDate denNgay) {
        if (vaiTro != VaiTro.CHU_CUA_HANG) throw new SecurityException("Chỉ Chủ cửa hàng được xem báo cáo tổng hợp.");
        if (tuNgay == null || denNgay == null || denNgay.isBefore(tuNgay)) {
            throw new IllegalArgumentException("Khoảng ngày thống kê không hợp lệ.");
        }
        if (loai == null || (!DOANH_THU.equals(loai) && !XE_PHIEU.equals(loai) && !DICH_VU_LINH_KIEN.equals(loai))) {
            throw new IllegalArgumentException("Loại báo cáo không hợp lệ.");
        }
        BaoCao b = new BaoCao(loai, tuNgay, denNgay);
        if (DOANH_THU.equals(loai)) loadDoanhThu(b);
        else if (XE_PHIEU.equals(loai)) loadPhieu(b);
        else loadDichVuLinhKien(b);
        return b;
    }

    private void loadDoanhThu(BaoCao b) {
        Object[] total = dao.layTongHopDoanhThu(b.tuNgay, b.denNgay).get(0);
        b.soLuong = num(total[0]).intValue(); b.tongThu = money(total[1]); b.tienMat = money(total[2]); b.chuyenKhoan = money(total[3]);
        b.hoanTien = money(dao.layTongHoanTien(b.tuNgay, b.denNgay));
        for (Object[] row : dao.layHoanTienTheoKhungGio(b.tuNgay, b.denNgay)) {
            int index = num(row[0]).intValue(); if (index >= 0 && index < 4) b.hoanTienKhung[index] = money(row[1]);
        }
        for (Object[] row : dao.layDoanhThuTheoKhungGio(b.tuNgay, b.denNgay)) {
            int index = num(row[0]).intValue(); if (index >= 0 && index < 4) {
                b.soHoaDonKhung[index] = num(row[1]).intValue();
                b.tienMatKhung[index] = money(row[2]); b.chuyenKhoanKhung[index] = money(row[3]);
            }
        }
        b.doanhThuThuan = b.tongThu - b.hoanTien;
        for (int i = 0; i < 4; i++) {
            double tm = b.tienMatKhung[i], ck = b.chuyenKhoanKhung[i], refund = b.hoanTienKhung[i];
            b.rows.add(new Dong(KHUNG_GIO[i], Integer.toString(b.soHoaDonKhung[i]), fmt(tm), fmt(ck), fmt(tm + ck), fmt(refund), fmt(tm + ck - refund)));
        }
        b.rows.add(new Dong("TỔNG CỘNG", Integer.toString(b.soLuong), fmt(b.tienMat), fmt(b.chuyenKhoan), fmt(b.tongThu), fmt(b.hoanTien), fmt(b.doanhThuThuan)));
        int maxIndex = 0;
        for (int i = 1; i < 4; i++) if (netBucket(b, i) > netBucket(b, maxIndex)) maxIndex = i;
        b.chartLabels = KHUNG_GIO; b.chartValues = new double[4];
        for (int i = 0; i < 4; i++) b.chartValues[i] = Math.max(0, netBucket(b, i));
        b.insights.add(KHUNG_GIO[maxIndex] + " đạt " + fmt(netBucket(b, maxIndex)) + " VNĐ sau hoàn, cao nhất trong kỳ.");
        if (b.tongThu > 0) {
            b.insights.add(String.format("Chuyển khoản chiếm %.1f%% tổng thu; tiền mặt chiếm %.1f%%.", b.chuyenKhoan * 100 / b.tongThu, b.tienMat * 100 / b.tongThu));
            b.insights.add(String.format("Tỷ lệ hoàn tiền: %.1f%% tổng thu.", b.hoanTien * 100 / b.tongThu));
        } else b.insights.add("Chưa có hóa đơn đã thanh toán trong khoảng thời gian này.");
        b.insights.add(b.soLuong == 0 ? "Chưa có hóa đơn để tính doanh thu bình quân." : "Doanh thu sau hoàn bình quân: " + fmt(b.doanhThuThuan / b.soLuong) + " VNĐ/hóa đơn.");
        b.insights.add("Doanh thu sau hoàn không phải lợi nhuận.");
    }

    private void loadPhieu(BaoCao b) {
        Object[] total = dao.layTongHopPhieu(b.tuNgay, b.denNgay).get(0);
        b.xe = num(total[0]).intValue(); b.soLuong = num(total[1]).intValue();
        b.hoanTat = num(total[2]).intValue(); b.dangSua = num(total[3]).intValue();
        b.choLinhKien = num(total[4]).intValue(); b.choSua = num(total[5]).intValue();
        b.chartLabels = new String[]{"Chờ sửa", "Đang sửa", "Chờ linh kiện", "Hoàn tất"};
        b.chartValues = new double[]{b.choSua, b.dangSua, b.choLinhKien, b.hoanTat};
        for (Object[] r : dao.layPhieuTheoTrangThai(b.tuNgay, b.denNgay)) {
            b.rows.add(new Dong(statusName(String.valueOf(r[0])), String.valueOf(r[1]), String.valueOf(r[2])));
        }
        b.insights.add("Có " + b.xe + " xe được tiếp nhận trong kỳ.");
        b.insights.add(b.hoanTat + " / " + b.soLuong + " phiếu đã hoàn tất.");
        b.insights.add((b.dangSua + b.choLinhKien) + " phiếu đang xử lý tại xưởng.");
    }

    private void loadDichVuLinhKien(BaoCao b) {
        List<Double> values = new ArrayList<>(); List<String> labels = new ArrayList<>();
        for (Object[] r : dao.layDichVuVaLinhKien(b.tuNgay, b.denNgay)) {
            String loai = String.valueOf(r[0]), ma = String.valueOf(r[1]), ten = String.valueOf(r[2]);
            b.rows.add(new Dong(loai, ma, ten, String.valueOf(r[3]), String.valueOf(r[4]), fmt(money(r[5]))));
            if (labels.size() < 6) { labels.add(ma); values.add(money(r[5])); }
        }
        b.chartLabels = labels.toArray(String[]::new); b.chartValues = values.stream().mapToDouble(Double::doubleValue).toArray();
        b.soLuong = b.rows.size(); b.insights.add(b.rows.isEmpty() ? "Không có dịch vụ hoặc linh kiện được ghi nhận trong kỳ." : "Danh sách sắp theo doanh thu phát sinh, tối đa 20 mặt hàng.");
        b.insights.add("Số liệu sử dụng giá đã ghi nhận trên từng phiếu sửa chữa.");
    }

    private static double netBucket(BaoCao b, int i) { return b.tienMatKhung[i] + b.chuyenKhoanKhung[i] - b.hoanTienKhung[i]; }
    private static Number num(Object value) { return value instanceof Number n ? n : 0; }
    private static double money(Object value) { return num(value).doubleValue(); }
    private static String fmt(double value) { return new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("vi-VN"))).format(value); }
    private static String statusName(String status) { return switch (status) {
        case "CHO_SUA" -> "Chờ sửa"; case "DANG_SUA" -> "Đang sửa"; case "CHO_LINH_KIEN" -> "Chờ linh kiện"; case "HOAN_TAT" -> "Hoàn tất"; default -> status;
    }; }

    public static class BaoCao {
        public final String loai; public final LocalDate tuNgay, denNgay;
        public double tongThu, hoanTien, doanhThuThuan, tienMat, chuyenKhoan;
        public int soLuong, xe, hoanTat, dangSua, choLinhKien, choSua;
        public final double[] tienMatKhung = new double[4], chuyenKhoanKhung = new double[4], hoanTienKhung = new double[4];
        public final int[] soHoaDonKhung = new int[4];
        public String[] chartLabels = new String[0]; public double[] chartValues = new double[0];
        public final List<Dong> rows = new ArrayList<>(); public final List<String> insights = new ArrayList<>();
        private BaoCao(String loai, LocalDate tuNgay, LocalDate denNgay) { this.loai=loai; this.tuNgay=tuNgay; this.denNgay=denNgay; }
    }
    public static class Dong {
        public final String[] cells;
        public Dong(String... cells) { this.cells = cells; }
    }
}
