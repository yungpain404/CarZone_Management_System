# Bộ Agent Skills CarZone

Bộ skill này biến tài liệu nghiệp vụ, database và code hiện tại của CarZone thành quy trình làm việc có thể lặp lại cho AI/agent. Mục tiêu không phải viết lại tài liệu, mà buộc agent đi đúng thứ tự: hiểu ngữ cảnh, phân tích use case, kiểm tra quyền, xác định luật nghiệp vụ, thiết kế thay đổi, triển khai, kiểm thử và tự đánh giá.

## Đọc theo thứ tự nào

1. `PROJECT_CONTEXT.md` — nguồn sự thật đã kiểm chứng về công nghệ, kiến trúc, entity, database.
2. `SKILL_INDEX.md` — chọn skill theo loại công việc.
3. `ROLE_USECASE_MATRIX.md` — xác định ai được làm gì.
4. `USECASE_SKILL_TRACEABILITY.md` — nối UC001–UC016 với skill, bảng, màn hình và điểm kiểm thử.
5. `KNOWN_DISCREPANCIES.md` — các chỗ tài liệu, database và code đang lệch nhau.
6. Skill chuyên môn trong `01-analysis` … `05-quality`.
7. Workflow trong `workflows/` khi cần quy trình đầu-cuối.

Chỉ đọc các file liên quan đến việc đang làm. Không nạp toàn bộ bộ skill vào một lần.

## Cấu trúc

```text
ai-context/skills/carzone/
├── README.md
├── PROJECT_CONTEXT.md
├── SKILL_INDEX.md
├── ROLE_USECASE_MATRIX.md
├── USECASE_SKILL_TRACEABILITY.md
├── KNOWN_DISCREPANCIES.md
├── workflows/
│   ├── implement-usecase.md
│   ├── review-change.md
│   └── debug-regression.md
├── 01-analysis/
├── 02-architecture/
├── 03-ui/
├── 04-features/
└── 05-quality/
```

Mỗi skill là một thư mục có `SKILL.md`, phần đầu có YAML `name` và `description` để agent chọn đúng skill.

## Quan hệ với skill cũ

`ai-context/skills/*.md` (flat) vẫn là tài liệu kỹ thuật chi tiết đã được nhóm dùng:

- `new-feature-slice.md`
- `jdbc-dao-mapping.md`
- `swing-screen.md`
- `flyway-migration.md`
- `verify-business-rules.md`

Bộ skill mới không sao chép các file này. Khi một skill mới cần chi tiết triển khai, nó trỏ về file cũ tương ứng. Thứ tự ưu tiên để xác định hiện trạng là database hiện hành, `ai-context/decisions.md`/`business-rules.md`, rồi code, rồi tài liệu Word. Nếu xung đột ảnh hưởng nghiệp vụ hoặc schema, dừng, ghi vào `KNOWN_DISCREPANCIES.md` và hỏi; không tự chọn.

## Cách dùng với agent

Yêu cầu tối thiểu khi giao việc:

```text
Đọc ai-context/skills/carzone/SKILL_INDEX.md và các skill liên quan đến UC0xx.
Làm theo workflows/implement-usecase.md.
Trước khi sửa code, trình bày: UC, actor, quyền, luật nghiệp vụ, bảng/view/proc liên quan,
file sẽ sửa, kế hoạch test. Dừng lại nếu gặp mâu thuẫn chưa được duyệt.
```

Agent phải nêu rõ skill nào đã dùng và mục nào trong checklist đã đạt/chưa đạt.

## Quy tắc bắt buộc

- Không bịa use case, trường dữ liệu, màn hình, mã lỗi hay quyền.
- Không tự chọn khi tài liệu, database và code mâu thuẫn. Ghi vào báo cáo và hỏi.
- Không thêm Hibernate/JPA, service layer, thư viện mới nếu chưa được duyệt.
- Không sinh mã ở Java khi database đã có procedure/trigger/sequence làm việc đó.
- Không tự trừ/cộng tồn kho ở Java.
- Không tính lại tiền/thuế/khuyến mại ở Java khi đã có view.
- Không để UI gọi SQL trực tiếp; controller không chứa SQL; DAO chịu trách nhiệm JDBC.
- Không dùng dữ liệu mock hoặc nhân viên mặc định để vào hệ thống.
- Không commit, không sửa migration cũ, không đổi quy ước chung khi chưa được yêu cầu.
- Mọi thay đổi phải truy vết được về UC, quyền, luật nghiệp vụ và test.

## Bảo trì

Cập nhật bộ skill khi:

- Có migration mới hoặc thay đổi trigger/procedure/view.
- Có quyết định nghiệp vụ mới trong `ai-context/decisions.md`.
- Có màn hình/DAO/controller mới được hoàn thành.
- Phát hiện mâu thuẫn mới giữa tài liệu và hệ thống.

Khi cập nhật, sửa `PROJECT_CONTEXT.md`, `USECASE_SKILL_TRACEABILITY.md` và `KNOWN_DISCREPANCIES.md` cùng lúc. Không chỉ sửa một skill riêng lẻ.
