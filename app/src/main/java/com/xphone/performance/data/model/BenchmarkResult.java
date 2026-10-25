package com.xphone.performance.data.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Kết quả một lần chạy benchmark (một "lượt chấm điểm" kiểu AnTuTu).
 *
 * Gồm 3 điểm thành phần + số liệu đo gốc:
 *  - smooth : điểm xử lý đồ họa  + thời gian vẽ trung bình 1 ảnh (ms)
 *  - cpu    : điểm xử lý dữ liệu + tổng thời gian tìm kiếm & sắp xếp (ms)
 *  - memory : điểm bộ nhớ        + tốc độ đọc/ghi bộ nhớ (MB/s)
 *
 * Có hàm chuyển qua lại JSON để lưu vào SharedPreferences (xem HistoryStore).
 */
public class BenchmarkResult {

    public final long timestamp;
    public final int smoothScore;
    public final int cpuScore;
    public final int memoryScore;
    public final double smoothMs;
    public final double cpuMs;
    public final double memoryMbps;

    public BenchmarkResult(long timestamp, int smoothScore, int cpuScore, int memoryScore,
                           double smoothMs, double cpuMs, double memoryMbps) {
        this.timestamp = timestamp;
        this.smoothScore = smoothScore;
        this.cpuScore = cpuScore;
        this.memoryScore = memoryScore;
        this.smoothMs = smoothMs;
        this.cpuMs = cpuMs;
        this.memoryMbps = memoryMbps;
    }

    /** Điểm tổng = cộng 3 điểm thành phần (tối đa 100.000). */
    public int total() {
        return smoothScore + cpuScore + memoryScore;
    }

    // ===== Chuyển đổi JSON để lưu trữ =====

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("time", timestamp);
        o.put("smooth", smoothScore);
        o.put("cpu", cpuScore);
        o.put("mem", memoryScore);
        o.put("smoothMs", smoothMs);
        o.put("cpuMs", cpuMs);
        o.put("memMbps", memoryMbps);
        return o;
    }

    public static BenchmarkResult fromJson(JSONObject o) throws JSONException {
        return new BenchmarkResult(
                o.getLong("time"),
                o.getInt("smooth"),
                o.getInt("cpu"),
                o.getInt("mem"),
                o.getDouble("smoothMs"),
                o.getDouble("cpuMs"),
                o.getDouble("memMbps"));
    }
}
