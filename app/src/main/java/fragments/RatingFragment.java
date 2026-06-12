package com.example.salestracker.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.salestracker.ApiClient;
import com.example.salestracker.R;
import com.example.salestracker.utils.NetworkUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;

public class RatingFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout containerRating;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private ApiClient apiClient;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rating, container, false);

        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        containerRating = view.findViewById(R.id.containerRating);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);

        apiClient = new ApiClient();

        swipeRefresh.setOnRefreshListener(() -> refreshData());

        loadRating();

        return view;
    }

    public void refreshData() {
        if (getContext() != null && isAdded()) {
            loadRating();
            Toast.makeText(getContext(), "Данные обновлены", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadRating() {
        progressBar.setVisibility(View.VISIBLE);
        containerRating.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.GONE);

        if (swipeRefresh != null) {
            swipeRefresh.setRefreshing(false);
        }

        if (!NetworkUtils.isNetworkAvailable(getContext())) {
            progressBar.setVisibility(View.GONE);
            showEmptyState("Нет подключения к интернету");
            NetworkUtils.showNoInternetMessage(getContext());
            return;
        }

        SharedPreferences prefs = requireActivity().getSharedPreferences("app", Context.MODE_PRIVATE);
        int officeId = prefs.getInt("office_id", 0);

        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;

        apiClient.getRating(year, month, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                progressBar.setVisibility(View.GONE);

                try {
                    JSONObject obj = new JSONObject(response);
                    String status = obj.getString("status");

                    if (status.equals("success")) {
                        JSONArray ratingArray = obj.getJSONArray("rating");
                        displayRating(ratingArray);
                    } else {
                        showEmptyState("Нет данных для отображения");
                    }
                } catch (Exception e) {
                    showEmptyState("Ошибка загрузки: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                showEmptyState("Ошибка соединения: " + error);
                Toast.makeText(getContext(), "Ошибка загрузки рейтинга", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayRating(JSONArray ratingArray) throws Exception {
        containerRating.removeAllViews();
        containerRating.setVisibility(View.VISIBLE);

        if (ratingArray.length() == 0) {
            showEmptyState("Нет данных о продажах за текущий месяц");
            return;
        }

        for (int i = 0; i < ratingArray.length(); i++) {
            JSONObject item = ratingArray.getJSONObject(i);
            String name = item.getString("name");
            int percent = item.getInt("percent");
            double sales = item.getDouble("sales");
            int place = item.getInt("place");
            String medal = item.getString("medal");

            CardView card = createRatingCard(place, name, percent, sales, medal);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(16, 8, 16, 8);
            card.setLayoutParams(params);
            containerRating.addView(card);
        }
    }

    private void showEmptyState(String message) {
        containerRating.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.VISIBLE);
        tvEmptyState.setText(message);
    }

    private CardView createRatingCard(int place, String name, int percent, double sales, String medal) {
        CardView card = new CardView(getContext());
        card.setRadius(12);
        card.setCardElevation(4);
        card.setBackgroundColor(0xFFFFFFFF);

        LinearLayout content = new LinearLayout(getContext());
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setPadding(16, 16, 16, 16);

        TextView tvPlace = new TextView(getContext());
        tvPlace.setText(place + " " + medal);
        tvPlace.setTextSize(18);
        tvPlace.setTypeface(null, android.graphics.Typeface.BOLD);
        tvPlace.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.8f));

        TextView tvName = new TextView(getContext());
        tvName.setText(name);
        tvName.setTextSize(16);
        tvName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.5f));

        LinearLayout percentLayout = new LinearLayout(getContext());
        percentLayout.setOrientation(LinearLayout.VERTICAL);
        percentLayout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvPercent = new TextView(getContext());
        tvPercent.setText(percent + "%");
        tvPercent.setTextSize(16);
        tvPercent.setTypeface(null, android.graphics.Typeface.BOLD);
        tvPercent.setTextColor(0xFFFF9800);
        percentLayout.addView(tvPercent);

        TextView tvSales = new TextView(getContext());
        tvSales.setText(String.format("%,.0f ₽", sales));
        tvSales.setTextSize(11);
        tvSales.setTextColor(0xFF666666);
        percentLayout.addView(tvSales);

        content.addView(tvPlace);
        content.addView(tvName);
        content.addView(percentLayout);

        card.addView(content);
        return card;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (apiClient != null) {
            apiClient.shutdown();
        }
    }
}