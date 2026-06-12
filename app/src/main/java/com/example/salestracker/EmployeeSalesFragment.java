package com.example.salestracker.fragments;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.salestracker.ApiClient;
import com.example.salestracker.R;
import com.example.salestracker.utils.NetworkUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class EmployeeSalesFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout containerSales;
    private ApiClient apiClient;
    private int employeeId;
    private String employeeName;

    public static EmployeeSalesFragment newInstance(int employeeId, String employeeName) {
        EmployeeSalesFragment fragment = new EmployeeSalesFragment();
        Bundle args = new Bundle();
        args.putInt("employee_id", employeeId);
        args.putString("employee_name", employeeName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            employeeId = getArguments().getInt("employee_id");
            employeeName = getArguments().getString("employee_name");
        }
        apiClient = new ApiClient();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_employee_sales, container, false);

        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        containerSales = view.findViewById(R.id.containerSales);

        swipeRefresh.setOnRefreshListener(() -> loadSales());

        loadSales();

        return view;
    }

    private void loadSales() {
        if (!NetworkUtils.isNetworkAvailable(getContext())) {
            swipeRefresh.setRefreshing(false);
            NetworkUtils.showNoInternetMessage(getContext());
            return;
        }

        swipeRefresh.setRefreshing(true);

        new Thread(() -> {
            try {
                String urlString = ApiClient.BASE_URL + "get_employee_sales.php?employee_id=" + employeeId;
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

                    requireActivity().runOnUiThread(() -> {
                        swipeRefresh.setRefreshing(false);
                        try {
                            JSONObject obj = new JSONObject(response);
                            if (obj.getString("status").equals("success")) {
                                displaySales(obj);
                            } else {
                                showError("Нет данных о продажах");
                            }
                        } catch (Exception e) {
                            showError("Ошибка: " + e.getMessage());
                        }
                    });
                } else {
                    requireActivity().runOnUiThread(() -> {
                        swipeRefresh.setRefreshing(false);
                        showError("Ошибка загрузки");
                    });
                }
                conn.disconnect();
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    swipeRefresh.setRefreshing(false);
                    showError("Ошибка: " + e.getMessage());
                });
            }
        }).start();
    }

    private void displaySales(JSONObject data) throws Exception {
        containerSales.removeAllViews();

        double totalAmount = 0;

        // Телефоны
        JSONArray phones = data.optJSONArray("phones");
        if (phones != null && phones.length() > 0) {
            addCategoryHeader("📱 ТЕЛЕФОНЫ");
            for (int i = 0; i < phones.length(); i++) {
                JSONObject item = phones.getJSONObject(i);
                String model = item.getString("model");
                int count = item.getInt("count");
                double amount = item.getDouble("amount");
                totalAmount += amount;
                addSaleItem("   " + model, count + " шт", (int)amount + " ₽");
            }
        }

        // SIM
        JSONObject sim = data.optJSONObject("sim");
        if (sim != null) {
            addCategoryHeader("📡 СИМ");
            int regular = sim.optInt("regular", 0);
            int sampling = sim.optInt("sampling", 0);
            int family = sim.optInt("family", 0);
            double amount = sim.optDouble("amount", 0);
            totalAmount += amount;
            addSaleItem("   Обычные", regular + " шт", "");
            addSaleItem("   Семплинг", sampling + " шт", "");
            addSaleItem("   Всё для семьи", family + " шт", "");
            addSaleItem("   Итого", "", (int)amount + " ₽");
        }

        // Финансовые услуги
        JSONArray finance = data.optJSONArray("finance");
        if (finance != null && finance.length() > 0) {
            addCategoryHeader("💰 ФИНАНСОВЫЕ УСЛУГИ");
            for (int i = 0; i < finance.length(); i++) {
                JSONObject item = finance.getJSONObject(i);
                int price = item.getInt("price");
                int count = item.getInt("count");
                double amount = item.getDouble("amount");
                totalAmount += amount;
                addSaleItem("   " + price + " ₽", count + " шт", (int)amount + " ₽");
            }
        }

        // ШПД
        JSONObject internet = data.optJSONObject("internet");
        if (internet != null) {
            addCategoryHeader("📶 ШПД");
            int request = internet.optInt("request", 0);
            int connected = internet.optInt("connected", 0);
            addSaleItem("   Заявка", request + " шт", "");
            addSaleItem("   Подключено", connected + " шт", "");
        }

        // Аксессуары
        int accessories = data.optInt("accessories", 0);
        if (accessories > 0) {
            addCategoryHeader("🎧 АКСЕССУАРЫ");
            addSaleItem("   Всего", accessories + " шт", "");
        }

        // Адаптеры
        int adapters = data.optInt("adapters", 0);
        if (adapters > 0) {
            addCategoryHeader("🔌 АДАПТЕРЫ");
            addSaleItem("   Всего", adapters + " шт", "");
        }

        // Итого
        addDivider();
        addTotalLine("ИТОГО ЗА МЕСЯЦ:", (int)totalAmount + " ₽");
    }

    private void addCategoryHeader(String title) {
        TextView tv = new TextView(getContext());
        tv.setText(title);
        tv.setTextSize(16);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setTextColor(0xFF333333);
        tv.setPadding(16, 24, 16, 8);
        containerSales.addView(tv);
    }

    private void addSaleItem(String name, String count, String amount) {
        LinearLayout itemLayout = new LinearLayout(getContext());
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setPadding(16, 4, 16, 4);

        TextView tvName = new TextView(getContext());
        tvName.setText(name);
        tvName.setTextSize(14);
        tvName.setTextColor(0xFF666666);
        tvName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView tvCount = new TextView(getContext());
        tvCount.setText(count);
        tvCount.setTextSize(14);
        tvCount.setTextColor(0xFF666666);
        tvCount.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView tvAmount = new TextView(getContext());
        tvAmount.setText(amount);
        tvAmount.setTextSize(14);
        tvAmount.setTextColor(0xFF2196F3);
        tvAmount.setTypeface(null, android.graphics.Typeface.BOLD);
        tvAmount.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        tvAmount.setPadding(16, 0, 0, 0);

        itemLayout.addView(tvName);
        if (!count.isEmpty()) itemLayout.addView(tvCount);
        if (!amount.isEmpty()) itemLayout.addView(tvAmount);

        containerSales.addView(itemLayout);
    }

    private void addDivider() {
        View divider = new View(getContext());
        divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(0xFFE0E0E0);
        divider.setPadding(16, 16, 16, 16);
        containerSales.addView(divider);
    }

    private void addTotalLine(String title, String amount) {
        LinearLayout totalLayout = new LinearLayout(getContext());
        totalLayout.setOrientation(LinearLayout.HORIZONTAL);
        totalLayout.setPadding(16, 16, 16, 16);

        TextView tvTitle = new TextView(getContext());
        tvTitle.setText(title);
        tvTitle.setTextSize(16);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setTextColor(0xFF333333);
        tvTitle.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView tvAmount = new TextView(getContext());
        tvAmount.setText(amount);
        tvAmount.setTextSize(16);
        tvAmount.setTypeface(null, android.graphics.Typeface.BOLD);
        tvAmount.setTextColor(0xFFFF9800);
        tvAmount.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        totalLayout.addView(tvTitle);
        totalLayout.addView(tvAmount);
        containerSales.addView(totalLayout);
    }

    private void showError(String message) {
        containerSales.removeAllViews();
        TextView tv = new TextView(getContext());
        tv.setText(message);
        tv.setPadding(32, 32, 32, 32);
        tv.setTextSize(14);
        tv.setTextColor(0xFF999999);
        tv.setGravity(android.view.Gravity.CENTER);
        containerSales.addView(tv);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (apiClient != null) {
            apiClient.shutdown();
        }
    }
}