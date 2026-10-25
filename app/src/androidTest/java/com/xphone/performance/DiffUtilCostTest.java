package com.xphone.performance;

import android.util.Log;

import androidx.recyclerview.widget.DiffUtil;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.xphone.performance.core.search.PhoneSearch;
import com.xphone.performance.data.FakePhoneRepository;
import com.xphone.performance.data.model.Phone;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

/**
 * Đo trên máy thật / máy ảo: DiffUtil tốn bao lâu khi đổi danh sách 20.000 máy sang kết quả tìm "pro".
 * Số liệu này dùng cho mục "DiffUtil" trong Nhật ký tối ưu. Xem kết quả trong Logcat, tag "DiffCost".
 */
@RunWith(AndroidJUnit4.class)
public class DiffUtilCostTest {

    @Test
    public void measureDiffUtilOn20kRows() {
        List<Phone> phones = FakePhoneRepository.generate(20_000);
        PhoneSearch search = new PhoneSearch(phones);
        List<Phone> oldList = search.search("").phones;
        List<Phone> newList = search.search("pro").phones;

        long start = System.nanoTime();
        DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldList.size();
            }

            @Override
            public int getNewListSize() {
                return newList.size();
            }

            @Override
            public boolean areItemsTheSame(int o, int n) {
                return oldList.get(o).id == newList.get(n).id;
            }

            @Override
            public boolean areContentsTheSame(int o, int n) {
                return true;
            }
        }, false);
        double ms = (System.nanoTime() - start) / 1_000_000.0;
        Log.i("DiffCost", "DiffUtil 20000 -> " + newList.size() + " rows: " + ms + " ms");
    }
}
