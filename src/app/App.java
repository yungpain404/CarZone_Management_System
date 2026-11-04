package app;

import backup.AppShutdownManager;
import backup.BackupScheduler;
import com.formdev.flatlaf.FlatLightLaf;
import entity.NhanVien;
import enums.VaiTro;
import ui.DangNhapUI;
import ui.KhoiDongUI;
import ui.MainFrame;

import javax.swing.*;
import java.awt.*;

/**
 * Entry point của ứng dụng CarZone.
 * Khởi động Look & Feel FlatLightLaf, chạy Splash Screen khởi động hệ thống.
 * Sau khi đăng nhập thành công, mở màn hình chính MainFrame theo phân quyền vai
 * trò.
 *
 * pom.xml trỏ main.class = app.App
 */
public class App {

    public static void main(String[] args) {
        // 1. Cài FlatLaf Light đồng bộ giao diện sáng theo thiết kế Figma
        FlatLightLaf.setup();

        // Tuỳ chỉnh theme token theo chuẩn Figma
        UIManager.put("Button.arc", 12);
        UIManager.put("Component.arc", 12);
        UIManager.put("TextComponent.arc", 12);
        UIManager.put("defaultFont", new Font("SansSerif", Font.PLAIN, 13));

        // 2. Khởi tạo Shutdown Hook dọn dẹp hệ thống
        AppShutdownManager.khoiTaoShutdownHook();

        // 3. Chạy trên Event Dispatch Thread: Mở Splash Screen (KhoiDongUI) trước
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

    /** Mở màn hình chính MainFrame theo vai trò sau khi đăng nhập thành công. */
    public static void moManHinhChinh(NhanVien nhanVien) {
        // Chỉ khởi chạy hệ thống sao lưu tự động Quartz trên máy của Chủ cửa hàng
        if (nhanVien != null && nhanVien.getVaiTro() == VaiTro.CHU_CUA_HANG) {
            Thread backupInitThread = new Thread(() -> {
                try {
                    BackupScheduler.getInstance().start();
                } catch (Throwable t) {
                    // Đã được log bên trong BackupScheduler
                }
            }, "carzone-cch-backup-scheduler");
            backupInitThread.setDaemon(true);
            backupInitThread.start();
        } else {
            // Các role khác (Tiếp nhận viên, Kỹ thuật viên) không chạy backup tự động
            BackupScheduler.getInstance().shutdown();
        }

        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame(nhanVien);
            mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            mainFrame.setVisible(true);
        });
    }
}
