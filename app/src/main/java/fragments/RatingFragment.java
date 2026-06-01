package com.example.salestracker.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import com.example.salestracker.R;

public class RatingFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rating, container, false);
        LinearLayout containerRating = view.findViewById(R.id.containerRating);

        String[][] ratingData = {
                {"1", "Анна", "125%", "🏆"},
                {"2", "Сергей", "98%", "🥈"},
                {"3", "Мария", "76%", "🥉"},
                {"4", "Дмитрий", "72%", "📊"}
        };

        for (String[] item : ratingData) {
            CardView card = createRatingCard(item);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(16, 8, 16, 8);
            card.setLayoutParams(params);
            containerRating.addView(card);
        }

        return view;
    }

    private CardView createRatingCard(String[] data) {
        CardView card = new CardView(getContext());
        card.setRadius(12);
        card.setCardElevation(4);
        card.setBackgroundColor(0xFFFFFFFF);

        LinearLayout content = new LinearLayout(getContext());
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setPadding(16, 16, 16, 16);

        TextView tvPlace = new TextView(getContext());
        tvPlace.setText(data[0]);
        tvPlace.setTextSize(18);
        tvPlace.setTypeface(null, android.graphics.Typeface.BOLD);
        tvPlace.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.5f));

        TextView tvName = new TextView(getContext());
        tvName.setText(data[1]);
        tvName.setTextSize(16);
        tvName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.5f));

        TextView tvPercent = new TextView(getContext());
        tvPercent.setText(data[2]);
        tvPercent.setTextSize(16);
        tvPercent.setTypeface(null, android.graphics.Typeface.BOLD);
        tvPercent.setTextColor(0xFFFF9800);
        tvPercent.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvMedal = new TextView(getContext());
        tvMedal.setText(data[3]);
        tvMedal.setTextSize(20);
        tvMedal.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.5f));

        content.addView(tvPlace);
        content.addView(tvName);
        content.addView(tvPercent);
        content.addView(tvMedal);

        card.addView(content);
        return card;
    }
}