package com.xphone.performance.ui.labs.memory;

import android.os.Bundle;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.xphone.performance.R;
import com.xphone.performance.core.leak.LeakTracker;
import com.xphone.performance.monitor.MemoryMonitor;
import com.xphone.performance.ui.labs.BaseLabActivity;
import com.xphone.performance.util.FormatUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Kiểm tra rò rỉ bộ nhớ (Memory Leak): app tự chứng minh các màn hình đã đóng được giải phóng.
 *
 * Cách hoạt động:
 *  - "Mở màn hình 8MB" : mở 1 HeavyActivity, bấm Back để đóng.
 *  - "Tự mở/đóng ×5"   : HeavyActivity tự mở nối tiếp 5 lần rồi tự đóng (tổng 40MB đã cấp phát).
 *  - "Dọn rác & kiểm tra rò rỉ": chạy GC ở thread nền, rồi LeakTracker đếm số HeavyActivity
 *    còn sống. Lúc này mọi HeavyActivity đã đóng, nên kết quả đúng phải là 0.
 *    Nếu > 0 => có leak (hiện màu cam và linh vật buồn).
 *  - onResume tự động kiểm tra lại, nên quay về từ HeavyActivity là thấy kết quả ngay.
 *
 * Kiểm chứng bằng công cụ:
 *  - Android Studio Profiler > Memory: RAM tăng ~8MB mỗi lần mở, rồi tụt về sau khi GC
 *    (không có kiểu tăng "bậc thang" mãi mãi).
 *  - "Capture heap dump", lọc "HeavyActivity" => 0 instance sau khi đóng.
 *  - LeakCanary (bản debug) không hiện thông báo nào.
 */
public class MemoryLabActivity extends BaseLabActivity {

    private final ExecutorService gcWorker = Executors.newSingleThreadExecutor();
    private int aliveCount;

    private TextView tvAlive, tvHeap, tvLog;
    private final StringBuilder log = new StringBuilder();

    @Override
    protected int layoutId() {
        return R.layout.activity_lab_memory;
    }

    @Override
    protected String title() {
        return "Kiểm tra rò rỉ bộ nhớ";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tvAlive = findViewById(R.id.tvAlive);
        tvHeap = findViewById(R.id.tvHeap);
        tvLog = findViewById(R.id.tvLog);

        findViewById(R.id.btnOpenOne).setOnClickListener(v -> {
            appendLog("Mở 1 màn hình 8MB");
            startActivity(HeavyActivity.intent(this, 0));
        });
        findViewById(R.id.btnOpenAuto).setOnClickListener(v -> {
            appendLog("Tự mở/đóng 5 màn hình 8MB");
            startActivity(HeavyActivity.intent(this, 5));
        });
        findViewById(R.id.btnGc).setOnClickListener(v -> gcAndCount());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Chờ 1 giây rồi mới kiểm tra: khi quay về từ HeavyActivity, Android gọi onResume của
        // màn này TRƯỚC khi HeavyActivity cuối cùng kịp onDestroy. Đếm ngay sẽ ra "1 màn hình
        // còn sống" dù thực tế không hề leak (báo động giả do thời điểm).
        mainHandler.removeCallbacks(delayedCheck);
        mainHandler.postDelayed(delayedCheck, 1000);
    }

    private final Runnable delayedCheck = this::gcAndCount;

    /** Chạy GC ở thread nền (GC + chờ có thể mất vài trăm ms), xong mới cập nhật UI. */
    private void gcAndCount() {
        tvHeap.setText("🧹 Đang dọn rác...");
        gcWorker.execute(() -> {
            MemoryMonitor.forceGc();
            int alive = LeakTracker.aliveCount();
            long heap = MemoryMonitor.usedHeapBytes();
            mainHandler.post(() -> showLeakResult(alive, heap));
        });
    }

    private void showLeakResult(int alive, long heapBytes) {
        aliveCount = alive;
        tvAlive.setText(String.valueOf(alive));
        tvAlive.setTextColor(ContextCompat.getColor(this, alive == 0 ? R.color.xp_good : R.color.xp_bad));
        tvHeap.setText("Heap sau GC: " + FormatUtils.mb(heapBytes)
                + (alive == 0 ? " · Không có rò rỉ ✅" : " · Phát hiện rò rỉ ⚠️"));
        appendLog("GC xong → " + alive + " màn hình còn trong bộ nhớ");
        refreshExtraMetric();
    }

    private void appendLog(String line) {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.ROOT).format(new Date());
        log.insert(0, time + "  " + line + "\n");
        tvLog.setText(log);
    }

    // ===== Ô thứ 4 & giải thích =====

    @Override
    protected String extraMetricLabel() {
        return "Bị leak";
    }

    @Override
    protected String extraMetricValue() {
        return String.valueOf(aliveCount);
    }

    @Override
    protected String explanation() {
        return "💡 Kỹ thuật chống rò rỉ bộ nhớ\n"
                + "GC chỉ thu hồi đối tượng không còn ai tham chiếu tới. Vì vậy app:\n"
                + "• Không lưu Activity vào biến static / Singleton (chỉ dùng applicationContext)\n"
                + "• Hẹn giờ bằng Runnable tĩnh + WeakReference, không dùng Runnable ẩn danh\n"
                + "• Hủy hẹn giờ, dừng thread nền, gỡ listener trong onDestroy()\n"
                + "• Bitmap có giới hạn qua LruCache\n"
                + "=> Đóng màn hình là bộ nhớ được trả lại, số \"còn trong bộ nhớ\" luôn về 0.";
    }

    @Override
    protected void onDestroy() {
        gcWorker.shutdownNow();
        super.onDestroy();
    }
}
