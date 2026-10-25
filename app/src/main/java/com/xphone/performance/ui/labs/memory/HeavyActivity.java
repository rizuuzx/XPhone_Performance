package com.xphone.performance.ui.labs.memory;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.xphone.performance.R;
import com.xphone.performance.core.leak.LeakTracker;
import com.xphone.performance.core.leak.SafeRunnable;

/**
 * Màn hình "nặng" giữ 8MB dữ liệu (giống màn xem ảnh / video), dùng để TỰ KIỂM TRA rò rỉ bộ nhớ:
 * mở rồi đóng nhiều lần, sau đó Memory Lab chạy GC và đếm xem còn bao nhiêu màn hình sống.
 *
 * Các nguyên tắc chống leak được áp dụng:
 *  1. Không lưu Activity vào biến static hay Singleton nào.
 *  2. Hẹn giờ bằng SafeRunnable (lớp tĩnh + WeakReference) thay vì "new Runnable(){}" ẩn danh,
 *     vì Runnable ẩn danh ngầm giữ tham chiếu tới Activity.
 *  3. onDestroy(): handler.removeCallbacksAndMessages(null) hủy mọi hẹn giờ còn chờ.
 *  => Đóng màn hình là 8MB được GC thu hồi; LeakCanary cũng sẽ không báo gì.
 *
 * Tham số "remaining" dùng cho nút "Tự mở/đóng ×5": màn hình tự đóng sau 0,4 giây
 * và mở màn hình kế tiếp cho tới khi đủ số lần.
 */
public class HeavyActivity extends AppCompatActivity {

    private static final String EXTRA_REMAINING = "remaining";
    private static final int PAYLOAD_BYTES = 8 * 1024 * 1024; // 8MB
    private static final long AUTO_CLOSE_MS = 400;

    /** Dữ liệu "nặng" giả lập (ảnh, cache...) để thấy rõ trên biểu đồ RAM của Profiler. */
    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private byte[] heavyPayload;

    private final Handler handler = new Handler(Looper.getMainLooper());

    public static Intent intent(Context context, int remaining) {
        return new Intent(context, HeavyActivity.class).putExtra(EXTRA_REMAINING, remaining);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        heavyPayload = new byte[PAYLOAD_BYTES];
        LeakTracker.track(this); // để Memory Lab đếm được màn hình này còn sống hay không

        // Giao diện đơn giản, tạo bằng code
        TextView tv = new TextView(this);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(18);
        tv.setTextColor(ContextCompat.getColor(this, R.color.xp_text));
        tv.setBackgroundColor(ContextCompat.getColor(this, R.color.xp_surface_soft));
        tv.setText("🐘 Màn hình nặng 8MB\n\nBấm Back để đóng");
        setContentView(tv);

        scheduleAutoClose();
    }

    /** Chế độ tự mở/đóng: sau 0,4 giây mở màn hình kế tiếp (nếu còn) rồi tự đóng. */
    private void scheduleAutoClose() {
        int remaining = getIntent().getIntExtra(EXTRA_REMAINING, 0);
        if (remaining <= 0) return;
        handler.postDelayed(new SafeRunnable<>(this, activity -> {
            if (remaining > 1) {
                activity.startActivity(intent(activity, remaining - 1));
            }
            activity.finish();
        }), AUTO_CLOSE_MS);
    }

    @Override
    protected void onDestroy() {
        // Hủy mọi hẹn giờ còn chờ để không có gì giữ Activity sau khi đóng
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
