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
    private Button btnPrevMonth, btnNextMonth;
    private ImageView btnEditShift;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout llEmployeesList;

    private int currentYear, currentMonth;
    private Map<String, List<ShiftData>> shifts = new HashMap<>();
    private List<Employee> employees = new ArrayList<>();
    private boolean isAdmin = false;
    private int selectedDay = 0;
    private ApiClient apiClient;
    private String currentEmployee;

    private final String[] monthNamesNominative = {"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};

    private final String[] monthNamesGenitive = {"января", "февраля", "марта", "апреля", "мая", "июня",
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
            TextView cell;
            if (convertView == null) {
                cell = new TextView(getContext());
                cell.setGravity(Gravity.CENTER);
                cell.setTextSize(12);
                cell.setTypeface(Typeface.DEFAULT_BOLD);
                int cellSizePx = (int) (38 * getResources().getDisplayMetrics().density);
                cell.setLayoutParams(new GridView.LayoutParams(GridView.LayoutParams.MATCH_PARENT, cellSizePx));
            } else {
                cell = (TextView) convertView;
            }

            int dayNumber = days.get(position);
            if (dayNumber == 0) {
                cell.setText("");
                cell.setBackgroundColor(0xFFF5F5F5);
            } else {
                cell.setText(String.valueOf(dayNumber));
                cell.setBackgroundColor(0xFFFFFFFF);
                cell.setTextColor(selectedDay == dayNumber ? 0xFFFFFFFF : 0xFF000000);
                cell.setBackgroundColor(selectedDay == dayNumber ? 0xFF2196F3 : 0xFFFFFFFF);
                cell.setOnClickListener(v -> {
                    selectedDay = dayNumber;
                    showDayInfo(selectedDay);
                    notifyDataSetChanged();
                });
            }
            return cell;
        }
    }

    private CalendarAdapter calendarAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_schedule, container, false);

        apiClient = new ApiClient();
        calendarAdapter = new CalendarAdapter();

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
        llEmployeesList = view.findViewById(R.id.llEmployeesList);

        gridSchedule.setAdapter(calendarAdapter);
        gridSchedule.setNumColumns(7);
        gridSchedule.setVerticalSpacing(1);
        gridSchedule.setHorizontalSpacing(1);

        SharedPreferences prefs = requireActivity().getSharedPreferences("app", Context.MODE_PRIVATE);
        currentEmployee = prefs.getString("employee_name", "");
        isAdmin = prefs.getString("user_role", "seller").equals("dm");

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
            if (currentMonth < 0) { currentMonth = 11; currentYear--; }
            selectedDay = 0;
            loadFromServer();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentMonth++;
            if (currentMonth > 11) { currentMonth = 0; currentYear++; }
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
                        employees.add(new Employee(emp.getInt("id"), emp.getString("name"), emp.getString("role")));
                    }
                } catch (Exception e) { setDefaultEmployees(); }
            }
            @Override
            public void onError(String error) { setDefaultEmployees(); }
        });
    }

    private void setDefaultEmployees() {
        employees.clear();
        employees.add(new Employee(1, "Владислав", "dm"));
        employees.add(new Employee(2, "Николай", "seller"));
        employees.add(new Employee(3, "Алена", "seller"));
        employees.add(new Employee(4, "Диана", "seller"));
    }

    private void loadFromServer() {
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
                            dayShifts.add(new ShiftData(data.getString("employee"), data.getString("shift_time")));
                        }
                        shifts.put(currentYear + "-" + (currentMonth + 1) + "-" + day, dayShifts);
                    }
                    updateCalendar();
                } catch (Exception e) { updateCalendar(); }
            }
            @Override
            public void onError(String error) { updateCalendar(); }
        });
    }

    private void saveToServer(int day, String employee, String shiftTime) {
        apiClient.saveSchedule(currentYear, currentMonth + 1, day, employee, shiftTime, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) { if (getContext() != null) Toast.makeText(getContext(), "Сохранено", Toast.LENGTH_SHORT).show(); }
            @Override
            public void onError(String error) { if (getContext() != null) Toast.makeText(getContext(), "Ошибка: " + error, Toast.LENGTH_SHORT).show(); }
        });
    }

    private void updateCalendar() {
        if (getContext() == null) return;
        tvMonthYear.setText(monthNamesNominative[currentMonth] + " " + currentYear);

        Calendar cal = Calendar.getInstance();
        cal.set(currentYear, currentMonth, 1);
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 2;
        if (firstDayOfWeek < 0) firstDayOfWeek = 6;
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        List<Integer> daysList = new ArrayList<>();
        for (int i = 0; i < firstDayOfWeek; i++) daysList.add(0);
        for (int day = 1; day <= daysInMonth; day++) daysList.add(day);
        while (daysList.size() < 42) daysList.add(0);

        calendarAdapter.setDays(daysList);
        if (selectedDay == 0 && daysInMonth > 0) selectedDay = 1;
        showDayInfo(selectedDay);
    }

    private void showDayInfo(int day) {
        if (getContext() == null) return;

        Calendar cal = Calendar.getInstance();
        cal.set(currentYear, currentMonth, day);
        int weekday = cal.get(Calendar.DAY_OF_WEEK) - 2;
        if (weekday < 0) weekday = 6;
        String[] weekDays = {"Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье"};
        tvSelectedDate.setText(weekDays[weekday] + " - " + day + " " + monthNamesGenitive[currentMonth]);

        String key = currentYear + "-" + (currentMonth + 1) + "-" + day;
        List<ShiftData> dayShifts = shifts.get(key);

        tvDirector.setText(currentEmployee);

        // Очищаем список сотрудников
        llEmployeesList.removeAllViews();

        String specialist = "";
        String shiftTime = "";

        if (dayShifts != null && !dayShifts.isEmpty()) {
            for (ShiftData data : dayShifts) {
                // Создаем карточку для каждого сотрудника
                TextView employeeCard = new TextView(getContext());
                String displayText = data.employee + " — " + getDisplayText(data.shiftTime);
                employeeCard.setText(displayText);
                employeeCard.setPadding(16, 10, 16, 10);
                employeeCard.setBackgroundColor(Color.WHITE);
                employeeCard.setTextSize(13);
                employeeCard.setTypeface(Typeface.DEFAULT_BOLD);
                employeeCard.setTextColor(0xFF333333);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 0, 0, 4);
                employeeCard.setLayoutParams(params);
                employeeCard.setElevation(2f);

                llEmployeesList.addView(employeeCard);

                // Запоминаем первого НЕ текущего сотрудника для полей "Специалист"
                if (!data.employee.equals(currentEmployee) && specialist.isEmpty()) {
                    specialist = data.employee;
                    shiftTime = data.shiftTime;
                }
            }
        } else {
            TextView tvEmpty = new TextView(getContext());
            tvEmpty.setText("На этот день никто не назначен");
            tvEmpty.setPadding(16, 16, 16, 16);
            tvEmpty.setTextSize(12);
            tvEmpty.setTextColor(0xFF999999);
            tvEmpty.setGravity(Gravity.CENTER);
            llEmployeesList.addView(tvEmpty);
        }

        if (!specialist.isEmpty()) {
            tvSpecialist.setText(specialist);
            tvShiftTime.setText(getDisplayText(shiftTime));
            tvShiftTime.setVisibility(View.VISIBLE);
        } else {
            tvSpecialist.setText("Не назначен");
            tvShiftTime.setVisibility(View.GONE);
        }

        tvUpdateTime.setText("Последнее обновление данных: " + new java.text.SimpleDateFormat("dd.MM.yyyy 'в' HH:mm", Locale.getDefault()).format(new java.util.Date()));
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
        if (employees.isEmpty()) return;

        String[] employeeNames = employees.stream().map(e -> e.name).toArray(String[]::new);
        String[] times = {"09:00-18:00", "10:00-19:00", "12:00-21:00", "Выходной", "Отпуск", "Больничный", "Другой офис"};

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_shift, null);
        Spinner spinnerEmployee = dialogView.findViewById(R.id.spinnerEmployee);
        Spinner spinnerTime = dialogView.findViewById(R.id.spinnerTime);
        spinnerEmployee.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, employeeNames));
        spinnerTime.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, times));

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