package com.xphone.performance.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Các hàm định dạng số / thời gian để hiển thị lên giao diện.
 * Gom về một chỗ để mọi màn hình hiển thị giống nhau.
 */
public final class FormatUtils {

    private FormatUtils() {
    }

    /** 98765 -> "98.765" (kiểu điểm của AnTuTu, phân tách hàng nghìn bằng dấu chấm). */
    public static String score(int value) {
        return String.format(Locale.GERMANY, "%,d", value);
    }

    /** 12990000 -> "12.990.000 đ" */
    public static String price(int vnd) {
        return String.format(Locale.GERMANY, "%,d đ", vnd);
    }

    /** 3.456 -> "3,5 ms" */
    public static String ms(double value) {
        return String.format(Locale.GERMANY, "%.1f ms", value);
    }

    /** Đổi byte sang MB, lấy 1 chữ số thập phân. */
    public static String mb(long bytes) {
        return String.format(Locale.GERMANY, "%.1f MB", bytes / 1024f / 1024f);
    }

    /** Thời điểm chạy, ví dụ "25/10 14:32". */
    public static String time(long millis) {
        return new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(new Date(millis));
    }
}
