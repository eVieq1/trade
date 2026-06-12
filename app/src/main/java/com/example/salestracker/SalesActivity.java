package com.example.salestracker;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.gridlayout.widget.GridLayout;

import com.example.salestracker.utils.NetworkUtils;

public class SalesActivity extends AppCompatActivity {

    private TextView tvEmployee;
    private CardView cardPhone, cardSim, cardAccessory, cardAdapter, cardInternet, cardFinance;
    private ApiClient apiClient;
    private String currentEmployee;
    private String currentUserRole;
    private int currentOfficeId;

    private final String[] phoneModels = {"Samsung", "Realme", "Huawei", "Honor", "Infinix", "Techo", "Другие"};
    private final String[] simTypes = {"Обычная СИМ", "Семплинг", "Всё для семьи"};
    private final String[] internetTypes = {"Заявка", "Подключено"};
    private final int[] financeAmounts = {299, 499, 1290};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sales);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Продажи");

        initViews();
        loadUserData();
        setupClickListeners();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "Продажи за месяц");
        menu.add(0, 2, 1, "Изменить планы");
        menu.add(0, 3, 2, "Выход");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == 1) {
            Intent intent = new Intent(SalesActivity.this, MonthlySalesActivity.class);
            startActivity(intent);
            return true;
        } else if (id == 2) {
            Intent intent = new Intent(SalesActivity.this, PlanSettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == 3) {
            getSharedPreferences("app", MODE_PRIVATE).edit().clear().apply();
            Intent intent = new Intent(SalesActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initViews() {
        tvEmployee = findViewById(R.id.tvEmployee);
        cardPhone = findViewById(R.id.cardPhone);
        cardSim = findViewById(R.id.cardSim);
        cardAccessory = findViewById(R.id.cardAccessory);
        cardAdapter = findViewById(R.id.cardAdapter);
        cardInternet = findViewById(R.id.cardInternet);
        cardFinance = findViewById(R.id.cardFinance);
    }

    private void loadUserData() {
        apiClient = new ApiClient();
        SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
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

        TextView tvTodaySales = findViewById(R.id.tvTodaySales);
        tvTodaySales.setOnClickListener(v -> {
            Intent intent = new Intent(SalesActivity.this, EditSalesActivity.class);
            startActivity(intent);
        });
    }

    private void showPhoneDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("ПРОДАЖА ТЕЛЕФОНА");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 24);

        TextView tvModel = new TextView(this);
        tvModel.setText("Модель:");
        layout.addView(tvModel);

        Spinner spinnerModel = new Spinner(this);
        ArrayAdapter<String> modelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, phoneModels);
        spinnerModel.setAdapter(modelAdapter);
        layout.addView(spinnerModel);

        TextView tvAmount = new TextView(this);
        tvAmount.setText("Сумма:");
        tvAmount.setPadding(0, 32, 0, 8);
        layout.addView(tvAmount);

        EditText etAmount = new EditText(this);
        etAmount.setHint("Введите сумму");
        etAmount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(etAmount);

        builder.setView(layout);
        builder.setPositiveButton("ПРОДАТЬ", (dialog, which) -> {
            String model = spinnerModel.getSelectedItem().toString();
            String amountStr = etAmount.getText().toString();
            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Введите сумму", Toast.LENGTH_SHORT).show();
                return;
            }
            double amount = Double.parseDouble(amountStr);
            addSale("Телефон", amount, model, 0, 0);
        });
        builder.setNegativeButton("ОТМЕНА", null);
        builder.show();
    }

    private void showSimDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("ПРОДАЖА СИМ");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 24);

        final int[] selectedIndex = {0};

        RadioGroup radioGroup = new RadioGroup(this);
        radioGroup.setOrientation(LinearLayout.VERTICAL);

        for (int i = 0; i < simTypes.length; i++) {
            RadioButton rb = new RadioButton(this);
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

    private void showAccessoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("ПРОДАЖА АКСЕССУАРА");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 24);

        TextView tvAmount = new TextView(this);
        tvAmount.setText("Сумма:");
        layout.addView(tvAmount);

        EditText etAmount = new EditText(this);
        etAmount.setHint("Введите сумму");
        etAmount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(etAmount);

        builder.setView(layout);
        builder.setPositiveButton("ПРОДАТЬ", (dialog, which) -> {
            String amountStr = etAmount.getText().toString();
            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Введите сумму", Toast.LENGTH_SHORT).show();
                return;
            }
            double amount = Double.parseDouble(amountStr);
            addSale("Аксессуар", amount, "", 0, 0);
        });
        builder.setNegativeButton("ОТМЕНА", null);
        builder.show();
    }

    private void showAdapterDialog() {
        new AlertDialog.Builder(this)
                .setTitle("ПРОДАЖА АДАПТЕРА")
                .setMessage("Продать адаптер?")
                .setPositiveButton("ПРОДАТЬ", (dialog, which) -> addSale("Адаптер", 0, "", 0, 0))
                .setNegativeButton("ОТМЕНА", null)
                .show();
    }

    private void showInternetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("ПРОДАЖА ШПД");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 24);

        final int[] selectedIndex = {0};

        RadioGroup radioGroup = new RadioGroup(this);
        radioGroup.setOrientation(LinearLayout.VERTICAL);

        for (int i = 0; i < internetTypes.length; i++) {
            RadioButton rb = new RadioButton(this);
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

    private void showFinanceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("ФИНАНСОВЫЕ УСЛУГИ");

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(48, 32, 48, 24);

        final int[] selectedAmount = {299};
        final int[] selectedInsurance = {0};

        GridLayout gridLayout = new GridLayout(this);
        gridLayout.setColumnCount(2);
        gridLayout.setRowCount(2);

        for (int i = 0; i < financeAmounts.length; i++) {
            final int amount = financeAmounts[i];
            Button btn = new Button(this);
            btn.setText(amount + " ₽");
            btn.setPadding(16, 24, 16, 24);
            btn.setOnClickListener(v -> {
                selectedAmount[0] = amount;
                Toast.makeText(this, "Выбрано: " + amount + " ₽", Toast.LENGTH_SHORT).show();
            });
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(8, 8, 8, 8);
            btn.setLayoutParams(params);
            gridLayout.addView(btn);
        }

        Button emptyBtn = new Button(this);
        emptyBtn.setVisibility(View.INVISIBLE);
        emptyBtn.setEnabled(false);
        GridLayout.LayoutParams emptyParams = new GridLayout.LayoutParams();
        emptyParams.width = 0;
        emptyParams.height = GridLayout.LayoutParams.WRAP_CONTENT;
        emptyParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        emptyParams.setMargins(8, 8, 8, 8);
        emptyBtn.setLayoutParams(emptyParams);
        gridLayout.addView(emptyBtn);

        mainLayout.addView(gridLayout);

        Button btnInsurance = new Button(this);
        btnInsurance.setText("СТРАХОВКА");
        btnInsurance.setPadding(16, 24, 16, 24);
        btnInsurance.setOnClickListener(v -> {
            AlertDialog.Builder insuranceBuilder = new AlertDialog.Builder(this);
            insuranceBuilder.setTitle("Введите сумму страховки");
            EditText etInsurance = new EditText(this);
            etInsurance.setHint("Сумма");
            etInsurance.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            insuranceBuilder.setView(etInsurance);
            insuranceBuilder.setPositiveButton("СОХРАНИТЬ", (dialog, which) -> {
                String insStr = etInsurance.getText().toString();
                if (!insStr.isEmpty()) {
                    selectedInsurance[0] = Integer.parseInt(insStr);
                    Toast.makeText(this, "Страховка: " + selectedInsurance[0] + " ₽", Toast.LENGTH_SHORT).show();
                }
            });
            insuranceBuilder.setNegativeButton("ОТМЕНА", null);
            insuranceBuilder.show();
        });
        mainLayout.addView(btnInsurance);

        builder.setView(mainLayout);
        builder.setPositiveButton("ПРОДАТЬ", (dialog, which) -> {
            addSale("Финансовые услуги", selectedAmount[0], "", 0, selectedInsurance[0]);
        });
        builder.setNegativeButton("ОТМЕНА", null);
        builder.show();
    }

    private void addSale(String product, double amount, String phoneModel, int simType, int insurance) {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            NetworkUtils.showNoInternetMessage(this);
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
                runOnUiThread(() -> {
                    Toast.makeText(SalesActivity.this, "✅ Продажа добавлена!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                        Toast.makeText(SalesActivity.this, "❌ Ошибка: " + error, Toast.LENGTH_SHORT).show());
            }
        });
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
    }
}