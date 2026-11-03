package dao;

import entity.LinhKien;
import java.util.List;

/**
 * Interface DAO cho thực thể LinhKien.
 */
public interface LinhKienDao {

    LinhKien timTheoMa(String maLK);

    List<LinhKien> layTatCaDangKinhDoanh();

    List<LinhKien> layTatCa();

    List<LinhKien> timKiem(String tuKhoa, Boolean dangKinhDoanh);
}
