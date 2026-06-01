package com.example.salestracker.fragments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import com.example.salestracker.R;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReportsFragment extends Fragment {

    private Button btnCopyReport;
    private ViewPager2 innerViewPager;
    private TextView tabGoals, tabMotivation, tabReports;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reports, container, false);

        btnCopyReport = view.findViewById(R.id.btnCopyReport);
        innerViewPager = view.findViewById(R.id.innerViewPager);
        tabGoals = view.findViewById(R.id.tabGoals);
        tabMotivation = view.findViewById(R.id.tabMotivation);
        tabReports = view.findViewById(R.id.tabReports);

        ReportsInnerFragment goalsFragment = new ReportsInnerFragment();
        goalsFragment.setType("goals");
        ReportsInnerFragment motivationFragment = new ReportsInnerFragment();
        motivationFragment.setType("motivation");
        ReportsInnerFragment reportsFragment = new ReportsInnerFragment();
        reportsFragment.setType("reports");

        InnerPagerAdapter adapter = new InnerPagerAdapter(this, goalsFragment, motivationFragment, reportsFragment);
        innerViewPager.setAdapter(adapter);

        innerViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateTabs(position);
                btnCopyReport.setVisibility(position == 2 ? View.VISIBLE : View.GONE);
            }
        });

        tabGoals.setOnClickListener(v -> innerViewPager.setCurrentItem(0));
        tabMotivation.setOnClickListener(v -> innerViewPager.setCurrentItem(1));
        tabReports.setOnClickListener(v -> innerViewPager.setCurrentItem(2));

        updateTabs(0);
        btnCopyReport.setOnClickListener(v -> copyReportToClipboard());

        return view;
    }

    private void updateTabs(int position) {
        int defaultColor = 0xFFE0E0E0;
        int selectedColor = 0xFF2196F3;
        int defaultTextColor = 0xFF333333;
        int selectedTextColor = 0xFFFFFFFF;

        tabGoals.setBackgroundColor(defaultColor);
        tabMotivation.setBackgroundColor(defaultColor);
        tabReports.setBackgroundColor(defaultColor);
        tabGoals.setTextColor(defaultTextColor);
        tabMotivation.setTextColor(defaultTextColor);
        tabReports.setTextColor(defaultTextColor);

        if (position == 0) {
            tabGoals.setBackgroundColor(selectedColor);
            tabGoals.setTextColor(selectedTextColor);
        } else if (position == 1) {
            tabMotivation.setBackgroundColor(selectedColor);
            tabMotivation.setTextColor(selectedTextColor);
        } else if (position == 2) {
            tabReports.setBackgroundColor(selectedColor);
            tabReports.setTextColor(selectedTextColor);
        }
    }

    private String generateReportText() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        String today = sdf.format(new Date());
        return "🏢 ОФИС КУКОНКОВЫХ\n📅 Отчёт за " + today + "\n\n📞 Сим: 30/9\n🎧 Семплинг: 3/3\n📱 ЯА: 7/3\n🌐 ШПД: 2/0";
    }

    private void copyReportToClipboard() {
        String reportText = generateReportText();
        ClipboardManager clipboard = (ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("sales_report", reportText);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(getContext(), "✅ Отчёт скопирован в буфер обмена", Toast.LENGTH_LONG).show();
    }

    class InnerPagerAdapter extends androidx.viewpager2.adapter.FragmentStateAdapter {
        private final Fragment[] fragments;

        public InnerPagerAdapter(Fragment fragment, Fragment f1, Fragment f2, Fragment f3) {
            super(fragment);
            this.fragments = new Fragment[]{f1, f2, f3};
        }

        @Override
        public int getItemCount() { return 3; }

        @Override
        public Fragment createFragment(int position) {
            return fragments[position];
        }
    }
}