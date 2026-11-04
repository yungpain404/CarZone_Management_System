package ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static ui.MainFrame.*;

/**
 * Component chọn ngày (Date Picker) dạng thanh nhập kèm nút lịch popup.
 * Không phụ thuộc thư viện ngoài, tương thích chuẩn giao diện FlatLaf và bảng màu CarZone.
 */
public class DatePickerField extends JPanel {

    private final SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM/yyyy");
    private final JTextField txtDate;
    private final JButton btnCalendar;
    private Date selectedDate;
    private Runnable onDateChanged;

    public DatePickerField() {
        this(new Date());
    }

    public DatePickerField(Date initialDate) {
        setLayout(new BorderLayout(2, 0));
        setOpaque(false);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_INPUT_STROKE, 1),
                new EmptyBorder(1, 4, 1, 1)
        ));
        setBackground(C_WHITE);

        txtDate = new JTextField(8);
        txtDate.setFont(f(Font.PLAIN, 13));
        txtDate.setBorder(null);
        txtDate.setOpaque(false);
        txtDate.addActionListener(e -> parseFromText());
        txtDate.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                parseFromText();
            }
        });
        add(txtDate, BorderLayout.CENTER);

        btnCalendar = new JButton("📅");
        btnCalendar.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 12));
        btnCalendar.setFocusPainted(false);
        btnCalendar.setBorderPainted(false);
        btnCalendar.setContentAreaFilled(false);
        btnCalendar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCalendar.setToolTipText("Chọn ngày từ lịch");
        btnCalendar.addActionListener(e -> hienThiCalendarPopup());
        add(btnCalendar, BorderLayout.EAST);

        setDate(initialDate);
    }

    public Date getDate() {
        parseFromText();
        return selectedDate;
    }

    public void setDate(Date date) {
        this.selectedDate = date;
        if (date != null) {
            txtDate.setText(dateFmt.format(date));
        } else {
            txtDate.setText("");
        }
    }

    public String getText() {
        return txtDate.getText().trim();
    }

    public void setText(String text) {
        txtDate.setText(text);
        parseFromText();
    }

    public void setOnDateChanged(Runnable onDateChanged) {
        this.onDateChanged = onDateChanged;
    }

    private void parseFromText() {
        String str = txtDate.getText().trim();
        if (str.isEmpty()) {
            selectedDate = null;
            return;
        }
        try {
            selectedDate = dateFmt.parse(str);
        } catch (Exception ignored) {
            // Giữ nguyên text hoặc format lại nếu parse lỗi
        }
    }

    private void hienThiCalendarPopup() {
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
        popup.setBackground(C_WHITE);

        Calendar cal = Calendar.getInstance();
        if (selectedDate != null) {
            cal.setTime(selectedDate);
        }

        JPanel pnlCalendar = new CalendarPopupPanel(cal, selectedDate, date -> {
            setDate(date);
            popup.setVisible(false);
            if (onDateChanged != null) {
                onDateChanged.run();
            }
        });

        popup.add(pnlCalendar);
        popup.show(this, 0, getHeight() + 2);
    }

    /**
     * Bảng lịch mini hiển thị các ngày trong tháng.
     */
    private static class CalendarPopupPanel extends JPanel {
        private final Calendar currentCal;
        private final Date initialSelected;
        private final java.util.function.Consumer<Date> onSelect;
        private final SimpleDateFormat monthYearFmt = new SimpleDateFormat("'Tháng' MM, yyyy");

        public CalendarPopupPanel(Calendar cal, Date initialSelected, java.util.function.Consumer<Date> onSelect) {
            this.currentCal = (Calendar) cal.clone();
            this.initialSelected = initialSelected;
            this.onSelect = onSelect;

            setLayout(new BorderLayout(0, 6));
            setBackground(C_WHITE);
            setBorder(new EmptyBorder(8, 10, 8, 10));
            setPreferredSize(new Dimension(240, 220));

            buildUI();
        }

        private void buildUI() {
            removeAll();

            // Header: < Tháng MM, yyyy >
            JPanel header = new JPanel(new BorderLayout(4, 0));
            header.setOpaque(false);

            JButton btnPrev = new JButton("◀");
            btnPrev.setFont(f(Font.BOLD, 10));
            btnPrev.setMargin(new Insets(2, 6, 2, 6));
            btnPrev.setFocusPainted(false);
            btnPrev.addActionListener(e -> {
                currentCal.add(Calendar.MONTH, -1);
                buildUI();
                revalidate();
                repaint();
            });

            JLabel lblMonthYear = new JLabel(monthYearFmt.format(currentCal.getTime()), SwingConstants.CENTER);
            lblMonthYear.setFont(f(Font.BOLD, 13));
            lblMonthYear.setForeground(C_TEXT_HEAD);

            JButton btnNext = new JButton("▶");
            btnNext.setFont(f(Font.BOLD, 10));
            btnNext.setMargin(new Insets(2, 6, 2, 6));
            btnNext.setFocusPainted(false);
            btnNext.addActionListener(e -> {
                currentCal.add(Calendar.MONTH, 1);
                buildUI();
                revalidate();
                repaint();
            });

            header.add(btnPrev, BorderLayout.WEST);
            header.add(lblMonthYear, BorderLayout.CENTER);
            header.add(btnNext, BorderLayout.EAST);
            add(header, BorderLayout.NORTH);

            // Days Grid
            JPanel daysGrid = new JPanel(new GridLayout(7, 7, 2, 2));
            daysGrid.setOpaque(false);

            String[] dayNames = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
            for (String dn : dayNames) {
                JLabel lblD = new JLabel(dn, SwingConstants.CENTER);
                lblD.setFont(f(Font.BOLD, 11));
                lblD.setForeground(C_TEXT_SUBTLE);
                daysGrid.add(lblD);
            }

            Calendar temp = (Calendar) currentCal.clone();
            temp.set(Calendar.DAY_OF_MONTH, 1);
            int firstDayOfWeek = temp.get(Calendar.DAY_OF_WEEK); // 1 = Chủ nhật, 2 = Thứ 2...
            // Đưa về Thứ 2 là 0, CN là 6
            int emptyCols = (firstDayOfWeek + 5) % 7;

            for (int i = 0; i < emptyCols; i++) {
                daysGrid.add(new JLabel(""));
            }

            int maxDay = temp.getActualMaximum(Calendar.DAY_OF_MONTH);
            Calendar today = Calendar.getInstance();

            for (int day = 1; day <= maxDay; day++) {
                final int currentDay = day;
                temp.set(Calendar.DAY_OF_MONTH, day);
                boolean isToday = isSameDay(temp, today);
                boolean isSelected = initialSelected != null && isSameDay(temp, initialSelected);

                JButton btnDay = new JButton(String.valueOf(day));
                btnDay.setFont(f(isSelected ? Font.BOLD : Font.PLAIN, 11));
                btnDay.setMargin(new Insets(1, 1, 1, 1));
                btnDay.setFocusPainted(false);
                btnDay.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                if (isSelected) {
                    btnDay.setBackground(C_PRIMARY);
                    btnDay.setForeground(C_WHITE);
                } else if (isToday) {
                    btnDay.setBackground(C_PRIMARY_BG);
                    btnDay.setForeground(C_PRIMARY);
                    btnDay.setBorder(BorderFactory.createLineBorder(C_PRIMARY, 1));
                } else {
                    btnDay.setBackground(C_WHITE);
                    btnDay.setForeground(C_TEXT_MAIN);
                    btnDay.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
                }

                btnDay.addActionListener(e -> {
                    Calendar selected = (Calendar) currentCal.clone();
                    selected.set(Calendar.DAY_OF_MONTH, currentDay);
                    onSelect.accept(selected.getTime());
                });

                daysGrid.add(btnDay);
            }

            // Fill empty cells at the end
            int totalCells = emptyCols + maxDay;
            int remaining = 42 - totalCells;
            for (int i = 0; i < remaining && totalCells + i < 42; i++) {
                daysGrid.add(new JLabel(""));
            }

            add(daysGrid, BorderLayout.CENTER);

            // Footer: Nút Hôm nay
            JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 2));
            footer.setOpaque(false);
            JButton btnToday = new JButton("Hôm nay");
            btnToday.setFont(f(Font.PLAIN, 11));
            btnToday.setForeground(C_PRIMARY);
            btnToday.setMargin(new Insets(2, 8, 2, 8));
            btnToday.setFocusPainted(false);
            btnToday.addActionListener(e -> onSelect.accept(new Date()));
            footer.add(btnToday);

            add(footer, BorderLayout.SOUTH);
        }

        private boolean isSameDay(Calendar c1, Calendar c2) {
            return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                   c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
        }

        private boolean isSameDay(Calendar c, Date d) {
            Calendar c2 = Calendar.getInstance();
            c2.setTime(d);
            return isSameDay(c, c2);
        }
    }
}
