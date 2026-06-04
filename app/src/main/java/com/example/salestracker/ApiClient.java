package com.example.salestracker;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.net.URLEncoder;
import java.util.Locale;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;

public class ApiClient {
    public static final String BASE_URL = "http://vanemya8.beget.tech/sales_api/";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isShutdown = false;

    public interface ApiCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    private void executeSafe(Runnable task) {
        if (!isShutdown) {
            executor.execute(task);
        }
    }

    public void shutdown() {
        isShutdown = true;
        executor.shutdown();
    }

    // ==================== МЕТОД ДЛЯ ПРОДАЖ (ОБНОВЛЁННЫЙ) ====================
    public void addSale(String employee, String product, double amount, boolean isSampling, String phoneModel, int insurance, ApiCallback callback) {
        executeSafe(() -> {
            HttpURLConnection conn = null;
            try {
                JSONObject json = new JSONObject();
                json.put("employee", employee);
                json.put("product", product);
                json.put("amount", amount);
                json.put("isSampling", isSampling);
                json.put("phoneModel", phoneModel != null ? phoneModel : "");
                json.put("insurance", insurance);

                Log.d("ApiClient", "Отправляем JSON: " + json.toString());

                URL url = new URL(BASE_URL + "add_sale.php");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.close();

                final int responseCode = conn.getResponseCode();

                if (responseCode == 200) {
                    Scanner s = new Scanner(conn.getInputStream(), "UTF-8").useDelimiter("\\A");
                    final String response = s.hasNext() ? s.next() : "";
                    s.close();
                    handler.post(() -> callback.onSuccess(response));
                } else {
                    handler.post(() -> callback.onError("HTTP ошибка: " + responseCode));
                }
            } catch (Exception e) {
                handler.post(() -> callback.onError("Ошибка: " + e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    // ==================== МЕТОД ДЛЯ ПРОВЕРКИ СОТРУДНИКА ====================
    public void checkEmployee(String name, ApiCallback callback) {
        executeSafe(() -> {
            HttpURLConnection conn = null;
            try {
                String encodedName = URLEncoder.encode(name, "UTF-8");
                URL url = new URL(BASE_URL + "check_employee.php?name=" + encodedName);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                final int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    Scanner s = new Scanner(conn.getInputStream(), "UTF-8").useDelimiter("\\A");
                    final String response = s.hasNext() ? s.next() : "";
                    s.close();
                    handler.post(() -> callback.onSuccess(response));
                } else {
                    handler.post(() -> callback.onError("HTTP " + responseCode));
                }
            } catch (Exception e) {
                handler.post(() -> callback.onError("Ошибка: " + e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    // ==================== МЕТОД ДЛЯ СОХРАНЕНИЯ ПЛАНОВ ====================
    public void savePlan(int year, int month, String category, double target, String unitType, ApiCallback callback) {
        executeSafe(() -> {
            HttpURLConnection conn = null;
            try {
                JSONObject json = new JSONObject();
                json.put("year", year);
                json.put("month", month);
                json.put("category", category);
                json.put("target", target);
                json.put("unit_type", unitType);

                URL url = new URL(BASE_URL + "save_plan.php");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.close();

                final int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    Scanner s = new Scanner(conn.getInputStream(), "UTF-8").useDelimiter("\\A");
                    final String response = s.hasNext() ? s.next() : "";
                    s.close();
                    handler.post(() -> callback.onSuccess(response));
                } else {
                    handler.post(() -> callback.onError("HTTP ошибка: " + responseCode));
                }
            } catch (Exception e) {
                handler.post(() -> callback.onError("Ошибка: " + e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    // ==================== МЕТОД ДЛЯ ПОЛУЧЕНИЯ ДАННЫХ ОТЧЁТА ====================
    public void getReportData(String startDate, String endDate, ApiCallback callback) {
        executeSafe(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(BASE_URL + "get_report_data.php?start_date=" + startDate + "&end_date=" + endDate);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                final int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    Scanner s = new Scanner(conn.getInputStream(), "UTF-8").useDelimiter("\\A");
                    final String response = s.hasNext() ? s.next() : "";
                    s.close();
                    handler.post(() -> callback.onSuccess(response));
                } else {
                    handler.post(() -> callback.onError("HTTP ошибка: " + responseCode));
                }
            } catch (Exception e) {
                handler.post(() -> callback.onError(e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    // ==================== МЕТОД ДЛЯ СОХРАНЕНИЯ ГРАФИКА ====================
    public void saveSchedule(int year, int month, int day, String employee, String shiftTime, ApiCallback callback) {
        executeSafe(() -> {
            HttpURLConnection conn = null;
            try {
                JSONObject json = new JSONObject();
                json.put("year", year);
                json.put("month", month);
                json.put("day", day);
                json.put("employee", employee);
                json.put("shift_time", shiftTime);

                URL url = new URL(BASE_URL + "save_schedule.php");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.close();

                final int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    Scanner s = new Scanner(conn.getInputStream(), "UTF-8").useDelimiter("\\A");
                    final String response = s.hasNext() ? s.next() : "";
                    s.close();
                    handler.post(() -> callback.onSuccess(response));
                } else {
                    handler.post(() -> callback.onError("HTTP ошибка: " + responseCode));
                }
            } catch (Exception e) {
                handler.post(() -> callback.onError("Ошибка: " + e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    // ==================== МЕТОД ДЛЯ ПОЛУЧЕНИЯ ГРАФИКА ====================
    public void getSchedule(int year, int month, ApiCallback callback) {
        executeSafe(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(BASE_URL + "get_schedule.php?year=" + year + "&month=" + month);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("Accept", "application/json");

                final int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    Scanner s = new Scanner(conn.getInputStream(), "UTF-8").useDelimiter("\\A");
                    final String response = s.hasNext() ? s.next() : "";
                    s.close();
                    handler.post(() -> callback.onSuccess(response));
                } else {
                    handler.post(() -> callback.onError("HTTP " + responseCode));
                }
            } catch (Exception e) {
                handler.post(() -> callback.onError("Ошибка: " + e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    // ==================== МЕТОД ДЛЯ ПОЛУЧЕНИЯ СПИСКА СОТРУДНИКОВ ====================
    public void getEmployees(ApiCallback callback) {
        executeSafe(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(BASE_URL + "get_employees.php");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("Accept", "application/json");

                final int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    Scanner s = new Scanner(conn.getInputStream(), "UTF-8").useDelimiter("\\A");
                    final String response = s.hasNext() ? s.next() : "";
                    s.close();
                    handler.post(() -> callback.onSuccess(response));
                } else {
                    handler.post(() -> callback.onError("HTTP " + responseCode));
                }
            } catch (Exception e) {
                handler.post(() -> callback.onError("Ошибка: " + e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    // ==================== МЕТОД ДЛЯ ПОЛУЧЕНИЯ МАГАЗИНОВ ====================
    public void getShops(ApiCallback callback) {
        executeSafe(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(BASE_URL + "get_shops.php");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("Accept", "application/json");

                final int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    Scanner s = new Scanner(conn.getInputStream(), "UTF-8").useDelimiter("\\A");
                    final String response = s.hasNext() ? s.next() : "";
                    s.close();
                    handler.post(() -> callback.onSuccess(response));
                } else {
                    handler.post(() -> callback.onError("HTTP " + responseCode));
                }
            } catch (Exception e) {
                handler.post(() -> callback.onError("Ошибка: " + e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    // ==================== МЕТОД ДЛЯ ПОЛУЧЕНИЯ РЕЙТИНГА ====================
    public void getRating(int year, int month, ApiCallback callback) {
        executeSafe(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(BASE_URL + "get_rating.php?year=" + year + "&month=" + month);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("Accept", "application/json");

                final int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    Scanner s = new Scanner(conn.getInputStream(), "UTF-8").useDelimiter("\\A");
                    final String response = s.hasNext() ? s.next() : "";
                    s.close();
                    handler.post(() -> callback.onSuccess(response));
                } else {
                    handler.post(() -> callback.onError("HTTP " + responseCode));
                }
            } catch (Exception e) {
                handler.post(() -> callback.onError("Ошибка: " + e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }
}