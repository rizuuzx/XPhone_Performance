package com.xphone.performance.ui.benchmark;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.xphone.performance.R;
import com.xphone.performance.benchmark.BenchmarkRunner;
import com.xphone.performance.benchmark.ScoreCalculator;
import com.xphone.performance.data.HistoryStore;
import com.xphone.performance.data.model.BenchmarkResult;
import com.xphone.performance.ui.widget.MascotView;
import com.xphone.performance.ui.widget.ScoreRingView;
import com.xphone.performance.util.FormatUtils;
import com.xphone.performance.util.UiUtils;

import java.util.Locale;

/**
 * Màn hình chạy benchmark và hiển thị kết quả.
 *
 * Cách hoạt động:
 *  1. Hiện trạng thái "đang chạy": linh vật nhún nhảy, thanh tiến độ.
 *  2. BenchmarkRunner chạy 3 bài trên thread nền, mỗi bài báo onProgress => cập nhật tiến độ.
 *  3. onFinished: lưu vào HistoryStore, chuyển sang trạng thái "kết quả":
 *       - vòng điểm chạy số, 3 dòng điểm thành phần kèm số đo gốc,
 *       - so sánh với lần chạy trước (tăng / giảm bao nhiêu %).
 *  4. Nút "Chạy lại" mở lại màn này để đo thêm lần nữa.
 *  5. onDestroy: hủy runner và animation (tránh giữ Activity đã đóng => leak).
 */
public class BenchmarkActivity extends AppCompatActivity {

    private final BenchmarkRunner runner = new BenchmarkRunner();
    private ObjectAnimator bounce;

    private View groupRunning, groupResult;
    private LinearProgressIndicator progress;
    private TextView tvStep, tvStepCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_benchmark);
        UiUtils.setupEdgeToEdge(this, findViewById(R.id.root));

        groupRunning = findViewById(R.id.groupRunning);
        groupResult = findViewById(R.id.groupResult);
        progress = findViewById(R.id.progress);
        tvStep = findViewById(R.id.tvStep);
        tvStepCount = findViewById(R.id.tvStepCount);
        ((TextView) findViewById(R.id.tvRunMode)).setText("Đang chấm điểm máy...");

        startRunning();
    }

    // ===== Trạng thái 1: ĐANG CHẠY =====
    private void startRunning() {
        MascotView runMascot = findViewById(R.id.runMascot);
        runMascot.setMood(MascotView.Mood.HAPPY);

        // Linh vật nhún lên xuống. ObjectAnimator chỉ đổi translationY (thuộc tính được
        // tăng tốc phần cứng), không cần vẽ lại view => rất nhẹ.
        bounce = ObjectAnimator.ofFloat(runMascot, View.TRANSLATION_Y, 0f, -40f);
        bounce.setDuration(400);
        bounce.setRepeatMode(ValueAnimator.REVERSE);
        bounce.setRepeatCount(ValueAnimator.INFINITE);
        bounce.start();

        runner.start(new BenchmarkRunner.Callback() {
            @Override
            public void onProgress(int index, int total, String taskName) {
                progress.setProgressCompat(index, true);
                tvStepCount.setText(String.format(Locale.ROOT, "Bài %d/%d", index + 1, total));
                tvStep.setText(taskName);
            }

            @Override
            public void onFinished(BenchmarkResult result) {
                progress.setProgressCompat(3, true);
                bounce.cancel();
                // Lưu ở thread nền, nhận về lần chạy trước để so sánh
                new HistoryStore(BenchmarkActivity.this).add(result, previous -> {
                    if (!isFinishing() && !isDestroyed()) showResult(result, previous);
                });
            }
        });
    }

    // ===== Trạng thái 2: KẾT QUẢ =====
    private void showResult(BenchmarkResult result, BenchmarkResult previous) {
        groupRunning.setVisibility(View.GONE);
        groupResult.setVisibility(View.VISIBLE);

        ((TextView) findViewById(R.id.tvResultMode)).setText("Kết quả chấm điểm");
        ScoreRingView ring = findViewById(R.id.resultRing);
        ring.setLabel("Điểm tổng");
        ring.setScore(result.total(), ScoreCalculator.MAX_TOTAL, true);

        float ratio = result.total() / (float) ScoreCalculator.MAX_TOTAL;
        ((MascotView) findViewById(R.id.resultMascot)).setMood(
                ratio >= 0.7f ? MascotView.Mood.HAPPY : ratio >= 0.4f ? MascotView.Mood.NORMAL : MascotView.Mood.SWEAT);

        // 3 dòng điểm thành phần
        LinearLayout rows = findViewById(R.id.resultRows);
        rows.removeAllViews();
        addRow(rows, "🌊 Smooth", "Vẽ 1 ảnh: " + FormatUtils.ms(result.smoothMs),
                result.smoothScore, ScoreCalculator.MAX_SMOOTH);
        addRow(rows, "⚡ CPU", "Tìm & sắp xếp 20.000 máy: " + FormatUtils.ms(result.cpuMs),
                result.cpuScore, ScoreCalculator.MAX_CPU);
        addRow(rows, "🧠 Memory", String.format(Locale.GERMANY, "Tốc độ bộ nhớ: %,.0f MB/s", result.memoryMbps),
                result.memoryScore, ScoreCalculator.MAX_MEMORY);

        ((TextView) findViewById(R.id.tvCompare)).setText(buildComparison(result, previous));

        findViewById(R.id.btnHome).setOnClickListener(v -> finish());
        findViewById(R.id.btnRerun).setOnClickListener(v -> {
            startActivity(new Intent(this, BenchmarkActivity.class));
            finish();
        });
    }

    private void addRow(LinearLayout parent, String title, String detail, int score, int max) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_score_row, parent, false);
        ((TextView) row.findViewById(R.id.tvRowTitle)).setText(title);
        ((TextView) row.findViewById(R.id.tvRowDetail)).setText(detail);
        ((TextView) row.findViewById(R.id.tvRowValue)).setText(
                FormatUtils.score(score) + " / " + FormatUtils.score(max));
        parent.addView(row);
    }

    /** So sánh với lần chạy trước. */
    private String buildComparison(BenchmarkResult now, BenchmarkResult before) {
        if (before == null) {
            return "💡 Đây là lần chạy đầu tiên. Hãy chạy thêm vài lần (ví dụ khi máy nguội và khi "
                    + "đang mở nhiều app) để thấy điểm thay đổi thế nào.";
        }
        return "📊 So với lần trước (" + FormatUtils.time(before.timestamp) + ")\n"
                + "• Điểm tổng: " + FormatUtils.score(before.total()) + " → " + FormatUtils.score(now.total())
                + " (" + percent(now.total(), before.total()) + ")\n"
                + "• Smooth: " + percent(now.smoothScore, before.smoothScore) + "\n"
                + "• CPU: " + percent(now.cpuScore, before.cpuScore) + "\n"
                + "• Memory: " + percent(now.memoryScore, before.memoryScore) + "\n"
                + "Điểm dao động vài % là bình thường (nhiệt độ máy, app chạy nền...).";
    }

    private static String percent(int now, int before) {
        double change = (now - before) * 100.0 / Math.max(before, 1);
        return String.format(Locale.GERMANY, "%+.1f%%", change);
    }

    @Override
    protected void onDestroy() {
        runner.cancel();
        if (bounce != null) bounce.cancel();
        super.onDestroy();
    }
}
