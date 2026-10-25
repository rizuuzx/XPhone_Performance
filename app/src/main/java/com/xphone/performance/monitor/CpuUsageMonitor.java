package com.xphone.performance.monitor;

import android.os.Process;
import android.os.SystemClock;

/**
 * Đo % CPU mà chính app đang sử dụng.
 *
 * Cách tính:
 *  - Process.getElapsedCpuTime(): tổng thời gian CPU (ms) mà tiến trình của app đã dùng,
 *    cộng dồn tất cả các thread (main thread + thread nền).
 *  - Mỗi lần gọi sample(), lấy phần CPU dùng thêm chia cho thời gian thực trôi qua,
 *    rồi chia cho số nhân CPU:
 *        % CPU = (ΔcpuTime / Δthời_gian) / số_nhân × 100
 *  - 100% nghĩa là app đang dùng hết TẤT CẢ các nhân. App đứng yên thường chỉ vài %.
 *
 * Chỉ đọc số liệu của chính tiến trình mình, không cần quyền đặc biệt
 * (Android 8+ không cho đọc CPU của app khác).
 */
public class CpuUsageMonitor {

    private final int cores = Runtime.getRuntime().availableProcessors();
    private long lastCpuMs = Process.getElapsedCpuTime();
    private long lastWallMs = SystemClock.elapsedRealtime();

    /** % CPU trung bình kể từ lần gọi trước. */
    public float sample() {
        long cpuMs = Process.getElapsedCpuTime();
        long wallMs = SystemClock.elapsedRealtime();
        long dWall = wallMs - lastWallMs;
        float percent = dWall <= 0 ? 0 : (cpuMs - lastCpuMs) * 100f / dWall / cores;
        lastCpuMs = cpuMs;
        lastWallMs = wallMs;
        return Math.max(0, Math.min(100, percent));
    }
}
