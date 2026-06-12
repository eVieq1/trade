package com.example.salestracker;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.salestracker.fragments.ScheduleFragment;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class OfficeTodayScheduleActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TodayScheduleAdapter adapter;
    private ApiClient apiClient;
    private int officeId;
    private String officeName;
    private TextView tvOfficeName, tvDate, tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_office_today_schedule);

        officeId = getIntent().getIntExtra("office_id", 0);
        officeName = getIntent().getStringExtra("office_name");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(officeName);

        tvOfficeName = findViewById(R.id.tvOfficeName);
        tvDate = findViewById(R.id.tvDate);
        tvEmpty = findViewById(R.id.tvEmpty);
        recyclerView = findViewById(R.id.recyclerViewTodayShifts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        tvOfficeName.setText(officeName);
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        tvDate.setText(sdf.format(Calendar.getInstance().getTime()));

        apiClient = new ApiClient();
        loadTodaySchedule();
    }

    private void loadTodaySchedule() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);

        apiClient.getSchedule(year, month, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    JSONObject schedule = obj.getJSONObject("schedule");
                    String key = String.valueOf(day);

                    List<ScheduleFragment.ShiftData> shifts = new ArrayList<>();

                    if (schedule.has(key)) {
                        JSONArray dayArray = schedule.getJSONArray(key);
                        for (int i = 0; i < dayArray.length(); i++) {
                            JSONObject data = dayArray.getJSONObject(i);
                            shifts.add(new ScheduleFragment.ShiftData(
                                    data.getString("employee"),
                                    data.getString("shift_time")
                            ));
                        }
                    }

                    adapter = new TodayScheduleAdapter(shifts);
                    recyclerView.setAdapter(adapter);

                    if (shifts.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                    }

                } catch (Exception e) {
                    tvEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String error) {
                tvEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}