package dao;

import entity.ChiTietDichVu;
import java.sql.Connection;
import java.util.List;

/**
 * Interface DAO cho bảng ChiTietDichVu.
 */
public interface ChiTietDichVuDao {

    List<ChiTietDichVu> layTheoPhieu(String maPhieu);

    boolean them(ChiTietDichVu ctdv);

    boolean them(ChiTietDichVu ctdv, Connection con);

    boolean xoaTheoPhieu(String maPhieu);
}
