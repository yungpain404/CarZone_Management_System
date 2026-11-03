---
name: testing-acceptance
description: Dùng khi viết test, kiểm thử acceptance, kiểm tra quyền/trạng thái/tiền/tồn kho hoặc chuẩn bị bằng chứng demo cho một use case CarZone.
---

# Kiểm thử và acceptance

## Hiện trạng

- `pom.xml` có `junit-jupiter` 5.10.2.
- Chưa có thư mục test, chưa có test nào.
- Chưa cấu hình `maven-surefire-plugin` 3.x; `mvn test` có thể không chạy JUnit 5.
- Chưa có DB test riêng.

Không tuyên bố "đã test" nếu chưa thực sự chạy.

## Cấu hình tối thiểu khi thêm test

1. Không đặt test dưới `src/test/java` khi `sourceDirectory=src`: main compile sẽ quét luôn thư mục đó. Chọn một trong hai:
   - Thêm `<testSourceDirectory>test/java</testSourceDirectory>` và đặt test dưới `test/java` (khuyến nghị, ít xâm lấn).
   - Giữ `src/test/java` nhưng thêm `<exclude>test/**</exclude>` cho `maven-compiler-plugin` để main compile bỏ qua.
2. Thêm `maven-surefire-plugin` 3.x vào `pom.xml`.
3. Giữ `junit-jupiter` scope `test`.
4. Không để test lọt vào jar main.
5. Không commit dữ liệu test vào migration/seed chính.

## Tầng test

### Unit test (ưu tiên)

- Entity validation.
- Controller với DAO giả (nếu tách được interface).
- Hàm tính toán/format.
- Mapping enum/trạng thái.
- Không cần DB.

### Integration test (khi cần)

- DAO/procedure/trigger với DB thật.
- Chỉ chạy trên DB local/disposable; không dùng dữ liệu thật.
- Dùng transaction rollback hoặc dọn dữ liệu theo tiền tố test.
- Không phụ thuộc thứ tự test.
- Gắn tag/bật thủ công nếu chạy chậm.

### Acceptance thủ công

- Chạy ứng dụng với DB local.
- Kiểm tra theo kịch bản UC.
- Ghi bằng chứng: thao tác, dữ liệu, kết quả, ảnh nếu cần.

## Mẫu test

```java
@Test
void khongChoLapHoaDonKhiPhieuChuaHoanTat() {
    // given: phiếu DANG_SUA
    // when: lập hóa đơn
    // then: trả lỗi, không tạo HoaDon
}
```

Tên test tiếng Việt không dấu hoặc tiếng Anh, nhất quán. Mỗi test một hành vi.

## Ma trận test tối thiểu

| Loại | Nội dung |
|---|---|
| Quyền | Đúng vai trò; sai vai trò; TNV/KTV truy cập dữ liệu người khác |
| Trạng thái | Chuyển hợp lệ; chuyển sai; trạng thái cuối; sửa bản ghi đã khóa |
| Dữ liệu | Rỗng, NULL, trùng, quá dài, sai định dạng, biên |
| Tiền | Chỉ DV, chỉ LK, cả hai, không có gì; khuyến mại 0/100/không áp dụng |
| Tồn kho | Đủ, vừa đủ, vượt, tồn 0, nhập kho hai lần |
| Thời gian | Trong 7 ngày, đúng ngày thứ 7, quá 7 ngày; ngày biên thống kê |
| DB | Lỗi trigger/procedure được dịch đúng |
| UI | Loading, rỗng, lỗi, resize, đóng giữa chừng |

## Dữ liệu test

- Dùng tiền tố dễ nhận: `TEST_`, `KH999...`, biển số `TEST-...`.
- Không dùng CCCD/SĐT thật.
- Không sửa dữ liệu demo của nhóm.
- Dọn dữ liệu sau test nếu chạy trên DB dùng chung.
- Với trigger, nhớ rằng xóa header có thể cascade chi tiết; kiểm tra tác động.

## Acceptance theo UC

Mỗi UC phải có:

1. Tiền điều kiện.
2. Luồng thành công.
3. Luồng bị chặn (quyền/trạng thái/dữ liệu).
4. Kết quả dữ liệu sau thao tác.
5. Bằng chứng chạy.

Ví dụ UC015:

- Phiếu `HOAN_TAT` -> lập HĐ thành công, tổng tiền đúng view.
- Phiếu `DANG_SUA` -> bị chặn.
- Lập HĐ lần hai -> bị chặn.
- Chuyển khoản thiếu mã giao dịch -> bị chặn.
- Thanh toán -> trạng thái `DA_THANH_TOAN`, không sửa được.

## Báo cáo

```text
Đã chạy: mvn -q compile / mvn test / thủ công
Kết quả: pass/fail/số test
Kịch bản đã kiểm:
Chưa kiểm:
Dữ liệu dùng:
Rủi ro:
```

## Checklist

- [ ] Test đặt đúng chỗ.
- [ ] Surefire JUnit 5 được cấu hình nếu chạy `mvn test`.
- [ ] Có test quyền.
- [ ] Có test trạng thái.
- [ ] Có test biên.
- [ ] Có test lỗi DB.
- [ ] Không dùng dữ liệu thật.
- [ ] Có bằng chứng chạy.
- [ ] Báo cáo trung thực phần chưa kiểm.

## Liên quan

- `05-quality/debugging-regression`
- `05-quality/code-review-completion`
- `workflows/implement-usecase.md`
