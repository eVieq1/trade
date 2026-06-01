package com.example.salestracker.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.fragment.app.Fragment;
import com.example.salestracker.ApiClient;
import com.example.salestracker.R;

public class SalesFragment extends Fragment {

    private ApiClient apiClient;
    private String currentEmployee;
    private Spinner spinnerProducts;
    private Spinner spinnerModel;
    private EditText etAmount;
    private Button btnSell;
    private TextView tvEmployee;
    private TextView tvModelLabel;
    private CheckBox chkSampling;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sales, container, false);

        apiClient = new ApiClient();

        SharedPreferences prefs = requireActivity().getSharedPreferences("app", Context.MODE_PRIVATE);
        currentEmployee = prefs.getString("employee_name", "");

        tvEmployee = view.findViewById(R.id.tvEmployee);
        spinnerProducts = view.findViewById(R.id.spinnerProducts);
        spinnerModel = view.findViewById(R.id.spinnerModel);
        tvModelLabel = view.findViewById(R.id.tvModelLabel);
        etAmount = view.findViewById(R.id.etAmount);
        btnSell = view.findViewById(R.id.btnSell);
        chkSampling = view.findViewById(R.id.chkSampling);

        tvEmployee.setText("Сотрудник: " + currentEmployee);

        // Товары
        String[] products = {"Телефон", "СИМ", "Аксессуар", "ШПД", "Адаптер", "Финансовые услуги"};
        ArrayAdapter<String> productAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, products);
        spinnerProducts.setAdapter(productAdapter);

        // Модели телефонов
        String[] models = {"Samsung", "Realme", "Huawei", "Honor", "Infinix", "Techo", "Другие"};
        ArrayAdapter<String> modelAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, models);
        spinnerModel.setAdapter(modelAdapter);

        // По умолчанию скрываем модель и семплинг
        spinnerModel.setVisibility(View.GONE);
        tvModelLabel.setVisibility(View.GONE);
        chkSampling.setVisibility(View.GONE);

        // Обработка выбора товара
        spinnerProducts.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();

                // Семплинг только для СИМ
                if (selected.equals("СИМ")) {
                    chkSampling.setVisibility(View.VISIBLE);
                } else {
                    chkSampling.setVisibility(View.GONE);
                    chkSampling.setChecked(false);
                }

                // Модель телефона только для "Телефон"
                if (selected.equals("Телефон")) {
                    spinnerModel.setVisibility(View.VISIBLE);
                    tvModelLabel.setVisibility(View.VISIBLE);
                } else {
                    spinnerModel.setVisibility(View.GONE);
                    tvModelLabel.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                spinnerModel.setVisibility(View.GONE);
                tvModelLabel.setVisibility(View.GONE);
                chkSampling.setVisibility(View.GONE);
            }
        });

        btnSell.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString().trim();
            if (amountStr.isEmpty()) {
                Toast.makeText(getContext(), "Введите сумму", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Введите корректную сумму", Toast.LENGTH_SHORT).show();
                return;
            }

            String product = spinnerProducts.getSelectedItem().toString();
            boolean isSampling = chkSampling.isChecked();

            // Получаем модель телефона (только если выбран товар "Телефон")
            String phoneModel = "";
            if (product.equals("Телефон")) {
                phoneModel = spinnerModel.getSelectedItem().toString();
            }

            apiClient.addSale(currentEmployee, product, amount, isSampling, phoneModel, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "✅ Продажа добавлена!", Toast.LENGTH_SHORT).show();
                        etAmount.setText("");
                        chkSampling.setChecked(false);
                    });
                }

                @Override
                public void onError(String error) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "❌ Ошибка: " + error, Toast.LENGTH_SHORT).show());
                }
            });
        });

        return view;
    }
}