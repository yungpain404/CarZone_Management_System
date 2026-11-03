package dao;

import entity.HoaDon;
import enums.PhuongThucThanhToan;
import enums.TrangThaiThanhToan;

import java.util.List;

/**
 * Interface DAO cho bảng HoaDon (UC014, UC015 - Tiếp nhận viên).
 */
public interface HoaDonDao {

    /**
     * Tìm hóa đơn theo mã hóa đơn.
     */
    HoaDon timTheoMa(String maHoaDon);

    /**
     * Tìm hóa đơn theo mã phiếu sửa chữa.
     */
    HoaDon timTheoPhieu(String maPhieu);

    /**
     * Lấy danh sách tất cả hóa đơn.
     */
    List<HoaDon> layTatCa();

    /**
     * Lấy danh sách hóa đơn theo trạng thái thanh toán.
     */
    List<HoaDon> timTheoTrangThai(TrangThaiThanhToan trangThai);

    /**
     * Lấy danh sách các hóa đơn đã thanh toán và còn trong thời hạn đổi trả 7 ngày.
     */
    List<HoaDon> layHoaDonDuDieuKienDoiTra();

    /**
     * Lập hóa đơn mới - gọi stored procedure sp_LapHoaDon sinh mã HDddMMyyyyAXXX.
     * @return mã hóa đơn vừa sinh
     */
    String lapHoaDon(String maPhieu, String maNV, String maKM);

    /**
     * Thanh toán hóa đơn - gọi stored procedure sp_ThanhToanHoaDon.
     */
    boolean thanhToanHoaDon(String maHoaDon, PhuongThucThanhToan phuongThucTT, String maGiaoDich);

    /**
     * Tìm kiếm hóa đơn theo từ khóa (mã HD, mã phiếu, biển số xe, tên khách hàng).
     */
    List<HoaDon> timKiem(String tuKhoa, TrangThaiThanhToan trangThai);

    /**
     * Thêm mới hóa đơn vào CSDL.
     */
    boolean them(HoaDon hd);
}
