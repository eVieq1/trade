package com.example.salestracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.salestracker.fragments.ScheduleFragment;
import java.util.List;

public class TodayScheduleAdapter extends RecyclerView.Adapter<TodayScheduleAdapter.ViewHolder> {

    private List<ScheduleFragment.ShiftData> shifts;

    public TodayScheduleAdapter(List<ScheduleFragment.ShiftData> shifts) {
        this.shifts = shifts;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_today_shift, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScheduleFragment.ShiftData shift = shifts.get(position);
        holder.tvEmployee.setText(shift.employee);
        holder.tvShiftTime.setText(shift.shiftTime);
    }

    @Override
    public int getItemCount() {
        return shifts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmployee, tvShiftTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmployee = itemView.findViewById(R.id.tvEmployee);
            tvShiftTime = itemView.findViewById(R.id.tvShiftTime);
        }
    }
}