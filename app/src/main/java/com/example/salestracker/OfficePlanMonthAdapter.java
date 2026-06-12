package com.example.salestracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class OfficePlanMonthAdapter extends RecyclerView.Adapter<OfficePlanMonthAdapter.ViewHolder> {

    private List<OfficePlan> plans;
    private String[] icons = {"📞", "🎧", "📱", "📶", "🔌", "💰"};

    public OfficePlanMonthAdapter(List<OfficePlan> plans) {
        this.plans = plans;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_plan_month, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OfficePlan plan = plans.get(position);
        holder.tvIcon.setText(icons[position % icons.length]);
        holder.tvTitle.setText(plan.getCategory());
        holder.tvTarget.setText("План: " + formatNumber(plan.getTarget()) + " " + plan.getUnit());
        holder.tvFact.setText("Факт: " + formatNumber(plan.getFact()) + " " + plan.getUnit());
        int percent = plan.getPercent();
        holder.tvPercent.setText(percent + "%");
        holder.progressBar.setProgress(percent);
    }

    @Override
    public int getItemCount() {
        return plans.size();
    }

    private String formatNumber(double value) {
        if (value >= 1000000) return String.format("%.1fM", value / 1000000);
        if (value >= 1000) return String.format("%.1fK", value / 1000);
        return String.valueOf((int) value);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon, tvTitle, tvTarget, tvFact, tvPercent;
        ProgressBar progressBar;

        ViewHolder(View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tvIcon);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvTarget = itemView.findViewById(R.id.tvTarget);
            tvFact = itemView.findViewById(R.id.tvFact);
            tvPercent = itemView.findViewById(R.id.tvPercent);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }
}