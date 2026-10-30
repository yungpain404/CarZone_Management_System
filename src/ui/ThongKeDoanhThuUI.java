package ui;

import entity.NhanVien;
import enums.VaiTro;
import controller.ThongKeDoanhThuController;
import controller.ThongKeDoanhThuController.BaoCao;
import controller.ThongKeDoanhThuController.Dong;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/** Báo cáo doanh thu, xe/phiếu và dịch vụ/linh kiện dành cho Chủ cửa hàng. */
public class ThongKeDoanhThuUI extends JPanel {
    private static final Color BG = new Color(0xF8FAFC), WHITE = Color.WHITE, BORDER = new Color(0xE2E8F0);
    private static final Color INPUT = new Color(0xCBD5E1), PRIMARY = new Color(0x2563EB), PRIMARY_BG = new Color(0xEFF6FF);
    private static final Color TEXT = new Color(0x0F172A), BODY = new Color(0x334155), SUBTLE = new Color(0x64748B), RED = new Color(0xD12929);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final NhanVien nhanVien;
    private final ThongKeDoanhThuController controller = new ThongKeDoanhThuController();
    private final JComboBox<String> cboLoai = new JComboBox<>(new String[]{ThongKeDoanhThuController.DOANH_THU, ThongKeDoanhThuController.XE_PHIEU, ThongKeDoanhThuController.DICH_VU_LINH_KIEN});
    private final JComboBox<String> cboKy = new JComboBox<>(new String[]{"Hôm nay", "Tuần này", "Tháng này", "Khoảng ngày"});
    private final JTextField txtTuNgay = new JTextField(10), txtDenNgay = new JTextField(10);
    private final JComboBox<String> cboShortcut = new JComboBox<>(new String[]{"Lối tắt quản lý", "Quản lý dịch vụ", "Linh kiện và tồn kho", "Tra cứu"});
    private final JButton btnXem = button("Xem thống kê", PRIMARY, Color.WHITE, null);
    private final JButton btnXuat = button("Xuất báo cáo", WHITE, PRIMARY, INPUT);
    private final JLabel lblStatus = label("Sẵn sàng", 12, SUBTLE, Font.PLAIN);
    private final JLabel lblError = label("", 12, RED, Font.PLAIN);
    private final JPanel reportContent = new JPanel(new BorderLayout(0, 12));
    private final JPanel tabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private String loai = ThongKeDoanhThuController.DOANH_THU;
    private BaoCao baoCao;
    private boolean loading;

    public ThongKeDoanhThuUI(NhanVien nhanVien) {
        this.nhanVien = nhanVien;
        setLayout(new BorderLayout()); setBackground(BG);
        setBorder(new EmptyBorder(24, 24, 20, 24));
        if (nhanVien == null || nhanVien.getVaiTro() != VaiTro.CHU_CUA_HANG) {
            add(new JLabel("Chỉ Chủ cửa hàng được xem báo cáo này."), BorderLayout.CENTER); return;
        }
        add(buildContent(), BorderLayout.CENTER);
        bindActions();
        chonKy("Hôm nay");
        SwingUtilities.invokeLater(() -> { txtTuNgay.requestFocusInWindow(); taiBaoCao(); });
    }

    private JComponent buildContent() {
        JPanel root = new JPanel(); root.setOpaque(false); root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.add(buildHeading()); root.add(Box.createVerticalStrut(18));
        root.add(buildFilters()); root.add(Box.createVerticalStrut(14));
        root.add(buildTabs()); root.add(Box.createVerticalStrut(14));
        reportContent.setOpaque(false); reportContent.setAlignmentX(Component.CENTER_ALIGNMENT);
        reportContent.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        root.add(reportContent); root.add(Box.createVerticalStrut(8));
        JPanel status = new JPanel(new BorderLayout(8, 0)); status.setOpaque(false); status.setAlignmentX(Component.CENTER_ALIGNMENT);
        status.add(lblError, BorderLayout.WEST); status.add(lblStatus, BorderLayout.EAST); root.add(status);
        JScrollPane scroll = new JScrollPane(root); scroll.setBorder(null); scroll.getViewport().setOpaque(false); scroll.setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER); scroll.getVerticalScrollBar().setUnitIncrement(18);
        return scroll;
    }

    private JComponent buildHeading() {
        JPanel p = new JPanel(new BorderLayout()); p.setOpaque(false); p.setAlignmentX(Component.CENTER_ALIGNMENT);
        JPanel left = new JPanel(new BorderLayout(12, 0)); left.setOpaque(false);
        JLabel icon = new JLabel(); icon.setPreferredSize(new Dimension(40, 40));
        try (InputStream in = getClass().getResourceAsStream("/revenue_illustration.png")) {
            if (in != null) { Image im = ImageIO.read(in).getScaledInstance(40, 40, Image.SCALE_SMOOTH); icon.setIcon(new ImageIcon(im)); }
        } catch (IOException ignored) { }
        JPanel texts = new JPanel(); texts.setOpaque(false); texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        JLabel crumb = label("Chủ Gara  /  Thống kê doanh thu", 13, PRIMARY, Font.BOLD);
        JLabel title = label("Thống kê doanh thu", 24, TEXT, Font.BOLD);
        texts.add(crumb); texts.add(Box.createVerticalStrut(4)); texts.add(title);
        left.add(icon, BorderLayout.WEST); left.add(texts, BorderLayout.CENTER); p.add(left, BorderLayout.WEST);
        return p;
    }

    private JComponent buildFilters() {
        JPanel card = new JPanel(new GridBagLayout()); card.setBackground(WHITE); card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(12, 14, 12, 14)));
        card.setAlignmentX(Component.CENTER_ALIGNMENT);
        GridBagConstraints g = new GridBagConstraints(); g.gridy=0; g.insets=new Insets(0, 0, 0, 10); g.anchor=GridBagConstraints.WEST;
        addField(card, g, "Loại báo cáo", cboLoai, 0, 0.20);
        addField(card, g, "Kỳ thống kê", cboKy, 1, 0.16);
        addField(card, g, "Từ ngày", txtTuNgay, 2, 0.14);
        addField(card, g, "Đến ngày", txtDenNgay, 3, 0.14);
        addField(card, g, "Lối tắt quản lý", cboShortcut, 4, 0.17);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 14)); actions.setOpaque(false); actions.add(btnXem); actions.add(btnXuat);
        g.gridx=5; g.weightx=0; g.fill=GridBagConstraints.NONE; g.insets=new Insets(0,0,0,0); card.add(actions,g);
        cboLoai.addActionListener(e -> { String selected=(String)cboLoai.getSelectedItem(); if(selected!=null&&!selected.equals(loai)){loai=selected;capNhatTab();taiBaoCao();} });
        return card;
    }

    private void addField(JPanel card, GridBagConstraints base, String caption, JComponent field, int x, double weight) {
        JPanel p = new JPanel(new BorderLayout(0, 6)); p.setOpaque(false); p.setMinimumSize(new Dimension(110, 66));
        p.add(label(caption, 13, BODY, Font.PLAIN), BorderLayout.NORTH);
        field.setFont(new Font("SansSerif", Font.PLAIN, 13)); field.setPreferredSize(new Dimension(130, 40));
        field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(INPUT), new EmptyBorder(0, 10, 0, 10)));
        p.add(field, BorderLayout.CENTER); GridBagConstraints g=(GridBagConstraints)base.clone(); g.gridx=x; g.weightx=weight; g.fill=GridBagConstraints.HORIZONTAL; card.add(p,g);
    }

    private JComponent buildTabs() {
        tabBar.setOpaque(false); tabBar.setAlignmentX(Component.CENTER_ALIGNMENT); capNhatTab(); return tabBar;
    }

    private void capNhatTab() {
        tabBar.removeAll();
        addTab(ThongKeDoanhThuController.DOANH_THU); addTab(ThongKeDoanhThuController.XE_PHIEU); addTab(ThongKeDoanhThuController.DICH_VU_LINH_KIEN);
        tabBar.revalidate(); tabBar.repaint();
    }
    private void addTab(String title) { JButton b=button(title, title.equals(loai)?PRIMARY:WHITE, title.equals(loai)?Color.WHITE:SUBTLE, title.equals(loai)?null:BORDER);
        b.addActionListener(e->{loai=title;cboLoai.setSelectedItem(title);capNhatTab();taiBaoCao();}); tabBar.add(b); }

    private void bindActions() {
        btnXem.addActionListener(e -> taiBaoCao()); btnXuat.addActionListener(e -> exportCsv());
        cboKy.addActionListener(e -> { if (cboKy.getSelectedItem()!=null) chonKy(String.valueOf(cboKy.getSelectedItem())); });
        DocumentListener dl=new DocumentListener(){public void insertUpdate(DocumentEvent e){clearError();} public void removeUpdate(DocumentEvent e){clearError();} public void changedUpdate(DocumentEvent e){clearError();}};
        txtTuNgay.getDocument().addDocumentListener(dl); txtDenNgay.getDocument().addDocumentListener(dl);
        txtTuNgay.addKeyListener(new KeyAdapter(){@Override public void keyPressed(KeyEvent e){if(e.getKeyCode()==KeyEvent.VK_ENTER)txtDenNgay.requestFocusInWindow();}});
        txtDenNgay.addKeyListener(new KeyAdapter(){@Override public void keyPressed(KeyEvent e){if(e.getKeyCode()==KeyEvent.VK_ENTER)taiBaoCao();}});
    }

    private void chonKy(String ky) {
        LocalDate today=LocalDate.now(), from=today;
        switch(ky){case "Tuần này" -> from=today.with(DayOfWeek.MONDAY); case "Tháng này" -> from=today.withDayOfMonth(1); case "Khoảng ngày" -> { if(txtTuNgay.getText().isBlank()) from=today; else return; } default -> {}}
        txtTuNgay.setText(from.format(DATE)); txtDenNgay.setText(today.format(DATE));
    }

    private void taiBaoCao() {
        if(loading)return;
        LocalDate from,to;
        try { from=LocalDate.parse(txtTuNgay.getText().trim(), DATE); to=LocalDate.parse(txtDenNgay.getText().trim(), DATE);
            if(to.isBefore(from)) throw new IllegalArgumentException("Ngày kết thúc phải bằng hoặc sau ngày bắt đầu.");
        } catch(DateTimeParseException|IllegalArgumentException ex){showError(ex.getMessage()==null?"Ngày phải có định dạng dd/MM/yyyy.":ex.getMessage());return;}
        setLoading(true); clearError();
        new SwingWorker<BaoCao,Void>(){
            @Override protected BaoCao doInBackground(){return controller.layBaoCao(nhanVien.getVaiTro(),loai,from,to);}
            @Override protected void done(){try{baoCao=get(); renderReport(); lblStatus.setText("Đã cập nhật · "+DATE.format(from)+" – "+DATE.format(to));}
                catch(Exception ex){baoCao=null; renderEmpty(); Throwable cause=ex.getCause()==null?ex:ex.getCause(); showError("Không thể tải thống kê: "+(cause.getMessage()==null?"hãy kiểm tra kết nối cơ sở dữ liệu.":cause.getMessage()));}
                finally{setLoading(false);}}
        }.execute();
    }

    private void renderReport() {
        reportContent.removeAll(); JPanel body=new JPanel(); body.setOpaque(false); body.setLayout(new BoxLayout(body,BoxLayout.Y_AXIS));
        body.add(buildSummary()); body.add(Box.createVerticalStrut(14));
        JPanel charts=new JPanel(new GridLayout(1,2,16,0)); charts.setOpaque(false); charts.setAlignmentX(Component.CENTER_ALIGNMENT);
        charts.add(buildChartCard()); charts.add(loai.equals(ThongKeDoanhThuController.DOANH_THU)?buildPaymentCard():buildInsightCard());
        body.add(charts); body.add(Box.createVerticalStrut(14));
        body.add(buildTableCard());
        if(loai.equals(ThongKeDoanhThuController.DOANH_THU)){body.add(Box.createVerticalStrut(14));body.add(buildInsightCard());}
        reportContent.add(body,BorderLayout.CENTER); reportContent.revalidate(); reportContent.repaint();
    }

    private JComponent buildSummary() {
        JPanel card=cardPanel(); card.setLayout(new BorderLayout(0,12)); card.add(label("Tổng hợp trong kỳ",16,TEXT,Font.BOLD),BorderLayout.NORTH);
        JPanel row=new JPanel(new GridLayout(1,3,12,0)); row.setOpaque(false);
        if(loai.equals(ThongKeDoanhThuController.DOANH_THU)){
            row.add(metric("Tổng thu",money(baoCao.tongThu),BODY)); row.add(metric("Hoàn tiền",money(baoCao.hoanTien),RED)); row.add(metric("Sau hoàn",money(baoCao.doanhThuThuan),PRIMARY));
        } else if(loai.equals(ThongKeDoanhThuController.XE_PHIEU)){
            row.add(metric("Xe tiếp nhận",String.valueOf(baoCao.xe),BODY)); row.add(metric("Phiếu sửa chữa",String.valueOf(baoCao.soLuong),BODY)); row.add(metric("Đã hoàn tất",String.valueOf(baoCao.hoanTat),PRIMARY));
        } else {
            row.add(metric("Mặt hàng có sử dụng",String.valueOf(baoCao.soLuong),BODY)); row.add(metric("Dịch vụ",String.valueOf(baoCao.rows.stream().filter(d->d.cells[0].equals("Dịch vụ")).count()),PRIMARY)); row.add(metric("Linh kiện",String.valueOf(baoCao.rows.stream().filter(d->d.cells[0].equals("Linh kiện")).count()),PRIMARY));
        }
        card.add(row,BorderLayout.CENTER); return card;
    }

    private JComponent metric(String title,String value,Color color){JPanel p=new JPanel();p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));p.setBackground(BG);p.setBorder(new EmptyBorder(10,12,10,12));
        p.add(label(title,12,SUBTLE,Font.PLAIN));p.add(Box.createVerticalStrut(4));p.add(label(value,18,color,Font.BOLD));return p;}

    private JComponent buildChartCard(){JPanel p=cardPanel();p.setLayout(new BorderLayout(0,10));String title=loai.equals(ThongKeDoanhThuController.DOANH_THU)?"Doanh thu sau hoàn (triệu VNĐ)":loai.equals(ThongKeDoanhThuController.XE_PHIEU)?"Phiếu theo trạng thái":"Mặt hàng nổi bật theo doanh thu";
        p.add(label(title,16,TEXT,Font.BOLD),BorderLayout.NORTH);p.add(new BarChart(baoCao.chartLabels,baoCao.chartValues),BorderLayout.CENTER);return p;}
    private JComponent buildPaymentCard(){JPanel p=cardPanel();p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));p.add(label("Cơ cấu thanh toán",16,TEXT,Font.BOLD));p.add(Box.createVerticalStrut(12));p.add(new PaymentBar(baoCao.tienMat,baoCao.chuyenKhoan));p.add(Box.createVerticalStrut(14));
        p.add(label("Chuyển khoản · "+money(baoCao.chuyenKhoan)+" VNĐ",13,BODY,Font.PLAIN));p.add(Box.createVerticalStrut(8));p.add(label("Tiền mặt · "+money(baoCao.tienMat)+" VNĐ",13,BODY,Font.PLAIN));p.add(Box.createVerticalStrut(14));p.add(label("Hoàn tiền",12,SUBTLE,Font.PLAIN));p.add(label(money(baoCao.hoanTien)+" VNĐ",16,RED,Font.BOLD));return p;}
    private JComponent buildInsightCard(){JPanel p=cardPanel();p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));p.add(label("Nhận xét",16,TEXT,Font.BOLD));p.add(Box.createVerticalStrut(10));
        for(String insight:baoCao.insights){JLabel line=label("•  "+insight,13,BODY,Font.PLAIN);line.setBorder(new EmptyBorder(2,0,2,0));p.add(line);}return p;}

    private JComponent buildTableCard(){JPanel p=cardPanel();p.setLayout(new BorderLayout(0,0));String[] columns=loai.equals(ThongKeDoanhThuController.DOANH_THU)?new String[]{"Khung giờ","Số HĐ","Tiền mặt","Chuyển khoản","Tổng thu","Hoàn tiền","Sau hoàn"}:
            loai.equals(ThongKeDoanhThuController.XE_PHIEU)?new String[]{"Trạng thái phiếu","Số phiếu","Số xe"}:new String[]{"Loại","Mã","Tên dịch vụ / linh kiện","Số phiếu","Số lượng","Thành tiền (VNĐ)"};
        DefaultTableModel m=new DefaultTableModel(columns,0){@Override public boolean isCellEditable(int row,int column){return false;}};
        for(Dong d:baoCao.rows)m.addRow(d.cells);
        JTable table=new JTable(m);table.setRowHeight(42);table.setFont(new Font("SansSerif",Font.PLAIN,12));table.setForeground(BODY);table.setGridColor(BORDER);table.setShowVerticalLines(false);
        table.getTableHeader().setFont(new Font("SansSerif",Font.BOLD,12));table.getTableHeader().setBackground(BG);table.getTableHeader().setForeground(BODY);table.setFillsViewportHeight(true);
        JScrollPane scroll=new JScrollPane(table);scroll.setBorder(BorderFactory.createLineBorder(BORDER));scroll.getViewport().setBackground(WHITE);p.add(scroll,BorderLayout.CENTER);
        JPanel footer=new JPanel(new BorderLayout());footer.setBackground(BG);footer.setBorder(new EmptyBorder(9,12,9,12));footer.add(label("Đơn vị: VNĐ · "+txtTuNgay.getText()+" – "+txtDenNgay.getText(),12,SUBTLE,Font.PLAIN),BorderLayout.WEST);
        if(loai.equals(ThongKeDoanhThuController.DOANH_THU))footer.add(label("Doanh thu sau hoàn không phải lợi nhuận",12,SUBTLE,Font.PLAIN),BorderLayout.EAST);p.add(footer,BorderLayout.SOUTH);return p;}

    private void exportCsv(){if(baoCao==null){showError("Hãy tải báo cáo trước khi xuất.");return;}JFileChooser chooser=new JFileChooser();chooser.setSelectedFile(new File("CarZone_ThongKe_"+LocalDate.now()+".csv"));
        if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;try(BufferedWriter w=Files.newBufferedWriter(chooser.getSelectedFile().toPath(),StandardCharsets.UTF_8)){
            w.write('\ufeff');String[] headers=loai.equals(ThongKeDoanhThuController.DOANH_THU)?new String[]{"Khung giờ","Số HĐ","Tiền mặt","Chuyển khoản","Tổng thu","Hoàn tiền","Sau hoàn"}:loai.equals(ThongKeDoanhThuController.XE_PHIEU)?new String[]{"Trạng thái phiếu","Số phiếu","Số xe"}:new String[]{"Loại","Mã","Tên","Số phiếu","Số lượng","Thành tiền"};
            writeCsvRow(w,headers);for(Dong d:baoCao.rows)writeCsvRow(w,d.cells);lblStatus.setText("Đã xuất báo cáo · "+chooser.getSelectedFile().getName());
        }catch(IOException ex){showError("Không thể lưu báo cáo: "+ex.getMessage());}}
    private static void writeCsvRow(Writer w,String[] cells)throws IOException{for(int i=0;i<cells.length;i++){if(i>0)w.write(',');w.write('"');w.write(cells[i].replace("\"","\"\""));w.write('"');}w.write("\r\n");}

    private void renderEmpty(){reportContent.removeAll();reportContent.revalidate();reportContent.repaint();}
    private void setLoading(boolean value){loading=value;btnXem.setEnabled(!value);btnXuat.setEnabled(!value&&baoCao!=null);cboKy.setEnabled(!value);cboLoai.setEnabled(!value);txtTuNgay.setEnabled(!value);txtDenNgay.setEnabled(!value);for(Component c:tabBar.getComponents())c.setEnabled(!value);lblStatus.setText(value?"Đang tải thống kê…":"Sẵn sàng");}
    private void showError(String message){lblError.setText(message==null?"Có lỗi khi tải dữ liệu.":message);}
    private void clearError(){lblError.setText("");}
    private JPanel cardPanel(){JPanel p=new JPanel();p.setBackground(WHITE);p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),new EmptyBorder(16,16,16,16)));p.setAlignmentX(Component.CENTER_ALIGNMENT);return p;}
    private static JLabel label(String text,int size,Color color,int style){JLabel l=new JLabel(text);l.setFont(new Font("SansSerif",style,size));l.setForeground(color);return l;}
    private static String money(double n){return new java.text.DecimalFormat("#,##0",java.text.DecimalFormatSymbols.getInstance(java.util.Locale.forLanguageTag("vi-VN"))).format(n);}
    private static JButton button(String text,Color bg,Color fg,Color stroke){JButton b=new RoundedActionButton(text,bg,fg,stroke);b.setFont(new Font("SansSerif",Font.BOLD,13));b.setPreferredSize(new Dimension(Math.max(112,b.getPreferredSize().width+30),40));return b;}

    private static class RoundedActionButton extends JButton{
        private final Color normal, foreground, stroke;
        RoundedActionButton(String text,Color normal,Color foreground,Color stroke){super(text);this.normal=normal;this.foreground=foreground;this.stroke=stroke;
            setForeground(foreground);setBackground(normal);setContentAreaFilled(false);setBorderPainted(false);setFocusPainted(false);setOpaque(false);
            setBorder(new EmptyBorder(0,12,0,12));setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));}
        @Override protected void paintComponent(Graphics g){Graphics2D x=(Graphics2D)g.create();x.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill=normal;if(!isEnabled())fill=new Color(normal.getRed(),normal.getGreen(),normal.getBlue(),145);else if(getModel().isPressed())fill=normal.darker();else if(getModel().isRollover())fill=normal.equals(PRIMARY)?new Color(0x1D4ED8):new Color(Math.min(255,normal.getRed()+7),Math.min(255,normal.getGreen()+7),Math.min(255,normal.getBlue()+7));
            x.setColor(fill);x.fillRoundRect(0,0,getWidth()-1,getHeight()-1,12,12);if(stroke!=null){x.setColor(stroke);x.setStroke(new BasicStroke(1f));x.drawRoundRect(0,0,getWidth()-1,getHeight()-1,12,12);}x.dispose();super.paintComponent(g);}
    }

    private static class BarChart extends JPanel{
        private final String[] labels;private final double[] values;BarChart(String[] l,double[] v){labels=l;values=v;setOpaque(false);setPreferredSize(new Dimension(420,220));}
        @Override protected void paintComponent(Graphics g){super.paintComponent(g);Graphics2D x=(Graphics2D)g.create();x.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);int w=getWidth(),h=getHeight();if(values.length==0){x.setColor(SUBTLE);x.drawString("Chưa có dữ liệu trong kỳ",20,h/2);x.dispose();return;}
            int left=50, bottom=32, top=12, plotH=h-top-bottom, plotW=w-left-12;double max=1;for(double v:values)max=Math.max(max,v);
            x.setFont(new Font("SansSerif",Font.PLAIN,10));for(int i=0;i<=4;i++){int y=top+plotH*i/4;x.setColor(BORDER);x.drawLine(left,y,w-8,y);x.setColor(SUBTLE);String val=String.format("%.1f tr",max*(4-i)/4/1_000_000d);x.drawString(val,2,y+4);}
            int gap=12,bw=Math.max(18,(plotW-gap*(values.length+1))/values.length);for(int i=0;i<values.length;i++){int x0=left+gap+i*(bw+gap),bh=(int)Math.round(plotH*values[i]/max),y=top+plotH-bh;x.setColor(PRIMARY);x.fillRoundRect(x0,y,bw,bh,8,8);x.setColor(BODY);String lab=labels[i];FontMetrics fm=x.getFontMetrics();x.drawString(lab,x0+(bw-fm.stringWidth(lab))/2,h-10);}
            x.dispose();}
    }
    private static class PaymentBar extends JComponent{private final double cash,transfer;PaymentBar(double c,double t){cash=c;transfer=t;setPreferredSize(new Dimension(400,18));setMaximumSize(new Dimension(Integer.MAX_VALUE,18));setOpaque(false);}
        @Override protected void paintComponent(Graphics g){Graphics2D x=(Graphics2D)g.create();x.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);int w=getWidth(),h=getHeight();double total=cash+transfer;int cw=total==0?w/2:(int)(w*cash/total);x.setColor(new Color(0x93C5FD));x.fillRoundRect(0,0,cw,h,12,12);x.setColor(PRIMARY);x.fillRoundRect(cw,0,w-cw,h,12,12);x.dispose();}}
}
