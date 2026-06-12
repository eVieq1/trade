package com.example.salestracker.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.Color;
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
import com.example.salestracker.EditDayScheduleDialog;
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

    private final String[] monthNamesNominative = {"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            btnEditShift.setOnClickListener(v -> {
                if (selectedDay > 0) {
                    EditDayScheduleDialog dialog = new EditDayScheduleDialog(getContext(), currentYear, currentMonth + 1, selectedDay);
                    dialog.show();
                } else {
                    Toast.makeText(getContext(), "Сначала выберите день", Toast.LENGTH_SHORT).show();
                }
            });
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

    private void showDayInfo(int day) {
        if (getContext() == null) return;

        Calendar cal = Calendar.getInstance();
        cal.set(currentYear, currentMonth, day);
        int weekday = cal.get(Calendar.DAY_OF_WEEK) - 2;
        if (weekday < 0) weekday = 6;
        String[] weekDays = {"Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье"};
        tvSelectedDate.setText(weekDays[weekday] + " - " + day + " " + monthNamesNominative[currentMonth]);

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