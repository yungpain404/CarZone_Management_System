package audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Tiện ích gửi cảnh báo an ninh khẩn cấp đến kênh giám sát (ntfy.sh / webhook / audit log).
 * Thực thi bất đồng bộ trên daemon thread để không bao giờ làm trễ hay treo ứng dụng.
 */
public final class AlertNotifier {

    private static final Logger log = LoggerFactory.getLogger(AlertNotifier.class);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private AlertNotifier() {}

    /**
     * Gửi cảnh báo vi phạm an ninh nghiêm trọng.
     */
    public static void guiCanhBaoKhanCap(ThongTinViPham viPham, int soLanSai, boolean daKhoaPhien) {
        String tieuDe = "🚨 [CẢNH BÁO BẢO MẬT CARZONE] Phát hiện hành vi phi lý!";
        String noiDung = String.format(
                "Loại vi phạm: %s\n"
                        + "Chi tiết: %s\n"
                        + "Bảng / Mục tiêu: %s\n"
                        + "Người thực hiện: %s\n"
                        + "Số lần sai PIN: %d\n"
                        + "Trạng thái: %s\n"
                        + "Thời điểm: %s",
                viPham.loai().getMoTa(),
                viPham.chiTiet(),
                viPham.tenBang(),
                viPham.nguoiThucHien(),
                soLanSai,
                daKhoaPhien ? "ĐÃ KHÓA PHIÊN & KÍCH HOẠT SAO LƯU KHẨN CẤP" : "ĐANG YÊU CẦU MÃ PIN",
                viPham.thoiDiem()
        );

        // 1. Ghi log kiểm toán Audit mức CRITICAL
        AuditLogger.logSecurityAlert(viPham.loai().name(), viPham.chiTiet(), viPham.nguoiThucHien(), daKhoaPhien);
        log.error("AN NINH: {}\n{}", tieuDe, noiDung);

        // 2. Bắn thông báo ra ntfy.sh (hoặc Telegram webhook) qua thread nền
        Thread alertThread = new Thread(() -> {
            try {
                String server = SecurityConfig.getNtfyUrl();
                String topic = SecurityConfig.getNtfyTopic();
                if (server == null || server.isBlank() || topic == null || topic.isBlank()) {
                    return;
                }

                String url = server.endsWith("/") ? (server + topic) : (server + "/" + topic);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(5))
                        .header("Title", "CarZone Security Alert: " + viPham.loai().name())
                        .header("Priority", daKhoaPhien ? "urgent" : "high")
                        .header("Tags", "warning,skull,lock")
                        .POST(HttpRequest.BodyPublishers.ofString(noiDung))
                        .build();

                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                log.info("Đã gửi cảnh báo an ninh tới ntfy.sh (HTTP {})", response.statusCode());
            } catch (Throwable t) {
                log.warn("Không thể gửi cảnh báo qua ntfy.sh: {}", t.getMessage());
            }
        }, "carzone-security-alert-sender");

        alertThread.setDaemon(true);
        alertThread.start();
    }
}

