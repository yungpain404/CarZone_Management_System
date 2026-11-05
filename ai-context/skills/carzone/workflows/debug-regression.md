# Workflow — debug và chống hồi quy

Dùng khi có lỗi chức năng, dữ liệu sai, màn hình trống, ngoại lệ JDBC hoặc hành vi khác mô tả.

## 1. Tái hiện

Ghi rõ:

- Vai trò đang đăng nhập và `maNV`.
- UC/thao tác.
- Dữ liệu đầu vào (mã phiếu, biển số, mã HĐ...).
- Kết quả mong đợi và kết quả thực tế.
- Ảnh/log/ngoại lệ nếu có.

Không sửa code khi chưa tái hiện được hoặc chưa hiểu luồng.

## 2. Khoanh vùng tầng

Đi theo luồng:

```text
UI -> controller -> DAO interface -> DAO impl -> SQL/procedure/trigger -> DB
```

Câu hỏi:

- Lỗi ở hiển thị hay dữ liệu?
- Controller có bị gọi không?
- DAO có chạy không?
- SQL có trả dòng nào không?
- Trigger/procedure có chặn không?

Nếu dashboard trống: kiểm tra tên cột trước. Các lỗi đã biết: `maXe`, `maNVSuaChua` (đúng là `bienSoXe`, `maNVKyThuat`).

## 3. Kiểm tra session/quyền

- `UserSession.daDangNhap()` có đúng không?
- `getVaiTro()` và `getMaNV()` có đúng người không?
- Có đang dùng nhân viên mặc định `CCH001` không?
- Query có lọc `maNV` không?
- UI có ẩn nút nhưng controller vẫn cho qua không?

## 4. Kiểm tra DB

- Kết nối: `docker compose -f infra/docker/compose.yml ps`.
- Flyway đã chạy V1/V2 chưa.
- Bảng/cột có đúng schema không.
- Trigger/procedure có tồn tại không.
- Chạy câu SQL tương đương trong SSMS/Azure Data Studio.
- Kiểm tra mã lỗi 50xxx và thông điệp gốc.

Không sửa dữ liệu thật để "che" lỗi. Nếu cần, dùng DB local/seed.

## 5. Kiểm tra transaction và tồn kho

- Có hai lần trừ/cộng tồn kho không?
- Trigger `trg_CTLK_KiemTra` và `trg_PYCNLK_KiemTra` có chạy hai lần do update lặp không?
- Có `autoCommit` sai không?
- Có rollback khi một bước fail không?
- Có giữ connection quá lâu hoặc rò rỉ không?

## 6. Kiểm tra UI/thread

- Có gọi DB trên EDT không?
- `SwingWorker.done()` có đọc `get()` đúng không?
- Exception trong `doInBackground` có bị nuốt không?
- Bảng có model cũ không được refresh không?
- Có gọi `dispose()`/mở frame trùng không?

## 7. Xác định nguyên nhân gốc

Viết một câu:

```text
Nguyên nhân gốc: ...
Bằng chứng: ...
Tầng chịu trách nhiệm: UI / controller / DAO / SQL / DB / dữ liệu
```

Nếu chỉ sửa triệu chứng, ghi rõ và tạo việc tiếp theo.

## 8. Sửa

- Sửa ở tầng gốc, không thêm try/catch nuốt lỗi.
- Không hạ cấp validation DB.
- Không thêm dữ liệu mock.
- Không đổi quy ước chung để né lỗi.
- Nếu cần đổi schema: migration mới, không sửa V1/V2.
- Nếu cần đổi nghiệp vụ: hỏi trước.

## 9. Test hồi quy

Tối thiểu:

- Test tái hiện đúng lỗi cũ (phải fail trước khi sửa, pass sau khi sửa).
- Test luồng thành công lân cận.
- Test quyền nếu lỗi liên quan vai trò.
- Test dữ liệu biên nếu lỗi liên quan số lượng/tiền/trạng thái.

Nếu chưa có hạ tầng test, ghi lại kịch bản kiểm tra thủ công và bằng chứng.

## 10. Báo cáo

```text
Hiện tượng:
Tái hiện:
Nguyên nhân gốc:
Đã sửa:
File thay đổi:
Test hồi quy:
Ảnh hưởng UC khác:
Rủi ro còn lại:
```

## Checklist chống hồi quy

- [ ] Không còn đường vào cũ gây lỗi.
- [ ] Không thêm nhánh `if` che lỗi.
- [ ] Không bỏ test/validate để pass.
- [ ] Không sửa dữ liệu thật.
- [ ] Đã kiểm tra UC lân cận.
- [ ] Đã ghi chú vào `KNOWN_DISCREPANCIES.md` nếu phát hiện lệch mới.
