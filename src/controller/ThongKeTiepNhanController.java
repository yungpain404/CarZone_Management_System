package controller;

import dao.ThongKeTiepNhanDao;
import dao.ThongKeTiepNhanDao.ThongKeData;
import dao.impl.ThongKeTiepNhanDaoImpl;
import enums.VaiTro;
import session.UserSession;

import java.util.Calendar;
import java.util.Date;

/**
 * Controller điều phối nghiệp vụ Thống kê tiếp nhận (UC013 - Tiếp nhận viên).
 * TNV chỉ xem thống kê trong phạm vi phiếu do mình lập (maNVTiepNhan = session.maNV).
 */
public class ThongKeTiepNhanController {

    private final ThongKeTiepNhanDao thongKeDao;

    public ThongKeTiepNhanController() {
        this.thongKeDao = new ThongKeTiepNhanDaoImpl();
    }

    /**
     * Lấy dữ liệu thống kê theo khoảng thời gian và trạng thái.
     * TNV tự động bị giới hạn theo mã nhân viên của mình.
     *
     * @param tuNgay    ngày bắt đầu (null = không giới hạn đầu)
     * @param denNgay   ngày kết thúc (null = ngày hiện tại)
     * @param trangThai lọc theo trạng thái phiếu (null = tất cả)
     * @return ThongKeData chứa KPI và danh sách phiếu chi tiết
     */
    public ThongKeData layThongKe(Date tuNgay, Date denNgay, String trangThai) {
        String maNVTiepNhan = null;
        VaiTro role = UserSession.getInstance().getVaiTro();
        if (role == VaiTro.TIEP_NHAN_VIEN) {
            maNVTiepNhan = UserSession.getInstance().getNhanVien().getMaNV();
        }

        // Nếu denNgay null → dùng ngày hiện tại cuối ngày
        if (denNgay == null) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            denNgay = cal.getTime();
        }

        // Nếu tuNgay null → mặc định là đầu tháng hiện tại
        if (tuNgay == null) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            tuNgay = cal.getTime();
        }

        return thongKeDao.layThongKe(maNVTiepNhan, tuNgay, denNgay, trangThai);
    }

    /**
     * Lấy thống kê nhanh cho tháng hiện tại (dùng cho Dashboard).
     */
    public ThongKeData layThongKeTiepNhanThangNay() {
        return layThongKe(null, null, null);
    }

    /**
     * Tính mảng ngày nhanh: 7 ngày gần nhất (dùng cho filter UI mặc định).
     */
    public Date[] layKhoang7NgayGanNhat() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        Date denNgay = cal.getTime();

        cal.add(Calendar.DAY_OF_YEAR, -6);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        Date tuNgay = cal.getTime();

        return new Date[]{tuNgay, denNgay};
    }

    /**
     * Tính mảng ngày: tháng hiện tại.
     */
    public Date[] layKhoangThangNay() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        Date tuNgay = cal.getTime();

        cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        Date denNgay = cal.getTime();

        return new Date[]{tuNgay, denNgay};
    }
}
