package audit;

import enums.LoaiHanhViBatThuong;

import java.util.Date;

/**
 * Ghi nhận thông tin chi tiết một hành vi phi lý phát hiện được.
 */
public record ThongTinViPham(
        LoaiHanhViBatThuong loai,
        String chiTiet,
        String tenBang,
        String cauLenh,
        String nguoiThucHien,
        Date thoiDiem
) {
    public ThongTinViPham(LoaiHanhViBatThuong loai, String chiTiet, String tenBang, String cauLenh, String nguoiThucHien) {
        this(loai, chiTiet, tenBang, cauLenh, nguoiThucHien, new Date());
    }
}

