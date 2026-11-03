package dao;

import entity.KhachHang;
import java.util.List;

/**
 * Interface DAO cho thực thể KhachHang (UC016 - Tiếp nhận viên).
 */
public interface KhachHangDao {

    /**
     * Tìm khách hàng theo mã khách hàng (KHxxxxxx).
     */
    KhachHang timTheoMa(String maKH);

    /**
     * Tìm khách hàng theo số điện thoại (duy nhất).
     */
    KhachHang timTheoSoDienThoai(String soDienThoai);

    /**
     * Tìm kiếm khách hàng theo từ khóa (họ tên, số điện thoại hoặc CCCD).
     */
    List<KhachHang> timKiem(String tuKhoa);

    /**
     * Lấy danh sách tất cả khách hàng.
     */
    List<KhachHang> layTatCa();

    /**
     * Thêm mới khách hàng.
     * Mã khách hàng (maKH) do DB sinh tự động từ sequence seq_KhachHang.
     * @return maKH vừa được sinh
     */
    String them(KhachHang kh);

    /**
     * Cập nhật thông tin khách hàng.
     */
    boolean capNhat(KhachHang kh);

    /**
     * Kiểm tra xem khách hàng đã có xe hoặc phiếu tiếp nhận sửa chữa chưa (để chặn xóa).
     */
    boolean kiemTraCoXeHoacPhieu(String maKH);
}
