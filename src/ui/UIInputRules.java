package ui;

import javax.swing.JSpinner;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.regex.Pattern;

/** Các mẫu định dạng dùng chung cho validate sớm tại giao diện. */
final class UIInputRules {
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

    static boolean isSearch(String value) { return value != null && SEARCH.matcher(value).matches(); }
    static boolean isCatalogName(String value) { return value != null && CATALOG_NAME.matcher(value).matches(); }
    static boolean isPromotionName(String value) { return value != null && PROMOTION_NAME.matcher(value).matches(); }
    static boolean isPersonName(String value) { return value != null && PERSON_NAME.matcher(value).matches(); }
    static boolean isUnit(String value) { return value != null && UNIT.matcher(value).matches(); }
    static boolean isPhone(String value) { return value != null && PHONE.matcher(value).matches(); }
    static boolean isCccd(String value) { return value != null && CCCD.matcher(value).matches(); }
    static boolean isAddress(String value) { return value != null && ADDRESS.matcher(value).matches(); }
    static boolean isDateText(String value) { return value != null && DATE_TEXT.matcher(value.trim()).matches(); }
    static boolean isMultilineText(String value) { return value != null && MULTILINE_TEXT.matcher(value).matches(); }

    static LocalDate parseDate(String value) {
        if (!isDateText(value)) throw new DateTimeParseException("Ngày phải theo định dạng dd/MM/yyyy.", String.valueOf(value), 0);
        return LocalDate.parse(value.trim(), DATE);
    }

    static LocalDate readDate(JSpinner spinner) {
        if (!(spinner.getEditor() instanceof JSpinner.DateEditor editor)) {
            throw new DateTimeParseException("Ngày phải theo định dạng dd/MM/yyyy.", "", 0);
        }
        return parseDate(editor.getTextField().getText());
    }
}
