package enums;

/**
 * Phân loại các hành vi phi lý (Behavioral Anomalies) cần kích hoạt mã PIN bảo mật cấp 2.
 */
public enum LoaiHanhViBatThuong {

    /**
     * Bất thường về tốc độ & số lượng: Xóa từ 3 bản ghi cốt lõi trở lên trong < 2 giây.
     */
    MASS_DELETE("Xóa hàng loạt dữ liệu cốt lõi (Mass Delete) trong thời gian cực ngắn"),

    /**
     * Bất thường về tốc độ & số lượng: Sửa đổi hàng loạt tài khoản/quyền hạn hoặc lệnh DDL phá hủy cấu trúc.
     */
    MASS_UPDATE_OR_DDL("Sửa đổi cấu trúc / cập nhật trạng thái diện rộng trái phép (Mass Update / DDL)"),

    /**
     * Bất thường về thời gian: Thao tác nhạy cảm vào khung giờ đóng cửa hoặc nửa đêm (00:00 - 05:00).
     */
    OFF_HOURS_SENSITIVE_ACTION("Thao tác dữ liệu nhạy cảm ngoài giờ làm việc (00:00 - 05:00 sáng)"),

    /**
     * Bất thường về phạm vi: Cố gắng can thiệp cấu hình hệ thống, bảo mật hoặc chuỗi kết nối DB.
     */
    ACCESS_HIGH_SENSITIVITY("Truy xuất / can thiệp khu vực dữ liệu bảo mật cấp cao"),

    /**
     * Bất thường về liên kết khóa ngoại: Xóa chủ thể cốt lõi (như Chủ cửa hàng) gây sụp đổ bảng con.
     */
    CASCADING_DANGER_DELETE("Xóa chủ thể quan trọng kéo theo rủi ro phá hủy dây chuyền (Cascading Danger)");

    private final String moTa;

    LoaiHanhViBatThuong(String moTa) {
        this.moTa = moTa;
    }

    public String getMoTa() {
        return moTa;
    }
}

