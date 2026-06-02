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

    private GridView gridSchedule;
    private TextView tvMonthYear, tvSelectedDate, tvDirector, tvSpecialist, tvShiftTime, tvUpdateTime, tvPostalCode;
    private Button btnPrevMonth, btnNextMonth, btnEditShift;
    private SwipeRefreshLayout swipeRefresh;

    private int currentYear, currentMonth;
    private Map<String, List<ShiftData>> shifts = new HashMap<>();
    private List<Employee> employees = new ArrayList<>();
    private boolean isAdmin = false;
    private int selectedDay = 0;
    private ApiClient apiClient;
    private String currentEmployee;

    private String[] monthNamesNominative = {"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};

    private String[] monthNamesGenitive = {"января", "февраля", "марта", "апреля", "мая", "июня",
            "июля", "августа", "сентября", "октября", "ноября", "декабря"};

    private class CalendarAdapter extends BaseAdapter {
        private List<Integer> days = new ArrayList<>();

        public void setDays(List<Integer> days) {
            this.days = days;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() { return days.size(); }

        @Override
        public Object getItem(int position) { return days.get(position); }

        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            int cellSizePx = (int) (40 * getResources().getDisplayMetrics().density);

            if (convertView == null) {
                convertView = new TextView(getContext());
                convertView.setLayoutParams(new GridView.LayoutParams(
                        GridView.LayoutParams.MATCH_PARENT,
                        cellSizePx));
                ((TextView) convertView).setGravity(Gravity.CENTER);
                ((TextView) convertView).setTextSize(12);
                ((TextView) convertView).setTypeface(Typeface.DEFAULT_BOLD);
            }

            TextView cell = (TextView) convertView;
            int dayNumber = days.get(position);

            if (dayNumber == 0) {
                cell.setText("");
                cell.setBackgroundColor(0xFFF5F5F5);
            } else {
                cell.setText(String.valueOf(dayNumber));
                cell.setBackgroundColor(0xFFFFFFFF);

                if (selectedDay == dayNumber) {
                    cell.setBackgroundColor(0xFF2196F3);
                    cell.setTextColor(0xFFFFFFFF);
                } else {
                    cell.setTextColor(0xFF000000);
                }

                cell.setOnClickListener(v -> {
                    selectedDay = dayNumber;
                    showDayInfo(selectedDay);
                    notifyDataSetChanged();
                });
            }

            return convertView;
        }
    }

    private CalendarAdapter calendarAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_schedule, container, false);

        apiClient = new ApiClient();

        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        gridSchedule = view.findViewById(R.id.gridSchedule);
        tvMonthYear = view.findViewById(R.id.tvMonthYear);
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate);
        tvDirector = view.findViewById(R.id.tvDirector);
        tvSpecialist = view.findViewById(R.id.tvSpecialist);
        tvShiftTime = view.findViewById(R.id.tvShiftTime);
        tvUpdateTime = view.findViewById(R.id.tvUpdateTime);
        tvPostalCode = view.findViewById(R.id.tvPostalCode);
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        btnNextMonth = view.findViewById(R.id.btnNextMonth);
        btnEditShift = view.findViewById(R.id.btnEditShift);

        calendarAdapter = new CalendarAdapter();
        gridSchedule.setAdapter(calendarAdapter);
        gridSchedule.setNumColumns(7);
        gridSchedule.setVerticalSpacing(1);
        gridSchedule.setHorizontalSpacing(1);

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
                    Toast.makeText(getContext(), "Ошибка: " + error, Toast.LENGTH_SHORT).show();
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
        if (getContext() == null || gridSchedule == null) return;

        tvMonthYear.setText(monthNamesNominative[currentMonth] + " " + currentYear);

        Calendar cal = Calendar.getInstance();
        cal.set(currentYear, currentMonth, 1);
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 2;
        if (firstDayOfWeek < 0) firstDayOfWeek = 6;
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        List<Integer> daysList = new ArrayList<>();

        for (int i = 0; i < firstDayOfWeek; i++) {
            daysList.add(0);
        }

        for (int day = 1; day <= daysInMonth; day++) {
            daysList.add(day);
        }

        int remaining = 42 - daysList.size();
        for (int i = 0; i < remaining; i++) {
            daysList.add(0);
        }

        calendarAdapter.setDays(daysList);

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

        tvSelectedDate.setText(weekDays[weekday] + " - " + day + " " + monthNamesGenitive[currentMonth]);

        String key = currentYear + "-" + (currentMonth + 1) + "-" + day;
        List<ShiftData> dayShifts = shifts.get(key);

        tvDirector.setText(currentEmployee);

        String specialist = "";
        String shiftTime = "";
        if (dayShifts != null && !dayShifts.isEmpty()) {
            for (ShiftData data : dayShifts) {
                if (!data.employee.equals(currentEmployee)) {
                    specialist = data.employee;
                    shiftTime = data.shiftTime;
                    break;
                }
            }
        }

        if (!specialist.isEmpty()) {
            tvSpecialist.setText(specialist);
            tvShiftTime.setText(getDisplayText(shiftTime));
            tvShiftTime.setVisibility(View.VISIBLE);
        } else {
            tvSpecialist.setText("Не назначен");
            tvShiftTime.setVisibility(View.GONE);
        }

        tvUpdateTime.setText("Последние обновления работы: " + new java.text.SimpleDateFormat("dd.MM.yyyy 'в' HH:mm", Locale.getDefault()).format(new java.util.Date()));
    }

    private String getDisplayText(String shiftTime) {
        if (shiftTime == null) return "?";
        switch (shiftTime) {
            case "09:00-18:00": return "Часы работы дневные";
            case "10:00-19:00": return "Часы работы вечерние";
            case "12:00-21:00": return "Часы работы ночные";
            case "Выходной": return "Выходной";
            case "Отпуск": return "Отпуск";
            case "Больничный": return "Больничный";
            case "Другой офис": return "Работа в другом офисе";
            default: return shiftTime;
        }
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