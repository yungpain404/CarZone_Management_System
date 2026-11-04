package controller;

import dao.ThongKeTiepNhanDao;
import dao.ThongKeTiepNhanDao.SoSanhNamData;
import dao.ThongKeTiepNhanDao.SoSanhThangData;
import dao.ThongKeTiepNhanDao.ThongKeData;
import dao.ThongKeTiepNhanDao.TinhHinhXuLyData;
import dao.impl.ThongKeTiepNhanDaoImpl;
import enums.VaiTro;
import session.UserSession;

import java.util.Calendar;
import java.util.Date;

/**
 * Controller điều phối nghiệp vụ Thống kê tiếp nhận (UC013).
 * TNV tự động bị giới hạn theo mã nhân viên của mình (maNVTiepNhan = session.maNV).
 * Quản lý / Chủ cửa hàng được quyền xem toàn bộ gara.
 */
public class ThongKeTiepNhanController {

    private final ThongKeTiepNhanDao thongKeDao;

    public ThongKeTiepNhanController() {
        this.thongKeDao = new ThongKeTiepNhanDaoImpl();
    }

    private String layMaNVNeuLaTNV() {
        if (UserSession.getInstance() != null && UserSession.getInstance().getVaiTro() == VaiTro.TIEP_NHAN_VIEN) {
            if (UserSession.getInstance().getNhanVien() != null) {
                return UserSession.getInstance().getNhanVien().getMaNV();
            }
        }
        return null;
    }

    /**
     * Lấy dữ liệu thống kê tổng quan (Phần 1).
     */
    public ThongKeData layThongKe(Date tuNgay, Date denNgay, String trangThai) {
        String maNV = layMaNVNeuLaTNV();
        return thongKeDao.layThongKe(maNV, tuNgay, denNgay, trangThai);
    }

    /**
     * Lấy dữ liệu so sánh Tháng này vs Tháng trước (Phần 2 - Dashboard A).
     */
    public SoSanhThangData laySoSanhThang() {
        String maNV = layMaNVNeuLaTNV();
        return thongKeDao.laySoSanhThang(maNV);
    }

    /**
     * Lấy dữ liệu so sánh Năm nay vs Năm trước (Phần 2 - Dashboard B).
     */
    public SoSanhNamData laySoSanhNam() {
        String maNV = layMaNVNeuLaTNV();
        return thongKeDao.laySoSanhNam(maNV);
    }

    /**
     * Lấy dữ liệu tình hình xử lý và tồn đọng phiếu (Phần 3).
     */
    public TinhHinhXuLyData layTinhHinhXuLy() {
        String maNV = layMaNVNeuLaTNV();
        return thongKeDao.layTinhHinhXuLy(maNV);
    }

    /**
     * Tính mảng ngày: 7 ngày gần nhất (từ ngày = hiện tại - 6 ngày, đến ngày = hiện tại).
     */
    public Date[] layKhoang7NgayGanNhat() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        Date denNgay = cal.getTime();

        cal.add(Calendar.DAY_OF_YEAR, -6);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date tuNgay = cal.getTime();

        return new Date[]{tuNgay, denNgay};
    }

    /**
     * Tính mảng ngày: Hôm nay.
     */
    public Date[] layKhoangHomNay() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date tuNgay = cal.getTime();

        cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        Date denNgay = cal.getTime();

        return new Date[]{tuNgay, denNgay};
    }

    /**
     * Tính mảng ngày: Tháng này.
     */
    public Date[] layKhoangThangNay() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date tuNgay = cal.getTime();

        cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        Date denNgay = cal.getTime();

        return new Date[]{tuNgay, denNgay};
    }
}
