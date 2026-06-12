package com.example.salestracker;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.salestracker.utils.NetworkUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class TrafficAnalyticsActivity extends AppCompatActivity {

    private Spinner spinnerOffice;
    private Spinner spinnerPeriod;
    private TextView tvWeekdayStats;
    private TextView tvHourStats;
    private TextView tvRecommendations;
    private ApiClient apiClient;
    private String currentUserRole;
    private int currentOfficeId;

    private List<String> officeList = new ArrayList<>();
    private List<Integer> officeIdList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_traffic_analytics);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Аналитика трафика");

        apiClient = new ApiClient();

        SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
        currentUserRole = prefs.getString("user_role", "seller");
        currentOfficeId = prefs.getInt("office_id", 0);

        spinnerOffice = findViewById(R.id.spinnerOffice);
        spinnerPeriod = findViewById(R.id.spinnerPeriod);
        tvWeekdayStats = findViewById(R.id.tvWeekdayStats);
        tvHourStats = findViewById(R.id.tvHourStats);
        tvRecommendations = findViewById(R.id.tvRecommendations);

        // Настройка периода
        String[] periods = {"День", "Неделя", "Месяц", "Квартал", "Год"};
        ArrayAdapter<String> periodAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, periods);
        spinnerPeriod.setAdapter(periodAdapter);

        // Загрузка офисов
        loadOffices();

        spinnerPeriod.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                loadAnalytics();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void loadOffices() {
        apiClient.getShops(new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    JSONArray arr = obj.getJSONArray("shops");
                    officeList.clear();
                    officeIdList.clear();

                    if (currentUserRole.equals("owner") || currentUserRole.equals("rgo")) {
                        officeList.add("Все офисы");
                        officeIdList.add(0);
                    }

                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject shop = arr.getJSONObject(i);
                        if (currentUserRole.equals("dm")) {
                            if (shop.getInt("id") == currentOfficeId) {
                                officeList.add(shop.getString("name"));
                                officeIdList.add(shop.getInt("id"));
                                break;
                            }
                        } else if (currentUserRole.equals("owner") || currentUserRole.equals("rgo")) {
                            officeList.add(shop.getString("name"));
                            officeIdList.add(shop.getInt("id"));
                        } else {
                            finish();
                            return;
                        }
                    }

                    ArrayAdapter<String> officeAdapter = new ArrayAdapter<>(TrafficAnalyticsActivity.this,
                            android.R.layout.simple_spinner_dropdown_item, officeList);
                    spinnerOffice.setAdapter(officeAdapter);

                    spinnerOffice.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                            loadAnalytics();
                        }
                        @Override
                        public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                    });

                    loadAnalytics();
                } catch (Exception e) {
                    Toast.makeText(TrafficAnalyticsActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(TrafficAnalyticsActivity.this, "Ошибка загрузки офисов", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAnalytics() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            NetworkUtils.showNoInternetMessage(this);
            return;
        }

        int officeId = 0;
        int pos = spinnerOffice.getSelectedItemPosition();
        if (pos >= 0 && pos < officeIdList.size()) {
            officeId = officeIdList.get(pos);
        }

        String period = spinnerPeriod.getSelectedItem().toString();

        // Делаем финальные копии для использования в лямбде
        final int finalOfficeId = officeId;
        final String finalPeriod = period;

        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Загрузка аналитики...");
        progress.show();

        new Thread(() -> {
            try {
                String urlString = ApiClient.BASE_URL + "get_traffic_analytics.php?office_id=" + finalOfficeId + "&period=" + finalPeriod;
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
                                displayAnalytics(obj);
                            } else {
                                Toast.makeText(TrafficAnalyticsActivity.this, "Нет данных", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(TrafficAnalyticsActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        progress.dismiss();
                        Toast.makeText(TrafficAnalyticsActivity.this, "Ошибка загрузки", Toast.LENGTH_SHORT).show();
                    });
                }
                conn.disconnect();
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(TrafficAnalyticsActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void displayAnalytics(JSONObject data) throws Exception {
        StringBuilder weekdaySb = new StringBuilder();
        StringBuilder hourSb = new StringBuilder();
        StringBuilder recommendationsSb = new StringBuilder();

        // Дни недели
        JSONArray weekdays = data.getJSONArray("weekdays");
        weekdaySb.append("📅 ПО ДНЯМ НЕДЕЛИ:\n\n");
        int maxWeekday = data.optInt("max_weekday", 1);
        for (int i = 0; i < weekdays.length(); i++) {
            JSONObject day = weekdays.getJSONObject(i);
            String name = day.getString("name");
            int count = day.getInt("count");
            int bars = (int)((float)count / maxWeekday * 20);
            weekdaySb.append(name).append(" ");
            for (int b = 0; b < bars; b++) weekdaySb.append("█");
            for (int b = bars; b < 20; b++) weekdaySb.append("░");
            weekdaySb.append(" ").append(count).append(" продаж\n");
        }
        tvWeekdayStats.setText(weekdaySb.toString());

        // Часы пика
        JSONArray hours = data.getJSONArray("hours");
        hourSb.append("⏰ ЧАСЫ ПИКА:\n\n");
        int maxHour = data.optInt("max_hour", 1);
        for (int i = 0; i < hours.length(); i++) {
            JSONObject hour = hours.getJSONObject(i);
            int time = hour.getInt("hour");
            int count = hour.getInt("count");
            int bars = (int)((float)count / maxHour * 20);
            hourSb.append(String.format("%02d:00 ", time));
            for (int b = 0; b < bars; b++) hourSb.append("█");
            for (int b = bars; b < 20; b++) hourSb.append("░");
            hourSb.append(" ").append(count).append("\n");
        }
        tvHourStats.setText(hourSb.toString());

        // Рекомендации
        recommendationsSb.append("💡 РЕКОМЕНДАЦИИ:\n\n");
        JSONArray recommendations = data.optJSONArray("recommendations");
        if (recommendations != null && recommendations.length() > 0) {
            for (int i = 0; i < recommendations.length(); i++) {
                recommendationsSb.append("• ").append(recommendations.getString(i)).append("\n");
            }
        } else {
            recommendationsSb.append("Нет рекомендаций");
        }
        tvRecommendations.setText(recommendationsSb.toString());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}