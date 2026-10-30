---
name: verify-business-rules
description: Dùng sau khi sửa entity/dao/controller hoặc migration của CarZone để kiểm tra vẫn tuân thủ luật nghiệp vụ (trạng thái, tồn kho, hóa đơn, thuế, khuyến mãi, đổi trả, phân quyền). Chạy trước khi báo xong.
---

# Kiểm tra luật nghiệp vụ

Đọc `ai-context/business-rules.md`. Với phần vừa đổi, trả lời từng mục liên quan, có bằng chứng (file:dòng, kết quả test, hoặc kết quả chạy SQL), không trả lời theo trí nhớ:

- [ ] Chuyển trạng thái phiếu/hóa đơn/yêu cầu nhập chỉ theo cạnh hợp lệ (DB: trigger mã 50103, 50194; Java: setter trong entity).
- [ ] Phiếu `HOAN_TAT` bị khóa; chỉ lập hóa đơn khi phiếu `HOAN_TAT` (50104, 50111, 50121, 50132); mỗi phiếu tối đa 1 hóa đơn.
- [ ] Dùng linh kiện: không vượt tồn (50123), tồn tự trừ/hoàn; cộng tồn khi yêu cầu sang `DA_NHAP_KHO`.
- [ ] Hóa đơn đã thanh toán không sửa/xóa (50141, 50142); chuyển khoản có `maGiaoDich`.
- [ ] Thuế theo nội dung hóa đơn (1,5% / 7% / 4,5%), tính trước khuyến mãi; `tongThanhToan = DV + LK − tienGiam + thue`, khớp `vw_HoaDon_TongTien`.
- [ ] Khuyến mãi: ≤ 1 trên hóa đơn, còn hiệu lực (50133), giảm đúng các dòng thuộc danh sách áp dụng; đã gắn vào hóa đơn thì không đổi (50172, 50181, 50182).
- [ ] Đổi trả: hóa đơn đã thanh toán (50152), ≤ 7 ngày (50153), số lượng không vượt đã thực hiện (50162, 50163); hoàn tiền tính vào ngày phát sinh.
- [ ] Vai trò: người lập đúng vai trò (50101, 50102, 50131, 50151, 50171, 50191, 50192); kỹ thuật viên chỉ đụng phiếu của mình.
- [ ] Mã tự sinh đúng định dạng và không trùng; mã `DV`/`LK` có ≥ 5 số.
- [ ] Đơn giá lưu theo thời điểm ghi nhận, không đọc lại giá hiện hành cho hóa đơn cũ.

## Cách kiểm
- DB: dựng sạch rồi chạy `infra/database/seed/CarZone_Demo.sql` (có sẵn bước cố ý vi phạm, ví dụ dùng vượt tồn phải ra lỗi 50123) và đối chiếu kết quả kỳ vọng ghi trong file.
- Java: `mvn -q test` nếu có test; nếu chưa có test framework hoạt động thì nói rõ chỉ kiểm bằng đọc code.
- Môi trường không có Docker/SQL Server thì mục DB ghi "chưa chạy".

Báo ngắn: mục nào đạt, lệch (kèm vị trí), hay chưa kiểm được. Không báo "xong" khi còn mục lệch mà không nói.
