package com.xphone.performance.ui.labs.cpu;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xphone.performance.R;
import com.xphone.performance.core.image.ImageLoader;
import com.xphone.performance.core.search.PhoneSearch;
import com.xphone.performance.core.search.SearchResult;
import com.xphone.performance.data.FakePhoneRepository;
import com.xphone.performance.ui.labs.BaseLabActivity;
import com.xphone.performance.ui.labs.PhoneAdapter;
import com.xphone.performance.util.FormatUtils;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Kiểm tra CPU: tìm kiếm trực tiếp trong cửa hàng 20.000 điện thoại.
 *
 * Cách hoạt động:
 *  1. Mở màn hình: sinh dữ liệu + dựng chỉ mục PhoneSearch ở THREAD NỀN (không làm chậm lúc mở).
 *  2. Gõ phím: DEBOUNCE 300ms — gõ liên tục thì chỉ tìm 1 lần sau khi ngừng gõ,
 *     tránh tốn CPU cho những từ khóa dở dang.
 *  3. Tìm kiếm chạy ở thread nền, xong mới post kết quả về main thread để hiển thị.
 *  4. "searchGeneration": mỗi truy vấn mới tăng số này; kết quả của truy vấn cũ về muộn
 *     sẽ bị bỏ qua, không ghi đè kết quả mới.
 *  5. Nút "Đo ×5": chạy liền 5 lần để đo thời gian xử lý ổn định hơn.
 *
 * Kiểm chứng: Android Studio Profiler > CPU > Java/Kotlin Method Trace, bấm "Đo ×5":
 * PhoneSearch.search nằm trên thread "pool-..." chứ không phải "main".
 */
public class CpuLabActivity extends BaseLabActivity {

    private static final int PHONE_COUNT = 20_000;
    private static final long DEBOUNCE_MS = 300;
    private static final int REPEAT_TIMES = 5;

    // Executor 1 luồng: các việc chạy tuần tự, nên lần tìm đầu tiên luôn đợi chỉ mục dựng xong
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private PhoneSearch phoneSearch;   // chỉ đọc/ghi trên thread của worker
    private int searchGeneration;      // chỉ đọc/ghi trên main thread
    private double lastSearchMs;

    private EditText etSearch;
    private TextView tvInfo;
    private PhoneAdapter adapter;
    private ImageLoader imageLoader;

    @Override
    protected int layoutId() {
        return R.layout.activity_lab_cpu;
    }

    @Override
    protected String title() {
        return "Kiểm tra CPU";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        etSearch = findViewById(R.id.etSearch);
        tvInfo = findViewById(R.id.tvSearchInfo);

        imageLoader = new ImageLoader();
        adapter = new PhoneAdapter(new ArrayList<>(), imageLoader, null);
        RecyclerView recycler = findViewById(R.id.recyclerResults);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        // Sinh dữ liệu + dựng chỉ mục ở thread nền, rồi hiện toàn bộ danh sách
        tvInfo.setText("🔄 Đang chuẩn bị 20.000 máy...");
        worker.execute(() -> phoneSearch = new PhoneSearch(FakePhoneRepository.generate(PHONE_COUNT)));
        runSearch("", 1);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Debounce: hủy lần tìm đang hẹn, hẹn lại sau 300ms
                mainHandler.removeCallbacks(debouncedSearch);
                mainHandler.postDelayed(debouncedSearch, DEBOUNCE_MS);
            }
        });

        findViewById(R.id.btnStress).setOnClickListener(v -> {
            mainHandler.removeCallbacks(debouncedSearch);
            runSearch(etSearch.getText().toString(), REPEAT_TIMES);
        });
    }

    private final Runnable debouncedSearch = () -> runSearch(etSearch.getText().toString(), 1);

    /** Tìm ở thread nền, chỉ quay về main thread để hiển thị. */
    private void runSearch(String query, int times) {
        final int generation = ++searchGeneration;
        worker.execute(() -> {
            SearchResult result = null;
            double total = 0;
            for (int i = 0; i < times; i++) {
                result = phoneSearch.search(query);
                total += result.elapsedMs;
            }
            final SearchResult finalResult = result;
            final double avgMs = total / times;
            mainHandler.post(() -> {
                if (generation != searchGeneration) return; // đã có truy vấn mới hơn
                showResult(finalResult, avgMs, times);
            });
        });
    }

    private void showResult(SearchResult result, double avgMs, int times) {
        lastSearchMs = avgMs;
        adapter.setItems(result.phones);
        String sameRange = result.phones.isEmpty() ? ""
                : "\nMáy rẻ nhất có " + FormatUtils.score(result.sameRangeCounts[0]) + " máy cùng tầm giá";
        String timing = times > 1
                ? String.format(Locale.ROOT, "TB %s / lần (%d lần)", FormatUtils.ms(avgMs), times)
                : FormatUtils.ms(avgMs);
        tvInfo.setText("Tìm thấy " + FormatUtils.score(result.phones.size()) + " máy · " + timing
                + " · thread nền ✨" + sameRange);
        refreshExtraMetric();
    }

    // ===== Ô thứ 4 & giải thích =====

    @Override
    protected String extraMetricLabel() {
        return "Xử lý";
    }

    @Override
    protected String extraMetricValue() {
        return FormatUtils.ms(lastSearchMs);
    }

    @Override
    protected String explanation() {
        return "💡 Kỹ thuật giảm tải CPU\n"
                + "• Debounce 300ms: chỉ tìm khi người dùng ngừng gõ\n"
                + "• Chạy ở thread nền, main thread luôn rảnh để vẽ giao diện\n"
                + "• Chỉ mục dựng sẵn 1 lần: chuỗi viết thường + HashMap đếm tầm giá\n"
                + "• Sắp xếp O(n log n) bằng TimSort thay vì thuật toán O(n²)\n"
                + "• RecyclerView chỉ vẽ ~10 dòng đang hiển thị dù kết quả có 20.000 máy\n"
                + "=> Gõ liên tục vẫn mượt dù dữ liệu 20.000 máy.";
    }

    @Override
    protected void onDestroy() {
        worker.shutdownNow();
        imageLoader.shutdown();
        super.onDestroy();
    }
}
