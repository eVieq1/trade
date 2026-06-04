package com.example.salestracker.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.salestracker.ApiClient;
import com.example.salestracker.R;
import com.example.salestracker.utils.NetworkUtils;

public class SalesFragment extends Fragment {

    private TextView tvEmployee;
    private CardView cardPhone, cardSim, cardAccessory, cardAdapter, cardInternet, cardFinance;
    private ApiClient apiClient;
    private String currentEmployee;
    private String currentUserRole;
    private int currentOfficeId;

    private final String[] phoneModels = {"Samsung", "Realme", "Huawei", "Honor", "Infinix", "Techo", "Другие"};

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sales, container, false);

        initViews(view);
        loadUserData();
        setupClickListeners();

        return view;
    }

    private void initViews(View view) {
        tvEmployee = view.findViewById(R.id.tvEmployee);
        cardPhone = view.findViewById(R.id.cardPhone);
        cardSim = view.findViewById(R.id.cardSim);
        cardAccessory = view.findViewById(R.id.cardAccessory);
        cardAdapter = view.findViewById(R.id.cardAdapter);
        cardInternet = view.findViewById(R.id.cardInternet);
        cardFinance = view.findViewById(R.id.cardFinance);
    }

    private void loadUserData() {
        apiClient = new ApiClient();
        SharedPreferences prefs = requireActivity().getSharedPreferences("app", Context.MODE_PRIVATE);
        currentEmployee = prefs.getString("employee_name", "");
        currentUserRole = prefs.getString("user_role", "seller");
        currentOfficeId = prefs.getInt("office_id", 0);
        tvEmployee.setText("Сотрудник: " + currentEmployee);
    }

    private void setupClickListeners() {
        cardPhone.setOnClickListener(v -> showPhoneDialog());
        cardSim.setOnClickListener(v -> showSimDialog());
        cardAccessory.setOnClickListener(v -> showAccessoryDialog());
        cardAdapter.setOnClickListener(v -> showAdapterDialog());
        cardInternet.setOnClickListener(v -> showInternetDialog());
        cardFinance.setOnClickListener(v -> showFinanceDialog());
    }

    // ==================== ТЕЛЕФОН ====================
    private void showPhoneDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("ПРОДАЖА ТЕЛЕФОНА");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 24);

        TextView tvModel = new TextView(getContext());
        tvModel.setText("Модель:");
        layout.addView(tvModel);

        Spinner spinnerModel = new Spinner(getContext());
        ArrayAdapter<String> modelAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, phoneModels);
        spinnerModel.setAdapter(modelAdapter);
        layout.addView(spinnerModel);

        TextView tvAmount = new TextView(getContext());
        tvAmount.setText("Сумма:");
        tvAmount.setPadding(0, 32, 0, 8);
        layout.addView(tvAmount);

        EditText etAmount = new EditText(getContext());
        etAmount.setHint("Введите сумму");
        etAmount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(etAmount);

        builder.setView(layout);
        builder.setPositiveButton("ПРОДАТЬ", (dialog, which) -> {
            String model = spinnerModel.getSelectedItem().toString();
            String amountStr = etAmount.getText().toString();
            if (amountStr.isEmpty()) {
                Toast.makeText(getContext(), "Введите сумму", Toast.LENGTH_SHORT).show();
                return;
            }
            double amount = Double.parseDouble(amountStr);
            addSale("Телефон", amount, model, 0, 0);
        });
        builder.setNegativeButton("ОТМЕНА", null);
        builder.show();
    }

    // ==================== СИМ ====================
    private void showSimDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("ПРОДАЖА СИМ");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 24);

        final int[] selectedIndex = {0};

        RadioGroup radioGroup = new RadioGroup(getContext());
        radioGroup.setOrientation(LinearLayout.VERTICAL);

        String[] simTypes = {"Обычная СИМ", "Семплинг", "Всё для семьи"};
        for (int i = 0; i < simTypes.length; i++) {
            RadioButton rb = new RadioButton(getContext());
            rb.setText(simTypes[i]);
            rb.setId(i);
            radioGroup.addView(rb);
        }
        radioGroup.check(0);
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> selectedIndex[0] = checkedId);

        layout.addView(radioGroup);

        builder.setView(layout);
        builder.setPositiveButton("ПРОДАТЬ", (dialog, which) -> {
            addSale("СИМ", 0, "", selectedIndex[0], 0);
        });
        builder.setNegativeButton("ОТМЕНА", null);
        builder.show();
    }

    // ==================== АКСЕССУАР ====================
    private void showAccessoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("ПРОДАЖА АКСЕССУАРА");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 24);

        TextView tvAmount = new TextView(getContext());
        tvAmount.setText("Сумма:");
        layout.addView(tvAmount);

        EditText etAmount = new EditText(getContext());
        etAmount.setHint("Введите сумму");
        etAmount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(etAmount);

        builder.setView(layout);
        builder.setPositiveButton("ПРОДАТЬ", (dialog, which) -> {
            String amountStr = etAmount.getText().toString();
            if (amountStr.isEmpty()) {
                Toast.makeText(getContext(), "Введите сумму", Toast.LENGTH_SHORT).show();
                return;
            }
            double amount = Double.parseDouble(amountStr);
            addSale("Аксессуар", amount, "", 0, 0);
        });
        builder.setNegativeButton("ОТМЕНА", null);
        builder.show();
    }

    // ==================== АДАПТЕР ====================
    private void showAdapterDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("ПРОДАЖА АДАПТЕРА")
                .setMessage("Продать адаптер?")
                .setPositiveButton("ПРОДАТЬ", (dialog, which) -> addSale("Адаптер", 0, "", 0, 0))
                .setNegativeButton("ОТМЕНА", null)
                .show();
    }

    // ==================== ШПД ====================
    private void showInternetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("ПРОДАЖА ШПД");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 24);

        final int[] selectedIndex = {0};

        RadioGroup radioGroup = new RadioGroup(getContext());
        radioGroup.setOrientation(LinearLayout.VERTICAL);

        String[] internetTypes = {"Заявка", "Подключено"};
        for (int i = 0; i < internetTypes.length; i++) {
            RadioButton rb = new RadioButton(getContext());
            rb.setText(internetTypes[i]);
            rb.setId(i);
            radioGroup.addView(rb);
        }
        radioGroup.check(0);
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> selectedIndex[0] = checkedId);

        layout.addView(radioGroup);

        builder.setView(layout);
        builder.setPositiveButton("ПРОДАТЬ", (dialog, which) -> {
            addSale("ШПД", 0, "", 0, selectedIndex[0]);
        });
        builder.setNegativeButton("ОТМЕНА", null);
        builder.show();
    }

    // ==================== ФИНАНСОВЫЕ УСЛУГИ (4 плашки 2×2) ====================
    private void showFinanceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("ФИНАНСОВЫЕ УСЛУГИ");

        LinearLayout mainLayout = new LinearLayout(getContext());
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(48, 32, 48, 24);

        final int[] selectedAmount = {299};
        final int[] selectedInsurance = {0};

        // GridLayout 2×2
        GridLayout gridLayout = new GridLayout(getContext());
        gridLayout.setColumnCount(2);
        gridLayout.setRowCount(2);

        // Плашка 299 ₽
        Button btn299 = new Button(getContext());
        btn299.setText("299 ₽");
        btn299.setTextSize(14);
        btn299.setPadding(16, 24, 16, 24);
        btn299.setOnClickListener(v -> {
            selectedAmount[0] = 299;
            Toast.makeText(getContext(), "Выбрано: 299 ₽", Toast.LENGTH_SHORT).show();
        });
        GridLayout.LayoutParams params299 = new GridLayout.LayoutParams();
        params299.width = 0;
        params299.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params299.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params299.setMargins(8, 8, 8, 8);
        btn299.setLayoutParams(params299);
        gridLayout.addView(btn299);

        // Плашка 499 ₽
        Button btn499 = new Button(getContext());
        btn499.setText("499 ₽");
        btn499.setTextSize(14);
        btn499.setPadding(16, 24, 16, 24);
        btn499.setOnClickListener(v -> {
            selectedAmount[0] = 499;
            Toast.makeText(getContext(), "Выбрано: 499 ₽", Toast.LENGTH_SHORT).show();
        });
        GridLayout.LayoutParams params499 = new GridLayout.LayoutParams();
        params499.width = 0;
        params499.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params499.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params499.setMargins(8, 8, 8, 8);
        btn499.setLayoutParams(params499);
        gridLayout.addView(btn499);

        // Плашка 1290 ₽
        Button btn1290 = new Button(getContext());
        btn1290.setText("1290 ₽");
        btn1290.setTextSize(14);
        btn1290.setPadding(16, 24, 16, 24);
        btn1290.setOnClickListener(v -> {
            selectedAmount[0] = 1290;
            Toast.makeText(getContext(), "Выбрано: 1290 ₽", Toast.LENGTH_SHORT).show();
        });
        GridLayout.LayoutParams params1290 = new GridLayout.LayoutParams();
        params1290.width = 0;
        params1290.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params1290.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params1290.setMargins(8, 8, 8, 8);
        btn1290.setLayoutParams(params1290);
        gridLayout.addView(btn1290);

        // Плашка СТРАХОВКА
        Button btnInsurance = new Button(getContext());
        btnInsurance.setText("СТРАХОВКА");
        btnInsurance.setTextSize(14);
        btnInsurance.setPadding(16, 24, 16, 24);
        btnInsurance.setOnClickListener(v -> {
            AlertDialog.Builder insuranceBuilder = new AlertDialog.Builder(getContext());
            insuranceBuilder.setTitle("Введите сумму страховки");

            LinearLayout insuranceLayout = new LinearLayout(getContext());
            insuranceLayout.setOrientation(LinearLayout.VERTICAL);
            insuranceLayout.setPadding(48, 32, 48, 24);

            EditText etInsurance = new EditText(getContext());
            etInsurance.setHint("Введите сумму");
            etInsurance.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            insuranceLayout.addView(etInsurance);

            insuranceBuilder.setView(insuranceLayout);
            insuranceBuilder.setPositiveButton("СОХРАНИТЬ", (dialog, which) -> {
                String insStr = etInsurance.getText().toString();
                if (!insStr.isEmpty()) {
                    selectedInsurance[0] = Integer.parseInt(insStr);
                    Toast.makeText(getContext(), "Страховка: " + selectedInsurance[0] + " ₽", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Сумма не введена", Toast.LENGTH_SHORT).show();
                }
            });
            insuranceBuilder.setNegativeButton("ОТМЕНА", null);
            insuranceBuilder.show();
        });
        GridLayout.LayoutParams paramsInsurance = new GridLayout.LayoutParams();
        paramsInsurance.width = 0;
        paramsInsurance.height = GridLayout.LayoutParams.WRAP_CONTENT;
        paramsInsurance.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        paramsInsurance.setMargins(8, 8, 8, 8);
        btnInsurance.setLayoutParams(paramsInsurance);
        gridLayout.addView(btnInsurance);

        mainLayout.addView(gridLayout);

        builder.setView(mainLayout);
        builder.setPositiveButton("ПРОДАТЬ", (dialog, which) -> {
            addSale("Финансовые услуги", selectedAmount[0], "", 0, selectedInsurance[0]);
        });
        builder.setNegativeButton("ОТМЕНА", null);
        builder.show();
    }

    // ==================== ОБЩИЙ МЕТОД ПРОДАЖИ ====================
    private void addSale(String product, double amount, String phoneModel, int simType, int insurance) {
        if (!NetworkUtils.isNetworkAvailable(getContext())) {
            NetworkUtils.showNoInternetMessage(getContext());
            return;
        }

        boolean isSampling = (product.equals("СИМ") && simType == 1);

        if (product.equals("ШПД")) {
            isSampling = (insurance == 1);
        }

        final boolean finalIsSampling = isSampling;

        apiClient.addSale(currentEmployee, product, amount, finalIsSampling, phoneModel, insurance, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "✅ Продажа добавлена!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "❌ Ошибка: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }
}