package dao;

import entity.DichVu;
import java.util.List;

/**
 * Interface DAO cho thực thể DichVu.
 */
public interface DichVuDao {

    DichVu timTheoMa(String maDV);

    List<DichVu> layTatCaDangCungCap();

    List<DichVu> layTatCa();

    List<DichVu> timKiem(String tuKhoa, Boolean dangCungCap);
}
