package com.example.salestracker;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.salestracker.adapters.TodayShiftsAdapter;
import com.example.salestracker.fragments.ScheduleFragment;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TodayShiftsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TodayShiftsAdapter adapter;
    private ApiClient apiClient;
    private int officeId;
    private String officeName;
    private TextView tvOfficeName, tvDate, tvEmpty;
    private List<Employee> employees = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_today_shifts);

        officeId = getIntent().getIntExtra("office_id", 0);
        officeName = getIntent().getStringExtra("office_name");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Сегодняшние смены");

        tvOfficeName = findViewById(R.id.tvOfficeName);
        tvDate = findViewById(R.id.tvDate);
        tvEmpty = findViewById(R.id.tvEmpty);
        recyclerView = findViewById(R.id.recyclerViewShifts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        tvOfficeName.setText(officeName);
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        tvDate.setText(sdf.format(Calendar.getInstance().getTime()));

        apiClient = new ApiClient();

        // Сначала загружаем сотрудников офиса, потом смены
        loadEmployeesOfOffice();
    }

    private void loadEmployeesOfOffice() {
        apiClient.getEmployees(new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    JSONArray arr = obj.getJSONArray("employees");
                    employees.clear();

                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject emp = arr.getJSONObject(i);
                        int empOfficeId = emp.optInt("office_id", 0);
                        // Только сотрудники этого офиса
                        if (empOfficeId == officeId) {
                            employees.add(new Employee(emp.getInt("id"), emp.getString("name"), emp.getString("role")));
                        }
                    }

                    loadTodayShifts();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(String error) {
                loadTodayShifts();
            }
        });
    }

    private void loadTodayShifts() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);

        apiClient.getSchedule(year, month, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    JSONObject schedule = obj.getJSONObject("schedule");
                    String key = String.valueOf(day);

                    List<ScheduleFragment.ShiftData> officeShifts = new ArrayList<>();

                    if (schedule.has(key)) {
                        JSONArray dayArray = schedule.getJSONArray(key);
                        for (int i = 0; i < dayArray.length(); i++) {
                            JSONObject data = dayArray.getJSONObject(i);
                            String employeeName = data.getString("employee");
                            String shiftTime = data.getString("shift_time");

                            // Проверяем, принадлежит ли сотрудник этому офису
                            boolean isInOffice = false;
                            for (Employee emp : employees) {
                                if (emp.name.equals(employeeName)) {
                                    isInOffice = true;
                                    break;
                                }
                            }

                            // Только рабочие смены сотрудников этого офиса
                            if (isInOffice && !shiftTime.equals("Выходной") && !shiftTime.equals("Отпуск") && !shiftTime.equals("Больничный")) {
                                officeShifts.add(new ScheduleFragment.ShiftData(employeeName, shiftTime));
                            }
                        }
                    }

                    adapter = new TodayShiftsAdapter(officeShifts);
                    recyclerView.setAdapter(adapter);

                    if (officeShifts.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                    }

                } catch (Exception e) {
                    tvEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String error) {
                tvEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    static class Employee {
        int id;
        String name, role;
        Employee(int id, String name, String role) {
            this.id = id;
            this.name = name;
            this.role = role;
        }
    }
}