package com.xphone.performance.core.leak;

import java.lang.ref.WeakReference;

/**
 * Runnable an toàn để dùng với Handler.postDelayed().
 *
 * Vấn đề cần tránh: viết "new Runnable() { ... }" bên trong Activity tạo ra một anonymous
 * inner class, ngầm giữ tham chiếu tới Activity. Nếu Runnable được hẹn giờ lâu, Activity
 * cũng bị giữ lại dù người dùng đã đóng màn hình => memory leak.
 *
 * Cách làm an toàn:
 *  1. Dùng lớp STATIC riêng (như lớp này) => không ngầm giữ Activity.
 *  2. Chỉ giữ Activity qua WeakReference => GC vẫn thu hồi được.
 *  3. Kết hợp handler.removeCallbacks(...) trong onDestroy() để hủy hẹn giờ.
 */
public class SafeRunnable<T> implements Runnable {

    /** Việc cần làm với đối tượng (nếu nó còn sống). */
    public interface Action<T> {
        void run(T target);
    }

    private final WeakReference<T> targetRef;
    private final Action<T> action;

    public SafeRunnable(T target, Action<T> action) {
        this.targetRef = new WeakReference<>(target);
        this.action = action;
    }

    @Override
    public void run() {
        T target = targetRef.get();
        if (target != null) {
            action.run(target);
        }
    }
}
