package com.xphone.performance.core.image;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.xphone.performance.data.model.Phone;

/**
 * Vẽ ảnh thumbnail cho một chiếc điện thoại (hoa văn sóng + chữ cái đầu của hãng).
 *
 * Mô phỏng công việc xử lý ảnh của app thật (decode, áp filter...): tính màu cho TỪNG pixel,
 * nên chi phí tỉ lệ với size × size. Đây cũng là "tải" cho bài đo Smooth trong benchmark.
 *
 * Lưu ý hiệu năng:
 *  - Hàm này nặng, KHÔNG BAO GIỜ gọi trên main thread (ImageLoader gọi nó ở thread nền).
 *  - Chỉ vẽ đúng kích thước cần hiển thị (ImageLoader.SIZE), không vẽ ảnh to rồi thu nhỏ.
 *  - Tính toàn bộ pixel vào mảng int[] rồi setPixels() một lần, nhanh hơn setPixel() từng điểm.
 */
public final class ThumbnailPainter {

    private ThumbnailPainter() {
    }

    public static Bitmap paint(Phone phone, int size) {
        // ===== Bước 1: tính màu từng pixel =====
        int[] pixels = new int[size * size];
        int base = phone.color;
        float scale = 12f / size; // giữ hoa văn giống nhau ở mọi kích thước
        for (int y = 0; y < size; y++) {
            double cosY = Math.cos(y * scale); // tính 1 lần cho cả hàng, không lặp lại trong vòng x
            for (int x = 0; x < size; x++) {
                double wave = Math.sin(x * scale + phone.id) * cosY;
                float t = (float) ((wave + 1) / 2) * 0.55f; // 0..0,55: tỉ lệ pha trắng
                pixels[y * size + x] = blendWithWhite(base, t);
            }
        }

        // ===== Bước 2: tạo Bitmap và vẽ chữ cái đầu của hãng =====
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, size, 0, 0, size, size);

        Canvas canvas = new Canvas(bitmap);
        Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        text.setColor(Color.WHITE);
        text.setFakeBoldText(true);
        text.setTextSize(size * 0.45f);
        text.setTextAlign(Paint.Align.CENTER);
        float baseline = size / 2f - (text.descent() + text.ascent()) / 2f;
        canvas.drawText(phone.brand.substring(0, 1), size / 2f, baseline, text);
        return bitmap;
    }

    /** Trộn màu gốc với màu trắng theo tỉ lệ t (0 = giữ nguyên, 1 = trắng hẳn). */
    private static int blendWithWhite(int color, float t) {
        int r = (int) (Color.red(color) + (255 - Color.red(color)) * t);
        int g = (int) (Color.green(color) + (255 - Color.green(color)) * t);
        int b = (int) (Color.blue(color) + (255 - Color.blue(color)) * t);
        return Color.rgb(r, g, b);
    }
}
