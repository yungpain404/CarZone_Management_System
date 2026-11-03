package dao;

import entity.ChiTietLinhKien;
import java.sql.Connection;
import java.util.List;

/**
 * Interface DAO cho bảng ChiTietLinhKien.
 */
public interface ChiTietLinhKienDao {

    List<ChiTietLinhKien> layTheoPhieu(String maPhieu);

    boolean them(ChiTietLinhKien ctlk);

    boolean them(ChiTietLinhKien ctlk, Connection con);
}
