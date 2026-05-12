package com.petunity.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.petunity.R;
import com.petunity.models.User;

import java.util.List;

public class OnlineUsersAdapter extends RecyclerView.Adapter<OnlineUsersAdapter.OnlineUserViewHolder> {

    private final List<User> userList;
    private final OnUserClickListener listener;

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    public OnlineUsersAdapter(List<User> userList, OnUserClickListener listener) {
        this.userList = userList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OnlineUserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_online_user, parent, false);
        return new OnlineUserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OnlineUserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.nameText.setText(user.getName());
        
        Glide.with(holder.itemView.getContext())
                .load(user.getProfileImageUrl())
                .circleCrop()
                .placeholder(R.drawable.ic_user)
                .into(holder.avatarImage);

        holder.onlineIndicator.setVisibility(user.isOnline() ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> listener.onUserClick(user));
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class OnlineUserViewHolder extends RecyclerView.ViewHolder {
        ImageView avatarImage;
        View onlineIndicator;
        TextView nameText;

        public OnlineUserViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImage = itemView.findViewById(R.id.onlineUserAvatar);
            onlineIndicator = itemView.findViewById(R.id.onlineIndicator);
            nameText = itemView.findViewById(R.id.onlineUserName);
        }
    }
}
