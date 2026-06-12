package com.example.salestracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class EmployeePlanFragment extends Fragment {

    private int employeeId;
    private String employeeName;
    private String userRole;

    private ApiClient apiClient;
    private String currentUserRole;
    private int currentOfficeId;
    private String currentEmployee;
    private int currentEmployeeId;

    private LinearLayout layoutEmployeeSelector;
    private Spinner spinnerEmployee;
    private TextView btnWeek, btnMonth;
    private RecyclerView recyclerViewWeek;
    private ScrollView scrollViewMonth;
    private GridLayout gridMonth;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    private List<OfficePlan> planList = new ArrayList<>();
    private List<EmployeeItem> employeeList = new ArrayList<>();
    private boolean isWeekMode = true;
    private int selectedEmployeeId;
    private int currentYear, currentMonth;

    public static EmployeePlanFragment newInstance(int employeeId, String employeeName, String userRole) {
        EmployeePlanFragment fragment = new EmployeePlanFragment();
        Bundle args = new Bundle();
        args.putInt("employee_id", employeeId);
        args.putString("employee_name", employeeName);
        args.putString("user_role", userRole);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            employeeId = getArguments().getInt("employee_id");
            employeeName = getArguments().getString("employee_name");
            userRole = getArguments().getString("user_role");
        }
        apiClient = new ApiClient();

        Calendar cal = Calendar.getInstance();
        currentYear = cal.get(Calendar.YEAR);
        currentMonth = cal.get(Calendar.MONTH) + 1;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_employee_plan, container, false);

        initViews(view);
        loadUserData();
        setupListeners();
        loadEmployees();

        btnWeek.setBackgroundResource(R.drawable.bg_period_selected);
        btnWeek.setTextColor(0xFFFFFFFF);
        btnMonth.setBackgroundResource(R.drawable.bg_period_unselected);
        btnMonth.setTextColor(0xFF666666);

        return view;
    }

    private void initViews(View view) {
        layoutEmployeeSelector = view.findViewById(R.id.layoutEmployeeSelector);
        spinnerEmployee = view.findViewById(R.id.spinnerEmployee);
        btnWeek = view.findViewById(R.id.btnWeek);
        btnMonth = view.findViewById(R.id.btnMonth);
        recyclerViewWeek = view.findViewById(R.id.recyclerViewWeek);
        scrollViewMonth = view.findViewById(R.id.scrollViewMonth);
        gridMonth = view.findViewById(R.id.gridMonth);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmpty = view.findViewById(R.id.tvEmpty);

        recyclerViewWeek.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void loadUserData() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("app", Context.MODE_PRIVATE);
        currentUserRole = prefs.getString("user_role", "seller");
        currentOfficeId = prefs.getInt("office_id", 0);
        currentEmployee = prefs.getString("employee_name", "");
        currentEmployeeId = prefs.getInt("employee_id", 0);

        if (currentUserRole.equals("owner") || currentUserRole.equals("rgo") || currentUserRole.equals("dm")) {
            layoutEmployeeSelector.setVisibility(View.VISIBLE);
        } else {
            layoutEmployeeSelector.setVisibility(View.GONE);
        }
    }

    private void setupListeners() {
        btnWeek.setOnClickListener(v -> switchToWeekMode());
        btnMonth.setOnClickListener(v -> switchToMonthMode());

        if (spinnerEmployee != null) {
            spinnerEmployee.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position > 0) {
                        selectedEmployeeId = employeeList.get(position - 1).id;
                        loadPlanData();
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }
    }

    private void loadEmployees() {
        apiClient.getEmployees(new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    JSONArray arr = obj.getJSONArray("employees");
                    employeeList.clear();

                    List<String> employeeNames = new ArrayList<>();
                    employeeNames.add("Выберите сотрудника");

                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject emp = arr.getJSONObject(i);
                        int officeId = emp.optInt("office_id", 0);
                        if (currentUserRole.equals("dm") && officeId != currentOfficeId) continue;

                        employeeList.add(new EmployeeItem(emp.getInt("id"), emp.getString("name")));
                        employeeNames.add(emp.getString("name"));
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                            android.R.layout.simple_spinner_dropdown_item, employeeNames);
                    spinnerEmployee.setAdapter(adapter);

                    // АВТОМАТИЧЕСКИ ВЫБИРАЕМ ТЕКУЩЕГО СОТРУДНИКА
                    selectCurrentEmployee();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onError(String error) {}
        });
    }

    private void selectCurrentEmployee() {
        if (employeeList.isEmpty()) return;

        int positionToSelect = -1;

        // Сначала пробуем по ID
        if (currentEmployeeId > 0) {
            for (int i = 0; i < employeeList.size(); i++) {
                if (employeeList.get(i).id == currentEmployeeId) {
                    positionToSelect = i + 1;
                    break;
                }
            }
        }

        // Если по ID не нашли, пробуем по имени
        if (positionToSelect == -1 && currentEmployee != null && !currentEmployee.isEmpty()) {
            for (int i = 0; i < employeeList.size(); i++) {
                if (employeeList.get(i).name.equals(currentEmployee)) {
                    positionToSelect = i + 1;
                    currentEmployeeId = employeeList.get(i).id;
                    // Сохраняем ID для будущего использования
                    SharedPreferences prefs = requireActivity().getSharedPreferences("app", Context.MODE_PRIVATE);
                    prefs.edit().putInt("employee_id", currentEmployeeId).apply();
                    break;
                }
            }
        }

        // Если нашли, выбираем и загружаем данные
        if (positionToSelect > 0) {
            spinnerEmployee.setSelection(positionToSelect);
            selectedEmployeeId = employeeList.get(positionToSelect - 1).id;
            loadPlanData();
        }
    }

    private void loadPlanData() {
        if (selectedEmployeeId == 0) return;

        progressBar.setVisibility(View.VISIBLE);
        planList.clear();

        String period = isWeekMode ? "week" : "month";

        apiClient.getEmployeePlanData(selectedEmployeeId, currentYear, currentMonth, period, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    if (obj.getString("status").equals("success")) {
                        JSONArray data = obj.getJSONArray("data");
                        planList.clear();

                        for (int i = 0; i < data.length(); i++) {
                            JSONObject item = data.getJSONObject(i);
                            planList.add(new OfficePlan(
                                    item.getString("category"),
                                    item.getDouble("target"),
                                    item.getDouble("fact"),
                                    item.getString("unit")
                            ));
                        }
                    }
                    displayData();
                    progressBar.setVisibility(View.GONE);
                } catch (Exception e) {
                    e.printStackTrace();
                    progressBar.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    private void displayData() {
        if (planList == null || planList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerViewWeek.setVisibility(View.GONE);
            scrollViewMonth.setVisibility(View.GONE);
            return;
        }

        tvEmpty.setVisibility(View.GONE);

        if (isWeekMode) {
            recyclerViewWeek.setVisibility(View.VISIBLE);
            scrollViewMonth.setVisibility(View.GONE);

            if (recyclerViewWeek.getAdapter() == null) {
                OfficePlanWeekAdapter adapter = new OfficePlanWeekAdapter(planList);
                recyclerViewWeek.setAdapter(adapter);
            } else {
                ((OfficePlanWeekAdapter) recyclerViewWeek.getAdapter()).notifyDataSetChanged();
            }
        } else {
            recyclerViewWeek.setVisibility(View.GONE);
            scrollViewMonth.setVisibility(View.VISIBLE);
            gridMonth.removeAllViews();

            String[] icons = {"📞", "🎧", "📱", "📶", "🔌", "💰"};
            String[] titles = {"SIM", "Аксессуары", "Товарная выручка", "ШПД", "Адаптеры", "Финансовые услуги"};

            for (int i = 0; i < planList.size(); i++) {
                OfficePlan plan = planList.get(i);
                View card = LayoutInflater.from(getContext()).inflate(R.layout.item_plan_month, null);

                TextView tvIcon = card.findViewById(R.id.tvIcon);
                TextView tvTitle = card.findViewById(R.id.tvTitle);
                TextView tvTarget = card.findViewById(R.id.tvTarget);
                TextView tvFact = card.findViewById(R.id.tvFact);
                TextView tvPercent = card.findViewById(R.id.tvPercent);
                ProgressBar progressBarCard = card.findViewById(R.id.progressBar);

                tvIcon.setText(icons[i % icons.length]);
                tvTitle.setText(titles[i]);
                tvTarget.setText("План: " + formatNumber(plan.getTarget()) + " " + plan.getUnit());
                tvFact.setText("Факт: " + formatNumber(plan.getFact()) + " " + plan.getUnit());
                int percent = plan.getPercent();
                tvPercent.setText(percent + "%");
                progressBarCard.setProgress(percent);

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 0;
                params.height = GridLayout.LayoutParams.WRAP_CONTENT;
                params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
                params.setMargins(8, 8, 8, 8);
                card.setLayoutParams(params);
                gridMonth.addView(card);
            }
        }
    }

    private void switchToWeekMode() {
        isWeekMode = true;
        btnWeek.setBackgroundResource(R.drawable.bg_period_selected);
        btnWeek.setTextColor(0xFFFFFFFF);
        btnMonth.setBackgroundResource(R.drawable.bg_period_unselected);
        btnMonth.setTextColor(0xFF666666);
        loadPlanData();
    }

    private void switchToMonthMode() {
        isWeekMode = false;
        btnMonth.setBackgroundResource(R.drawable.bg_period_selected);
        btnMonth.setTextColor(0xFFFFFFFF);
        btnWeek.setBackgroundResource(R.drawable.bg_period_unselected);
        btnWeek.setTextColor(0xFF666666);
        loadPlanData();
    }

    private String formatNumber(double value) {
        if (value >= 1000000) return String.format("%.1fM", value / 1000000);
        if (value >= 1000) return String.format("%.1fK", value / 1000);
        return String.valueOf((int) value);
    }

    static class EmployeeItem {
        int id;
        String name;
        EmployeeItem(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}