package dao;

import entity.NhanVien;
import enums.VaiTro;

import java.util.List;

/**
 * Interface DAO cho bảng NhanVien – chỉ đọc/lưu dữ liệu, không chứa luật nghiệp vụ.
 */
public interface NhanVienDao {

    /**
     * Tìm nhân viên theo mã nhân viên.
     *
     * @param maNV mã nhân viên (NVxxxxx)
     * @return NhanVien hoặc null nếu không tìm thấy
     */
    NhanVien timTheoMa(String maNV);

    /**
     * Lấy danh sách tất cả nhân viên.
     *
     * @return danh sách nhân viên
     */
    List<NhanVien> layTatCa();

    /**
     * Tìm danh sách nhân viên theo vai trò.
     *
     * @param vaiTro vai trò nhân viên
     * @return danh sách nhân viên thuộc vai trò
     */
    List<NhanVien> timTheoVaiTro(VaiTro vaiTro);

    /**
     * Thêm mới nhân viên vào CSDL.
     *
     * @param nv nhân viên cần thêm
     * @return true nếu thêm thành công
     */
    boolean them(NhanVien nv);

    /**
     * Cập nhật thông tin nhân viên.
     *
     * @param nv nhân viên cần cập nhật
     * @return true nếu cập nhật thành công
     */
    boolean capNhat(NhanVien nv);
}
