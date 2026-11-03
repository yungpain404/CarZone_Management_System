package backup;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog modal hiển thị khi nhân viên bấm đóng ứng dụng.
 * Không thể đóng từ nút X – bắt buộc chờ backup hoàn tất.
 *
 * Dùng qua:
 *   ShutdownDialog dlg = new ShutdownDialog(ownerFrame);
 *   dlg.setVisible(true);  // phải gọi trên EDT, sẽ block EDT (modal)
 */
public class ShutdownDialog extends JDialog {

    private final JLabel lblTrangThai;
    private final JProgressBar progressBar;
    private final JLabel lblCanhBao;
    private JPanel pnlLoi;      // panel lỗi, ẩn khi không lỗi
    private JButton btnThuLai;
    private JButton btnThoatKhongSaoLuu;

    public ShutdownDialog(Frame owner) {
        super(owner, "Đang lưu dữ liệu ca làm việc", true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setUndecorated(false);
        setResizable(false);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));
        content.setBackground(Color.WHITE);

        // Tiêu đề
        JLabel lblTitle = new JLabel("🔄 Đang lưu dữ liệu ca làm việc");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Trạng thái
        lblTrangThai = new JLabel("Đang chuẩn bị sao lưu…");
        lblTrangThai.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblTrangThai.setForeground(new Color(0x374151));
        lblTrangThai.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("0%");
        progressBar.setPreferredSize(new Dimension(380, 20));
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Cảnh báo
        lblCanhBao = new JLabel("⚠ Vui lòng không tắt máy hoặc rút nguồn.");
        lblCanhBao.setFont(new Font("SansSerif", Font.ITALIC, 12));
        lblCanhBao.setForeground(new Color(0xB45309));
        lblCanhBao.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Panel lỗi (ẩn mặc định)
        pnlLoi = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        pnlLoi.setBackground(Color.WHITE);
        pnlLoi.setVisible(false);

        btnThuLai = new JButton("Thử lại");
        btnThoatKhongSaoLuu = new JButton("Thoát không sao lưu");
        btnThoatKhongSaoLuu.setForeground(new Color(0xDC2626));
        pnlLoi.add(btnThuLai);
        pnlLoi.add(btnThoatKhongSaoLuu);

        content.add(lblTitle);
        content.add(Box.createVerticalStrut(14));
        content.add(lblTrangThai);
        content.add(Box.createVerticalStrut(12));
        content.add(progressBar);
        content.add(Box.createVerticalStrut(10));
        content.add(lblCanhBao);
        content.add(Box.createVerticalStrut(12));
        content.add(pnlLoi);

        setContentPane(content);
        pack();
        setMinimumSize(new Dimension(440, getHeight()));
        setLocationRelativeTo(owner);
    }

    /**
     * Cập nhật tiến độ (0–100) – phải gọi trên EDT.
     */
    public void setProgress(int percent) {
        progressBar.setValue(percent);
        progressBar.setString(percent + "%");
    }

    /**
     * Cập nhật dòng trạng thái – phải gọi trên EDT.
     */
    public void setStatus(String text) {
        lblTrangThai.setText(text);
    }

    /**
     * Hiện thông báo lỗi kèm hai nút lựa chọn.
     * @param msg         Mô tả lỗi
     * @param onRetry     Callback khi nhấn "Thử lại"
     * @param onForceExit Callback khi nhấn "Thoát không sao lưu"
     */
    public void showError(String msg, Runnable onRetry, Runnable onForceExit) {
        SwingUtilities.invokeLater(() -> {
            lblTrangThai.setText("❌ " + msg);
            lblTrangThai.setForeground(new Color(0xDC2626));
            lblCanhBao.setVisible(false);
            pnlLoi.setVisible(true);

            // Xoá listeners cũ trước khi gán mới (tránh chồng callback)
            for (var l : btnThuLai.getActionListeners()) {
                btnThuLai.removeActionListener(l);
            }
            for (var l : btnThoatKhongSaoLuu.getActionListeners()) {
                btnThoatKhongSaoLuu.removeActionListener(l);
            }

            btnThuLai.addActionListener(e -> {
                // Reset trạng thái trước khi thử lại
                lblTrangThai.setText("Đang thử lại…");
                lblTrangThai.setForeground(new Color(0x374151));
                lblCanhBao.setVisible(true);
                pnlLoi.setVisible(false);
                progressBar.setValue(0);
                progressBar.setString("0%");
                pack();
                onRetry.run();
            });
            btnThoatKhongSaoLuu.addActionListener(e -> onForceExit.run());

            pack();
            setLocationRelativeTo(getOwner());
        });
    }

    /**
     * Đóng dialog – phải gọi trên EDT.
     */
    public void close() {
        SwingUtilities.invokeLater(this::dispose);
    }
}

