package audit;

import backup.BackupService;
import backup.BackupType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import session.UserSession;
import ui.DangNhapUI;
import ui.PinVerificationDialog;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Điều phối quy trình xác thực mã PIN bảo mật cấp 2 trên giao diện Swing (EDT).
 * Xử lý chặn ngang tiến trình, đếm số lần sai, khóa phiên làm việc và sao lưu khẩn cấp.
 */
public final class PinVerificationManager {

    private static final Logger log = LoggerFactory.getLogger(PinVerificationManager.class);

    /** Bộ đếm số lần nhập sai PIN liên tiếp */
    private static final AtomicInteger SO_LAN_SAI = new AtomicInteger(0);

    private PinVerificationManager() {}

    /**
     * Yêu cầu xác thực mã PIN khi phát hiện hành vi bất thường.
     * Có thể gọi an toàn từ luồng nền (SwingWorker) hoặc luồng EDT.
     *
     * @param viPham Thông tin hành vi phi lý được ghi nhận
     * @return true nếu người dùng nhập đúng mã PIN; false nếu hủy bỏ hoặc vượt quá số lần thử
     */
    public static boolean yeuCauXacThuc(ThongTinViPham viPham) {
        log.info("Kích hoạt quy trình yêu cầu mã PIN bảo mật cho hành vi: [{}]", viPham.loai().name());

        if (GraphicsEnvironment.isHeadless()) {
            log.warn("Môi trường Headless (không có màn hình GUI) - Tự động từ chối xác thực mã PIN.");
            return false;
        }

        AtomicBoolean ketQua = new AtomicBoolean(false);

        // Chạy trên luồng EDT
        Runnable showDialogTask = () -> {
            Frame activeFrame = timCuaSoChinh();
            int maxAttempts = SecurityConfig.getMaxAttempts();

            while (SO_LAN_SAI.get() < maxAttempts) {
                int soLanConLai = maxAttempts - SO_LAN_SAI.get();

                PinVerificationDialog dialog = new PinVerificationDialog(activeFrame, viPham, soLanConLai);
                dialog.setVisible(true);

                if (dialog.isDaXacNhan()) {
                    // Nhập đúng PIN
                    SO_LAN_SAI.set(0);
                    ketQua.set(true);
                    log.info("Xác thực mã PIN thành công bởi nhân viên [{}]", viPham.nguoiThucHien());
                    return;
                }

                // Nếu người dùng đóng dialog mà chưa nhập mã PIN hợp lệ
                String rawPin = dialog.getPinDaNhap();
                if (rawPin.isEmpty()) {
                    // Người dùng chủ động bấm Hủy
                    log.warn("Người dùng đã hủy thao tác xác thực mã PIN.");
                    ketQua.set(false);
                    return;
                }

                // Nhập sai PIN -> Tăng số lần sai
                int saiMoi = SO_LAN_SAI.incrementAndGet();
                log.warn("Nhập sai mã PIN lần {}/{}", saiMoi, maxAttempts);

                if (saiMoi < maxAttempts) {
                    JOptionPane.showMessageDialog(
                            activeFrame,
                            "Mã PIN không chính xác! Bạn còn " + (maxAttempts - saiMoi) + " lần thử.",
                            "Sai mã PIN",
                            JOptionPane.WARNING_MESSAGE
                    );
                } else {
                    // Đã sai quá 3 lần -> KÍCH HOẠT KHÓA PHIÊN KHẨN CẤP
                    kichHoatKhoaPhienVaCanhBao(viPham);
                    ketQua.set(false);
                    return;
                }
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            showDialogTask.run();
        } else {
            try {
                // Tạm dừng luồng nền của Database để chờ người dùng xác thực trên EDT
                SwingUtilities.invokeAndWait(showDialogTask);
            } catch (Throwable t) {
                log.error("Lỗi khi điều phối hộp thoại xác thực PIN trên EDT: {}", t.getMessage(), t);
                ketQua.set(false);
            }
        }

        return ketQua.get();
    }

    /**
     * Kích hoạt xử lý khẩn cấp khi nhập sai PIN quá 3 lần:
     * 1. Hủy phiên làm việc (Force Logout).
     * 2. Kích hoạt sao lưu khẩn cấp (Emergency Log Backup).
     * 3. Gửi cảnh báo khẩn cấp tới kênh giám sát (ntfy.sh / Audit Log).
     * 4. Đóng toàn bộ giao diện và đưa về màn hình Đăng nhập.
     */
    public static void kichHoatKhoaPhienVaCanhBao(ThongTinViPham viPham) {
        log.error("CẢNH BÁO KHẨN CẤP: Nhập sai mã PIN quá 3 lần! Tiến hành khóa phiên và sao lưu khẩn cấp...");

        // 1. Gửi cảnh báo khẩn cấp tới ntfy.sh và Audit Logger
        AlertNotifier.guiCanhBaoKhanCap(viPham, SO_LAN_SAI.get(), true);

        // 2. Kích hoạt sao lưu khẩn cấp dữ liệu (Emergency Transaction Log Backup)
        Thread backupThread = new Thread(() -> {
            try {
                log.info("Đang thực hiện sao lưu CSDL khẩn cấp trước khi cưỡng chế thoát...");
                BackupService.getInstance().thucHienBackup(BackupType.LOG);
            } catch (Throwable t) {
                log.error("Sao lưu khẩn cấp thất bại: {}", t.getMessage());
            }
        }, "carzone-emergency-backup");
        backupThread.setDaemon(true);
        backupThread.start();

        // 3. Hiển thị thông báo cưỡng chế đăng xuất trên EDT
        SwingUtilities.invokeLater(() -> {
            Frame activeFrame = timCuaSoChinh();
            JOptionPane.showMessageDialog(
                    activeFrame,
                    "<html><div style='width: 320px; font-family: sans-serif;'>"
                            + "<b style='color: #DC2626; font-size: 13px;'>HỆ THỐNG ĐÃ BỊ KHÓA AN NINH!</b><br><br>"
                            + "Bạn đã nhập sai Mã PIN quá 3 lần liên tiếp.<br>"
                            + "Hệ thống đã tự động hủy bỏ giao dịch, kích hoạt sao lưu khẩn cấp "
                            + "và đóng phiên làm việc hiện tại để bảo vệ dữ liệu."
                            + "</div></html>",
                    "Cưỡng chế đóng phiên làm việc",
                    JOptionPane.ERROR_MESSAGE
            );

            // 4. Force Logout và dọn dẹp phiên
            UserSession.getInstance().huyPhien();
            SO_LAN_SAI.set(0);

            // Đóng tất cả các Frame đang mở
            for (Frame f : Frame.getFrames()) {
                if (f.isDisplayable()) {
                    f.dispose();
                }
            }

            // Mở lại màn hình Đăng nhập
            DangNhapUI loginFrame = new DangNhapUI();
            loginFrame.setDangNhapListener(app.App::moManHinhChinh);
            loginFrame.setVisible(true);
        });
    }

    private static Frame timCuaSoChinh() {
        for (Frame f : Frame.getFrames()) {
            if (f instanceof JFrame && f.isVisible() && f.isActive()) {
                return f;
            }
        }
        for (Frame f : Frame.getFrames()) {
            if (f instanceof JFrame && f.isVisible()) {
                return f;
            }
        }
        return null;
    }
}

