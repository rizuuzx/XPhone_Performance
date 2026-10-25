package com.xphone.performance.ui.labs;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.xphone.performance.R;
import com.xphone.performance.ui.labs.cpu.CpuLabActivity;
import com.xphone.performance.ui.labs.lag.LagLabActivity;
import com.xphone.performance.ui.labs.memory.MemoryLabActivity;
import com.xphone.performance.ui.log.OptimizationLogActivity;
import com.xphone.performance.data.OptimizationLog;

/**
 * Thẻ Nhật ký tối ưu + danh sách 3 màn kiểm tra trực tiếp. Mỗi thẻ dùng chung layout item_lab_card,
 * hàm bindCard() điền icon / tên / mô tả và gắn sự kiện mở Activity.
 */
public class LabsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_labs, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        bindCard(view.findViewById(R.id.labLog), "📋", "Nhật ký tối ưu",
                OptimizationLog.all().size() + " vấn đề hiệu năng đã phát hiện và sửa, kèm số đo trước / sau",
                OptimizationLogActivity.class);
        bindCard(view.findViewById(R.id.labLag), "🌊", "Kiểm tra độ mượt",
                "Cuộn danh sách 300 máy có ảnh, đo FPS và số lần giật", LagLabActivity.class);
        bindCard(view.findViewById(R.id.labCpu), "⚡", "Kiểm tra CPU",
                "Tìm kiếm trực tiếp trong 20.000 máy, đo thời gian xử lý", CpuLabActivity.class);
        bindCard(view.findViewById(R.id.labMemory), "🧠", "Kiểm tra rò rỉ bộ nhớ",
                "Mở/đóng màn hình 8MB rồi xác nhận bộ nhớ được trả lại", MemoryLabActivity.class);
    }

    private void bindCard(View card, String icon, String name, String desc, Class<?> target) {
        ((TextView) card.findViewById(R.id.tvLabIcon)).setText(icon);
        ((TextView) card.findViewById(R.id.tvLabName)).setText(name);
        ((TextView) card.findViewById(R.id.tvLabDesc)).setText(desc);
        card.setOnClickListener(v -> startActivity(new Intent(requireContext(), target)));
    }
}
