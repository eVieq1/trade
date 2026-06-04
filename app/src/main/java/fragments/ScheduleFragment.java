package com.example.salestracker.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.text.Spanned;
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
import androidx.fragment.app.Fragment;
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

    private static final String TAG = "ScheduleFragment";

    // Views
    private RecyclerView monthRecyclerView;
    private GridView gridSchedule;
    private TextView tvSelectedDate;
    private TextView tvUpdateTime;
    private LinearLayout llEmployeesList;
    private Button btnEditShift;

    // Data
    private int currentYear, currentMonth;
    private Map<String, List<ShiftData>> shifts = new HashMap<>();
    private List<Employee> employees = new ArrayList<>();
    private boolean isAdmin = false;
    private int selectedDay = 0;
    private ApiClient apiClient;
    private String currentEmployee;
    private String currentUserRole;
    private int currentOfficeId = 0;

    // Month list for horizontal scroll
    private List<MonthData> monthList = new ArrayList<>();
    private int currentPosition = 120;
    private MonthAdapter monthAdapter;

    // Launchers
    private ActivityResultLauncher<String> exportLauncher;

    private final String[] monthNamesNominative = {"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};

    private final String[] monthNamesGenitive = {"января", "февраля", "марта", "апреля", "мая", "июня",
            "июля", "августа", "сентября", "октября", "ноября", "декабря"};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        exportLauncher = registerForActivityResult(new ActivityResultContracts.CreateDocument(), uri -> {
            if (uri != null) exportToCsv(uri);
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_schedule, container, false);

        initViews(view);
        initData();
        setupMonthScrolling();
        loadEmployees();
        loadFromServer();

        return view;
    }

    private void initViews(View view) {
        monthRecyclerView = view.findViewById(R.id.monthRecyclerView);
        gridSchedule = view.findViewById(R.id.gridSchedule);
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate);
        tvUpdateTime = view.findViewById(R.id.tvUpdateTime);
        llEmployeesList = view.findViewById(R.id.llEmployeesList);
        btnEditShift = view.findViewById(R.id.btnEditShift);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        monthRecyclerView.setLayoutManager(layoutManager);
        monthRecyclerView.setNestedScrollingEnabled(false);

        gridSchedule.setNumColumns(7);
        gridSchedule.setVerticalSpacing(1);
        gridSchedule.setHorizontalSpacing(1);
    }

    private void initData() {
        apiClient = new ApiClient();

        SharedPreferences prefs = requireActivity().getSharedPreferences("app", Context.MODE_PRIVATE);
        currentEmployee = prefs.getString("employee_name", "");
        currentUserRole = prefs.getString("user_role", "seller");
        isAdmin = currentUserRole.equals("dm") || currentUserRole.equals("owner") || currentUserRole.equals("rgo");

        if (currentUserRole.equals("dm")) {
            currentOfficeId = prefs.getInt("office_id", 0);
        } else {
            currentOfficeId = prefs.getInt("current_office_id", 0);
        }

        if (isAdmin) {
            btnEditShift.setVisibility(View.VISIBLE);
            btnEditShift.setOnClickListener(v -> showAddEmployeeDialog(selectedDay));
        } else {
            btnEditShift.setVisibility(View.GONE);
        }

        Calendar cal = Calendar.getInstance();
        currentYear = cal.get(Calendar.YEAR);
        currentMonth = cal.get(Calendar.MONTH);

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
    }

    private void setupMonthScrolling() {
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
                        int officeId = emp.optInt("office_id", 0);
                        if (currentOfficeId == 0 || officeId == currentOfficeId) {
                            employees.add(new Employee(
                                    emp.getInt("id"),
                                    emp.getString("name"),
                                    emp.getString("role")
                            ));
                        }
                    }
                    Log.d(TAG, "Загружено сотрудников: " + employees.size());
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка загрузки сотрудников: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Ошибка загрузки сотрудников: " + error);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Ошибка загрузки сотрудников", Toast.LENGTH_SHORT).show();
                }
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
                    updateUpdateTime();
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка парсинга: " + e.getMessage());
                    updateCalendar();
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Ошибка загрузки: " + error);
                updateCalendar();
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Ошибка загрузки графика", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void saveToServer(int day, String employee, String shiftTime) {
        apiClient.saveSchedule(currentYear, currentMonth + 1, day, employee, shiftTime, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                Log.d(TAG, "Сохранено: " + response);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Ошибка сохранения: " + error);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Ошибка сохранения", Toast.LENGTH_SHORT).show();
                }
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

        if (gridSchedule.getAdapter() == null) {
            CalendarAdapter calendarAdapter = new CalendarAdapter();
            gridSchedule.setAdapter(calendarAdapter);
        }
        ((CalendarAdapter) gridSchedule.getAdapter()).setDays(daysList);

        if (selectedDay == 0 || selectedDay > daysInMonth) {
            selectedDay = daysInMonth > 0 ? 1 : 0;
        }
        if (selectedDay > 0) {
            showDayInfo(selectedDay);
        }
    }

    private void updateUpdateTime() {
        if (tvUpdateTime != null) {
            tvUpdateTime.setText("Обновлено: " + new java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new java.util.Date()));
        }
    }

    private void showAddEmployeeDialog(int day) {
        if (day == 0) {
            Toast.makeText(getContext(), "Сначала выберите день", Toast.LENGTH_SHORT).show();
            return;
        }

        if (employees.isEmpty()) {
            Toast.makeText(getContext(), "Список сотрудников пуст", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] employeeNames = new String[employees.size()];
        for (int i = 0; i < employees.size(); i++) {
            employeeNames[i] = employees.get(i).name;
        }

        String[] times = {"09:00-18:00", "10:00-19:00", "12:00-21:00", "Выходной", "Отпуск", "Больничный", "Другой офис"};

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

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

        builder.setTitle("Добавить сотрудника")
                .setView(layout)
                .setPositiveButton("Добавить", (dialog, which) -> {
                    String employee = spinnerEmployee.getSelectedItem().toString();
                    String time = spinnerTime.getSelectedItem().toString();

                    String key = currentYear + "-" + (currentMonth + 1) + "-" + day;
                    List<ShiftData> currentShifts = shifts.get(key);
                    if (currentShifts == null) currentShifts = new ArrayList<>();

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
                .setNegativeButton("Отмена", null);

        builder.show();
    }

    private boolean isNonWorkingShift(String shiftTime) {
        return shiftTime != null && (shiftTime.equals("Выходной") || shiftTime.equals("Отпуск") ||
                shiftTime.equals("Больничный") || shiftTime.equals("Другой офис"));
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

        if (dayShifts != null && !dayShifts.isEmpty()) {
            for (ShiftData data : dayShifts) {
                LinearLayout card = new LinearLayout(getContext());
                card.setOrientation(LinearLayout.HORIZONTAL);
                card.setPadding(16, 12, 16, 12);
                card.setBackgroundColor(Color.WHITE);
                card.setElevation(2f);
                card.setClickable(true);
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                cardParams.setMargins(8, 4, 8, 4);
                card.setLayoutParams(cardParams);

                String role = "Специалист";
                for (Employee emp : employees) {
                    if (emp.name.equals(data.employee)) {
                        if (emp.role.equals("dm")) role = "Директор";
                        else if (emp.role.equals("senior_seller")) role = "Старший специалист";
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
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                tvRight.setGravity(Gravity.END);

                card.addView(tvLeft);
                card.addView(tvRight);
                card.setOnClickListener(v -> showEditEmployeeDialog(data.employee, data.shiftTime, day));
                llEmployeesList.addView(card);
            }
        } else {
            TextView tvEmpty = new TextView(getContext());
            tvEmpty.setText("На этот день никто не назначен");
            tvEmpty.setPadding(16, 24, 16, 24);
            tvEmpty.setTextSize(13);
            tvEmpty.setTextColor(0xFF999999);
            tvEmpty.setGravity(Gravity.CENTER);
            llEmployeesList.addView(tvEmpty);
        }
        updateUpdateTime();
    }

    // ==================== ДИАЛОГ РЕДАКТИРОВАНИЯ СМЕНЫ (С ПРОКРУТКОЙ) ====================

    private void showEditEmployeeDialog(String employee, String currentShiftTime, int day) {
        if (!isAdmin) {
            Toast.makeText(getContext(), "Доступ только для директора, владельца или РГО", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Редактировать смену: " + employee);

        // Основной контейнер с ScrollView для прокрутки диалога
        LinearLayout mainLayout = new LinearLayout(getContext());
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(48, 32, 48, 24);

        ScrollView scrollView = new ScrollView(getContext());
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        // Текущая смена
        TextView tvCurrent = new TextView(getContext());
        tvCurrent.setText("Текущее: " + getDisplayText(currentShiftTime));
        tvCurrent.setPadding(0, 0, 0, 16);
        tvCurrent.setTextSize(14);
        layout.addView(tvCurrent);

        // Разделитель
        TextView tvOr = new TextView(getContext());
        tvOr.setText("──────────  Выберите тип  ──────────");
        tvOr.setGravity(Gravity.CENTER);
        tvOr.setPadding(0, 16, 0, 16);
        tvOr.setTextSize(14);
        layout.addView(tvOr);

        // Spinner для выбора типа смены
        Spinner spinnerType = new Spinner(getContext());
        String[] types = {"Рабочий", "Выходной", "Отпуск", "Больничный", "Другой офис"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, types);
        spinnerType.setAdapter(typeAdapter);
        layout.addView(spinnerType);

        // Контейнер для времени
        LinearLayout timeContainer = new LinearLayout(getContext());
        timeContainer.setOrientation(LinearLayout.VERTICAL);
        timeContainer.setPadding(0, 16, 0, 0);

        TextView tvStart = new TextView(getContext());
        tvStart.setText("Начало смены:");
        tvStart.setPadding(0, 8, 0, 4);
        tvStart.setTextSize(14);
        timeContainer.addView(tvStart);

        TimePicker startPicker = new TimePicker(getContext());
        startPicker.setIs24HourView(true);
        timeContainer.addView(startPicker);

        TextView tvEnd = new TextView(getContext());
        tvEnd.setText("Конец смены:");
        tvEnd.setPadding(0, 16, 0, 4);
        tvEnd.setTextSize(14);
        timeContainer.addView(tvEnd);

        TimePicker endPicker = new TimePicker(getContext());
        endPicker.setIs24HourView(true);
        timeContainer.addView(endPicker);

        layout.addView(timeContainer);

        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                timeContainer.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Установка текущих значений
        if (isNonWorkingShift(currentShiftTime)) {
            if (currentShiftTime.equals("Выходной")) spinnerType.setSelection(1);
            else if (currentShiftTime.equals("Отпуск")) spinnerType.setSelection(2);
            else if (currentShiftTime.equals("Больничный")) spinnerType.setSelection(3);
            else if (currentShiftTime.equals("Другой офис")) spinnerType.setSelection(4);
            timeContainer.setVisibility(View.GONE);
        } else {
            spinnerType.setSelection(0);
            if (currentShiftTime != null && currentShiftTime.contains("-")) {
                try {
                    String start = currentShiftTime.split("-")[0];
                    String end = currentShiftTime.split("-")[1];
                    startPicker.setHour(Integer.parseInt(start.split(":")[0]));
                    startPicker.setMinute(Integer.parseInt(start.split(":")[1]));
                    endPicker.setHour(Integer.parseInt(end.split(":")[0]));
                    endPicker.setMinute(Integer.parseInt(end.split(":")[1]));
                } catch (Exception e) {}
            }
        }

        scrollView.addView(layout);
        mainLayout.addView(scrollView);
        builder.setView(mainLayout);

        // Кнопки
        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String selectedType = spinnerType.getSelectedItem().toString();
            String newShiftTime;
            if (selectedType.equals("Рабочий")) {
                newShiftTime = String.format(Locale.getDefault(), "%02d:%02d-%02d:%02d",
                        startPicker.getHour(), startPicker.getMinute(),
                        endPicker.getHour(), endPicker.getMinute());
            } else {
                newShiftTime = selectedType;
            }
            saveToServer(day, employee, newShiftTime);

            String key = currentYear + "-" + (currentMonth + 1) + "-" + day;
            List<ShiftData> currentShifts = shifts.get(key);
            if (currentShifts == null) currentShifts = new ArrayList<>();
            boolean found = false;
            for (ShiftData s : currentShifts) {
                if (s.employee.equals(employee)) {
                    s.shiftTime = newShiftTime;
                    found = true;
                    break;
                }
            }
            if (!found) {
                currentShifts.add(new ShiftData(employee, newShiftTime));
            }
            shifts.put(key, currentShifts);
            showDayInfo(selectedDay);
            updateCalendar();
            Toast.makeText(getContext(), "Смена сохранена", Toast.LENGTH_SHORT).show();
        });

        builder.setNeutralButton("Удалить", (dialog, which) -> {
            String key = currentYear + "-" + (currentMonth + 1) + "-" + day;
            List<ShiftData> currentShifts = shifts.get(key);
            if (currentShifts != null) {
                currentShifts.removeIf(s -> s.employee.equals(employee));
                if (currentShifts.isEmpty()) shifts.remove(key);
                else shifts.put(key, currentShifts);
            }
            showDayInfo(selectedDay);
            updateCalendar();
            Toast.makeText(getContext(), "Сотрудник удален", Toast.LENGTH_SHORT).show();
            saveToServer(day, employee, "");
        });

        builder.setNegativeButton("Отмена", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private int getStatusColor(String shiftTime) {
        if (shiftTime == null) return 0xFFFFFFFF;
        switch (shiftTime) {
            case "Выходной": return 0xFFBBDEFB;
            case "Отпуск": return 0xFFBB40AC;
            case "Больничный": return 0xFFBB40AC;
            case "Другой офис": return 0xFFFFF9C4;
            default: return 0xFFC8E6C9;
        }
    }

    private String getFormattedShiftDisplay(String shiftTime) {
        if (shiftTime == null) return "";
        switch (shiftTime) {
            case "Выходной": return "Вых";
            case "Отпуск": return "Отп";
            case "Больничный": return "Бол";
            case "Другой офис": return "Др";
            default:
                if (shiftTime.contains("-")) {
                    String[] parts = shiftTime.split("-");
                    if (parts.length == 2) {
                        return parts[0].trim() + "\n" + parts[1].trim();
                    }
                }
                return shiftTime;
        }
    }

    private String getDisplayText(String shiftTime) {
        if (shiftTime == null) return "?";
        switch (shiftTime) {
            case "Выходной": return "Выходной";
            case "Отпуск": return "Отпуск";
            case "Больничный": return "Больничный";
            case "Другой офис": return "Другой офис";
            default: return shiftTime;
        }
    }

    public void refreshData() {
        if (getContext() != null) {
            loadFromServer();
            Toast.makeText(getContext(), "Данные обновлены", Toast.LENGTH_SHORT).show();
        }
    }

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

    // ==================== ADAPTERS ====================

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
                        selectedDay = 0;
                        loadFromServer();
                        notifyDataSetChanged();
                        monthRecyclerView.smoothScrollToPosition(currentPosition);
                    }
                });
            }
        }
    }

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
                cell.setTypeface(Typeface.DEFAULT_BOLD);
                int cellSizePx = (int) (45 * getResources().getDisplayMetrics().density);
                cell.setLayoutParams(new GridView.LayoutParams(GridView.LayoutParams.MATCH_PARENT, cellSizePx));
            } else {
                cell = (TextView) convertView;
            }

            int dayNumber = days.get(position);
            if (dayNumber == 0) {
                cell.setText("");
                cell.setBackgroundColor(0xFFF5F5F5);
                cell.setTextColor(0xFF000000);
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

                if (currentUserStatus != null && !currentUserStatus.isEmpty()) {
                    String shiftDisplay = getFormattedShiftDisplay(currentUserStatus);

                    if (shiftDisplay.contains("\n")) {
                        String[] lines = shiftDisplay.split("\n");
                        if (lines.length == 2) {
                            SpannableStringBuilder ssb = new SpannableStringBuilder();
                            ssb.append(dayNumber + "\n");
                            ssb.setSpan(new AbsoluteSizeSpan(14, true), 0, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            ssb.append(lines[0] + "\n");
                            ssb.setSpan(new AbsoluteSizeSpan(9, true), ssb.length() - (lines[0].length() + 1), ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            ssb.append(lines[1]);
                            ssb.setSpan(new AbsoluteSizeSpan(9, true), ssb.length() - lines[1].length(), ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            cell.setText(ssb);
                        } else {
                            cell.setText(dayNumber + "\n" + shiftDisplay);
                        }
                    } else {
                        cell.setText(dayNumber + "\n" + shiftDisplay);
                    }
                    cell.setTextSize(12);
                    cell.setBackgroundColor(getStatusColor(currentUserStatus));
                } else {
                    cell.setText(String.valueOf(dayNumber));
                    cell.setTextSize(12);
                    cell.setBackgroundColor(0xFFFFFFFF);
                }

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
            return cell;
        }
    }

    // ==================== DATA CLASSES ====================

    private static class MonthData {
        int year, month;
        MonthData(int year, int month) {
            this.year = year;
            this.month = month;
        }
    }

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (apiClient != null) {
            apiClient.shutdown();
        }
    }
}