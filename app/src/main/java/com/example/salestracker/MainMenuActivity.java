package com.example.salestracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.example.salestracker.fragments.SalesFragment;
import com.example.salestracker.fragments.ScheduleFragment;
import com.example.salestracker.fragments.ReportsFragment;
import com.example.salestracker.fragments.RatingFragment;

public class MainMenuActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView tvToolbarTitle, tvAvatar;
    private ImageView ivBack, ivNotification;
    private BottomNavigationView bottomNavigation;
    private String currentUserRole;
    private String currentEmployee;

    private DashboardFragment dashboardFragment;
    private SalesFragment salesFragment;
    private ScheduleFragment scheduleFragment;
    private ReportsFragment reportsFragment;
    private RatingFragment ratingFragment;

    private boolean isDashboardMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        initViews();
        loadUserData();
        setupToolbar();
        setupAvatarClick();
        setupBottomNavigation();

        showDashboardMode();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        tvAvatar = findViewById(R.id.tvAvatar);
        ivBack = findViewById(R.id.ivBack);
        ivNotification = findViewById(R.id.ivNotification);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    private void loadUserData() {
        SharedPreferences prefs = getSharedPreferences("app", Context.MODE_PRIVATE);
        currentUserRole = prefs.getString("user_role", "seller");
        currentEmployee = prefs.getString("employee_name", "Анна Иванова");

        String firstName = currentEmployee.split(" ")[0];
        String avatarLetter = firstName.substring(0, 1);
        tvAvatar.setText(avatarLetter);
    }

    private void setupToolbar() {
        ivBack.setOnClickListener(v -> onBackPressed());
        ivNotification.setOnClickListener(v -> {
            Toast.makeText(this, "Уведомления", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupAvatarClick() {
        tvAvatar.setOnClickListener(v -> {
            String[] items = {"Профиль", "Выход"};
            new AlertDialog.Builder(this)
                    .setTitle("Меню пользователя")
                    .setItems(items, (dialog, which) -> {
                        if (which == 0) {
                            openProfile();
                        } else if (which == 1) {
                            logout();
                        }
                    })
                    .show();
        });
    }

    private void openProfile() {
        SharedPreferences prefs = getSharedPreferences("app", Context.MODE_PRIVATE);
        int employeeId = prefs.getInt("employee_id", 0);
        String employeeName = prefs.getString("employee_name", "");
        String employeeRole = prefs.getString("user_role", "seller");
        String employeeOffice = prefs.getString("office_name", "Не привязан");

        Intent intent = new Intent(this, EmployeeDetailActivity.class);
        intent.putExtra("employee_id", employeeId);
        intent.putExtra("employee_name", employeeName);
        intent.putExtra("employee_role", employeeRole);
        intent.putExtra("employee_office", employeeOffice);
        startActivity(intent);
    }

    private void logout() {
        getSharedPreferences("app", MODE_PRIVATE).edit().clear().apply();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                showDashboardMode();
                return true;
            } else if (id == R.id.nav_sales) {
                showAppMode(getSalesFragment(), "Продажи");
                return true;
            } else if (id == R.id.nav_schedule) {
                showAppMode(getScheduleFragment(), "Графики");
                return true;
            } else if (id == R.id.nav_reports) {
                showAppMode(getReportsFragment(), "Отчёты");
                return true;
            } else if (id == R.id.nav_rating) {
                showAppMode(getRatingFragment(), "Рейтинг");
                return true;
            }
            return false;
        });
    }

    private void showDashboardMode() {
        isDashboardMode = true;
        toolbar.setVisibility(View.GONE);
        bottomNavigation.setVisibility(View.GONE);
        loadFragment(new DashboardFragment());
    }

    public void showAppMode(Fragment fragment, String title) {
        isDashboardMode = false;
        toolbar.setVisibility(View.VISIBLE);
        bottomNavigation.setVisibility(View.VISIBLE);
        tvToolbarTitle.setText(title);
        ivBack.setVisibility(View.VISIBLE);
        loadFragment(fragment);
        bottomNavigation.setSelectedItemId(getNavIdByTitle(title));
    }

    private int getNavIdByTitle(String title) {
        switch (title) {
            case "Продажи": return R.id.nav_sales;
            case "Графики": return R.id.nav_schedule;
            case "Отчёты": return R.id.nav_reports;
            case "Рейтинг": return R.id.nav_rating;
            default: return R.id.nav_dashboard;
        }
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.container, fragment);
        transaction.commit();
    }

    public SalesFragment getSalesFragment() {
        if (salesFragment == null) salesFragment = new SalesFragment();
        return salesFragment;
    }

    public ScheduleFragment getScheduleFragment() {
        if (scheduleFragment == null) scheduleFragment = new ScheduleFragment();
        return scheduleFragment;
    }

    public ReportsFragment getReportsFragment() {
        if (reportsFragment == null) reportsFragment = new ReportsFragment();
        return reportsFragment;
    }

    public RatingFragment getRatingFragment() {
        if (ratingFragment == null) ratingFragment = new RatingFragment();
        return ratingFragment;
    }

    @Override
    public void onBackPressed() {
        if (!isDashboardMode) {
            showDashboardMode();
        } else {
            super.onBackPressed();
        }
    }
}