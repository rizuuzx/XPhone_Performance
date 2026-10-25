package com.xphone.performance.monitor;

import android.view.Choreographer;

/**
 * Đo FPS và đếm số frame bị giật (jank) theo thời gian thực.
 *
 * Kiến thức nền:
 *  - Màn hình 60Hz vẽ 60 frame/giây => mỗi frame chỉ có ~16,7ms.
 *  - Nếu main thread bận quá lâu (vẽ ảnh nặng, tính toán...), frame tiếp theo bị trễ
 *    => người dùng thấy "khựng" (jank / lag).
 *
 * Cách hoạt động:
 *  - Choreographer là "nhạc trưởng" của Android, gọi doFrame() mỗi khi màn hình sắp vẽ frame mới.
 *  - Đăng ký doFrame() liên tục, ghi lại thời điểm từng frame (frameTimeNanos).
 *  - Khoảng cách giữa 2 frame > JANK_THRESHOLD (≈ 2 frame) => tính là 1 lần giật.
 *  - Cứ mỗi 1 giây, đếm số frame đã vẽ => ra FPS, rồi báo về Listener để cập nhật UI.
 *  - Khi main thread bị chặn, doFrame() cũng bị trễ theo => FPS tụt, jank tăng. Đó chính là
 *    thứ cần đo.
 */
public class FrameMonitor implements Choreographer.FrameCallback {

    /** Nhận số liệu mỗi giây. */
    public interface Listener {
        void onFrameStats(int fps, int totalJank, float worstFrameMs);
    }

    /** Frame chậm hơn 32ms (tức trễ ít nhất 1 frame ở 60Hz) bị tính là giật. */
    private static final long JANK_THRESHOLD_NS = 32_000_000L;
    private static final long ONE_SECOND_NS = 1_000_000_000L;

    private final Listener listener;

    private boolean running;
    private long lastFrameNs;
    private long windowStartNs;
    private int framesInWindow;
    private int totalJank;
    private long worstFrameNs;

    public FrameMonitor(Listener listener) {
        this.listener = listener;
    }

    public void start() {
        if (running) return;
        running = true;
        lastFrameNs = 0;
        framesInWindow = 0;
        worstFrameNs = 0;
        Choreographer.getInstance().postFrameCallback(this);
    }

    public void stop() {
        running = false;
        Choreographer.getInstance().removeFrameCallback(this);
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (!running) return;

        if (lastFrameNs == 0) {
            // Frame đầu tiên: chỉ đánh dấu mốc thời gian
            windowStartNs = frameTimeNanos;
        } else {
            long delta = frameTimeNanos - lastFrameNs;
            if (delta > JANK_THRESHOLD_NS) totalJank++;
            if (delta > worstFrameNs) worstFrameNs = delta;
        }
        lastFrameNs = frameTimeNanos;
        framesInWindow++;

        // Đủ 1 giây => tính FPS và báo kết quả
        long elapsed = frameTimeNanos - windowStartNs;
        if (elapsed >= ONE_SECOND_NS) {
            int fps = Math.round(framesInWindow * (float) ONE_SECOND_NS / elapsed);
            listener.onFrameStats(fps, totalJank, worstFrameNs / 1_000_000f);
            framesInWindow = 0;
            worstFrameNs = 0;
            windowStartNs = frameTimeNanos;
        }

        // Đăng ký tiếp cho frame sau
        Choreographer.getInstance().postFrameCallback(this);
    }
}
