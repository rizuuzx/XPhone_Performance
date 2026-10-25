package com.xphone.performance.core.image;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import com.xphone.performance.data.model.Phone;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Tải ảnh thumbnail cho danh sách mà không làm giật giao diện.
 *
 * 3 kỹ thuật tối ưu:
 *  1. Kích thước vừa đủ: 160×160 px cho ô 56dp (~100KB/ảnh). Ảnh 900×900 sẽ tốn ~3,2MB
 *     và chậm hơn ~30 lần mà mắt thường không thấy khác.
 *  2. Thread nền: việc vẽ chạy trên ExecutorService 2 luồng. Main thread chỉ gán Bitmap
 *     đã có sẵn, nên mỗi frame vẫn trong ngưỡng 16,7ms.
 *  3. LruCache: ảnh đã vẽ được giữ lại, cuộn ngược lên là hiện ngay. Cache giới hạn 1/8 heap
 *     và tự bỏ ảnh ít dùng nhất khi đầy, nên không bao giờ gây OutOfMemory.
 *
 * Xử lý "ô bị tái sử dụng" (RecyclerView dùng lại View khi cuộn):
 *  - Trước khi vẽ, gắn tag = id điện thoại vào ImageView.
 *  - Khi vẽ xong, chỉ gán ảnh nếu tag vẫn đúng id đó; nếu ô đã chuyển sang máy khác thì bỏ qua.
 *
 * Vòng đời: gọi shutdown() trong onDestroy() để dừng thread nền và giải phóng cache.
 */
public class ImageLoader {

    public static final int SIZE = 160;

    private final LruCache<Integer, Bitmap> cache;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public ImageLoader() {
        // sizeOf tính theo KB thực tế của từng Bitmap
        int maxKb = (int) (Runtime.getRuntime().maxMemory() / 1024 / 8);
        cache = new LruCache<Integer, Bitmap>(maxKb) {
            @Override
            protected int sizeOf(Integer key, Bitmap value) {
                return value.getByteCount() / 1024;
            }
        };
    }

    /** Gọi trên main thread từ onBindViewHolder. Luôn trả về ngay, không chặn. */
    public void load(Phone phone, ImageView target) {
        target.setTag(phone.id);

        // Có sẵn trong cache => hiển thị ngay
        Bitmap cached = cache.get(phone.id);
        if (cached != null) {
            target.setImageBitmap(cached);
            return;
        }

        // Chưa có => để ô trống, vẽ ở background rồi quay về main thread để gán
        target.setImageDrawable(null);
        executor.execute(() -> {
            Bitmap bitmap = ThumbnailPainter.paint(phone, SIZE);
            cache.put(phone.id, bitmap);
            mainHandler.post(() -> {
                Object tag = target.getTag();
                if (tag instanceof Integer && (Integer) tag == phone.id) {
                    target.setImageBitmap(bitmap);
                }
            });
        });
    }

    public void shutdown() {
        executor.shutdownNow();
        mainHandler.removeCallbacksAndMessages(null);
        cache.evictAll();
    }
}
