---
name: use-case-analysis
description: Dùng khi cần bóc tách một use case CarZone từ tài liệu Word hoặc yêu cầu mơ hồ thành đặc tả có actor, luồng, dữ liệu, quyền, luật nghiệp vụ và tiêu chí chấp nhận trước khi thiết kế hoặc viết code.
---

# Phân tích use case

## Mục đích

Chuyển UC thô thành đặc tả đủ chặt để triển khai và kiểm thử. Không viết code trong bước này.

## Khi nào dùng

- Nhận UC001–UC016 hoặc yêu cầu "làm chức năng X".
- Cần làm rõ luồng, dữ liệu, quyền trước khi sửa.
- Tài liệu và database có mâu thuẫn.
- Cần tạo test acceptance.

## Đầu vào

- Mã UC hoặc mô tả.
- Tài liệu Word nếu có.
- `ai-context/skills/carzone/ROLE_USECASE_MATRIX.md`, `ai-context/skills/carzone/PROJECT_CONTEXT.md`, `ai-context/skills/carzone/KNOWN_DISCREPANCIES.md`.

## Các bước

1. Xác định mã UC, tên, actor chính/phụ.
2. Ghi mục tiêu nghiệp vụ và giá trị mang lại.
3. Ghi điều kiện tiên quyết (dữ liệu phải có, trạng thái phải đạt, quyền).
4. Ghi luồng chính theo từng bước, mỗi bước có tác nhân và dữ liệu.
5. Ghi luồng thay thế và luồng lỗi (dữ liệu thiếu, trạng thái sai, quyền sai, DB lỗi).
6. Liệt kê dữ liệu vào/ra và nguồn của từng trường.
7. Liệt kê luật nghiệp vụ, mã lỗi dự kiến, bảng/view/procedure liên quan.
8. Đối chiếu quyền với `ROLE_USECASE_MATRIX.md`.
9. Đối chiếu trạng thái với máy trạng thái trong `PROJECT_CONTEXT.md`.
10. Ghi các điểm chưa rõ và câu hỏi cần chốt.

## Mẫu đặc tả

```text
UC:
Tên:
Actor:
Mục tiêu:
Tiền điều kiện:
Hậu điều kiện:
Luồng chính:
  1. ...
Luồng thay thế:
  A1. ...
Luồng lỗi:
  E1. ...
Dữ liệu vào:
Dữ liệu ra:
Luật nghiệp vụ:
Bảng/view/procedure:
Quyền:
Trạng thái:
Tiêu chí chấp nhận:
Điểm chưa rõ:
```

## Quy tắc CarZone

- Actor phải là một trong ba vai trò, không tự thêm vai trò mới.
- Không mô tả màn hình bằng cảm hứng; chỉ nêu thành phần nghiệp vụ, việc thiết kế thuộc skill UI.
- Không bỏ qua quyền sở hữu dữ liệu của TNV/KTV.
- Không bỏ qua trạng thái phiếu/hóa đơn/yêu cầu.
- Không tự thêm trường database; nếu thiếu, ghi đề xuất migration mới.
- Nếu UC trong Word dùng công thức/thuật ngữ cũ, ghi rõ và đối chiếu `decisions.md`.
- Mỗi tiêu chí chấp nhận phải kiểm thử được.

## Checklist

- [ ] Có actor và quyền cho từng hành động.
- [ ] Có tiền điều kiện và hậu điều kiện.
- [ ] Có luồng chính, thay thế, lỗi.
- [ ] Có dữ liệu vào/ra.
- [ ] Có luật nghiệp vụ và mã lỗi.
- [ ] Có bảng/view/procedure liên quan.
- [ ] Có tiêu chí chấp nhận đo được.
- [ ] Có danh sách điểm chưa rõ.
- [ ] Không có quyết định tự phát.

## Lỗi thường gặp

- Trộn UC010 (lập phiếu) với UC011 (cập nhật phiếu).
- Coi UC012 là "trả hàng" đơn giản, bỏ qua 7 ngày và hóa đơn đã thanh toán.
- Bỏ qua việc KTV chỉ thấy phiếu được giao.
- Mô tả "hệ thống tự tính" mà không nêu view/procedure nào.
- Dùng lại công thức `(DV+LK)*(1-KM)+thue` từ Word.

## Liên quan

- `01-analysis/role-authorization`
- `01-analysis/business-rules-workflow`
- `04-features/usecase-orchestrator`
- `USECASE_SKILL_TRACEABILITY.md`
