package ui;

import enums.VaiTro;

/**
 * TraCuuChuCuaHangUI: Màn hình tra cứu dành riêng cho Chủ cửa hàng.
 * Kế thừa từ TraCuuUI đa năng với quyền CHU_CUA_HANG (đủ 5 tab chức năng).
 */
public class TraCuuChuCuaHangUI extends TraCuuUI {
    public TraCuuChuCuaHangUI() {
        super(VaiTro.CHU_CUA_HANG);
    }
}
