package com.example.salestracker.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.salestracker.OfficeStatus;
import com.example.salestracker.R;
import com.example.salestracker.TodayShiftsActivity;
import java.util.ArrayList;
import java.util.List;

public class OfficeStatusAdapter extends RecyclerView.Adapter<OfficeStatusAdapter.ViewHolder> {

    private Context context;
    private List<OfficeStatus> list = new ArrayList<>();

    public OfficeStatusAdapter(Context context) {
        this.context = context;
    }

    public void setData(List<OfficeStatus> data) {
        this.list = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_office_status, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OfficeStatus office = list.get(position);

        holder.tvOfficeName.setText(office.getOfficeName());

        if (office.isOpen()) {
            holder.tvStatus.setText("🟢 ОТКРЫТ");
            holder.tvStatus.setTextColor(Color.parseColor("#4CAF50"));
            holder.tvDetail.setText("Открыл: " + office.getOpenedBy() + " в " + office.getOpenedAt());
        } else {
            holder.tvStatus.setText("🔴 ЗАКРЫТ");
            holder.tvStatus.setTextColor(Color.parseColor("#F44336"));
            holder.tvDetail.setText("Не открыт сегодня");
        }

        holder.cardView.setOnClickListener(v -> {
            Intent intent = new Intent(context, TodayShiftsActivity.class);
            intent.putExtra("office_id", office.getOfficeId());
            intent.putExtra("office_name", office.getOfficeName());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvOfficeName, tvStatus, tvDetail;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            tvOfficeName = itemView.findViewById(R.id.tvOfficeName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDetail = itemView.findViewById(R.id.tvDetail);
        }
    }
}