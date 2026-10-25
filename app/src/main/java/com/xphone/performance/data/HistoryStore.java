package com.xphone.performance.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.xphone.performance.data.model.BenchmarkResult;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lưu lịch sử các lần chạy benchmark.
 *
 * Cách hoạt động:
 *  - Toàn bộ lịch sử được lưu thành 1 chuỗi JSON (mảng) trong SharedPreferences.
 *  - Phần tử MỚI NHẤT nằm ở ĐẦU danh sách, giữ tối đa MAX_ITEMS phần tử.
 *  - Dữ liệu rất nhỏ (vài KB) nên dùng SharedPreferences là đủ, không cần database.
 *
 * Mọi thao tác đọc/ghi đều chạy trên MỘT thread nền (IO), kết quả trả về main thread qua callback.
 *  - Lý do: lần đầu gọi getSharedPreferences(), Android phải đọc file XML từ bộ nhớ trong.
 *    Khi đọc trên main thread, StrictMode báo DiskReadViolation ~54ms (chặn hơn 3 frame)
 *    ngay lúc mở Trang chủ. Chuyển sang thread nền thì hết vi phạm.
 *  - Dùng 1 thread duy nhất nên các thao tác chạy tuần tự, không cần khóa (lock) khi ghi.
 */
public class HistoryStore {

    /** Nhận kết quả trên main thread. */
    public interface Callback<T> {
        void onResult(T value);
    }

    private static final String PREFS = "xphone_history";
    private static final String KEY_ITEMS = "items";
    private static final int MAX_ITEMS = 30;

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private final Context appContext;
    private SharedPreferences prefs; // chỉ truy cập trên thread IO

    public HistoryStore(Context context) {
        // Dùng applicationContext để không giữ tham chiếu tới Activity (tránh leak)
        appContext = context.getApplicationContext();
    }

    // ===== API bất đồng bộ dùng từ giao diện =====

    /** Đọc toàn bộ lịch sử (mới nhất trước). */
    public void loadAll(Callback<List<BenchmarkResult>> callback) {
        IO.execute(() -> {
            List<BenchmarkResult> all = readAll();
            MAIN.post(() -> callback.onResult(all));
        });
    }

    /** Đọc kết quả mới nhất (null nếu chưa chạy lần nào). */
    public void loadLatest(Callback<BenchmarkResult> callback) {
        IO.execute(() -> {
            List<BenchmarkResult> all = readAll();
            BenchmarkResult latest = all.isEmpty() ? null : all.get(0);
            MAIN.post(() -> callback.onResult(latest));
        });
    }

    /**
     * Thêm kết quả mới vào đầu danh sách.
     * Callback nhận lần chạy TRƯỚC đó (để so sánh), null nếu đây là lần đầu.
     */
    public void add(BenchmarkResult result, Callback<BenchmarkResult> previousCallback) {
        IO.execute(() -> {
            List<BenchmarkResult> all = readAll();
            BenchmarkResult previous = all.isEmpty() ? null : all.get(0);
            all.add(0, result);
            while (all.size() > MAX_ITEMS) {
                all.remove(all.size() - 1);
            }
            save(all);
            MAIN.post(() -> previousCallback.onResult(previous));
        });
    }

    public void clear(Runnable done) {
        IO.execute(() -> {
            prefs().edit().remove(KEY_ITEMS).apply();
            MAIN.post(done);
        });
    }

    // ===== Phần chạy trên thread IO =====

    private SharedPreferences prefs() {
        if (prefs == null) {
            prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        }
        return prefs;
    }

    private List<BenchmarkResult> readAll() {
        List<BenchmarkResult> list = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs().getString(KEY_ITEMS, "[]"));
            for (int i = 0; i < array.length(); i++) {
                list.add(BenchmarkResult.fromJson(array.getJSONObject(i)));
            }
        } catch (JSONException ignored) {
            // Dữ liệu hỏng thì coi như chưa có lịch sử
        }
        return list;
    }

    private void save(List<BenchmarkResult> list) {
        JSONArray array = new JSONArray();
        try {
            for (BenchmarkResult r : list) {
                array.put(r.toJson());
            }
        } catch (JSONException ignored) {
        }
        prefs().edit().putString(KEY_ITEMS, array.toString()).apply();
    }
}
