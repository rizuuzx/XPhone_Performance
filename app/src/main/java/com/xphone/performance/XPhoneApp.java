package com.xphone.performance;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.os.StrictMode;

/**
 * Lớp Application của XPhone Performance.
 *
 * Cách hoạt động:
 *  - Được Android khởi tạo đầu tiên, trước mọi Activity (khai báo ở AndroidManifest: android:name=".XPhoneApp").
 *  - Ở bản debug, bật StrictMode: Android sẽ ghi log (Logcat, tag "StrictMode") mỗi khi
 *    + main thread đọc/ghi đĩa hoặc gọi mạng (những việc có thể làm giật giao diện),
 *    + một Activity bị rò rỉ (leak) hoặc đối tượng Closeable không được đóng.
 *  - Đây là "camera an ninh" thứ nhất giúp phát hiện lỗi hiệu năng ngay khi đang code.
 *    LeakCanary (thêm ở build.gradle.kts) là camera thứ hai, chuyên về memory leak.
 */
public class XPhoneApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Chỉ bật khi app đang chạy bản debug (không ảnh hưởng người dùng thật)
        boolean isDebuggable = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        if (isDebuggable) {
            enableStrictMode();
        }
    }

    // ===== StrictMode: phát hiện thao tác chậm trên main thread và leak =====
    private void enableStrictMode() {
        // ThreadPolicy: theo dõi những gì chạy trên main thread
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()            // chỉ ghi log, không làm crash app
                .build());

        // VmPolicy: theo dõi rò rỉ bộ nhớ ở mức toàn ứng dụng
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectActivityLeaks()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build());
    }
}
