package com.xphone.performance.data;

import com.xphone.performance.data.model.OptimizationEntry;
import com.xphone.performance.data.model.OptimizationEntry.Category;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Nhật ký tối ưu: các vấn đề hiệu năng THẬT đã gặp khi phát triển XPhone Performance.
 *
 * Số liệu đo trên máy ảo Pixel 5 (Android Emulator), ngày 25/10/2026:
 *  - StrictMode: đọc trong Logcat (tag "StrictMode") khi chạy bản debug.
 *  - DiffUtil: đo bằng instrumented test DiffUtilCostTest (Logcat tag "DiffCost").
 *  - Lint: báo cáo app/build/reports/lint-results-debug.html (lệnh gradlew lintDebug).
 *  - Điểm benchmark & kích thước APK: chạy benchmark của app trên bản debug và bản release.
 *
 * Dữ liệu cố định, nhỏ nên khai báo thẳng trong code (không cần đọc file hay database).
 */
public final class OptimizationLog {

    private OptimizationLog() {
    }

    private static final List<OptimizationEntry> ENTRIES = Collections.unmodifiableList(Arrays.asList(

            new OptimizationEntry(Category.CPU,
                    "DiffUtil so sánh danh sách 20.000 dòng",
                    "Instrumented test DiffUtilCostTest (đo trên máy ảo)",
                    "Màn Kiểm tra CPU cập nhật danh sách kết quả mỗi lần tìm kiếm. Phiên bản đầu dùng "
                            + "DiffUtil.calculateDiff() trên main thread.",
                    "DiffUtil dùng thuật toán Myers, chi phí tăng theo số khác biệt giữa 2 danh sách. "
                            + "Kết quả tìm kiếm thường khác hẳn danh sách cũ (20.000 → 2.871 dòng) nên rất chậm.",
                    "Dùng notifyDataSetChanged(): RecyclerView chỉ bind lại khoảng 10 dòng đang hiển thị.",
                    "3.657 ms đơ giao diện",
                    "Giữ 60 FPS khi tìm kiếm",
                    "ui/labs/PhoneAdapter.java"),

            new OptimizationEntry(Category.LAG,
                    "Đọc lịch sử điểm trên main thread",
                    "StrictMode (DiskReadViolation trong Logcat)",
                    "Mở Trang chủ thì main thread bị chặn trong lúc đọc dữ liệu lịch sử.",
                    "HomeFragment.onResume() gọi getSharedPreferences() và parse JSON trực tiếp. Lần đầu, "
                            + "Android phải đọc file XML từ bộ nhớ trong. Màn Lịch sử và màn Benchmark cũng làm tương tự.",
                    "HistoryStore chạy mọi thao tác đọc/ghi trên một thread nền riêng, "
                            + "trả kết quả về main thread qua callback.",
                    "Chặn main thread 54 ms (≈ 3 frame)",
                    "0 vi phạm StrictMode",
                    "data/HistoryStore.java"),

            new OptimizationEntry(Category.LAG,
                    "Vẽ nền 2 lần (Overdraw)",
                    "Android Lint – Overdraw",
                    "6 màn hình tốn công GPU tô cùng một vùng 2 lần cho mỗi frame.",
                    "View gốc của từng màn đặt android:background trùng màu với windowBackground "
                            + "mà theme đã vẽ sẵn.",
                    "Bỏ android:background ở view gốc, để theme vẽ nền đúng một lần.",
                    "6 cảnh báo Overdraw",
                    "0 cảnh báo",
                    "res/layout/activity_*.xml"),

            new OptimizationEntry(Category.LAG,
                    "Layout lồng thừa và đo thừa",
                    "Android Lint – UselessParent, DisableBaselineAlignment",
                    "Mỗi lần vẽ, hệ thống phải đo thêm các view không cần thiết.",
                    "Header 3 màn kiểm tra bọc 2 lớp LinearLayout, trong đó một lớp không có tác dụng. "
                            + "2 LinearLayout ngang còn căn baseline cho các cột không cần căn.",
                    "Gộp header còn 1 lớp, thêm android:baselineAligned=\"false\".",
                    "3 cảnh báo",
                    "0 cảnh báo",
                    "res/layout/view_lab_header.xml"),

            new OptimizationEntry(Category.MEMORY,
                    "Báo nhầm rò rỉ bộ nhớ",
                    "LeakTracker (màn Kiểm tra rò rỉ) + kiểm tra lại thủ công",
                    "Sau khi tự mở/đóng 5 màn hình 8MB, app báo còn 1 màn trong bộ nhớ. "
                            + "Bấm kiểm tra lại vài giây sau thì ra 0, nên đây không phải leak thật.",
                    "Khi quay về, onResume() của màn Lab chạy TRƯỚC onDestroy() của màn 8MB cuối cùng, "
                            + "nên lúc đếm màn đó vẫn còn sống hợp lệ.",
                    "Chờ 1 giây sau onResume() rồi mới chạy GC và đếm.",
                    "Báo 1 màn còn sống (sai)",
                    "0 qua 3 lần thử",
                    "ui/labs/memory/MemoryLabActivity.java"),

            new OptimizationEntry(Category.BUILD,
                    "Bản release chưa bật R8",
                    "Benchmark của app + kích thước file APK",
                    "Template mặc định của Android Studio tắt tối ưu ở bản release.",
                    "optimization { enable = false } trong build.gradle.kts: code không được thu gọn "
                            + "và tối ưu bytecode.",
                    "Bật R8 cho bản release và thêm <profileable> để vẫn đo được bằng Profiler.",
                    "Bản debug: 7,6 MB · 52.696 điểm",
                    "Bản release: 1,5 MB · 72.280 điểm",
                    "app/build.gradle.kts")
    ));

    public static List<OptimizationEntry> all() {
        return ENTRIES;
    }
}
