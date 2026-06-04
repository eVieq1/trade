package com.example.salestracker;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ShopsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ShopAdapter adapter;
    private List<Shop> shopList = new ArrayList<>();
    private String currentUserRole;
    private ApiClient apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shops);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Магазины");

        apiClient = new ApiClient();

        recyclerView = findViewById(R.id.recyclerViewShops);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ShopAdapter();
        recyclerView.setAdapter(adapter);

        SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
        currentUserRole = prefs.getString("user_role", "seller");

        if (currentUserRole.equals("owner")) {
            findViewById(R.id.btnAddShop).setVisibility(View.VISIBLE);
            findViewById(R.id.btnAddShop).setOnClickListener(v -> showAddShopDialog());
        } else {
            findViewById(R.id.btnAddShop).setVisibility(View.GONE);
        }

        if (currentUserRole.equals("owner")) {
            setupSwipeToDelete();
        }

        loadShops();
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
                Shop shop = shopList.get(position);

                new AlertDialog.Builder(ShopsActivity.this)
                        .setTitle("Удалить магазин")
                        .setMessage("Вы уверены, что хотите удалить магазин " + shop.name + "?")
                        .setPositiveButton("Удалить", (dialog, which) -> deleteShop(shop.id, position))
                        .setNegativeButton("Отмена", (dialog, which) -> adapter.notifyItemChanged(position))
                        .show();
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (currentUserRole.equals("owner") || currentUserRole.equals("rgo")) {
            menu.add(0, 1, 0, "Сотрудники");
            menu.add(0, 2, 1, "Выход");
        } else {
            menu.add(0, 2, 0, "Выход");
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == 1) {
            startActivity(new Intent(ShopsActivity.this, EmployeesActivity.class));
            return true;
        } else if (id == 2) {
            finishAffinity();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadShops() {
        apiClient.getShops(new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    if (obj.getString("status").equals("success")) {
                        JSONArray arr = obj.getJSONArray("shops");
                        shopList.clear();
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject shop = arr.getJSONObject(i);
                            shopList.add(new Shop(
                                    shop.getInt("id"),
                                    shop.getString("name"),
                                    shop.getString("address")
                            ));
                        }
                        adapter.notifyDataSetChanged();
                    }
                } catch (Exception e) {
                    Toast.makeText(ShopsActivity.this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(ShopsActivity.this, "Ошибка: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddShopDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        EditText etName = new EditText(this);
        etName.setHint("Название магазина");
        etName.setPadding(16, 16, 16, 16);
        layout.addView(etName);

        EditText etAddress = new EditText(this);
        etAddress.setHint("Адрес");
        etAddress.setPadding(16, 16, 16, 16);
        layout.addView(etAddress);

        builder.setTitle("Добавить магазин")
                .setView(layout)
                .setPositiveButton("Добавить", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String address = etAddress.getText().toString().trim();
                    if (!name.isEmpty()) {
                        addShop(name, address);
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void addShop(String name, String address) {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Добавление...");
        progress.show();

        new Thread(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("name", name);
                json.put("address", address);

                URL url = new URL(ApiClient.BASE_URL + "add_shop.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                runOnUiThread(() -> {
                    progress.dismiss();
                    if (responseCode == 200) {
                        Toast.makeText(ShopsActivity.this, "Магазин добавлен", Toast.LENGTH_SHORT).show();
                        loadShops();
                    } else {
                        Toast.makeText(ShopsActivity.this, "Ошибка сервера: " + responseCode, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(ShopsActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void deleteShop(int id, int position) {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Удаление...");
        progress.show();

        new Thread(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("id", id);

                URL url = new URL(ApiClient.BASE_URL + "delete_shop.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                runOnUiThread(() -> {
                    progress.dismiss();
                    if (responseCode == 200) {
                        Toast.makeText(ShopsActivity.this, "Магазин удалён", Toast.LENGTH_SHORT).show();
                        loadShops();
                    } else {
                        Toast.makeText(ShopsActivity.this, "Ошибка сервера: " + responseCode, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(ShopsActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void updateShop(int id, String name, String address, int position) {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Сохранение...");
        progress.show();

        new Thread(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("id", id);
                json.put("name", name);
                json.put("address", address);

                URL url = new URL(ApiClient.BASE_URL + "update_shop.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                runOnUiThread(() -> {
                    progress.dismiss();
                    if (responseCode == 200) {
                        Toast.makeText(ShopsActivity.this, "Магазин обновлён", Toast.LENGTH_SHORT).show();
                        loadShops();
                    } else {
                        Toast.makeText(ShopsActivity.this, "Ошибка сервера: " + responseCode, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(ShopsActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void editShop(Shop shop, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        EditText etName = new EditText(this);
        etName.setHint("Название магазина");
        etName.setText(shop.name);
        etName.setPadding(16, 16, 16, 16);
        layout.addView(etName);

        EditText etAddress = new EditText(this);
        etAddress.setHint("Адрес");
        etAddress.setText(shop.address);
        etAddress.setPadding(16, 16, 16, 16);
        layout.addView(etAddress);

        builder.setTitle("Редактировать магазин");
        builder.setView(layout);
        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String newName = etName.getText().toString().trim();
            String newAddress = etAddress.getText().toString().trim();
            if (!newName.isEmpty()) {
                updateShop(shop.id, newName, newAddress, position);
            }
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void showShopEmployees(Shop shop) {
        Intent intent = new Intent(ShopsActivity.this, ShopEmployeesActivity.class);
        intent.putExtra("shop_id", shop.id);
        intent.putExtra("shop_name", shop.name);
        startActivity(intent);
    }

    private class ShopAdapter extends RecyclerView.Adapter<ShopAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shop, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Shop shop = shopList.get(position);
            holder.tvName.setText(shop.name);
            holder.tvAddress.setText(shop.address);

            holder.itemView.setOnClickListener(v -> showShopEmployees(shop));

            if (currentUserRole.equals("owner") || currentUserRole.equals("rgo")) {
                holder.itemView.setOnLongClickListener(v -> {
                    editShop(shop, position);
                    return true;
                });
            }
        }

        @Override
        public int getItemCount() { return shopList.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvAddress;
            ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvShopName);
                tvAddress = itemView.findViewById(R.id.tvShopAddress);
            }
        }
    }

    static class Shop {
        int id;
        String name, address;
        Shop(int id, String name, String address) {
            this.id = id;
            this.name = name;
            this.address = address;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}