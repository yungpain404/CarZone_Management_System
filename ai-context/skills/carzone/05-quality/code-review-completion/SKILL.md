---
name: code-review-completion
description: Dùng trước khi báo hoàn thành, commit hoặc merge một thay đổi CarZone. Rà soát kiến trúc, quyền, nghiệp vụ, transaction, UI, bảo mật, test và tính đầy đủ so với use case.
---

# Review và xác nhận hoàn thành

## Mục đích

Ngăn việc tuyên bố "xong" khi code chưa biên dịch, chưa đúng quyền, còn SQL sai tầng, còn mock, hoặc chưa kiểm thử.

## Quy trình

1. Xác định phạm vi thay đổi so với UC.
2. Chạy `mvn -q compile`.
3. Chạy test nếu có; nếu chưa, ghi rõ.
4. Rà soát 12 nhóm dưới đây.
5. Kết luận PASS/PASS có điều kiện/FAIL.
6. Nêu rủi ro và việc tiếp theo.

## 12 nhóm rà soát

### 1. Đúng phạm vi

- [ ] Thay đổi khớp UC/mục tiêu.
- [ ] Không sửa file ngoài phạm vi.
- [ ] Không thêm tính năng chưa được yêu cầu.

### 2. Kiến trúc

- [ ] UI không SQL/không `DbUtil`.
- [ ] Controller không SQL.
- [ ] DAO interface không SQL.
- [ ] DAO impl dùng `PreparedStatement`, `try-with-resources`.
- [ ] Không service layer/Hibernate/JPA.
- [ ] Entity vẫn POJO, dùng `Date`/`double`.

### 3. Quyền

- [ ] Lấy vai trò từ `UserSession`.
- [ ] Không nhận role/`maNV` từ form.
- [ ] TNV/KTV lọc dữ liệu của mình.
- [ ] Không có nhân viên mặc định/fallback.
- [ ] DB trigger/procedure vẫn là chốt cuối.

### 4. Nghiệp vụ

- [ ] Đúng máy trạng thái.
- [ ] Giữ bất biến phiếu `HOAN_TAT`, hóa đơn `DA_THANH_TOAN`.
- [ ] Đúng điều kiện 7 ngày, một hóa đơn/phiếu, khuyến mại hợp lệ.
- [ ] Không tự sinh mã.
- [ ] Không tự trừ/cộng tồn kho.
- [ ] Không tính lại tiền/thuế/khuyến mại.

### 5. Dữ liệu/SQL

- [ ] Tên cột đúng schema.
- [ ] Kiểu dữ liệu/parse ngày đúng.
- [ ] Không N+1.
- [ ] Có phân trang/giới hạn khi cần.
- [ ] Không nối chuỗi SQL bằng dữ liệu người dùng.
- [ ] Không dùng `SELECT *` trong code mới nếu không cần.

Đặc biệt: `bienSoXe` không phải `maXe`; `maNVKyThuat` không phải `maNVSuaChua`.

### 6. Transaction

- [ ] Nhiều bước ghi dùng một transaction/procedure.
- [ ] Có rollback.
- [ ] Không commit từng bước.
- [ ] Không giữ transaction qua thao tác UI.
- [ ] Không khóa bảng lâu.

### 7. UI/thread

- [ ] DB chạy ngoài EDT.
- [ ] Nút khóa khi chạy, bật lại trong `finally`.
- [ ] Có loading/rỗng/lỗi.
- [ ] Không dialog từ luồng nền.
- [ ] Focus/Enter/clear lỗi.
- [ ] Bố cục theo Figma.
- [ ] Bảng chỉ đọc khi chỉ hiển thị.

### 8. Bảo mật

- [ ] Không log mật khẩu/hash/dữ liệu nhạy cảm.
- [ ] Không trả mật khẩu trong `toString`.
- [ ] Không hard-code bí mật.
- [ ] Không mở quyền CCH cho vai trò khác.
- [ ] Không đưa `.env`/bí mật mới vào tài liệu.

### 9. Lỗi

- [ ] Giữ nguyên nhân `SQLException`.
- [ ] Dịch mã 50xxx đúng.
- [ ] Không nuốt exception.
- [ ] Thông báo tiếng Việt, không stack trace cho người dùng.

### 10. Test

- [ ] Có test luồng chính.
- [ ] Có test guard quan trọng.
- [ ] Có test biên.
- [ ] Có test quyền.
- [ ] Đã chạy hoặc nêu rõ chưa chạy.

### 11. Tài liệu

- [ ] Cập nhật `USECASE_SKILL_TRACEABILITY.md` nếu UC xong.
- [ ] Cập nhật `KNOWN_DISCREPANCIES.md` nếu có lệch mới.
- [ ] Không sửa migration cũ.
- [ ] Không sửa quyết định nghiệp vụ mà không hỏi.

### 12. Vệ sinh mã

- [ ] Không TODO/FIXME che lỗi.
- [ ] Không code chết.
- [ ] Không import thừa.
- [ ] Không file tạm.
- [ ] Không thay đổi định dạng ngoài phạm vi.

## Nợ kỹ thuật phải kiểm tra nếu chạm

- `DashboardController` sai cột + SQL trực tiếp.
- `MainFrame` và `DashboardUI` trùng nhau.
- `taoNhanVienMacDinh()` trong UI.
- SQL trực tiếp trong `DashboardUI`.
- `DbUtil` hard-code `sa`.
- SHA-256 không salt.
- `PROGRESS.md` lỗi thời.
- Chưa có surefire JUnit 5.

Không tự sửa toàn bộ nợ trong một thay đổi nhỏ; ghi lại và tách việc.

## Mẫu kết luận

```text
Kết luận: PASS / PASS có điều kiện / FAIL
Đã chạy: mvn -q compile; mvn test; ...
Đã kiểm: ...
Chưa kiểm: ... (lý do)
Rủi ro còn lại: ...
Việc trước khi merge: ...
```

## Quy tắc kết luận

- `PASS` chỉ khi biên dịch pass, không còn FAIL chặn, test/acceptance cốt lõi đã chạy.
- `PASS có điều kiện` khi còn việc nhỏ không chặn, đã ghi rõ.
- `FAIL` khi còn lỗi quyền/nghiệp vụ/dữ liệu chưa xử lý.
- Không dùng "chắc là chạy được".
- Không nói "đã test" nếu chưa chạy.

## Liên quan

- `workflows/review-change.md`
- `05-quality/testing-acceptance`
- `KNOWN_DISCREPANCIES.md`
- `ROLE_USECASE_MATRIX.md`
