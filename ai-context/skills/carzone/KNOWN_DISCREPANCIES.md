# KNOWN_DISCREPANCIES — những chỗ chưa thống nhất

Mỗi mục ghi: hiện tượng, nguồn, cách xử lý. Không tự chọn bên nào khi chưa có quyết định. Khi giải quyết xong, cập nhật mục này và nguồn tương ứng.

## D01 — Thứ tự trạng thái phiếu sửa chữa

- Tài liệu Word (UC011): mô tả chuỗi "Chờ sửa → Đang sửa → Chờ linh kiện → Hoàn tất".
- Database: cho phép `CHO_SUA -> CHO_LINH_KIEN` trực tiếp, `DANG_SUA -> CHO_LINH_KIEN`, `CHO_LINH_KIEN -> DANG_SUA`, và `DANG_SUA -> HOAN_TAT`.
- `enums.TrangThaiPhieu` chỉ là danh sách giá trị, không chứa transition.
- Xử lý: dùng máy trạng thái trong `PROJECT_CONTEXT.md` (theo DB) làm chuẩn. Nếu UI/tài liệu cần chuỗi tuyến tính, hỏi trước khi giới hạn bớt.

## D02 — Công thức thuế và khuyến mại

- Tài liệu Word: `tongThanhToan = (tongTienDichVu + tongTienLinhKien) * (1 - khuyenMai) + thue`.
- Database/view và `decisions.md`: `tongThanhToan = tongTienDichVu + tongTienLinhKien - tienGiam + thue`; thuế tính trên tiền trước giảm.
- Xử lý: view là nguồn cuối. Không tính lại ở Java. Báo cáo Word cần sửa, không sửa view theo Word.

## D03 — `HoaDon` thiếu `maPhieu` trong mô tả lớp

- Tài liệu Word: lớp `HoaDon` liệt kê `nhanVien`, `khuyenMai` nhưng không nêu quan hệ phiếu sửa chữa.
- Database: `HoaDon.maPhieu` NOT NULL, UNIQUE; mỗi phiếu tối đa một hóa đơn.
- Entity: `HoaDon.phieuSuaChua`.
- Xử lý: DB/entity là chuẩn. Mọi thiết kế hóa đơn phải gắn `maPhieu`.

## D04 — Tên cột trong tài liệu và database

- Tài liệu dùng tên nghiệp vụ `nhanVien`, `khuyenMai`; database dùng `maNV`, `maKM`.
- Xử lý: entity giữ quan hệ `NhanVien`/`KhuyenMai`; DAO map cột `maNV`/`maKM`. Không đổi schema vì tài liệu.

## D05 — Vai trò tạo phiếu đổi trả

- Tài liệu: actor UC012 là Tiếp nhận viên.
- `ROLE_USECASE_MATRIX.md` và yêu cầu tổng quát: Kỹ thuật viên "xử lý đổi trả".
- Database: `trg_PhieuDoiTra_KiemTra` chỉ cho `TIEP_NHAN_VIEN` tạo header; `ChiTietDoiTra` không kiểm tra vai trò.
- Xử lý: chỉ TNV lập phiếu. Nếu cho KTV tham gia, phải chốt là KTV cập nhật chi tiết trên phiếu do TNV lập hay mở rộng quyền tạo; cần migration/trigger mới. Chưa chốt thì không thiết kế luồng KTV tạo phiếu.

## D06 — Bảo hành không có bảng riêng

- Tài liệu nêu chính sách bảo hành 6 tháng/5000 km; dầu và phụ kiện 3 tháng/3000 km, tính từ ngày lập hóa đơn.
- Database không có bảng bảo hành, không có trường hạn bảo hành.
- Xử lý: chỉ tính/hiển thị suy diễn từ hóa đơn và danh mục. Không tạo bảng mới khi chưa được duyệt. Ghi rõ "bảo hành là suy diễn" trong tài liệu.

## D07 — Số điện thoại khách hàng

- Tài liệu/business-rules có chỗ ghi 10 số; database cho 10–11 số.
- Xử lý: theo DB (10–11 số, bắt đầu 0, duy nhất). Không siết còn 10 nếu chưa có quyết định.

## D08 — Tuổi tối thiểu nhân viên

- `decisions.md` từng ghi entity kiểm tra 16 tuổi; DB yêu cầu đủ 18 tuổi tại ngày vào làm.
- Xử lý: DB là chuẩn (>=18). Nếu entity còn 16, đó là lỗi cần sửa, không phải quyền hợp lệ.

## D09 — Mã định danh trong ví dụ tài liệu

- Tài liệu có ví dụ `CCH01`, `DV01`, `LK01`; hệ thống dùng `CCH001`, `DV00001`, `LK00001`.
- Xử lý: theo DB/procedure. Ví dụ trong báo cáo chỉ là minh họa.

## D10 — `PROGRESS.md` lỗi thời

- `PROGRESS.md` còn nhắc Hibernate/JPA, `JpaUtil`, `persistence.xml`.
- Thực tế đã bỏ Hibernate từ 28/10/2026; dùng JDBC thuần.
- Xử lý: không dùng `PROGRESS.md` để suy ra kiến trúc. Đọc `AGENTS.md`, `decisions.md`, code. Đề nghị cập nhật `PROGRESS.md` khi có thời gian.

## D11 — `DashboardController` dùng sai tên cột

- Code hiện tại dùng `p.maXe`, `p.maNVSuaChua`; DB dùng `bienSoXe`, `maNVKyThuat`.
- Hệ quả: dashboard TNV/KTV có thể luôn rỗng hoặc lỗi.
- Xử lý: sửa controller/DAO theo schema, thêm test. Đây là lỗi code, không phải mâu thuẫn nghiệp vụ.

## D12 — Hai khung giao diện gần trùng nhau

- Tồn tại `ui.MainFrame` và `ui.DashboardUI`, cùng dựng header/sidebar/content; dễ lệch hành vi.
- Xử lý: chọn một khung chính (theo `AGENTS.md` là `MainFrame`), chuyển dashboard thành panel/trang trong `CardLayout`; xóa hoặc khai tử bản còn lại sau khi được duyệt.

## D13 — Nhân viên mặc định khi chưa đăng nhập

- `MainFrame`/`DashboardUI` có `taoNhanVienMacDinh()` trả về CCH001 khi không có session.
- Hệ quả: có thể vào giao diện quản trị mà không xác thực.
- Xử lý: bỏ fallback, chuyển về `DangNhapUI`; chỉ mở khung chính khi `UserSession.daDangNhap()`.

## D14 — UI/controller truy vấn SQL trực tiếp

- `DashboardUI` và `DashboardController` có câu SQL trực tiếp; trái kiến trúc `ui -> controller -> dao`.
- Xử lý: chuyển query sang DAO mới (ví dụ `DashboardDao`/các DAO nghiệp vụ), controller chỉ điều phối. Không nhân rộng mẫu này.

## D15 — Hard-code username `sa`

- `DbUtil.getConnection()` dùng `"sa"` trực tiếp; mật khẩu đọc từ `.env`.
- Xử lý: đưa username vào cấu hình (`DatabaseConfig`) khi có dịp; không hard-code thêm. Không đổi `.env`/quyết định git nếu chưa được yêu cầu.

## D16 — Mật khẩu SHA-256 không salt

- `TaiKhoan.setMatKhau` băm SHA-256 hex in hoa, không salt; đây là quyết định của nhóm cho đồ án.
- Xử lý: giữ nguyên nếu chưa được duyệt đổi. Trong báo cáo bảo mật, ghi rõ đây là điểm yếu đã biết; đề xuất PBKDF2/bcrypt nếu được phép.

## D17 — Test JUnit 5 chưa chạy được

- `pom.xml` có `junit-jupiter` nhưng không cấu hình `maven-surefire-plugin` 3.x; `mvn test` có thể không chạy test JUnit 5.
- `sourceDirectory=src`, nên nếu đặt test dưới `src/test/java` thì main compile cũng quét thư mục này; dễ lỗi vì JUnit chỉ có scope `test`.
- Chưa có thư mục test.
- Xử lý: khi thêm test, cấu hình `testSourceDirectory` riêng (khuyến nghị `test/java`) hoặc exclude `test/**` khỏi main compile; thêm surefire 3.x; không tuyên bố đã test nếu chưa chạy.

## D18 — Kiểu ngày trong entity không đồng nhất

- `AGENTS.md` và quyết định nhóm ghi entity dùng `java.util.Date`.
- `NhanVien.ngaySinh` và `NhanVien.ngayVaoLam` dùng `java.time.LocalDate`; các entity khác như `HoaDon`, `PhieuDoiTra` dùng `Date`.
- Xử lý: giữ nguyên hiện trạng; DAO map `java.sql.Date` sang `LocalDate` khi cần. Không đổi toàn bộ entity sang `LocalDate`/`LocalDateTime` nếu chưa có quyết định. Khi viết code mới, theo kiểu dữ liệu của entity đang sửa và ghi rõ nếu thêm ngoại lệ.

## D19 — Dịch vụ/linh kiện chưa có cơ chế sinh mã

- `AGENTS.md` và các skill ghi mã do DB sinh, Java không tự sinh.
- DB có sequence/default cho `KhachHang`, `KhuyenMai`; procedure cho `NhanVien`, `TaiKhoan`, phiếu sửa chữa, hóa đơn, đổi trả, yêu cầu nhập.
- `DichVu.maDV` và `LinhKien.maLK` chỉ có CHECK định dạng, không có sequence/default/procedure sinh mã; chưa có DAO/controller cho hai danh mục này.
- Xử lý: trước khi làm UC005/UC006, chốt một trong ba hướng: (a) thêm sequence/default bằng migration mới, (b) người dùng nhập mã và validate, (c) sinh ở Java (trái quy ước chung, chỉ dùng nếu được duyệt rõ). Khuyến nghị (a). Không tự chọn.

## Quy trình xử lý mâu thuẫn

1. Ghi mục mới vào file này với mã `Dxx`.
2. Chỉ rõ nguồn A, nguồn B, ảnh hưởng.
3. Đề xuất cách xử lý và hỏi người dùng nếu có thay đổi nghiệp vụ/schema.
4. Sau khi chốt, cập nhật `PROJECT_CONTEXT.md`, `business-rules.md` hoặc `decisions.md`.
5. Không sửa code theo phỏng đoán.
