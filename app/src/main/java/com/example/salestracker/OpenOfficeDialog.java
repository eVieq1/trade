package com.example.salestracker;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.Toast;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class OpenOfficeDialog {

    public interface OnOfficeOpenedListener {
        void onOpened();
    }

    public static void showIfNeeded(Context context, int officeId, String employee, OnOfficeOpenedListener listener) {
        ApiClient apiClient = new ApiClient();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        apiClient.getOfficeStatus(officeId, today, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    boolean isOpen = obj.optBoolean("is_open", false);

                    if (!isOpen) {
                        showOpenDialog(context, officeId, employee, listener);
                    } else if (listener != null) {
                        listener.onOpened();
                    }
                } catch (Exception e) {
                    showOpenDialog(context, officeId, employee, listener);
                }
            }

            @Override
            public void onError(String error) {
                showOpenDialog(context, officeId, employee, listener);
            }
        });
    }

    private static void showOpenDialog(Context context, int officeId, String employee, OnOfficeOpenedListener listener) {
        new AlertDialog.Builder(context)
                .setTitle("🏢 Открытие офиса")
                .setMessage("Доброе утро! Офис ещё не открыт. Открыть сейчас?")
                .setPositiveButton("ОТКРЫТЬ ОФИС", (dialog, which) -> {
                    ApiClient apiClient = new ApiClient();
                    String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                    apiClient.openOffice(officeId, employee, today, new ApiClient.ApiCallback() {
                        @Override
                        public void onSuccess(String response) {
                            Toast.makeText(context, "✅ Офис открыт!", Toast.LENGTH_LONG).show();
                            if (listener != null) listener.onOpened();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(context, "❌ Ошибка: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("ПОЗЖЕ", null)
                .setCancelable(false)
                .show();
    }
}