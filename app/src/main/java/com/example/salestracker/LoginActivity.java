package com.example.salestracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private EditText etName;
    private Button btnLogin;
    private ApiClient apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        apiClient = new ApiClient();

        SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
        String savedName = prefs.getString("employee_name", "");

        if (!savedName.isEmpty()) {
            startActivity(new Intent(LoginActivity.this, MainMenuActivity.class));
            finish();
            return;
        }

        etName = findViewById(R.id.etName);
        btnLogin = findViewById(R.id.btnLogin);

        // Запрещаем ввод пробелов и цифр
        etName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String rawName = s.toString();
                String original = rawName;

                // Удаляем пробелы
                if (rawName.contains(" ")) {
                    rawName = rawName.replace(" ", "");
                }

                // Удаляем цифры
                String newText = rawName.replaceAll("[0-9]", "");

                if (!newText.equals(original)) {
                    etName.setText(newText);
                    etName.setSelection(newText.length());

                    if (original.matches(".*[0-9].*")) {
                        Toast.makeText(LoginActivity.this, "Цифры запрещены", Toast.LENGTH_SHORT).show();
                    } else if (original.contains(" ")) {
                        Toast.makeText(LoginActivity.this, "Пробелы запрещены", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }

                String name = newText.trim();
                boolean hasOnlyLetters = name.matches("^[a-zA-Zа-яА-Я]+$");
                boolean isValidLength = name.length() >= 2 && name.length() <= 12;

                if (hasOnlyLetters && isValidLength && !name.isEmpty()) {
                    btnLogin.setEnabled(true);
                    etName.setError(null);
                } else if (name.isEmpty()) {
                    btnLogin.setEnabled(false);
                    etName.setError(null);
                } else if (!hasOnlyLetters) {
                    btnLogin.setEnabled(false);
                    etName.setError("Только буквы, без цифр и знаков");
                } else if (!isValidLength) {
                    btnLogin.setEnabled(false);
                    etName.setError("Имя должно быть от 2 до 12 букв");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnLogin.setEnabled(false);

        btnLogin.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            checkEmployeeOnServer(name);
        });
    }

    private void checkEmployeeOnServer(String name) {
        apiClient.checkEmployee(name, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    boolean exists = obj.getBoolean("exists");
                    String role = obj.optString("role", "seller");

                    if (exists) {
                        SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
                        prefs.edit()
                                .putString("employee_name", name)
                                .putString("user_role", role)
                                .apply();
                        startActivity(new Intent(LoginActivity.this, MainMenuActivity.class));
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Сотрудник не найден в системе", Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(LoginActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(LoginActivity.this, "Ошибка соединения: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}