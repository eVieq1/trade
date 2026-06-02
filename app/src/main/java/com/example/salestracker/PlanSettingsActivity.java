package com.example.salestracker;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import java.util.Calendar;

public class PlanSettingsActivity extends AppCompatActivity {

    private GridView gridDays;
    private TextView tvMonthYear;
    private int currentYear, currentMonth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.plan_settings_activity); // ИСПРАВЛЕНО

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Изменить планы");

        tvMonthYear = findViewById(R.id.tvMonthYear);
        gridDays = findViewById(R.id.gridDays);

        Calendar cal = Calendar.getInstance();
        currentYear = cal.get(Calendar.YEAR);
        currentMonth = cal.get(Calendar.MONTH);

        tvMonthYear.setText(getMonthName(currentMonth) + " " + currentYear);
        setupCalendar();
    }

    private void setupCalendar() {
        Calendar cal = Calendar.getInstance();
        cal.set(currentYear, currentMonth, 1);
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 2;
        if (firstDayOfWeek < 0) firstDayOfWeek = 6;
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        int totalCells = 42;
        Integer[] days = new Integer[totalCells];
        for (int i = 0; i < totalCells; i++) days[i] = 0;

        for (int i = 0; i < daysInMonth; i++) {
            days[firstDayOfWeek + i] = i + 1;
        }

        ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(this, R.layout.grid_item_day, days) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                if (view == null) {
                    view = new TextView(PlanSettingsActivity.this);
                }
                int day = getItem(position);
                view.setText(day == 0 ? "" : String.valueOf(day));
                view.setGravity(Gravity.CENTER);
                view.setPadding(8, 12, 8, 12);
                view.setTextSize(14);
                view.setBackgroundColor(day == 0 ? 0xFFEEEEEE : 0xFFFFFFFF);

                if (day > 0) {
                    final int selectedDay = day;
                    view.setOnClickListener(v -> openDayEdit(selectedDay));
                }
                return view;
            }
        };

        gridDays.setAdapter(adapter);
        gridDays.setNumColumns(7);
    }

    private void openDayEdit(int day) {
        EditDayScheduleDialog dialog = new EditDayScheduleDialog(this, currentYear, currentMonth + 1, day);
        dialog.show();
    }

    private String getMonthName(int month) {
        String[] months = {"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
                "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};
        return months[month];
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}