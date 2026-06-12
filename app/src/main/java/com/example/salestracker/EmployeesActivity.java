package com.example.salestracker;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.salestracker.databinding.ActivityEmployeesBinding;
import com.example.salestracker.utils.NetworkUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class EmployeesActivity extends AppCompatActivity {

    private ActivityEmployeesBinding binding;
    private EmployeeAdapter adapter;
    private List<Employee> employeeList = new ArrayList<>();
    private List<Shop> shopList = new ArrayList<>();
    private ApiClient apiClient;
    private String currentUserRole;
    private int currentUserOfficeId = 0;
    private boolean canEdit;
    private boolean isGlobalAdmin;
    private int selectedOfficeId = 0;
    private String currentOfficeName;

    private final String[] roles = {"owner", "rgo", "dm", "senior_seller", "seller", "bot"};
    private final String[] roleDisplayNames = {"Владелец", "РГО", "Директор", "Старший специалист", "Специалист", "Бот"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityEmployeesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Сотрудники");

        apiClient = new ApiClient();

        binding.recyclerViewEmployees.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EmployeeAdapter();
        binding.recyclerViewEmployees.setAdapter(adapter);

        SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
        currentUserRole = prefs.getString("user_role", "seller");
        currentUserOfficeId = prefs.getInt("office_id", 0);
        currentOfficeName = prefs.getString("office_name", "Мой офис");
        selectedOfficeId = currentUserOfficeId;

        canEdit = currentUserRole.equals("owner") || currentUserRole.equals("rgo") || currentUserRole.equals("dm");
        isGlobalAdmin = currentUserRole.equals("owner") || currentUserRole.equals("rgo");

        setupUI();
        loadShops();
        setupSwipeToDelete();
    }

    private void setupSwipeToDelete() {
        // Только для глобальных администраторов (owner/rgo)
        if (isGlobalAdmin) {
            ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                @Override
                public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                    return false;
                }

                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                    int position = viewHolder.getAdapterPosition();
                    if (position < 0 || position >= employeeList.size()) {
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    Employee employee = employeeList.get(position);

                    new AlertDialog.Builder(EmployeesActivity.this)
                            .setTitle("Удалить сотрудника")
                            .setMessage("Вы уверены, что хотите удалить " + employee.name + "?")
                            .setPositiveButton("Удалить", (dialog, which) -> deleteEmployee(employee.id, position))
                            .setNegativeButton("Отмена", (dialog, which) -> adapter.notifyItemChanged(position))
                            .show();
                }
            };
            new ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recyclerViewEmployees);
        }
    }

    private void setupUI() {
        if (isGlobalAdmin) {
            // Для РГО и Владельца: показываем спиннер выбора офиса
            binding.officeHeader.setVisibility(View.VISIBLE);
            binding.tvEmployeesCount.setVisibility(View.VISIBLE);
            binding.btnAddEmployeeQuick.setVisibility(View.GONE);
            binding.btnAddOfficeQuick.setVisibility(View.GONE);
            binding.btnAddEmployee.setVisibility(View.GONE);
        } else if (currentUserRole.equals("dm")) {
            binding.officeHeader.setVisibility(View.VISIBLE);
            binding.tvEmployeesCount.setVisibility(View.VISIBLE);
            binding.spinnerOffice.setVisibility(View.GONE);
            binding.btnAddEmployeeQuick.setVisibility(View.GONE);
            binding.btnAddOfficeQuick.setVisibility(View.GONE);

            binding.btnAddEmployee.setVisibility(View.VISIBLE);
            binding.btnAddEmployee.setOnClickListener(v -> showAddEmployeeDialog());

            TextView tvOfficeTitle = new TextView(this);
            tvOfficeTitle.setText("🏢 " + currentOfficeName);
            tvOfficeTitle.setTextSize(16);
            tvOfficeTitle.setTextColor(0xFF333333);
            tvOfficeTitle.setPadding(0, 0, 16, 0);
            ((LinearLayout) binding.officeHeader).addView(tvOfficeTitle, 1);
        } else {
            binding.officeHeader.setVisibility(View.GONE);
            binding.tvEmployeesCount.setVisibility(View.GONE);
            binding.btnAddEmployee.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isGlobalAdmin) {
            // Сначала ОФИСЫ, потом СОТРУДНИКИ
            menu.add(0, 4, 0, "🏢 Офисы");
            menu.add(0, 1, 1, "➕ Добавить сотрудника");
            menu.add(0, 2, 2, "➕ Добавить офис");
            menu.add(0, 3, 3, "🚪 Выход");
        } else if (currentUserRole.equals("dm")) {
            menu.add(0, 1, 0, "➕ Добавить сотрудника");
            menu.add(0, 3, 1, "🚪 Выход");
        } else {
            menu.add(0, 3, 0, "🚪 Выход");
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == 4 && isGlobalAdmin) {
            startActivity(new Intent(EmployeesActivity.this, ShopsActivity.class));
            return true;
        } else if (id == 1 && isGlobalAdmin) {
            showAddEmployeeDialog();
            return true;
        } else if (id == 2 && isGlobalAdmin) {
            showAddOfficeDialog();
            return true;
        } else if (id == 1 && currentUserRole.equals("dm")) {
            showAddEmployeeDialog();
            return true;
        } else if (id == 3) {
            finishAffinity();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadShops() {
        apiClient.getShops(new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    JSONArray arr = obj.getJSONArray("shops");
                    shopList.clear();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject shop = arr.getJSONObject(i);
                        shopList.add(new Shop(shop.getInt("id"), shop.getString("name")));
                    }

                    if (isGlobalAdmin && !shopList.isEmpty()) {
                        setupOfficeSpinner();
                    }

                    loadEmployees();
                } catch (Exception e) {
                    Log.e("EmployeesActivity", "Ошибка загрузки офисов: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                Log.e("EmployeesActivity", "Ошибка: " + error);
                Toast.makeText(EmployeesActivity.this, "Ошибка загрузки офисов", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupOfficeSpinner() {
        List<String> officeNames = new ArrayList<>();
        List<Integer> officeIds = new ArrayList<>();

        officeNames.add("Все офисы");
        officeIds.add(0);

        for (Shop shop : shopList) {
            officeNames.add(shop.name);
            officeIds.add(shop.id);
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, officeNames);
        binding.spinnerOffice.setAdapter(spinnerAdapter);

        for (int i = 0; i < officeIds.size(); i++) {
            if (officeIds.get(i) == currentUserOfficeId) {
                binding.spinnerOffice.setSelection(i);
                break;
            }
        }

        binding.spinnerOffice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedOfficeId = officeIds.get(position);
                loadEmployees();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadEmployees() {
        apiClient.getEmployees(new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    JSONArray arr = obj.getJSONArray("employees");
                    employeeList.clear();

                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject emp = arr.getJSONObject(i);
                        int officeId = emp.optInt("office_id", 0);
                        String role = emp.getString("role");
                        String name = emp.getString("name");
                        int id = emp.getInt("id");

                        if (isGlobalAdmin && selectedOfficeId > 0 && officeId != selectedOfficeId) {
                            continue;
                        }
                        if (currentUserRole.equals("dm") && officeId != currentUserOfficeId && officeId != 0) {
                            continue;
                        }

                        employeeList.add(new Employee(id, name, role, officeId));
                    }

                    adapter.notifyDataSetChanged();

                    if (binding.tvEmployeesCount.getVisibility() == View.VISIBLE) {
                        binding.tvEmployeesCount.setText("Всего сотрудников: " + employeeList.size());
                    }

                } catch (Exception e) {
                    Log.e("EmployeesActivity", "Ошибка парсинга: " + e.getMessage());
                    Toast.makeText(EmployeesActivity.this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                Log.e("EmployeesActivity", "Ошибка: " + error);
                Toast.makeText(EmployeesActivity.this, "Ошибка: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddOfficeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 24);

        EditText etName = new EditText(this);
        etName.setHint("Название офиса");
        etName.setPadding(16, 16, 16, 16);
        layout.addView(etName);

        EditText etAddress = new EditText(this);
        etAddress.setHint("Адрес");
        etAddress.setPadding(16, 16, 16, 16);
        layout.addView(etAddress);

        builder.setTitle("Добавить офис")
                .setView(layout)
                .setPositiveButton("Добавить", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String address = etAddress.getText().toString().trim();
                    if (!name.isEmpty()) {
                        addShop(name, address);
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void addShop(String name, String address) {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Добавление...");
        progress.show();

        new Thread(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("name", name);
                json.put("address", address);

                URL url = new URL(ApiClient.BASE_URL + "add_shop.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                runOnUiThread(() -> {
                    progress.dismiss();
                    if (responseCode == 200) {
                        Toast.makeText(EmployeesActivity.this, "Офис добавлен", Toast.LENGTH_SHORT).show();
                        loadShops();
                    } else {
                        Toast.makeText(EmployeesActivity.this, "Ошибка сервера: " + responseCode, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(EmployeesActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void showAddEmployeeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 24);

        EditText etName = new EditText(this);
        etName.setHint("Имя сотрудника");
        etName.setPadding(16, 16, 16, 16);
        layout.addView(etName);

        TextView tvRoleLabel = new TextView(this);
        tvRoleLabel.setText("Роль:");
        tvRoleLabel.setPadding(0, 16, 0, 8);
        layout.addView(tvRoleLabel);

        Spinner spinnerRole = new Spinner(this);

        String[] roleOptions;
        if (isGlobalAdmin) {
            roleOptions = roleDisplayNames;
        } else {
            roleOptions = new String[]{"Специалист", "Старший специалист"};
        }

        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, roleOptions);
        spinnerRole.setAdapter(roleAdapter);
        layout.addView(spinnerRole);

        builder.setTitle("Добавить сотрудника")
                .setView(layout)
                .setPositiveButton("Добавить", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Введите имя", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int roleIndex = spinnerRole.getSelectedItemPosition();
                    String role;
                    if (isGlobalAdmin) {
                        role = roles[roleIndex];
                    } else {
                        role = (roleIndex == 0) ? "seller" : "senior_seller";
                    }
                    createNewEmployee(name, role);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void createNewEmployee(String name, String role) {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Создание...");
        progress.show();

        new Thread(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("name", name);
                json.put("role", role);
                json.put("office_id", selectedOfficeId > 0 ? selectedOfficeId : currentUserOfficeId);

                URL url = new URL(ApiClient.BASE_URL + "add_employee.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();

                Scanner s = new Scanner(conn.getInputStream(), "UTF-8").useDelimiter("\\A");
                String response = s.hasNext() ? s.next() : "";
                s.close();

                runOnUiThread(() -> {
                    progress.dismiss();
                    if (responseCode == 200) {
                        try {
                            JSONObject obj = new JSONObject(response);
                            if (obj.getString("status").equals("success")) {
                                Toast.makeText(EmployeesActivity.this, "Сотрудник создан", Toast.LENGTH_SHORT).show();
                                loadEmployees();
                            } else {
                                Toast.makeText(EmployeesActivity.this, "Ошибка: " + obj.optString("message"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(EmployeesActivity.this, "Ошибка сервера", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(EmployeesActivity.this, "Ошибка сервера: " + responseCode, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(EmployeesActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void deleteEmployee(int id, int position) {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Удаление...");
        progress.show();

        new Thread(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("id", id);

                URL url = new URL(ApiClient.BASE_URL + "delete_employee.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                runOnUiThread(() -> {
                    progress.dismiss();
                    if (responseCode == 200) {
                        Toast.makeText(EmployeesActivity.this, "Сотрудник удалён", Toast.LENGTH_SHORT).show();
                        loadEmployees();
                    } else {
                        Toast.makeText(EmployeesActivity.this, "Ошибка сервера: " + responseCode, Toast.LENGTH_SHORT).show();
                        adapter.notifyItemChanged(position);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(EmployeesActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    adapter.notifyItemChanged(position);
                });
            }
        }).start();
    }

    private void showEmployeeActionsDialog(Employee employee, int position) {
        if (!canEdit) {
            Toast.makeText(this, "Нет прав на редактирование", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> actions = new ArrayList<>();
        List<Runnable> runnables = new ArrayList<>();

        actions.add("Сменить офис");
        runnables.add(() -> showOfficeDialog(employee, position));

        actions.add("Изменить роль");
        runnables.add(() -> showRoleDialog(employee, position));

        new AlertDialog.Builder(this)
                .setTitle(employee.name)
                .setItems(actions.toArray(new String[0]), (dialog, which) -> runnables.get(which).run())
                .show();
    }

    private void showOfficeDialog(Employee employee, int position) {
        if (shopList.isEmpty()) {
            Toast.makeText(this, "Список офисов пуст", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] officeNames = new String[shopList.size() + 1];
        officeNames[0] = "Не привязан";
        for (int i = 0; i < shopList.size(); i++) {
            officeNames[i + 1] = shopList.get(i).name;
        }

        new AlertDialog.Builder(this)
                .setTitle("Сменить офис: " + employee.name)
                .setSingleChoiceItems(officeNames, 0, (dialog, which) -> {
                    int newOfficeId = 0;
                    if (which > 0) {
                        newOfficeId = shopList.get(which - 1).id;
                    }
                    changeEmployeeOffice(employee.id, newOfficeId, position);
                    dialog.dismiss();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void changeEmployeeOffice(int employeeId, int newOfficeId, int position) {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Сохранение...");
        progress.show();

        new Thread(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("employee_id", employeeId);
                json.put("office_id", newOfficeId);

                URL url = new URL(ApiClient.BASE_URL + "update_employee_office.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                runOnUiThread(() -> {
                    progress.dismiss();
                    if (responseCode == 200) {
                        Toast.makeText(EmployeesActivity.this, "Офис изменён", Toast.LENGTH_SHORT).show();
                        loadEmployees();
                    } else {
                        Toast.makeText(EmployeesActivity.this, "Ошибка сервера: " + responseCode, Toast.LENGTH_SHORT).show();
                        adapter.notifyItemChanged(position);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(EmployeesActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    adapter.notifyItemChanged(position);
                });
            }
        }).start();
    }

    private void showRoleDialog(Employee employee, int position) {
        List<String> roleOptions = new ArrayList<>();
        List<String> roleValues = new ArrayList<>();

        if (isGlobalAdmin) {
            for (int i = 0; i < roles.length; i++) {
                if (!roles[i].equals("bot") || currentUserRole.equals("owner")) {
                    roleOptions.add(roleDisplayNames[i]);
                    roleValues.add(roles[i]);
                }
            }
        } else if (currentUserRole.equals("dm")) {
            roleOptions.add("Специалист");
            roleValues.add("seller");
            roleOptions.add("Старший специалист");
            roleValues.add("senior_seller");
        }

        int currentSelection = 0;
        for (int i = 0; i < roleValues.size(); i++) {
            if (roleValues.get(i).equals(employee.role)) {
                currentSelection = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Изменить роль: " + employee.name)
                .setSingleChoiceItems(roleOptions.toArray(new String[0]), currentSelection, (dialog, which) -> {
                    String newRole = roleValues.get(which);
                    if (!newRole.equals(employee.role)) {
                        changeEmployeeRole(employee.id, newRole, position);
                    }
                    dialog.dismiss();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void changeEmployeeRole(int employeeId, String newRole, int position) {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Сохранение...");
        progress.show();

        new Thread(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("employee_id", employeeId);
                json.put("role", newRole);

                URL url = new URL(ApiClient.BASE_URL + "update_employee_role.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                runOnUiThread(() -> {
                    progress.dismiss();
                    if (responseCode == 200) {
                        Toast.makeText(EmployeesActivity.this, "Роль изменена", Toast.LENGTH_SHORT).show();
                        loadEmployees();
                    } else {
                        Toast.makeText(EmployeesActivity.this, "Ошибка сервера: " + responseCode, Toast.LENGTH_SHORT).show();
                        adapter.notifyItemChanged(position);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(EmployeesActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    adapter.notifyItemChanged(position);
                });
            }
        }).start();
    }

    private void openEmployeeProfile(Employee employee) {
        Intent intent = new Intent(EmployeesActivity.this, EmployeeDetailActivity.class);
        intent.putExtra("employee_id", employee.id);
        intent.putExtra("employee_name", employee.name);
        intent.putExtra("employee_role", getRoleDisplayName(employee.role));
        intent.putExtra("employee_office", getOfficeName(employee.officeId));
        startActivity(intent);
    }

    private String getOfficeName(int officeId) {
        for (Shop shop : shopList) {
            if (shop.id == officeId) {
                return shop.name;
            }
        }
        return "";
    }

    private String getRoleDisplayName(String role) {
        for (int i = 0; i < roles.length; i++) {
            if (roles[i].equals(role)) return roleDisplayNames[i];
        }
        return role;
    }

    // ==================== ADAPTER ====================

    private class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_employee, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Employee emp = employeeList.get(position);
            holder.tvName.setText(emp.name);
            holder.tvRole.setText(getRoleDisplayName(emp.role));

            String officeName = getOfficeName(emp.officeId);
            holder.tvOffice.setText(officeName.isEmpty() ? "Не привязан" : officeName);

            holder.itemView.setOnClickListener(v -> openEmployeeProfile(emp));

            if (canEdit) {
                holder.itemView.setOnLongClickListener(v -> {
                    showEmployeeActionsDialog(emp, position);
                    return true;
                });
            }
        }

        @Override
        public int getItemCount() { return employeeList.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvRole, tvOffice;
            ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvEmployeeName);
                tvRole = itemView.findViewById(R.id.tvEmployeeRole);
                tvOffice = itemView.findViewById(R.id.tvEmployeeOffice);
            }
        }
    }

    static class Employee {
        int id;
        String name, role;
        int officeId;
        Employee(int id, String name, String role, int officeId) {
            this.id = id;
            this.name = name;
            this.role = role;
            this.officeId = officeId;
        }
    }

    static class Shop {
        int id;
        String name;
        Shop(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (apiClient != null) {
            apiClient.shutdown();
        }
        binding = null;
    }
}