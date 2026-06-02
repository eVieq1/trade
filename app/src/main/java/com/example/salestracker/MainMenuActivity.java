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
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        viewPager = findViewById(R.id.viewPager);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        btnMenu = findViewById(R.id.btnMenu);

        viewPager.setUserInputEnabled(false);

        btnMenu.setOnClickListener(v -> showMenuDialog());

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
                    case 0:
                        bottomNavigation.setSelectedItemId(R.id.nav_sales);
                        break;
                    case 1:
                        bottomNavigation.setSelectedItemId(R.id.nav_schedule);
                        break;
                    case 2:
                        bottomNavigation.setSelectedItemId(R.id.nav_reports);
                        break;
                    case 3:
                        bottomNavigation.setSelectedItemId(R.id.nav_rating);
                        break;
                }
                tvToolbarTitle.setText(titles[position]);
            }
        });

        tvToolbarTitle.setText(titles[0]);
    }

    private void showMenuDialog() {
        int currentTab = viewPager.getCurrentItem();
        String[] items;
        String title = "Меню";

        if (currentTab == 0) { // Продажи
            Toast.makeText(this, "Меню недоступно на этой вкладке", Toast.LENGTH_SHORT).show();
            return;
        } else if (currentTab == 1) { // Графики
            // ДОБАВЛЕНА КНОПКА "ОБНОВИТЬ"
            items = new String[]{"Редактировать смену", "Обновить", "Выход"};
            title = "Графики";
        } else if (currentTab == 2) { // Отчёты
            items = new String[]{"Изменить планы", "Выход"};
            title = "Отчёты";
        } else { // Рейтинг
            Toast.makeText(this, "Меню недоступно на этой вкладке", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setItems(items, (dialog, which) -> {
                    if (items[which].equals("Редактировать смену")) {
                        Fragment currentFragment = getSupportFragmentManager()
                                .findFragmentByTag("f" + viewPager.getCurrentItem());
                        if (currentFragment instanceof ScheduleFragment) {
                            ((ScheduleFragment) currentFragment).showEditDialog();
                        } else {
                            Toast.makeText(this, "Ошибка: фрагмент не найден", Toast.LENGTH_SHORT).show();
                        }
                    } else if (items[which].equals("Обновить")) {
                        // ОБНОВЛЕНИЕ ДАННЫХ НА СТРАНИЦЕ ГРАФИКОВ
                        Fragment currentFragment = getSupportFragmentManager()
                                .findFragmentByTag("f" + viewPager.getCurrentItem());
                        if (currentFragment instanceof ScheduleFragment) {
                            ((ScheduleFragment) currentFragment).refreshData();
                            Toast.makeText(this, "🔄 Данные обновлены", Toast.LENGTH_SHORT).show();
                        }
                    } else if (items[which].equals("Изменить планы")) {
                        SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
                        String userRole = prefs.getString("user_role", "seller");
                        if (userRole.equals("dm")) {
                            startActivity(new Intent(MainMenuActivity.this, PlanSettingsActivity.class));
                        } else {
                            Toast.makeText(this, "Доступ только для директора", Toast.LENGTH_SHORT).show();
                        }
                    } else if (items[which].equals("Выход")) {
                        getSharedPreferences("app", MODE_PRIVATE).edit().clear().apply();
                        startActivity(new Intent(MainMenuActivity.this, LoginActivity.class));
                        finish();
                    }
                })
                .show();
    }
}