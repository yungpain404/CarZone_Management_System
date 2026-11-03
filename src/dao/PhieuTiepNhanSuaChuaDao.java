package dao;

import entity.ChiTietDichVu;
import entity.PhieuTiepNhanSuaChua;
import enums.TrangThaiPhieu;

import java.util.List;

/**
 * Interface DAO cho thực thể PhieuTiepNhanSuaChua (UC010, UC011, UC013 - Tiếp nhận viên).
 */
public interface PhieuTiepNhanSuaChuaDao {

    PhieuTiepNhanSuaChua timTheoMa(String maPhieu);

    List<PhieuTiepNhanSuaChua> layTatCa();

    List<PhieuTiepNhanSuaChua> layTheoTiepNhanVien(String maNVTiepNhan);

    /**
     * Lấy các phiếu sửa chữa đã HOAN_TAT và chưa từng lập hóa đơn (đủ điều kiện lập hóa đơn).
     */
    List<PhieuTiepNhanSuaChua> layDanhSachDuDieuKienLapHoaDon();

    /**
     * Lập phiếu tiếp nhận - gọi stored procedure sp_LapPhieuTiepNhan sinh mã và lưu các dịch vụ đề nghị.
     * @return mã phiếu PSC... vừa sinh
     */
    String lapPhieuTiepNhan(PhieuTiepNhanSuaChua phieu, List<ChiTietDichVu> dsDichVu);

    /**
     * Cập nhật trạng thái phiếu.
     */
    boolean capNhatTrangThai(String maPhieu, TrangThaiPhieu trangThai);

    /**
     * Tìm kiếm phiếu tiếp nhận.
     */
    List<PhieuTiepNhanSuaChua> timKiem(String tuKhoa, String maNVTiepNhan, TrangThaiPhieu trangThai);
}
