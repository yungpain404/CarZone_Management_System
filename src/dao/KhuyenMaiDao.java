package dao;

import entity.KhuyenMai;
import java.util.List;

/**
 * Interface DAO cho thực thể KhuyenMai.
 */
public interface KhuyenMaiDao {

    KhuyenMai timTheoMa(String maKM);

    /**
     * Lấy danh sách khuyến mãi còn hiệu lực tại thời điểm hiện tại (ngayBatDau <= now <= ngayKetThuc).
     */
    List<KhuyenMai> layKhuyenMaiHieuLuc();

    List<KhuyenMai> layTatCa();
}
