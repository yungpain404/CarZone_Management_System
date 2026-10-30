---
name: tranh-conflict
description: Dùng khi bắt đầu một phiên làm việc trên repo CarZone, khi `git status` có file `UU`/conflict, hoặc trước khi sửa bất kỳ file nào để thay đổi không conflict với code và tài liệu của nhóm. Đọc lúc đầu phiên.
---

# Tránh conflict khi nhiều người cùng làm

Repo có nhiều người/agent sửa song song. Làm theo file này để thay đổi của bạn không conflict với code và tài liệu của nhóm.

## 1. Trước khi bắt đầu
1. `git status`: thấy trạng thái `UU` (conflict đang mở) thì **dừng, báo người dùng**, không tự chọn bên, không tự `git add` để "cho qua".
2. Đọc `ai-context/PROGRESS.md` + `ai-context/decisions.md`: việc nào đã chốt thì không lật lại, việc nào ghi "xong" thì không làm lại lần nữa.
3. Đối chiếu **cấu trúc thật** trên đĩa (`src/`, `pom.xml`) thay vì nhớ lại — tài liệu có thể cũ hơn code; lệch thì hỏi, không tự suy diễn.

## 2. Khi viết code
4. Không tái tạo cấu trúc đã gỡ ngày 30/10/2026 (danh sách ở mục **Cấm** của skill `new-feature-slice`): package `service`, `src/dao/impl/`, interface `I*Dao`, lớp `*Id.java` khóa ghép, `src/resources/images/`, `.png` trong `src/ui/`, `src/app/Main.java`. Tái tạo = code của bạn tự conflict với code của nhóm lúc merge.
5. Đổi gì thì cập nhật **cùng lúc** tài liệu liên quan (`AGENTS.md`, `ai-context/decisions.md`, `ai-context/PROGRESS.md`, skill liên quan), để agent của người khác không viết ngược lại thay đổi của bạn.
6. **Chỉ sửa đúng đoạn cần**: không format lại cả file khi việc cần làm không liên quan tới formatting. Bỏ/gộp dòng, đổi thụt lề hàng loạt trên file nhiều người sửa là nguyên nhân conflict kinh điển.
7. Nếu bắt buộc phải đổi formatting/nhập lại cả file (rename, dọn khoảng trắng toàn thư mục), làm trong **một commit riêng**, ghi vào `ai-context/PROGRESS.md` và báo nhóm trước khi làm việc khác.
8. Cẩn thận với file nhiều người sửa: `src/ui/MainFrame.java`, `src/ui/DashboardUI.java`, `src/ui/DangNhapUI.java`, `pom.xml`, `AGENTS.md`, `ai-context/PROGRESS.md`. Đọc kỹ phần cần đổi, chỉ sửa đúng đoạn đó.

## 3. Khi git báo conflict
9. Không `git stash apply`, không `git checkout --ours/--theirs` bừa khi chưa hiểu nội dung; hỏi người dùng chọn bên nào, hoặc chốt "tích hợp cả hai" rồi gộp từng hunk.
10. Không tự `git add`/`commit`/`merge` khi chưa được yêu cầu; làm việc trên nhánh `develop`, agent không merge (xem `AGENTS.md`).

## 4. Sau khi sửa xong
- `mvn -q compile` phải exit 0 trước khi báo xong.
- Ghi nhận việc đã làm vào `ai-context/PROGRESS.md` (mục ngày hiện tại).
- Báo ngắn: đổi những gì, file nào đang chờ người dùng `git add`.
