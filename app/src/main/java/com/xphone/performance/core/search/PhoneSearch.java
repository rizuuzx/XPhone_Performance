package com.xphone.performance.core.search;

import com.xphone.performance.data.model.Phone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Tìm kiếm điện thoại theo tên/hãng, sắp xếp theo giá, kèm số máy cùng tầm giá.
 *
 * Nguyên tắc tối ưu: việc gì làm 1 lần được thì không làm lại mỗi lần gõ phím.
 *
 *  Khi khởi tạo (chạy 1 lần, ở thread nền) — O(n):
 *   + Tạo sẵn chuỗi tìm kiếm viết thường cho từng máy (searchKeys), để lúc tìm
 *     không phải gọi toLowerCase() và nối chuỗi lại (tránh tạo hàng nghìn object rác => GC).
 *   + Đếm sẵn số máy theo từng tầm giá vào HashMap (bucketCounts).
 *
 *  Khi tìm (mỗi lần truy vấn):
 *   1. Lọc trên chuỗi có sẵn, từ khóa chỉ chuyển về chữ thường 1 lần.   O(n)
 *   2. Sắp xếp bằng Collections.sort (TimSort).                          O(m log m)
 *      (Thuật toán ngây thơ như Bubble Sort là O(m²): 5.000 máy => 12,5 triệu phép so sánh.)
 *   3. Tra số máy cùng tầm giá trong HashMap.                             O(m)
 *      (Nếu duyệt lại toàn bộ cửa hàng cho mỗi máy sẽ là O(m × n).)
 *
 * Nơi gọi (CpuLabActivity) còn chạy hàm này ở thread nền + debounce 300ms.
 * Tính đúng đắn được kiểm chứng bằng unit test PhoneSearchTest.
 */
public class PhoneSearch {

    /** Sắp theo giá tăng dần, cùng giá thì theo id để kết quả luôn ổn định. */
    public static final Comparator<Phone> BY_PRICE =
            (a, b) -> a.price != b.price ? Integer.compare(a.price, b.price) : Integer.compare(a.id, b.id);

    private final List<Phone> all;
    private final String[] searchKeys;
    private final Map<Integer, Integer> bucketCounts = new HashMap<>();

    /** Xây chỉ mục: O(n), chỉ chạy một lần. */
    public PhoneSearch(List<Phone> all) {
        this.all = all;
        searchKeys = new String[all.size()];
        for (int i = 0; i < all.size(); i++) {
            Phone p = all.get(i);
            searchKeys[i] = (p.name + " " + p.brand).toLowerCase(Locale.ROOT);
            Integer old = bucketCounts.get(p.priceBucket());
            bucketCounts.put(p.priceBucket(), old == null ? 1 : old + 1);
        }
    }

    public SearchResult search(String query) {
        long start = System.nanoTime();
        String q = query.toLowerCase(Locale.ROOT).trim();

        // ===== Bước 1: lọc trên chuỗi đã chuẩn bị sẵn =====
        List<Phone> matched = new ArrayList<>();
        for (int i = 0; i < searchKeys.length; i++) {
            if (searchKeys[i].contains(q)) matched.add(all.get(i));
        }

        // ===== Bước 2: sắp xếp O(m log m) =====
        Collections.sort(matched, BY_PRICE);

        // ===== Bước 3: tra bảng băm O(1) cho mỗi máy =====
        int[] counts = new int[matched.size()];
        for (int i = 0; i < matched.size(); i++) {
            Integer c = bucketCounts.get(matched.get(i).priceBucket());
            counts[i] = c == null ? 0 : c;
        }

        double ms = (System.nanoTime() - start) / 1_000_000.0;
        return new SearchResult(matched, counts, ms);
    }
}
