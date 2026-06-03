package com.example.salestracker;

import android.content.Context;
import android.net.Uri;
import android.widget.Toast;

import com.example.salestracker.fragments.ScheduleFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class CsvHelper {

    private Context context;

    public CsvHelper(Context context) {
        this.context = context;
    }

    // Экспорт в CSV через Uri (для сохранения файла)
    public void exportToCsv(int year, int month, Map<String, List<ScheduleFragment.ShiftData>> shifts,
                            List<ScheduleFragment.Employee> employees, Uri uri) {
        try {
            OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
            OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);

            // Заголовки
            writer.append("Дата;День недели;Сотрудник;Должность;Статус/Время\n");

            Calendar cal = Calendar.getInstance();
            cal.set(year, month - 1, 1);
            int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            String[] weekDays = {"Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье"};

            for (int day = 1; day <= daysInMonth; day++) {
                String key = year + "-" + month + "-" + day;
                List<ScheduleFragment.ShiftData> dayShifts = shifts.get(key);

                if (dayShifts != null && !dayShifts.isEmpty()) {
                    for (ScheduleFragment.ShiftData shift : dayShifts) {
                        if (shift.shiftTime == null || shift.shiftTime.isEmpty()) continue;

                        cal.set(year, month - 1, day);
                        int weekday = cal.get(Calendar.DAY_OF_WEEK) - 2;
                        if (weekday < 0) weekday = 6;

                        String role = getRoleDisplay(shift.employee, employees);
                        String line = day + "." + month + "." + year + ";" +
                                weekDays[weekday] + ";" +
                                shift.employee + ";" +
                                role + ";" +
                                getDisplayText(shift.shiftTime) + "\n";
                        writer.append(line);
                    }
                }
            }

            writer.flush();
            writer.close();
            outputStream.close();
            Toast.makeText(context, "Экспорт завершен", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(context, "Ошибка экспорта: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // НОВЫЙ МЕТОД: Экспорт в файл (для отправки на почту)
    public void exportToCsvToFile(int year, int month, Map<String, List<ScheduleFragment.ShiftData>> shifts,
                                  List<ScheduleFragment.Employee> employees, File file) {
        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);

            // Заголовки
            writer.append("Дата;День недели;Сотрудник;Должность;Статус/Время\n");

            Calendar cal = Calendar.getInstance();
            cal.set(year, month - 1, 1);
            int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            String[] weekDays = {"Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье"};

            for (int day = 1; day <= daysInMonth; day++) {
                String key = year + "-" + month + "-" + day;
                List<ScheduleFragment.ShiftData> dayShifts = shifts.get(key);

                if (dayShifts != null && !dayShifts.isEmpty()) {
                    for (ScheduleFragment.ShiftData shift : dayShifts) {
                        if (shift.shiftTime == null || shift.shiftTime.isEmpty()) continue;

                        cal.set(year, month - 1, day);
                        int weekday = cal.get(Calendar.DAY_OF_WEEK) - 2;
                        if (weekday < 0) weekday = 6;

                        String role = getRoleDisplay(shift.employee, employees);
                        String line = day + "." + month + "." + year + ";" +
                                weekDays[weekday] + ";" +
                                shift.employee + ";" +
                                role + ";" +
                                getDisplayText(shift.shiftTime) + "\n";
                        writer.append(line);
                    }
                }
            }

            writer.flush();
            writer.close();
            outputStream.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getRoleDisplay(String employeeName, List<ScheduleFragment.Employee> employees) {
        for (ScheduleFragment.Employee emp : employees) {
            if (emp.name.equals(employeeName)) {
                if (emp.role.equals("dm")) return "Директор";
                if (emp.role.equals("senior_seller")) return "Старший продавец";
                return "Продавец";
            }
        }
        return "Сотрудник";
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
}