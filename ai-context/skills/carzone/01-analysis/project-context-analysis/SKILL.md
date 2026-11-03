---
name: project-context-analysis
description: Dùng khi bắt đầu phiên làm việc trong repo CarZone, khi nhận một yêu cầu mới, hoặc khi cần xác minh công nghệ, kiến trúc, quy ước và nợ kỹ thuật trước khi sửa code. Không dùng để thay thế phân tích use case chi tiết.
---

# Phân tích ngữ cảnh dự án

## Mục đích

Dựng "context card" ngắn gọn, đúng sự thật, trước khi agent chạm vào code. Tránh các lỗi phổ biến: dùng Hibernate vì tài liệu cũ, tạo service layer, tự sinh mã, tự trừ tồn kho, tin vào `PROGRESS.md`.

## Khi nào dùng

- Bắt đầu task mới trong repo.
- Người dùng hỏi "hệ thống hiện tại thế nào".
- Cần xác định file nào liên quan trước khi lập kế hoạch.
- Nghi ngờ tài liệu và code lệch nhau.

Không dùng khi đã có context card còn hiệu lực trong cùng phiên và không có thay đổi lớn.

## Đầu vào

- Yêu cầu của người dùng.
- Quyền truy cập repo.

## Các bước

1. Đọc `ai-context/skills/carzone/PROJECT_CONTEXT.md` (bộ skill này).
2. Đọc `AGENTS.md`, `ai-context/decisions.md`, `ai-context/business-rules.md`.
3. Kiểm tra `pom.xml` để xác nhận Java 17, FlatLaf, mssql-jdbc, JUnit 5, `sourceDirectory`, `main.class`.
4. Liệt kê `src/` theo package; xác định entity/DAO/controller/UI nào đã có.
5. Liệt kê migration trong `infra/database/fly_way_migrations/`; đọc tên bảng, view, procedure, trigger liên quan.
6. Đọc `USECASE_SKILL_TRACEABILITY.md` để biết UC nào đã có code.
7. Đọc `KNOWN_DISCREPANCIES.md`.
8. Nếu tài liệu Word cần thiết, chỉ trích đúng UC/mục; không đọc toàn bộ.

## Context card đầu ra

```text
Mục tiêu task:
Nguồn sự thật đã kiểm:
- DB/migration:
- Quyết định:
- Code:
- Tài liệu:
Công nghệ liên quan:
Tầng sẽ chạm:
File hiện có:
File còn thiếu:
Nợ kỹ thuật ảnh hưởng task:
Rủi ro/mâu thuẫn:
Câu hỏi cần chốt:
```

## Quy tắc

- Không suy ra kiến trúc từ `PROGRESS.md`.
- Không coi tài liệu Word là đúng hơn database.
- Không đề xuất thư viện mới khi chưa kiểm tra `pom.xml`.
- Không tạo service layer; dự án dùng controller.
- Không đề xuất Hibernate/JPA.
- Không khẳng định "đã có" nếu chưa thấy file/DDL.
- Không đọc toàn bộ báo cáo Word nếu chỉ cần một UC.

## Checklist

- [ ] Đã xác nhận Java 17, Swing, JDBC thuần.
- [ ] Đã xác nhận không có Hibernate/JPA/service.
- [ ] Đã xác định đúng package và quy ước tên.
- [ ] Đã biết bảng/view/procedure/trigger liên quan.
- [ ] Đã biết file nào đã có, file nào còn thiếu.
- [ ] Đã ghi rõ mâu thuẫn thay vì tự chọn.
- [ ] Context card không dài quá mức cần thiết.

## Lỗi thường gặp

- Tin `PROGRESS.md` và đề xuất `JpaUtil`.
- Tạo `service/` vì tài liệu kiến trúc mẫu.
- Tự sinh `maHoaDon` trong Java.
- Tự trừ `soLuongTon` trong DAO.
- Đọc toàn bộ docx rồi trích sai UC.
- Bỏ qua `decisions.md` và dùng công thức cũ trong Word.

## Liên quan

- `01-analysis/use-case-analysis`
- `02-architecture/java-architecture`
- `PROJECT_CONTEXT.md`
- `KNOWN_DISCREPANCIES.md`
