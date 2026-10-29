package ui;

import connectDB.DbUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.URL;

/**
 * Màn hình Khởi động CarZone (Splash Screen) – Chuẩn mực tuyệt đối theo frame "Carzone · Khởi động" (Node 95:681) trong Figma.
 *
 * Cấu trúc thiết kế Figma:
 * - Kích thước: 1280×840px, nền #F8FAFC, viền ngoài #E2E8F0, bo góc 20px
 * - Khối Startup Identity (768px, layout vertical, centered, gap 32px):
 *   + Brand Block:
 *     - Logo: 200×58.49px căn giữa
 *     - Brand Name: "CARZONE" (Arimo Bold 32px, #0F172A)
 *     - App Purpose: "Hệ thống quản lý gara ô tô" (Arimo Regular 16px, #64748B)
 *   + Loading State:
 *     - Progress Track: 280×4px, nền #E2E8F0, bo tròn pill (cornerRadius 999)
 *     - Progress Bar: 4px, màu #2563EB, bo tròn pill, tăng dần 0% - 100%
 *     - Loading Message: "Đang khởi động hệ thống…" (Arimo Regular 13px, #334155)
 *     - Loading Instruction: "Vui lòng chờ trong giây lát" (Arimo Regular 12px, #94A3B8)
 * - Khối Startup Footer (72px, layout vertical, centered, gap 6px):
 *   + Version: "CarZone 1.0" (Arimo Bold 12px, #64748B)
 *   + Copyright: "© 2026 Carzone. Bảo lưu mọi quyền." (Arimo Regular 12px, #94A3B8)
 */
public class KhoiDongUI extends JFrame {

    // ── Bảng màu chuẩn tuyệt đối từ Figma ────────────────────────────────────
    private static final Color C_BG           = new Color(0xF8FAFC); // Nền app #F8FAFC
    private static final Color C_TEXT_NAME    = new Color(0x0F172A); // Chữ CARZONE #0F172A
    private static final Color C_TEXT_PURPOSE = new Color(0x64748B); // Hệ thống quản lý #64748B
    private static final Color C_TEXT_MSG     = new Color(0x334155); // Đang khởi động #334155
    private static final Color C_TEXT_WAIT    = new Color(0x94A3B8); // Vui lòng chờ #94A3B8
    private static final Color C_TRACK        = new Color(0xE2E8F0); // Track nền progress #E2E8F0
    private static final Color C_PRIMARY      = new Color(0x2563EB); // Thanh tiến trình #2563EB
    private static final Color C_FOOTER_VER   = new Color(0x64748B); // Version #64748B
    private static final Color C_FOOTER_COPY  = new Color(0x94A3B8); // Copyright #94A3B8

    // ── Font chữ: Ưu tiên Arimo, fallback SansSerif ───────────────────────────
    private static final String FONT_FAMILY = detectFont();

    private static String detectFont() {
        for (String f : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            if (f.equalsIgnoreCase("Arimo")) return "Arimo";
        }
        return "SansSerif";
    }

    private static Font f(int style, int size) {
        return new Font(FONT_FAMILY, style, size);
    }

    // ── Thành phần giao diện ──────────────────────────────────────────────────
    private RoundedProgressBar progressBar;
    private JLabel lblLoadingMsg;
    private JLabel lblLoadingInstruction;
    private Timer progressTimer;

    // ── Callback khi tải xong ─────────────────────────────────────────────────
    public interface KhoiDongListener {
        void onHoanTat();
    }

    private KhoiDongListener listener;

    public KhoiDongUI() {
        setTitle("CarZone – Khởi động hệ thống");
        setSize(1280, 840);
        setMinimumSize(new Dimension(1024, 720));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(C_BG);

        // Icon ứng dụng
        ImageIcon appIcon = loadLogoImage(32, 32);
        if (appIcon != null) {
            setIconImage(appIcon.getImage());
        }

        initComponents();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                batDauTienTrinh();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                if (progressTimer != null && progressTimer.isRunning()) {
                    progressTimer.stop();
                }
            }
        });
    }

    public void setKhoiDongListener(KhoiDongListener l) {
        this.listener = l;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // KHUNG CHÍNH (BorderLayout: Center là Identity, South là Footer)
    // ─────────────────────────────────────────────────────────────────────────
    private void initComponents() {
        setLayout(new BorderLayout(0, 0));

        add(buildIdentityPanel(), BorderLayout.CENTER);
        add(buildFooterPanel(), BorderLayout.SOUTH);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. STARTUP IDENTITY (Trung tâm)
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildIdentityPanel() {
        // Dùng GridBagLayout để căn giữa toàn bộ khối vào tâm cửa sổ
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);

        JPanel centerBox = new JPanel();
        centerBox.setLayout(new BoxLayout(centerBox, BoxLayout.Y_AXIS));
        centerBox.setOpaque(false);

        // ── Brand Block (Logo + "CARZONE" + "Hệ thống quản lý gara ô tô") ─────
        JPanel pnlBrand = new JPanel();
        pnlBrand.setLayout(new BoxLayout(pnlBrand, BoxLayout.Y_AXIS));
        pnlBrand.setOpaque(false);
        pnlBrand.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Logo 200×58.49px
        JComponent logoComp = createLogoComponent(200, 58);
        logoComp.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlBrand.add(logoComp);

        pnlBrand.add(Box.createVerticalStrut(20));

        // Brand name: "CARZONE" (Bold 32px)
        JLabel lblBrandName = new JLabel("CARZONE");
        lblBrandName.setFont(f(Font.BOLD, 32));
        lblBrandName.setForeground(C_TEXT_NAME);
        lblBrandName.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlBrand.add(lblBrandName);

        pnlBrand.add(Box.createVerticalStrut(8));

        // App purpose: "Hệ thống quản lý gara ô tô" (Regular 16px)
        JLabel lblAppPurpose = new JLabel("Hệ thống quản lý gara ô tô");
        lblAppPurpose.setFont(f(Font.PLAIN, 16));
        lblAppPurpose.setForeground(C_TEXT_PURPOSE);
        lblAppPurpose.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlBrand.add(lblAppPurpose);

        centerBox.add(pnlBrand);

        // Gap giữa Brand và Loading: 32px
        centerBox.add(Box.createVerticalStrut(32));

        // ── Loading State (Progress Track + Message + Instruction) ────────────
        JPanel pnlLoading = new JPanel();
        pnlLoading.setLayout(new BoxLayout(pnlLoading, BoxLayout.Y_AXIS));
        pnlLoading.setOpaque(false);
        pnlLoading.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Progress bar 280×4px pill
        progressBar = new RoundedProgressBar();
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlLoading.add(progressBar);

        pnlLoading.add(Box.createVerticalStrut(14));

        // Loading message: "Đang khởi động hệ thống…" (Regular 13px)
        lblLoadingMsg = new JLabel("Đang khởi động hệ thống…");
        lblLoadingMsg.setFont(f(Font.PLAIN, 13));
        lblLoadingMsg.setForeground(C_TEXT_MSG);
        lblLoadingMsg.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlLoading.add(lblLoadingMsg);

        pnlLoading.add(Box.createVerticalStrut(14));

        // Loading instruction: "Vui lòng chờ trong giây lát" (Regular 12px)
        lblLoadingInstruction = new JLabel("Vui lòng chờ trong giây lát");
        lblLoadingInstruction.setFont(f(Font.PLAIN, 12));
        lblLoadingInstruction.setForeground(C_TEXT_WAIT);
        lblLoadingInstruction.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlLoading.add(lblLoadingInstruction);

        centerBox.add(pnlLoading);

        wrapper.add(centerBox);
        return wrapper;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. STARTUP FOOTER (72px đáy)
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildFooterPanel() {
        JPanel footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setOpaque(false);
        footer.setPreferredSize(new Dimension(1280, 72));
        footer.setBorder(new EmptyBorder(8, 0, 18, 0));

        // Version: "CarZone 1.0" (Bold 12px)
        JLabel lblVersion = new JLabel("CarZone 1.0");
        lblVersion.setFont(f(Font.BOLD, 12));
        lblVersion.setForeground(C_FOOTER_VER);
        lblVersion.setAlignmentX(Component.CENTER_ALIGNMENT);
        footer.add(lblVersion);

        footer.add(Box.createVerticalStrut(6));

        // Copyright: "© 2026 Carzone. Bảo lưu mọi quyền." (Regular 12px)
        JLabel lblCopyright = new JLabel("© 2026 Carzone. Bảo lưu mọi quyền.");
        lblCopyright.setFont(f(Font.PLAIN, 12));
        lblCopyright.setForeground(C_FOOTER_COPY);
        lblCopyright.setAlignmentX(Component.CENTER_ALIGNMENT);
        footer.add(lblCopyright);

        return footer;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CUSTOM COMPONENT: ROUNDED PROGRESS BAR (280×4px pill)
    // ─────────────────────────────────────────────────────────────────────────
    private static class RoundedProgressBar extends JComponent {
        private int progress = 0; // 0 -> 100

        public RoundedProgressBar() {
            setPreferredSize(new Dimension(280, 4));
            setMaximumSize(new Dimension(280, 4));
            setMinimumSize(new Dimension(280, 4));
            setOpaque(false);
        }

        public void setProgress(int val) {
            this.progress = Math.max(0, Math.min(100, val));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Vẽ track nền #E2E8F0
            g2.setColor(C_TRACK);
            g2.fillRoundRect(0, 0, w, h, h, h);

            // Vẽ inner bar #2563EB
            if (progress > 0) {
                int fillW = Math.max(h, (int) Math.round((w * (double) progress) / 100.0));
                g2.setColor(C_PRIMARY);
                g2.fillRoundRect(0, 0, fillW, h, h, h);
            }

            g2.dispose();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOGO LOADING & FALLBACK
    // ─────────────────────────────────────────────────────────────────────────    // ─────────────────────────────────────────────────────────────────────────
    // LOGO COMPONENT (Vẽ ảnh gốc độ nét cao với Bicubic Antialiasing & giữ đúng tỷ lệ)
    // ─────────────────────────────────────────────────────────────────────────

    private JComponent createLogoComponent(int targetW, int targetH) {
        Image rawImg = loadRawLogoImage();
        if (rawImg != null) {
            JPanel panel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
                    g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

                    int cw = getWidth();
                    int ch = getHeight();
                    double iw = rawImg.getWidth(null);
                    double ih = rawImg.getHeight(null);
                    if (iw > 0 && ih > 0) {
                        double imgAspect = iw / ih;
                        double compAspect = (double) cw / ch;
                        int drawW, drawH;
                        if (compAspect > imgAspect) {
                            drawH = ch;
                            drawW = (int) Math.round(drawH * imgAspect);
                        } else {
                            drawW = cw;
                            drawH = (int) Math.round(drawW / imgAspect);
                        }
                        int x = (cw - drawW) / 2;
                        int y = (ch - drawH) / 2;
                        g2.drawImage(rawImg, x, y, drawW, drawH, null);
                    }
                    g2.dispose();
                }
            };
            panel.setOpaque(false);
            panel.setPreferredSize(new Dimension(targetW, targetH));
            panel.setMaximumSize(new Dimension(targetW, targetH));
            return panel;
        }

        JPanel fallback = new JPanel();
        fallback.setOpaque(false);
        fallback.setPreferredSize(new Dimension(targetW, targetH));
        return fallback;
    }

    private static Image rawLogoImage = null;

    private static Image loadRawLogoImage() {
        if (rawLogoImage != null) return rawLogoImage;
        try {
            java.net.URL url = KhoiDongUI.class.getResource("/resources/images/carzone_logo.png");
            if (url == null) {
                url = KhoiDongUI.class.getResource("carzone_logo.png");
            }
            if (url != null) {
                rawLogoImage = javax.imageio.ImageIO.read(url);
            } else {
                java.io.File file = new java.io.File("src/resources/images/carzone_logo.png");
                if (!file.exists()) file = new java.io.File("src/ui/carzone_logo.png");
                if (file.exists()) {
                    rawLogoImage = javax.imageio.ImageIO.read(file);
                }
            }
        } catch (Exception ignored) {
        }
        return rawLogoImage;
    }

    private static ImageIcon loadLogoImage(int targetW, int targetH) {
        try {
            Image img = loadRawLogoImage();
            if (img != null) {
                Image scaled = img.getScaledInstance(targetW, targetH, Image.SCALE_SMOOTH);
                return new ImageIcon(scaled);
            }
        } catch (Exception ignored) {
        }
        return null;
    }
    // ─────────────────────────────────────────────────────────────────────────
    // TIẾN TRÌNH KHỞI ĐỘNG (Pre-warm DB + Animation mượt mà)
    // ─────────────────────────────────────────────────────────────────────────
    private void batDauTienTrinh() {
        // Khởi động kết nối ngầm (pre-warm kết nối CSDL)
        new Thread(() -> {
            try {
                DbUtil.isDatabaseOnline();
            } catch (Exception ignored) {
                // Tiếp tục khởi động bình thường ngay cả khi DB chưa sẵn sàng
            }
        }, "CarZone-PreWarmDB").start();

        // Timer mô phỏng tiến trình nạp mượt mà (khoảng 1.8s)
        final int[] percent = {0};
        progressTimer = new Timer(25, e -> {
            percent[0] += 2;
            if (percent[0] > 100) percent[0] = 100;

            progressBar.setProgress(percent[0]);

            // Cập nhật thông điệp theo từng giai đoạn
            if (percent[0] < 30) {
                lblLoadingMsg.setText("Đang khởi động hệ thống…");
            } else if (percent[0] < 65) {
                lblLoadingMsg.setText("Đang tải cấu hình và giao diện…");
            } else if (percent[0] < 90) {
                lblLoadingMsg.setText("Đang khởi tạo kết nối cơ sở dữ liệu…");
            } else {
                lblLoadingMsg.setText("Sẵn sàng khởi chạy!");
            }

            if (percent[0] >= 100) {
                progressTimer.stop();

                // Dừng lại 250ms cho người dùng cảm nhận hoàn tất rồi chuyển màn hình
                Timer delayTimer = new Timer(250, ev -> {
                    ((Timer) ev.getSource()).stop();
                    dispose();
                    if (listener != null) {
                        listener.onHoanTat();
                    }
                });
                delayTimer.setRepeats(false);
                delayTimer.start();
            }
        });
        progressTimer.start();
    }
}
