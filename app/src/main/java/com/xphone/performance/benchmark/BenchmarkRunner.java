package com.xphone.performance.benchmark;

import android.os.Handler;
import android.os.Looper;

import com.xphone.performance.data.model.BenchmarkResult;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Chạy lần lượt 3 bài kiểm tra trên MỘT thread nền và báo tiến độ về giao diện.
 *
 * Luồng chạy:
 *   [main] start()
 *     └─> [nền] Smooth -> CPU -> Memory   (mỗi bài bắt đầu thì post onProgress về main)
 *           └─> gộp thành BenchmarkResult -> post onFinished về main
 *
 * Vì sao chạy nền? Các bài test cố tình nặng để vắt hết sức máy. Nếu chạy trên main thread,
 * màn hình sẽ đơ và Android có thể báo ANR ("Ứng dụng không phản hồi") sau 5 giây.
 * Nhờ chạy nền, linh vật và thanh tiến độ vẫn chuyển động mượt trong lúc đo.
 */
public class BenchmarkRunner {

    public interface Callback {
        /** Bắt đầu bài thứ index (0..total-1). */
        void onProgress(int index, int total, String taskName);

        void onFinished(BenchmarkResult result);
    }

    private final BenchmarkTask[] tasks = {
            new SmoothBenchmark(), new CpuBenchmark(), new MemoryBenchmark()
    };

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile boolean cancelled;

    public void start(Callback callback) {
        cancelled = false;
        executor.execute(() -> {
            BenchmarkTask.TaskResult[] results = new BenchmarkTask.TaskResult[tasks.length];
            for (int i = 0; i < tasks.length; i++) {
                if (cancelled) return;
                final int index = i;
                mainHandler.post(() -> callback.onProgress(index, tasks.length, tasks[index].name()));
                results[i] = tasks[i].run();
            }
            if (cancelled) return;

            BenchmarkResult result = new BenchmarkResult(
                    System.currentTimeMillis(),
                    results[0].score, results[1].score, results[2].score,
                    results[0].value, results[1].value, results[2].value);
            mainHandler.post(() -> {
                if (!cancelled) callback.onFinished(result);
            });
        });
    }

    /**
     * Gọi trong onDestroy: dừng nhận kết quả và gỡ mọi tin nhắn đang chờ,
     * để callback (thường là Activity) không bị giữ lại sau khi đóng => tránh leak.
     */
    public void cancel() {
        cancelled = true;
        mainHandler.removeCallbacksAndMessages(null);
        executor.shutdownNow();
    }
}
