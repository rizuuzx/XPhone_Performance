package com.xphone.performance.core.search;

import com.xphone.performance.data.model.Phone;

import java.util.List;

/**
 * Kết quả một lần tìm kiếm.
 *
 *  - phones          : danh sách máy khớp từ khóa, sắp xếp theo giá tăng dần
 *  - sameRangeCounts : sameRangeCounts[i] = số máy (trong toàn cửa hàng) cùng tầm giá với phones[i]
 *  - elapsedMs       : thời gian xử lý
 */
public class SearchResult {

    public final List<Phone> phones;
    public final int[] sameRangeCounts;
    public final double elapsedMs;

    public SearchResult(List<Phone> phones, int[] sameRangeCounts, double elapsedMs) {
        this.phones = phones;
        this.sameRangeCounts = sameRangeCounts;
        this.elapsedMs = elapsedMs;
    }
}
