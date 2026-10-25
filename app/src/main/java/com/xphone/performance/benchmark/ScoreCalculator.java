package com.xphone.performance.benchmark;

/**
 * Quy đổi số liệu đo thành điểm, kiểu AnTuTu. Tổng tối đa 100.000 điểm.
 *
 * 1) Số đo là THỜI GIAN (càng nhỏ càng tốt) — Smooth, CPU:
 *        điểm = ĐIỂM_TỐI_ĐA × MỐC / (MỐC + thời_gian)
 *    - thời gian = 0     => điểm tối đa
 *    - thời gian = MỐC   => một nửa điểm
 *
 * 2) Số đo là TỐC ĐỘ (càng lớn càng tốt) — Memory:
 *        điểm = ĐIỂM_TỐI_ĐA × tốc_độ / (tốc_độ + MỐC)
 *    - tốc độ = MỐC      => một nửa điểm, tốc độ càng cao càng gần tối đa
 *
 * Các mốc chọn theo ý nghĩa thực tế:
 *  - Smooth : MỐC 16,67ms   = thời gian của đúng 1 frame ở 60Hz.            Tối đa 40.000
 *  - CPU    : MỐC 50ms      = ngưỡng người dùng bắt đầu cảm thấy chậm.       Tối đa 35.000
 *  - Memory : MỐC 4000 MB/s = tốc độ copy bộ nhớ của máy tầm trung.          Tối đa 25.000
 */
public final class ScoreCalculator {

    public static final int MAX_SMOOTH = 40_000;
    public static final int MAX_CPU = 35_000;
    public static final int MAX_MEMORY = 25_000;
    public static final int MAX_TOTAL = MAX_SMOOTH + MAX_CPU + MAX_MEMORY;

    static final double SMOOTH_TARGET_MS = 16.67;
    static final double CPU_TARGET_MS = 50;
    static final double MEMORY_TARGET_MBPS = 4000;

    private ScoreCalculator() {
    }

    /** @param avgMsPerImage thời gian trung bình để vẽ 1 ảnh thumbnail */
    public static int smooth(double avgMsPerImage) {
        return fromTime(avgMsPerImage, SMOOTH_TARGET_MS, MAX_SMOOTH);
    }

    /** @param totalMs tổng thời gian dựng chỉ mục + chạy các truy vấn tìm kiếm */
    public static int cpu(double totalMs) {
        return fromTime(totalMs, CPU_TARGET_MS, MAX_CPU);
    }

    /** @param mbPerSecond tốc độ cấp phát + ghi + copy bộ nhớ */
    public static int memory(double mbPerSecond) {
        return fromSpeed(mbPerSecond, MEMORY_TARGET_MBPS, MAX_MEMORY);
    }

    static int fromTime(double ms, double target, int max) {
        double m = Math.max(0, ms);
        return (int) Math.round(max * target / (target + m));
    }

    static int fromSpeed(double speed, double target, int max) {
        double s = Math.max(0, speed);
        return (int) Math.round(max * s / (s + target));
    }
}
