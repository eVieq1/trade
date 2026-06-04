package com.example.salestracker;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class PlanSettingsActivity extends AppCompatActivity {

    private GridView gridDays;
    private TextView tvMonthYear;
    private RecyclerView monthRecyclerView;
    private MonthAdapter monthAdapter;
    private int currentYear, currentMonth;
    private int currentPosition = 120;
    private List<MonthData> monthList = new ArrayList<>();

    private final String[] monthNames = {"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Редактировать график");

        tvMonthYear = findViewById(R.id.tvMonthYear);
        gridDays = findViewById(R.id.gridDays);
        monthRecyclerView = findViewById(R.id.monthRecyclerView);

        Calendar cal = Calendar.getInstance();
        currentYear = cal.get(Calendar.YEAR);
        currentMonth = cal.get(Calendar.MONTH);

        // Создаём список месяцев (от -120 до +120)
        for (int i = -120; i <= 120; i++) {
            int y = currentYear + (i / 12);
            int m = currentMonth + i;
            while (m < 0) {
                m += 12;
                y--;
            }
            while (m > 11) {
                m -= 12;
                y++;
            }
            monthList.add(new MonthData(y, m));
        }
        currentPosition = 120;

        // Настройка горизонтального RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        monthRecyclerView.setLayoutManager(layoutManager);
        monthAdapter = new MonthAdapter();
        monthRecyclerView.setAdapter(monthAdapter);

        monthRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                    int centerPosition = lm.findFirstCompletelyVisibleItemPosition();
                    if (centerPosition == -1) {
                        centerPosition = lm.findFirstVisibleItemPosition();
                    }
                    if (centerPosition != -1 && centerPosition != currentPosition) {
                        currentPosition = centerPosition;
                        MonthData data = monthList.get(currentPosition);
                        currentYear = data.year;
                        currentMonth = data.month;
                        updateMonthDisplay();
                        setupCalendar();
                        monthAdapter.notifyDataSetChanged();
                    }
                }
            }
        });

        monthRecyclerView.scrollToPosition(currentPosition);
        updateMonthDisplay();
        setupCalendar();
    }

    private void updateMonthDisplay() {
        tvMonthYear.setText(monthNames[currentMonth] + " " + currentYear);
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

    // ==================== АДАПТЕР ДЛЯ МЕСЯЦЕВ ====================

    private class MonthAdapter extends RecyclerView.Adapter<MonthAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(16);
            tv.setPadding(48, 16, 48, 16);
            return new ViewHolder(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MonthData data = monthList.get(position);
            String text = monthNames[data.month] + " " + data.year;
            holder.textView.setText(text);
            if (position == currentPosition) {
                holder.textView.setTextColor(0xFF2196F3);
                holder.textView.setTextSize(20);
            } else {
                holder.textView.setTextColor(0xFF999999);
                holder.textView.setTextSize(16);
            }
        }

        @Override
        public int getItemCount() {
            return monthList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            ViewHolder(View itemView) {
                super(itemView);
                textView = (TextView) itemView;
                itemView.setOnClickListener(v -> {
                    int pos = getAdapterPosition();
                    if (pos != -1 && pos != currentPosition) {
                        currentPosition = pos;
                        MonthData data = monthList.get(pos);
                        currentYear = data.year;
                        currentMonth = data.month;
                        updateMonthDisplay();
                        setupCalendar();
                        notifyDataSetChanged();
                        monthRecyclerView.smoothScrollToPosition(currentPosition);
                    }
                });
            }
        }
    }

    // ==================== DATA CLASS ====================

    private static class MonthData {
        int year, month;
        MonthData(int year, int month) {
            this.year = year;
            this.month = month;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}