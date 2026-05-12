package com.petunity.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.petunity.R;
import com.petunity.models.PlaydateRequest;
import java.util.List;

public class PlaydateRequestsAdapter extends RecyclerView.Adapter<PlaydateRequestsAdapter.ViewHolder> {
    private List<PlaydateRequest> requests;
    private OnRequestActionListener listener;
    private String currentUserId;

    public interface OnRequestActionListener {
        void onAccept(PlaydateRequest request);
        void onDecline(PlaydateRequest request);
        void onChat(PlaydateRequest request);
        void onComplete(PlaydateRequest request);
    }

    public PlaydateRequestsAdapter(List<PlaydateRequest> requests, String currentUserId, OnRequestActionListener listener) {
        this.requests = requests;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playdate_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlaydateRequest request = requests.get(position);
        
        boolean isReceiver = request.getReceiverId().equals(currentUserId);
        
        if (isReceiver) {
            holder.title.setText(request.getSenderPetName() + " wants a playdate!");
        } else {
            holder.title.setText("Request sent to " + request.getReceiverPetName());
        }
        
        holder.details.setText("📍 " + request.getSuggestedLocation() + " • " + request.getSuggestedTime());
        holder.status.setText(request.getStatus());
        
        // Status styling & Visibility
        if ("Pending".equals(request.getStatus())) {
            holder.status.setText("Pending");
            holder.status.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFF3E0));
            holder.status.setTextColor(0xFFF57C00);
            holder.actionLayout.setVisibility(isReceiver ? View.VISIBLE : View.GONE);
            holder.postAcceptActions.setVisibility(View.GONE);
        } else if ("Accepted".equals(request.getStatus())) {
            holder.status.setText("Upcoming");
            holder.status.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE3F2FD));
            holder.status.setTextColor(0xFF1976D2);
            holder.actionLayout.setVisibility(View.GONE);
            holder.postAcceptActions.setVisibility(View.VISIBLE);
        } else if ("Done".equals(request.getStatus())) {
            holder.status.setText("Done");
            holder.status.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE8F5E9));
            holder.status.setTextColor(0xFF2E7D32);
            holder.actionLayout.setVisibility(View.GONE);
            holder.postAcceptActions.setVisibility(View.GONE);
        } else if ("Declined".equals(request.getStatus())) {
            holder.status.setText("Declined");
            holder.status.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFEBEE));
            holder.status.setTextColor(0xFFD32F2F);
            holder.actionLayout.setVisibility(View.GONE);
            holder.postAcceptActions.setVisibility(View.GONE);
        } else {
            holder.actionLayout.setVisibility(View.GONE);
            holder.postAcceptActions.setVisibility(View.GONE);
        }

        holder.acceptBtn.setOnClickListener(v -> listener.onAccept(request));
        holder.declineBtn.setOnClickListener(v -> listener.onDecline(request));
        holder.chatButton.setOnClickListener(v -> listener.onChat(request));
        holder.completeBtn.setOnClickListener(v -> listener.onComplete(request));
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, details, status;
        View actionLayout, postAcceptActions;
        MaterialButton acceptBtn, declineBtn, chatButton, completeBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.requestTitle);
            details = itemView.findViewById(R.id.meetingDetails);
            status = itemView.findViewById(R.id.statusBadge);
            actionLayout = itemView.findViewById(R.id.actionButtons);
            postAcceptActions = itemView.findViewById(R.id.postAcceptActions);
            acceptBtn = itemView.findViewById(R.id.acceptButton);
            declineBtn = itemView.findViewById(R.id.declineButton);
            chatButton = itemView.findViewById(R.id.chatButton);
            completeBtn = itemView.findViewById(R.id.completeButton);
        }
    }
}
