package com.example.salestracker;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.example.salestracker.adapters.EmployeeDetailPagerAdapter;

public class EmployeeDetailActivity extends AppCompatActivity {

    private TextView tvEmployeeName, tvEmployeeRole, tvEmployeeOffice, tvEmployeeBirthday;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private EmployeeDetailPagerAdapter pagerAdapter;

    private int employeeId;
    private String employeeName;
    private String employeeRole;
    private String employeeOffice;
    private String currentUserRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");

        // Получаем данные из Intent
        employeeId = getIntent().getIntExtra("employee_id", 0);
        employeeName = getIntent().getStringExtra("employee_name");
        employeeRole = getIntent().getStringExtra("employee_role");
        employeeOffice = getIntent().getStringExtra("employee_office");

        SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
        currentUserRole = prefs.getString("user_role", "seller");

        initViews();
        setupViewPager();
    }

    private void initViews() {
        tvEmployeeName = findViewById(R.id.tvEmployeeName);
        tvEmployeeRole = findViewById(R.id.tvEmployeeRole);
        tvEmployeeOffice = findViewById(R.id.tvEmployeeOffice);
        tvEmployeeBirthday = findViewById(R.id.tvEmployeeBirthday);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        tvEmployeeName.setText(employeeName != null ? employeeName : "Сотрудник");
        tvEmployeeRole.setText(employeeRole != null ? employeeRole : "Специалист");
        tvEmployeeOffice.setText(employeeOffice != null ? employeeOffice : "Не привязан");
        tvEmployeeBirthday.setText("Дата рождения: —");
    }

    private void setupViewPager() {
        pagerAdapter = new EmployeeDetailPagerAdapter(this, employeeId, currentUserRole, employeeName);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("📊 ПРОДАЖИ");
                            break;
                        case 1:
                            tab.setText("🎓 ОБУЧЕНИЕ");
                            break;
                        case 2:
                            tab.setText("🏆 ДОСТИЖЕНИЯ");
                            break;
                        case 3:
                            tab.setText("📈 ПЛАН");
                            break;
                        case 4:
                            tab.setText("✅ ЗАДАЧИ");
                            break;
                        case 5:
                            tab.setText("🤝 ДОГОВОР");
                            break;
                    }
                }
        ).attach();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}