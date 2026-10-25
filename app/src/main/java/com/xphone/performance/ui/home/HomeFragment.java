package com.xphone.performance.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.xphone.performance.R;
import com.xphone.performance.benchmark.ScoreCalculator;
import com.xphone.performance.data.HistoryStore;
import com.xphone.performance.data.model.BenchmarkResult;
import com.xphone.performance.ui.benchmark.BenchmarkActivity;
import com.xphone.performance.ui.labs.cpu.CpuLabActivity;
import com.xphone.performance.ui.labs.lag.LagLabActivity;
import com.xphone.performance.ui.labs.memory.MemoryLabActivity;
import com.xphone.performance.ui.widget.MascotView;
import com.xphone.performance.ui.widget.ScoreRingView;
import com.xphone.performance.util.FormatUtils;

/**
 * Trang chủ kiểu AnTuTu.
 *
 * Cách hoạt động:
 *  - onResume(): đọc kết quả mới nhất từ HistoryStore (ở thread nền) rồi hiển thị lên vòng điểm và 3 thẻ.
 *    (dùng onResume để khi chạy benchmark xong quay về, số liệu tự cập nhật)
 *  - Bấm "Bắt đầu kiểm tra" => mở BenchmarkActivity.
 *  - Bấm vào thẻ Smooth / CPU / Memory => mở màn kiểm tra trực tiếp tương ứng.
 *  - Linh vật đổi biểu cảm theo điểm: >= 70% vui, >= 40% bình thường, thấp hơn thì buồn.
 */
public class HomeFragment extends Fragment {

    private ScoreRingView scoreRing;
    private MascotView mascot;
    private TextView tvGreeting, tvLastRun, tvSmooth, tvCpu, tvMemory;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        scoreRing = view.findViewById(R.id.scoreRing);
        mascot = view.findViewById(R.id.homeMascot);
        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvLastRun = view.findViewById(R.id.tvLastRun);
        tvSmooth = view.findViewById(R.id.tvSmoothScore);
        tvCpu = view.findViewById(R.id.tvCpuScore);
        tvMemory = view.findViewById(R.id.tvMemoryScore);

        view.findViewById(R.id.btnStart).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), BenchmarkActivity.class)));

        // ===== 3 thẻ điểm: mở màn kiểm tra trực tiếp =====
        view.findViewById(R.id.cardSmooth).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), LagLabActivity.class)));
        view.findViewById(R.id.cardCpu).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CpuLabActivity.class)));
        view.findViewById(R.id.cardMemory).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), MemoryLabActivity.class)));
    }

    @Override
    public void onResume() {
        super.onResume();
        bindLatestResult();
    }

    private void bindLatestResult() {
        // Đọc lịch sử ở thread nền; khi có kết quả thì Fragment có thể đã bị đóng => kiểm tra getView()
        new HistoryStore(requireContext()).loadLatest(latest -> {
            if (getView() != null) showResult(latest);
        });
    }

    private void showResult(BenchmarkResult latest) {
        if (latest == null) {
            scoreRing.setScore(0, ScoreCalculator.MAX_TOTAL, false);
            scoreRing.setLabel("Chưa có điểm");
            tvGreeting.setText("Xin chào! Mình là Mèo XPhone 🐾\nBấm \"Bắt đầu kiểm tra\" để chấm điểm máy của bạn nhé.");
            tvLastRun.setText("");
            mascot.setMood(MascotView.Mood.NORMAL);
            return;
        }

        scoreRing.setScore(latest.total(), ScoreCalculator.MAX_TOTAL, true);
        scoreRing.setLabel("Điểm tổng");
        tvLastRun.setText("Lần chạy gần nhất: " + FormatUtils.time(latest.timestamp));
        tvSmooth.setText(FormatUtils.score(latest.smoothScore));
        tvCpu.setText(FormatUtils.score(latest.cpuScore));
        tvMemory.setText(FormatUtils.score(latest.memoryScore));

        float ratio = latest.total() / (float) ScoreCalculator.MAX_TOTAL;
        if (ratio >= 0.7f) {
            mascot.setMood(MascotView.Mood.HAPPY);
            tvGreeting.setText("Máy bạn khỏe ghê! 😸");
        } else if (ratio >= 0.4f) {
            mascot.setMood(MascotView.Mood.NORMAL);
            tvGreeting.setText("Máy chạy ổn định, đủ dùng mượt mà.");
        } else {
            mascot.setMood(MascotView.Mood.SWEAT);
            tvGreeting.setText("Máy hơi đuối rồi... Thử đóng bớt app nền rồi đo lại nhé 😿");
        }
    }
}
