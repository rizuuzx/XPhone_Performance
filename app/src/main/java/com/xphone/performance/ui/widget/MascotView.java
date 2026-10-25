package com.xphone.performance.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.xphone.performance.R;

/**
 * Linh vật "Mèo XPhone", tự vẽ bằng Canvas (không dùng ảnh có sẵn).
 *
 * Biểu cảm thay đổi theo tình trạng hiệu năng:
 *  - HAPPY  : mắt cười ^ ^       => app chạy mượt / điểm cao
 *  - NORMAL : mắt chấm tròn      => bình thường
 *  - SWEAT  : đổ mồ hôi          => đang lag / CPU nặng
 *  - SAD    : rơi nước mắt       => bị leak / điểm thấp
 *
 * Mọi Paint / Path được tạo sẵn trong constructor, onDraw() chỉ vẽ.
 */
public class MascotView extends View {

    public enum Mood { HAPPY, NORMAL, SWEAT, SAD }

    private final Paint facePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint earInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint eyePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cheekPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint waterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF oval = new RectF();

    private Mood mood = Mood.HAPPY;

    public MascotView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        facePaint.setColor(0xFFFFE4EC);
        earInnerPaint.setColor(ContextCompat.getColor(context, R.color.xp_primary));
        int outline = ContextCompat.getColor(context, R.color.xp_text);
        linePaint.setColor(outline);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        eyePaint.setColor(outline);
        cheekPaint.setColor(0x66FF8FAB);
        waterPaint.setColor(0xFF8EC9FF);
    }

    public void setMood(Mood mood) {
        if (this.mood != mood) {
            this.mood = mood;
            invalidate();
        }
    }

    public Mood getMood() {
        return mood;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float size = Math.min(getWidth(), getHeight());
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f + size * 0.06f;
        float r = size * 0.36f;
        linePaint.setStrokeWidth(size * 0.025f);

        // ===== Tai (2 tam giác) =====
        for (int side = -1; side <= 1; side += 2) {
            drawTriangle(canvas, facePaint,
                    cx + side * r * 0.95f, cy - r * 0.25f,
                    cx + side * r * 0.75f, cy - r * 1.3f,
                    cx + side * r * 0.2f, cy - r * 0.85f);
            drawTriangle(canvas, earInnerPaint,
                    cx + side * r * 0.8f, cy - r * 0.45f,
                    cx + side * r * 0.72f, cy - r * 1.1f,
                    cx + side * r * 0.38f, cy - r * 0.8f);
            drawTriangle(canvas, linePaint,
                    cx + side * r * 0.95f, cy - r * 0.25f,
                    cx + side * r * 0.75f, cy - r * 1.3f,
                    cx + side * r * 0.2f, cy - r * 0.85f);
        }

        // ===== Mặt =====
        canvas.drawCircle(cx, cy, r, facePaint);
        canvas.drawCircle(cx, cy, r, linePaint);

        // ===== Má hồng =====
        float cheekY = cy + r * 0.25f;
        canvas.drawCircle(cx - r * 0.55f, cheekY, r * 0.14f, cheekPaint);
        canvas.drawCircle(cx + r * 0.55f, cheekY, r * 0.14f, cheekPaint);

        // ===== Mắt =====
        float eyeY = cy - r * 0.1f;
        float eyeDx = r * 0.38f;
        if (mood == Mood.HAPPY) {
            // Mắt cười: 2 cung cong lên
            for (int side = -1; side <= 1; side += 2) {
                oval.set(cx + side * eyeDx - r * 0.13f, eyeY - r * 0.1f,
                        cx + side * eyeDx + r * 0.13f, eyeY + r * 0.16f);
                canvas.drawArc(oval, 200, 140, false, linePaint);
            }
        } else {
            canvas.drawCircle(cx - eyeDx, eyeY, r * 0.08f, eyePaint);
            canvas.drawCircle(cx + eyeDx, eyeY, r * 0.08f, eyePaint);
        }

        // ===== Miệng =====
        float mouthY = cy + r * 0.2f;
        if (mood == Mood.SAD) {
            // Miệng mếu (cung úp xuống)
            oval.set(cx - r * 0.16f, mouthY, cx + r * 0.16f, mouthY + r * 0.24f);
            canvas.drawArc(oval, 200, 140, false, linePaint);
        } else {
            // Miệng mèo "ω"
            oval.set(cx - r * 0.2f, mouthY - r * 0.1f, cx, mouthY + r * 0.1f);
            canvas.drawArc(oval, 0, 180, false, linePaint);
            oval.set(cx, mouthY - r * 0.1f, cx + r * 0.2f, mouthY + r * 0.1f);
            canvas.drawArc(oval, 0, 180, false, linePaint);
        }

        // ===== Ria mép =====
        for (int side = -1; side <= 1; side += 2) {
            canvas.drawLine(cx + side * r * 0.75f, cy + r * 0.05f, cx + side * r * 1.15f, cy - r * 0.02f, linePaint);
            canvas.drawLine(cx + side * r * 0.75f, cy + r * 0.18f, cx + side * r * 1.15f, cy + r * 0.22f, linePaint);
        }

        // ===== Mồ hôi / nước mắt =====
        if (mood == Mood.SWEAT) {
            drawDrop(canvas, cx + r * 0.85f, cy - r * 0.75f, r * 0.15f);
        } else if (mood == Mood.SAD) {
            drawDrop(canvas, cx - eyeDx, eyeY + r * 0.3f, r * 0.09f);
            drawDrop(canvas, cx + eyeDx, eyeY + r * 0.3f, r * 0.09f);
        }
    }

    private void drawTriangle(Canvas canvas, Paint paint,
                              float x1, float y1, float x2, float y2, float x3, float y3) {
        path.reset();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        path.lineTo(x3, y3);
        path.close();
        canvas.drawPath(path, paint);
    }

    /** Giọt nước: hình tròn + tam giác nhọn phía trên. */
    private void drawDrop(Canvas canvas, float x, float y, float radius) {
        canvas.drawCircle(x, y, radius, waterPaint);
        drawTriangle(canvas, waterPaint, x - radius, y, x + radius, y, x, y - radius * 2.2f);
    }
}
