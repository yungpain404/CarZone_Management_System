package dao;

import connectDB.JpaUtil;
import entity.TaiKhoan;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

/**
 * DAO truy vấn bảng TaiKhoan.
 * Chỉ truy vấn / lưu, không chứa luật nghiệp vụ.
 */
public class TaiKhoanDao {

    /**
     * Tìm TaiKhoan theo tên đăng nhập (JOIN FETCH NhanVien để tránh lazy N+1).
     *
     * @param tenDangNhap tên đăng nhập (CCHxxx / TNVxxx / KTVxxx)
     * @return TaiKhoan kèm NhanVien, hoặc {@code null} nếu không tìm thấy
     */
    public TaiKhoan timTheoTenDangNhap(String tenDangNhap) {
        EntityManager em = JpaUtil.createEntityManager();
        try {
            TypedQuery<TaiKhoan> q = em.createQuery(
                    "SELECT t FROM TaiKhoan t JOIN FETCH t.nhanVien nv "
                    + "WHERE t.tenDangNhap = :ten",
                    TaiKhoan.class);
            q.setParameter("ten", tenDangNhap);
            return q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            em.close();
        }
    }
}
