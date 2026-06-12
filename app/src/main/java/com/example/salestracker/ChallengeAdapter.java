package com.example.salestracker.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.salestracker.R;
import com.example.salestracker.ChallengePlan;
import java.util.List;

public class ChallengeAdapter extends RecyclerView.Adapter<ChallengeAdapter.ViewHolder> {

    private List<ChallengePlan> challenges;
    private boolean canEdit;
    private OnEditListener editListener;

    public interface OnEditListener {
        void onEdit(ChallengePlan challenge);
    }

    public ChallengeAdapter(List<ChallengePlan> challenges, boolean canEdit, OnEditListener listener) {
        this.challenges = challenges;
        this.canEdit = canEdit;
        this.editListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_challenge_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChallengePlan challenge = challenges.get(position);

        if (challenge.getCategory().equals("phone")) {
            holder.tvIcon.setText("📱");
        } else {
            holder.tvIcon.setText("🔌");
        }

        holder.tvModel.setText(challenge.getModel());
        holder.tvTarget.setText("План: " + formatNumber(challenge.getTarget()) + " " + challenge.getUnit());
        holder.tvFact.setText("Факт: " + formatNumber(challenge.getFact()) + " " + challenge.getUnit());

        int percent = challenge.getPercent();
        holder.tvPercent.setText(percent + "%");
        holder.progressBar.setProgress(percent);

        if (canEdit) {
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnEdit.setOnClickListener(v -> {
                if (editListener != null) {
                    editListener.onEdit(challenge);
                }
            });
        } else {
            holder.btnEdit.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return challenges.size();
    }

    private String formatNumber(double value) {
        if (value >= 1000000) {
            return String.format("%.1fM", value / 1000000);
        } else if (value >= 1000) {
            return String.format("%.1fK", value / 1000);
        }
        return String.valueOf((int) value);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon, tvModel, tvTarget, tvFact, tvPercent;
        ProgressBar progressBar;
        Button btnEdit;

        ViewHolder(View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tvIcon);
            tvModel = itemView.findViewById(R.id.tvModel);
            tvTarget = itemView.findViewById(R.id.tvTarget);
            tvFact = itemView.findViewById(R.id.tvFact);
            tvPercent = itemView.findViewById(R.id.tvPercent);
            progressBar = itemView.findViewById(R.id.progressBar);
            btnEdit = itemView.findViewById(R.id.btnEdit);
        }
    }
}