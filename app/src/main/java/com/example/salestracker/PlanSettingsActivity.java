package com.example.salestracker;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.salestracker.R;

public class PlanSettingsActivity extends AppCompatActivity {

    private Spinner spinnerMonth;
    private EditText etSim, etSampling, etGoodsRevenue, etFinanceRevenue, etInternet, etAccessories;
    private EditText etSamsung, etRealme, etHuawei, etHonor, etInfinix, etTecho;
    private Button btnSave;
    private ApiClient apiClient;
    private int currentYear = 2026;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan_settings);

        apiClient = new ApiClient();

        spinnerMonth = findViewById(R.id.spinnerMonth);
        etSim = findViewById(R.id.etSim);
        etSampling = findViewById(R.id.etSampling);
        etGoodsRevenue = findViewById(R.id.etGoodsRevenue);
        etFinanceRevenue = findViewById(R.id.etFinanceRevenue);
        etInternet = findViewById(R.id.etInternet);
        etAccessories = findViewById(R.id.etAccessories);
        etSamsung = findViewById(R.id.etSamsung);
        etRealme = findViewById(R.id.etRealme);
        etHuawei = findViewById(R.id.etHuawei);
        etHonor = findViewById(R.id.etHonor);
        etInfinix = findViewById(R.id.etInfinix);
        etTecho = findViewById(R.id.etTecho);
        btnSave = findViewById(R.id.btnSavePlans);

        // Месяцы
        String[] months = {"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
                "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, months);
        spinnerMonth.setAdapter(adapter);

        btnSave.setOnClickListener(v -> saveAllPlans());
    }

    private void saveAllPlans() {
        int month = spinnerMonth.getSelectedItemPosition() + 1;

        savePlan("sim", getDouble(etSim), "qty");
        savePlan("sampling", getDouble(etSampling), "qty");
        savePlan("goods_revenue", getDouble(etGoodsRevenue), "money");
        savePlan("finance_revenue", getDouble(etFinanceRevenue), "money");
        savePlan("internet", getDouble(etInternet), "qty");
        savePlan("accessories", getDouble(etAccessories), "money");
        savePlan("samsung", getDouble(etSamsung), "money");
        savePlan("realme", getDouble(etRealme), "money");
        savePlan("huawei", getDouble(etHuawei), "money");
        savePlan("honor", getDouble(etHonor), "money");
        savePlan("infinix", getDouble(etInfinix), "money");
        savePlan("techo", getDouble(etTecho), "money");

        Toast.makeText(this, "✅ Планы сохранены", Toast.LENGTH_SHORT).show();
        finish();
    }

    private double getDouble(EditText et) {
        String text = et.getText().toString().trim();
        if (text.isEmpty()) return 0;
        return Double.parseDouble(text);
    }

    private void savePlan(String category, double target, String unitType) {
        apiClient.savePlan(currentYear, spinnerMonth.getSelectedItemPosition() + 1, category, target, unitType, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                // Успешно
            }
            @Override
            public void onError(String error) {
                Toast.makeText(PlanSettingsActivity.this, "Ошибка: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}