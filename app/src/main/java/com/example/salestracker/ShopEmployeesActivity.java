package com.example.salestracker;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ShopEmployeesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EmployeeAdapter adapter;
    private List<Employee> employeeList = new ArrayList<>();
    private List<Employee> allEmployees = new ArrayList<>();
    private List<Shop> shopList = new ArrayList<>();
    private String currentUserRole;
    private int shopId;
    private String shopName;
    private ApiClient apiClient;

    private final String[] roles = {"owner", "rgo", "dm", "senior_seller", "seller"};
    private final String[] roleDisplayNames = {"Владелец", "РГО", "Директор", "Старший специалист", "Специалист"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop_employees);

        shopId = getIntent().getIntExtra("shop_id", 0);
        shopName = getIntent().getStringExtra("shop_name");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(shopName);

        apiClient = new ApiClient();

        recyclerView = findViewById(R.id.recyclerViewEmployees);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EmployeeAdapter();
        recyclerView.setAdapter(adapter);

        SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
        currentUserRole = prefs.getString("user_role", "seller");

        boolean canEdit = currentUserRole.equals("owner") || currentUserRole.equals("rgo");
        if (canEdit) {
            findViewById(R.id.btnAddEmployee).setVisibility(View.VISIBLE);
            findViewById(R.id.btnAddEmployee).setOnClickListener(v -> showAddEmployeeDialog());
            setupSwipeToDelete();
        } else {
            findViewById(R.id.btnAddEmployee).setVisibility(View.GONE);
        }

        loadShops();
        loadAllEmployees();
        loadEmployees();
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
                Employee employee = employeeList.get(position);

                new AlertDialog.Builder(ShopEmployeesActivity.this)
                        .setTitle("Удалить сотрудника из офиса")
                        .setMessage("Вы уверены, что хотите удалить " + employee.name + " из офиса " + shopName + "?")
                        .setPositiveButton("Удалить", (dialog, which) -> removeEmployeeFromOffice(employee.id, position))
                        .setNegativeButton("Отмена", (dialog, which) -> adapter.notifyItemChanged(position))
                        .show();
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
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
                } catch (Exception e) {
                    Log.e("ShopEmployeesActivity", "Ошибка загрузки офисов: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                Log.e("ShopEmployeesActivity", "Ошибка: " + error);
            }
        });
    }

    private void loadAllEmployees() {
        Log.d("ShopEmployeesActivity", "=== loadAllEmployees вызван ===");

        apiClient.getEmployees(new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                Log.d("ShopEmployeesActivity", "getEmployees (all) ответ: " + response);
                try {
                    JSONObject obj = new JSONObject(response);
                    JSONArray arr = obj.getJSONArray("employees");
                    allEmployees.clear();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject emp = arr.getJSONObject(i);
                        allEmployees.add(new Employee(
                                emp.getInt("id"),
                                emp.getString("name"),
                                emp.getString("role"),
                                emp.optInt("office_id", 0)
                        ));
                    }
                    Log.d("ShopEmployeesActivity", "Всего сотрудников: " + allEmployees.size());
                } catch (Exception e) {
                    Log.e("ShopEmployeesActivity", "Ошибка парсинга allEmployees: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                Log.e("ShopEmployeesActivity", "Ошибка загрузки allEmployees: " + error);
            }
        });
    }

    private void loadEmployees() {
        Log.d("ShopEmployeesActivity", "=== loadEmployees вызван ===");
        Log.d("ShopEmployeesActivity", "Загружаем сотрудников для офиса ID: " + shopId);

        employeeList.clear();
        adapter.notifyDataSetChanged();

        final ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Загрузка сотрудников...");
        progress.show();

        apiClient.getEmployees(new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                Log.d("ShopEmployeesActivity", "getEmployees ответ: " + response);

                runOnUiThread(() -> progress.dismiss());

                try {
                    JSONObject obj = new JSONObject(response);
                    String status = obj.getString("status");

                    if (status.equals("success")) {
                        JSONArray arr = obj.getJSONArray("employees");

                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject emp = arr.getJSONObject(i);
                            int officeId = emp.optInt("office_id", 0);

                            if (officeId == shopId) {
                                employeeList.add(new Employee(
                                        emp.getInt("id"),
                                        emp.getString("name"),
                                        emp.getString("role"),
                                        officeId
                                ));
                            }
                        }

                        Log.d("ShopEmployeesActivity", "Найдено сотрудников в этом офисе: " + employeeList.size());
                        runOnUiThread(() -> {
                            adapter.notifyDataSetChanged();
                            if (employeeList.isEmpty()) {
                                Toast.makeText(ShopEmployeesActivity.this, "В этом офисе нет сотрудников", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e("ShopEmployeesActivity", "Ошибка парсинга: " + e.getMessage());
                    runOnUiThread(() -> Toast.makeText(ShopEmployeesActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onError(String error) {
                Log.e("ShopEmployeesActivity", "Ошибка загрузки: " + error);
                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(ShopEmployeesActivity.this, "Ошибка: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void removeEmployeeFromOffice(int employeeId, int position) {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Удаление...");
        progress.show();

        new Thread(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("employee_id", employeeId);
                json.put("office_id", 0);

                URL url = new URL(ApiClient.BASE_URL + "update_employee_office.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                runOnUiThread(() -> {
                    progress.dismiss();
                    if (responseCode == 200) {
                        Toast.makeText(ShopEmployeesActivity.this, "Сотрудник удалён из офиса", Toast.LENGTH_SHORT).show();
                        loadEmployees();
                        loadAllEmployees();
                    } else {
                        Toast.makeText(ShopEmployeesActivity.this, "Ошибка сервера: " + responseCode, Toast.LENGTH_SHORT).show();
                        adapter.notifyItemChanged(position);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(ShopEmployeesActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                runOnUiThread(() -> {
                    progress.dismiss();
                    if (responseCode == 200) {
                        Toast.makeText(ShopEmployeesActivity.this, "Офис изменён", Toast.LENGTH_SHORT).show();
                        loadEmployees();
                        loadAllEmployees();
                    } else {
                        Toast.makeText(ShopEmployeesActivity.this, "Ошибка сервера: " + responseCode, Toast.LENGTH_SHORT).show();
                        adapter.notifyItemChanged(position);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(ShopEmployeesActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                runOnUiThread(() -> {
                    progress.dismiss();
                    if (responseCode == 200) {
                        Toast.makeText(ShopEmployeesActivity.this, "Роль изменена", Toast.LENGTH_SHORT).show();
                        loadEmployees();
                        loadAllEmployees();
                    } else {
                        Toast.makeText(ShopEmployeesActivity.this, "Ошибка сервера: " + responseCode, Toast.LENGTH_SHORT).show();
                        adapter.notifyItemChanged(position);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(ShopEmployeesActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    adapter.notifyItemChanged(position);
                });
            }
        }).start();
    }

    private void showEmployeeActionsDialog(Employee employee, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        List<String> actions = new ArrayList<>();
        List<Runnable> runnables = new ArrayList<>();

        // Смена офиса (для владельца и РГО)
        if (currentUserRole.equals("owner") || currentUserRole.equals("rgo")) {
            actions.add("Сменить офис");
            runnables.add(() -> showOfficeDialog(employee, position));
        }

        // Смена роли (для владельца и РГО)
        if (currentUserRole.equals("owner") || currentUserRole.equals("rgo")) {
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

        new AlertDialog.Builder(this)
                .setTitle("Сменить офис: " + employee.name)
                .setItems(officeNames, (dialog, which) -> {
                    int newOfficeId = 0;
                    if (which > 0) {
                        newOfficeId = shopList.get(which - 1).id;
                    }
                    changeEmployeeOffice(employee.id, newOfficeId, position);
                })
                .show();
    }

    private void showRoleDialog(Employee employee, int position) {
        List<String> roleOptions = new ArrayList<>();
        List<String> roleValues = new ArrayList<>();

        // Доступные роли для смены
        roleOptions.add("Специалист");
        roleValues.add("seller");
        roleOptions.add("Старший специалист");
        roleValues.add("senior_seller");
        roleOptions.add("Директор");
        roleValues.add("dm");

        new AlertDialog.Builder(this)
                .setTitle("Изменить роль: " + employee.name)
                .setItems(roleOptions.toArray(new String[0]), (dialog, which) -> {
                    String newRole = roleValues.get(which);
                    if (!newRole.equals(employee.role)) {
                        changeEmployeeRole(employee.id, newRole, position);
                    }
                })
                .show();
    }

    private void moveEmployeeToShop(int employeeId) {
        Log.d("ShopEmployeesActivity", "=== moveEmployeeToShop вызван ===");
        Log.d("ShopEmployeesActivity", "employeeId: " + employeeId);
        Log.d("ShopEmployeesActivity", "shopId: " + shopId);

        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Перемещение...");
        progress.show();

        new Thread(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("employee_id", employeeId);
                json.put("office_id", shopId);

                String jsonString = json.toString();
                Log.d("ShopEmployeesActivity", "Отправляем JSON: " + jsonString);

                URL url = new URL(ApiClient.BASE_URL + "update_employee_office.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                OutputStream os = conn.getOutputStream();
                os.write(jsonString.getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                Log.d("ShopEmployeesActivity", "Response code: " + responseCode);

                Scanner s = new Scanner(conn.getInputStream(), "UTF-8").useDelimiter("\\A");
                String response = s.hasNext() ? s.next() : "";
                Log.d("ShopEmployeesActivity", "Сервер ответил: " + response);

                runOnUiThread(() -> {
                    progress.dismiss();
                    if (responseCode == 200) {
                        try {
                            JSONObject obj = new JSONObject(response);
                            String status = obj.getString("status");
                            if (status.equals("success")) {
                                Toast.makeText(ShopEmployeesActivity.this, "Сотрудник добавлен в офис", Toast.LENGTH_SHORT).show();
                                loadAllEmployees();
                                loadEmployees();
                            } else {
                                Toast.makeText(ShopEmployeesActivity.this, "Ошибка: " + status, Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e("ShopEmployeesActivity", "Ошибка парсинга: " + e.getMessage());
                            Toast.makeText(ShopEmployeesActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(ShopEmployeesActivity.this, "Ошибка сервера: " + responseCode, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                Log.e("ShopEmployeesActivity", "Исключение: " + e.getMessage());
                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(ShopEmployeesActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void showAddEmployeeDialog() {
        String[] options = {"Выбрать существующего сотрудника", "Создать нового сотрудника"};

        new AlertDialog.Builder(this)
                .setTitle("Добавить сотрудника в " + shopName)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showSelectEmployeeDialog();
                    } else {
                        showCreateNewEmployeeDialog();
                    }
                })
                .show();
    }

    private void showSelectEmployeeDialog() {
        List<Employee> availableEmployees = new ArrayList<>();
        for (Employee emp : allEmployees) {
            if (emp.officeId != shopId) {
                availableEmployees.add(emp);
            }
        }

        if (availableEmployees.isEmpty()) {
            Toast.makeText(this, "Нет доступных сотрудников для добавления", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] employeeNames = new String[availableEmployees.size()];
        for (int i = 0; i < availableEmployees.size(); i++) {
            employeeNames[i] = availableEmployees.get(i).name + " (" + getRoleDisplayName(availableEmployees.get(i).role) + ")";
        }

        new AlertDialog.Builder(this)
                .setTitle("Выберите сотрудника")
                .setItems(employeeNames, (dialog, which) -> {
                    Employee selected = availableEmployees.get(which);
                    moveEmployeeToShop(selected.id);
                })
                .show();
    }

    private void showCreateNewEmployeeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        EditText etName = new EditText(this);
        etName.setHint("Имя сотрудника");
        etName.setPadding(16, 16, 16, 16);
        layout.addView(etName);

        Spinner spinnerRole = new Spinner(this);
        String[] roleOptions = {"Специалист", "Старший специалист", "Директор"};
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, roleOptions);
        spinnerRole.setAdapter(roleAdapter);
        layout.addView(spinnerRole);

        builder.setTitle("Создать нового сотрудника")
                .setView(layout)
                .setPositiveButton("Создать", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(ShopEmployeesActivity.this, "Введите имя", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int roleIndex = spinnerRole.getSelectedItemPosition();
                    String role = "";
                    if (roleIndex == 0) role = "seller";
                    else if (roleIndex == 1) role = "senior_seller";
                    else role = "dm";
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
                json.put("office_id", shopId);

                URL url = new URL(ApiClient.BASE_URL + "add_employee.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                runOnUiThread(() -> {
                    progress.dismiss();
                    if (responseCode == 200) {
                        Toast.makeText(ShopEmployeesActivity.this, "Сотрудник создан и добавлен в офис", Toast.LENGTH_SHORT).show();
                        loadAllEmployees();
                        loadEmployees();
                    } else {
                        Toast.makeText(ShopEmployeesActivity.this, "Ошибка сервера: " + responseCode, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(ShopEmployeesActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shop_employee, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Employee emp = employeeList.get(position);
            holder.tvName.setText(emp.name);
            holder.tvRole.setText(getRoleDisplayName(emp.role));

            holder.itemView.setOnClickListener(v -> showEmployeeActionsDialog(emp, position));
        }

        @Override
        public int getItemCount() { return employeeList.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvRole;
            ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvEmployeeName);
                tvRole = itemView.findViewById(R.id.tvEmployeeRole);
            }
        }
    }

    private String getRoleDisplayName(String role) {
        for (int i = 0; i < roles.length; i++) {
            if (roles[i].equals(role)) return roleDisplayNames[i];
        }
        return role;
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
}