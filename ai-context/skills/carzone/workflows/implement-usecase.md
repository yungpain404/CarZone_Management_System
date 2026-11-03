# Workflow — triển khai một use case

Dùng khi nhận một UC mới hoặc một yêu cầu đủ lớn để đi qua nhiều tầng. Không dùng cho sửa typo hoặc đổi màu.

## Đầu vào bắt buộc

- Mã UC hoặc mô tả yêu cầu.
- Phạm vi: chỉ phân tích, chỉ sửa UI, hay triển khai đầy đủ.
- Nếu người dùng không nêu, mặc định là "phân tích trước, chưa sửa code".

## Các bước

### Bước 1 — Nạp ngữ cảnh

Đọc:

1. `ai-context/skills/carzone/PROJECT_CONTEXT.md`
2. `ai-context/skills/carzone/SKILL_INDEX.md`
3. `ai-context/skills/carzone/ROLE_USECASE_MATRIX.md`
4. `ai-context/skills/carzone/USECASE_SKILL_TRACEABILITY.md`
5. `ai-context/skills/carzone/KNOWN_DISCREPANCIES.md`
6. `ai-context/decisions.md`, `ai-context/business-rules.md`
7. Skill cũ liên quan trong `ai-context/skills/` nếu cần chi tiết JDBC/Swing/Flyway.

Không đọc toàn bộ tài liệu Word. Chỉ đọc đúng UC và mục liên quan.

### Bước 2 — Chuẩn hóa UC

Dùng `01-analysis/use-case-analysis`. Xuất:

- Mã UC, tên, actor, mục tiêu.
- Điều kiện tiên quyết và hậu điều kiện.
- Luồng chính, luồng thay thế, luồng lỗi.
- Dữ liệu vào/ra.
- Luật nghiệp vụ và mã lỗi dự kiến.
- Điểm chưa rõ.

Nếu UC không tồn tại trong tài liệu hoặc mâu thuẫn với DB: dừng, hỏi.

### Bước 3 — Chốt quyền

Dùng `01-analysis/role-authorization` và `ROLE_USECASE_MATRIX.md`. Xuất bảng:

| Hành động | CCH | TNV | KTV | Điều kiện sở hữu | Chốt ở đâu |
|---|---|---|---|---|---|

Ghi rõ nếu quyền trong tài liệu khác DB. Không thiết kế nút cho vai trò không được phép.

### Bước 4 — Chốt luật và trạng thái

Dùng `01-analysis/business-rules-workflow`. Xuất:

- Máy trạng thái (nếu có).
- Guard trước mỗi hành động.
- Bất biến dữ liệu.
- Ranh giới transaction.
- Bảng mã lỗi DB 50xxx và thông báo tiếng Việt.

### Bước 5 — Thiết kế kỹ thuật

Dùng `02-architecture/java-architecture` và `02-architecture/database-transaction-integrity`.

Xuất danh sách file:

```text
src/entity/...              (chỉ sửa nếu thiếu trường/hành vi)
src/dao/XxxDao.java         (mới/sửa)
src/dao/impl/XxxDaoImpl.java
src/controller/XxxController.java
src/ui/XxxUI.java
src/enums/...               (chỉ khi có trạng thái mới, cần duyệt)
infra/database/.../V3__...  (chỉ khi schema thật sự đổi)
```

Ghi rõ:

- Bảng/view/procedure/trigger dùng lại.
- Câu SQL/procedure mới (nếu có).
- Kiểu dữ liệu, cách map.
- Cách xử lý lỗi.
- Có cần transaction nhiều bước không.

Nếu cần đổi schema: dừng, đề xuất migration mới, không sửa V1/V2.

### Bước 6 — Thiết kế UI

Dùng `03-ui/swing-ui-development`, `03-ui/role-navigation-dashboard` (nếu là trang/menu), `03-ui/swing-threading-responsiveness` (nếu có DB).

Xuất:

- Tên lớp `UI`, vị trí trong `CardLayout`/menu.
- Thành phần và bố cục theo Figma.
- Trạng thái: loading, rỗng, lỗi, thành công.
- Validation và thứ tự focus/Enter.
- Tác vụ nền và nút bị khóa khi chạy.

Nếu chưa mở được Figma, dừng và xin ảnh/mô tả. Không tự sáng tác layout.

### Bước 7 — Triển khai theo slice dọc

Thứ tự an toàn:

1. Entity/enum (nếu cần).
2. DAO interface.
3. DAO impl + mapping + procedure.
4. Controller + quyền + validate + dịch lỗi.
5. UI + luồng sự kiện + `SwingWorker`.
6. Đăng ký menu/trang.
7. Test.

Không viết UI trước khi chốt hợp đồng controller/DAO.

### Bước 8 — Kiểm thử

Dùng `05-quality/testing-acceptance`.

Tối thiểu:

- Một test cho luồng thành công.
- Một test cho mỗi guard quan trọng (quyền, trạng thái, số lượng, tiền).
- Một test cho dữ liệu biên.
- Một test cho lỗi DB được dịch đúng.
- Kiểm tra hồi quy UC lân cận.

Nếu chưa chạy được test, ghi rõ "chưa chạy" và lý do. Không ghi "đã kiểm thử".

### Bước 9 — Tự đánh giá

Dùng `05-quality/code-review-completion`. Kiểm tra:

- Đúng UC, đúng quyền, đúng luật.
- Không SQL trong UI/controller.
- Không sinh mã/không cập nhật tồn kho ở Java.
- Không mock, không nhân viên mặc định.
- Resource JDBC đóng đúng.
- EDT không bị block.
- Lỗi DB được giữ nguyên nhân.
- Có test hoặc có lý do.
- Có cập nhật traceability/discrepancies nếu cần.

### Bước 10 — Báo cáo

Dùng mẫu:

```text
UC:
Skill đã dùng:
Đã làm:
File thay đổi:
Quyền đã chặn:
Luật đã giữ:
Test đã chạy / chưa chạy:
Rủi ro còn lại:
Việc tiếp theo:
Câu hỏi cần chốt:
```

## Điểm dừng bắt buộc

Dừng và hỏi nếu:

- UC không có trong tài liệu hoặc trái DB.
- Quyền của vai trò chưa rõ.
- Cần đổi schema/migration cũ.
- Cần thêm thư viện/ORM/service layer.
- Cần đổi công thức tiền, thuế, khuyến mại, bảo hành.
- Figma không truy cập được.
- Phát hiện dữ liệu thật bị ảnh hưởng.

## Definition of Done

- Có đặc tả UC và ma trận quyền.
- Có danh sách file thay đổi khớp kiến trúc.
- Code biên dịch được.
- Luồng chính chạy được với DB thật hoặc có test tương đương.
- Các guard quan trọng có test hoặc bằng chứng kiểm tra.
- Không còn TODO che giấu lỗi.
- Báo cáo nêu rõ phần chưa làm và rủi ro.
