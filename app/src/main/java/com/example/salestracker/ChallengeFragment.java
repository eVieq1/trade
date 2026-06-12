package com.example.salestracker;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class ChallengeFragment extends Fragment {

    private ApiClient apiClient;
    private String currentUserRole;
    private int currentOfficeId;

    private LinearLayout layoutOfficeSelector;
    private Spinner spinnerOffice;
    private ImageButton btnAddChallenge;
    private GridLayout gridChallenges;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    private List<ChallengePlan> challengeList = new ArrayList<>();
    private List<Shop> shopList = new ArrayList<>();
    private int selectedOfficeId;
    private boolean canEdit;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_challenge, container, false);

        initViews(view);
        loadUserData();
        setupListeners();
        loadShops();

        return view;
    }

    private void initViews(View view) {
        layoutOfficeSelector = view.findViewById(R.id.layoutOfficeSelector);
        spinnerOffice = view.findViewById(R.id.spinnerOffice);
        btnAddChallenge = view.findViewById(R.id.btnAddChallenge);
        gridChallenges = view.findViewById(R.id.gridChallenges);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmpty = view.findViewById(R.id.tvEmpty);
    }

    private void loadUserData() {
        apiClient = new ApiClient();
        SharedPreferences prefs = requireActivity().getSharedPreferences("app", Context.MODE_PRIVATE);
        currentUserRole = prefs.getString("user_role", "seller");
        currentOfficeId = prefs.getInt("office_id", 0);

        canEdit = currentUserRole.equals("owner") || currentUserRole.equals("rgo") || currentUserRole.equals("dm");

        if (currentUserRole.equals("owner") || currentUserRole.equals("rgo")) {
            layoutOfficeSelector.setVisibility(View.VISIBLE);
            if (canEdit) btnAddChallenge.setVisibility(View.VISIBLE);
        } else if (currentUserRole.equals("dm")) {
            layoutOfficeSelector.setVisibility(View.GONE);
            selectedOfficeId = currentOfficeId;
            if (canEdit) btnAddChallenge.setVisibility(View.VISIBLE);
            loadChallenges();
        } else {
            layoutOfficeSelector.setVisibility(View.GONE);
            selectedOfficeId = currentOfficeId;
            loadChallenges();
        }

        if (btnAddChallenge != null) {
            btnAddChallenge.setOnClickListener(v -> showAddChallengeDialog());
        }
    }

    private void setupListeners() {
        if (spinnerOffice != null) {
            spinnerOffice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position > 0) {
                        selectedOfficeId = shopList.get(position - 1).id;
                        loadChallenges();
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }
    }

    private void loadShops() {
        apiClient.getShops(new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    JSONArray arr = obj.getJSONArray("shops");
                    shopList.clear();

                    List<String> officeNames = new ArrayList<>();
                    officeNames.add("Выберите офис");

                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject shop = arr.getJSONObject(i);
                        shopList.add(new Shop(shop.getInt("id"), shop.getString("name")));
                        officeNames.add(shop.getString("name"));
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                            android.R.layout.simple_spinner_dropdown_item, officeNames);
                    spinnerOffice.setAdapter(adapter);

                    if (currentOfficeId > 0) {
                        for (int i = 0; i < shopList.size(); i++) {
                            if (shopList.get(i).id == currentOfficeId) {
                                spinnerOffice.setSelection(i + 1);
                                selectedOfficeId = currentOfficeId;
                                loadChallenges();
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onError(String error) {}
        });
    }

    private void loadChallenges() {
        if (selectedOfficeId == 0) return;

        progressBar.setVisibility(View.VISIBLE);

        // Тестовые данные
        challengeList.clear();
        String[] phoneModels = {"Samsung", "Realme", "Huawei", "Tecno", "Redmi", "Infinix", "Honor", "Другие"};
        double[] phoneTargets = {500000, 700000, 300000, 200000, 400000, 0, 0, 0};
        double[] phoneFacts = {500000, 450000, 180000, 100000, 250000, 0, 0, 0};

        for (int i = 0; i < phoneModels.length; i++) {
            if (phoneTargets[i] > 0) {
                challengeList.add(new ChallengePlan("phone", phoneModels[i], phoneTargets[i], phoneFacts[i], "₽"));
            }
        }

        challengeList.add(new ChallengePlan("adapter", "Адаптер", 150, 90, "шт"));

        displayChallenges();
        progressBar.setVisibility(View.GONE);
    }

    private void displayChallenges() {
        if (challengeList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }
        tvEmpty.setVisibility(View.GONE);

        gridChallenges.removeAllViews();

        for (ChallengePlan challenge : challengeList) {
            View card = LayoutInflater.from(getContext()).inflate(R.layout.item_challenge_card, null);

            TextView tvIcon = card.findViewById(R.id.tvIcon);
            TextView tvModel = card.findViewById(R.id.tvModel);
            TextView tvTarget = card.findViewById(R.id.tvTarget);
            TextView tvFact = card.findViewById(R.id.tvFact);
            TextView tvPercent = card.findViewById(R.id.tvPercent);
            ProgressBar progressBar = card.findViewById(R.id.progressBar);
            Button btnEdit = card.findViewById(R.id.btnEdit);

            tvIcon.setText(challenge.getCategory().equals("phone") ? "📱" : "🔌");
            tvModel.setText(challenge.getModel());
            tvTarget.setText("План: " + formatNumber(challenge.getTarget()) + " " + challenge.getUnit());
            tvFact.setText("Факт: " + formatNumber(challenge.getFact()) + " " + challenge.getUnit());
            int percent = challenge.getPercent();
            tvPercent.setText(percent + "%");
            progressBar.setProgress(percent);

            if (canEdit) {
                btnEdit.setVisibility(View.VISIBLE);
                btnEdit.setOnClickListener(v -> showEditChallengeDialog(challenge));
            }

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(8, 8, 8, 8);
            card.setLayoutParams(params);
            gridChallenges.addView(card);
        }
    }

    private void showAddChallengeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_challenge, null);

        Spinner spinnerCategory = view.findViewById(R.id.spinnerCategory);
        Spinner spinnerModel = view.findViewById(R.id.spinnerModel);
        EditText etTarget = view.findViewById(R.id.etTarget);

        String[] categories = {"Телефон", "Адаптер"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCategory.setAdapter(categoryAdapter);

        String[] phoneModels = {"Samsung", "Realme", "Huawei", "Tecno", "Redmi", "Infinix", "Honor", "Другие"};
        String[] adapterModels = {"Адаптер"};

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    ArrayAdapter<String> modelAdapter = new ArrayAdapter<>(getContext(),
                            android.R.layout.simple_spinner_dropdown_item, phoneModels);
                    spinnerModel.setAdapter(modelAdapter);
                } else {
                    ArrayAdapter<String> modelAdapter = new ArrayAdapter<>(getContext(),
                            android.R.layout.simple_spinner_dropdown_item, adapterModels);
                    spinnerModel.setAdapter(modelAdapter);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerCategory.setSelection(0);

        builder.setTitle("Добавить вызов")
                .setView(view)
                .setPositiveButton("Добавить", (dialog, which) -> {
                    // Здесь будет сохранение
                    Toast.makeText(getContext(), "Вызов добавлен", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showEditChallengeDialog(ChallengePlan challenge) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_challenge, null);

        EditText etTarget = view.findViewById(R.id.etTarget);
        etTarget.setText(String.valueOf((int) challenge.getTarget()));

        builder.setTitle("Редактировать: " + challenge.getModel())
                .setView(view)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    // Здесь будет сохранение
                    Toast.makeText(getContext(), "Сохранено", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Удалить", (dialog, which) -> {
                    Toast.makeText(getContext(), "Удалено", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("Отмена", null)
                .show();
    }

    private String formatNumber(double value) {
        if (value >= 1000000) return String.format("%.1fM", value / 1000000);
        if (value >= 1000) return String.format("%.1fK", value / 1000);
        return String.valueOf((int) value);
    }

    static class Shop {
        int id;
        String name;
        Shop(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}