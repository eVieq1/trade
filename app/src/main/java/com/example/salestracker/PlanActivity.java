package com.example.salestracker;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class PlanActivity extends AppCompatActivity {

    private TextView tabOffice, tabEmployee, tabChallenge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Планы");

        tabOffice = findViewById(R.id.tabOffice);
        tabEmployee = findViewById(R.id.tabEmployee);
        tabChallenge = findViewById(R.id.tabChallenge);

        tabOffice.setOnClickListener(v -> switchToFragment(new OfficePlanFragment(), tabOffice));
        tabEmployee.setOnClickListener(v -> switchToFragment(new EmployeePlanFragment(), tabEmployee));
        tabChallenge.setOnClickListener(v -> switchToFragment(new ChallengeFragment(), tabChallenge));

        switchToFragment(new OfficePlanFragment(), tabOffice);
    }

    private void switchToFragment(Fragment fragment, TextView selectedTab) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.container, fragment);
        transaction.commit();

        resetTabStyles();
        selectedTab.setBackgroundResource(R.drawable.bg_tab_selected);
        selectedTab.setTextColor(0xFFFFFFFF);
    }

    private void resetTabStyles() {
        tabOffice.setBackgroundResource(R.drawable.bg_tab_unselected);
        tabOffice.setTextColor(0xFF666666);
        tabEmployee.setBackgroundResource(R.drawable.bg_tab_unselected);
        tabEmployee.setTextColor(0xFF666666);
        tabChallenge.setBackgroundResource(R.drawable.bg_tab_unselected);
        tabChallenge.setTextColor(0xFF666666);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}