package com.xphone.performance.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.xphone.performance.R;
import com.xphone.performance.util.FormatUtils;

/**
 * Vòng điểm lớn ở giữa màn hình (lấy cảm hứng từ AnTuTu).
 *
 * Cách vẽ:
 *  - Một cung tròn 270° (hở phía dưới) làm "đường ray" màu nhạt.
 *  - Một cung tô gradient hồng → tím → mint, độ dài tỉ lệ với điểm / điểm tối đa.
 *  - Con số điểm ở giữa + nhãn nhỏ phía dưới.
 *
 * Hiệu ứng: setScore(..., true) dùng ValueAnimator chạy số từ 0 lên điểm thật trong 1,4 giây.
 * Mỗi bước animator chỉ gọi invalidate() để vẽ lại, không tạo object mới trong onDraw()
 * (tạo object trong onDraw là một lỗi hiệu năng phổ biến).
 */
public class ScoreRingView extends View {

    private static final float START_ANGLE = 135f;
    private static final float SWEEP_ANGLE = 270f;

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint scorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcRect = new RectF();

    private float progress;       // 0..1, phần cung đang tô
    private int displayedScore;   // số đang hiển thị (chạy theo animation)
    private String label = "Điểm tổng";
    private ValueAnimator animator;

    public ScoreRingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float stroke = dp(16);

        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(stroke);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);
        trackPaint.setColor(ContextCompat.getColor(context, R.color.xp_surface_soft));

        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(stroke);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        scorePaint.setColor(ContextCompat.getColor(context, R.color.xp_text));
        scorePaint.setFakeBoldText(true);
        scorePaint.setTextAlign(Paint.Align.CENTER);

        labelPaint.setColor(ContextCompat.getColor(context, R.color.xp_text_soft));
        labelPaint.setTextAlign(Paint.Align.CENTER);
    }

    /**
     * Đặt điểm hiển thị.
     * @param animate true = chạy số từ 0 lên (dùng khi vừa có kết quả mới)
     */
    public void setScore(int score, int maxScore, boolean animate) {
        float target = maxScore <= 0 ? 0 : Math.min(1f, score / (float) maxScore);
        if (animator != null) animator.cancel();

        if (!animate) {
            progress = target;
            displayedScore = score;
            invalidate();
            return;
        }
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1400);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue();
            progress = target * f;
            displayedScore = Math.round(score * f);
            invalidate();
        });
        animator.start();
    }

    public void setLabel(String label) {
        this.label = label;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        // Tính sẵn khung cung tròn và gradient MỘT LẦN mỗi khi đổi kích thước
        float inset = trackPaint.getStrokeWidth() / 2 + dp(4);
        float size = Math.min(w, h);
        float left = (w - size) / 2f + inset;
        float top = (h - size) / 2f + inset;
        arcRect.set(left, top, left + size - 2 * inset, top + size - 2 * inset);

        int pink = ContextCompat.getColor(getContext(), R.color.xp_primary);
        int lavender = ContextCompat.getColor(getContext(), R.color.xp_secondary);
        int mint = ContextCompat.getColor(getContext(), R.color.xp_accent);
        SweepGradient gradient = new SweepGradient(w / 2f, h / 2f,
                new int[]{pink, lavender, mint, pink}, new float[]{0f, 0.4f, 0.75f, 1f});
        // Xoay gradient để màu hồng bắt đầu đúng tại điểm đầu cung (135°)
        Matrix matrix = new Matrix();
        matrix.setRotate(START_ANGLE, w / 2f, h / 2f);
        gradient.setLocalMatrix(matrix);
        arcPaint.setShader(gradient);

        scorePaint.setTextSize(size * 0.17f);
        labelPaint.setTextSize(size * 0.065f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawArc(arcRect, START_ANGLE, SWEEP_ANGLE, false, trackPaint);
        if (progress > 0) {
            canvas.drawArc(arcRect, START_ANGLE, SWEEP_ANGLE * progress, false, arcPaint);
        }
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        canvas.drawText(FormatUtils.score(displayedScore), cx, cy + scorePaint.getTextSize() * 0.35f, scorePaint);
        canvas.drawText(label, cx, cy + scorePaint.getTextSize() * 1.1f, labelPaint);
    }

    @Override
    protected void onDetachedFromWindow() {
        // Hủy animation khi view rời màn hình, tránh animator giữ view (leak nhỏ)
        if (animator != null) animator.cancel();
        super.onDetachedFromWindow();
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
