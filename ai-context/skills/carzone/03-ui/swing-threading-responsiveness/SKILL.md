---
name: swing-threading-responsiveness
description: Dùng khi màn hình Swing gọi database, tìm kiếm, tải bảng, xuất file hoặc chạy tác vụ có thể lâu, cần giữ EDT không bị block và xử lý lỗi/hủy đúng cách.
---

# Swing threading và độ phản hồi

## Nguyên tắc

- Mọi thao tác UI chạy trên EDT.
- Mọi tác vụ I/O/DB chạy ngoài EDT.
- Không gọi `DbUtil`, DAO, controller có truy vấn trên EDT.
- Dùng `SwingWorker<T, V>` cho tác vụ nền.

## Mẫu

```java
new SwingWorker<List<XxxRow>, Void>() {
    @Override
    protected List<XxxRow> doInBackground() throws Exception {
        return controller.layDanhSach(...);
    }

    @Override
    protected void done() {
        try {
            List<XxxRow> rows = get();
            model.setRowCount(0);
            for (XxxRow r : rows) model.addRow(...);
            lblTrangThai.setText(rows.isEmpty() ? "Chưa có dữ liệu" : "");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            JOptionPane.showMessageDialog(this, "Lỗi tải dữ liệu: " + cause.getMessage());
        } finally {
            btnTim.setEnabled(true);
            btnLamMoi.setEnabled(true);
        }
    }
}.execute();
```

## Quy tắc trạng thái

- Trước khi chạy: khóa nút submit/tìm, hiện "Đang tải...".
- Trong khi chạy: không cho bấm lặp; có thể hiện progress nếu biết tổng.
- Xong: bật lại nút, cập nhật bảng, hiện thông báo rỗng nếu không có dòng.
- Lỗi: bật lại nút, hiện thông báo thân thiện, giữ dữ liệu đã nhập.
- Không gọi `JOptionPane` trong `doInBackground`.
- Không cập nhật model bảng trong `doInBackground`.

## Hủy và đóng

- Nếu tác vụ dài, lưu `SwingWorker` và gọi `cancel(true)` khi đóng dialog/chuyển trang.
- Kiểm tra `isCancelled()` trong vòng lặp dài.
- Không để worker cũ ghi đè dữ liệu mới của lần tìm sau (dùng token/version nếu cần).
- Khi đóng ứng dụng, dừng worker và đóng tài nguyên.

## Lỗi thường gặp

- Quên bật lại nút trong `finally`.
- Gọi `get()` trên EDT mà không bọc try/catch.
- Nuốt `ExecutionException`, khiến người dùng thấy màn hình trống.
- Dùng `Thread.sleep` trên EDT.
- Chạy song song nhiều worker cùng ghi một bảng.
- Cập nhật UI từ luồng nền.

## Checklist

- [ ] Không DB trên EDT.
- [ ] Có khóa nút khi chạy.
- [ ] Có trạng thái loading/rỗng/lỗi.
- [ ] Bật lại nút trong `finally`.
- [ ] Không dialog trong luồng nền.
- [ ] Có xử lý hủy/đóng nếu tác vụ dài.
- [ ] Không để worker cũ ghi đè kết quả mới.
- [ ] Ngoại lệ được hiển thị, không bị nuốt.

## Liên quan

- `03-ui/swing-ui-development`
- `02-architecture/java-architecture`
- `05-quality/debugging-regression`
