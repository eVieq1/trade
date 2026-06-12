package com.example.salestracker;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.salestracker.utils.NetworkUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class MonthlySalesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SaleAdapter adapter;
    private List<Sale> saleList = new ArrayList<>();
    private List<Employee> employees = new ArrayList<>();
    private ApiClient apiClient;
    private String currentUserRole;
    private int currentOfficeId;
    private String currentEmployee;

    private Spinner spinnerMonth, spinnerEmployee, spinnerCategory;
    private TextView tvTotal;

    private String[] months = {"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};
    private String[] categories = {"Все товары", "Телефон", "СИМ", "Аксессуар", "Адаптер", "ШПД", "Финансовые услуги"};

    private int selectedYear, selectedMonth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monthly_sales);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Продажи за месяц");

        apiClient = new ApiClient();

        SharedPreferences prefs = getSharedPreferences("app", Context.MODE_PRIVATE);
        currentUserRole = prefs.getString("user_role", "seller");
        currentOfficeId = prefs.getInt("office_id", 0);
        currentEmployee = prefs.getString("employee_name", "");

        initViews();
        setupSpinners();
        loadEmployees();
        loadSales();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewSales);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        tvTotal = findViewById(R.id.tvTotal);

        spinnerMonth = findViewById(R.id.spinnerMonth);
        spinnerEmployee = findViewById(R.id.spinnerEmployee);
        spinnerCategory = findViewById(R.id.spinnerCategory);
    }

    private void setupSpinners() {
        Calendar cal = Calendar.getInstance();
        selectedYear = cal.get(Calendar.YEAR);
        selectedMonth = cal.get(Calendar.MONTH);

        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, months);
        spinnerMonth.setAdapter(monthAdapter);
        spinnerMonth.setSelection(selectedMonth);

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCategory.setAdapter(categoryAdapter);

        spinnerMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedMonth = position;
                loadSales();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadSales();
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
                    employees.clear();
                    employees.add(new Employee(0, "Все сотрудники", ""));
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject emp = arr.getJSONObject(i);
                        int officeId = emp.optInt("office_id", 0);
                        if (currentOfficeId == 0 || officeId == currentOfficeId) {
                            employees.add(new Employee(emp.getInt("id"), emp.getString("name"), emp.getString("role")));
                        }
                    }
                    ArrayAdapter<Employee> employeeAdapter = new ArrayAdapter<>(MonthlySalesActivity.this,
                            android.R.layout.simple_spinner_dropdown_item, employees);
                    spinnerEmployee.setAdapter(employeeAdapter);

                    spinnerEmployee.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            loadSales();
                        }
                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MonthlySalesActivity.this, "Ошибка загрузки сотрудников", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadSales() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            NetworkUtils.showNoInternetMessage(this);
            return;
        }

        int employeeId = 0;
        if (spinnerEmployee.getSelectedItemPosition() > 0) {
            Employee selected = (Employee) spinnerEmployee.getSelectedItem();
            employeeId = selected.id;
        }

        String category = categories[spinnerCategory.getSelectedItemPosition()];
        String categoryParam = category.equals("Все товары") ? "" : category;

        final int finalEmployeeId = employeeId;
        final String finalCategoryParam = categoryParam;
        final int finalYear = selectedYear;
        final int finalMonth = selectedMonth + 1;
        final int finalOfficeId = currentOfficeId;

        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Загрузка...");
        progress.show();

        new Thread(() -> {
            try {
                String urlString = ApiClient.BASE_URL + "get_monthly_sales.php?year=" + finalYear +
                        "&month=" + finalMonth + "&office_id=" + finalOfficeId +
                        "&employee_id=" + finalEmployeeId + "&category=" + java.net.URLEncoder.encode(finalCategoryParam, "UTF-8");
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    Scanner s = new Scanner(conn.getInputStream(), "UTF-8").useDelimiter("\\A");
                    String response = s.hasNext() ? s.next() : "";
                    s.close();

                    runOnUiThread(() -> {
                        progress.dismiss();
                        try {
                            JSONObject obj = new JSONObject(response);
                            if (obj.getString("status").equals("success")) {
                                JSONArray salesArray = obj.getJSONArray("sales");
                                saleList.clear();
                                double total = 0;
                                for (int i = 0; i < salesArray.length(); i++) {
                                    JSONObject saleObj = salesArray.getJSONObject(i);
                                    Sale sale = new Sale();
                                    sale.id = saleObj.getInt("id");
                                    sale.employeeName = saleObj.getString("employee_name");
                                    sale.product = saleObj.getString("product");
                                    sale.amount = saleObj.getDouble("amount");
                                    sale.phoneModel = saleObj.optString("phone_model", "");
                                    sale.insurance = saleObj.optInt("insurance", 0);
                                    sale.saleDate = saleObj.getString("sale_date");
                                    sale.saleTime = saleObj.getString("sale_time");
                                    saleList.add(sale);
                                    total += sale.amount;
                                }
                                adapter = new SaleAdapter();
                                recyclerView.setAdapter(adapter);
                                tvTotal.setText(String.format("ИТОГО: %,.0f ₽", total));
                            }
                        } catch (Exception e) {
                            Toast.makeText(MonthlySalesActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        progress.dismiss();
                        Toast.makeText(MonthlySalesActivity.this, "Ошибка загрузки", Toast.LENGTH_SHORT).show();
                    });
                }
                conn.disconnect();
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(MonthlySalesActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void editSale(Sale sale, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Редактирование продажи");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 24);

        // Продавец
        TextView tvEmployeeLabel = new TextView(this);
        tvEmployeeLabel.setText("Продавец:");
        layout.addView(tvEmployeeLabel);

        Spinner spinnerEmployee = new Spinner(this);
        ArrayAdapter<Employee> employeeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, employees);
        spinnerEmployee.setAdapter(employeeAdapter);
        for (int i = 0; i < employees.size(); i++) {
            if (employees.get(i).name.equals(sale.employeeName)) {
                spinnerEmployee.setSelection(i);
                break;
            }
        }
        layout.addView(spinnerEmployee);

        // Товар
        TextView tvProductLabel = new TextView(this);
        tvProductLabel.setText("Товар:");
        tvProductLabel.setPadding(0, 16, 0, 8);
        layout.addView(tvProductLabel);

        Spinner spinnerProduct = new Spinner(this);
        ArrayAdapter<String> productAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerProduct.setAdapter(productAdapter);
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equals(sale.product)) {
                spinnerProduct.setSelection(i);
                break;
            }
        }
        layout.addView(spinnerProduct);

        // Модель (только для телефона)
        LinearLayout modelLayout = new LinearLayout(this);
        modelLayout.setOrientation(LinearLayout.VERTICAL);
        modelLayout.setVisibility(View.GONE);

        TextView tvModelLabel = new TextView(this);
        tvModelLabel.setText("Модель:");
        modelLayout.addView(tvModelLabel);

        Spinner spinnerModel = new Spinner(this);
        String[] models = {"Samsung", "Realme", "Huawei", "Honor", "Infinix", "Techo", "Другие"};
        ArrayAdapter<String> modelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, models);
        spinnerModel.setAdapter(modelAdapter);
        if (sale.phoneModel != null && !sale.phoneModel.isEmpty()) {
            for (int i = 0; i < models.length; i++) {
                if (models[i].equals(sale.phoneModel)) {
                    spinnerModel.setSelection(i);
                    break;
                }
            }
        }
        modelLayout.addView(spinnerModel);
        layout.addView(modelLayout);

        // Сумма
        TextView tvAmountLabel = new TextView(this);
        tvAmountLabel.setText("Сумма:");
        tvAmountLabel.setPadding(0, 16, 0, 8);
        layout.addView(tvAmountLabel);

        EditText etAmount = new EditText(this);
        etAmount.setText(String.valueOf((int) sale.amount));
        etAmount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(etAmount);

        // Дата
        TextView tvDateLabel = new TextView(this);
        tvDateLabel.setText("Дата:");
        tvDateLabel.setPadding(0, 16, 0, 8);
        layout.addView(tvDateLabel);

        EditText etDate = new EditText(this);
        etDate.setText(sale.saleDate);
        etDate.setHint("ГГГГ-ММ-ДД");
        layout.addView(etDate);

        // Время
        TextView tvTimeLabel = new TextView(this);
        tvTimeLabel.setText("Время:");
        tvTimeLabel.setPadding(0, 16, 0, 8);
        layout.addView(tvTimeLabel);

        EditText etTime = new EditText(this);
        etTime.setText(sale.saleTime);
        etTime.setHint("ЧЧ:ММ:СС");
        layout.addView(etTime);

        // Страховка (только для финуслуг)
        LinearLayout insuranceLayout = new LinearLayout(this);
        insuranceLayout.setOrientation(LinearLayout.VERTICAL);
        insuranceLayout.setVisibility(sale.product.equals("Финансовые услуги") ? View.VISIBLE : View.GONE);

        TextView tvInsuranceLabel = new TextView(this);
        tvInsuranceLabel.setText("Страховка:");
        insuranceLayout.addView(tvInsuranceLabel);

        EditText etInsurance = new EditText(this);
        etInsurance.setText(String.valueOf(sale.insurance));
        etInsurance.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        insuranceLayout.addView(etInsurance);
        layout.addView(insuranceLayout);

        spinnerProduct.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = categories[position];
                modelLayout.setVisibility(selected.equals("Телефон") ? View.VISIBLE : View.GONE);
                insuranceLayout.setVisibility(selected.equals("Финансовые услуги") ? View.VISIBLE : View.GONE);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        builder.setView(layout);
        builder.setPositiveButton("СОХРАНИТЬ", (dialog, which) -> {
            Employee selectedEmployee = (Employee) spinnerEmployee.getSelectedItem();
            String product = categories[spinnerProduct.getSelectedItemPosition()];
            String amountStr = etAmount.getText().toString();
            String date = etDate.getText().toString();
            String time = etTime.getText().toString();
            String model = spinnerModel.getSelectedItem().toString();
            String insuranceStr = etInsurance.getText().toString();

            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Введите сумму", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(amountStr);
            int insurance = 0;
            if (!insuranceStr.isEmpty()) {
                insurance = Integer.parseInt(insuranceStr);
            }

            updateSale(sale.id, selectedEmployee.name, product, amount, model, date, time, insurance, position);
        });
        builder.setNeutralButton("УДАЛИТЬ", (dialog, which) -> deleteSale(sale.id, position));
        builder.setNegativeButton("ОТМЕНА", null);
        builder.show();
    }

    private void updateSale(int id, String employee, String product, double amount, String phoneModel, String date, String time, int insurance, int position) {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Сохранение...");
        progress.show();

        new Thread(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("id", id);
                json.put("employee", employee);
                json.put("product", product);
                json.put("amount", amount);
                json.put("phone_model", phoneModel);
                json.put("sale_date", date);
                json.put("sale_time", time);
                json.put("insurance", insurance);

                URL url = new URL(ApiClient.BASE_URL + "update_sale_full.php");
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
                        Toast.makeText(MonthlySalesActivity.this, "Сохранено", Toast.LENGTH_SHORT).show();
                        loadSales();
                    } else {
                        Toast.makeText(MonthlySalesActivity.this, "Ошибка сохранения", Toast.LENGTH_SHORT).show();
                    }
                });
                conn.disconnect();
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(MonthlySalesActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void deleteSale(int id, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Удалить продажу")
                .setMessage("Вы уверены?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    ProgressDialog progress = new ProgressDialog(this);
                    progress.setMessage("Удаление...");
                    progress.show();

                    new Thread(() -> {
                        try {
                            JSONObject json = new JSONObject();
                            json.put("id", id);

                            URL url = new URL(ApiClient.BASE_URL + "delete_sale.php");
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
                                    Toast.makeText(MonthlySalesActivity.this, "Удалено", Toast.LENGTH_SHORT).show();
                                    loadSales();
                                } else {
                                    Toast.makeText(MonthlySalesActivity.this, "Ошибка удаления", Toast.LENGTH_SHORT).show();
                                }
                            });
                            conn.disconnect();
                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                progress.dismiss();
                                Toast.makeText(MonthlySalesActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    }).start();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private class SaleAdapter extends RecyclerView.Adapter<SaleAdapter.ViewHolder> {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_monthly_sale, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Sale sale = saleList.get(position);
            holder.tvEmployee.setText(sale.employeeName);
            String productDisplay = sale.product;
            if (!sale.phoneModel.isEmpty() && sale.product.equals("Телефон")) {
                productDisplay = sale.product + " (" + sale.phoneModel + ")";
            }
            if (sale.insurance > 0 && sale.product.equals("Финансовые услуги")) {
                productDisplay = sale.product + " + страховка " + sale.insurance + " ₽";
            }
            holder.tvProduct.setText(productDisplay);
            holder.tvAmount.setText(String.format("%,.0f ₽", sale.amount));
            holder.tvDate.setText(sale.saleDate + " " + sale.saleTime.substring(0, 5));

            boolean canEdit = currentUserRole.equals("owner") || currentUserRole.equals("rgo") || currentUserRole.equals("dm");
            if (!canEdit && !sale.employeeName.equals(currentEmployee)) {
                holder.itemView.setEnabled(false);
                holder.itemView.setAlpha(0.6f);
            } else {
                holder.itemView.setEnabled(true);
                holder.itemView.setAlpha(1f);
                holder.itemView.setOnClickListener(v -> editSale(sale, position));
            }
        }

        @Override
        public int getItemCount() { return saleList.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvEmployee, tvProduct, tvDate, tvAmount;
            ViewHolder(View itemView) {
                super(itemView);
                tvEmployee = itemView.findViewById(R.id.tvEmployee);
                tvProduct = itemView.findViewById(R.id.tvProduct);
                tvDate = itemView.findViewById(R.id.tvDate);
                tvAmount = itemView.findViewById(R.id.tvAmount);
            }
        }
    }

    static class Sale {
        int id;
        String employeeName;
        String product;
        double amount;
        String phoneModel;
        int insurance;
        String saleDate;
        String saleTime;
    }

    static class Employee {
        int id;
        String name, role;
        Employee(int id, String name, String role) {
            this.id = id;
            this.name = name;
            this.role = role;
        }
        @Override
        public String toString() { return name; }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}