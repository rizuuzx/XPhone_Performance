package com.xphone.performance.ui.labs;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.xphone.performance.R;
import com.xphone.performance.monitor.CpuUsageMonitor;
import com.xphone.performance.monitor.FrameMonitor;
import com.xphone.performance.monitor.MemoryMonitor;
import com.xphone.performance.ui.widget.MascotView;
import com.xphone.performance.util.UiUtils;

import java.util.Locale;

/**
 * Lớp cha của 3 màn kiểm tra trực tiếp. Gom phần dùng chung để mỗi màn chỉ viết phần riêng.
 *
 * Phần dùng chung:
 *  - Header: nút quay lại, tiêu đề, linh vật.
 *  - Thanh đo trực tiếp (cập nhật mỗi giây):
 *      + FPS và số lần giật: lấy từ FrameMonitor (Choreographer).
 *      + % CPU của app: lấy từ CpuUsageMonitor.
 *      + Heap: đọc từ MemoryMonitor.
 *      + Ô thứ 4: màn con tự quyết định qua extraMetricLabel() / extraMetricValue().
 *  - Thẻ "💡 Kỹ thuật tối ưu": nội dung lấy từ explanation() của màn con.
 *  - Linh vật: FPS >= 50 vui, >= 30 bình thường, thấp hơn đổ mồ hôi.
 *
 * Vòng đời:
 *  - onResume  => bắt đầu đo (FrameMonitor.start)
 *  - onPause   => dừng đo, không tốn tài nguyên khi màn hình bị che
 *  - onDestroy => gỡ mọi tin nhắn đang chờ trong mainHandler (tránh giữ Activity => leak)
 */
public abstract class BaseLabActivity extends AppCompatActivity implements FrameMonitor.Listener {

    /** Handler main thread dùng chung cho các màn con. */
    protected final Handler mainHandler = new Handler(Looper.getMainLooper());

    private FrameMonitor frameMonitor;
    private final CpuUsageMonitor cpuMonitor = new CpuUsageMonitor();
    private MascotView mascot;
    private TextView tvFps, tvCpu, tvJank, tvRam, tvExtraValue;

    // ===== Các hàm màn con cần cài đặt =====

    @LayoutRes
    protected abstract int layoutId();

    protected abstract String title();

    /** Nội dung thẻ "Kỹ thuật tối ưu". */
    protected abstract String explanation();

    protected abstract String extraMetricLabel();

    /** Giá trị ô thứ 4, được gọi mỗi giây. */
    protected abstract String extraMetricValue();

    // ===== Khởi tạo phần dùng chung =====

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layoutId());
        UiUtils.setupEdgeToEdge(this, findViewById(R.id.root));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.tvLabTitle)).setText(title());
        ((TextView) findViewById(R.id.tvExtraLabel)).setText(extraMetricLabel());
        ((TextView) findViewById(R.id.tvWhy)).setText(explanation());
        mascot = findViewById(R.id.labMascot);
        tvFps = findViewById(R.id.tvFps);
        tvCpu = findViewById(R.id.tvCpu);
        tvJank = findViewById(R.id.tvJank);
        tvRam = findViewById(R.id.tvRam);
        tvExtraValue = findViewById(R.id.tvExtraValue);

        frameMonitor = new FrameMonitor(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        frameMonitor.start();
    }

    @Override
    protected void onPause() {
        frameMonitor.stop();
        super.onPause();
    }

    // ===== Nhận số liệu mỗi giây từ FrameMonitor =====

    @Override
    public void onFrameStats(int fps, int totalJank, float worstFrameMs) {
        tvFps.setText(String.valueOf(fps));
        tvFps.setTextColor(ContextCompat.getColor(this, fps >= 50 ? R.color.xp_good : R.color.xp_bad));
        tvCpu.setText(String.format(Locale.ROOT, "%.0f%%", cpuMonitor.sample()));
        tvJank.setText(String.valueOf(totalJank));
        tvRam.setText(String.format(Locale.ROOT, "%.0f", MemoryMonitor.usedHeapBytes() / 1024f / 1024f));
        tvExtraValue.setText(extraMetricValue());

        if (fps >= 50) {
            mascot.setMood(MascotView.Mood.HAPPY);
        } else if (fps >= 30) {
            mascot.setMood(MascotView.Mood.NORMAL);
        } else {
            mascot.setMood(MascotView.Mood.SWEAT);
        }
    }

    /** Màn con gọi khi muốn cập nhật ngay ô thứ 4 (không đợi tới giây kế tiếp). */
    protected void refreshExtraMetric() {
        tvExtraValue.setText(extraMetricValue());
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
