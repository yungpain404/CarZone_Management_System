package dao;

import entity.Xe;
import java.util.List;

/**
 * Interface DAO cho thực thể Xe (UC016 - Tiếp nhận viên).
 */
public interface XeDao {

    /**
     * Tìm xe theo biển số xe.
     */
    Xe timTheoBienSo(String bienSoXe);

    /**
     * Lấy danh sách xe thuộc về một khách hàng cụ thể.
     */
    List<Xe> layTheoKhachHang(String maKH);

    /**
     * Tìm kiếm xe theo từ khóa (biển số, hãng xe, model, hoặc mã khách hàng).
     */
    List<Xe> timKiem(String tuKhoa);

    /**
     * Lấy danh sách tất cả xe trong hệ thống.
     */
    List<Xe> layTatCa();

    /**
     * Thêm mới xe vào cơ sở dữ liệu.
     */
    boolean them(Xe xe);

    /**
     * Cập nhật thông tin xe.
     */
    boolean capNhat(Xe xe);

    /**
     * Kiểm tra xe đã từng có phiếu sửa chữa nào chưa.
     */
    boolean kiemTraCoPhieu(String bienSoXe);
}
