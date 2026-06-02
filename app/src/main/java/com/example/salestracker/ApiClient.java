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
    // ПРАВИЛЬНЫЙ ПУТЬ (без public_html)
    private static final String BASE_URL = "http://vanemya8.beget.tech/sales_api/";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    public interface ApiCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    // ==================== МЕТОД ДЛЯ ПРОДАЖ ====================
    public void addSale(String employee, String product, double amount, boolean isSampling, String phoneModel, ApiCallback callback) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    String amountStr = String.format(Locale.US, "%.2f", amount);
                    String json = String.format(Locale.US, "{\"employee\":\"%s\",\"product\":\"%s\",\"amount\":%s,\"isSampling\":%b,\"phoneModel\":\"%s\"}",
                            employee, product, amountStr, isSampling, phoneModel != null ? phoneModel : "");
                    Log.d("ApiClient", "Отправляем JSON: " + json);

                    URL url = new URL(BASE_URL + "add_sale.php");
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);

                    OutputStream os = conn.getOutputStream();
                    os.write(json.getBytes("UTF-8"));
                    os.close();

                    final int responseCode = conn.getResponseCode();

                    if (responseCode == 200) {
                        Scanner s = new Scanner(conn.getInputStream(), "UTF-8").useDelimiter("\\A");
                        final String response = s.hasNext() ? s.next() : "";
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(response);
                            }
                        });
                    } else {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError("HTTP ошибка: " + responseCode);
                            }
                        });
                    }
                } catch (Exception e) {
                    final String error = e.getMessage();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError("Ошибка: " + error);
                        }
                    });
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }
        });
    }

    // ==================== МЕТОД ДЛЯ ПРОВЕРКИ СОТРУДНИКА ====================
    public void checkEmployee(String name, ApiCallback callback) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    // ✅ КЛЮЧЕВОЕ ИСПРАВЛЕНИЕ: кодируем кириллицу
                    String encodedName = URLEncoder.encode(name, "UTF-8");
                    final String urlString = BASE_URL + "check_employee.php?name=" + encodedName;
                    Log.d("ApiClient", "Запрос: " + urlString);

                    URL url = new URL(urlString);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);

                    final int responseCode = conn.getResponseCode();
                    Log.d("ApiClient", "Response code: " + responseCode);

                    if (responseCode == 200) {
                        Scanner s = new Scanner(conn.getInputStream(), "UTF-8").useDelimiter("\\A");
                        final String response = s.hasNext() ? s.next() : "";
                        Log.d("ApiClient", "Ответ: " + response);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(response);
                            }
                        });
                    } else {
                        String errorMsg = "";
                        if (conn.getErrorStream() != null) {
                            Scanner s = new Scanner(conn.getErrorStream(), "UTF-8").useDelimiter("\\A");
                            errorMsg = s.hasNext() ? s.next() : "";
                        }
                        final String finalErrorMsg = errorMsg;
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError("HTTP " + responseCode + ": " + finalErrorMsg);
                            }
                        });
                    }
                } catch (Exception e) {
                    final String error = e.getMessage();
                    Log.e("ApiClient", "Ошибка: " + error);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError("Ошибка: " + error);
                        }
                    });
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }
        });
    }

    // ==================== МЕТОД ДЛЯ СОХРАНЕНИЯ ПЛАНОВ ====================
    public void savePlan(int year, int month, String category, double target, String unitType, ApiCallback callback) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
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
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(response);
                            }
                        });
                    } else {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError("HTTP ошибка: " + responseCode);
                            }
                        });
                    }
                } catch (Exception e) {
                    final String error = e.getMessage();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError("Ошибка: " + error);
                        }
                    });
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }
        });
    }

    // ==================== МЕТОД ДЛЯ ПОЛУЧЕНИЯ ДАННЫХ ОТЧЁТА ====================
    public void getReportData(String startDate, String endDate, ApiCallback callback) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    final String urlString = BASE_URL + "get_report_data.php?start_date=" + startDate + "&end_date=" + endDate;
                    URL url = new URL(urlString);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);

                    final int responseCode = conn.getResponseCode();

                    if (responseCode == 200) {
                        Scanner s = new Scanner(conn.getInputStream(), "UTF-8").useDelimiter("\\A");
                        final String response = s.hasNext() ? s.next() : "";
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(response);
                            }
                        });
                    } else {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError("HTTP ошибка: " + responseCode);
                            }
                        });
                    }
                } catch (Exception e) {
                    final String error = e.getMessage();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(error);
                        }
                    });
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }
        });
    }

    // ==================== МЕТОД ДЛЯ СОХРАНЕНИЯ ГРАФИКА ====================
    public void saveSchedule(int year, int month, int day, String employee, String shiftTime, ApiCallback callback) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
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
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(response);
                            }
                        });
                    } else {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError("HTTP ошибка: " + responseCode);
                            }
                        });
                    }
                } catch (Exception e) {
                    final String error = e.getMessage();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError("Ошибка: " + error);
                        }
                    });
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }
        });
    }

    // ==================== МЕТОД ДЛЯ ПОЛУЧЕНИЯ ГРАФИКА ====================
    public void getSchedule(int year, int month, ApiCallback callback) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    final String urlString = BASE_URL + "get_schedule.php?year=" + year + "&month=" + month;
                    URL url = new URL(urlString);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);
                    conn.setRequestProperty("Accept", "application/json");

                    final int responseCode = conn.getResponseCode();

                    if (responseCode == 200) {
                        Scanner s = new Scanner(conn.getInputStream(), "UTF-8").useDelimiter("\\A");
                        final String response = s.hasNext() ? s.next() : "";
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(response);
                            }
                        });
                    } else {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError("HTTP " + responseCode);
                            }
                        });
                    }
                } catch (Exception e) {
                    final String error = e.getMessage();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError("Ошибка: " + error);
                        }
                    });
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }
        });
    }

    // ==================== НОВЫЙ МЕТОД ДЛЯ ПОЛУЧЕНИЯ СПИСКА СОТРУДНИКОВ ====================
    public void getEmployees(ApiCallback callback) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    final String urlString = BASE_URL + "get_employees.php";
                    URL url = new URL(urlString);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);
                    conn.setRequestProperty("Accept", "application/json");

                    final int responseCode = conn.getResponseCode();

                    if (responseCode == 200) {
                        Scanner s = new Scanner(conn.getInputStream(), "UTF-8").useDelimiter("\\A");
                        final String response = s.hasNext() ? s.next() : "";
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(response);
                            }
                        });
                    } else {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError("HTTP " + responseCode);
                            }
                        });
                    }
                } catch (Exception e) {
                    final String error = e.getMessage();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError("Ошибка: " + error);
                        }
                    });
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }
        });
    }
}