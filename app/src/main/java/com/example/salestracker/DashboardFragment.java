package com.example.salestracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.salestracker.Task;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardFragment extends Fragment {

    private TextView tvEmployeeName, tvAvatar, tvOfficeStatus;
    private GridLayout gridMetrics;
    private LinearLayout containerTasks;
    private TextView btnAddTask;
    private SwipeRefreshLayout swipeRefresh;
    private ApiClient apiClient;
    private int currentOfficeId;
    private String currentEmployee;
    private String currentUserRole;
    private int currentYear, currentMonth, currentDay, daysInMonth;
    private int currentEmployeeId;

    // Категории показателей
    private String[] displayCategories = {"SIM", "Аксессуары", "Товарка", "ШПД", "Адаптеры", "Финансовые услуги"};
    private String[] dbCategories = {"SIM", "Аксессуары", "Товарная выручка", "ШПД", "Адаптеры", "Финансовые услуги"};
    private String[] units = {"шт", "₽", "₽", "шт", "шт", "₽"};

    private Map<String, Integer> monthlyPlans = new HashMap<>();
    private Map<String, Integer> todaySales = new HashMap<>();
    private Map<String, Integer> monthSales = new HashMap<>();
    private List<Task> taskList = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        initViews(view);
        loadUserData();
        setupClickListeners();
        loadDashboardData();
        loadTasks();

        return view;
    }

    private void initViews(View view) {
        tvEmployeeName = view.findViewById(R.id.tvEmployeeName);
        tvAvatar = view.findViewById(R.id.tvAvatarDashboard);
        tvOfficeStatus = view.findViewById(R.id.tvOfficeStatus);
        gridMetrics = view.findViewById(R.id.gridMetrics);
        containerTasks = view.findViewById(R.id.containerTasks);
        btnAddTask = view.findViewById(R.id.btnAddTask);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);

        tvAvatar.setOnClickListener(v -> showExitMenu());

        view.findViewById(R.id.btnQuickSales).setOnClickListener(v -> navigateToSales());
        view.findViewById(R.id.btnQuickSchedule).setOnClickListener(v -> navigateToSchedule());
        view.findViewById(R.id.btnQuickEmployees).setOnClickListener(v -> navigateToEmployees());
        view.findViewById(R.id.btnQuickReports).setOnClickListener(v -> navigateToReports());
        view.findViewById(R.id.btnQuickRating).setOnClickListener(v -> navigateToRating());
        view.findViewById(R.id.btnQuickTasks).setOnClickListener(v -> navigateToTasks());

        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(() -> refreshData());
        }
    }

    private void loadUserData() {
        apiClient = new ApiClient();
        SharedPreferences prefs = requireActivity().getSharedPreferences("app", Context.MODE_PRIVATE);
        currentEmployee = prefs.getString("employee_name", "Анна");
        currentOfficeId = prefs.getInt("office_id", 0);
        currentUserRole = prefs.getString("user_role", "seller");
        currentEmployeeId = prefs.getInt("employee_id", 0);

        String firstName = currentEmployee.split(" ")[0];
        tvEmployeeName.setText("Добрый день, " + firstName);
        tvAvatar.setText(firstName.substring(0, 1));

        Calendar cal = Calendar.getInstance();
        currentYear = cal.get(Calendar.YEAR);
        currentMonth = cal.get(Calendar.MONTH) + 1;
        currentDay = cal.get(Calendar.DAY_OF_MONTH);
        daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    private void refreshData() {
        loadDashboardData();
        loadTasks();
        checkOfficeStatus();
        if (swipeRefresh != null) {
            swipeRefresh.setRefreshing(false);
        }
        Toast.makeText(getContext(), "Данные обновлены", Toast.LENGTH_SHORT).show();
    }

    private void loadDashboardData() {
        apiClient.getPlans(currentYear, currentMonth, currentOfficeId, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    if (obj.getString("status").equals("success")) {
                        JSONObject plansObj = obj.getJSONObject("plans");
                        monthlyPlans.clear();

                        for (String dbCategory : dbCategories) {
                            int value = plansObj.optInt(dbCategory, 0);
                            monthlyPlans.put(dbCategory, value);
                        }
                        loadMonthSales();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showStubData();
                }
            }
            @Override
            public void onError(String error) {
                showStubData();
            }
        });
    }

    private void loadMonthSales() {
        apiClient.getMonthSales(currentOfficeId, currentYear, currentMonth, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    if (obj.getString("status").equals("success")) {
                        JSONObject salesObj = obj.getJSONObject("sales");
                        monthSales.clear();
                        for (String dbCategory : dbCategories) {
                            int value = salesObj.optInt(dbCategory, 0);
                            monthSales.put(dbCategory, value);
                        }
                        loadTodaySales();
                    }
                } catch (Exception e) {
                    loadTodaySales();
                }
            }
            @Override
            public void onError(String error) {
                loadTodaySales();
            }
        });
    }

    private void loadTodaySales() {
        apiClient.getTodaySales(currentOfficeId, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    if (obj.getString("status").equals("success")) {
                        JSONArray salesArray = obj.getJSONArray("sales");

                        todaySales.clear();
                        for (String dbCategory : dbCategories) {
                            todaySales.put(dbCategory, 0);
                        }

                        int accessoriesAmount = 0;
                        int phonesAmount = 0;

                        for (int i = 0; i < salesArray.length(); i++) {
                            JSONObject sale = salesArray.getJSONObject(i);
                            String product = sale.getString("product");
                            double amount = sale.getDouble("amount");

                            switch (product) {
                                case "СИМ":
                                    todaySales.put("SIM", todaySales.get("SIM") + 1);
                                    break;
                                case "Аксессуар":
                                    accessoriesAmount += (int)amount;
                                    todaySales.put("Аксессуары", accessoriesAmount);
                                    break;
                                case "Телефон":
                                    phonesAmount += (int)amount;
                                    break;
                                case "ШПД":
                                    todaySales.put("ШПД", todaySales.get("ШПД") + 1);
                                    break;
                                case "Адаптер":
                                    todaySales.put("Адаптеры", todaySales.get("Адаптеры") + 1);
                                    break;
                                case "Финансовые услуги":
                                    todaySales.put("Финансовые услуги", todaySales.get("Финансовые услуги") + (int)amount);
                                    break;
                            }
                        }

                        int productRevenue = accessoriesAmount + phonesAmount;
                        todaySales.put("Товарная выручка", productRevenue);

                        displayMetrics();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showStubData();
                }
            }
            @Override
            public void onError(String error) {
                showStubData();
            }
        });
    }

    private void displayMetrics() {
        gridMetrics.removeAllViews();
        gridMetrics.setColumnCount(2);

        for (int i = 0; i < displayCategories.length; i++) {
            String displayCategory = displayCategories[i];
            String dbCategory = dbCategories[i];

            int monthlyPlan = monthlyPlans.getOrDefault(dbCategory, 0);
            int soldToday = todaySales.getOrDefault(dbCategory, 0);
            int soldMonth = monthSales.getOrDefault(dbCategory, 0);

            int daysLeft = daysInMonth - currentDay + 1;
            if (daysLeft < 1) daysLeft = 1;

            int remainingPlan = monthlyPlan - soldMonth;
            if (remainingPlan < 0) remainingPlan = 0;

            int dailyPlan = remainingPlan / daysLeft;
            if (dailyPlan < 1 && monthlyPlan > 0) dailyPlan = 1;

            int percent = dailyPlan > 0 ? (soldToday * 100 / dailyPlan) : 0;
            if (percent > 100) percent = 100;

            int delta;
            if (dailyPlan > 0) {
                if (soldToday == 0) {
                    delta = -100;
                } else {
                    delta = Math.round((float) (soldToday - dailyPlan) / dailyPlan * 100);
                }
            } else {
                delta = 0;
            }

            View card = LayoutInflater.from(getContext()).inflate(R.layout.item_metric_card, null);

            TextView tvIcon = card.findViewById(R.id.tvIcon);
            TextView tvValue = card.findViewById(R.id.tvValue);
            TextView tvUnit = card.findViewById(R.id.tvUnit);
            TextView tvTitle = card.findViewById(R.id.tvTitle);
            TextView tvDelta = card.findViewById(R.id.tvDelta);
            ProgressBar progressBar = card.findViewById(R.id.progressBar);

            String icon = getIconForCategory(displayCategory);
            tvIcon.setText(icon);
            tvValue.setText(soldToday + "/" + dailyPlan);
            tvUnit.setText(units[i]);
            tvTitle.setText(displayCategory);

            if (delta == -100) {
                tvDelta.setText("▼ 100%");
                tvDelta.setTextColor(0xFFEF4444);
            } else if (delta >= 0) {
                tvDelta.setText("▲ +" + delta + "%");
                tvDelta.setTextColor(0xFF10B981);
            } else {
                tvDelta.setText("▼ " + Math.abs(delta) + "%");
                tvDelta.setTextColor(0xFFEF4444);
            }

            progressBar.setProgress(percent);

            if (soldToday >= dailyPlan && dailyPlan > 0) {
                tvValue.setTextColor(0xFF10B981);
            } else {
                tvValue.setTextColor(0xFF333333);
            }

            addCardToGrid(card);
        }

        displayTasks();
        checkOfficeStatus();
        setupStatusClickListener();
    }

    private String getIconForCategory(String category) {
        switch (category) {
            case "SIM": return "📞";
            case "Аксессуары": return "🎧";
            case "Товарка": return "📱";
            case "ШПД": return "📶";
            case "Адаптеры": return "🔌";
            case "Финансовые услуги": return "💰";
            default: return "📊";
        }
    }

    private void addCardToGrid(View card) {
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(6, 6, 6, 6);
        card.setLayoutParams(params);
        gridMetrics.addView(card);
    }

    // ==================== ЗАДАЧИ ====================

    private void loadTasks() {
        apiClient.getTasks(currentOfficeId, currentUserRole, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    JSONArray arr = obj.getJSONArray("tasks");
                    taskList.clear();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject task = arr.getJSONObject(i);
                        Task t = new Task(
                                task.getInt("id"),
                                task.getString("title"),
                                task.optString("deadline", ""),
                                task.getString("priority")
                        );
                        taskList.add(t);
                    }
                    displayTasks();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onError(String error) {}
        });
    }

    private void displayTasks() {
        containerTasks.removeAllViews();

        if (taskList.isEmpty()) {
            TextView tvEmpty = new TextView(getContext());
            tvEmpty.setText("Нет задач");
            tvEmpty.setTextSize(12);
            tvEmpty.setTextColor(0xFF999999);
            tvEmpty.setPadding(16, 16, 16, 16);
            tvEmpty.setGravity(Gravity.CENTER);
            containerTasks.addView(tvEmpty);
            return;
        }

        for (Task task : taskList) {
            View taskView = LayoutInflater.from(getContext()).inflate(R.layout.item_task_card, containerTasks, false);

            View colorStrip = taskView.findViewById(R.id.colorStrip);
            TextView tvTitle = taskView.findViewById(R.id.tvTitle);
            TextView tvDeadline = taskView.findViewById(R.id.tvDeadline);
            CheckBox cbComplete = taskView.findViewById(R.id.cbComplete);

            int color;
            switch (task.getPriority()) {
                case "urgent": color = 0xFFEF4444; break;
                case "important": color = 0xFFF59E0B; break;
                case "normal": color = 0xFF9CA3AF; break;
                case "done": color = 0xFF10B981; break;
                default: color = 0xFF9CA3AF;
            }

            colorStrip.setBackgroundColor(color);
            tvTitle.setText(task.getTitle());

            String deadlineText = task.getDeadline();
            if (deadlineText != null && !deadlineText.isEmpty()) {
                tvDeadline.setText("до " + formatDate(deadlineText));
            } else {
                tvDeadline.setText("");
            }

            if (task.getPriority().equals("done")) {
                cbComplete.setChecked(true);
                taskView.setAlpha(0.5f);
            }

            cbComplete.setOnCheckedChangeListener((buttonView, isChecked) -> {
                String newPriority = isChecked ? "done" : "normal";
                apiClient.updateTask(task.getId(), newPriority, new ApiClient.ApiCallback() {
                    @Override
                    public void onSuccess(String response) {
                        task.setPriority(newPriority);
                        loadTasks();
                    }
                    @Override
                    public void onError(String error) {}
                });
            });

            containerTasks.addView(taskView);
        }
    }

    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_task, null);

        EditText etTitle = view.findViewById(R.id.etTaskTitle);
        EditText etDeadline = view.findViewById(R.id.etTaskDeadline);
        Spinner spinnerPriority = view.findViewById(R.id.spinnerPriority);

        etDeadline.setHint("ДД.ММ.ГГГГ (необязательно)");

        String[] priorities = {"normal", "important", "urgent"};
        String[] priorityNames = {"Обычная", "Важная", "Срочная"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, priorityNames);
        spinnerPriority.setAdapter(adapter);

        builder.setTitle("Добавить задачу")
                .setView(view)
                .setPositiveButton("Добавить", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String deadline = etDeadline.getText().toString().trim();
                    String priority = priorities[spinnerPriority.getSelectedItemPosition()];

                    if (!title.isEmpty()) {
                        String deadlineFormatted = deadline.isEmpty() ? null : convertToServerDate(deadline);
                        apiClient.addTask(currentOfficeId, title, deadlineFormatted, priority, currentEmployeeId, new ApiClient.ApiCallback() {
                            @Override
                            public void onSuccess(String response) {
                                try {
                                    JSONObject obj = new JSONObject(response);
                                    if (obj.getString("status").equals("success")) {
                                        JSONArray arr = obj.getJSONArray("tasks");
                                        taskList.clear();
                                        for (int i = 0; i < arr.length(); i++) {
                                            JSONObject task = arr.getJSONObject(i);
                                            Task t = new Task(
                                                    task.getInt("id"),
                                                    task.getString("title"),
                                                    task.optString("deadline", ""),
                                                    task.getString("priority")
                                            );
                                            taskList.add(t);
                                        }
                                        displayTasks();
                                        Toast.makeText(getContext(), "✅ Задача добавлена", Toast.LENGTH_SHORT).show();
                                    }
                                } catch (Exception e) {
                                    loadTasks();
                                }
                            }
                            @Override
                            public void onError(String error) {
                                Toast.makeText(getContext(), "❌ Ошибка: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(getContext(), "Введите название задачи", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private String formatDate(String date) {
        if (date == null || date.isEmpty()) return "";
        try {
            String[] parts = date.split("-");
            if (parts.length == 3) {
                return parts[2] + "." + parts[1] + "." + parts[0];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return date;
    }

    private String convertToServerDate(String date) {
        if (date == null || date.isEmpty()) return null;
        try {
            String[] parts = date.split("\\.");
            if (parts.length == 3) {
                return parts[2] + "-" + parts[1] + "-" + parts[0];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // ==================== НАВИГАЦИЯ ====================

    private void showStubData() {
        gridMetrics.removeAllViews();
        gridMetrics.setColumnCount(2);

        String[][] stubMetrics = {
                {"📞", "2/4", "шт", "SIM", "▼50%", "50"},
                {"🎧", "500/1000", "₽", "Аксессуары", "▼50%", "50"},
                {"📱", "8000/16667", "₽", "Товарка", "▼52%", "48"},
                {"📶", "0/2", "шт", "ШПД", "▼100%", "0"},
                {"🔌", "1/1", "шт", "Адаптеры", "▲0%", "100"},
                {"💰", "1200/2667", "₽", "Финансовые услуги", "▼55%", "45"}
        };

        for (String[] metric : stubMetrics) {
            View card = LayoutInflater.from(getContext()).inflate(R.layout.item_metric_card, null);

            TextView tvIcon = card.findViewById(R.id.tvIcon);
            TextView tvValue = card.findViewById(R.id.tvValue);
            TextView tvUnit = card.findViewById(R.id.tvUnit);
            TextView tvTitle = card.findViewById(R.id.tvTitle);
            TextView tvDelta = card.findViewById(R.id.tvDelta);
            ProgressBar progressBar = card.findViewById(R.id.progressBar);

            tvIcon.setText(metric[0]);
            tvValue.setText(metric[1]);
            tvUnit.setText(metric[2]);
            tvTitle.setText(metric[3]);
            tvDelta.setText(metric[4]);
            if (metric[4].contains("▼")) {
                tvDelta.setTextColor(0xFFEF4444);
            } else {
                tvDelta.setTextColor(0xFF10B981);
            }
            progressBar.setProgress(Integer.parseInt(metric[5]));

            addCardToGrid(card);
        }

        displayTasks();
    }

    private void showExitMenu() {
        new AlertDialog.Builder(getContext())
                .setTitle("Выход")
                .setMessage("Вы уверены, что хотите выйти?")
                .setPositiveButton("Выйти", (dialog, which) -> {
                    SharedPreferences prefs = requireActivity().getSharedPreferences("app", Context.MODE_PRIVATE);
                    prefs.edit().clear().apply();

                    Intent intent = new Intent(getContext(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void setupClickListeners() {
        btnAddTask.setOnClickListener(v -> {
            if (currentUserRole.equals("owner") || currentUserRole.equals("rgo") || currentUserRole.equals("dm")) {
                showAddTaskDialog();
            } else {
                Toast.makeText(getContext(), "Нет прав для добавления задач", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToSales() {
        Intent intent = new Intent(getContext(), SalesActivity.class);
        startActivity(intent);
    }

    private void navigateToSchedule() {
        Intent intent = new Intent(getContext(), ScheduleActivity.class);
        startActivity(intent);
    }

    private void navigateToEmployees() {
        if (currentUserRole.equals("owner") || currentUserRole.equals("rgo")) {
            Intent intent = new Intent(getContext(), ShopsActivity.class);
            startActivity(intent);
        } else {
            Intent intent = new Intent(getContext(), ShopEmployeesActivity.class);
            intent.putExtra("shop_id", currentOfficeId);
            intent.putExtra("shop_name", getOfficeName());
            startActivity(intent);
        }
    }

    private void navigateToReports() {
        Intent intent = new Intent(getContext(), PlanActivity.class);
        startActivity(intent);
    }

    private void navigateToRating() {
        Intent intent = new Intent(getContext(), RatingActivity.class);
        startActivity(intent);
    }

    private void navigateToTasks() {
        Toast.makeText(getContext(), "Задачи", Toast.LENGTH_SHORT).show();
    }

    // ==================== СТАТУС ОФИСА ====================

    private void checkOfficeStatus() {
        if (currentUserRole.equals("owner") || currentUserRole.equals("rgo")) {
            if (tvOfficeStatus != null) {
                tvOfficeStatus.setVisibility(View.GONE);
            }
            return;
        }

        if (currentUserRole.equals("dm") || currentUserRole.equals("seller") || currentUserRole.equals("senior_seller")) {
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

            apiClient.getOfficeStatus(currentOfficeId, today, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    try {
                        JSONObject obj = new JSONObject(response);
                        boolean isOpen = obj.optBoolean("is_open", false);
                        updateOfficeStatusUI(isOpen, false);

                        if (currentUserRole.equals("dm") && !isOpen) {
                            OpenOfficeDialog.showIfNeeded(getContext(), currentOfficeId, currentEmployee,
                                    () -> updateOfficeStatusUI(true, false));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        updateOfficeStatusUI(false, false);
                    }
                }
                @Override
                public void onError(String error) {
                    updateOfficeStatusUI(false, false);
                }
            });
        }
    }

    private void updateOfficeStatusUI(boolean isOpen, boolean isOwnerMode) {
        if (getView() != null && tvOfficeStatus != null) {
            tvOfficeStatus.setVisibility(View.VISIBLE);

            if (isOwnerMode) {
                tvOfficeStatus.setText("🟢 Открытые смены");
                tvOfficeStatus.setTextColor(0xFF4CAF50);
            } else {
                if (isOpen) {
                    tvOfficeStatus.setText("🟢 Офис открыт");
                    tvOfficeStatus.setTextColor(0xFF4CAF50);
                } else {
                    tvOfficeStatus.setText("🔴 Офис закрыт");
                    tvOfficeStatus.setTextColor(0xFFF44336);
                }
            }
        }
    }

    private void setupStatusClickListener() {
        if (tvOfficeStatus != null) {
            tvOfficeStatus.setOnClickListener(v -> {
                if (currentUserRole.equals("owner") || currentUserRole.equals("rgo")) {
                    return;
                } else {
                    String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                    apiClient.getOfficeStatus(currentOfficeId, today, new ApiClient.ApiCallback() {
                        @Override
                        public void onSuccess(String response) {
                            try {
                                JSONObject obj = new JSONObject(response);
                                boolean isOpen = obj.optBoolean("is_open", false);

                                if (!isOpen) {
                                    OpenOfficeDialog.showIfNeeded(getContext(), currentOfficeId, currentEmployee,
                                            () -> {
                                                updateOfficeStatusUI(true, false);
                                                Toast.makeText(getContext(), "Офис открыт!", Toast.LENGTH_SHORT).show();
                                            });
                                } else {
                                    Toast.makeText(getContext(), "Офис уже открыт", Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        @Override
                        public void onError(String error) {
                            Toast.makeText(getContext(), "Ошибка: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        }
    }

    private String getOfficeName() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("app", Context.MODE_PRIVATE);
        return prefs.getString("office_name", "Мой офис");
    }
}