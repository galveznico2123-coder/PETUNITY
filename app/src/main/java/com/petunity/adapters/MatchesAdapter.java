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
import com.petunity.models.PetProfile;

import java.util.List;
import java.util.Locale;

public class MatchesAdapter extends RecyclerView.Adapter<MatchesAdapter.ViewHolder> {

    private final List<PetProfile> matches;
    private final PetProfile myPet;
    private final OnMatchClickListener listener;

    public interface OnMatchClickListener {
        void onMatchClick(PetProfile profile);
    }

    public MatchesAdapter(List<PetProfile> matches, PetProfile myPet, OnMatchClickListener listener) {
        this.matches = matches;
        this.myPet = myPet;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Use the dedicated play match layout
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_play_match, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PetProfile match = matches.get(position);
        
        // Calculate score based on user's pet vs the other pet
        int score = 0;
        if (myPet != null) {
            score = myPet.calculateMatchScore(match);
        }

        holder.nameText.setText(String.format(Locale.getDefault(), "%s, %d", match.getName(), match.getAge()));
        holder.breedText.setText(String.format(Locale.getDefault(), "%s • %s", match.getBreed(), match.getSize()));
        holder.locationText.setText(match.getLocation() != null ? match.getLocation() : "Manila, PH");
        
        // Display score prominently
        holder.matchScoreText.setText(String.format(Locale.getDefault(), "%d%% Match", score));

        Glide.with(holder.itemView.getContext())
                .load(match.getImageUrl())
                .placeholder(R.drawable.logo_pet)
                .centerCrop()
                .into(holder.petImage);

        holder.itemView.setOnClickListener(v -> listener.onMatchClick(match));
    }

    @Override
    public int getItemCount() {
        return matches.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView petImage;
        TextView nameText, breedText, locationText, matchScoreText;

        ViewHolder(View view) {
            super(view);
            petImage = view.findViewById(R.id.petImage);
            nameText = view.findViewById(R.id.petNameText);
            breedText = view.findViewById(R.id.petBreedText);
            locationText = view.findViewById(R.id.petLocationText);
            matchScoreText = view.findViewById(R.id.matchScoreText);
        }
    }
}
