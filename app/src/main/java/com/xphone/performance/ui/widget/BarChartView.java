package com.xphone.performance.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.xphone.performance.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Biểu đồ cột đơn giản tự vẽ (không cần thư viện ngoài) cho màn Lịch sử.
 *
 * Mỗi cột = một lần chạy benchmark: chiều cao tỉ lệ với điểm tổng,
 * cột kỷ lục màu mint, các cột khác màu hồng. Trên đầu cột ghi điểm dạng "52k".
 */
public class BarChartView extends View {

    /** Dữ liệu một cột. */
    public static class Bar {
        final String label;
        final float value;
        final int color;

        public Bar(String label, float value, int color) {
            this.label = label;
            this.value = value;
            this.color = color;
        }
    }

    private final List<Bar> bars = new ArrayList<>();
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private float maxValue = 1;

    public BarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        textPaint.setColor(ContextCompat.getColor(context, R.color.xp_text_soft));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 11, getResources().getDisplayMetrics()));
    }

    public void setBars(List<Bar> data, float maxValue) {
        bars.clear();
        bars.addAll(data);
        this.maxValue = Math.max(1, maxValue);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bars.isEmpty()) return;

        float textH = textPaint.getTextSize() * 1.6f;
        float chartTop = textH;                 // chừa chỗ ghi điểm trên đầu cột
        float chartBottom = getHeight() - textH; // chừa chỗ ghi nhãn dưới chân cột
        float slot = getWidth() / (float) bars.size();
        float barWidth = slot * 0.55f;
        float radius = barWidth / 3f;

        for (int i = 0; i < bars.size(); i++) {
            Bar bar = bars.get(i);
            float cx = slot * i + slot / 2f;
            float top = chartBottom - (chartBottom - chartTop) * (bar.value / maxValue);

            barPaint.setColor(bar.color);
            rect.set(cx - barWidth / 2f, top, cx + barWidth / 2f, chartBottom);
            canvas.drawRoundRect(rect, radius, radius, barPaint);

            canvas.drawText(String.format(Locale.ROOT, "%.0fk", bar.value / 1000f), cx, top - textH * 0.25f, textPaint);
            canvas.drawText(bar.label, cx, getHeight() - textH * 0.25f, textPaint);
        }
    }
}
