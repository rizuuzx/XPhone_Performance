package com.xphone.performance.data.model;

/**
 * Một sản phẩm điện thoại trong cửa hàng giả lập "XPhone".
 * Dữ liệu này được các phòng lab dùng làm "tải" (workload) để đo hiệu năng.
 */
public class Phone {

    public final int id;
    public final String name;   // ví dụ "Galaxy Nova 12 Pro"
    public final String brand;  // ví dụ "Samsung"
    public final int price;     // giá bán (VND)
    public final int ramGb;     // RAM (GB)
    public final int color;     // màu pastel dùng để vẽ ảnh thumbnail

    public Phone(int id, String name, String brand, int price, int ramGb, int color) {
        this.id = id;
        this.name = name;
        this.brand = brand;
        this.price = price;
        this.ramGb = ramGb;
        this.color = color;
    }

    /** "Tầm giá" tính theo triệu đồng: 12.990.000 -> 12. Dùng để đếm máy cùng tầm giá. */
    public int priceBucket() {
        return price / 1_000_000;
    }
}
