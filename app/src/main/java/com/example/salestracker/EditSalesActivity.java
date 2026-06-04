package com.example.salestracker;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.salestracker.utils.NetworkUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class EditSalesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SaleAdapter adapter;
    private List<Sale> saleList = new ArrayList<>();
    private ApiClient apiClient;
    private String currentUserRole;
    private int currentOfficeId;
    private String currentEmployee;
    private TextView tvOffice, tvDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_sales);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Редактирование продаж");

        tvOffice = findViewById(R.id.tvOffice);
        tvDate = findViewById(R.id.tvDate);
        recyclerView = findViewById(R.id.recyclerViewSales);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        apiClient = new ApiClient();

        SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
        currentUserRole = prefs.getString("user_role", "seller");
        currentOfficeId = prefs.getInt("office_id", 0);
        currentEmployee = prefs.getString("employee_name", "");

        // Получаем название офиса из SharedPreferences
        String officeName = prefs.getString("office_name", "Мой офис");
        tvOffice.setText("Офис: " + officeName);

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        tvDate.setText("Дата: " + sdf.format(new Date()));

        setupSwipeToDelete();
        loadSales();
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position < 0 || position >= saleList.size()) {
                    adapter.notifyDataSetChanged();
                    return;
                }

                Sale sale = saleList.get(position);

                boolean canDelete = false;
                if (currentUserRole.equals("owner") || currentUserRole.equals("dm")) {
                    canDelete = true;
                } else if ((currentUserRole.equals("senior_seller") || currentUserRole.equals("seller"))
                        && sale.employeeName.equals(currentEmployee)) {
                    canDelete = true;
                }

                if (!canDelete) {
                    Toast.makeText(EditSalesActivity.this, "Нет прав на удаление", Toast.LENGTH_SHORT).show();
                    adapter.notifyItemChanged(position);
                    return;
                }

                new AlertDialog.Builder(EditSalesActivity.this)
                        .setTitle("Удалить продажу")
                        .setMessage("Удалить продажу на " + (int) sale.amount + " ₽?")
                        .setPositiveButton("Удалить", (dialog, which) -> deleteSale(sale.id, position))
                        .setNegativeButton("Отмена", (dialog, which) -> adapter.notifyItemChanged(position))
                        .show();
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView);
    }

    private void loadSales() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            NetworkUtils.showNoInternetMessage(this);
            return;
        }

        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Загрузка...");
        progress.show();

        new Thread(() -> {
            try {
                String urlString = ApiClient.BASE_URL + "get_today_sales.php";
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

                    Log.d("EditSalesActivity", "Ответ сервера: " + response);

                    runOnUiThread(() -> {
                        progress.dismiss();
                        try {
                            JSONObject obj = new JSONObject(response);
                            if (obj.getString("status").equals("success")) {
                                JSONArray arr = obj.getJSONArray("sales");
                                saleList.clear();
                                for (int i = 0; i < arr.length(); i++) {
                                    JSONObject saleObj = arr.getJSONObject(i);
                                    Sale sale = new Sale();
                                    sale.id = saleObj.getInt("id");
                                    sale.employeeName = saleObj.getString("employee_name");
                                    sale.product = saleObj.getString("product");
                                    sale.amount = saleObj.getDouble("amount");
                                    sale.phoneModel = saleObj.optString("phone_model", "");
                                    sale.insurance = saleObj.optInt("insurance", 0);
                                    sale.saleTime = saleObj.getString("sale_time");
                                    saleList.add(sale);
                                }
                                adapter = new SaleAdapter();
                                recyclerView.setAdapter(adapter);

                                if (saleList.isEmpty()) {
                                    Toast.makeText(EditSalesActivity.this, "Нет продаж за сегодня", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(EditSalesActivity.this, "Загружено: " + saleList.size() + " продаж", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } catch (Exception e) {
                            Log.e("EditSalesActivity", "Ошибка парсинга: " + e.getMessage());
                            Toast.makeText(EditSalesActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        progress.dismiss();
                        Toast.makeText(EditSalesActivity.this, "Ошибка загрузки: " + responseCode, Toast.LENGTH_SHORT).show();
                    });
                }
                conn.disconnect();
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(EditSalesActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void deleteSale(int id, int position) {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Удаление...");
        progress.show();

        new Thread(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("id", id);

                URL url = new URL(ApiClient.BASE_URL + "delete_sale.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                runOnUiThread(() -> {
                    progress.dismiss();
                    if (responseCode == 200) {
                        saleList.remove(position);
                        adapter.notifyItemRemoved(position);
                        Toast.makeText(EditSalesActivity.this, "Продажа удалена", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(EditSalesActivity.this, "Ошибка удаления", Toast.LENGTH_SHORT).show();
                        adapter.notifyItemChanged(position);
                    }
                });
                conn.disconnect();
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(EditSalesActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    adapter.notifyItemChanged(position);
                });
            }
        }).start();
    }

    private void editSale(Sale sale, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Редактирование продажи");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 24);

        TextView tvProduct = new TextView(this);
        tvProduct.setText(sale.product + (sale.phoneModel.isEmpty() ? "" : " (" + sale.phoneModel + ")"));
        tvProduct.setTextSize(16);
        tvProduct.setPadding(0, 0, 0, 16);
        layout.addView(tvProduct);

        TextView tvAmountLabel = new TextView(this);
        tvAmountLabel.setText("Сумма:");
        tvAmountLabel.setTextSize(14);
        layout.addView(tvAmountLabel);

        EditText etAmount = new EditText(this);
        etAmount.setHint("Введите сумму");
        etAmount.setText(String.valueOf((int) sale.amount));
        etAmount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(etAmount);

        builder.setView(layout);
        builder.setPositiveButton("СОХРАНИТЬ", (dialog, which) -> {
            String newAmount = etAmount.getText().toString();
            if (!newAmount.isEmpty()) {
                updateSale(sale.id, Double.parseDouble(newAmount), position);
            } else {
                Toast.makeText(this, "Введите сумму", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("ОТМЕНА", null);
        builder.show();
    }

    private void updateSale(int id, double newAmount, int position) {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Сохранение...");
        progress.show();

        new Thread(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("id", id);
                json.put("amount", newAmount);

                URL url = new URL(ApiClient.BASE_URL + "update_sale.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                runOnUiThread(() -> {
                    progress.dismiss();
                    if (responseCode == 200) {
                        saleList.get(position).amount = newAmount;
                        adapter.notifyItemChanged(position);
                        Toast.makeText(EditSalesActivity.this, "Сохранено", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(EditSalesActivity.this, "Ошибка сохранения", Toast.LENGTH_SHORT).show();
                    }
                });
                conn.disconnect();
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(EditSalesActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    // ==================== ADAPTER ====================

    private class SaleAdapter extends RecyclerView.Adapter<SaleAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_edit_sale, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Sale sale = saleList.get(position);
            holder.tvEmployee.setText(sale.employeeName);
            holder.tvProduct.setText(sale.product);
            holder.tvAmount.setText((int) sale.amount + " ₽");
            holder.tvTime.setText(sale.saleTime);

            if (sale.insurance > 0) {
                holder.tvInsurance.setVisibility(View.VISIBLE);
                holder.tvInsurance.setText("страховка: " + sale.insurance + " ₽");
            } else {
                holder.tvInsurance.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(v -> editSale(sale, position));
        }

        @Override
        public int getItemCount() {
            return saleList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvEmployee, tvProduct, tvAmount, tvTime, tvInsurance;
            ViewHolder(View itemView) {
                super(itemView);
                tvEmployee = itemView.findViewById(R.id.tvEmployee);
                tvProduct = itemView.findViewById(R.id.tvProduct);
                tvAmount = itemView.findViewById(R.id.tvAmount);
                tvTime = itemView.findViewById(R.id.tvTime);
                tvInsurance = itemView.findViewById(R.id.tvInsurance);
            }
        }
    }

    // ==================== DATA CLASS ====================

    static class Sale {
        int id;
        String employeeName;
        String product;
        double amount;
        String phoneModel;
        int insurance;
        String saleTime;
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