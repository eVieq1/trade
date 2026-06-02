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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.example.salestracker.ApiClient;
import com.example.salestracker.R;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

public class MonthCalendarFragment extends Fragment {

    private GridView gridSchedule;
    private TextView tvMonthYear, tvSelectedDate, tvUpdateTime;
    private LinearLayout llEmployeesList;

    private int year, month;
    private Map<String, List<ScheduleFragment.ShiftData>> shifts = new HashMap<>();
    private List<ScheduleFragment.Employee> employees = new ArrayList<>();
    private boolean isAdmin = false;
    private int selectedDay = 0;
    private ApiClient apiClient;
    private String currentEmployee;

    private final String[] monthNamesNominative = {"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};

    private final String[] monthNamesGenitive = {"января", "февраля", "марта", "апреля", "мая", "июня",
            "июля", "августа", "сентября", "октября", "ноября", "декабря"};

    public void setData(int year, int month, Map<String, List<ScheduleFragment.ShiftData>> shifts,
                        List<ScheduleFragment.Employee> employees, String currentEmployee, boolean isAdmin) {
        this.year = year;
        this.month = month;
        this.shifts = shifts;
        this.employees = employees;
        this.currentEmployee = currentEmployee;
        this.isAdmin = isAdmin;
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
        View view = inflater.inflate(R.layout.fragment_month_calendar, container, false);

        apiClient = new ApiClient();
        calendarAdapter = new CalendarAdapter();

        gridSchedule = view.findViewById(R.id.gridSchedule);
        tvMonthYear = view.findViewById(R.id.tvMonthYear);
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate);
        tvUpdateTime = view.findViewById(R.id.tvUpdateTime);
        llEmployeesList = view.findViewById(R.id.llEmployeesList);

        gridSchedule.setAdapter(calendarAdapter);
        gridSchedule.setNumColumns(7);
        gridSchedule.setVerticalSpacing(1);
        gridSchedule.setHorizontalSpacing(1);

        updateCalendar();
        return view;
    }

    public void updateData(Map<String, List<ScheduleFragment.ShiftData>> newShifts) {
        this.shifts = newShifts;
        updateCalendar();
    }

    private void updateCalendar() {
        if (getContext() == null) return;
        tvMonthYear.setText(monthNamesNominative[month] + " " + year);

        Calendar cal = Calendar.getInstance();
        cal.set(year, month, 1);
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

    private void showEditEmployeeDialog(String employee, String currentShiftTime, int day) {
        if (!isAdmin) {
            Toast.makeText(getContext(), "🔒 Доступ только для директора", Toast.LENGTH_SHORT).show();
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
                .setTitle("✏️ Редактировать смену")
                .setView(layout)
                .setPositiveButton("💾 Сохранить", (dialog, which) -> {
                    String newTime = spinnerTime.getSelectedItem().toString();
                    List<ScheduleFragment.ShiftData> newShifts = new ArrayList<>();
                    newShifts.add(new ScheduleFragment.ShiftData(employee, newTime));
                    String key = year + "-" + (month + 1) + "-" + day;
                    shifts.put(key, newShifts);

                    if (onShiftChangedListener != null) {
                        onShiftChangedListener.onShiftChanged(day, employee, newTime);
                    }
                    showDayInfo(selectedDay);
                })
                .setNeutralButton("🗑 Удалить", (dialog, which) -> {
                    String key = year + "-" + (month + 1) + "-" + day;
                    shifts.remove(key);
                    if (onShiftChangedListener != null) {
                        onShiftChangedListener.onShiftChanged(day, employee, "");
                    }
                    showDayInfo(selectedDay);
                })
                .setNegativeButton("❌ Отмена", null)
                .show();
    }

    private void showDayInfo(int day) {
        if (getContext() == null) return;

        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day);
        int weekday = cal.get(Calendar.DAY_OF_WEEK) - 2;
        if (weekday < 0) weekday = 6;
        String[] weekDays = {"Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье"};
        tvSelectedDate.setText(weekDays[weekday] + " - " + day + " " + monthNamesGenitive[month]);

        String key = year + "-" + (month + 1) + "-" + day;
        List<ScheduleFragment.ShiftData> dayShifts = shifts.get(key);

        llEmployeesList.removeAllViews();

        if (dayShifts != null && !dayShifts.isEmpty()) {
            for (ScheduleFragment.ShiftData data : dayShifts) {
                LinearLayout card = new LinearLayout(getContext());
                card.setOrientation(LinearLayout.HORIZONTAL);
                card.setPadding(16, 12, 16, 12);
                card.setBackgroundColor(Color.WHITE);
                card.setElevation(2f);
                card.setClickable(true);

                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                cardParams.setMargins(0, 0, 0, 6);
                card.setLayoutParams(cardParams);

                String rolePrefix = "";
                for (ScheduleFragment.Employee emp : employees) {
                    if (emp.name.equals(data.employee)) {
                        rolePrefix = emp.role.equals("dm") ? "👔 Директор: " : "👨‍💼 Специалист: ";
                        break;
                    }
                }

                TextView tvName = new TextView(getContext());
                tvName.setText(rolePrefix + data.employee);
                tvName.setTextSize(14);
                tvName.setTypeface(Typeface.DEFAULT_BOLD);
                tvName.setTextColor(0xFF333333);
                tvName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                TextView tvStatus = new TextView(getContext());
                tvStatus.setText(getDisplayText(data.shiftTime));
                tvStatus.setTextSize(12);
                tvStatus.setTextColor(getStatusColor(data.shiftTime));
                tvStatus.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                tvStatus.setGravity(Gravity.END);

                card.addView(tvName);
                card.addView(tvStatus);

                final String employeeName = data.employee;
                final String currentShift = data.shiftTime;
                card.setOnClickListener(v -> showEditEmployeeDialog(employeeName, currentShift, day));

                llEmployeesList.addView(card);
            }
        } else {
            TextView tvEmpty = new TextView(getContext());
            tvEmpty.setText("📅 На этот день никто не назначен");
            tvEmpty.setPadding(16, 24, 16, 24);
            tvEmpty.setTextSize(13);
            tvEmpty.setTextColor(0xFF999999);
            tvEmpty.setGravity(Gravity.CENTER);
            llEmployeesList.addView(tvEmpty);
        }

        tvUpdateTime.setText("🔄 Обновлено: " + new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new java.util.Date()));
    }

    private int getStatusColor(String shiftTime) {
        if (shiftTime == null) return 0xFF999999;
        switch (shiftTime) {
            case "09:00-18:00": return 0xFF4CAF50;
            case "10:00-19:00": return 0xFF4CAF50;
            case "12:00-21:00": return 0xFF4CAF50;
            case "Выходной": return 0xFFF44336;
            case "Отпуск": return 0xFFFF9800;
            case "Больничный": return 0xFF9E9E9E;
            case "Другой офис": return 0xFF2196F3;
            default: return 0xFF999999;
        }
    }

    private String getDisplayText(String shiftTime) {
        if (shiftTime == null) return "?";
        switch (shiftTime) {
            case "09:00-18:00": return "🕘 09:00-18:00";
            case "10:00-19:00": return "🕙 10:00-19:00";
            case "12:00-21:00": return "🕛 12:00-21:00";
            case "Выходной": return "🏠 Выходной";
            case "Отпуск": return "🏖 Отпуск";
            case "Больничный": return "🏥 Больничный";
            case "Другой офис": return "🏢 Другой офис";
            default: return shiftTime;
        }
    }

    public interface OnShiftChangedListener {
        void onShiftChanged(int day, String employee, String shiftTime);
    }

    private OnShiftChangedListener onShiftChangedListener;

    public void setOnShiftChangedListener(OnShiftChangedListener listener) {
        this.onShiftChangedListener = listener;
    }
}