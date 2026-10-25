package com.xphone.performance.benchmark;

/**
 * Một bài kiểm tra thành phần (Smooth / CPU / Memory).
 *
 * Mỗi bài chạy một khối công việc cố định, đo thời gian / tốc độ của máy rồi quy ra điểm.
 * Hàm run() luôn được gọi trên THREAD NỀN (xem BenchmarkRunner).
 */
public interface BenchmarkTask {

    /** Tên hiển thị khi đang chạy, ví dụ "Đo xử lý đồ họa". */
    String name();

    TaskResult run();

    /** Kết quả một bài: điểm, số liệu đo gốc và mô tả ngắn. */
    class TaskResult {
        public final int score;
        public final double value;
        public final String detail;

        public TaskResult(int score, double value, String detail) {
            this.score = score;
            this.value = value;
            this.detail = detail;
        }
    }
}
