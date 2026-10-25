package com.xphone.performance.ui.main;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.xphone.performance.R;
import com.xphone.performance.ui.device.DeviceInfoActivity;
import com.xphone.performance.ui.history.HistoryFragment;
import com.xphone.performance.ui.home.HomeFragment;
import com.xphone.performance.ui.labs.LabsFragment;
import com.xphone.performance.util.UiUtils;

/**
 * Màn hình khung của app.
 *
 * Cách hoạt động:
 *  - Header phía trên: tên app + nút (i) mở DeviceInfoActivity.
 *  - Giữa: FragmentContainerView, hiển thị 1 trong 3 Fragment (Home / Labs / History).
 *  - Dưới: BottomNavigationView, bấm tab nào thì thay Fragment tương ứng.
 *  - Khi xoay màn hình, Android tự khôi phục Fragment cũ (savedInstanceState != null)
 *    nên chỉ gắn HomeFragment ở lần mở đầu tiên.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        UiUtils.setupEdgeToEdge(this, findViewById(R.id.main));

        findViewById(R.id.btnDeviceInfo).setOnClickListener(v ->
                startActivity(new Intent(this, DeviceInfoActivity.class)));

        BottomNavigationView nav = findViewById(R.id.bottomNav);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                show(new HomeFragment());
            } else if (id == R.id.nav_labs) {
                show(new LabsFragment());
            } else if (id == R.id.nav_history) {
                show(new HistoryFragment());
            }
            return true;
        });

        if (savedInstanceState == null) {
            show(new HomeFragment());
        }
    }

    /** Thay Fragment đang hiển thị. */
    private void show(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
