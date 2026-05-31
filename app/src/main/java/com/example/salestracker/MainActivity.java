package com.example.salestracker;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private ApiClient apiClient;
    private String currentEmployee;
    private Spinner spinnerProducts;
    private EditText etAmount;
    private Button btnSell;
    private TextView tvEmployee;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        apiClient = new ApiClient();

        // Для теста пока имя фиксированное
        currentEmployee = "Анна";

        tvEmployee = findViewById(R.id.tvEmployee);
        spinnerProducts = findViewById(R.id.spinnerProducts);
        etAmount = findViewById(R.id.etAmount);
        btnSell = findViewById(R.id.btnSell);

        tvEmployee.setText("Сотрудник: " + currentEmployee);

        String[] products = {"Телефон", "СИМ", "Аксессуар", "ШПД", "Адаптер", "Финансовые услуги"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, products);
        spinnerProducts.setAdapter(adapter);

        btnSell.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString().trim();
            if (amountStr.isEmpty()) {
                Toast.makeText(MainActivity.this, "Введите сумму", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                Toast.makeText(MainActivity.this, "Введите корректную сумму", Toast.LENGTH_SHORT).show();
                return;
            }

            String product = spinnerProducts.getSelectedItem().toString();

            apiClient.addSale(currentEmployee, product, amount, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "✅ Продажа добавлена!", Toast.LENGTH_SHORT).show();
                        etAmount.setText("");
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "❌ Ошибка: " + error, Toast.LENGTH_SHORT).show());
                }
            });
        });
    }
}