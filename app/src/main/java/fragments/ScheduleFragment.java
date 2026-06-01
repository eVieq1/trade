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

    // ========== ОБЪЯВЛЕНИЕ ВСЕХ ПЕРЕМЕННЫХ ==========
    private TableLayout tableSchedule;
    private TextView tvMonthYear;
    private TextView tvSelectedDate;
    private TextView tvEmployeeInfo;
    private TextView tvShiftTime;
    private TextView tvUpdateTime;
    // tvDirectorLabel УДАЛЕН
    private Button btnPrevMonth;
    private Button btnNextMonth;
    private ImageView btnEditShift;
    private SwipeRefreshLayout swipeRefresh;

    private int currentYear;
    private int currentMonth;
    private Map<String, ShiftData> shifts = new HashMap<>();
    private boolean isAdmin = false;
    private int selectedDay = 0;
    private ApiClient apiClient;
    private String currentEmployee;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_schedule, container, false);

        apiClient = new ApiClient();

        // ========== ИНИЦИАЛИЗАЦИЯ View ==========
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        tableSchedule = view.findViewById(R.id.tableSchedule);
        tvMonthYear = view.findViewById(R.id.tvMonthYear);
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate);
        tvEmployeeInfo = view.findViewById(R.id.tvEmployeeInfo);
        tvShiftTime = view.findViewById(R.id.tvShiftTime);
        tvUpdateTime = view.findViewById(R.id.tvUpdateTime);
        // tvDirectorLabel = view.findViewById(R.id.tvDirectorLabel); УДАЛЕНО
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        btnNextMonth = view.findViewById(R.id.btnNextMonth);
        btnEditShift = view.findViewById(R.id.btnEditShift);

        // ========== ПОЛУЧАЕМ ДАННЫЕ О ПОЛЬЗОВАТЕЛЕ ==========
        SharedPreferences prefs = requireActivity().getSharedPreferences("app", Context.MODE_PRIVATE);
        currentEmployee = prefs.getString("employee_name", "");
        String userRole = prefs.getString("user_role", "seller");
        isAdmin = userRole.equals("dm");

        // ========== НАСТРОЙКА ДЛЯ АДМИНИСТРАТОРА ==========
        if (isAdmin) {
            if (btnEditShift != null) {
                btnEditShift.setVisibility(View.VISIBLE);
                btnEditShift.setOnClickListener(v -> showEditDialog());
            }
        } else {
            if (btnEditShift != null) {
                btnEditShift.setVisibility(View.GONE);
            }
        }

        // ========== ОБНОВЛЕНИЕ ПРИ СВАЙПЕ ==========
        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(() -> {
                loadFromServer();
                swipeRefresh.setRefreshing(false);
            });
        }

        // ========== УСТАНАВЛИВАЕМ ТЕКУЩУЮ ДАТУ ==========
        Calendar cal = Calendar.getInstance();
        currentYear = cal.get(Calendar.YEAR);
        currentMonth = cal.get(Calendar.MONTH);

        // ========== ЗАГРУЖАЕМ ДАННЫЕ ==========
        loadFromServer();

        // ========== КНОПКИ ПЕРЕКЛЮЧЕНИЯ МЕСЯЦЕВ ==========
        if (btnPrevMonth != null) {
            btnPrevMonth.setOnClickListener(v -> {
                currentMonth--;
                if (currentMonth < 0) {
                    currentMonth = 11;
                    currentYear--;
                }
                loadFromServer();
            });
        }

        if (btnNextMonth != null) {
            btnNextMonth.setOnClickListener(v -> {
                currentMonth++;
                if (currentMonth > 11) {
                    currentMonth = 0;
                    currentYear++;
                }
                loadFromServer();
            });
        }

        return view;
    }

    // ========== ЗАГРУЗКА С СЕРВЕРА ==========
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

    // ========== СОХРАНЕНИЕ НА СЕРВЕР ==========
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

    // ========== ОБНОВЛЕНИЕ КАЛЕНДАРЯ ==========
    private void updateCalendar() {
        if (getContext() == null || tableSchedule == null) return;

        String[] monthNames = {"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
                "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};

        if (tvMonthYear != null) {
            tvMonthYear.setText(monthNames[currentMonth] + " " + currentYear);
        }

        Calendar cal = Calendar.getInstance();
        cal.set(currentYear, currentMonth, 1);
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 2;
        if (firstDayOfWeek < 0) firstDayOfWeek = 6;

        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        tableSchedule.removeAllViews();

        int day = 1;
        int rowCount = 0;

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

        if (selectedDay == 0) {
            selectedDay = 1;
        }
        showDayInfo(selectedDay);
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========
    private String getStatusChar(String shiftTime) {
        if (shiftTime == null) return "?";
        if (shiftTime.equals("Выходной")) return "О";
        if (shiftTime.equals("Больничный")) return "Б";
        return "Р";
    }

    private int getStatusColor(String status) {
        if (status.equals("Р")) return 0xFF4CAF50;
        if (status.equals("О")) return 0xFFF44336;
        if (status.equals("Б")) return 0xFFFF9800;
        return 0xFFFFFFFF;
    }

    // ========== ПОКАЗ ИНФОРМАЦИИ О ДНЕ ==========
    private void showDayInfo(int day) {
        if (getContext() == null) return;

        String[] weekDays = {"Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье"};
        Calendar cal = Calendar.getInstance();
        cal.set(currentYear, currentMonth, day);
        int weekday = cal.get(Calendar.DAY_OF_WEEK) - 2;
        if (weekday < 0) weekday = 6;
        String weekDayName = weekDays[weekday];

        if (tvSelectedDate != null) {
            String monthText = tvMonthYear != null ? tvMonthYear.getText().toString() : "";
            tvSelectedDate.setText(weekDayName + " - " + day + " " + monthText);
        }

        String key = currentYear + "-" + (currentMonth + 1) + "-" + day;
        ShiftData data = shifts.get(key);

        if (tvEmployeeInfo != null) {
            if (data != null) {
                if (data.employee.equals(currentEmployee)) {
                    tvEmployeeInfo.setText(data.employee + " (Я)");
                } else {
                    tvEmployeeInfo.setText(data.employee);
                }
            } else {
                tvEmployeeInfo.setText("Не назначен");
            }
        }

        if (tvShiftTime != null) {
            if (data != null) {
                tvShiftTime.setText(data.shiftTime);
            } else {
                tvShiftTime.setText("—");
            }
        }

        if (tvUpdateTime != null) {
            tvUpdateTime.setText("Последнее обновление: " + new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(new java.util.Date()));
        }
    }

    // ========== ДИАЛОГ РЕДАКТИРОВАНИЯ ==========
    private void showEditDialog() {
        if (getContext() == null) return;

        String[] employees = {"Анна", "Сергей", "Мария", "Дмитрий"};
        String[] times = {"09:00-18:00", "10:00-19:00", "12:00-21:00", "Выходной", "Больничный"};

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_shift, null);
        Spinner spinnerEmployee = dialogView.findViewById(R.id.spinnerEmployee);
        Spinner spinnerTime = dialogView.findViewById(R.id.spinnerTime);

        ArrayAdapter<String> empAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, employees);
        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, times);

        if (spinnerEmployee != null) spinnerEmployee.setAdapter(empAdapter);
        if (spinnerTime != null) spinnerTime.setAdapter(timeAdapter);

        String title = "Смена на " + selectedDay + " " + (tvMonthYear != null ? tvMonthYear.getText().toString() : "");

        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String employee = spinnerEmployee != null ? spinnerEmployee.getSelectedItem().toString() : employees[0];
                    String time = spinnerTime != null ? spinnerTime.getSelectedItem().toString() : times[0];
                    String key = currentYear + "-" + (currentMonth + 1) + "-" + selectedDay;
                    shifts.put(key, new ShiftData(employee, time));
                    saveToServer(selectedDay, employee, time);
                    updateCalendar();
                    showDayInfo(selectedDay);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    // ========== ВНУТРЕННИЙ КЛАСС ДЛЯ ДАННЫХ СМЕНЫ ==========
    static class ShiftData {
        String employee, shiftTime;
        ShiftData(String employee, String shiftTime) {
            this.employee = employee;
            this.shiftTime = shiftTime;
        }
    }
}