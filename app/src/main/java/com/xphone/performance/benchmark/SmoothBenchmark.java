package com.xphone.performance.benchmark;

import com.xphone.performance.core.image.ImageLoader;
import com.xphone.performance.core.image.ThumbnailPainter;
import com.xphone.performance.data.FakePhoneRepository;
import com.xphone.performance.data.model.Phone;
import com.xphone.performance.util.FormatUtils;

import java.util.List;

/**
 * Bài 1 — SMOOTH: đo tốc độ xử lý ảnh của máy.
 *
 * Cách đo:
 *  - Vẽ 60 ảnh thumbnail khác nhau (đúng kích thước app dùng khi hiển thị danh sách).
 *  - KHÔNG dùng cache, để đo đúng sức của CPU chứ không đo tốc độ đọc cache.
 *  - Vẽ 5 ảnh "khởi động" trước (warm-up) để JIT biên dịch xong code, số đo ổn định hơn.
 *  - Số đo = thời gian trung bình / ảnh. Máy vẽ 1 ảnh càng nhanh thì càng ít nguy cơ
 *    trễ frame khi cuộn danh sách nhiều hình.
 */
public class SmoothBenchmark implements BenchmarkTask {

    private static final int IMAGES = 60;
    private static final int WARM_UP = 5;

    @Override
    public String name() {
        return "Đo xử lý đồ họa";
    }

    @Override
    public TaskResult run() {
        List<Phone> phones = FakePhoneRepository.generate(IMAGES);

        for (int i = 0; i < WARM_UP; i++) {
            ThumbnailPainter.paint(phones.get(i), ImageLoader.SIZE);
        }

        long start = System.nanoTime();
        for (Phone p : phones) {
            ThumbnailPainter.paint(p, ImageLoader.SIZE).recycle(); // trả bộ nhớ ngay, đỡ việc cho GC
        }
        double avgMs = (System.nanoTime() - start) / 1_000_000.0 / IMAGES;

        return new TaskResult(ScoreCalculator.smooth(avgMs), avgMs,
                "TB " + FormatUtils.ms(avgMs) + " / ảnh");
    }
}
