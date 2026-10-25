package com.xphone.performance.benchmark;

import java.util.Arrays;
import java.util.Locale;

/**
 * Bài 3 — MEMORY: đo tốc độ làm việc với bộ nhớ của máy.
 *
 * Mỗi vòng lặp làm 3 việc trên khối 8MB (giống việc app xử lý ảnh / dữ liệu lớn):
 *  1. Ghi    : Arrays.fill() điền dữ liệu vào khối nguồn
 *  2. Copy   : System.arraycopy() sang khối đích (hàm native, nhanh nhất có thể)
 *  3. Đọc    : cộng dồn 1 byte mỗi 4KB để chắc chắn dữ liệu thực sự được đọc
 * Lặp 24 vòng, tính tổng số MB đã xử lý / thời gian => MB/s.
 *
 * Lưu ý thiết kế:
 *  - Chỉ cấp phát 2 khối 8MB DUY NHẤT rồi dùng lại, không cấp phát trong vòng lặp
 *    (tránh tạo rác => GC chạy giữa chừng làm sai số đo).
 *  - Khi xong, 2 khối này hết tham chiếu và GC thu hồi được ngay => không rò rỉ.
 */
public class MemoryBenchmark implements BenchmarkTask {

    private static final int BLOCK_BYTES = 8 * 1024 * 1024;
    private static final int ROUNDS = 24;

    @Override
    public String name() {
        return "Đo tốc độ bộ nhớ";
    }

    @Override
    public TaskResult run() {
        byte[] source = new byte[BLOCK_BYTES];
        byte[] target = new byte[BLOCK_BYTES];
        long checksum = 0;

        long start = System.nanoTime();
        for (int round = 0; round < ROUNDS; round++) {
            Arrays.fill(source, (byte) round);                     // ghi
            System.arraycopy(source, 0, target, 0, BLOCK_BYTES);   // copy
            for (int i = 0; i < BLOCK_BYTES; i += 4096) {          // đọc
                checksum += target[i];
            }
        }
        double seconds = (System.nanoTime() - start) / 1_000_000_000.0;

        // Mỗi vòng: ghi 8MB + copy 8MB (đọc 8MB, ghi 8MB) = 24MB lưu lượng
        double totalMb = ROUNDS * 3.0 * BLOCK_BYTES / (1024 * 1024);
        double mbps = totalMb / Math.max(seconds, 1e-6);

        // Dùng checksum để trình biên dịch không "lược bỏ" vòng đọc
        String detail = String.format(Locale.GERMANY, "%,.0f MB/s", mbps) + (checksum == -1 ? "!" : "");
        return new TaskResult(ScoreCalculator.memory(mbps), mbps, detail);
    }
}
