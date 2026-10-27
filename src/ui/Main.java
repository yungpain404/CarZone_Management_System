package ui;

import com.formdev.flatlaf.FlatLightLaf;
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

    /** Mở màn hình Dashboard theo vai trò sau khi đăng nhập thành công. */
    private static void moManHinhChinh(NhanVien nhanVien) {
        SwingUtilities.invokeLater(() -> {
            DashboardUI dashboard = new DashboardUI(nhanVien);
            dashboard.setVisible(true);
        });
    }
}
