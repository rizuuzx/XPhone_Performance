package com.xphone.performance.monitor;

import android.app.ActivityManager;
import android.content.Context;

/**
 * Đọc thông tin bộ nhớ.
 *
 *  - usedHeapBytes(): bộ nhớ Java heap app ĐANG dùng = tổng heap đã xin - phần còn trống.
 *    Nếu số này cứ tăng mãi sau mỗi lần mở/đóng màn hình => dấu hiệu memory leak.
 *  - maxHeapBytes(): giới hạn heap Android cấp cho app. Vượt quá => OutOfMemoryError (crash).
 *  - totalRamBytes(): tổng RAM vật lý của máy (hiển thị ở màn Thông tin thiết bị).
 */
public final class MemoryMonitor {

    private MemoryMonitor() {
    }

    public static long usedHeapBytes() {
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }

    public static long maxHeapBytes() {
        return Runtime.getRuntime().maxMemory();
    }

    public static long totalRamBytes(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(info);
        return info.totalMem;
    }

    /**
     * Yêu cầu dọn rác (Garbage Collection) vài lần cho chắc.
     * GC không đảm bảo chạy ngay, nên gọi lặp + nghỉ ngắn. KHÔNG gọi hàm này trên main thread
     * trong app thật; ở đây chỉ dùng cho mục đích đo đạc / demo.
     */
    public static void forceGc() {
        for (int i = 0; i < 3; i++) {
            Runtime.getRuntime().gc();
            System.runFinalization();
            try {
                Thread.sleep(60);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
