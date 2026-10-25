package com.xphone.performance.monitor;

/**
 * Thống kê thời gian "bind" (đổ dữ liệu vào 1 dòng của danh sách).
 *
 * Cách hoạt động:
 *  - Adapter gọi record(ns) sau mỗi lần bind một item.
 *  - Mỗi giây màn hình Lab gọi drainAverageMs() để lấy thời gian trung bình
 *    kể từ lần đọc trước, rồi đặt lại bộ đếm.
 *  - Bind > 16ms nghĩa là chỉ riêng 1 item đã ăn hết thời gian của 1 frame => chắc chắn giật.
 *
 * Chỉ được dùng trên main thread nên không cần đồng bộ (synchronized).
 */
public class BindStats {

    private long totalNs;
    private int count;
    private double lastAverageMs;

    public void record(long durationNs) {
        totalNs += durationNs;
        count++;
    }

    /** Trung bình (ms) kể từ lần gọi trước; nếu không có bind mới thì trả lại số cũ. */
    public double drainAverageMs() {
        if (count > 0) {
            lastAverageMs = totalNs / 1_000_000.0 / count;
            totalNs = 0;
            count = 0;
        }
        return lastAverageMs;
    }

    public void reset() {
        totalNs = 0;
        count = 0;
        lastAverageMs = 0;
    }
}
