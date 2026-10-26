---
name: flyway-migration
description: Dùng khi cần thay đổi schema SQL Server của CarZone (thêm/sửa bảng, cột, ràng buộc, trigger, view, stored procedure) bằng migration Flyway. Không dùng cho truy vấn thông thường hay dữ liệu chạy thử.
---

# Viết migration Flyway cho CarZone

## Vị trí và cách chạy (đã kiểm tra trong repo)
- Migration: `infra/database/fly_way_migrations/` (tên thư mục có dấu gạch dưới, giữ nguyên).
- Hiện có `V1__init_CarZone_Database.sql` và `V2__update_KhachHang_Entity.sql` (V2 nằm ở nhánh `feature/infrastructure`). Kiểm tra lại thư mục trước khi chọn số, vì hai nhánh chưa merge; không tạo hai file cùng số phiên bản.
- Flyway chạy trong Docker (`infra/docker/compose.yml`), mount thư mục trên ở chế độ chỉ đọc. Dựng lại sạch: `docker compose -f infra/docker/compose.yml down -v` rồi `up -d`.
- Dữ liệu chạy thử nằm ở `infra/database/seed/CarZone_Demo.sql`, **không** phải migration; không đặt vào thư mục migration.

## Quy tắc
1. Tên file: `V{số}__{Mo_ta_ngan}.sql` (ví dụ `V3__update_HoaDon_Entity.sql`). Không sửa file đã có, kể cả khi sai; sửa bằng migration mới.
2. Không `CREATE DATABASE`, không `USE`. Database tạo ở bước `db-init` của compose.
3. Mở đầu `SET ANSI_NULLS ON; SET QUOTED_IDENTIFIER ON; GO`, dùng `GO` tách batch như V1/V2 (Flyway hỗ trợ).
4. Theo cách đặt tên của V1: `PK_`, `FK_`, `UQ_`, `CK_`, `DF_`, `IX_`/`UX_` cho index, `trg_` trigger, `sp_` procedure, `vw_` view.
5. Muốn đổi cột có CHECK/UNIQUE/DEFAULT phụ thuộc: gỡ ràng buộc, `ALTER COLUMN`, tạo lại (xem V2). Sau khi đổi cột mà view đọc cột đó thì gọi `sp_refreshview`.
6. Trigger báo lỗi bằng `THROW` với mã ≥ 50000 và thông báo tiếng Việt. Các mã đã dùng nằm trong khoảng 50001–50261 (V1); chọn mã chưa dùng, không tái sử dụng.
7. Thuộc tính dẫn xuất (tổng tiền, thuế, hoàn tiền) đặt ở view `vw_*`, không lưu cột.
8. Thay đổi schema phải khớp entity (`src/entity`) và `ai-context/business-rules.md`. Nếu đổi luật nghiệp vụ, hỏi người dùng và ghi vào `ai-context/decisions.md`.
9. Nếu bảng đã có dữ liệu thì migration phải xử lý dữ liệu cũ (V2 ghi chú rõ trường hợp SĐT 11 số sẽ làm bước ALTER lỗi).

## Kiểm tra
Dựng DB sạch, xác nhận Flyway chạy từ V1 đến phiên bản mới không lỗi, rồi chạy `CarZone_Demo.sql` trong SSMS/Azure Data Studio và so kết quả với kỳ vọng trong file. Nếu không có Docker trong môi trường làm việc thì nói rõ là chưa chạy thật.
