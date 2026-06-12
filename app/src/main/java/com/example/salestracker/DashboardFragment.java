package com.example.salestracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DashboardFragment extends Fragment {

    private TextView tvEmployeeName, tvAvatar, tvOfficeStatus, tvQuickEmployeesText;
    private GridLayout gridMetrics;
    private LinearLayout containerTasks;
    private TextView btnAddTask;
    private ApiClient apiClient;
    private int currentOfficeId;
    private String currentEmployee;
    private String currentUserRole;
    private int currentYear, currentMonth, currentDay, daysInMonth;

    // Категории показателей
    private String[] displayCategories = {"SIM", "Аксессуары", "Товарка", "ШПД", "Адаптеры", "Финансовые услуги"};
    private String[] dbCategories = {"SIM", "Аксессуары", "Товарная выручка", "ШПД", "Адаптеры", "Финансовые услуги"};
    private String[] units = {"шт", "₽", "₽", "шт", "шт", "₽"};

    private Map<String, Integer> monthlyPlans = new HashMap<>();
    private Map<String, Integer> todaySales = new HashMap<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        initViews(view);
        loadUserData();
        setupClickListeners();
        loadDashboardData();
        checkOfficeStatus();
        setupStatusClickListener();

        return view;
    }

    private void initViews(View view) {
        tvEmployeeName = view.findViewById(R.id.tvEmployeeName);
        tvAvatar = view.findViewById(R.id.tvAvatarDashboard);
        tvOfficeStatus = view.findViewById(R.id.tvOfficeStatus);
        tvQuickEmployeesText = view.findViewById(R.id.tvQuickEmployeesText);
        gridMetrics = view.findViewById(R.id.gridMetrics);
        containerTasks = view.findViewById(R.id.containerTasks);
        btnAddTask = view.findViewById(R.id.btnAddTask);

        tvAvatar.setOnClickListener(v -> showExitMenu());

        view.findViewById(R.id.btnQuickSales).setOnClickListener(v -> navigateToSales());
        view.findViewById(R.id.btnQuickSchedule).setOnClickListener(v -> navigateToSchedule());
        view.findViewById(R.id.btnQuickEmployees).setOnClickListener(v -> navigateToEmployees());
        view.findViewById(R.id.btnQuickReports).setOnClickListener(v -> navigateToReports());
        view.findViewById(R.id.btnQuickRating).setOnClickListener(v -> navigateToRating());
        view.findViewById(R.id.btnQuickTasks).setOnClickListener(v -> navigateToTasks());
    }

    private void loadUserData() {
        apiClient = new ApiClient();
        SharedPreferences prefs = requireActivity().getSharedPreferences("app", Context.MODE_PRIVATE);
        currentEmployee = prefs.getString("employee_name", "Анна");
        currentOfficeId = prefs.getInt("office_id", 0);
        currentUserRole = prefs.getString("user_role", "seller");

        String firstName = currentEmployee.split(" ")[0];
        tvEmployeeName.setText("Добрый день, " + firstName);
        tvAvatar.setText(firstName.substring(0, 1));

        // Меняем текст кнопки "Сотрудники" на "Офисы" для Owner/RGO
        if (tvQuickEmployeesText != null) {
            if (currentUserRole.equals("owner") || currentUserRole.equals("rgo")) {
                tvQuickEmployeesText.setText("ОФИСЫ");
            } else {
                tvQuickEmployeesText.setText("СОТРУДНИКИ");
            }
        }

        Calendar cal = Calendar.getInstance();
        currentYear = cal.get(Calendar.YEAR);
        currentMonth = cal.get(Calendar.MONTH) + 1;
        currentDay = cal.get(Calendar.DAY_OF_MONTH);
        daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
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

                        loadTodaySales();
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

            int daysLeft = daysInMonth - currentDay + 1;
            if (daysLeft < 1) daysLeft = 1;

            int remainingPlan = monthlyPlan - (soldToday * currentDay);
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

    private void displayTasks() {
        String[][] tasks = {
                {"Обзвонить 10 клиентов", "до 18:00", "urgent"},
                {"Сдать отчёт по продажам", "до 19:00", "important"},
                {"Подготовить презентацию", "завтра", "normal"},
                {"Провести планёрку", "выполнено", "done"}
        };

        containerTasks.removeAllViews();

        for (String[] task : tasks) {
            View taskView = LayoutInflater.from(getContext()).inflate(R.layout.item_task_card, containerTasks, false);

            View colorStrip = taskView.findViewById(R.id.colorStrip);
            TextView tvTitle = taskView.findViewById(R.id.tvTitle);
            TextView tvDeadline = taskView.findViewById(R.id.tvDeadline);
            CheckBox cbComplete = taskView.findViewById(R.id.cbComplete);

            int color;
            if (task[2].equals("urgent")) color = 0xFFEF4444;
            else if (task[2].equals("important")) color = 0xFFF59E0B;
            else if (task[2].equals("normal")) color = 0xFF9CA3AF;
            else color = 0xFF10B981;

            colorStrip.setBackgroundColor(color);
            tvTitle.setText(task[0]);
            tvDeadline.setText(task[1]);

            if (task[2].equals("done")) {
                cbComplete.setChecked(true);
                taskView.setAlpha(0.5f);
            }

            containerTasks.addView(taskView);
        }
    }

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
            Toast.makeText(getContext(), "Добавление задачи", Toast.LENGTH_SHORT).show();
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

    // ==================== МЕТОДЫ ДЛЯ СТАТУСА ОФИСА ====================

    private void checkOfficeStatus() {
        // Для Owner/RGO - скрываем статус
        if (currentUserRole.equals("owner") || currentUserRole.equals("rgo")) {
            if (tvOfficeStatus != null) {
                tvOfficeStatus.setVisibility(View.GONE);
            }
            return;
        }

        // Для DM и сотрудников - показываем статус
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