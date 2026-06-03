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
import com.example.salestracker.fragments.SalesFragment;
import com.example.salestracker.fragments.ScheduleFragment;
import com.example.salestracker.fragments.ReportsFragment;
import com.example.salestracker.fragments.RatingFragment;
import java.util.ArrayList;
import java.util.List;

public class MainMenuActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigation;
    private TextView tvToolbarTitle;
    private TextView btnMenu;
    private String[] titles = {"Продажи", "Графики", "Отчёты", "Рейтинг"};

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

        viewPager.setUserInputEnabled(false);

        btnMenu.setOnClickListener(v -> showContextMenu());

        List<Fragment> fragments = new ArrayList<>();
        fragments.add(new SalesFragment());
        fragments.add(new ScheduleFragment());
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
            }
        });

        tvToolbarTitle.setText(titles[0]);
    }

    private void showContextMenu() {
        int currentPage = viewPager.getCurrentItem();
        final String[] items;

        switch (currentPage) {
            case 0: // Продажи
                items = new String[]{"Выход"};
                break;
            case 1: // Графики
                items = new String[]{"Редактировать смену", "Обновить", "Экспорт в CSV", "Импорт из CSV", "Выход"};
                break;
            case 2: // Отчёты
                items = new String[]{"Изменить планы", "Выход"};
                break;
            case 3: // Рейтинг
                items = new String[]{"Выход"};
                break;
            default:
                items = new String[]{"Выход"};
                break;
        }

        final int exitIndex = items.length - 1;

        new AlertDialog.Builder(this)
                .setTitle("Меню")
                .setItems(items, (dialog, which) -> {
                    // Проверяем, выбран ли "Выход" (последний пункт)
                    if (which == exitIndex) {
                        getSharedPreferences("app", MODE_PRIVATE).edit().clear().apply();
                        startActivity(new Intent(MainMenuActivity.this, LoginActivity.class));
                        finish();
                        return;
                    }

                    // Обработка остальных пунктов меню
                    if (currentPage == 1) {
                        Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("f" + currentPage);
                        if (currentFragment instanceof ScheduleFragment) {
                            ScheduleFragment scheduleFragment = (ScheduleFragment) currentFragment;
                            if (which == 0) {
                                scheduleFragment.showEditDialog();
                            } else if (which == 1) {
                                scheduleFragment.refreshData();
                            } else if (which == 2) {
                                scheduleFragment.exportToExcel();
                            } else if (which == 3) {
                                scheduleFragment.importFromExcel();
                            }
                        }
                    } else if (currentPage == 2 && which == 0) {
                        SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
                        String userRole = prefs.getString("user_role", "seller");
                        if (userRole.equals("dm")) {
                            startActivity(new Intent(MainMenuActivity.this, PlanSettingsActivity.class));
                        } else {
                            Toast.makeText(MainMenuActivity.this, "Доступ только для директора", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .show();
    }
}