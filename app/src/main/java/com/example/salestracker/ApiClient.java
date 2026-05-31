package com.example.salestracker;

import android.os.Handler;
import android.os.Looper;
import java.util.Locale;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApiClient {
    private static final String BASE_URL = "http://vanemya8.beget.tech/sales_api/";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    public interface ApiCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    public void addSale(String employee, String product, double amount, ApiCallback callback) {
        executor.execute(() -> {
            try {
                String amountStr = String.format(Locale.US, "%.2f", amount);
                String json = String.format(Locale.US, "{\"employee\":\"%s\",\"product\":\"%s\",\"amount\":%s}",
                        employee, product, amountStr);

                URL url = new URL(BASE_URL + "add_sale.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                OutputStream os = conn.getOutputStream();
                os.write(json.getBytes());
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    Scanner s = new Scanner(conn.getInputStream()).useDelimiter("\\A");
                    String response = s.hasNext() ? s.next() : "";
                    handler.post(() -> callback.onSuccess(response));
                } else {
                    handler.post(() -> callback.onError("HTTP ошибка: " + responseCode));
                }
                conn.disconnect();
            } catch (Exception e) {
                handler.post(() -> callback.onError("Ошибка: " + e.getMessage()));
            }
        });
    }
}