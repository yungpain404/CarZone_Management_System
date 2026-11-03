package backup;

import ch.qos.logback.classic.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Điều phối luồng thoát ứng dụng:
 *  1. Kiểm tra đã có backup gần đây chưa → thoát nhanh nếu có
 *  2. Hiện ShutdownDialog với JProgressBar
 *  3. Chạy DIFF → LOG backup trên thread nền (SwingWorker)
 *  4. Sau khi xong: đóng dialog → System.exit(0)
 *  5. Đăng ký shutdown hook để flush log và dừng Quartz
 *
 * Gọi: AppShutdownManager.handleExit(mainFrame);
 */
public class AppShutdownManager {

    private static final Logger log = LoggerFactory.getLogger(AppShutdownManager.class);

    /** Đảm bảo shutdown hook chỉ đăng ký 1 lần. */
    private static final AtomicBoolean hookDaKhoiTao = new AtomicBoolean(false);

    private AppShutdownManager() {}

    /** Đăng ký shutdown hook (chỉ gọi 1 lần khi app khởi động). */
    public static void khoiTaoShutdownHook() {
        if (hookDaKhoiTao.compareAndSet(false, true)) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("[ShutdownHook] JVM đang tắt – dừng Quartz và flush log…");
                try {
                    BackupScheduler.getInstance().shutdown();
                } catch (Exception e) {
                    log.error("[ShutdownHook] Lỗi khi dừng BackupScheduler: {}", e.getMessage());
                }
                // Flush Logback để không mất dòng log cuối
                try {
                    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
                    lc.stop();
                } catch (Exception ignored) {}
            }, "carzone-shutdown-hook"));
        }
    }

    /**
     * Xử lý khi nhân viên bấm nút X / thoát.
     * Phải gọi trên EDT.
     *
     * @param owner JFrame chính (dùng để định vị dialog)
     */
    public static void handleExit(JFrame owner) {
        // 1. Kiểm tra backup gần đây – thoát nhanh
        if (BackupService.getInstance().vuaCoBackupGanDay()) {
            log.info("[AppShutdownManager] Có backup gần đây – thoát nhanh không cần sao lưu thêm.");
            System.exit(0);
            return;
        }

        // 2. Nếu khoá đang bận (Quartz đang backup theo lịch), chờ nó xong rồi thoát nhanh
        boolean lockDaBan = BackupService.getInstance().getBackupLock().isLocked();
        if (lockDaBan) {
            log.info("[AppShutdownManager] Quartz đang backup – chờ job hiện tại hoàn tất rồi thoát…");
            // Chờ trên thread nền để không block EDT
            ExecutorService waiter = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "carzone-exit-waiter");
                t.setDaemon(true);
                return t;
            });
            waiter.submit(() -> {
                try {
                    int timeoutSec = BackupConfig.getInstance().getExitTimeoutSeconds();
                    boolean acquired = BackupService.getInstance().getBackupLock()
                            .tryLock(timeoutSec, TimeUnit.SECONDS);
                    if (acquired) {
                        BackupService.getInstance().getBackupLock().unlock();
                    }
                } catch (InterruptedException ignored) {}
                // Sau khi job hiện tại xong → thoát
                SwingUtilities.invokeLater(() -> System.exit(0));
            });
            waiter.shutdown();
            return;
        }

        // 3. Không có backup gần đây, không có job đang chạy → hiện dialog và backup
        ShutdownDialog dialog = new ShutdownDialog(owner);
        lancBackupThoatVaHienDialog(dialog, owner, false);
        dialog.setVisible(true); // modal – block EDT đến khi dialog.dispose()
    }

    /**
     * Chạy backup DIFF → LOG trên thread nền, cập nhật progress vào dialog.
     * @param dialog     Dialog đang hiển thị
     * @param owner      Frame chính
     * @param laThayLai  true nếu đây là lần retry
     */
    private static void lancBackupThoatVaHienDialog(ShutdownDialog dialog, JFrame owner, boolean laThayLai) {
        int timeoutSec = BackupConfig.getInstance().getExitTimeoutSeconds();

        SwingWorker<Boolean, Integer> worker = new SwingWorker<>() {
            private String thongBaoLoi = null;

            @Override
            protected Boolean doInBackground() throws Exception {
                // Tiến độ ảo tăng dần đến ~45% trong khi DIFF chạy
                publish(0);
                SwingUtilities.invokeLater(() -> dialog.setStatus("Đang sao lưu thay đổi trong ca làm việc (DIFF)…"));

                // Chạy tiến độ ảo từ 0→44% trong khi chờ DIFF
                ProgressSimulator sim1 = new ProgressSimulator(0, 44, 30_000, p -> publish(p));
                sim1.start();

                BackupResult diffResult = BackupService.getInstance().thucHienBackup(BackupType.DIFF);
                sim1.stop();

                if (!diffResult.thanhCong()) {
                    thongBaoLoi = "Sao lưu DIFF thất bại: " + diffResult.thongBaoLoi();
                    return false;
                }
                publish(45);

                // LOG backup
                SwingUtilities.invokeLater(() -> dialog.setStatus("Đang chốt giao dịch cuối ca (LOG)…"));
                ProgressSimulator sim2 = new ProgressSimulator(45, 90, 20_000, p -> publish(p));
                sim2.start();

                BackupResult logResult = BackupService.getInstance().thucHienBackup(BackupType.LOG);
                sim2.stop();

                if (!logResult.thanhCong() && logResult.thongBaoLoi() != null
                        && !logResult.thongBaoLoi().startsWith("SKIP")) {
                    log.warn("[AppShutdownManager] LOG backup thất bại khi thoát: {} – tiếp tục thoát", logResult.thongBaoLoi());
                }

                publish(100);
                return true;
            }

            @Override
            protected void process(java.util.List<Integer> chunks) {
                int latest = chunks.get(chunks.size() - 1);
                dialog.setProgress(latest);
            }

            @Override
            protected void done() {
                try {
                    boolean ok = get();
                    if (ok) {
                        dialog.setStatus("✅ Sao lưu hoàn tất. Đang thoát…");
                        dialog.setProgress(100);
                        // Delay nhỏ để người dùng thấy 100%
                        Timer t = new Timer(600, e2 -> {
                            dialog.close();
                            System.exit(0);
                        });
                        t.setRepeats(false);
                        t.start();
                    } else {
                        dialog.showError(
                                thongBaoLoi,
                                () -> lancBackupThoatVaHienDialog(dialog, owner, true),
                                () -> {
                                    log.warn("[AppShutdownManager] Nhân viên chọn thoát không sao lưu.");
                                    dialog.close();
                                    System.exit(0);
                                }
                        );
                    }
                } catch (Exception e) {
                    dialog.showError(
                            "Lỗi không xác định: " + e.getMessage(),
                            () -> lancBackupThoatVaHienDialog(dialog, owner, true),
                            () -> {
                                log.warn("[AppShutdownManager] Thoát không sao lưu sau lỗi.", e);
                                dialog.close();
                                System.exit(0);
                            }
                    );
                }
            }
        };

        // Timeout: nếu quá timeoutSec giây vẫn chưa xong → cảnh báo
        Timer timeoutTimer = new Timer(timeoutSec * 1000, e -> {
            if (!worker.isDone()) {
                log.warn("[AppShutdownManager] Backup khi thoát quá {} giây – thông báo người dùng", timeoutSec);
                dialog.showError(
                        "Sao lưu mất quá " + timeoutSec + " giây. Vui lòng chờ thêm hoặc chọn thoát không sao lưu.",
                        () -> {}, // nút thử lại không làm gì – worker đang chạy
                        () -> {
                            log.warn("[AppShutdownManager] Thoát không sao lưu do timeout.");
                            worker.cancel(true);
                            dialog.close();
                            System.exit(0);
                        }
                );
            }
        });
        timeoutTimer.setRepeats(false);
        timeoutTimer.start();

        worker.execute();
    }

    // ─────────────────────────────────────────────────────────────────
    // Inner utility: mô phỏng tiến độ tăng dần từ min đến max
    // ─────────────────────────────────────────────────────────────────
    private static class ProgressSimulator {
        private final int from;
        private final int to;
        private final long totalMs;
        private final java.util.function.Consumer<Integer> publisher;
        private volatile boolean running = true;
        private Thread thread;

        ProgressSimulator(int from, int to, long totalMs, java.util.function.Consumer<Integer> publisher) {
            this.from = from;
            this.to = to;
            this.totalMs = totalMs;
            this.publisher = publisher;
        }

        void start() {
            int steps = to - from;
            long sleepMs = steps > 0 ? totalMs / steps : totalMs;
            thread = new Thread(() -> {
                for (int i = from; i <= to && running; i++) {
                    publisher.accept(i);
                    try { Thread.sleep(sleepMs); } catch (InterruptedException e) { break; }
                }
            }, "carzone-progress-sim");
            thread.setDaemon(true);
            thread.start();
        }

        void stop() {
            running = false;
            if (thread != null) thread.interrupt();
        }
    }
}

