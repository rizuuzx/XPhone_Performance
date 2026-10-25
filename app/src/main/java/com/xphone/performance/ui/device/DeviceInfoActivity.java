package com.xphone.performance.ui.device;

import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.xphone.performance.R;
import com.xphone.performance.monitor.MemoryMonitor;
import com.xphone.performance.util.FormatUtils;
import com.xphone.performance.util.UiUtils;

import java.util.Locale;

/**
 * Thông tin thiết bị (giống tab "Thông tin máy" của các app benchmark).
 *
 * Lấy dữ liệu từ:
 *  - Build                     : hãng, model, phiên bản Android, kiến trúc CPU (ABI)
 *  - Runtime                   : số nhân CPU, giới hạn heap của app
 *  - ActivityManager           : tổng RAM (qua MemoryMonitor)
 *  - DisplayMetrics / Display  : độ phân giải, tần số quét
 *
 * Tần số quét quan trọng: màn 60Hz có 16,7ms/frame, màn 120Hz chỉ còn 8,3ms/frame
 * => máy càng "xịn" thì yêu cầu code càng phải nhanh mới không giật.
 */
public class DeviceInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_info);
        UiUtils.setupEdgeToEdge(this, findViewById(R.id.root));
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        LinearLayout list = findViewById(R.id.infoList);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float refreshRate = currentDisplay().getRefreshRate();

        addRow(list, "📱 Thiết bị", Build.MANUFACTURER + " " + Build.MODEL);
        addRow(list, "🤖 Android", Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        addRow(list, "⚙️ Số nhân CPU", String.valueOf(Runtime.getRuntime().availableProcessors()));
        addRow(list, "🧩 Kiến trúc", Build.SUPPORTED_ABIS[0]);
        addRow(list, "💾 Tổng RAM", FormatUtils.mb(MemoryMonitor.totalRamBytes(this)));
        addRow(list, "📦 Heap tối đa của app", FormatUtils.mb(MemoryMonitor.maxHeapBytes()));
        addRow(list, "🖼 Độ phân giải", dm.widthPixels + " × " + dm.heightPixels + " px");
        addRow(list, "🎞 Tần số quét", String.format(Locale.ROOT, "%.0f Hz", refreshRate));
        addRow(list, "⏱ Thời gian 1 frame", FormatUtils.ms(1000f / refreshRate));
    }

    @SuppressWarnings("deprecation")
    private Display currentDisplay() {
        // API 30+ có getDisplay() trên Activity; bản cũ hơn dùng WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return getDisplay();
        }
        return getWindowManager().getDefaultDisplay();
    }

    private void addRow(LinearLayout parent, String key, String value) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_score_row, parent, false);
        ((TextView) row.findViewById(R.id.tvRowTitle)).setText(key);
        ((TextView) row.findViewById(R.id.tvRowValue)).setText(value);
        row.findViewById(R.id.tvRowDetail).setVisibility(View.GONE);
        parent.addView(row);
    }
}
