package com.example.salestracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.salestracker.adapters.OfficeStatusAdapter;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OfficesStatusActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private OfficeStatusAdapter adapter;
    private ApiClient apiClient;
    private List<OfficeStatus> officeList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offices_status);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Открытие офисов");

        apiClient = new ApiClient();

        recyclerView = findViewById(R.id.recyclerViewOffices);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OfficeStatusAdapter(this);
        recyclerView.setAdapter(adapter);

        loadAllOfficesStatus();
    }

    private void loadAllOfficesStatus() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        apiClient.getAllOfficesStatus(today, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    if (obj.getString("status").equals("success")) {
                        JSONArray arr = obj.getJSONArray("offices");

                        officeList.clear();
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject office = arr.getJSONObject(i);

                            int officeId = office.getInt("office_id");
                            String officeName = office.getString("office_name");
                            boolean isOpen = office.getInt("is_open") == 1;
                            String openedBy = office.optString("opened_by", "");
                            String openedAt = office.optString("opened_at", "");

                            officeList.add(new OfficeStatus(officeId, officeName, isOpen, openedBy, openedAt));
                        }
                        adapter.setData(officeList);

                        if (officeList.isEmpty()) {
                            Toast.makeText(OfficesStatusActivity.this, "Нет офисов", Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(OfficesStatusActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(OfficesStatusActivity.this, "Ошибка API: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}