package com.xphone.performance.ui.labs;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.xphone.performance.R;
import com.xphone.performance.core.image.ImageLoader;
import com.xphone.performance.data.model.Phone;
import com.xphone.performance.monitor.BindStats;
import com.xphone.performance.util.FormatUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter hiển thị danh sách điện thoại trong RecyclerView (dùng ở màn Smooth và màn CPU).
 *
 * Kỹ thuật tối ưu:
 *  1. RecyclerView tự tái sử dụng view: chỉ inflate khoảng (số dòng trên màn hình + vài dòng),
 *     dù danh sách có hàng nghìn phần tử.
 *  2. ViewHolder giữ sẵn tham chiếu các view con => không gọi findViewById khi cuộn.
 *  3. Ảnh do ImageLoader xử lý (nhỏ + thread nền + cache) => onBind chỉ tốn < 1ms.
 *  4. setHasStableIds(true): RecyclerView nhận diện dòng theo id, giữ đúng view khi dữ liệu đổi.
 *
 * Thời gian mỗi lần bind được ghi vào BindStats (nếu có) để hiển thị trên thanh đo.
 */
public class PhoneAdapter extends RecyclerView.Adapter<PhoneAdapter.PhoneViewHolder> {

    private final List<Phone> phones = new ArrayList<>();
    private final ImageLoader imageLoader;
    @Nullable
    private final BindStats stats;

    public PhoneAdapter(List<Phone> initial, ImageLoader imageLoader, @Nullable BindStats stats) {
        this.phones.addAll(initial);
        this.imageLoader = imageLoader;
        this.stats = stats;
        setHasStableIds(true); // id cố định giúp RecyclerView nhận diện dòng khi dữ liệu đổi
    }

    /**
     * Thay toàn bộ dữ liệu bằng kết quả tìm kiếm mới.
     *
     * Dùng notifyDataSetChanged() thay vì DiffUtil: kết quả tìm kiếm thường khác hẳn danh sách cũ
     * và có thể tới 20.000 dòng. DiffUtil phải so sánh 2 danh sách (thuật toán Myers): đo trên máy ảo,
     * đổi 20.000 -> 2.871 dòng tốn 3.657 ms (xem DiffUtilCostTest). Trong khi đó notifyDataSetChanged()
     * chỉ khiến RecyclerView bind lại ~10 dòng đang hiển thị — rẻ hơn nhiều trong trường hợp này.
     */
    @SuppressWarnings("NotifyDataSetChanged")
    public void setItems(List<Phone> newItems) {
        phones.clear();
        phones.addAll(newItems);
        notifyDataSetChanged();
    }

    /** Giữ sẵn các view con của một dòng. */
    static class PhoneViewHolder extends RecyclerView.ViewHolder {
        final ImageView img;
        final TextView name, sub, price;

        PhoneViewHolder(View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.imgThumb);
            name = itemView.findViewById(R.id.tvName);
            sub = itemView.findViewById(R.id.tvSub);
            price = itemView.findViewById(R.id.tvPrice);
        }
    }

    @NonNull
    @Override
    public PhoneViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_phone, parent, false);
        return new PhoneViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhoneViewHolder holder, int position) {
        long start = System.nanoTime();
        Phone p = phones.get(position);
        holder.name.setText(p.name);
        holder.sub.setText(p.brand + " · " + p.ramGb + "GB RAM");
        holder.price.setText(FormatUtils.price(p.price));
        imageLoader.load(p, holder.img);
        if (stats != null) stats.record(System.nanoTime() - start);
    }

    @Override
    public long getItemId(int position) {
        return phones.get(position).id;
    }

    @Override
    public int getItemCount() {
        return phones.size();
    }
}
