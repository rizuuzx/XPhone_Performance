package com.xphone.performance;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.xphone.performance.benchmark.ScoreCalculator;
import com.xphone.performance.core.search.PhoneSearch;
import com.xphone.performance.core.search.SearchResult;
import com.xphone.performance.data.FakePhoneRepository;
import com.xphone.performance.data.model.Phone;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Unit test chạy trên máy tính (không cần điện thoại).
 *
 *  1. Tối ưu hiệu năng KHÔNG được làm sai kết quả: PhoneSearch (có chỉ mục, HashMap, TimSort)
 *     phải cho kết quả y hệt một cách tính "thẳng" đơn giản, dễ kiểm chứng bằng mắt.
 *  2. Công thức chấm điểm đúng như mô tả trong ScoreCalculator.
 */
public class PhoneSearchTest {

    /** Cách tính đơn giản để đối chiếu: lọc, sắp xếp chèn, đếm bằng vòng lặp lồng nhau. */
    private static SearchResult referenceSearch(List<Phone> all, String query) {
        String q = query.toLowerCase(Locale.ROOT).trim();
        List<Phone> matched = new ArrayList<>();
        for (Phone p : all) {
            if ((p.name + " " + p.brand).toLowerCase(Locale.ROOT).contains(q)) {
                // chèn vào đúng vị trí để danh sách luôn được sắp theo giá rồi theo id
                int pos = 0;
                while (pos < matched.size() && PhoneSearch.BY_PRICE.compare(matched.get(pos), p) < 0) pos++;
                matched.add(pos, p);
            }
        }
        int[] counts = new int[matched.size()];
        for (int i = 0; i < matched.size(); i++) {
            for (Phone other : all) {
                if (other.priceBucket() == matched.get(i).priceBucket()) counts[i]++;
            }
        }
        return new SearchResult(matched, counts, 0);
    }

    @Test
    public void search_matchesReferenceImplementation() {
        List<Phone> phones = FakePhoneRepository.generate(800);
        PhoneSearch search = new PhoneSearch(phones);

        for (String q : new String[]{"", "pro", "SAM", "galaxy 1", "khong-ton-tai"}) {
            SearchResult expected = referenceSearch(phones, q);
            SearchResult actual = search.search(q);

            assertEquals("Số kết quả khác nhau với từ khóa '" + q + "'", expected.phones.size(), actual.phones.size());
            for (int i = 0; i < expected.phones.size(); i++) {
                assertEquals(expected.phones.get(i).id, actual.phones.get(i).id);
            }
            assertArrayEquals(expected.sameRangeCounts, actual.sameRangeCounts);
        }
    }

    @Test
    public void score_followsFormula() {
        // Thời gian = 0 => điểm tối đa; thời gian = mốc => một nửa
        assertEquals(ScoreCalculator.MAX_SMOOTH, ScoreCalculator.smooth(0));
        assertEquals(ScoreCalculator.MAX_SMOOTH / 2, ScoreCalculator.smooth(16.67));
        assertTrue(ScoreCalculator.cpu(500) < ScoreCalculator.cpu(5));
        // Tốc độ = mốc => một nửa; tốc độ cao hơn => điểm cao hơn
        assertEquals(ScoreCalculator.MAX_MEMORY / 2, ScoreCalculator.memory(4000));
        assertTrue(ScoreCalculator.memory(10_000) > ScoreCalculator.memory(2_000));
    }
}
