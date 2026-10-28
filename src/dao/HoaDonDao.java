package dao;

import entity.HoaDon;
import enums.TrangThaiThanhToan;

import java.util.List;

/**
 * Interface DAO cho bảng HoaDon.
 */
public interface HoaDonDao {

    /**
     * Tìm hóa đơn theo mã hóa đơn.
     *
     * @param maHoaDon mã hóa đơn (HDxxxxx)
     * @return HoaDon hoặc null nếu không tìm thấy
     */
    HoaDon timTheoMa(String maHoaDon);

    /**
     * Lấy danh sách tất cả hóa đơn.
     *
     * @return danh sách hóa đơn
     */
    List<HoaDon> layTatCa();

    /**
     * Lấy danh sách hóa đơn theo trạng thái thanh toán.
     *
     * @param trangThai trạng thái thanh toán
     * @return danh sách hóa đơn
     */
    List<HoaDon> timTheoTrangThai(TrangThaiThanhToan trangThai);

    /**
     * Thêm mới hóa đơn vào CSDL.
     *
     * @param hd hóa đơn cần thêm
     * @return true nếu thêm thành công
     */
    boolean them(HoaDon hd);
}
