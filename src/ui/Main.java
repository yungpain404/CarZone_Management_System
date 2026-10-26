package ui;

import com.formdev.flatlaf.FlatLightLaf;
import connectDB.JpaUtil;
import entity.NhanVien;

import javax.swing.*;
import java.awt.*;

/**
 * Entry point của ứng dụng CarZone.
 * Khởi động Look & Feel, mở màn hình đăng nhập.
 * Sau khi đăng nhập thành công, chuyển sang màn hình chính tương ứng với vai trò.
 *
 * pom.xml trỏ main.class = ui.Main
 */
public class Main {

    public static void main(String[] args) {
        // 1. Cài FlatLaf Light đồng bộ giao diện sáng theo thiết kế Figma
        FlatLightLaf.setup();

        // Tuỳ chỉnh theme token theo chuẩn Figma
        UIManager.put("Button.arc", 12);
        UIManager.put("Component.arc", 12);
        UIManager.put("TextComponent.arc", 12);
        UIManager.put("defaultFont", new Font("SansSerif", Font.PLAIN, 13));

        // 2. Chạy trên Event Dispatch Thread: Mở Splash Screen (KhoiDongUI) trước
        SwingUtilities.invokeLater(() -> {
            KhoiDongUI splashFrame = new KhoiDongUI();

            splashFrame.setKhoiDongListener(() -> {
                // Sau khi hoàn tất khởi động, mở màn hình Đăng nhập
                SwingUtilities.invokeLater(() -> {
                    DangNhapUI loginFrame = new DangNhapUI();
                    loginFrame.setDangNhapListener(nhanVien -> {
                        moManHinhChinh(nhanVien);
                    });
                    loginFrame.setVisible(true);
                });
            });

            splashFrame.setVisible(true);
        });
    }

    /** Mở màn hình chính theo vai trò sau khi đăng nhập thành công. */
    private static void moManHinhChinh(NhanVien nhanVien) {
        // Placeholder – sẽ thay thế bằng màn hình thật của từng vai trò
        SwingUtilities.invokeLater(() -> {
            String tenVaiTro = switch (nhanVien.getVaiTro()) {
                case CHU_CUA_HANG     -> "Chủ cửa hàng";
                case TIEP_NHAN_VIEN   -> "Tiếp nhận viên";
                case KY_THUAT_VIEN    -> "Kỹ thuật viên";
            };

            JOptionPane.showMessageDialog(
                    null,
                    "<html><b>Đăng nhập thành công!</b><br><br>"
                    + "Xin chào: " + nhanVien.getHoTen() + "<br>"
                    + "Vai trò: " + tenVaiTro + "<br>"
                    + "Mã NV: " + nhanVien.getMaNV() + "</html>",
                    "CarZone",
                    JOptionPane.INFORMATION_MESSAGE);

            // Đóng EMF khi thoát hẳn
            JFrame frame = new JFrame("CarZone – " + tenVaiTro);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    JpaUtil.close();
                }
            });
            frame.setSize(1280, 800);
            frame.setLocationRelativeTo(null);
            // TODO: thêm JPanel nội dung tương ứng vai trò
            frame.setVisible(true);
        });
    }
}
