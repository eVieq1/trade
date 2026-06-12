package com.example.salestracker;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.gridlayout.widget.GridLayout;  // ← ИСПРАВЛЕНО

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class PlanSettingsActivity extends AppCompatActivity {

    private GridLayout gridPlans;
    private ApiClient apiClient;
    private int currentYear;
    private int currentMonth;
    private String currentUserRole;
    private int currentOfficeId;
    private String currentOfficeName;

    // Постоянные показатели
    private String[] constantCategories = {
            "SIM",
            "Товарная выручка",
            "Аксессуары",
            "ШПД",
            "Адаптеры",
            "Финансовые услуги",
            "Телефоны (общий)"
    };

    // Модели телефонов
    private String[] phoneModels = {
            "Samsung",
            "Realme",
            "Huawei",
            "Tecno",
            "Redmi",
            "Infinix",
            "Honor"
    };

    private Map<String, Integer> plans = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Планы продаж");

        gridPlans = findViewById(R.id.gridPlans);

        SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
        currentUserRole = prefs.getString("user_role", "seller");

        // Получаем office_id из Intent (если пришёл из ShopEmployeesActivity)
        int intentOfficeId = getIntent().getIntExtra("office_id", 0);
        String intentOfficeName = getIntent().getStringExtra("office_name");

        if (intentOfficeId > 0) {
            currentOfficeId = intentOfficeId;
            currentOfficeName = intentOfficeName;
        } else {
            currentOfficeId = prefs.getInt("office_id", 0);
            currentOfficeName = prefs.getString("office_name", "Мой офис");
        }

        if (!currentUserRole.equals("owner") && !currentUserRole.equals("rgo") && !currentUserRole.equals("dm")) {
            Toast.makeText(this, "Нет прав для редактирования планов", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        getSupportActionBar().setSubtitle("Офис: " + currentOfficeName);

        java.util.Calendar cal = java.util.Calendar.getInstance();
        currentYear = cal.get(java.util.Calendar.YEAR);
        currentMonth = cal.get(java.util.Calendar.MONTH) + 1;

        loadPlans();
    }

    private void loadPlans() {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Загрузка планов...");
        progress.show();

        apiClient = new ApiClient();
        apiClient.getPlans(currentYear, currentMonth, currentOfficeId, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                progress.dismiss();
                Log.d("PlanSettings", "Ответ: " + response);
                try {
                    JSONObject obj = new JSONObject(response);
                    if (obj.getString("status").equals("success")) {
                        JSONObject plansObj = obj.getJSONObject("plans");
                        plans.clear();

                        for (String category : constantCategories) {
                            int value = plansObj.optInt(category, 0);
                            plans.put(category, value);
                        }
                        for (String model : phoneModels) {
                            int value = plansObj.optInt(model, 0);
                            plans.put(model, value);
                        }

                        setupGrid();
                    } else {
                        Toast.makeText(PlanSettingsActivity.this, "Ошибка загрузки", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e("PlanSettings", "Ошибка: " + e.getMessage());
                    Toast.makeText(PlanSettingsActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                progress.dismiss();
                Log.e("PlanSettings", "Ошибка: " + error);
                Toast.makeText(PlanSettingsActivity.this, "Ошибка: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getUnitForCategory(String category) {
        switch (category) {
            case "SIM": return "шт";
            case "Товарная выручка": return "₽";
            case "Аксессуары": return "₽";
            case "ШПД": return "шт";
            case "Адаптеры": return "шт";
            case "Финансовые услуги": return "₽";
            case "Телефоны (общий)": return "₽";
            default: return "₽";
        }
    }

    private void setupGrid() {
        gridPlans.removeAllViews();
        gridPlans.setColumnCount(2);

        for (int i = 0; i < constantCategories.length; i++) {
            final String category = constantCategories[i];
            final int target = plans.getOrDefault(category, 0);
            final String unit = getUnitForCategory(category);

            View card = LayoutInflater.from(this).inflate(R.layout.item_plan_card, null);

            TextView tvTitle = card.findViewById(R.id.tvPlanTitle);
            TextView tvValue = card.findViewById(R.id.tvPlanValue);
            Button btnEdit = card.findViewById(R.id.btnEditPlan);

            tvTitle.setText(category);
            tvValue.setText(formatValue(target, unit));

            btnEdit.setOnClickListener(v -> showEditDialog(category, target, unit));

            addCardToGrid(card);
        }

        for (int i = 0; i < phoneModels.length; i++) {
            final String model = phoneModels[i];
            final int target = plans.getOrDefault(model, 0);
            final String unit = "₽";

            View card = LayoutInflater.from(this).inflate(R.layout.item_plan_card, null);

            TextView tvTitle = card.findViewById(R.id.tvPlanTitle);
            TextView tvValue = card.findViewById(R.id.tvPlanValue);
            Button btnEdit = card.findViewById(R.id.btnEditPlan);

            tvTitle.setText(model);
            tvValue.setText(formatValue(target, unit));

            if (target == 0) {
                tvValue.setTextColor(0xFF999999);
                btnEdit.setText("АКТИВИРОВАТЬ");
                btnEdit.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFF9800));
            } else {
                tvValue.setTextColor(0xFF2196F3);
                btnEdit.setText("ИЗМЕНИТЬ");
                btnEdit.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF2196F3));
            }

            btnEdit.setOnClickListener(v -> {
                if (target == 0) {
                    showActivateDialog(model, unit);
                } else {
                    showEditDialog(model, target, unit);
                }
            });

            addCardToGrid(card);
        }
    }

    private String formatValue(int value, String unit) {
        return String.format("%,d %s", value, unit).replace(",", " ");
    }

    private void addCardToGrid(View card) {
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(8, 8, 8, 8);
        card.setLayoutParams(params);
        gridPlans.addView(card);
    }

    private void showActivateDialog(String category, String unit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Активация плана");
        builder.setMessage("Введите план для " + category + " (в " + unit + "):");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 24);

        EditText etPlan = new EditText(this);
        etPlan.setHint("Сумма в " + unit);
        etPlan.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(etPlan);

        builder.setView(layout);
        builder.setPositiveButton("АКТИВИРОВАТЬ", (dialog, which) -> {
            String planStr = etPlan.getText().toString();
            if (!planStr.isEmpty()) {
                int newPlan = Integer.parseInt(planStr);
                savePlan(category, newPlan);
            }
        });
        builder.setNegativeButton("ОТМЕНА", null);
        builder.show();
    }

    private void showEditDialog(String category, int currentValue, String unit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("План по " + category);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 24);

        TextView tvCurrent = new TextView(this);
        tvCurrent.setText("Текущий план: " + formatValue(currentValue, unit));
        tvCurrent.setPadding(0, 0, 0, 16);
        layout.addView(tvCurrent);

        EditText etNewPlan = new EditText(this);
        etNewPlan.setHint("Новый план");
        etNewPlan.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(etNewPlan);

        builder.setView(layout);
        builder.setPositiveButton("СОХРАНИТЬ", (dialog, which) -> {
            String newPlanStr = etNewPlan.getText().toString();
            if (!newPlanStr.isEmpty()) {
                int newPlan = Integer.parseInt(newPlanStr);
                if (newPlan == 0 && !isConstantCategory(category)) {
                    confirmDeactivate(category);
                } else {
                    savePlan(category, newPlan);
                }
            }
        });
        builder.setNegativeButton("ОТМЕНА", null);
        builder.show();
    }

    private boolean isConstantCategory(String category) {
        for (String c : constantCategories) {
            if (c.equals(category)) return true;
        }
        return false;
    }

    private void confirmDeactivate(String category) {
        new AlertDialog.Builder(this)
                .setTitle("Деактивация")
                .setMessage("Вы уверены, что хотите деактивировать план для " + category + "?")
                .setPositiveButton("ДА", (dialog, which) -> savePlan(category, 0))
                .setNegativeButton("НЕТ", null)
                .show();
    }

    private void savePlan(String category, int newPlan) {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Сохранение...");
        progress.show();

        new Thread(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("year", currentYear);
                json.put("month", currentMonth);
                json.put("category", category);
                json.put("target", newPlan);
                json.put("unit_type", getUnitForCategory(category));
                json.put("office_id", currentOfficeId);

                Log.d("PlanSettings", "Отправляем: " + json.toString());

                URL url = new URL(ApiClient.BASE_URL + "save_plan.php");
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

                Log.d("PlanSettings", "Ответ сервера: " + response);
                Log.d("PlanSettings", "Response code: " + responseCode);

                runOnUiThread(() -> {
                    progress.dismiss();
                    if (responseCode == 200) {
                        try {
                            JSONObject obj = new JSONObject(response);
                            if (obj.getString("status").equals("success")) {
                                plans.put(category, newPlan);
                                setupGrid();
                                Toast.makeText(PlanSettingsActivity.this, "План сохранён", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(PlanSettingsActivity.this, "Ошибка: " + obj.optString("message"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(PlanSettingsActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(PlanSettingsActivity.this, "Ошибка сервера: " + responseCode, Toast.LENGTH_SHORT).show();
                    }
                });
                conn.disconnect();
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(PlanSettingsActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}