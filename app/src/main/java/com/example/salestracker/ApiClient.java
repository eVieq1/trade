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
    // ПРОБУЙ РАЗНЫЕ ВАРИАНТЫ - РАСКОММЕНТИРУЙ ТОТ, КОТОРЫЙ РАБОТАЕТ

    // Вариант 1: полный путь как работало раньше
    private static final String BASE_URL = "http://vanemya8.beget.tech/public_html/sales_api/";

    // Вариант 2: без public_html
    // private static final String BASE_URL = "http://vanemya8.beget.tech/sales_api/";

    // Вариант 3: корень
    // private static final String BASE_URL = "http://vanemya8.beget.tech/";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    public interface ApiCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    // ==================== МЕТОД ДЛЯ ПРОДАЖ ====================
    public void addSale(String employee, String product, double amount, boolean isSampling, String phoneModel, ApiCallback callback) {
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                String amountStr = String.format(Locale.US, "%.2f", amount);
                String json = String.format(Locale.US, "{\"employee\":\"%s\",\"product\":\"%s\",\"amount\":%s,\"isSampling\":%b,\"phoneModel\":\"%s\"}",
                        employee, product, amountStr, isSampling, phoneModel != null ? phoneModel : "");
                Log.d("ApiClient", "Отправляем JSON: " + json);

                URL url = new URL(BASE_URL + "add_sale.php");
                Log.e("API_URL", "URL: " + url.toString());

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                OutputStream os = conn.getOutputStream();
                os.write(json.getBytes());
                os.close();

                int responseCode = conn.getResponseCode();
                Log.e("API_RESPONSE", "Response code: " + responseCode);

                if (responseCode == 200) {
                    Scanner s = new Scanner(conn.getInputStream()).useDelimiter("\\A");
                    String response = s.hasNext() ? s.next() : "";
                    handler.post(() -> callback.onSuccess(response));
                } else {
                    String errorMsg = "";
                    if (conn.getErrorStream() != null) {
                        Scanner s = new Scanner(conn.getErrorStream()).useDelimiter("\\A");
                        errorMsg = s.hasNext() ? s.next() : "";
                    }
                    handler.post(() -> callback.onError("HTTP ошибка: " + responseCode + " - " + errorMsg));
                }
            } catch (Exception e) {
                Log.e("API_ERROR", "Exception: " + e.getMessage());
                handler.post(() -> callback.onError("Ошибка: " + e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    // ==================== МЕТОД ДЛЯ ПРОВЕРКИ СОТРУДНИКА ====================
    public void checkEmployee(String name, ApiCallback callback) {
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                String urlString = BASE_URL + "check_employee.php?name=" + URLEncoder.encode(name, "UTF-8");
                Log.e("API_URL", "URL: " + urlString);

                URL url = new URL(urlString);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                int responseCode = conn.getResponseCode();
                Log.e("API_RESPONSE", "Response code: " + responseCode);

                if (responseCode == 200) {
                    Scanner s = new Scanner(conn.getInputStream()).useDelimiter("\\A");
                    String response = s.hasNext() ? s.next() : "";
                    handler.post(() -> callback.onSuccess(response));
                } else {
                    String errorMsg = "";
                    if (conn.getErrorStream() != null) {
                        Scanner s = new Scanner(conn.getErrorStream()).useDelimiter("\\A");
                        errorMsg = s.hasNext() ? s.next() : "";
                    }
                    handler.post(() -> callback.onError("HTTP ошибка: " + responseCode + " - " + errorMsg));
                }
            } catch (Exception e) {
                Log.e("API_ERROR", "Exception: " + e.getMessage());
                handler.post(() -> callback.onError(e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    // ==================== МЕТОД ДЛЯ СОХРАНЕНИЯ ПЛАНОВ ====================
    public void savePlan(int year, int month, String category, double target, String unitType, ApiCallback callback) {
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                JSONObject json = new JSONObject();
                json.put("year", year);
                json.put("month", month);
                json.put("category", category);
                json.put("target", target);
                json.put("unit_type", unitType);

                Log.d("ApiClient", "Сохраняем план: " + json.toString());

                URL url = new URL(BASE_URL + "save_plan.php");
                Log.e("API_URL", "URL: " + url.toString());

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes());
                os.close();

                int responseCode = conn.getResponseCode();
                Log.e("API_RESPONSE", "Response code: " + responseCode);

                if (responseCode == 200) {
                    Scanner s = new Scanner(conn.getInputStream()).useDelimiter("\\A");
                    String response = s.hasNext() ? s.next() : "";
                    handler.post(() -> callback.onSuccess(response));
                } else {
                    String errorMsg = "";
                    if (conn.getErrorStream() != null) {
                        Scanner s = new Scanner(conn.getErrorStream()).useDelimiter("\\A");
                        errorMsg = s.hasNext() ? s.next() : "";
                    }
                    handler.post(() -> callback.onError("HTTP ошибка: " + responseCode + " - " + errorMsg));
                }
            } catch (Exception e) {
                Log.e("API_ERROR", "Exception: " + e.getMessage());
                handler.post(() -> callback.onError("Ошибка: " + e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    // ==================== МЕТОД ДЛЯ ПОЛУЧЕНИЯ ДАННЫХ ОТЧЁТА ====================
    public void getReportData(String startDate, String endDate, ApiCallback callback) {
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                String urlString = BASE_URL + "get_report_data.php?start_date=" + startDate + "&end_date=" + endDate;
                Log.e("API_URL", "URL: " + urlString);

                URL url = new URL(urlString);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                int responseCode = conn.getResponseCode();
                Log.e("API_RESPONSE", "Response code: " + responseCode);

                if (responseCode == 200) {
                    Scanner s = new Scanner(conn.getInputStream()).useDelimiter("\\A");
                    String response = s.hasNext() ? s.next() : "";
                    handler.post(() -> callback.onSuccess(response));
                } else {
                    String errorMsg = "";
                    if (conn.getErrorStream() != null) {
                        Scanner s = new Scanner(conn.getErrorStream()).useDelimiter("\\A");
                        errorMsg = s.hasNext() ? s.next() : "";
                    }
                    handler.post(() -> callback.onError("HTTP ошибка: " + responseCode + " - " + errorMsg));
                }
            } catch (Exception e) {
                Log.e("API_ERROR", "Exception: " + e.getMessage());
                handler.post(() -> callback.onError(e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    // ==================== МЕТОД ДЛЯ СОХРАНЕНИЯ ГРАФИКА ====================
    public void saveSchedule(int year, int month, int day, String employee, String shiftTime, ApiCallback callback) {
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                JSONObject json = new JSONObject();
                json.put("year", year);
                json.put("month", month);
                json.put("day", day);
                json.put("employee", employee);
                json.put("shift_time", shiftTime);

                URL url = new URL(BASE_URL + "save_schedule.php");
                Log.e("API_URL", "URL: " + url.toString());

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes());
                os.close();

                int responseCode = conn.getResponseCode();
                Log.e("API_RESPONSE", "Response code: " + responseCode);

                if (responseCode == 200) {
                    Scanner s = new Scanner(conn.getInputStream()).useDelimiter("\\A");
                    String response = s.hasNext() ? s.next() : "";
                    handler.post(() -> callback.onSuccess(response));
                } else {
                    String errorMsg = "";
                    if (conn.getErrorStream() != null) {
                        Scanner s = new Scanner(conn.getErrorStream()).useDelimiter("\\A");
                        errorMsg = s.hasNext() ? s.next() : "";
                    }
                    handler.post(() -> callback.onError("HTTP ошибка: " + responseCode + " - " + errorMsg));
                }
            } catch (Exception e) {
                Log.e("API_ERROR", "Exception: " + e.getMessage());
                handler.post(() -> callback.onError("Ошибка: " + e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    // ==================== МЕТОД ДЛЯ ПОЛУЧЕНИЯ ГРАФИКА ====================
    public void getSchedule(int year, int month, ApiCallback callback) {
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                String urlString = BASE_URL + "get_schedule.php?year=" + year + "&month=" + month;

                // ВАЖНО: смотри эти логи в Logcat!
                Log.e("API_DEBUG", "========== НАЧАЛО ЗАПРОСА ==========");
                Log.e("API_DEBUG", "BASE_URL: " + BASE_URL);
                Log.e("API_DEBUG", "Полный URL: " + urlString);

                URL url = new URL(urlString);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("Accept", "application/json");

                int responseCode = conn.getResponseCode();
                Log.e("API_DEBUG", "Response Code: " + responseCode);

                if (responseCode == 200) {
                    Scanner s = new Scanner(conn.getInputStream()).useDelimiter("\\A");
                    String response = s.hasNext() ? s.next() : "";
                    Log.e("API_DEBUG", "Ответ сервера: " + response);
                    handler.post(() -> callback.onSuccess(response));
                } else {
                    String errorMsg = "";
                    try {
                        if (conn.getErrorStream() != null) {
                            Scanner s = new Scanner(conn.getErrorStream()).useDelimiter("\\A");
                            errorMsg = s.hasNext() ? s.next() : "";
                        }
                    } catch (Exception e) {
                        errorMsg = e.getMessage();
                    }
                    Log.e("API_DEBUG", "Ошибка: " + responseCode + " - " + errorMsg);
                    handler.post(() -> callback.onError("HTTP " + responseCode + ": " + errorMsg));
                }

            } catch (Exception e) {
                Log.e("API_DEBUG", "Исключение: " + e.getMessage());
                e.printStackTrace();
                handler.post(() -> callback.onError("Ошибка: " + e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
                Log.e("API_DEBUG", "========== КОНЕЦ ЗАПРОСА ==========");
            }
        });
    }
}