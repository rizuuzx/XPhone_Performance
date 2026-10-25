package com.xphone.performance.core.leak;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * "Máy đếm" số đối tượng còn sống trong bộ nhớ, dùng WeakReference. App dùng nó để TỰ KIỂM TRA
 * rằng các màn hình đã đóng thật sự được giải phóng (không bị memory leak).
 *
 * Kiến thức nền:
 *  - Tham chiếu thường (strong reference) giữ đối tượng sống.
 *  - WeakReference KHÔNG giữ đối tượng sống: khi không còn ai giữ strong reference,
 *    GC thu hồi đối tượng và weakRef.get() trả về null.
 *
 * Cách hoạt động:
 *  - Mỗi HeavyActivity khi tạo ra sẽ gọi track(this).
 *  - Sau khi đóng màn hình và chạy GC, aliveCount() đếm số weakRef vẫn còn trỏ tới đối tượng.
 *    Màn hình đã đóng mà vẫn "còn sống" => có ai đó đang giữ nó => LEAK.
 *  - LeakCanary cũng hoạt động theo đúng nguyên lý này (WeakReference + ReferenceQueue).
 */
public final class LeakTracker {

    private static final List<WeakReference<Object>> REFS = new ArrayList<>();

    private LeakTracker() {
    }

    public static synchronized void track(Object object) {
        REFS.add(new WeakReference<>(object));
    }

    /** Đếm số đối tượng còn sống, đồng thời dọn các weakRef đã rỗng. */
    public static synchronized int aliveCount() {
        int alive = 0;
        Iterator<WeakReference<Object>> it = REFS.iterator();
        while (it.hasNext()) {
            if (it.next().get() == null) {
                it.remove();
            } else {
                alive++;
            }
        }
        return alive;
    }
}
