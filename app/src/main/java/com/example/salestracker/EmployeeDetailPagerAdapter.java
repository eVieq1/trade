package com.example.salestracker.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.salestracker.EmployeePlanFragment;
import com.example.salestracker.fragments.*;

public class EmployeeDetailPagerAdapter extends FragmentStateAdapter {

    private int employeeId;
    private String userRole;
    private String employeeName;

    public EmployeeDetailPagerAdapter(@NonNull FragmentActivity fragmentActivity, int employeeId, String userRole, String employeeName) {
        super(fragmentActivity);
        this.employeeId = employeeId;
        this.userRole = userRole;
        this.employeeName = employeeName;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return EmployeeSalesFragment.newInstance(employeeId, employeeName);
            case 1:
                return EmployeeTrainingFragment.newInstance(employeeId, employeeName);
            case 2:
                return EmployeeAchievementsFragment.newInstance(employeeId, employeeName);
            case 3:
                return EmployeePlanFragment.newInstance(employeeId, employeeName, userRole);
            case 4:
                return EmployeeTasksFragment.newInstance(employeeId, employeeName);
            case 5:
                return EmployeeAgreementsFragment.newInstance(employeeId, employeeName);
            default:
                return EmployeeSalesFragment.newInstance(employeeId, employeeName);
        }
    }

    @Override
    public int getItemCount() {
        return 6;
    }
}