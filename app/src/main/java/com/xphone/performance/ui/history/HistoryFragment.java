package com.xphone.performance.ui.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.xphone.performance.R;
import com.xphone.performance.benchmark.ScoreCalculator;
import com.xphone.performance.data.HistoryStore;
import com.xphone.performance.data.model.BenchmarkResult;
import com.xphone.performance.ui.widget.BarChartView;
import com.xphone.performance.util.FormatUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Màn Lịch sử (giống "bảng xếp hạng" của AnTuTu, nhưng xếp các lần đo trên chính máy này).
 *
 * Cách hoạt động:
 *  - Đọc toàn bộ lịch sử từ HistoryStore.
 *  - Biểu đồ: lấy 8 lần gần nhất, đảo thứ tự để cột cũ bên trái, mới bên phải.
 *    Cột có điểm cao nhất (kỷ lục) tô màu mint, các cột còn lại màu hồng.
 *  - Danh sách: inflate item_history cho từng lần chạy. Tối đa 30 dòng nên dùng
 *    LinearLayout là đủ, không cần RecyclerView.
 */
public class HistoryFragment extends Fragment {

    private static final int CHART_BARS = 8;

    private BarChartView barChart;
    private LinearLayout historyList;
    private TextView tvEmpty;
    private HistoryStore store;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        store = new HistoryStore(requireContext());
        barChart = view.findViewById(R.id.barChart);
        historyList = view.findViewById(R.id.historyList);
        tvEmpty = view.findViewById(R.id.tvEmpty);

        // Hỏi lại trước khi xóa vì thao tác này không hoàn tác được
        view.findViewById(R.id.btnClear).setOnClickListener(v ->
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Xóa lịch sử?")
                        .setMessage("Toàn bộ các lần đo sẽ bị xóa và không thể khôi phục.")
                        .setNegativeButton("Hủy", null)
                        .setPositiveButton("Xóa", (dialog, which) -> store.clear(this::reload))
                        .show());
        reload();
    }

    /** Đọc lịch sử ở thread nền rồi vẽ lại; bỏ qua nếu Fragment đã đóng. */
    private void reload() {
        store.loadAll(all -> {
            if (getView() != null) render(all);
        });
    }

    private void render(List<BenchmarkResult> all) {
        tvEmpty.setVisibility(all.isEmpty() ? View.VISIBLE : View.GONE);
        requireView().findViewById(R.id.btnClear).setVisibility(all.isEmpty() ? View.GONE : View.VISIBLE);
        barChart.setVisibility(all.isEmpty() ? View.GONE : View.VISIBLE);

        int normalColor = ContextCompat.getColor(requireContext(), R.color.xp_primary);
        int bestColor = ContextCompat.getColor(requireContext(), R.color.xp_good);

        // Tìm điểm cao nhất trong toàn bộ lịch sử
        int best = 0;
        for (BenchmarkResult r : all) best = Math.max(best, r.total());

        // ===== Biểu đồ cột =====
        List<BarChartView.Bar> bars = new ArrayList<>();
        int count = Math.min(CHART_BARS, all.size());
        for (int i = count - 1; i >= 0; i--) {
            BenchmarkResult r = all.get(i);
            // Nhãn "#N": số thứ tự lần đo, lần cũ nhất trong lịch sử là #1
            bars.add(new BarChartView.Bar("#" + (all.size() - i), r.total(),
                    r.total() == best ? bestColor : normalColor));
        }
        barChart.setBars(bars, ScoreCalculator.MAX_TOTAL);

        // ===== Danh sách chi tiết =====
        historyList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (BenchmarkResult r : all) {
            View row = inflater.inflate(R.layout.item_history, historyList, false);
            TextView tvTitle = row.findViewById(R.id.tvHistMode);
            tvTitle.setText(r.total() == best ? "🏆 Kỷ lục" : "Lần đo");
            tvTitle.setTextColor(r.total() == best ? bestColor : normalColor);
            ((TextView) row.findViewById(R.id.tvHistTime)).setText(FormatUtils.time(r.timestamp));
            ((TextView) row.findViewById(R.id.tvHistTotal)).setText(FormatUtils.score(r.total()));
            ((TextView) row.findViewById(R.id.tvHistParts)).setText(
                    "🌊 " + FormatUtils.score(r.smoothScore)
                            + "  ⚡ " + FormatUtils.score(r.cpuScore)
                            + "  🧠 " + FormatUtils.score(r.memoryScore));
            historyList.addView(row);
        }
    }
}
