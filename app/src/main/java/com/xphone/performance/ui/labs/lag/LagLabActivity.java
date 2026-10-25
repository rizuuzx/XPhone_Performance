package com.xphone.performance.ui.labs.lag;

import android.os.Bundle;
import android.widget.Button;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xphone.performance.R;
import com.xphone.performance.core.image.ImageLoader;
import com.xphone.performance.data.FakePhoneRepository;
import com.xphone.performance.monitor.BindStats;
import com.xphone.performance.ui.labs.BaseLabActivity;
import com.xphone.performance.ui.labs.PhoneAdapter;
import com.xphone.performance.util.FormatUtils;

/**
 * Kiểm tra độ mượt (Smooth): cuộn danh sách 300 điện thoại có ảnh, đo FPS thực tế.
 *
 * Cách hoạt động:
 *  - RecyclerView + PhoneAdapter + ImageLoader (xem giải thích chi tiết trong từng lớp).
 *  - Nút "Tự động cuộn": mỗi frame cuộn 14px (đảo chiều khi chạm đáy/đỉnh). Dùng
 *    postOnAnimation() để bước cuộn khớp đúng nhịp vẽ của màn hình, không dư không thiếu.
 *  - Thanh đo: FPS, số lần giật, heap, và thời gian bind trung bình 1 dòng (ms).
 *    Mục tiêu: FPS ~60 (hoặc bằng tần số quét của máy), bind < 1ms, số lần giật gần như không tăng.
 *
 * Kiểm chứng bằng công cụ: Developer options > Profile GPU Rendering (các cột phải nằm
 * dưới vạch xanh 16ms), hoặc Android Studio Profiler > CPU > System Trace.
 */
public class LagLabActivity extends BaseLabActivity {

    private static final int PHONE_COUNT = 300;
    private static final int SCROLL_STEP_PX = 14;

    private final BindStats bindStats = new BindStats();
    private ImageLoader imageLoader;

    private RecyclerView recycler;
    private Button btnAutoScroll;
    private boolean autoScrolling;
    private int scrollDirection = 1;

    @Override
    protected int layoutId() {
        return R.layout.activity_lab_lag;
    }

    @Override
    protected String title() {
        return "Kiểm tra độ mượt";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imageLoader = new ImageLoader();

        recycler = findViewById(R.id.recyclerPhones);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setHasFixedSize(true); // kích thước RecyclerView không đổi theo nội dung => bỏ bớt bước đo layout
        recycler.setAdapter(new PhoneAdapter(FakePhoneRepository.generate(PHONE_COUNT), imageLoader, bindStats));

        btnAutoScroll = findViewById(R.id.btnAutoScroll);
        btnAutoScroll.setOnClickListener(v -> toggleAutoScroll());
    }

    // ===== Tự động cuộn: chạy mỗi frame bằng postOnAnimation =====

    private final Runnable scrollStep = new Runnable() {
        @Override
        public void run() {
            if (!autoScrolling) return;
            if (!recycler.canScrollVertically(scrollDirection)) scrollDirection = -scrollDirection;
            recycler.scrollBy(0, SCROLL_STEP_PX * scrollDirection);
            recycler.postOnAnimation(this);
        }
    };

    private void toggleAutoScroll() {
        autoScrolling = !autoScrolling;
        btnAutoScroll.setText(autoScrolling ? "⏸ Dừng cuộn" : "▶ Tự động cuộn");
        if (autoScrolling) scrollStep.run();
    }

    @Override
    protected void onPause() {
        // Dừng cuộn khi rời màn hình
        if (autoScrolling) toggleAutoScroll();
        super.onPause();
    }

    // ===== Ô thứ 4 & giải thích =====

    @Override
    protected String extraMetricLabel() {
        return "Bind TB";
    }

    @Override
    protected String extraMetricValue() {
        return FormatUtils.ms(bindStats.drainAverageMs());
    }

    @Override
    protected String explanation() {
        return "💡 Kỹ thuật giúp cuộn mượt\n"
                + "Mỗi frame chỉ có 16,7ms (60Hz), nên main thread không được làm việc nặng.\n"
                + "• RecyclerView + ViewHolder: tái sử dụng view, không findViewById khi cuộn\n"
                + "• Ảnh 160×160 vừa đủ ô 56dp, không vẽ ảnh to rồi thu nhỏ\n"
                + "• Vẽ ảnh ở thread nền, main thread chỉ gán kết quả\n"
                + "• LruCache: cuộn ngược lại là có ảnh ngay\n"
                + "=> Bind mỗi dòng < 1ms, FPS giữ ổn định.";
    }

    @Override
    protected void onDestroy() {
        autoScrolling = false;
        imageLoader.shutdown();
        super.onDestroy();
    }
}
