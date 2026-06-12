package com.example.salestracker.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public class EmployeeTasksFragment extends Fragment {

    private int employeeId;
    private String employeeName;

    public static EmployeeTasksFragment newInstance(int employeeId, String employeeName) {
        EmployeeTasksFragment fragment = new EmployeeTasksFragment();
        Bundle args = new Bundle();
        args.putInt("employee_id", employeeId);
        args.putString("employee_name", employeeName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getArguments() != null) {
            employeeId = getArguments().getInt("employee_id");
            employeeName = getArguments().getString("employee_name");
        }
        TextView tv = new TextView(getContext());
        tv.setText("✅ ЗАДАЧИ\n\nСотрудник: " + employeeName + "\nID: " + employeeId + "\n\nВ разработке");
        tv.setPadding(32, 32, 32, 32);
        tv.setTextSize(14);
        tv.setTextColor(0xFF666666);
        tv.setGravity(android.view.Gravity.CENTER);
        return tv;
    }
}