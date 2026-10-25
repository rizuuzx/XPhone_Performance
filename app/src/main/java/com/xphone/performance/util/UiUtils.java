package com.xphone.performance.util;

import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Tiện ích giao diện dùng chung.
 */
public final class UiUtils {

    private UiUtils() {
    }

    /**
     * Bật chế độ tràn viền (edge-to-edge) và đệm (padding) cho view gốc
     * để nội dung không bị thanh trạng thái / thanh điều hướng che mất.
     */
    public static void setupEdgeToEdge(AppCompatActivity activity, View root) {
        EdgeToEdge.enable(activity);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }
}
