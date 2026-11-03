# Workflow — review thay đổi trước khi báo xong

Dùng sau khi code xong, trước khi commit/merge hoặc trước khi nói "đã hoàn thành".

## 1. Xác định phạm vi

- Liệt kê file đã thay đổi.
- Đối chiếu với UC/mục tiêu ban đầu.
- Phát hiện file sửa ngoài phạm vi.

Không review bằng cảm nhận. Đọc diff thật.

## 2. Kiểm tra biên dịch

```powershell
mvn -q compile
```

Nếu fail: dừng, sửa, chạy lại. Không báo xong khi chưa biên dịch.

Nếu có test:

```powershell
mvn test
```

Nếu chưa cấu hình surefire JUnit 5, ghi rõ và không tuyên bố test đã chạy.

## 3. Kiểm tra kiến trúc

- [ ] UI không có SQL, không gọi `DbUtil`, không xử lý nghiệp vụ.
- [ ] Controller không có SQL; chỉ điều phối DAO, kiểm quyền, validate, dịch lỗi.
- [ ] DAO interface không có SQL.
- [ ] DAO impl dùng `PreparedStatement`, `try-with-resources`.
- [ ] Không thêm service layer, Hibernate/JPA, thư viện mới.
- [ ] Entity vẫn là POJO; ngày/tiền theo đúng kiểu hiện có (`Date`/`LocalDate`, `double`).
- [ ] Không hard-code mã, mật khẩu, chuỗi kết nối.

## 4. Kiểm tra quyền

- [ ] Vai trò lấy từ `UserSession`, không từ tham số UI.
- [ ] Menu/nút ẩn đúng vai trò.
- [ ] Controller chặn lại, không chỉ ẩn nút.
- [ ] Dữ liệu TNV/KTV được lọc theo `maNV`.
- [ ] Không có nhân viên mặc định/fallback khi chưa đăng nhập.
- [ ] DB trigger/procedure vẫn là chốt cuối.

## 5. Kiểm tra nghiệp vụ

- [ ] Trạng thái đi đúng máy trạng thái.
- [ ] Guard tiền/khuyến mại/tồn kho/7 ngày/1 hóa đơn được giữ.
- [ ] Không tự sinh mã ở Java.
- [ ] Không tự trừ/cộng tồn kho ở Java.
- [ ] Không tính lại tiền/thuế/khuyến mại ở Java.
- [ ] Phiếu `HOAN_TAT`, hóa đơn `DA_THANH_TOAN` vẫn bất biến.
- [ ] Lỗi DB 50xxx được giữ nguyên nhân và dịch thông báo.

## 6. Kiểm tra dữ liệu và transaction

- [ ] Câu SQL dùng đúng tên cột thật.
- [ ] `Connection`/`PreparedStatement`/`ResultSet` được đóng.
- [ ] Nhiều bước ghi dùng một transaction hoặc một procedure.
- [ ] Có rollback khi lỗi.
- [ ] Không giữ transaction trong lúc chờ UI.
- [ ] Không N+1 query trong bảng lớn.

Đặc biệt kiểm tra các cột từng bị sai: `bienSoXe` (không phải `maXe`), `maNVKyThuat` (không phải `maNVSuaChua`).

## 7. Kiểm tra UI/thread

- [ ] Tác vụ DB chạy ngoài EDT (`SwingWorker`).
- [ ] Nút bị khóa khi đang chạy.
- [ ] Có trạng thái loading/rỗng/lỗi.
- [ ] Không gọi `JOptionPane` từ luồng nền.
- [ ] Focus/Enter/clear lỗi hoạt động.
- [ ] Bố cục theo Figma, không tự sáng tác.
- [ ] Bảng dùng model chỉ đọc nếu chỉ hiển thị.

## 8. Kiểm tra bảo mật

- [ ] Không log mật khẩu/token/dữ liệu nhạy cảm.
- [ ] Không trả mật khẩu trong `toString`.
- [ ] Không nối chuỗi SQL bằng dữ liệu người dùng.
- [ ] Không mở quyền CCH cho vai trò khác.
- [ ] Không đưa `.env`/bí mật mới vào tài liệu.

## 9. Kiểm tra test

- [ ] Có test cho luồng chính.
- [ ] Có test cho guard quan trọng.
- [ ] Có test biên.
- [ ] Có test quyền.
- [ ] Test chạy được hoặc có lý do chưa chạy.
- [ ] Không test phụ thuộc thứ tự/ dữ liệu còn sót.

## 10. Kiểm tra tài liệu

- [ ] `USECASE_SKILL_TRACEABILITY.md` cập nhật nếu UC đã xong.
- [ ] `KNOWN_DISCREPANCIES.md` cập nhật nếu phát hiện lệch mới.
- [ ] Không sửa migration cũ.
- [ ] Không sửa quyết định nghiệp vụ mà không hỏi.

## 11. Kết luận

Báo cáo:

```text
Kết luận: PASS / PASS có điều kiện / FAIL
Bằng chứng: lệnh đã chạy, kết quả
Đã kiểm: ...
Chưa kiểm: ... (lý do)
Rủi ro còn lại: ...
Việc phải làm trước khi merge: ...
```

Chỉ nói "hoàn thành" khi:

- Biên dịch pass.
- Không còn FAIL mức chặn.
- Các mục "chưa kiểm" đã được nêu rõ, không che giấu.
