package com.example.salestracker.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.salestracker.ApiClient;
import com.example.salestracker.R;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

public class ScheduleFragment extends Fragment {

    private TableLayout tableSchedule;
    private TextView tvMonthYear, tvSelectedDate, tvUpdateTime;
    private LinearLayout llEmployeesList;
    private Button btnPrevMonth, btnNextMonth;
    private ImageView btnEditShift;
    private SwipeRefreshLayout swipeRefresh;

    private int currentYear, currentMonth;
    // КЛЮЧЕВОЕ ИЗМЕНЕНИЕ: List<ShiftData> для нескольких сотрудников в день
    private Map<String, List<ShiftData>> shifts = new HashMap<>();
    private List<Employee> employees = new ArrayList<>();
    private boolean isAdmin = false;
    private int selectedDay = 0;
    private ApiClient apiClient;
    private String currentEmployee;

    private String getDisplayText(String shiftTime) {
        if (shiftTime == null) return "?";
        switch (shiftTime) {
            case "09:00-18:00": return "09:00-18:00";
            case "10:00-19:00": return "10:00-19:00";
            case "12:00-21:00": return "12:00-21:00";
            case "Выходной": return "Выходной";
            case "Отпуск": return "Отпуск";
            case "Больничный": return "Больничный";
            case "Другой офис": return "Другой офис";
            default: return shiftTime;
        }
    }

    private String getShortStatus(String shiftTime) {
        if (shiftTime == null) return "?";
        switch (shiftTime) {
            case "09:00-18:00": return "9-18";
            case "10:00-19:00": return "10-19";
            case "12:00-21:00": return "12-21";
            case "Выходной": return "Вых";
            case "Отпуск": return "Отп";
            case "Больничный": return "Бол";
            case "Другой офис": return "Др";
            default: return "?";
        }
    }

    private int getStatusColor(String shiftTime) {
        if (shiftTime == null) return 0xFFFFFFFF;
        switch (shiftTime) {
            case "09:00-18:00": return 0xFFA5D6A7;
            case "10:00-19:00": return 0xFFA5D6A7;
            case "12:00-21:00": return 0xFFA5D6A7;
            case "Выходной": return 0xFF90CAF9;
            case "Отпуск": return 0xFFFFFFFF;
            case "Больничный": return 0xFFFFFFFF;
            case "Другой офис": return 0xFFFFF59D;
            default: return 0xFFFFFFFF;
        }
    }

    private int getShiftTextColor(String shiftTime) {
        if (shiftTime == null) return 0xFF333333;
        switch (shiftTime) {
            case "Выходной": return 0xFF1976D2;
            case "Отпуск": return 0xFFF57C00;
            case "Больничный": return 0xFF757575;
            case "Другой офис": return 0xFFF9A825;
            default: return 0xFF4CAF50;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_schedule, container, false);

        apiClient = new ApiClient();

        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        tableSchedule = view.findViewById(R.id.tableSchedule);
        tvMonthYear = view.findViewById(R.id.tvMonthYear);
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate);
        tvUpdateTime = view.findViewById(R.id.tvUpdateTime);
        llEmployeesList = view.findViewById(R.id.llEmployeesList);
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
            loadEmployees();
            loadFromServer();
            swipeRefresh.setRefreshing(false);
        });

        Calendar cal = Calendar.getInstance();
        currentYear = cal.get(Calendar.YEAR);
        currentMonth = cal.get(Calendar.MONTH);

        loadEmployees();
        loadFromServer();

        btnPrevMonth.setOnClickListener(v -> {
            currentMonth--;
            if (currentMonth < 0) {
                currentMonth = 11;
                currentYear--;
            }
            selectedDay = 0;
            loadFromServer();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentMonth++;
            if (currentMonth > 11) {
                currentMonth = 0;
                currentYear++;
            }
            selectedDay = 0;
            loadFromServer();
        });

        return view;
    }

    private void loadEmployees() {
        apiClient.getEmployees(new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    JSONArray arr = obj.getJSONArray("employees");
                    employees.clear();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject emp = arr.getJSONObject(i);
                        employees.add(new Employee(
                                emp.getInt("id"),
                                emp.getString("name"),
                                emp.getString("role")
                        ));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    setDefaultEmployees();
                }
            }

            @Override
            public void onError(String error) {
                setDefaultEmployees();
            }
        });
    }

    private void setDefaultEmployees() {
        employees.clear();
        employees.add(new Employee(1, "Владислав", "dm"));
        employees.add(new Employee(2, "Николай", "senior_seller"));
        employees.add(new Employee(3, "Алена", "seller"));
        employees.add(new Employee(4, "Диана", "seller"));
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

                    // КЛЮЧЕВОЕ ИЗМЕНЕНИЕ: парсим МАССИВ сотрудников для каждого дня
                    for (Iterator<String> it = schedule.keys(); it.hasNext(); ) {
                        String day = it.next();
                        JSONArray dayArray = schedule.getJSONArray(day);
                        List<ShiftData> dayShifts = new ArrayList<>();
                        for (int i = 0; i < dayArray.length(); i++) {
                            JSONObject data = dayArray.getJSONObject(i);
                            dayShifts.add(new ShiftData(
                                    data.getString("employee"),
                                    data.getString("shift_time")
                            ));
                        }
                        shifts.put(currentYear + "-" + (currentMonth + 1) + "-" + day, dayShifts);
                    }
                    updateCalendar();
                } catch (Exception e) {
                    e.printStackTrace();
                    updateCalendar();
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

        TableRow headerRow = new TableRow(getContext());
        String[] weekDays = {"ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС"};
        for (String dayName : weekDays) {
            TextView header = new TextView(getContext());
            header.setText(dayName);
            header.setPadding(12, 12, 12, 12);
            header.setGravity(Gravity.CENTER);
            header.setTypeface(Typeface.DEFAULT_BOLD);
            header.setBackgroundColor(0xFFE0E0E0);
            header.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));
            headerRow.addView(header);
        }
        tableSchedule.addView(headerRow);

        int day = 1;
        int rowCount = 0;

        while (day <= daysInMonth) {
            TableRow row = new TableRow(getContext());
            for (int col = 0; col < 7; col++) {
                TextView cell = new TextView(getContext());
                cell.setPadding(8, 12, 8, 12);
                cell.setGravity(Gravity.CENTER);
                cell.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));

                if (rowCount == 0 && col < firstDayOfWeek) {
                    cell.setText("");
                    cell.setBackgroundColor(0xFFEEEEEE);
                } else if (day <= daysInMonth) {
                    final int currentDay = day;

                    String key = currentYear + "-" + (currentMonth + 1) + "-" + day;
                    List<ShiftData> dayShifts = shifts.get(key);

                    if (dayShifts != null && !dayShifts.isEmpty()) {
                        String shortStatus = getShortStatus(dayShifts.get(0).shiftTime);
                        cell.setText(day + "\n" + shortStatus);
                        cell.setTextSize(10);
                        cell.setBackgroundColor(getStatusColor(dayShifts.get(0).shiftTime));
                    } else {
                        cell.setText(String.valueOf(day));
                        cell.setBackgroundColor(0xFFFFFFFF);
                    }

                    cell.setClickable(true);
                    cell.setOnClickListener(v -> {
                        selectedDay = currentDay;
                        showDayInfo(selectedDay);
                        updateCalendar();
                    });

                    if (selectedDay == day) {
                        cell.setBackgroundColor(0xFF2196F3);
                        cell.setTextColor(0xFFFFFFFF);
                    } else {
                        cell.setTextColor(0xFF000000);
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

        if (selectedDay == 0 && daysInMonth > 0) {
            selectedDay = 1;
        }
        if (selectedDay > 0) {
            showDayInfo(selectedDay);
        }
    }

    private void showDayInfo(int day) {
        if (getContext() == null) return;

        String[] weekDays = {"Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье"};
        Calendar cal = Calendar.getInstance();
        cal.set(currentYear, currentMonth, day);
        int weekday = cal.get(Calendar.DAY_OF_WEEK) - 2;
        if (weekday < 0) weekday = 6;

        String[] monthNames = {"Января", "Февраля", "Марта", "Апреля", "Мая", "Июня",
                "Июля", "Августа", "Сентября", "Октября", "Ноября", "Декабря"};
        tvSelectedDate.setText(weekDays[weekday] + " - " + day + " " + monthNames[currentMonth] + " " + currentYear);

        llEmployeesList.removeAllViews();

        String key = currentYear + "-" + (currentMonth + 1) + "-" + day;
        List<ShiftData> dayShifts = shifts.get(key);

        if (dayShifts != null && !dayShifts.isEmpty()) {
            // КЛЮЧЕВОЕ ИЗМЕНЕНИЕ: показываем ВСЕХ сотрудников
            for (ShiftData data : dayShifts) {
                View employeeCard = createEmployeeCard(data.employee, data.shiftTime, data.employee.equals(currentEmployee));
                llEmployeesList.addView(employeeCard);
            }
        } else {
            TextView tvEmpty = new TextView(getContext());
            tvEmpty.setText("На этот день никто не назначен");
            tvEmpty.setPadding(16, 24, 16, 24);
            tvEmpty.setTextSize(14);
            tvEmpty.setTextColor(0xFF999999);
            tvEmpty.setGravity(Gravity.CENTER);
            llEmployeesList.addView(tvEmpty);
        }

        tvUpdateTime.setText("Последнее обновление: " + new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new java.util.Date()));
    }

    private View createEmployeeCard(String name, String shiftTime, boolean isCurrentUser) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setPadding(16, 14, 16, 14);
        card.setBackgroundColor(Color.WHITE);
        card.setElevation(2f);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 8);
        card.setLayoutParams(params);
        card.setWeightSum(2);

        TextView tvName = new TextView(getContext());
        tvName.setText(name + (isCurrentUser ? " (Я)" : ""));
        tvName.setTextSize(14);
        tvName.setTypeface(Typeface.DEFAULT_BOLD);
        tvName.setTextColor(0xFF333333);
        tvName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvShift = new TextView(getContext());
        tvShift.setText(getDisplayText(shiftTime));
        tvShift.setTextSize(12);
        tvShift.setTextColor(getShiftTextColor(shiftTime));
        tvShift.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tvShift.setGravity(Gravity.END);

        card.addView(tvName);
        card.addView(tvShift);

        return card;
    }

    private void showEditDialog() {
        if (getContext() == null || employees.isEmpty()) return;

        String[] employeeNames = new String[employees.size()];
        for (int i = 0; i < employees.size(); i++) {
            employeeNames[i] = employees.get(i).name;
        }

        String[] times = {"09:00-18:00", "10:00-19:00", "12:00-21:00", "Выходной", "Отпуск", "Больничный", "Другой офис"};

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_shift, null);
        Spinner spinnerEmployee = dialogView.findViewById(R.id.spinnerEmployee);
        Spinner spinnerTime = dialogView.findViewById(R.id.spinnerTime);

        ArrayAdapter<String> employeeAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, employeeNames);
        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, times);
        spinnerEmployee.setAdapter(employeeAdapter);
        spinnerTime.setAdapter(timeAdapter);

        String key = currentYear + "-" + (currentMonth + 1) + "-" + selectedDay;
        List<ShiftData> currentShifts = shifts.get(key);

        if (currentShifts != null && !currentShifts.isEmpty()) {
            ShiftData currentData = currentShifts.get(0);
            for (int i = 0; i < employeeNames.length; i++) {
                if (employeeNames[i].equals(currentData.employee)) {
                    spinnerEmployee.setSelection(i);
                    break;
                }
            }
            for (int i = 0; i < times.length; i++) {
                if (times[i].equals(currentData.shiftTime)) {
                    spinnerTime.setSelection(i);
                    break;
                }
            }
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Смена на " + selectedDay + " " + tvMonthYear.getText())
                .setView(dialogView)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String employee = spinnerEmployee.getSelectedItem().toString();
                    String time = spinnerTime.getSelectedItem().toString();

                    List<ShiftData> newShifts = new ArrayList<>();
                    newShifts.add(new ShiftData(employee, time));
                    shifts.put(key, newShifts);

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