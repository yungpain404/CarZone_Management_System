---
name: usecase-orchestrator
description: Dùng khi nhận một use case CarZone trọn vẹn cần đi từ phân tích, thiết kế, code, test đến báo cáo. Điều phối các skill chuyên môn thay vì tự ghép rời rạc.
---

# Điều phối triển khai use case

## Mục đích

Là điểm vào cho một UC hoàn chỉnh. Skill này không thay thế skill chuyên môn; nó quyết định nạp skill nào, theo thứ tự nào, và khi nào dừng.

## Đầu vào

- Mã UC (UC001–UC016) hoặc mô tả tương đương.
- Phạm vi: phân tích / thiết kế / triển khai / kiểm thử / cả bốn.

## Quy trình

### 1. Định vị

Đọc `ai-context/skills/carzone/USECASE_SKILL_TRACEABILITY.md`, tìm dòng UC. Ghi lại:

- Actor.
- DB liên quan.
- Code hiện có.
- Skill chính.
- Trọng tâm test.

### 2. Nạp skill theo nhóm

| Giai đoạn | Skill |
|---|---|
| Ngữ cảnh | `01-analysis/project-context-analysis` |
| Đặc tả | `01-analysis/use-case-analysis` |
| Quyền | `01-analysis/role-authorization` |
| Luật/trạng thái | `01-analysis/business-rules-workflow` |
| Kiến trúc | `02-architecture/java-architecture` |
| DB/transaction | `02-architecture/database-transaction-integrity` |
| Xác thực | `02-architecture/authentication-session` nếu UC002/UC004 |
| UI | `03-ui/swing-ui-development` |
| Menu/dashboard | `03-ui/role-navigation-dashboard` nếu có trang/menu |
| Thread | `03-ui/swing-threading-responsiveness` nếu có DB |
| Nghiệp vụ | skill `04-features/*` tương ứng |
| Test | `05-quality/testing-acceptance` |
| Review | `05-quality/code-review-completion` |

### 3. Lập kế hoạch

Xuất:

```text
UC:
Actor:
Quyền:
Tiền điều kiện:
Luồng chính:
Luật/trạng thái:
DB dùng lại:
DB cần thêm (nếu có):
File sẽ thêm/sửa:
Rủi ro:
Điểm cần hỏi:
Kế hoạch test:
```

Chưa sửa code khi kế hoạch còn điểm "cần hỏi" ảnh hưởng nghiệp vụ.

### 4. Triển khai

Theo thứ tự trong `workflows/implement-usecase.md`:

1. Entity/enum nếu thiếu.
2. DAO interface.
3. DAO impl.
4. Controller.
5. UI.
6. Menu/đăng ký trang.
7. Test.

### 5. Kiểm tra chéo

- Quyền: thử cả ba vai trò.
- Trạng thái: thử chuyển hợp lệ và không hợp lệ.
- Dữ liệu: thử biên, trùng, rỗng, NULL.
- Tiền/tồn: thử làm tròn, vượt tồn, khuyến mại biên.
- DB: thử lỗi trigger/procedure.
- UI: thử loading, rỗng, lỗi, resize.

### 6. Báo cáo

Dùng mẫu cuối `workflows/implement-usecase.md`. Nêu rõ test đã chạy/chưa chạy.

## Điểm dừng

- UC không có trong tài liệu.
- Quyền chưa chốt.
- Cần đổi schema hoặc migration cũ.
- Cần thêm thư viện/ORM/service.
- Cần đổi công thức tiền/bảo hành.
- Figma không có.
- Phát hiện mâu thuẫn chưa ghi nhận.

## Definition of Done

- Có đặc tả UC, ma trận quyền, bảng luật.
- Code đúng kiến trúc, biên dịch được.
- Luồng chính chạy với DB thật hoặc test tương đương.
- Guard quan trọng có test/bằng chứng.
- Không mock, không fallback nhân viên mặc định.
- Báo cáo minh bạch phần chưa làm.

## Liên quan

- `workflows/implement-usecase.md`
- `SKILL_INDEX.md`
- `USECASE_SKILL_TRACEABILITY.md`
