package com.example.salestracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.salestracker.fragments.RatingFragment;
import com.example.salestracker.fragments.ReportsFragment;
import com.example.salestracker.fragments.SalesFragment;
import com.example.salestracker.fragments.ScheduleFragment;
import com.example.salestracker.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.List;

public class MainMenuActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigation;
    private TextView tvToolbarTitle;
    private TextView btnMenu;
    private String[] titles = {"Продажи", "Графики", "Отчёты", "Рейтинг"};
    private String currentUserRole;
    private ScheduleFragment scheduleFragment;
    private SalesFragment salesFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setElevation(0);

        tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        viewPager = findViewById(R.id.viewPager);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        btnMenu = findViewById(R.id.btnMenu);

        SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
        currentUserRole = prefs.getString("user_role", "seller");

        viewPager.setUserInputEnabled(false);

        btnMenu.setOnClickListener(v -> showContextMenu());

        List<Fragment> fragments = new ArrayList<>();
        salesFragment = new SalesFragment();
        fragments.add(salesFragment);
        scheduleFragment = new ScheduleFragment();
        fragments.add(scheduleFragment);
        fragments.add(new ReportsFragment());
        fragments.add(new RatingFragment());

        ViewPagerAdapter adapter = new ViewPagerAdapter(this, fragments);
        viewPager.setAdapter(adapter);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_sales) {
                viewPager.setCurrentItem(0);
                tvToolbarTitle.setText(titles[0]);
            } else if (id == R.id.nav_schedule) {
                viewPager.setCurrentItem(1);
                tvToolbarTitle.setText(titles[1]);
            } else if (id == R.id.nav_reports) {
                viewPager.setCurrentItem(2);
                tvToolbarTitle.setText(titles[2]);
            } else if (id == R.id.nav_rating) {
                viewPager.setCurrentItem(3);
                tvToolbarTitle.setText(titles[3]);
            }
            return true;
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0: bottomNavigation.setSelectedItemId(R.id.nav_sales); break;
                    case 1: bottomNavigation.setSelectedItemId(R.id.nav_schedule); break;
                    case 2: bottomNavigation.setSelectedItemId(R.id.nav_reports); break;
                    case 3: bottomNavigation.setSelectedItemId(R.id.nav_rating); break;
                }
                tvToolbarTitle.setText(titles[position]);

                if (position == 1 && scheduleFragment != null && NetworkUtils.isNetworkAvailable(MainMenuActivity.this)) {
                    scheduleFragment.refreshData();
                }
            }
        });

        tvToolbarTitle.setText(titles[0]);
    }

    private void showContextMenu() {
        int currentPage = viewPager.getCurrentItem();

        // ========== МЕНЮ ДЛЯ СТРАНИЦЫ "ПРОДАЖИ" (position 0) ==========
        if (currentPage == 0) {
            final String[] items;

            // РГО не имеет меню продаж
            if (currentUserRole.equals("rgo")) {
                items = new String[]{"Выход"};
            } else {
                items = new String[]{"Редактировать продажи", "Выход"};
            }

            final int exitIndex = items.length - 1;
            final int editIndex = 0;

            new AlertDialog.Builder(this)
                    .setTitle("Меню")
                    .setItems(items, (dialog, which) -> {
                        if (which == exitIndex) {
                            getSharedPreferences("app", MODE_PRIVATE).edit().clear().apply();
                            startActivity(new Intent(MainMenuActivity.this, LoginActivity.class));
                            finish();
                        } else if (which == editIndex && !currentUserRole.equals("rgo")) {
                            startActivity(new Intent(MainMenuActivity.this, EditSalesActivity.class));
                        }
                    })
                    .show();
            return;
        }

        // ========== МЕНЮ ДЛЯ СТРАНИЦЫ "ГРАФИКИ" (position 1) ==========
        if (currentPage == 1) {
            final String[] items;

            if (currentUserRole.equals("owner") || currentUserRole.equals("rgo")) {
                items = new String[]{"Обновить", "Магазины", "Сотрудники", "Изменить планы", "Выход"};
            } else if (currentUserRole.equals("dm")) {
                items = new String[]{"Обновить", "Сотрудники", "Выход"};
            } else {
                items = new String[]{"Обновить", "Выход"};
            }

            final int refreshIndex = 0;
            final int exitIndex = items.length - 1;

            new AlertDialog.Builder(this)
                    .setTitle("Меню")
                    .setItems(items, (dialog, which) -> {
                        if (which == refreshIndex) {
                            if (scheduleFragment != null) {
                                scheduleFragment.refreshData();
                            }
                            return;
                        }
                        if (which == exitIndex) {
                            getSharedPreferences("app", MODE_PRIVATE).edit().clear().apply();
                            startActivity(new Intent(MainMenuActivity.this, LoginActivity.class));
                            finish();
                            return;
                        }
                        if (currentUserRole.equals("owner") || currentUserRole.equals("rgo")) {
                            if (which == 1) {
                                startActivity(new Intent(MainMenuActivity.this, ShopsActivity.class));
                            } else if (which == 2) {
                                startActivity(new Intent(MainMenuActivity.this, EmployeesActivity.class));
                            } else if (which == 3) {
                                startActivity(new Intent(MainMenuActivity.this, PlanSettingsActivity.class));
                            }
                        } else if (currentUserRole.equals("dm")) {
                            if (which == 1) {
                                startActivity(new Intent(MainMenuActivity.this, EmployeesActivity.class));
                            }
                        }
                    })
                    .show();
            return;
        }

        // ========== ДЛЯ ОСТАЛЬНЫХ СТРАНИЦ ==========
        Toast.makeText(this, "Меню доступно на вкладках 'Продажи' и 'Графики'", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}