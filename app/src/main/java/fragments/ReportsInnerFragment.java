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

public class ReportsInnerFragment extends Fragment {

    private String type = "goals";

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reports_inner, container, false);
        LinearLayout containerLayout = view.findViewById(R.id.innerContainer);

        if (type.equals("goals")) {
            // Данные для целей
            String[][] data = {
                    {"Gross SIM", "Факт: 0", "План: 0", "Вып. факт: 0%"},
                    {"Товарная выручка", "Факт: 0", "План: 0", "Вып. факт: 0%"},
                    {"ЯА", "Факт: 0", "План: 0", "Вып. факт: 0%"},
                    {"ШПД", "Факт: 0", "План: 0", "Вып. факт: 0%"}
            };
            displayCards(containerLayout, data);
        } else if (type.equals("motivation")) {
            String[][] data = {
                    {"ЯА", "План: 0 руб"},
                    {"Samsung", "План: 0 шт"},
                    {"Realme", "План: 0 шт"},
                    {"Huawei", "План: 0 шт"}
            };
            displayCards(containerLayout, data);

            TextView noPlanText = new TextView(getContext());
            noPlanText.setText("📦 Модели без плана: Honor, Infinix, Techo");
            noPlanText.setPadding(16, 16, 16, 16);
            noPlanText.setBackgroundColor(0xFFFFFFFF);
            containerLayout.addView(noPlanText);
        } else if (type.equals("reports")) {
            TextView tv = new TextView(getContext());
            tv.setText("Список отчётов:\n• Отчёт по продажам\n• Отчёт по сотрудникам");
            tv.setPadding(16, 16, 16, 16);
            containerLayout.addView(tv);
        }

        return view;
    }

    private void displayCards(LinearLayout container, String[][] data) {
        container.removeAllViews();
        for (int i = 0; i < data.length; i++) {
            if (i % 2 == 0) {
                LinearLayout row = new LinearLayout(getContext());
                row.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setWeightSum(2);
                container.addView(row);
            }
            LinearLayout row = (LinearLayout) container.getChildAt(container.getChildCount() - 1);
            CardView card = createCard(data[i]);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            params.setMargins(8, 8, 8, 8);
            card.setLayoutParams(params);
            row.addView(card);
        }
    }

    private CardView createCard(String[] data) {
        CardView card = new CardView(getContext());
        card.setRadius(12);
        card.setCardElevation(4);
        card.setBackgroundColor(0xFFFFFFFF);

        LinearLayout content = new LinearLayout(getContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(16, 16, 16, 16);

        for (String text : data) {
            TextView tv = new TextView(getContext());
            tv.setText(text);
            tv.setTextSize(14);
            if (text.equals(data[0])) {
                tv.setTypeface(null, android.graphics.Typeface.BOLD);
            }
            content.addView(tv);
        }

        card.addView(content);
        return card;
    }
}