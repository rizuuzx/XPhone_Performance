package com.xphone.performance.ui.log;

import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.xphone.performance.R;
import com.xphone.performance.data.OptimizationLog;
import com.xphone.performance.data.model.OptimizationEntry;
import com.xphone.performance.util.UiUtils;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Màn "Nhật ký tối ưu": liệt kê các vấn đề hiệu năng đã phát hiện và sửa trong quá trình làm app.
 *
 * Cách hoạt động:
 *  - Lấy danh sách cố định từ OptimizationLog.all().
 *  - Thẻ tóm tắt: tổng số vấn đề + số vấn đề theo từng nhóm (Lag / CPU / Bộ nhớ / Build).
 *  - Mỗi mục hiện tiêu đề, công cụ phát hiện và số đo TRƯỚC → SAU.
 *    Bấm vào thẻ để mở/đóng phần chi tiết: triệu chứng, nguyên nhân, cách sửa, file mã nguồn.
 *  - Chỉ có vài mục nên dùng LinearLayout trong ScrollView, không cần RecyclerView.
 */
public class OptimizationLogActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_optimization_log);
        UiUtils.setupEdgeToEdge(this, findViewById(R.id.root));
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        List<OptimizationEntry> entries = OptimizationLog.all();
        bindSummary(entries);

        LinearLayout list = findViewById(R.id.logList);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (OptimizationEntry e : entries) {
            list.addView(createItem(inflater, list, e));
        }
    }

    // ===== Thẻ tóm tắt =====
    private void bindSummary(List<OptimizationEntry> entries) {
        Map<OptimizationEntry.Category, Integer> counts = new EnumMap<>(OptimizationEntry.Category.class);
        for (OptimizationEntry e : entries) {
            Integer old = counts.get(e.category);
            counts.put(e.category, old == null ? 1 : old + 1);
        }
        StringBuilder detail = new StringBuilder();
        for (Map.Entry<OptimizationEntry.Category, Integer> c : counts.entrySet()) {
            if (detail.length() > 0) detail.append("  ·  ");
            detail.append(c.getKey().label).append(": ").append(c.getValue());
        }
        ((TextView) findViewById(R.id.tvSummary)).setText(
                "🔧 " + entries.size() + " vấn đề đã phát hiện và xử lý");
        ((TextView) findViewById(R.id.tvSummaryDetail)).setText(detail
                + "\nĐo trên máy ảo Pixel 5 · StrictMode, Android Lint, LeakTracker, instrumented test");
    }

    // ===== Một mục nhật ký =====
    private View createItem(LayoutInflater inflater, LinearLayout parent, OptimizationEntry e) {
        View card = inflater.inflate(R.layout.item_optimization, parent, false);
        ((TextView) card.findViewById(R.id.tvCategory)).setText(e.category.label);
        ((TextView) card.findViewById(R.id.tvTitle)).setText(e.title);
        ((TextView) card.findViewById(R.id.tvTool)).setText("🔍 Phát hiện bằng: " + e.tool);
        ((TextView) card.findViewById(R.id.tvBefore)).setText("TRƯỚC\n" + e.before);
        ((TextView) card.findViewById(R.id.tvAfter)).setText("SAU\n" + e.after);

        TextView detail = card.findViewById(R.id.tvDetail);
        detail.setText(buildDetail(e));
        TextView toggle = card.findViewById(R.id.tvToggle);

        // Bấm vào thẻ để mở/đóng phần chi tiết
        card.setOnClickListener(v -> {
            boolean show = detail.getVisibility() != View.VISIBLE;
            detail.setVisibility(show ? View.VISIBLE : View.GONE);
            toggle.setText(show ? "Thu gọn ▴" : "Xem chi tiết ▾");
        });
        return card;
    }

    /** Ghép phần chi tiết, in đậm các tiêu đề nhỏ. */
    private CharSequence buildDetail(OptimizationEntry e) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        appendSection(sb, "Triệu chứng: ", e.symptom);
        appendSection(sb, "Nguyên nhân: ", e.cause);
        appendSection(sb, "Cách sửa: ", e.fix);
        appendSection(sb, "File: ", e.sourceFile);
        return sb;
    }

    private void appendSection(SpannableStringBuilder sb, String label, String text) {
        if (sb.length() > 0) sb.append("\n\n");
        int start = sb.length();
        sb.append(label);
        sb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.append(text);
    }
}
