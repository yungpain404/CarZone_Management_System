package controller;

import dao.ThongKeDoanhThuDAO;
import enums.VaiTro;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Điều phối báo cáo doanh thu cho Chủ cửa hàng: kiểm quyền, kiểm tra đầu vào
 * rồi tổng hợp số liệu từ {@link ThongKeDoanhThuDAO} (chỉ đọc, không ghi DB).
 */
public class ThongKeDoanhThuController {
    public static final String DOANH_THU = "Doanh thu";
    public static final String XE_PHIEU = "Xe / phiếu sửa chữa";
    public static final String DICH_VU_LINH_KIEN = "Dịch vụ / linh kiện sử dụng nhiều";
    private static final DateTimeFormatter NGAY_NHAN = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter THANG_NHAN = DateTimeFormatter.ofPattern("MM/yyyy");
    private final ThongKeDoanhThuDAO dao;

    public ThongKeDoanhThuController() { this(new ThongKeDoanhThuDAO()); }
    public ThongKeDoanhThuController(ThongKeDoanhThuDAO dao) { this.dao = dao; }

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
        b.doanhThuThuan = b.tongThu - b.hoanTien;

        long soNgay = ChronoUnit.DAYS.between(b.tuNgay, b.denNgay) + 1;
        boolean theoNgay = soNgay <= 14;
        boolean theoTuan = soNgay <= 180;
        boolean theoThang = soNgay <= 730;
        Map<LocalDate, RevenueBucket> buckets = new LinkedHashMap<>();
        for (LocalDate date = b.tuNgay; !date.isAfter(b.denNgay);) {
            LocalDate start;
            LocalDate end;
            String label;
            if (theoNgay) {
                start = date; end = date; label = NGAY_NHAN.format(date);
                date = date.plusDays(1);
            } else if (theoTuan) {
                start = date; end = date.plusDays(6).isAfter(b.denNgay) ? b.denNgay : date.plusDays(6);
                label = NGAY_NHAN.format(start);
                date = end.plusDays(1);
            } else if (theoThang) {
                YearMonth month = YearMonth.from(date);
                start = date; end = month.atEndOfMonth().isAfter(b.denNgay) ? b.denNgay : month.atEndOfMonth();
                label = THANG_NHAN.format(date);
                date = end.plusDays(1);
            } else {
                start = date; end = date.withDayOfYear(date.lengthOfYear());
                if (end.isAfter(b.denNgay)) end = b.denNgay;
                label = Integer.toString(date.getYear());
                date = end.plusDays(1);
            }
            buckets.put(start, new RevenueBucket(label, start, end));
        }

        TreeMap<LocalDate, RevenueBucket> byDate = new TreeMap<>(buckets);
        for (Object[] row : dao.layDoanhThuTheoNgay(b.tuNgay, b.denNgay)) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            RevenueBucket bucket = bucketFor(byDate, date);
            if (bucket != null) {
                bucket.invoices += num(row[1]).intValue();
                bucket.cash += money(row[2]);
                bucket.transfer += money(row[3]);
            }
        }
        for (Object[] row : dao.layHoanTienTheoNgay(b.tuNgay, b.denNgay)) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            RevenueBucket bucket = bucketFor(byDate, date);
            if (bucket != null) bucket.refund += money(row[1]);
        }

        b.chartLabels = new String[buckets.size()];
        b.chartValues = new double[buckets.size()];
        int index = 0, maxIndex = 0;
        double maxRevenue = Double.NEGATIVE_INFINITY;
        for (RevenueBucket bucket : buckets.values()) {
            double gross = bucket.cash + bucket.transfer;
            double net = gross - bucket.refund;
            b.chartLabels[index] = bucket.label;
            b.chartValues[index] = Math.max(0, net);
            b.rows.add(new Dong(bucket.label, Integer.toString(bucket.invoices), fmt(bucket.cash), fmt(bucket.transfer), fmt(gross), fmt(bucket.refund), fmt(net)));
            if (net > maxRevenue) { maxRevenue = net; maxIndex = index; }
            index++;
        }
        b.rows.add(new Dong("TỔNG CỘNG", Integer.toString(b.soLuong), fmt(b.tienMat), fmt(b.chuyenKhoan), fmt(b.tongThu), fmt(b.hoanTien), fmt(b.doanhThuThuan)));
        RevenueBucket peak = buckets.values().stream().skip(maxIndex).findFirst().orElse(null);
        String peakPeriod = peak == null ? "Kỳ" : (theoNgay ? "Ngày " : theoTuan ? "Tuần bắt đầu " : theoThang ? "Tháng " : "Năm ") + peak.label;
        b.insights.add(peakPeriod + " đạt " + fmt(peak == null ? 0 : peak.net()) + " VNĐ sau hoàn, cao nhất trong kỳ.");
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

    private static RevenueBucket bucketFor(TreeMap<LocalDate, RevenueBucket> buckets, LocalDate date) {
        Map.Entry<LocalDate, RevenueBucket> entry = buckets.floorEntry(date);
        return entry != null && !date.isAfter(entry.getValue().end) ? entry.getValue() : null;
    }
    private static final class RevenueBucket {
        final String label; final LocalDate start, end; int invoices; double cash, transfer, refund;
        RevenueBucket(String label, LocalDate start, LocalDate end) { this.label=label; this.start=start; this.end=end; }
        double net() { return cash + transfer - refund; }
    }
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
