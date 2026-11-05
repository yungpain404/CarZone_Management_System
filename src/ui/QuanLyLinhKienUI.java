package ui;

import controller.LinhKienController;
import controller.PhieuYeuCauNhapLinhKienController;
import entity.LinhKien;
import entity.PhieuYeuCauNhapLinhKien;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** Danh mục linh kiện Chủ cửa hàng; chỉ tìm và hiển thị dữ liệu sau khi có từ khóa. */
public class QuanLyLinhKienUI extends JPanel {
    private static final Color BG = new Color(0xF8FAFC);
    private static final Color WHITE = Color.WHITE;
    private static final Color BORDER = new Color(0xDCE4EF);
    private static final Color PRIMARY = new Color(0x2563EB);
    private static final Color PRIMARY_BG = new Color(0xEFF6FF);
    private static final Color TEXT = new Color(0x17233A);
    private static final Color BODY = new Color(0x3B4B65);
    private static final Color SUBTLE = new Color(0x6B7D98);
    private static final Color RED = new Color(0xDC2626);

    private final LinhKienController controller = new LinhKienController();
    private final PhieuYeuCauNhapLinhKienController yeuCauController = new PhieuYeuCauNhapLinhKienController();
    private final JTextField txtTimKiem = new JTextField();
    private final JComboBox<String> cboKinhDoanh = new JComboBox<>(new String[]{"Tất cả", "Đang kinh doanh", "Ngừng kinh doanh"});
    private final JComboBox<String> cboTonKho = new JComboBox<>(new String[]{"Tất cả", "Còn hàng", "Hết hàng"});
    private final JButton btnTim = new JButton();
    private JButton btnTimAction;
    private final JButton btnLamMoi = button("Làm mới", false);
    private final JButton btnThem = button("Thêm", true);
    private final JButton btnSua = button("Sửa", false);
    private final JButton btnXoa = button("Xóa", false);
    private final JButton btnTrangThai = button("Ngừng kinh doanh", false);
    private final JButton btnNhapKho = button("Xác nhận nhập kho", false);
    private final JLabel lblStatus = label("", 12, SUBTLE, Font.PLAIN);
    private final JLabel lblError = label("", 12, RED, Font.PLAIN);
    private final JPanel pnlKetQua = new JPanel(new BorderLayout());
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Mã linh kiện", "Tên linh kiện", "Đơn vị", "Giá bán (VNĐ)", "Tồn", "Trạng thái"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private List<LinhKien> ketQua = new ArrayList<>();
    private boolean daTimKiem;
    private boolean dangXuLy;

    public QuanLyLinhKienUI() {
        setLayout(new BorderLayout()); setBackground(BG); setBorder(new EmptyBorder(24, 24, 22, 24));
        add(buildContent(), BorderLayout.CENTER); bindActions(); capNhatNut(); hienTrangThaiRong();
        SwingUtilities.invokeLater(() -> txtTimKiem.requestFocusInWindow());
    }

    private JComponent buildContent() {
        JPanel root = new MainFrame.ScrollableContentPanel(); root.setOpaque(false);
        JPanel heading = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0)); heading.setOpaque(false); heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        heading.add(label("📦", 25, TEXT, Font.PLAIN)); heading.add(label("Quản lý linh kiện và tồn kho", 24, TEXT, Font.BOLD));
        root.add(heading); root.add(leftGap(18)); root.add(buildFilters()); root.add(leftGap(14)); root.add(buildActions());
        root.add(leftGap(18)); pnlKetQua.setOpaque(false); pnlKetQua.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlKetQua.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE)); root.add(pnlKetQua);
        root.add(leftGap(8));
        JPanel status = new JPanel(new BorderLayout(8, 0)); status.setOpaque(false); status.setAlignmentX(Component.LEFT_ALIGNMENT);
        status.add(lblError, BorderLayout.WEST); status.add(lblStatus, BorderLayout.EAST); root.add(status);
        JScrollPane scroll = new JScrollPane(root); scroll.setBorder(null); scroll.setOpaque(false); scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER); scroll.getVerticalScrollBar().setUnitIncrement(18);
        return scroll;
    }

    private JComponent buildFilters() {
        JPanel row = new JPanel(new GridBagLayout()); row.setOpaque(false); row.setAlignmentX(Component.LEFT_ALIGNMENT);
        GridBagConstraints g = new GridBagConstraints(); g.gridy=0; g.anchor=GridBagConstraints.WEST; g.insets=new Insets(0,0,0,12);
        addFilter(row,g,"Tìm mã / tên linh kiện",txtTimKiem,0,0.34);
        addFilter(row,g,"Trạng thái kinh doanh",cboKinhDoanh,1,0.24);
        addFilter(row,g,"Tình trạng tồn kho",cboTonKho,2,0.22);
        g.gridx=3; g.weightx=1; g.fill=GridBagConstraints.HORIZONTAL; g.insets=new Insets(0,0,0,0); row.add(Box.createHorizontalGlue(),g);
        return row;
    }

    private void addFilter(JPanel row, GridBagConstraints base, String caption, JComponent field, int x, double weight) {
        JPanel cell = new JPanel(new BorderLayout(0,6)); cell.setOpaque(false); cell.setMinimumSize(new Dimension(160,64));
        cell.add(label(caption,13,BODY,Font.PLAIN),BorderLayout.NORTH); field.setFont(new Font("SansSerif",Font.PLAIN,13));
        field.setPreferredSize(new Dimension(caption.startsWith("Tìm")?220:190,44));
        if (field instanceof JTextField input) {
            JPanel search = new JPanel(new BorderLayout()); search.setBackground(WHITE);
            search.setBorder(new RoundedBorder(new Color(0xC9D5E5),10)); input.setBorder(new EmptyBorder(0,11,0,8)); input.setOpaque(false);
            input.putClientProperty("JTextField.placeholderText","Nhập mã hoặc tên…");
            btnTim.setIcon(new MainFrame.SearchIcon(SUBTLE,16)); btnTim.setBackground(WHITE); btnTim.setBorder(new EmptyBorder(0,8,0,10));
            btnTim.setFocusPainted(false); btnTim.setToolTipText("Tìm linh kiện"); btnTim.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            search.add(input,BorderLayout.CENTER); search.add(btnTim,BorderLayout.EAST); search.setPreferredSize(new Dimension(220,44)); cell.add(search,BorderLayout.CENTER);
        } else {
            field.setBackground(WHITE); field.setBorder(new RoundedBorder(new Color(0xC9D5E5),10)); cell.add(field,BorderLayout.CENTER);
        }
        GridBagConstraints g=(GridBagConstraints)base.clone(); g.gridx=x; g.weightx=weight; g.fill=GridBagConstraints.HORIZONTAL; row.add(cell,g);
    }

    private JComponent buildActions() {
        JPanel row=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0)); row.setOpaque(false); row.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnTimAction=button("Tìm kiếm",true);
        btnTimAction.addActionListener(e->timKiem());
        row.add(btnTimAction);
        row.add(btnLamMoi); row.add(btnThem); row.add(btnSua); row.add(btnXoa); row.add(btnTrangThai); row.add(btnNhapKho);
        return row;
    }

    private void bindActions() {
        btnTim.addActionListener(e->timKiem()); txtTimKiem.addActionListener(e->timKiem());
        cboKinhDoanh.addActionListener(e->{if(daTimKiem)timKiem();});
        cboTonKho.addActionListener(e->{if(daTimKiem)timKiem();});
        btnLamMoi.addActionListener(e->lamMoi()); btnThem.addActionListener(e->moFormThem());
        btnSua.addActionListener(e->moFormSua()); btnXoa.addActionListener(e->xoaLinhKien()); btnTrangThai.addActionListener(e->doiTrangThai()); btnNhapKho.addActionListener(e->chonYeuCauNhapKho());
        table.getSelectionModel().addListSelectionListener(e->{if(!e.getValueIsAdjusting())capNhatNut();});
        table.addMouseListener(new java.awt.event.MouseAdapter(){@Override public void mouseClicked(java.awt.event.MouseEvent e){if(e.getClickCount()==2&&table.getSelectedRow()>=0)moFormSua();}});
        txtTimKiem.addKeyListener(new KeyAdapter(){@Override public void keyPressed(KeyEvent e){if(e.getKeyCode()==KeyEvent.VK_ESCAPE&&!txtTimKiem.getText().isBlank())lamMoi();}});
    }

    private void timKiem() {
        String query=txtTimKiem.getText().trim();
        if(!UIInputRules.isSearch(query)){showError("Từ khóa tìm kiếm tối đa 150 ký tự; chỉ nhập chữ, số và dấu câu.");txtTimKiem.requestFocusInWindow();return;}
        Boolean kinhDoanh=switch(String.valueOf(cboKinhDoanh.getSelectedItem())){case "Đang kinh doanh"->true;case "Ngừng kinh doanh"->false;default->null;};
        Boolean conTon=switch(String.valueOf(cboTonKho.getSelectedItem())){case "Còn hàng"->true;case "Hết hàng"->false;default->null;};
        setBusy(true);clearError();
        new SwingWorker<List<LinhKien>,Void>(){
            @Override protected List<LinhKien> doInBackground(){return controller.timKiem(query,kinhDoanh,conTon);}
            @Override protected void done(){
                try{ketQua=get();daTimKiem=true;hienDanhSach();lblStatus.setText(ketQua.size()+" linh kiện");}
                catch(Exception ex){ketQua=new ArrayList<>();hienTrangThaiRong();showWorkerError("Không thể tìm linh kiện",ex);}
                finally{setBusy(false);}
            }
        }.execute();
    }

    /** Hiển thị ngay danh sách linh kiện hết hàng từ KPI Dashboard. */
    public void locHetHangTuDashboard() {
        daTimKiem = false;
        txtTimKiem.setText("");
        cboKinhDoanh.setSelectedIndex(0);
        cboTonKho.setSelectedItem("Hết hàng");
        timKiem();
    }

    /** Mở danh mục linh kiện với kết quả tra cứu được chọn từ thanh header. */
    public void timKiemTuHeader(String tuKhoa) {
        daTimKiem=false;
        txtTimKiem.setText(tuKhoa==null?"":tuKhoa);
        cboKinhDoanh.setSelectedIndex(0);cboTonKho.setSelectedIndex(0);
        timKiem();
    }

    private void lamMoi() {
        // Reset cờ trước các select vì listener tự tìm kiếm khi đã từng có kết quả.
        daTimKiem=false;
        txtTimKiem.setText(""); cboKinhDoanh.setSelectedIndex(0); cboTonKho.setSelectedIndex(0);
        ketQua=new ArrayList<>();clearError();hienTrangThaiRong();
        lblStatus.setText("");
    }

    private void hienDanhSach() {
        pnlKetQua.removeAll(); model.setRowCount(0); table.clearSelection();
        JPanel card=cardPanel();card.setLayout(new BorderLayout());card.setAlignmentX(Component.LEFT_ALIGNMENT);
        table.setFont(new Font("SansSerif",Font.PLAIN,12));table.setForeground(BODY);table.setRowHeight(44);table.setShowVerticalLines(false);
        table.setGridColor(BORDER);table.setSelectionBackground(PRIMARY_BG);table.setSelectionForeground(PRIMARY);table.setFillsViewportHeight(true);
        table.getTableHeader().setFont(new Font("SansSerif",Font.BOLD,12));table.getTableHeader().setForeground(BODY);table.getTableHeader().setBackground(BG);
        table.getTableHeader().setPreferredSize(new Dimension(0,42));
        DefaultTableCellRenderer striped=new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable t,Object value,boolean selected,boolean focus,int row,int column){
                JLabel cell=(JLabel)super.getTableCellRendererComponent(t,value,selected,focus,row,column);
                cell.setForeground(BODY);cell.setBackground(selected?PRIMARY_BG:(row%2==0?new Color(0xF7F9FC):WHITE));
                cell.setBorder(new EmptyBorder(0,10,0,10));return cell;
            }
        };
        for(int column=0;column<table.getColumnCount();column++)table.getColumnModel().getColumn(column).setCellRenderer(striped);
        DefaultTableCellRenderer number=new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable t,Object value,boolean selected,boolean focus,int row,int column){
                JLabel cell=(JLabel)super.getTableCellRendererComponent(t,value,selected,focus,row,column);
                cell.setHorizontalAlignment(SwingConstants.RIGHT);cell.setForeground(BODY);cell.setBackground(selected?PRIMARY_BG:(row%2==0?new Color(0xF7F9FC):WHITE));cell.setBorder(new EmptyBorder(0,10,0,10));return cell;
            }
        };table.getColumnModel().getColumn(3).setCellRenderer(number);
        JScrollPane tableScroll=new JScrollPane(table);tableScroll.setBorder(BorderFactory.createLineBorder(BORDER));tableScroll.getViewport().setBackground(WHITE);
        card.add(tableScroll,BorderLayout.CENTER);pnlKetQua.add(card,BorderLayout.CENTER);
        if(ketQua.isEmpty()){
            JPanel empty=new JPanel(new GridBagLayout());empty.setBackground(WHITE);empty.setPreferredSize(new Dimension(400,120));
            empty.add(label("Không tìm thấy linh kiện phù hợp.",13,SUBTLE,Font.PLAIN));card.add(empty,BorderLayout.CENTER);
        }
        DecimalFormat money=new DecimalFormat("#,##0");
        for(LinhKien lk:ketQua)model.addRow(new Object[]{lk.getMaLK(),lk.getTenLK(),lk.getDonViTinh(),money.format(lk.getDonGia()),lk.getSoLuongTon(),lk.isDangKinhDoanh()?"Đang kinh doanh":"Ngừng kinh doanh"});
        if(!ketQua.isEmpty())table.setRowSelectionInterval(0,0);
        pnlKetQua.revalidate();pnlKetQua.repaint();capNhatNut();
    }

    private void hienTrangThaiRong(){
        pnlKetQua.removeAll();model.setRowCount(0);table.clearSelection();
        JPanel prompt=new JPanel(new BorderLayout());prompt.setOpaque(false);prompt.setAlignmentX(Component.LEFT_ALIGNMENT);
        prompt.add(label("Chưa có kết quả tra cứu.",13,SUBTLE,Font.PLAIN),BorderLayout.WEST);
        pnlKetQua.add(prompt,BorderLayout.NORTH);pnlKetQua.revalidate();pnlKetQua.repaint();capNhatNut();
    }

    private LinhKien selected(){int row=table.getSelectedRow();if(row<0||row>=ketQua.size())return null;return ketQua.get(table.convertRowIndexToModel(row));}
    private void moFormThem(){runTask("Không thể chuẩn bị mã linh kiện",controller::layMaTiepTheo,code->moForm(null,code));}
    private void moFormSua(){LinhKien current=selected();if(current==null)return;runTask("Không thể tải linh kiện",()->controller.timTheoMa(current.getMaLK()),item->moForm(item,item.getMaLK()));}
    private void moForm(LinhKien current,String code){
        LinhKienFormDialog dialog=new LinhKienFormDialog(SwingUtilities.getWindowAncestor(this),controller,current,code);dialog.setVisible(true);
        if(dialog.saved!=null){txtTimKiem.setText(dialog.saved.getMaLK());timKiem();}
    }
    private void xoaLinhKien(){
        LinhKien current=selected();if(current==null)return;
        int answer=JOptionPane.showConfirmDialog(this,"Xóa linh kiện "+current.getMaLK()+" — "+current.getTenLK()+"?\nLinh kiện đã có lịch sử sử dụng chỉ có thể ngừng kinh doanh.","Xác nhận xóa",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);
        if(answer==JOptionPane.YES_OPTION)runTask("Không thể xóa linh kiện",()->{controller.xoa(current.getMaLK());return current.getMaLK();},code->timKiem());
    }
    private void doiTrangThai(){LinhKien current=selected();if(current==null)return;boolean active=!current.isDangKinhDoanh();runTask("Không thể đổi trạng thái",()->{controller.doiTrangThai(current.getMaLK(),active);return current.getMaLK();},code->timKiem());}

    private void chonYeuCauNhapKho(){
        LinhKien current=selected();if(current==null)return;
        runTask("Không thể tải yêu cầu đã duyệt",()->yeuCauController.timYeuCauDaDuyetTheoMaLinhKien(current.getMaLK()),requests->{
            if(requests.isEmpty()){
                JOptionPane.showMessageDialog(this,"Linh kiện này chưa có yêu cầu nào đã duyệt đang chờ nhập kho.","Chưa có yêu cầu nhập kho",JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            Object[] options=requests.stream().map(r->r.getMaYeuCau()+" · "+r.getSoLuongYeuCau()+" "+r.getLinhKien().getDonViTinh()+" · "+r.getNhanVienLap().getHoTen()).toArray();
            Object choice=JOptionPane.showInputDialog(this,"Chọn yêu cầu đã duyệt cần xác nhận nhập kho:","Chọn yêu cầu nhập kho",JOptionPane.QUESTION_MESSAGE,null,options,options[0]);
            if(choice==null)return;
            int index=java.util.Arrays.asList(options).indexOf(choice);
            if(index<0)return;
            XacNhanNhapKhoDialog dialog=new XacNhanNhapKhoDialog(SwingUtilities.getWindowAncestor(this),requests.get(index),yeuCauController);
            dialog.setVisible(true);
            if(dialog.isConfirmed()){txtTimKiem.setText(current.getMaLK());timKiem();}
        });
    }

    private <T> void runTask(String errorTitle,Task<T> task,java.util.function.Consumer<T> success){
        if(dangXuLy)return;setBusy(true);clearError();
        new SwingWorker<T,Void>(){
            @Override protected T doInBackground()throws Exception{return task.run();}
            @Override protected void done(){try{T value=get();setBusy(false);success.accept(value);}catch(Exception ex){setBusy(false);showWorkerError(errorTitle,ex);}}
        }.execute();
    }

    private void setBusy(boolean busy){
        dangXuLy=busy;txtTimKiem.setEnabled(!busy);cboKinhDoanh.setEnabled(!busy);cboTonKho.setEnabled(!busy);btnTim.setEnabled(!busy);
        btnLamMoi.setEnabled(!busy);btnThem.setEnabled(!busy);if(btnTimAction!=null)btnTimAction.setEnabled(!busy);capNhatNut();lblStatus.setText(busy?"Đang xử lý…":(daTimKiem?ketQua.size()+" linh kiện":""));
    }
    private void capNhatNut(){LinhKien current=selected();boolean enabled=current!=null&&!dangXuLy;btnSua.setEnabled(enabled);btnXoa.setEnabled(enabled);btnTrangThai.setEnabled(enabled);btnNhapKho.setEnabled(enabled);btnTrangThai.setText(current!=null&&!current.isDangKinhDoanh()?"Kinh doanh lại":"Ngừng kinh doanh");btnTrangThai.setForeground(current!=null&&!current.isDangKinhDoanh()?PRIMARY:RED);}
    private void showWorkerError(String title,Exception ex){Throwable cause=ex instanceof ExecutionException&&ex.getCause()!=null?ex.getCause():ex;showError(cause.getMessage()==null?"Hãy thử lại hoặc kiểm tra kết nối cơ sở dữ liệu.":cause.getMessage());lblStatus.setText(title);}
    private void showError(String text){lblError.setText(text==null?"Có lỗi khi xử lý linh kiện.":text);}private void clearError(){lblError.setText("");}

    private static JButton button(String text,boolean primary){
        JButton b=new MainFrame.RoundedButton(text,10,primary?PRIMARY:WHITE,primary?new Color(0x1D4ED8):new Color(0xF1F5F9),primary?PRIMARY:BORDER);
        b.setFont(new Font("SansSerif",Font.BOLD,13));boolean danger=text.equals("Xóa")||text.startsWith("Ngừng");b.setForeground(primary?WHITE:(danger?RED:BODY));
        int width=switch(text){case "Làm mới"->92;case "Thêm"->70;case "Sửa","Xóa"->72;case "Ngừng kinh doanh","Xác nhận nhập kho"->150;default->94;};b.setPreferredSize(new Dimension(width,42));return b;
    }
    private static JPanel cardPanel(){return new MainFrame.RoundedPanel(WHITE,12,BORDER,new Insets(14,16,14,16));}
    private static JLabel label(String text,int size,Color color,int style){JLabel l=new JLabel(text);l.setFont(new Font("SansSerif",style,size));l.setForeground(color);return l;}
    private static Component leftGap(int height){JComponent c=(JComponent)Box.createRigidArea(new Dimension(0,height));c.setAlignmentX(Component.LEFT_ALIGNMENT);return c;}
    private static class RoundedBorder extends javax.swing.border.AbstractBorder{
        private final Color color;private final int arc;RoundedBorder(Color color,int arc){this.color=color;this.arc=arc;}
        @Override public void paintBorder(Component c,Graphics g,int x,int y,int w,int h){Graphics2D g2=(Graphics2D)g.create();g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g2.setColor(color);g2.drawRoundRect(x,y,w-1,h-1,arc,arc);g2.dispose();}
        @Override public Insets getBorderInsets(Component c){return new Insets(1,1,1,1);}
    }
    @FunctionalInterface private interface Task<T>{T run()throws Exception;}
    private static class LengthFilter extends DocumentFilter{
        private final int max;LengthFilter(int max){this.max=max;}
        @Override public void insertString(FilterBypass fb,int offset,String text,AttributeSet attr)throws BadLocationException{if(text!=null&&fb.getDocument().getLength()+text.length()<=max)super.insertString(fb,offset,text,attr);}
        @Override public void replace(FilterBypass fb,int offset,int length,String text,AttributeSet attrs)throws BadLocationException{int current=fb.getDocument().getLength(),next=current-length+(text==null?0:text.length());if(next<=max||(current>max&&next<current))super.replace(fb,offset,length,text,attrs);}
    }

    private class LinhKienFormDialog extends JDialog{
        private final LinhKienController formController;private final LinhKien existing;private final boolean adding;
        private final JTextField txtTen=new JTextField();private final JComboBox<String> cboDonVi=new JComboBox<>(new String[]{"Cái","Bộ","Lít","Chai","Bình","Gói","Chiếc"});
        private final JSpinner spnDonGia=new JSpinner(new SpinnerNumberModel(150000d,1d,9999999999d,1000d));
        private final JSpinner spnTon=new JSpinner(new SpinnerNumberModel(0,0,Integer.MAX_VALUE,1));
        private final JCheckBox chkKinhDoanh=new JCheckBox("Đang kinh doanh",true);private final JLabel lblError=label("",12,RED,Font.PLAIN);
        private final JButton btnSave=button("Lưu",true);private final JButton btnCancel=button("Hủy",false);private LinhKien saved;private boolean saving;
        LinhKienFormDialog(Window owner,LinhKienController controller,LinhKien existing,String code){
            super(owner,existing==null?"CarZone — Thêm linh kiện":"CarZone — Sửa linh kiện",ModalityType.APPLICATION_MODAL);
            this.formController=controller;this.existing=existing;this.adding=existing==null;setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            setContentPane(buildForm(code));pack();setSize(680,550);setMinimumSize(new Dimension(620,510));setLocationRelativeTo(owner);bind();SwingUtilities.invokeLater(()->txtTen.requestFocusInWindow());
        }
        private JComponent buildForm(String code){
            JPanel base=new JPanel(new BorderLayout());base.setBackground(BG);JPanel center=new JPanel(new BorderLayout(0,14));center.setOpaque(false);center.setBorder(new EmptyBorder(22,24,14,24));
            center.add(label(adding?"📦  Thêm linh kiện":"📦  Sửa linh kiện",21,TEXT,Font.BOLD),BorderLayout.NORTH);
            JPanel fields=new JPanel(new GridBagLayout());fields.setOpaque(false);GridBagConstraints g=new GridBagConstraints();g.insets=new Insets(0,0,12,12);g.anchor=GridBagConstraints.WEST;g.fill=GridBagConstraints.HORIZONTAL;
            JTextField codeField=new JTextField(code);codeField.setEditable(false);codeField.setForeground(SUBTLE);codeField.setBackground(new Color(0xF4F7FB));
            addField(fields,g,"Mã linh kiện",codeField,0,0,1);
            addField(fields,g,"Tên linh kiện *",txtTen,0,1,2);
            cboDonVi.setEditable(true);addField(fields,g,"Đơn vị tính *",cboDonVi,0,2,1);
            ((JSpinner.NumberEditor)spnDonGia.getEditor()).getFormat().setGroupingUsed(true);((JSpinner.NumberEditor)spnDonGia.getEditor()).getFormat().setMaximumFractionDigits(0);
            UIInputRules.restrictIntegerSpinner(spnDonGia);UIInputRules.restrictIntegerSpinner(spnTon);
            addField(fields,g,"Đơn giá bán * (VNĐ)",spnDonGia,1,2,1);
            addField(fields,g,adding?"Tồn ban đầu * (≥ 0)":"Tồn kho hiện tại",spnTon,0,3,1);
            if(!adding)spnTon.setEnabled(false);
            chkKinhDoanh.setOpaque(false);chkKinhDoanh.setFont(new Font("SansSerif",Font.PLAIN,13));g.gridx=0;g.gridy=4;g.gridwidth=2;g.weightx=1;g.insets=new Insets(0,0,0,0);fields.add(chkKinhDoanh,g);
            if(!adding)populate();else spnTon.setValue(0);
            setLength(txtTen,100);
            JPanel content=new JPanel(new BorderLayout(0,8));content.setOpaque(false);content.add(fields,BorderLayout.CENTER);content.add(lblError,BorderLayout.SOUTH);center.add(content,BorderLayout.CENTER);base.add(center,BorderLayout.CENTER);
            JPanel footer=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,12));footer.setBackground(WHITE);footer.setBorder(BorderFactory.createMatteBorder(1,0,0,0,BORDER));footer.add(btnSave);footer.add(btnCancel);base.add(footer,BorderLayout.SOUTH);return base;
        }
        private void addField(JPanel panel,GridBagConstraints base,String title,JComponent field,int x,int y,int width){
            JPanel wrapper=new JPanel(new BorderLayout(0,6));wrapper.setOpaque(false);wrapper.add(label(title,12,BODY,Font.PLAIN),BorderLayout.NORTH);field.setFont(new Font("SansSerif",Font.PLAIN,13));
            field.setPreferredSize(new Dimension(230,42));field.setBorder(BorderFactory.createCompoundBorder(new RoundedBorder(new Color(0xC9D5E5),10),new EmptyBorder(0,10,0,10)));
            GridBagConstraints g=(GridBagConstraints)base.clone();g.gridx=x;g.gridy=y;g.gridwidth=width;g.weightx=width==2?1:0.5;g.insets=new Insets(0,0,12,12);wrapper.add(field,BorderLayout.CENTER);panel.add(wrapper,g);
        }
        private void populate(){txtTen.setText(existing.getTenLK());cboDonVi.setSelectedItem(existing.getDonViTinh());spnDonGia.setValue(existing.getDonGia());spnTon.setValue(existing.getSoLuongTon());chkKinhDoanh.setSelected(existing.isDangKinhDoanh());}
        private void bind(){
            btnSave.addActionListener(e->save());btnCancel.addActionListener(e->dispose());txtTen.addActionListener(e->cboDonVi.requestFocusInWindow());
            spnDonGia.addKeyListener(new KeyAdapter(){@Override public void keyPressed(KeyEvent e){if(e.getKeyCode()==KeyEvent.VK_ENTER)spnTon.requestFocusInWindow();}});
            spnTon.addKeyListener(new KeyAdapter(){@Override public void keyPressed(KeyEvent e){if(e.getKeyCode()==KeyEvent.VK_ENTER)chkKinhDoanh.requestFocusInWindow();}});
            DocumentListener listener=new DocumentListener(){public void insertUpdate(DocumentEvent e){lblError.setText("");}public void removeUpdate(DocumentEvent e){lblError.setText("");}public void changedUpdate(DocumentEvent e){lblError.setText("");}};
            txtTen.getDocument().addDocumentListener(listener);
        }
        private void save(){
            if(saving)return;String name=txtTen.getText().trim();Object unitObj=cboDonVi.getEditor().getItem();String unit=unitObj==null?"":unitObj.toString().trim();
            double price;int initialStock;
            try{spnDonGia.commitEdit();price=((Number)spnDonGia.getValue()).doubleValue();spnTon.commitEdit();initialStock=((Number)spnTon.getValue()).intValue();}
            catch(java.text.ParseException ex){showError("Đơn giá hoặc số lượng tồn không hợp lệ.");return;}
            if(name.isEmpty()){showError("Tên linh kiện không được để trống.");txtTen.requestFocusInWindow();return;}
            if(!UIInputRules.isCatalogName(name)){showError("Tên linh kiện chỉ gồm chữ, số, khoảng trắng và dấu câu thông dụng (tối đa 100 ký tự).");txtTen.requestFocusInWindow();return;}
            if(unit.isEmpty()){showError("Đơn vị tính không được để trống.");cboDonVi.requestFocusInWindow();return;}
            if(!UIInputRules.isUnit(unit)){showError("Đơn vị tính chỉ gồm chữ cái, tối đa 20 ký tự.");cboDonVi.requestFocusInWindow();return;}
            if(price<=0){showError("Đơn giá phải lớn hơn 0.");spnDonGia.requestFocusInWindow();return;}
            if(initialStock<0){showError("Tồn ban đầu không được âm.");spnTon.requestFocusInWindow();return;}
            boolean active=chkKinhDoanh.isSelected();setSaving(true);
            new SwingWorker<LinhKien,Void>(){
                @Override protected LinhKien doInBackground(){return adding?formController.them(name,price,unit,initialStock,active):formController.capNhat(existing.getMaLK(),name,price,unit,active);}
                @Override protected void done(){try{saved=get();dispose();}catch(Exception ex){setSaving(false);Throwable cause=ex.getCause()==null?ex:ex.getCause();showError(cause.getMessage());}}
            }.execute();
        }
        private void setSaving(boolean value){saving=value;btnSave.setEnabled(!value);btnCancel.setEnabled(!value);txtTen.setEnabled(!value);cboDonVi.setEnabled(!value);spnDonGia.setEnabled(!value);if(adding)spnTon.setEnabled(!value);chkKinhDoanh.setEnabled(!value);btnSave.setText(value?"Đang lưu…":"Lưu");}
        private void showError(String text){lblError.setText(text==null?"Không thể lưu linh kiện.":text);}
        private void setLength(JTextField field,int max){((AbstractDocument)field.getDocument()).setDocumentFilter(new LengthFilter(max));}
    }
}
