package com.example.salestracker.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.salestracker.ApiClient;
import com.example.salestracker.CsvHelper;
import com.example.salestracker.R;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.util.*;

public class ScheduleFragment extends Fragment {

    private RecyclerView rvCalendar;
    private TextView tvSelectedDate, tvUpdateTime, tvPostalCode;
    private LinearLayout llEmployeesList;
    private RecyclerView monthRecyclerView;
    private MonthAdapter monthAdapter;
    private CalendarAdapter calendarAdapter;

    private int currentYear, currentMonth;
    private Map<String, List<ShiftData>> shifts = new HashMap<>();
    private List<Employee> employees = new ArrayList<>();
    private boolean isAdmin = false;
    private int selectedDay = 0;
    private ApiClient apiClient;
    private String currentEmployee;

    private List<MonthData> monthList = new ArrayList<>();
    private int currentPosition = 120;

    // ActivityResultLauncher для экспорта и импорта
    private ActivityResultLauncher<String> exportLauncher;
    private ActivityResultLauncher<String[]> importLauncher;

    private final String[] monthNamesNominative = {"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};

    private final String[] monthNamesGenitive = {"января", "февраля", "марта", "апреля", "мая", "июня",
            "июля", "августа", "сентября", "октября", "ноября", "декабря"};

    // Получить цвет для ячейки календаря по статусу
    private int getStatusColor(String shiftTime) {
        if (shiftTime == null) return 0xFFFFFFFF;
        switch (shiftTime) {
            case "09:00-18:00": return 0xFFC8E6C9;
            case "10:00-19:00": return 0xFFC8E6C9;
            case "12:00-21:00": return 0xFFC8E6C9;
            case "Выходной": return 0xFFBBDEFB;
            case "Отпуск": return 0xFFBB40AC;
            case "Больничный": return 0xFFBB40AC;
            case "Другой офис": return 0xFFFFF9C4;
            default: return 0xFFFFFFFF;
        }
    }

    private String getShortStatus(String shiftTime) {
        if (shiftTime == null) return "";
        switch (shiftTime) {
            case "09:00-18:00": return "9-18";
            case "10:00-19:00": return "10-19";
            case "12:00-21:00": return "12-21";
            case "Выходной": return "Вых";
            case "Отпуск": return "Отп";
            case "Больничный": return "Бол";
            case "Другой офис": return "Др";
            default: return "";
        }
    }

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

    private static class MonthData {
        int year, month;
        MonthData(int year, int month) {
            this.year = year;
            this.month = month;
        }
    }

    private class MonthAdapter extends RecyclerView.Adapter<MonthAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(16);
            tv.setTypeface(Typeface.DEFAULT_BOLD);
            tv.setPadding(48, 16, 48, 16);
            return new ViewHolder(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MonthData data = monthList.get(position);
            String text = monthNamesNominative[data.month] + " " + data.year;
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
        public int getItemCount() { return monthList.size(); }

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
                        selectedDay = 0;
                        loadFromServer();
                        notifyDataSetChanged();
                        monthRecyclerView.smoothScrollToPosition(currentPosition);
                    }
                });
            }
        }
    }

    private class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.ViewHolder> {
        private List<Integer> days = new ArrayList<>();

        public void setDays(List<Integer> days) {
            this.days = days;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(12);
            tv.setTypeface(Typeface.DEFAULT_BOLD);
            int cellSizePx = (int) (45 * getResources().getDisplayMetrics().density);
            tv.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    cellSizePx
            ));
            return new ViewHolder(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            int dayNumber = days.get(position);
            if (dayNumber == 0) {
                holder.textView.setText("");
                holder.textView.setBackgroundColor(0xFFF5F5F5);
            } else {
                String key = currentYear + "-" + (currentMonth + 1) + "-" + dayNumber;
                List<ShiftData> dayShifts = shifts.get(key);

                String currentUserStatus = null;
                if (dayShifts != null) {
                    for (ShiftData data : dayShifts) {
                        if (data.employee.equals(currentEmployee)) {
                            currentUserStatus = data.shiftTime;
                            break;
                        }
                    }
                }

                String cellText = String.valueOf(dayNumber);
                String shortStatus = "";
                if (currentUserStatus != null && !currentUserStatus.isEmpty()) {
                    shortStatus = getShortStatus(currentUserStatus);
                    if (!shortStatus.isEmpty()) {
                        cellText = dayNumber + "\n" + shortStatus;
                    }
                }

                holder.textView.setText(cellText);
                holder.textView.setTextSize(currentUserStatus != null ? 10 : 12);

                if (currentUserStatus != null && !currentUserStatus.isEmpty()) {
                    holder.textView.setBackgroundColor(getStatusColor(currentUserStatus));
                } else {
                    holder.textView.setBackgroundColor(0xFFFFFFFF);
                }

                if (selectedDay == dayNumber) {
                    holder.textView.setBackgroundColor(0xFF2196F3);
                    holder.textView.setTextColor(0xFFFFFFFF);
                } else {
                    holder.textView.setTextColor(0xFF000000);
                }

                holder.textView.setOnClickListener(v -> {
                    selectedDay = dayNumber;
                    showDayInfo(selectedDay);
                    notifyDataSetChanged();
                });
            }
        }

        @Override
        public int getItemCount() { return days.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            ViewHolder(View itemView) {
                super(itemView);
                textView = (TextView) itemView;
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Инициализация лаунчеров для экспорта и импорта
        exportLauncher = registerForActivityResult(new ActivityResultContracts.CreateDocument(), uri -> {
            if (uri != null) {
                exportToCsv(uri);
            }
        });

        importLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) {
                Toast.makeText(getContext(), "Импорт пока в разработке", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_schedule, container, false);

        apiClient = new ApiClient();
        calendarAdapter = new CalendarAdapter();

        rvCalendar = view.findViewById(R.id.rvCalendar);
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate);
        tvUpdateTime = view.findViewById(R.id.tvUpdateTime);
        tvPostalCode = view.findViewById(R.id.tvPostalCode);
        llEmployeesList = view.findViewById(R.id.llEmployeesList);
        monthRecyclerView = view.findViewById(R.id.monthRecyclerView);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 7);
        rvCalendar.setLayoutManager(gridLayoutManager);
        rvCalendar.setAdapter(calendarAdapter);

        SharedPreferences prefs = requireActivity().getSharedPreferences("app", Context.MODE_PRIVATE);
        currentEmployee = prefs.getString("employee_name", "");
        isAdmin = prefs.getString("user_role", "seller").equals("dm");

        Calendar cal = Calendar.getInstance();
        currentYear = cal.get(Calendar.YEAR);
        currentMonth = cal.get(Calendar.MONTH);

        // Генерируем список месяцев для свайпа
        for (int i = -120; i <= 120; i++) {
            int y = currentYear + (i / 12);
            int m = currentMonth + i;
            while (m < 0) { m += 12; y--; }
            while (m > 11) { m -= 12; y++; }
            monthList.add(new MonthData(y, m));
        }
        currentPosition = 120;

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
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
                        selectedDay = 0;
                        loadFromServer();
                        monthAdapter.notifyDataSetChanged();
                    }
                }
            }
        });

        monthRecyclerView.scrollToPosition(currentPosition);

        loadEmployees();
        loadFromServer();

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
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onError(String error) {
                Log.e("ScheduleFragment", "Ошибка загрузки сотрудников: " + error);
            }
        });
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
            public void onSuccess(String response) {
                Log.d("ScheduleFragment", "Сохранено: " + response);
            }
            @Override
            public void onError(String error) {
                Log.e("ScheduleFragment", "Ошибка: " + error);
            }
        });
    }

    private void updateCalendar() {
        if (getContext() == null) return;

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

        if (selectedDay == 0 || selectedDay > daysInMonth) {
            selectedDay = 1;
        }

        showDayInfo(selectedDay);

        if (monthAdapter != null) {
            monthAdapter.notifyDataSetChanged();
        }
    }

    private void showAddEmployeeDialog(int day) {
        if (!isAdmin) {
            Toast.makeText(getContext(), "Доступ только для директора", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] employeeNames = new String[employees.size()];
        for (int i = 0; i < employees.size(); i++) {
            employeeNames[i] = employees.get(i).name;
        }
        String[] times = {"09:00-18:00", "10:00-19:00", "12:00-21:00", "Выходной", "Отпуск", "Больничный", "Другой офис"};

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 16);

        TextView tvDay = new TextView(getContext());
        tvDay.setText("Дата: " + day + " " + monthNamesGenitive[currentMonth]);
        tvDay.setTextSize(16);
        tvDay.setTypeface(Typeface.DEFAULT_BOLD);
        tvDay.setPadding(0, 0, 0, 16);
        layout.addView(tvDay);

        TextView tvEmployeeLabel = new TextView(getContext());
        tvEmployeeLabel.setText("Выберите сотрудника:");
        tvEmployeeLabel.setTextSize(14);
        tvEmployeeLabel.setPadding(0, 8, 0, 8);
        layout.addView(tvEmployeeLabel);

        Spinner spinnerEmployee = new Spinner(getContext());
        ArrayAdapter<String> employeeAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, employeeNames);
        spinnerEmployee.setAdapter(employeeAdapter);
        layout.addView(spinnerEmployee);

        TextView tvTimeLabel = new TextView(getContext());
        tvTimeLabel.setText("Выберите время/статус:");
        tvTimeLabel.setTextSize(14);
        tvTimeLabel.setPadding(0, 8, 0, 8);
        layout.addView(tvTimeLabel);

        Spinner spinnerTime = new Spinner(getContext());
        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, times);
        spinnerTime.setAdapter(timeAdapter);
        layout.addView(spinnerTime);

        new AlertDialog.Builder(getContext())
                .setTitle("Добавить сотрудника")
                .setView(layout)
                .setPositiveButton("Добавить", (dialog, which) -> {
                    String employee = spinnerEmployee.getSelectedItem().toString();
                    String time = spinnerTime.getSelectedItem().toString();

                    String key = currentYear + "-" + (currentMonth + 1) + "-" + day;
                    List<ShiftData> currentShifts = shifts.get(key);
                    if (currentShifts == null) {
                        currentShifts = new ArrayList<>();
                    }

                    boolean exists = false;
                    for (ShiftData s : currentShifts) {
                        if (s.employee.equals(employee)) {
                            exists = true;
                            break;
                        }
                    }

                    if (!exists) {
                        currentShifts.add(new ShiftData(employee, time));
                        shifts.put(key, currentShifts);
                        saveToServer(day, employee, time);
                        showDayInfo(selectedDay);
                        updateCalendar();
                        Toast.makeText(getContext(), "Сотрудник добавлен", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Сотрудник уже есть в этот день", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showEditEmployeeDialog(String employee, String currentShiftTime, int day) {
        if (!isAdmin) {
            Toast.makeText(getContext(), "Доступ только для директора", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] times = {"09:00-18:00", "10:00-19:00", "12:00-21:00", "Выходной", "Отпуск", "Больничный", "Другой офис"};

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 16);

        TextView tvEmployee = new TextView(getContext());
        tvEmployee.setText("Сотрудник: " + employee);
        tvEmployee.setTextSize(16);
        tvEmployee.setTypeface(Typeface.DEFAULT_BOLD);
        tvEmployee.setPadding(0, 0, 0, 16);
        layout.addView(tvEmployee);

        TextView tvTimeLabel = new TextView(getContext());
        tvTimeLabel.setText("Выберите время/статус:");
        tvTimeLabel.setTextSize(14);
        tvTimeLabel.setPadding(0, 8, 0, 8);
        layout.addView(tvTimeLabel);

        Spinner spinnerTime = new Spinner(getContext());
        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, times);
        spinnerTime.setAdapter(timeAdapter);

        for (int i = 0; i < times.length; i++) {
            if (times[i].equals(currentShiftTime)) {
                spinnerTime.setSelection(i);
                break;
            }
        }
        layout.addView(spinnerTime);

        new AlertDialog.Builder(getContext())
                .setTitle("Редактировать смену")
                .setView(layout)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String newTime = spinnerTime.getSelectedItem().toString();
                    saveToServer(day, employee, newTime);
                    String key = currentYear + "-" + (currentMonth + 1) + "-" + day;
                    List<ShiftData> currentShifts = shifts.get(key);
                    if (currentShifts == null) {
                        currentShifts = new ArrayList<>();
                    }
                    boolean found = false;
                    for (ShiftData s : currentShifts) {
                        if (s.employee.equals(employee)) {
                            s.shiftTime = newTime;
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        currentShifts.add(new ShiftData(employee, newTime));
                    }
                    shifts.put(key, currentShifts);
                    showDayInfo(selectedDay);
                    updateCalendar();
                    Toast.makeText(getContext(), "Смена сохранена", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("Удалить", (dialog, which) -> {
                    String key = currentYear + "-" + (currentMonth + 1) + "-" + day;
                    List<ShiftData> currentShifts = shifts.get(key);
                    if (currentShifts != null) {
                        currentShifts.removeIf(s -> s.employee.equals(employee));
                        if (currentShifts.isEmpty()) {
                            shifts.remove(key);
                        } else {
                            shifts.put(key, currentShifts);
                        }
                    }
                    showDayInfo(selectedDay);
                    updateCalendar();
                    Toast.makeText(getContext(), "Сотрудник удален", Toast.LENGTH_SHORT).show();
                    saveToServer(day, employee, "");
                })
                .setNegativeButton("Отмена", null)
                .show();
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

        llEmployeesList.removeAllViews();

        if (isAdmin) {
            Button btnAddEmployee = new Button(getContext());
            btnAddEmployee.setText("+ Добавить сотрудника");
            btnAddEmployee.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF2196F3));
            btnAddEmployee.setTextColor(Color.WHITE);
            btnAddEmployee.setPadding(16, 12, 16, 12);
            btnAddEmployee.setOnClickListener(v -> showAddEmployeeDialog(day));
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            btnParams.setMargins(0, 0, 0, 12);
            btnAddEmployee.setLayoutParams(btnParams);
            llEmployeesList.addView(btnAddEmployee);
        }

        if (dayShifts != null && !dayShifts.isEmpty()) {
            for (ShiftData data : dayShifts) {
                if (data.shiftTime == null || data.shiftTime.isEmpty()) {
                    continue;
                }

                LinearLayout card = new LinearLayout(getContext());
                card.setOrientation(LinearLayout.HORIZONTAL);
                card.setPadding(16, 12, 16, 12);
                card.setBackgroundColor(Color.WHITE);
                card.setElevation(2f);
                card.setClickable(true);

                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                cardParams.setMargins(8, 4, 8, 4);
                card.setLayoutParams(cardParams);

                String role = "Специалист";
                for (Employee emp : employees) {
                    if (emp.name.equals(data.employee)) {
                        if (emp.role.equals("dm")) {
                            role = "Директор";
                        }
                        break;
                    }
                }

                TextView tvLeft = new TextView(getContext());
                tvLeft.setText(role + ": " + data.employee);
                tvLeft.setTextSize(14);
                tvLeft.setTypeface(Typeface.DEFAULT_BOLD);
                tvLeft.setTextColor(0xFF333333);
                tvLeft.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

                TextView tvRight = new TextView(getContext());
                tvRight.setText(getDisplayText(data.shiftTime));
                tvRight.setTextSize(14);
                tvRight.setTypeface(Typeface.DEFAULT_BOLD);
                tvRight.setTextColor(getStatusColor(data.shiftTime));
                tvRight.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                tvRight.setGravity(Gravity.END);

                card.addView(tvLeft);
                card.addView(tvRight);

                final String employeeName = data.employee;
                final String currentShift = data.shiftTime;
                card.setOnClickListener(v -> showEditEmployeeDialog(employeeName, currentShift, day));

                llEmployeesList.addView(card);
            }
        }

        tvUpdateTime.setText("Обновлено: " + new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new java.util.Date()));
    }

    // ==================== ПУБЛИЧНЫЕ МЕТОДЫ ДЛЯ ВЫЗОВА ИЗ МЕНЮ ====================

    public void refreshData() {
        if (getContext() != null) {
            loadFromServer();
            Toast.makeText(getContext(), "Данные обновлены", Toast.LENGTH_SHORT).show();
        }
    }

    // Экспорт в CSV и сразу отправка на почту
    public void exportToExcel() {
        exportLauncher.launch("schedules_" + currentYear + "_" + (currentMonth + 1) + ".csv");
    }

    private void exportToCsv(Uri uri) {
        try {
            File cacheFile = new File(requireContext().getCacheDir(), "schedules_" + currentYear + "_" + (currentMonth + 1) + ".csv");
            CsvHelper csvHelper = new CsvHelper(getContext());
            csvHelper.exportToCsvToFile(currentYear, currentMonth + 1, shifts, employees, cacheFile);
            sendEmailWithCsv(uri);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void importFromExcel() {
        importLauncher.launch(new String[]{"text/csv", "application/vnd.ms-excel"});
    }

    public void showEditDialog() {
        if (getContext() == null || employees.isEmpty()) return;

        if (!isAdmin) {
            Toast.makeText(getContext(), "Доступ только для директора", Toast.LENGTH_SHORT).show();
            return;
        }

        showAddEmployeeDialog(selectedDay);
    }

    // Отправка CSV на почту
    private void sendEmailWithCsv(Uri uri) {
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("text/csv");
        emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"eVieq@yandex.ru"});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Экспорт смен за " + monthNamesNominative[currentMonth] + " " + currentYear);
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Файл с графиком смен прилагается.");
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(emailIntent, "Отправить email"));
    }

    // Публичные статические классы для доступа из CsvHelper
    public static class ShiftData {
        public String employee, shiftTime;
        public ShiftData(String employee, String shiftTime) {
            this.employee = employee;
            this.shiftTime = shiftTime;
        }
    }

    public static class Employee {
        public int id;
        public String name, role;
        public Employee(int id, String name, String role) {
            this.id = id;
            this.name = name;
            this.role = role;
        }
    }
}