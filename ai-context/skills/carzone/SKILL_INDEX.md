# SKILL_INDEX — chọn skill theo công việc

Mỗi dòng là một skill độc lập. Dùng cột "Nạp khi" để quyết định. Không nạp toàn bộ.

## Nhóm 01 — Phân tích

| Skill | Nạp khi | Đầu ra chính | Phụ thuộc |
|---|---|---|---|
| `01-analysis/project-context-analysis` | Bắt đầu phiên làm việc, nhận repo lạ, hoặc cần xác minh tech/kiến trúc | Context card: nguồn sự thật, file liên quan, nợ kỹ thuật | `PROJECT_CONTEXT.md` |
| `01-analysis/use-case-analysis` | Có UC mới hoặc yêu cầu mơ hồ cần bóc tách | Đặc tả UC chuẩn hóa: actor, luồng, dữ liệu, luật, acceptance | `project-context-analysis` |
| `01-analysis/role-authorization` | Thêm/sửa chức năng có phân quyền hoặc dữ liệu theo người dùng | Ma trận quyền, điểm chặn UI/controller/DB, test quyền | `ROLE_USECASE_MATRIX.md` |
| `01-analysis/business-rules-workflow` | Có trạng thái, điều kiện tiên quyết, tiền, tồn kho, khóa dữ liệu | Bảng luật, máy trạng thái, guard, mã lỗi | `business-rules.md` |

## Nhóm 02 — Kiến trúc

| Skill | Nạp khi | Đầu ra chính | Phụ thuộc |
|---|---|---|---|
| `02-architecture/java-architecture` | Tạo feature slice, thêm lớp/package, refactor luồng | Sơ đồ file cần thêm/sửa, hợp đồng lớp, thứ tự triển khai | `project-context-analysis` |
| `02-architecture/database-transaction-integrity` | Gọi procedure, nhiều bảng, tồn kho, khóa, migration | Kế hoạch transaction, ánh xạ lỗi DB, kiểm tra trigger/view | `business-rules-workflow` |
| `02-architecture/authentication-session` | Đăng nhập, đăng xuất, phân quyền theo phiên, tài khoản | Luồng xác thực, quy tắc session, checklist bảo mật | `role-authorization` |

## Nhóm 03 — Giao diện

| Skill | Nạp khi | Đầu ra chính | Phụ thuộc |
|---|---|---|---|
| `03-ui/swing-ui-development` | Tạo/sửa màn hình, form, bảng, dialog | Bố cục Figma, thành phần, trạng thái UI, validation | skill cũ `swing-screen.md` |
| `03-ui/role-navigation-dashboard` | Thêm menu, dashboard, điều hướng theo vai trò | Menu theo role, KPI, ánh xạ `CardLayout`, chặn truy cập | `role-authorization` |
| `03-ui/swing-threading-responsiveness` | Tác vụ DB, tìm kiếm, tải bảng, xuất file, thao tác dài | Thiết kế `SwingWorker`, trạng thái loading, hủy, lỗi | `swing-ui-development` |

## Nhóm 04 — Nghiệp vụ

| Skill | Nạp khi | Đầu ra chính | Phụ thuộc |
|---|---|---|---|
| `04-features/usecase-orchestrator` | Bắt đầu một UC trọn vẹn từ phân tích đến báo cáo | Kế hoạch và báo cáo hoàn thành theo UC | tất cả skill liên quan |
| `04-features/crud-master-data` | UC002–UC006, UC016; danh mục khách hàng/xe/dịch vụ/linh kiện/nhân viên/tài khoản/khuyến mại | Form, validate, DAO CRUD, quy tắc xóa/ngừng dùng | `role-authorization` |
| `04-features/repair-order-workflow` | UC010, UC011; tiếp nhận, phân công, cập nhật sửa chữa | Máy trạng thái phiếu, guard, chi tiết DV/LK, khóa hoàn tất | `business-rules-workflow` |
| `04-features/inventory-replenishment` | UC006, UC009; yêu cầu nhập, duyệt, nhập kho, cảnh báo tồn | Luồng yêu cầu, side effect tồn kho, quyền KTV/CCH | `repair-order-workflow` |
| `04-features/invoice-payment-promotion` | UC001, UC003, UC014, UC015; hóa đơn, thanh toán, khuyến mại, doanh thu | Điều kiện lập HĐ, công thức tiền, khóa sau thanh toán, test | `database-transaction-integrity` |
| `04-features/returns-warranty` | UC012; đổi trả, hoàn tiền, bảo hành | Điều kiện 7 ngày, chi tiết đổi trả, doanh thu hoàn, bảo hành | `invoice-payment-promotion` |
| `04-features/search-statistics` | UC001, UC007, UC008, UC013; tra cứu, lịch sử, thống kê | Câu query, phạm vi dữ liệu theo role, định nghĩa KPI | `role-authorization` |

## Nhóm 05 — Chất lượng

| Skill | Nạp khi | Đầu ra chính | Phụ thuộc |
|---|---|---|---|
| `05-quality/testing-acceptance` | Viết test, kiểm thử acceptance, chuẩn bị demo | Ma trận test, test case, bằng chứng chạy | UC skill tương ứng |
| `05-quality/debugging-regression` | Có lỗi, hành vi sai, hồi quy sau sửa | Tái hiện, khoanh vùng tầng, RCA, test hồi quy | `PROJECT_CONTEXT.md` |
| `05-quality/code-review-completion` | Trước khi báo xong, trước commit/merge | Báo cáo PASS/FAIL, rủi ro còn lại, việc tiếp theo | tất cả |

## Đường đi gợi ý

- Làm một UC mới: `project-context-analysis` → `use-case-analysis` → `role-authorization` → `business-rules-workflow` → `java-architecture` → `database-transaction-integrity` (nếu có DB) → skill UI → skill feature → `testing-acceptance` → `code-review-completion`.
- Sửa một màn hình: `swing-ui-development` → `swing-threading-responsiveness` (nếu có tác vụ nền) → `role-authorization` (nếu có nút/quyền) → `code-review-completion`.
- Sửa lỗi: `debugging-regression` → skill feature liên quan → `testing-acceptance` → `code-review-completion`.
- Chuẩn bị báo cáo/demo: `USECASE_SKILL_TRACEABILITY.md` → `testing-acceptance` → `code-review-completion`.

## Quy tắc chọn skill

1. Nếu công việc chạm database, luôn nạp `database-transaction-integrity`.
2. Nếu công việc có phân quyền hoặc dữ liệu "của tôi", luôn nạp `role-authorization`.
3. Nếu công việc có tiền, thuế, khuyến mại, tồn kho hoặc trạng thái, luôn nạp `business-rules-workflow`.
4. Nếu công việc tạo/sửa UI, luôn nạp `swing-ui-development`; nếu có gọi DB, thêm `swing-threading-responsiveness`.
5. Nếu công việc là một UC trọn vẹn, dùng `usecase-orchestrator` để điều phối thay vì tự ghép.
