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

import com.example.salestracker.databinding.ActivityLoginBinding;
import com.example.salestracker.utils.NetworkUtils;

import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private ApiClient apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiClient = new ApiClient();

        SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
        String savedName = prefs.getString("employee_name", "");

        if (!savedName.isEmpty()) {
            startActivity(new Intent(LoginActivity.this, MainMenuActivity.class));
            finish();
            return;
        }

        binding.etName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String rawName = s.toString();
                String original = rawName;

                if (rawName.contains(" ")) {
                    rawName = rawName.replace(" ", "");
                }

                String newText = rawName.replaceAll("[0-9]", "");

                if (!newText.equals(original)) {
                    binding.etName.setText(newText);
                    binding.etName.setSelection(newText.length());

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
                    binding.btnLogin.setEnabled(true);
                    binding.etName.setError(null);
                } else if (name.isEmpty()) {
                    binding.btnLogin.setEnabled(false);
                    binding.etName.setError(null);
                } else if (!hasOnlyLetters) {
                    binding.btnLogin.setEnabled(false);
                    binding.etName.setError("Только буквы, без цифр и знаков");
                } else if (!isValidLength) {
                    binding.btnLogin.setEnabled(false);
                    binding.etName.setError("Имя должно быть от 2 до 12 букв");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.btnLogin.setEnabled(false);

        binding.btnLogin.setOnClickListener(v -> {
            if (!NetworkUtils.checkAndShowNoInternet(this)) {
                return;
            }
            String name = binding.etName.getText().toString().trim();
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
                    int officeId = obj.optInt("office_id", 0);
                    String officeName = obj.optString("office_name", "Не привязан");

                    if (exists) {
                        SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
                        prefs.edit()
                                .putString("employee_name", name)
                                .putString("user_role", role)
                                .putInt("office_id", officeId)
                                .putString("office_name", officeName)
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (apiClient != null) {
            apiClient.shutdown();
        }
        binding = null;
    }
}