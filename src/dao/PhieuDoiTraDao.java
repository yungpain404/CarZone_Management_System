package dao;

import entity.ChiTietDoiTra;
import entity.PhieuDoiTra;

import java.util.List;

/**
 * Interface DAO cho bảng PhieuDoiTra và ChiTietDoiTra (UC012 - Tiếp nhận viên).
 */
public interface PhieuDoiTraDao {

    PhieuDoiTra timTheoMa(String maPhieuDT);

    List<PhieuDoiTra> layTatCa();

    List<PhieuDoiTra> layTheoTiepNhanVien(String maNV);

    List<ChiTietDoiTra> layChiTiet(String maPhieuDT);

    /**
     * Lập phiếu đổi trả và lưu các dòng chi tiết đổi trả trong một transaction.
     * Gọi stored procedure sp_LapPhieuDoiTra để sinh mã PDT...
     * @return mã phiếu đổi trả vừa sinh
     */
    String lapPhieuDoiTra(PhieuDoiTra pdt, List<ChiTietDoiTra> dsChiTiet);

    List<PhieuDoiTra> timKiem(String tuKhoa, String maNV);
}
