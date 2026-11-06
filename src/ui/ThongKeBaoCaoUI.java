package ui;

import entity.NhanVien;
import enums.VaiTro;
import controller.ThongKeDoanhThuController;
import controller.ThongKeDoanhThuController.BaoCao;
import controller.ThongKeDoanhThuController.Dong;
import com.toedter.calendar.JDateChooser;
import com.toedter.calendar.JTextFieldDateEditor;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/** Báo cáo doanh thu, xe/phiếu và dịch vụ/linh kiện dành cho Chủ cửa hàng. */
public class ThongKeBaoCaoUI extends JPanel {
    private static final Color BG = new Color(0xF8FAFC), WHITE = Color.WHITE, BORDER = new Color(0xE2E8F0);
    private static final Color INPUT = new Color(0xCBD5E1), PRIMARY = new Color(0x2563EB), PRIMARY_BG = new Color(0xEFF6FF);
    private static final Color TEXT = new Color(0x0F172A), BODY = new Color(0x334155), SUBTLE = new Color(0x64748B), RED = new Color(0xD12929);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private final NhanVien nhanVien;
    private final ThongKeDoanhThuController controller = new ThongKeDoanhThuController();
    private final JComboBox<String> cboLoai = new JComboBox<>(new String[]{ThongKeDoanhThuController.DOANH_THU, ThongKeDoanhThuController.XE_PHIEU, ThongKeDoanhThuController.DICH_VU_LINH_KIEN});
    private final JDateChooser dcTuNgay = createDateChooser(164), dcDenNgay = createDateChooser(164);
    private final JButton btnXem = new JButton("Xem");
    private final JButton btnXuat = new JButton("Xuất báo cáo");
    private final JLabel lblError = label("", 12, RED, Font.PLAIN);
    private final JPanel reportContent = new JPanel(new BorderLayout(0, 12));
    private String loai = ThongKeDoanhThuController.DOANH_THU;
    private BaoCao baoCao;
    private boolean loading;

    public ThongKeBaoCaoUI(NhanVien nhanVien) {
        this.nhanVien = nhanVien;
        setLayout(new BorderLayout()); setBackground(BG);
        setBorder(new EmptyBorder(20, 24, 22, 24));
        if (nhanVien == null || nhanVien.getVaiTro() != VaiTro.CHU_CUA_HANG) {
            add(new JLabel("Chỉ Chủ cửa hàng được xem báo cáo này."), BorderLayout.CENTER); return;
        }
        add(buildContent(), BorderLayout.CENTER);
        bindActions();
        Date today = Date.from(LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        dcTuNgay.setDate(today);
        dcDenNgay.setDate(today);
        SwingUtilities.invokeLater(() -> { dcTuNgay.getCalendarButton().requestFocusInWindow(); taiBaoCao(); });
    }

    private JComponent buildContent() {
        JPanel root = new MainFrame.ScrollableContentPanel(); root.setOpaque(false);
        JComponent heading = buildHeading(); heading.setAlignmentX(Component.CENTER_ALIGNMENT); root.add(heading); root.add(Box.createVerticalStrut(18));
        JComponent filters = buildFilters(); filters.setAlignmentX(Component.CENTER_ALIGNMENT); root.add(filters); root.add(Box.createVerticalStrut(18));
        reportContent.setOpaque(false); reportContent.setAlignmentX(Component.CENTER_ALIGNMENT);
        reportContent.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        root.add(reportContent); root.add(Box.createVerticalStrut(8));
        JPanel status = new JPanel(new BorderLayout(8, 0)); status.setOpaque(false); status.setAlignmentX(Component.CENTER_ALIGNMENT);
        JScrollPane scroll = new JScrollPane(root); scroll.setBorder(null); scroll.getViewport().setOpaque(false); scroll.setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER); scroll.getVerticalScrollBar().setUnitIncrement(18);
        return scroll;
    }

    private JComponent buildHeading() {
        JPanel p = new JPanel(new BorderLayout()); p.setOpaque(false); p.setAlignmentX(Component.CENTER_ALIGNMENT);
        JPanel left = new JPanel(new BorderLayout(12, 0)); left.setOpaque(false);
        JLabel icon = new JLabel(); icon.setPreferredSize(new Dimension(40, 40));
        icon.setText("📊"); icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 25));
        try (InputStream in = getClass().getResourceAsStream("/revenue_illustration.png")) {
            if (in != null) { Image im = ImageIO.read(in).getScaledInstance(40, 40, Image.SCALE_SMOOTH); icon.setIcon(new ImageIcon(im)); }
        } catch (IOException ignored) { }
        JPanel texts = new JPanel(); texts.setOpaque(false); texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        JLabel title = label("Thống kê báo cáo", 24, TEXT, Font.BOLD);
        texts.add(title);
        left.add(icon, BorderLayout.WEST); left.add(texts, BorderLayout.CENTER); p.add(left, BorderLayout.WEST);
        return p;
    }

    private JComponent buildFilters() {
        JPanel card = new JPanel(new GridBagLayout()); card.setOpaque(false);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        GridBagConstraints g = new GridBagConstraints(); g.gridy=0; g.insets=new Insets(0, 0, 0, 12); g.anchor=GridBagConstraints.WEST;
        addField(card, g, "Loại báo cáo", cboLoai, 0, 0.28);
        addField(card, g, "Từ ngày", dcTuNgay, 1, 0.20);
        addField(card, g, "Đến ngày", dcDenNgay, 2, 0.20);
        btnXem.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnXem.setForeground(WHITE);
        btnXem.setBackground(PRIMARY);
        btnXem.setPreferredSize(new Dimension(96, 44));
        btnXem.setMinimumSize(new Dimension(96, 44));
        btnXem.setMaximumSize(new Dimension(96, 44));
        btnXem.setFocusPainted(false);
        btnXem.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnXem.putClientProperty("JButton.arc", 12);
        btnXuat.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnXuat.setForeground(BODY);
        btnXuat.setBackground(WHITE);
        btnXuat.setPreferredSize(new Dimension(132, 44));
        btnXuat.setMinimumSize(new Dimension(132, 44));
        btnXuat.setMaximumSize(new Dimension(132, 44));
        btnXuat.setFocusPainted(false);
        btnXuat.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnXuat.putClientProperty("JButton.arc", 12);
        btnXuat.putClientProperty("JButton.borderColor", INPUT);
        btnXuat.setEnabled(false);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(btnXem);
        buttons.add(btnXuat);
        JPanel action = new JPanel(new BorderLayout(0, 6));
        action.setOpaque(false);
        action.add(label(" ", 13, BODY, Font.PLAIN), BorderLayout.NORTH);
        action.add(buttons, BorderLayout.CENTER);
        GridBagConstraints actionConstraints = (GridBagConstraints) g.clone();
        actionConstraints.gridx = 3;
        actionConstraints.weightx = 0;
        actionConstraints.insets = new Insets(0, 0, 0, 0);
        actionConstraints.fill = GridBagConstraints.NONE;
        card.add(action, actionConstraints);
        GridBagConstraints errorConstraints = (GridBagConstraints) g.clone();
        errorConstraints.gridx = 0; errorConstraints.gridy = 1; errorConstraints.gridwidth = 4;
        errorConstraints.weightx = 1; errorConstraints.fill = GridBagConstraints.HORIZONTAL;
        errorConstraints.insets = new Insets(8, 0, 0, 0);
        lblError.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lblError, errorConstraints);
        cboLoai.addActionListener(e -> { String selected=(String)cboLoai.getSelectedItem(); if(selected!=null&&!selected.equals(loai)){loai=selected;taiBaoCao();} });
        btnXem.addActionListener(e -> taiBaoCao());
        btnXuat.addActionListener(e -> xuatBaoCao());
        return card;
    }

    private void addField(JPanel card, GridBagConstraints base, String caption, JComponent field, int x, double weight) {
        JPanel p = new JPanel(new BorderLayout(0, 6)); p.setOpaque(false); p.setMinimumSize(new Dimension(150, 64));
        p.add(label(caption, 13, BODY, Font.PLAIN), BorderLayout.NORTH);
        field.setFont(new Font("SansSerif", Font.PLAIN, 13));
        field.setPreferredSize(new Dimension(caption.equals("Loại báo cáo") ? 216 : 164, 44));
        if (!(field instanceof JDateChooser)) {
            field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(INPUT), new EmptyBorder(0, 10, 0, 10)));
        }
        p.add(field, BorderLayout.CENTER); GridBagConstraints g=(GridBagConstraints)base.clone(); g.gridx=x; g.weightx=weight; g.fill=GridBagConstraints.HORIZONTAL; card.add(p,g);
    }

    private void bindActions() {
        dcTuNgay.addPropertyChangeListener("date", e -> { clearError(); capNhatTrangThaiXuat(); });
        dcDenNgay.addPropertyChangeListener("date", e -> { clearError(); capNhatTrangThaiXuat(); });
    }

    private void taiBaoCao() {
        if(loading)return;
        LocalDate from,to;
        try { from=UIInputRules.readDate(dcTuNgay); to=UIInputRules.readDate(dcDenNgay);
            if(to.isBefore(from)) throw new IllegalArgumentException("Ngày kết thúc phải bằng hoặc sau ngày bắt đầu.");
        } catch(DateTimeParseException|IllegalArgumentException ex){showError(ex.getMessage()==null?"Ngày phải có định dạng dd/MM/yyyy.":ex.getMessage());return;}
        setLoading(true); clearError();
        new SwingWorker<BaoCao,Void>(){
            @Override protected BaoCao doInBackground(){return controller.layBaoCao(nhanVien.getVaiTro(),loai,from,to);}
            @Override protected void done(){try{baoCao=get(); renderReport();}
                catch(Exception ex){baoCao=null; renderEmpty(); Throwable cause=ex.getCause()==null?ex:ex.getCause(); showError("Không thể tải thống kê: "+(cause.getMessage()==null?"hãy kiểm tra kết nối cơ sở dữ liệu.":cause.getMessage()));}
                finally{setLoading(false);}}
        }.execute();
    }

    private void renderReport() {
        reportContent.removeAll();
        JPanel body = new JPanel(); body.setOpaque(false); body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        JPanel charts = new JPanel(new GridLayout(1, 2, 16, 0)); charts.setOpaque(false);
        charts.setAlignmentX(Component.LEFT_ALIGNMENT);
        charts.add(buildChartCard());
        charts.add(loai.equals(ThongKeDoanhThuController.DOANH_THU) ? buildPaymentCard() : buildInsightCard());
        body.add(charts); body.add(leftGap(16));
        if (loai.equals(ThongKeDoanhThuController.DOANH_THU)) {
            body.add(buildInsightCard()); body.add(leftGap(16));
        }
        body.add(buildSummary()); body.add(leftGap(16));
        body.add(buildTableCard());
        body.setAlignmentX(Component.LEFT_ALIGNMENT);
        reportContent.add(body, BorderLayout.CENTER); reportContent.revalidate(); reportContent.repaint();
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

    private JComponent buildChartCard(){
        JPanel p=cardPanel(); p.setLayout(new BorderLayout(0,10));
        String title=loai.equals(ThongKeDoanhThuController.DOANH_THU)?"Doanh thu sau hoàn trong kỳ (triệu VNĐ)":loai.equals(ThongKeDoanhThuController.XE_PHIEU)?"Phiếu theo trạng thái":"Dịch vụ và linh kiện sử dụng nhiều";
        p.add(label(title,16,TEXT,Font.BOLD),BorderLayout.NORTH);
        p.add(new BarChart(baoCao.chartLabels,baoCao.chartValues,loai.equals(ThongKeDoanhThuController.DOANH_THU)),BorderLayout.CENTER);
        return p;
    }
    private JComponent buildPaymentCard(){
        JPanel p=cardPanel(); p.setLayout(new BorderLayout(0,10));
        p.add(label("Cơ cấu thanh toán",16,TEXT,Font.BOLD),BorderLayout.NORTH);
        JPanel details=new JPanel(new BorderLayout(18,0)); details.setOpaque(false);
        details.add(new PaymentDonut(baoCao.tienMat,baoCao.chuyenKhoan,baoCao.tongThu),BorderLayout.WEST);
        JPanel values=new JPanel(); values.setOpaque(false); values.setLayout(new BoxLayout(values,BoxLayout.Y_AXIS));
        values.add(paymentLine("Chuyển khoản",baoCao.chuyenKhoan,baoCao.tongThu,PRIMARY)); values.add(Box.createVerticalStrut(10));
        values.add(paymentLine("Tiền mặt",baoCao.tienMat,baoCao.tongThu,new Color(0x93C5FD)));
        JPanel refund=new JPanel(); refund.setBackground(BG); refund.setLayout(new BoxLayout(refund,BoxLayout.Y_AXIS));
        refund.setBorder(new EmptyBorder(10,12,10,12)); refund.add(label("Hoàn tiền",12,SUBTLE,Font.PLAIN));
        refund.add(label(money(baoCao.hoanTien)+" VNĐ",16,RED,Font.BOLD));
        values.add(Box.createVerticalStrut(14)); values.add(refund);
        details.add(values,BorderLayout.CENTER); p.add(details,BorderLayout.CENTER); return p;
    }
    private JPanel paymentLine(String name,double value,double total,Color color){
        JPanel line=new JPanel(new BorderLayout(8,0)); line.setOpaque(false);
        JLabel dot=label("●",13,color,Font.BOLD); line.add(dot,BorderLayout.WEST);
        double percent=total>0?value*100/total:0;
        line.add(label(name+" · "+money(value)+" · "+String.format(java.util.Locale.US,"%.1f%%",percent),12,BODY,Font.PLAIN),BorderLayout.CENTER);
        return line;
    }
    private JComponent buildInsightCard(){
        JPanel p=cardPanel(); p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
        JLabel title=label("Nhận xét",16,TEXT,Font.BOLD); title.setAlignmentX(Component.LEFT_ALIGNMENT); p.add(title); p.add(leftGap(8));
        for(String insight:baoCao.insights){
            JTextArea line=new JTextArea("•  "+insight); line.setFont(new Font("SansSerif",Font.PLAIN,12)); line.setForeground(BODY);
            line.setOpaque(false); line.setEditable(false); line.setLineWrap(true); line.setWrapStyleWord(true); line.setBorder(new EmptyBorder(3,0,3,0));
            line.setAlignmentX(Component.LEFT_ALIGNMENT); line.setMaximumSize(new Dimension(Integer.MAX_VALUE,40)); p.add(line);
        }
        return p;
    }

    private String[] reportColumns(){return loai.equals(ThongKeDoanhThuController.DOANH_THU)?new String[]{"Ngày / kỳ","Số HĐ","Tiền mặt","Chuyển khoản","Tổng thu","Hoàn tiền","Sau hoàn"}:
            loai.equals(ThongKeDoanhThuController.XE_PHIEU)?new String[]{"Trạng thái phiếu","Số phiếu","Số xe"}:new String[]{"Loại","Mã","Tên dịch vụ / linh kiện","Số phiếu","Số lượng","Thành tiền (VNĐ)"};}
    private JComponent buildTableCard(){JPanel p=cardPanel();p.setLayout(new BorderLayout(0,0));String[] columns=reportColumns();
        DefaultTableModel m=new DefaultTableModel(columns,0){@Override public boolean isCellEditable(int row,int column){return false;}};
        for(Dong d:baoCao.rows)m.addRow(d.cells);
        JTable table=new JTable(m);table.setRowHeight(42);table.setFont(new Font("SansSerif",Font.PLAIN,12));table.setForeground(BODY);table.setGridColor(BORDER);table.setShowVerticalLines(false);
        table.getTableHeader().setFont(new Font("SansSerif",Font.BOLD,12));table.getTableHeader().setBackground(BG);table.getTableHeader().setForeground(BODY);table.setFillsViewportHeight(true);
        JScrollPane scroll=new JScrollPane(table);scroll.setBorder(BorderFactory.createLineBorder(BORDER));scroll.getViewport().setBackground(WHITE);
        scroll.setPreferredSize(new Dimension(880,Math.min(330,34+table.getRowHeight()*(m.getRowCount()+1))));p.add(scroll,BorderLayout.CENTER);
        JPanel footer=new JPanel(new BorderLayout());
        if(loai.equals(ThongKeDoanhThuController.DOANH_THU)) footer.add(label("Doanh thu sau hoàn không phải lợi nhuận",12,SUBTLE,Font.PLAIN),BorderLayout.EAST);p.add(footer,BorderLayout.SOUTH);return p;}

    private void capNhatTrangThaiXuat(){
        boolean current=baoCao!=null&&!loading&&baoCao.loai.equals(loai);
        if(current){
            try{current=baoCao.tuNgay.equals(UIInputRules.readDate(dcTuNgay))&&baoCao.denNgay.equals(UIInputRules.readDate(dcDenNgay));}
            catch(DateTimeParseException ex){current=false;}
        }
        btnXuat.setEnabled(current);
    }

    private void xuatBaoCao(){
        capNhatTrangThaiXuat();
        if(!btnXuat.isEnabled())return;
        JFileChooser chooser=new JFileChooser();
        chooser.setDialogTitle("Xuất báo cáo thống kê");
        chooser.setSelectedFile(new java.io.File("Thong_ke_bao_cao_"+LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)+".csv"));
        if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        Path path=chooser.getSelectedFile().toPath();
        if(!path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".csv"))path=path.resolveSibling(path.getFileName()+".csv");
        if(Files.exists(path)&&JOptionPane.showConfirmDialog(this,"Tệp đã tồn tại. Bạn có muốn ghi đè không?","Xác nhận xuất báo cáo",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        try(var writer=Files.newBufferedWriter(path,StandardCharsets.UTF_8)){
            writer.write('\uFEFF');
            writeCsvRow(writer,"Báo cáo", "Thống kê báo cáo");
            writeCsvRow(writer,"Loại báo cáo",baoCao.loai);
            writeCsvRow(writer,"Từ ngày",DATE.format(baoCao.tuNgay));
            writeCsvRow(writer,"Đến ngày",DATE.format(baoCao.denNgay));
            writeCsvRow(writer);
            writeCsvRow(writer,reportColumns());
            for(Dong row:baoCao.rows)writeCsvRow(writer,row.cells);
            JOptionPane.showMessageDialog(this,"Đã xuất báo cáo thành công.","Xuất báo cáo",JOptionPane.INFORMATION_MESSAGE);
        }catch(IOException ex){
            JOptionPane.showMessageDialog(this,"Không thể xuất báo cáo: "+ex.getMessage(),"Xuất báo cáo",JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void writeCsvRow(java.io.Writer writer,String... values)throws IOException{
        for(int i=0;i<values.length;i++){
            if(i>0)writer.write(',');
            String value=values[i]==null?"":values[i];
            writer.write('"');writer.write(value.replace("\"","\"\""));writer.write('"');
        }
        writer.write("\r\n");
    }

    private void renderEmpty(){reportContent.removeAll();reportContent.revalidate();reportContent.repaint();}
    private void setLoading(boolean value){loading=value;cboLoai.setEnabled(!value);dcTuNgay.setEnabled(!value);dcDenNgay.setEnabled(!value);dcTuNgay.getCalendarButton().setEnabled(!value);dcDenNgay.getCalendarButton().setEnabled(!value);btnXem.setEnabled(!value);capNhatTrangThaiXuat();}
    private void showError(String message){lblError.setText(message==null?"Có lỗi khi tải dữ liệu.":message);}
    private void clearError(){lblError.setText("");}
    private static Component leftGap(int height){JComponent gap=(JComponent)Box.createRigidArea(new Dimension(0,height));gap.setAlignmentX(Component.LEFT_ALIGNMENT);return gap;}
    private JPanel cardPanel(){JPanel p=new JPanel();p.setBackground(WHITE);p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),new EmptyBorder(16,16,16,16)));p.setAlignmentX(Component.LEFT_ALIGNMENT);return p;}
    private static JLabel label(String text,int size,Color color,int style){JLabel l=new JLabel(text);l.setFont(new Font("SansSerif",style,size));l.setForeground(color);return l;}
    private static String money(double n){return new java.text.DecimalFormat("#,##0",java.text.DecimalFormatSymbols.getInstance(java.util.Locale.forLanguageTag("vi-VN"))).format(n);}

    private static JDateChooser createDateChooser(int width) {
        JDateChooser chooser = new JDateChooser();
        chooser.setDateFormatString("dd/MM/yyyy");
        chooser.setPreferredSize(new Dimension(width, 44));
        chooser.setBackground(WHITE);
        if (chooser.getDateEditor() instanceof JTextFieldDateEditor editor) {
            editor.setEditable(false);
            editor.setFont(new Font("SansSerif", Font.PLAIN, 13));
            editor.setBorder(new EmptyBorder(0, 10, 0, 2));
            editor.setMargin(new Insets(0, 0, 0, 0));
        }
        JButton calendar = chooser.getCalendarButton();
        calendar.setPreferredSize(new Dimension(32, 34));
        calendar.setFocusable(false);
        calendar.setMargin(new Insets(0, 0, 0, 0));
        chooser.setBorder(new TraCuuUI.RoundedLineBorder(INPUT, 10));
        return chooser;
    }

    private static class BarChart extends JPanel {
        private final String[] labels;
        private final double[] values;
        private final boolean currency;
        BarChart(String[] labels, double[] values, boolean currency) {
            this.labels=labels; this.values=values; this.currency=currency;
            setOpaque(false); setPreferredSize(new Dimension(420,220));
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D x=(Graphics2D)g.create();
            x.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(),h=getHeight();
            if(values.length==0){x.setColor(SUBTLE);x.drawString("Chưa có dữ liệu trong kỳ",20,h/2);x.dispose();return;}
            int left=42, right=8, top=18, bottom=48, plotH=Math.max(30,h-top-bottom), plotW=w-left-right;
            double max=currency?1_000_000d:1d;for(double v:values)max=Math.max(max,v);
            x.setFont(new Font("SansSerif",Font.PLAIN,10));
            for(int i=0;i<=4;i++){
                int y=top+plotH*i/4; x.setColor(BORDER);x.drawLine(left,y,w-right,y);x.setColor(SUBTLE);
                String axis=currency?String.format(java.util.Locale.US,"%.1f",max*(4-i)/4/1_000_000d):String.format(java.util.Locale.US,"%.0f",max*(4-i)/4);
                x.drawString(axis,4,y+4);
            }
            int gap=Math.max(3,Math.min(12,plotW/Math.max(3,values.length*3)));int bw=Math.max(8,(plotW-gap*(values.length+1))/values.length);
            int labelStep=Math.max(1,(int)Math.ceil(values.length/10d));
            for(int i=0;i<values.length;i++){
                int x0=left+gap+i*(bw+gap), bh=(int)Math.round(plotH*Math.max(0,values[i])/max), y=top+plotH-bh;
                x.setColor(PRIMARY);x.fillRoundRect(x0,y,bw,bh,8,8);
                if(i%labelStep==0||i==values.length-1){
                    x.setFont(new Font("SansSerif",Font.BOLD,10));x.setColor(PRIMARY);
                    String amount=currency?String.format(java.util.Locale.US,"%.1f",values[i]/1_000_000d):money(values[i]);
                    FontMetrics fm=x.getFontMetrics();x.drawString(amount,x0+(bw-fm.stringWidth(amount))/2,Math.max(12,y-5));
                    x.setFont(new Font("SansSerif",Font.PLAIN,10));x.setColor(BODY);
                    String lab=labels[i];fm=x.getFontMetrics();
                    x.drawString(lab,x0+(bw-fm.stringWidth(lab))/2,h-24);
                }
            }
            x.dispose();
        }
    }
    private static class PaymentDonut extends JComponent {
        private final double cash,transfer,total;
        PaymentDonut(double cash,double transfer,double total){this.cash=cash;this.transfer=transfer;this.total=total;setPreferredSize(new Dimension(184,184));setMinimumSize(new Dimension(160,160));setOpaque(false);}
        @Override protected void paintComponent(Graphics g){
            Graphics2D x=(Graphics2D)g.create();x.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int side=Math.min(getWidth(),getHeight());float stroke=Math.max(16,(side-8)*0.18f/1.18f);int size=(int)((side-8)/1.18f), ox=(getWidth()-size)/2, oy=(getHeight()-size)/2;
            x.setStroke(new BasicStroke(stroke,BasicStroke.CAP_BUTT,BasicStroke.JOIN_ROUND));double sum=cash+transfer;
            x.setColor(new Color(0x93C5FD));x.drawArc(ox,oy,size,size,90,360);
            if(sum>0){int transferArc=(int)Math.round(360*transfer/sum);x.setColor(PRIMARY);x.drawArc(ox,oy,size,size,90,-transferArc);}
            x.setFont(new Font("SansSerif",Font.BOLD,22));x.setColor(TEXT);String value=String.format(java.util.Locale.US,"%.1f tr",total/1_000_000d);
            FontMetrics fm=x.getFontMetrics();x.drawString(value,(getWidth()-fm.stringWidth(value))/2,getHeight()/2+2);
            x.setFont(new Font("SansSerif",Font.PLAIN,11));x.setColor(SUBTLE);String label="Tổng thu";fm=x.getFontMetrics();x.drawString(label,(getWidth()-fm.stringWidth(label))/2,getHeight()/2+20);x.dispose();
        }
    }
}
