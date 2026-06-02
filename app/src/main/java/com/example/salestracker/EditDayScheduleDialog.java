package com.example.salestracker;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EditDayScheduleDialog extends Dialog {

    private Context context;
    private int year, month, day;
    private ApiClient apiClient;
    private List<EmployeeShift> shifts = new ArrayList<>();
    private RecyclerView recyclerView;
    private ShiftAdapter adapter;
    private List<Employee> employees = new ArrayList<>();

    public EditDayScheduleDialog(@NonNull Context context, int year, int month, int day) {
        super(context);
        this.context = context;
        this.year = year;
        this.month = month;
        this.day = day;
        apiClient = new ApiClient();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_edit_day_schedule);

        setTitle("Смены на " + day + "." + month + "." + year);

        recyclerView = findViewById(R.id.recyclerViewShifts);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        Button btnAddShift = findViewById(R.id.btnAddShift);
        btnAddShift.setOnClickListener(v -> loadEmployeesAndShowAddDialog());

        loadShifts();
        loadEmployees();
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
                Toast.makeText(context, "Ошибка загрузки сотрудников", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadShifts() {
        apiClient.getSchedule(year, month, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    JSONObject schedule = obj.getJSONObject("schedule");
                    shifts.clear();

                    String key = String.valueOf(day);
                    if (schedule.has(key)) {
                        JSONArray dayArray = schedule.getJSONArray(key);
                        for (int i = 0; i < dayArray.length(); i++) {
                            JSONObject data = dayArray.getJSONObject(i);
                            EmployeeShift shift = new EmployeeShift();
                            shift.employee = data.getString("employee");
                            shift.shiftTime = data.getString("shift_time");
                            shifts.add(shift);
                        }
                    }

                    if (adapter == null) {
                        adapter = new ShiftAdapter();
                        recyclerView.setAdapter(adapter);
                    } else {
                        adapter.notifyDataSetChanged();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(context, "Ошибка загрузки: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadEmployeesAndShowAddDialog() {
        if (employees.isEmpty()) {
            Toast.makeText(context, "Список сотрудников пуст", Toast.LENGTH_SHORT).show();
            return;
        }
        showAddEmployeeDialog();
    }

    private void showAddEmployeeDialog() {
        String[] employeeNames = new String[employees.size()];
        for (int i = 0; i < employees.size(); i++) {
            employeeNames[i] = employees.get(i).name;
        }
        String[] times = {"09:00-18:00", "10:00-19:00", "12:00-21:00", "Выходной", "Отпуск", "Больничный", "Другой офис"};

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_shift, null);
        Spinner spinnerEmployee = view.findViewById(R.id.spinnerEmployee);
        Spinner spinnerTime = view.findViewById(R.id.spinnerTime);

        spinnerEmployee.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, employeeNames));
        spinnerTime.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, times));

        builder.setTitle("Добавить сотрудника")
                .setView(view)
                .setPositiveButton("Добавить", (dialog, which) -> {
                    String employee = spinnerEmployee.getSelectedItem().toString();
                    String shiftTime = spinnerTime.getSelectedItem().toString();
                    saveShift(employee, shiftTime);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void saveShift(String employee, String shiftTime) {
        apiClient.saveSchedule(year, month, day, employee, shiftTime, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                Toast.makeText(context, "Сохранено", Toast.LENGTH_SHORT).show();
                loadShifts();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(context, "Ошибка: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteShift(String employee, int position) {
        apiClient.saveSchedule(year, month, day, employee, "", new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                shifts.remove(position);
                adapter.notifyItemRemoved(position);
                Toast.makeText(context, "Удалено", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(context, "Ошибка: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void editShift(String employee, String oldShiftTime, int position) {
        String[] times = {"09:00-18:00", "10:00-19:00", "12:00-21:00", "Выходной", "Отпуск", "Больничный", "Другой офис"};

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_shift, null);
        Spinner spinnerTime = view.findViewById(R.id.spinnerTime);
        spinnerTime.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, times));

        view.findViewById(R.id.spinnerEmployee).setVisibility(View.GONE);

        for (int i = 0; i < times.length; i++) {
            if (times[i].equals(oldShiftTime)) {
                spinnerTime.setSelection(i);
                break;
            }
        }

        builder.setTitle("Редактировать смену: " + employee)
                .setView(view)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String newTime = spinnerTime.getSelectedItem().toString();
                    apiClient.saveSchedule(year, month, day, employee, newTime, new ApiClient.ApiCallback() {
                        @Override
                        public void onSuccess(String response) {
                            shifts.get(position).shiftTime = newTime;
                            adapter.notifyItemChanged(position);
                            Toast.makeText(context, "Сохранено", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(context, "Ошибка: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private class ShiftAdapter extends RecyclerView.Adapter<ShiftAdapter.ViewHolder> {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_shift_edit, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            EmployeeShift shift = shifts.get(position);
            holder.tvEmployee.setText(shift.employee);
            holder.tvShiftTime.setText(shift.shiftTime);

            holder.btnDelete.setOnClickListener(v -> deleteShift(shift.employee, position));
            holder.btnEdit.setOnClickListener(v -> editShift(shift.employee, shift.shiftTime, position));
        }

        @Override
        public int getItemCount() { return shifts.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvEmployee, tvShiftTime;
            ImageButton btnEdit, btnDelete;
            ViewHolder(View itemView) {
                super(itemView);
                tvEmployee = itemView.findViewById(R.id.tvEmployee);
                tvShiftTime = itemView.findViewById(R.id.tvShiftTime);
                btnEdit = itemView.findViewById(R.id.btnEdit);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }
        }
    }

    static class EmployeeShift {
        String employee, shiftTime;
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