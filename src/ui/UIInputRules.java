package ui;

import com.toedter.calendar.JDateChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.text.DefaultFormatterFactory;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.regex.Pattern;

/** Các mẫu định dạng dùng chung cho validate sớm tại giao diện. */
public final class UIInputRules {
    private static final Pattern SEARCH = Pattern.compile("^[\\p{L}\\p{M}\\p{N}\\p{P}\\p{Zs}]{0,150}$");
    private static final Pattern CATALOG_NAME = Pattern.compile("^[\\p{L}\\p{M}\\p{N}][\\p{L}\\p{M}\\p{N}\\s.,'’()&/+\\-]{0,99}$");
    private static final Pattern PROMOTION_NAME = Pattern.compile("^[\\p{L}\\p{M}\\p{N}][\\p{L}\\p{M}\\p{N}\\s.,'’()&/+\\-]{0,149}$");
    private static final Pattern PERSON_NAME = Pattern.compile("^[\\p{L}\\p{M}]+(?:[\\s'’\\-][\\p{L}\\p{M}]+)*$");
    private static final Pattern UNIT = Pattern.compile("^[\\p{L}\\p{M}]{1,20}$");
    private static final Pattern PHONE = Pattern.compile("^0\\d{9,10}$");
    private static final Pattern CCCD = Pattern.compile("^\\d{12}$");
    private static final Pattern ADDRESS = Pattern.compile("^[\\p{L}\\p{M}\\p{N}][\\p{L}\\p{M}\\p{N}\\s.,'’/#()\\-]{0,199}$");
    private static final Pattern DATE_TEXT = Pattern.compile("^(0[1-9]|[12]\\d|3[01])/(0[1-9]|1[0-2])/\\d{4}$");
    private static final Pattern MULTILINE_TEXT = Pattern.compile("^[\\p{L}\\p{M}\\p{N}\\p{P}\\p{Zs}\\t\\r\\n]*$");
    private static final DateTimeFormatter DATE = new DateTimeFormatterBuilder()
            .appendPattern("dd/MM/uuuu").toFormatter().withResolverStyle(ResolverStyle.STRICT);

    private UIInputRules() { }

    public static boolean isSearch(String value) { return value != null && SEARCH.matcher(value).matches(); }
    public static boolean isCatalogName(String value) { return value != null && CATALOG_NAME.matcher(value).matches(); }
    public static boolean isPromotionName(String value) { return value != null && PROMOTION_NAME.matcher(value).matches(); }
    public static boolean isPersonName(String value) { return value != null && PERSON_NAME.matcher(value).matches(); }
    public static boolean isUnit(String value) { return value != null && UNIT.matcher(value).matches(); }
    public static boolean isPhone(String value) { return value != null && PHONE.matcher(value).matches(); }
    public static boolean isCccd(String value) { return value != null && CCCD.matcher(value).matches(); }
    public static boolean isAddress(String value) { return value != null && ADDRESS.matcher(value).matches(); }
    public static boolean isDateText(String value) { return value != null && DATE_TEXT.matcher(value.trim()).matches(); }
    public static boolean isMultilineText(String value) { return value != null && MULTILINE_TEXT.matcher(value).matches(); }

    public static LocalDate parseDate(String value) {
        if (!isDateText(value)) throw new DateTimeParseException("Ngày phải theo định dạng dd/MM/yyyy.", String.valueOf(value), 0);
        return LocalDate.parse(value.trim(), DATE);
    }

    public static LocalDate readDate(JSpinner spinner) {
        if (!(spinner.getEditor() instanceof JSpinner.DateEditor editor)) {
            throw new DateTimeParseException("Ngày phải theo định dạng dd/MM/yyyy.", "", 0);
        }
        return parseDate(editor.getTextField().getText());
    }

    public static LocalDate readDate(JDateChooser chooser) {
        if (chooser == null || chooser.getDate() == null) {
            throw new DateTimeParseException("Vui lòng chọn ngày theo định dạng dd/MM/yyyy.", "", 0);
        }
        return chooser.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /** Chặn ký tự ngoài chữ số ngay khi nhập vào NumberEditor của spinner nguyên. */
    public static void restrictIntegerSpinner(JSpinner spinner) {
        if (!(spinner.getEditor() instanceof JSpinner.NumberEditor editor)) return;
        editor.getFormat().setGroupingUsed(false);
        installNumericFormatter(spinner, editor, false, 0);
    }

    /** Chặn chữ/khoảng trắng; cho phép tối đa một dấu thập phân cho spinner phần trăm/tiền lẻ. */
    public static void restrictDecimalSpinner(JSpinner spinner, int fractionDigits) {
        if (!(spinner.getEditor() instanceof JSpinner.NumberEditor editor)) return;
        DecimalFormat format = editor.getFormat();
        format.setGroupingUsed(false);
        format.setMaximumFractionDigits(fractionDigits);
        format.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.US));
        installNumericFormatter(spinner, editor, true, fractionDigits);
    }

    private static void installNumericFormatter(JSpinner spinner, JSpinner.NumberEditor editor,
            boolean decimal, int fractionDigits) {
        JFormattedTextField field = editor.getTextField();
        String pattern = decimal ? "\\d+(\\.\\d{0," + fractionDigits + "})?" : "\\d+";
        javax.swing.text.NumberFormatter formatter = new javax.swing.text.NumberFormatter(editor.getFormat()) {
            @Override public Object stringToValue(String text) throws java.text.ParseException {
                String value = text == null ? "" : text;
                if (!value.matches(pattern)) throw new java.text.ParseException(
                        decimal ? "Chỉ nhập số và tối đa " + fractionDigits + " chữ số thập phân." : "Chỉ nhập chữ số 0–9.", 0);
                return super.stringToValue(value);
            }
        };
        formatter.setValueClass(spinner.getValue().getClass());
        formatter.setAllowsInvalid(false);
        formatter.setCommitsOnValidEdit(true);
        field.setFormatterFactory(new DefaultFormatterFactory(formatter));
        field.setValue(spinner.getValue());
        field.setToolTipText(decimal ? "Chỉ nhập số và tối đa " + fractionDigits + " chữ số thập phân." : "Chỉ nhập chữ số 0–9.");
    }
}
