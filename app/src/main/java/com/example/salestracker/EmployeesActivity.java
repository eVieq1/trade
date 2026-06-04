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
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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

        setupSwipeToDelete();

        boolean canAdd = currentUserRole.equals("owner") || currentUserRole.equals("rgo") || currentUserRole.equals("dm");
        if (canAdd) {
            binding.btnAddEmployee.setVisibility(View.VISIBLE);
            binding.btnAddEmployee.setOnClickListener(v -> showAddEmployeeDialog());
        } else {
            binding.btnAddEmployee.setVisibility(View.GONE);
        }

        if (NetworkUtils.isNetworkAvailable(this)) {
            loadShops();
            loadEmployees();
        } else {
            NetworkUtils.showNoInternetMessage(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (currentUserRole.equals("owner") || currentUserRole.equals("rgo")) {
            menu.add(0, 1, 0, "Офисы");
            menu.add(0, 2, 1, "Выход");
        } else {
            menu.add(0, 2, 0, "Выход");
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == 1) {
            startActivity(new Intent(EmployeesActivity.this, ShopsActivity.class));
            return true;
        } else if (id == 2) {
            finishAffinity();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupSwipeToDelete() {
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

                // ИСПРАВЛЕННАЯ ПРОВЕРКА ПРАВ НА УДАЛЕНИЕ
                boolean canDelete = false;
                if (currentUserRole.equals("owner") || currentUserRole.equals("rgo")) {
                    canDelete = true;
                } else if (currentUserRole.equals("dm")) {
                    // DM может удалять только сотрудников своего офиса
                    if (employee.officeId == currentUserOfficeId &&
                            !employee.role.equals("owner") &&
                            !employee.role.equals("rgo")) {
                        canDelete = true;
                    }
                }

                if (!canDelete) {
                    Toast.makeText(EmployeesActivity.this, "У вас нет прав на удаление этого сотрудника", Toast.LENGTH_SHORT).show();
                    adapter.notifyItemChanged(position);
                    return;
                }

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
                    adapter.notifyDataSetChanged();
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

                        // ИСПРАВЛЕННАЯ ФИЛЬТРАЦИЯ ДЛЯ DM
                        if (currentUserRole.equals("dm")) {
                            // DM видит сотрудников своего офиса И непривязанных (officeId == 0)
                            if (officeId == currentUserOfficeId || officeId == 0) {
                                employeeList.add(new Employee(id, name, role, officeId));
                            }
                        } else {
                            // Для owner и rgo показываем всех
                            employeeList.add(new Employee(id, name, role, officeId));
                        }
                    }

                    adapter.notifyDataSetChanged();

                    if (employeeList.isEmpty()) {
                        Toast.makeText(EmployeesActivity.this, "Список сотрудников пуст", Toast.LENGTH_SHORT).show();
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

    private void showAddEmployeeDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_employee, null);

        EditText etName = view.findViewById(R.id.etName);
        Spinner spinnerRole = view.findViewById(R.id.spinnerRole);

        String[] roleOptions = {"Специалист", "Старший специалист"};
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, roleOptions);
        spinnerRole.setAdapter(roleAdapter);

        new AlertDialog.Builder(this)
                .setTitle("Добавить сотрудника")
                .setView(view)
                .setPositiveButton("Добавить", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Введите имя", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int roleIndex = spinnerRole.getSelectedItemPosition();
                    String role = (roleIndex == 0) ? "seller" : "senior_seller";
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
                json.put("office_id", 0);

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
                                loadShops();
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
                        loadShops();
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
                        loadShops();
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

    private void showEmployeeActionsDialog(Employee employee, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        List<String> actions = new ArrayList<>();
        List<Runnable> runnables = new ArrayList<>();

        // Смена офиса (для owner, rgo и dm для сотрудников своего офиса)
        if (currentUserRole.equals("owner") || currentUserRole.equals("rgo")) {
            actions.add("Сменить офис");
            runnables.add(() -> showOfficeDialog(employee, position));
        } else if (currentUserRole.equals("dm") && employee.officeId == currentUserOfficeId) {
            actions.add("Сменить офис");
            runnables.add(() -> showOfficeDialog(employee, position));
        }

        // Смена роли
        boolean canChangeRole = false;
        if (currentUserRole.equals("owner") || currentUserRole.equals("rgo")) {
            canChangeRole = true;
        } else if (currentUserRole.equals("dm")) {
            if (employee.officeId == currentUserOfficeId &&
                    (employee.role.equals("seller") || employee.role.equals("senior_seller"))) {
                canChangeRole = true;
            }
        }

        if (canChangeRole) {
            actions.add("Изменить роль");
            runnables.add(() -> showRoleDialog(employee, position));
        }

        if (actions.isEmpty()) {
            Toast.makeText(this, "Нет доступных действий", Toast.LENGTH_SHORT).show();
            return;
        }

        builder.setTitle(employee.name)
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

        int currentSelection = 0;
        for (int i = 0; i < shopList.size(); i++) {
            if (shopList.get(i).id == employee.officeId) {
                currentSelection = i + 1;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Сменить офис: " + employee.name)
                .setSingleChoiceItems(officeNames, currentSelection, (dialog, which) -> {
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

    private void showRoleDialog(Employee employee, int position) {
        List<String> roleOptions = new ArrayList<>();
        List<String> roleValues = new ArrayList<>();

        if (currentUserRole.equals("owner") || currentUserRole.equals("rgo")) {
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

            holder.itemView.setOnClickListener(v -> showEmployeeActionsDialog(emp, position));
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

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

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

    // ==================== DATA CLASSES ====================

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