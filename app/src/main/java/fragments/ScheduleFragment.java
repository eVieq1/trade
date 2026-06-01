package com.example.salestracker.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.salestracker.ApiClient;
import com.example.salestracker.R;
import org.json.JSONObject;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ScheduleFragment extends Fragment {

    private TableLayout tableSchedule;
    private TextView tvMonthYear, tvSelectedDate, tvEmployeeInfo, tvShiftTime, tvUpdateTime;
    private Button btnPrevMonth, btnNextMonth;
    private ImageView btnEditShift;
    private SwipeRefreshLayout swipeRefresh;

    private int currentYear, currentMonth;
    private Map<String, ShiftData> shifts = new HashMap<>();
    private boolean isAdmin = false;
    private int selectedDay = 0;
    private ApiClient apiClient;
    private String currentEmployee;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_schedule, container, false);

        apiClient = new ApiClient();

        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        tableSchedule = view.findViewById(R.id.tableSchedule);
        tvMonthYear = view.findViewById(R.id.tvMonthYear);
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate);
        tvEmployeeInfo = view.findViewById(R.id.tvEmployeeInfo);
        tvShiftTime = view.findViewById(R.id.tvShiftTime);
        tvUpdateTime = view.findViewById(R.id.tvUpdateTime);
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        btnNextMonth = view.findViewById(R.id.btnNextMonth);
        btnEditShift = view.findViewById(R.id.btnEditShift);

        SharedPreferences prefs = requireActivity().getSharedPreferences("app", Context.MODE_PRIVATE);
        currentEmployee = prefs.getString("employee_name", "");
        String userRole = prefs.getString("user_role", "seller");
        isAdmin = userRole.equals("dm");

        if (isAdmin) {
            btnEditShift.setVisibility(View.VISIBLE);
            btnEditShift.setOnClickListener(v -> showEditDialog());
        } else {
            btnEditShift.setVisibility(View.GONE);
        }

        swipeRefresh.setOnRefreshListener(() -> {
            loadFromServer();
            swipeRefresh.setRefreshing(false);
        });

        Calendar cal = Calendar.getInstance();
        currentYear = cal.get(Calendar.YEAR);
        currentMonth = cal.get(Calendar.MONTH);

        loadFromServer();

        btnPrevMonth.setOnClickListener(v -> {
            currentMonth--;
            if (currentMonth < 0) {
                currentMonth = 11;
                currentYear--;
            }
            loadFromServer();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentMonth++;
            if (currentMonth > 11) {
                currentMonth = 0;
                currentYear++;
            }
            loadFromServer();
        });

        return view;
    }

    private void loadFromServer() {
        if (apiClient == null) return;
        apiClient.getSchedule(currentYear, currentMonth + 1, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    JSONObject schedule = obj.getJSONObject("schedule");
                    shifts.clear();
                    for (Iterator<String> it = schedule.keys(); it.hasNext(); ) {
                        String day = it.next();
                        JSONObject data = schedule.getJSONObject(day);
                        shifts.put(currentYear + "-" + (currentMonth + 1) + "-" + day,
                                new ShiftData(data.getString("employee"), data.getString("shift_time")));
                    }
                    updateCalendar();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(String error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Ошибка загрузки: " + error, Toast.LENGTH_SHORT).show();
                }
                updateCalendar();
            }
        });
    }

    private void saveToServer(int day, String employee, String shiftTime) {
        if (apiClient == null) return;
        apiClient.saveSchedule(currentYear, currentMonth + 1, day, employee, shiftTime, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Сохранено", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Ошибка: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateCalendar() {
        if (getContext() == null || tableSchedule == null) return;
        String[] monthNames = {"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
                "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};
        tvMonthYear.setText(monthNames[currentMonth] + " " + currentYear);

        Calendar cal = Calendar.getInstance();
        cal.set(currentYear, currentMonth, 1);
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 2;
        if (firstDayOfWeek < 0) firstDayOfWeek = 6;
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        tableSchedule.removeAllViews();
        int day = 1, rowCount = 0;

        while (day <= daysInMonth) {
            TableRow row = new TableRow(getContext());
            for (int col = 0; col < 7; col++) {
                TextView cell = new TextView(getContext());
                cell.setPadding(8, 12, 8, 12);
                cell.setGravity(android.view.Gravity.CENTER);
                cell.setBackgroundColor(0xFFFFFFFF);

                if (rowCount == 0 && col < firstDayOfWeek) {
                    cell.setText("");
                    cell.setBackgroundColor(0xFFEEEEEE);
                } else if (day <= daysInMonth) {
                    final int currentDay = day;
                    cell.setText(String.valueOf(day));
                    cell.setClickable(true);
                    cell.setOnClickListener(v -> {
                        selectedDay = currentDay;
                        showDayInfo(selectedDay);
                        updateCalendar();
                    });

                    String key = currentYear + "-" + (currentMonth + 1) + "-" + day;
                    ShiftData data = shifts.get(key);
                    if (data != null) {
                        String status = getStatusChar(data.shiftTime);
                        cell.setText(day + "\n" + status);
                        cell.setTextSize(10);
                        cell.setBackgroundColor(getStatusColor(status));
                    }
                    if (selectedDay == day) {
                        cell.setBackgroundColor(0xFF2196F3);
                        cell.setTextColor(0xFFFFFFFF);
                    }
                    day++;
                } else {
                    cell.setText("");
                    cell.setBackgroundColor(0xFFEEEEEE);
                }
                row.addView(cell);
            }
            tableSchedule.addView(row);
            rowCount++;
        }
        if (selectedDay == 0) selectedDay = 1;
        showDayInfo(selectedDay);
    }

    private String getStatusChar(String shiftTime) {
        if (shiftTime == null) return "?";
        if (shiftTime.equals("Выходной")) return "О";
        if (shiftTime.equals("Больничный")) return "Б";
        return "Р";
    }

    private int getStatusColor(String status) {
        switch (status) {
            case "Р": return 0xFF4CAF50;
            case "О": return 0xFFF44336;
            case "Б": return 0xFFFF9800;
            default: return 0xFFFFFFFF;
        }
    }

    private void showDayInfo(int day) {
        if (getContext() == null) return;
        String[] weekDays = {"Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье"};
        Calendar cal = Calendar.getInstance();
        cal.set(currentYear, currentMonth, day);
        int weekday = cal.get(Calendar.DAY_OF_WEEK) - 2;
        if (weekday < 0) weekday = 6;
        tvSelectedDate.setText(weekDays[weekday] + " - " + day + " " + tvMonthYear.getText());

        String key = currentYear + "-" + (currentMonth + 1) + "-" + day;
        ShiftData data = shifts.get(key);
        if (data != null) {
            tvEmployeeInfo.setText(data.employee.equals(currentEmployee) ? data.employee + " (Я)" : data.employee);
            tvShiftTime.setText(data.shiftTime);
        } else {
            tvEmployeeInfo.setText("Не назначен");
            tvShiftTime.setText("—");
        }
        tvUpdateTime.setText("Последнее обновление: " + new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(new java.util.Date()));
    }

    private void showEditDialog() {
        if (getContext() == null) return;
        String[] employees = {"Анна", "Сергей", "Мария", "Дмитрий"};
        String[] times = {"09:00-18:00", "10:00-19:00", "12:00-21:00", "Выходной", "Больничный"};

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_shift, null);
        Spinner spinnerEmployee = dialogView.findViewById(R.id.spinnerEmployee);
        Spinner spinnerTime = dialogView.findViewById(R.id.spinnerTime);
        spinnerEmployee.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, employees));
        spinnerTime.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, times));

        new AlertDialog.Builder(getContext())
                .setTitle("Смена на " + selectedDay + " " + tvMonthYear.getText())
                .setView(dialogView)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String employee = spinnerEmployee.getSelectedItem().toString();
                    String time = spinnerTime.getSelectedItem().toString();
                    shifts.put(currentYear + "-" + (currentMonth + 1) + "-" + selectedDay, new ShiftData(employee, time));
                    saveToServer(selectedDay, employee, time);
                    updateCalendar();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    static class ShiftData {
        String employee, shiftTime;
        ShiftData(String employee, String shiftTime) {
            this.employee = employee;
            this.shiftTime = shiftTime;
        }
    }
}