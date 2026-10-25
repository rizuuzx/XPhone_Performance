package com.xphone.performance.benchmark;

import com.xphone.performance.core.search.PhoneSearch;
import com.xphone.performance.data.FakePhoneRepository;
import com.xphone.performance.data.model.Phone;
import com.xphone.performance.util.FormatUtils;

import java.util.List;

/**
 * Bài 2 — CPU: đo tốc độ xử lý dữ liệu (lọc, sắp xếp, thống kê) trên cửa hàng 20.000 máy.
 *
 * Cách đo:
 *  - Thời gian dựng chỉ mục (PhoneSearch) được tính vào, cho đúng thực tế.
 *  - Chạy 5 truy vấn giống người dùng thật, gồm cả truy vấn rỗng (= hiển thị tất cả, nặng nhất
 *    vì phải sắp xếp toàn bộ 20.000 máy).
 *  - Số đo = tổng thời gian (ms).
 */
public class CpuBenchmark implements BenchmarkTask {

    private static final int PHONE_COUNT = 20_000;
    private static final String[] QUERIES = {"", "pro", "sam", "galaxy 1", "ultra"};

    @Override
    public String name() {
        return "Đo xử lý dữ liệu (CPU)";
    }

    @Override
    public TaskResult run() {
        List<Phone> phones = FakePhoneRepository.generate(PHONE_COUNT);

        long start = System.nanoTime();
        PhoneSearch search = new PhoneSearch(phones);
        for (String q : QUERIES) {
            search.search(q);
        }
        double totalMs = (System.nanoTime() - start) / 1_000_000.0;

        return new TaskResult(ScoreCalculator.cpu(totalMs), totalMs,
                "20.000 máy · " + QUERIES.length + " truy vấn · " + FormatUtils.ms(totalMs));
    }
}
