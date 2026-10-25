package com.xphone.performance.data;

import com.xphone.performance.data.model.Phone;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Sinh danh sách điện thoại giả lập để làm dữ liệu thử nghiệm.
 *
 * Cách hoạt động:
 *  - Dùng Random với seed cố định (2026) => mỗi lần chạy đều ra CÙNG một danh sách.
 *    Nhờ vậy điểm benchmark giữa các lần đo / các máy là công bằng (cùng đầu vào).
 *  - Tên máy = hãng + dòng + số đời + hậu tố, giá từ 2 đến 40 triệu.
 */
public final class FakePhoneRepository {

    private static final String[] BRANDS = {"Samsung", "Apple", "Xiaomi", "Oppo", "Vivo", "Realme", "Nokia", "Sony"};
    private static final String[] SERIES = {"Galaxy", "Nova", "Mi", "Reno", "Find", "Pixel", "Aurora", "Neo", "Zen", "Lumi"};
    private static final String[] SUFFIX = {"", " Pro", " Max", " Ultra", " Lite", " Plus", " Mini"};

    /** Màu pastel cho ảnh thumbnail (cùng tông với giao diện). */
    private static final int[] PASTELS = {
            0xFFFF8FAB, 0xFFB8A4F4, 0xFF7FD8BE, 0xFFFFC3A0, 0xFFA0D2FF, 0xFFFFE08A
    };

    private FakePhoneRepository() {
    }

    public static List<Phone> generate(int count) {
        Random random = new Random(2026);
        List<Phone> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String brand = BRANDS[random.nextInt(BRANDS.length)];
            String name = SERIES[random.nextInt(SERIES.length)] + " "
                    + (random.nextInt(20) + 1)
                    + SUFFIX[random.nextInt(SUFFIX.length)];
            // Giá là bội số của 10.000đ, từ 2 triệu đến dưới 40 triệu
            int price = 2_000_000 + random.nextInt(3_800) * 10_000;
            int ram = 4 << random.nextInt(3); // 4, 8 hoặc 16 GB
            list.add(new Phone(i, name, brand, price, ram, PASTELS[i % PASTELS.length]));
        }
        return list;
    }
}
